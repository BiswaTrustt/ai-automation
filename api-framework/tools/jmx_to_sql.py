#!/usr/bin/env python3
"""
Parse a JMeter .jmx file and emit a Flyway migration that seeds the
api-automation-framework so the entire JMX flow runs end-to-end via the
journey executor.

Usage:
  python3 jmx_to_sql.py <input.jmx> <product_code> <out.sql>

Emits SQL that inserts:
  - one api_master row per enabled HTTPSamplerProxy
  - one api_request_templates row per sampler with a JSON body
  - api_headers rows (default Authorization + standard set)
  - one product_module_mapping per TransactionController (under <product_code>)
  - one test_scenario_master row "<PRODUCT>_E2E_FLOW" flagged cross_module
  - one api_scenario_mapping per sampler with execution_order, loop_count,
    extraction_mappings derived from JSONPostProcessor / RegexExtractor

The script keeps the JMX flow's exact ordering and inner LoopController
counts; WhileController / IfController bodies are emitted with loop_count=1
(condition-driven loops are out of scope for first-cut replay).
"""

from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import OrderedDict
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# ---------------------------------------------------------------------------
# JMX user-defined variables (resolved at parse time so URLs become literal).
# Pulled from the <Arguments> block at the top of Common Data Usage - qa5.jmx.
# ---------------------------------------------------------------------------
JMX_CONSTANTS: Dict[str, str] = {
    "URL": "qa5-mfi.novopay.in",
    "PORT": "",
    "PATH": "/api-gateway/perf/v1",
    "npPATH": "/api-gateway/api/v1",
    "BPMNPATH": "/api-gateway/api/v1",
    "sesPath": "/api-gateway/api/v1",
    "mfiURL": "qa-mfi.novopay.in",
    "perfURL": "mfi-perf.novopay.in",
    "AccountURL": "mfi-perf-yugabyte.novopay.in",
    "MaxRetry": "3",
    "OTP": "123456",
    # Outer load-test loop in the JMX wraps the entire flow — for a one-shot
    # functional replay we only want to execute each step once. Pre-setting
    # this stops parse_loop_count from multiplying every step by 2.
    "loopCount": "1",
}

# JMeter ${__functions} → DynamicValueResolver tokens.
FUNCTION_REWRITES: List[Tuple[re.Pattern, str]] = [
    (re.compile(r"\$\{__RandomString\((\d+),[^)]*\)\}"), r"${RANDOM:\1}"),
    (re.compile(r"\$\{__Random\((\d+),(\d+)[^)]*\)\}"), r"${RANDOM:10}"),
    (re.compile(r"\$\{__time\([^)]*\)\}"), r"${TIMESTAMP}"),
    (re.compile(r"\$\{__UUID\(\)\}"), r"${RANDOM:UUID}"),
    (re.compile(r"\$\{__counter\([^)]*\)\}"), r"${RANDOM:6}"),
]

# Variables the resolver should map directly to RANDOM/TIMESTAMP rather than CTX.
SYNTHETIC_VARS: Dict[str, str] = {
    "timestamp": "${RANDOM:6}",
    "randomMobNumber": "${RANDOM:10}",
    "mobileno": "${RANDOM:10}",
    "mobileno1": "${RANDOM:10}",
    "mobileno2": "${RANDOM:10}",
    "barcode_number": "${RANDOM:12}",
    "counter": "${RANDOM:6}",
    "custom_loop_index": "${ITERATION}",
    "iteration_index": "${ITERATION}",
    "loopCount": "1",
    "MaxRetry": "3",
    "OTP": "123456",
    "moveprocess_delay": "0",
    "servicestatus_delay": "0",
    "stage_delay": "0",
    "constant_delay": "0",
    "random_delay": "0",
    "ttime": "0",
}


def sql_escape(text: Optional[str]) -> str:
    if text is None:
        return "NULL"
    return "'" + text.replace("'", "''") + "'"


def resolve_constants(text: str) -> str:
    """Replace ${VAR} for VARs in JMX_CONSTANTS with their literal values."""
    if text is None:
        return ""
    out = text
    for k, v in JMX_CONSTANTS.items():
        out = out.replace("${" + k + "}", v)
    return out


def rewrite_placeholders(text: str) -> str:
    """Convert JMeter placeholders to DynamicValueResolver tokens.

    - ${__RandomString(N,chars,name)} -> ${RANDOM:N}
    - ${__time(...)} -> ${TIMESTAMP}
    - ${__UUID()} -> ${RANDOM:UUID}
    - SYNTHETIC_VARS map (timestamp, mobileno, ...) to specific tokens.
    - Any other ${var} becomes ${CTX:var}.
    """
    if text is None:
        return ""

    out = text
    for pat, repl in FUNCTION_REWRITES:
        out = pat.sub(repl, out)

    # Replace constants we already know.
    out = resolve_constants(out)

    # Special-case synthetic vars before generic CTX rewrite.
    for var, repl in SYNTHETIC_VARS.items():
        out = out.replace("${" + var + "}", repl)

    # Generic ${name} -> ${CTX:name}, but skip already-prefixed tokens.
    def to_ctx(match: re.Match) -> str:
        token = match.group(1)
        if ":" in token:
            return match.group(0)
        return "${CTX:" + token + "}"

    out = re.sub(r"\$\{([A-Za-z_][A-Za-z0-9_]*)\}", to_ctx, out)
    return out


def is_enabled(elem: ET.Element) -> bool:
    return elem.get("enabled", "true").lower() != "false"


def child_text(elem: ET.Element, name: str) -> Optional[str]:
    for child in elem:
        if child.get("name") == name:
            return child.text
    return None


def safe_module_name(raw: str) -> str:
    """Compress a TransactionController name into a stable module code."""
    cleaned = re.sub(r"[^A-Za-z0-9]+", "_", raw or "").strip("_")
    if not cleaned:
        cleaned = "MODULE"
    return cleaned.upper()[:90]


def safe_api_name(raw: str, used: set, fallback_index: int) -> str:
    """Sanitize and dedupe an api_name (UNIQUE in api_master)."""
    base = re.sub(r"[^A-Za-z0-9_.\-]+", "_", raw or "").strip("_")
    if not base:
        base = f"sampler_{fallback_index}"
    base = base[:200]
    candidate = base
    suffix = 2
    while candidate in used:
        candidate = f"{base}__{suffix}"
        suffix += 1
    used.add(candidate)
    return candidate


def parse_loop_count(controller: ET.Element) -> int:
    raw = child_text(controller, "LoopController.loops") or "1"
    raw = resolve_constants(raw)
    if raw.startswith("${"):
        return 1
    try:
        n = int(raw)
        return max(1, n)
    except ValueError:
        return 1


# ---------------------------------------------------------------------------
# Walker — JMeter's hashTree pattern: each element is followed by a hashTree
# whose direct children are that element's children.
# ---------------------------------------------------------------------------

class Walker:
    def __init__(self):
        self.samplers: List[Dict] = []
        self.modules: "OrderedDict[str, str]" = OrderedDict()  # code -> display name
        self.order: int = 0
        self.module_seq: int = 0

    def walk(
        self,
        hash_tree: ET.Element,
        controller_stack: List[ET.Element],
        loop_multiplier: int,
        module_code: Optional[str],
        module_display: Optional[str],
        enabled: bool,
        thread_group_headers: Dict[str, str],
    ) -> None:
        children = list(hash_tree)
        i = 0
        while i < len(children):
            elem = children[i]
            sub = children[i + 1] if i + 1 < len(children) and children[i + 1].tag == "hashTree" else None
            i += 2 if sub is not None else 1

            tag = elem.tag
            elem_enabled = enabled and is_enabled(elem)

            if tag in ("LoopController",) and elem.get("testname") != "Loop Controller-stop":
                inner_loop = parse_loop_count(elem)
                if sub is not None:
                    self.walk(sub, controller_stack + [elem], loop_multiplier * inner_loop,
                              module_code, module_display, elem_enabled, thread_group_headers)
            elif tag in ("WhileController", "IfController", "OnceOnlyController",
                         "ForeachController", "ThroughputController", "RunTime"):
                if sub is not None:
                    self.walk(sub, controller_stack + [elem], loop_multiplier,
                              module_code, module_display, elem_enabled, thread_group_headers)
            elif tag == "TransactionController":
                tname = elem.get("testname", "Transaction")
                code = safe_module_name(tname)
                if code not in self.modules:
                    self.module_seq += 1
                    self.modules[code] = tname
                if sub is not None:
                    self.walk(sub, controller_stack + [elem], loop_multiplier,
                              code, tname, elem_enabled, thread_group_headers)
            elif tag == "GenericController":
                if sub is not None:
                    self.walk(sub, controller_stack + [elem], loop_multiplier,
                              module_code, module_display, elem_enabled, thread_group_headers)
            elif tag == "HTTPSamplerProxy":
                if elem_enabled:
                    self._record_sampler(elem, sub, loop_multiplier,
                                         module_code or "DEFAULT",
                                         module_display or "Default",
                                         thread_group_headers)
            elif tag == "HeaderManager":
                # Headers declared at this scope apply to siblings/descendants.
                # JMeter merges, but we just inherit into the current scope.
                if elem_enabled:
                    for k, v in self._read_headers(elem).items():
                        thread_group_headers.setdefault(k, v)
            elif tag == "ThreadGroup":
                if sub is not None:
                    self.walk(sub, controller_stack + [elem], loop_multiplier,
                              module_code, module_display, elem_enabled, thread_group_headers)
            elif tag == "hashTree":
                # Defensive — shouldn't happen at this position.
                self.walk(elem, controller_stack, loop_multiplier,
                          module_code, module_display, elem_enabled, thread_group_headers)
            else:
                # JDBCSampler, BeanShell*, RandomVariableConfig, etc. — ignore for now.
                if sub is not None:
                    # Recurse only if the element might contain controllers/samplers.
                    if tag in ("Arguments", "JDBCDataSource", "JSR223PreProcessor",
                               "JSR223PostProcessor", "JSR223Sampler", "JDBCSampler",
                               "ConstantTimer", "RandomVariableConfig", "CounterConfig",
                               "BeanShellPreProcessor", "BeanShellPostProcessor",
                               "ResponseAssertion", "JSONPostProcessor", "RegexExtractor",
                               "BoundaryExtractor", "DurationAssertion", "DebugSampler",
                               "CSVDataSet", "ConfigTestElement", "kg"):
                        continue
                    self.walk(sub, controller_stack, loop_multiplier,
                              module_code, module_display, elem_enabled, thread_group_headers)

    def _read_headers(self, header_mgr: ET.Element) -> Dict[str, str]:
        out: Dict[str, str] = OrderedDict()
        coll = None
        for c in header_mgr:
            if c.get("name") == "HeaderManager.headers":
                coll = c
                break
        if coll is None:
            return out
        for hp in coll.findall("elementProp"):
            name = child_text(hp, "Header.name") or ""
            value = child_text(hp, "Header.value") or ""
            if name:
                out[name] = value
        return out

    def _read_body(self, sampler: ET.Element) -> Optional[str]:
        post_raw = False
        for c in sampler:
            if c.get("name") == "HTTPSampler.postBodyRaw" and (c.text or "").lower() == "true":
                post_raw = True
        args = None
        for c in sampler:
            if c.get("name") == "HTTPsampler.Arguments":
                args = c
                break
        if args is None:
            return None
        coll = None
        for c in args:
            if c.get("name") == "Arguments.arguments":
                coll = c
                break
        if coll is None:
            return None
        first_arg = coll.find("elementProp")
        if first_arg is None:
            return None
        body = child_text(first_arg, "Argument.value")
        if body is None:
            return None
        if not post_raw:
            # Form-encoded -- emit as JSON map of name->value.
            data = {}
            for ap in coll.findall("elementProp"):
                k = child_text(ap, "Argument.name") or ""
                v = child_text(ap, "Argument.value") or ""
                if k:
                    data[k] = v
            return json.dumps(data) if data else None
        return body

    def _read_extractions(self, sub_tree: Optional[ET.Element]) -> Dict[str, str]:
        out: Dict[str, str] = OrderedDict()
        if sub_tree is None:
            return out
        for child in sub_tree:
            if child.tag == "JSONPostProcessor" and is_enabled(child):
                names = (child_text(child, "JSONPostProcessor.referenceNames") or "").strip()
                paths = (child_text(child, "JSONPostProcessor.jsonPathExprs") or "").strip()
                if names and paths:
                    name_list = [n.strip() for n in names.split(";") if n.strip()]
                    path_list = [p.strip() for p in paths.split(";") if p.strip()]
                    for idx, n in enumerate(name_list):
                        if idx < len(path_list):
                            out[n] = path_list[idx]
            elif child.tag == "RegexExtractor" and is_enabled(child):
                name = (child_text(child, "RegexExtractor.refname") or "").strip()
                regex = (child_text(child, "RegexExtractor.regex") or "").strip()
                if name and regex:
                    out[name] = "REGEX:" + regex
        return out

    def _record_sampler(self, sampler: ET.Element, sub_tree: Optional[ET.Element],
                        loop_multiplier: int, module_code: str, module_display: str,
                        thread_group_headers: Dict[str, str]) -> None:
        self.order += 10
        domain = child_text(sampler, "HTTPSampler.domain") or "${URL}"
        port = child_text(sampler, "HTTPSampler.port") or ""
        protocol = (child_text(sampler, "HTTPSampler.protocol") or "https").strip() or "https"
        path = child_text(sampler, "HTTPSampler.path") or "/"
        method = (child_text(sampler, "HTTPSampler.method") or "GET").upper()

        domain = resolve_constants(domain).strip()
        port = resolve_constants(port).strip()
        path_resolved = resolve_constants(path)

        if not domain:
            domain = "qa5-mfi.novopay.in"

        host = domain
        if port and port != "80" and port != "443":
            host = f"{domain}:{port}"
        base_url = f"{protocol}://{host}"

        body_raw = self._read_body(sampler)
        body = rewrite_placeholders(body_raw) if body_raw is not None else None
        # If body resolves to whitespace-only, drop it.
        if body is not None and not body.strip():
            body = None

        headers: Dict[str, str] = OrderedDict()
        for k, v in thread_group_headers.items():
            headers[k] = rewrite_placeholders(v)

        extractions = self._read_extractions(sub_tree)

        self.samplers.append({
            "testname": sampler.get("testname", f"sampler_{self.order}"),
            "module_code": module_code,
            "module_display": module_display,
            "base_url": base_url,
            "endpoint": rewrite_placeholders(path_resolved),
            "method": method,
            "body": body,
            "headers": headers,
            "extractions": extractions,
            "execution_order": self.order,
            "loop_count": loop_multiplier,
        })


# ---------------------------------------------------------------------------
# SQL emitter
# ---------------------------------------------------------------------------

HEADER_DOC = """\
-- =============================================================
-- V8__shg_jmx_seed.sql  (auto-generated by tools/jmx_to_sql.py)
-- Source: Common Data Usage - qa5.jmx  (SHG NTB E2E flow)
--
-- Seeds:
--   - {samplers} api_master rows (one per enabled JMeter sampler)
--   - request templates + default headers
--   - {modules} product_module_mapping rows under product '{product}'
--   - 1 test_scenario_master row '{scenario_code}' (cross_module)
--   - {samplers} api_scenario_mapping rows in JMX execution order
--
-- Each row keeps its parent TransactionController as module_name; the scenario
-- is flagged cross_module=TRUE so the executor walks every step regardless
-- of which module is selected in the UI.
--
-- Re-runnable: a leading TRUNCATE-style cleanup keeps reseeds idempotent.
-- =============================================================
"""

CLEANUP_SQL = """\
-- ---------------------------------------------------------------
-- Cleanup any prior run of this seed (safe across re-applications)
-- ---------------------------------------------------------------
DELETE FROM api_scenario_mapping
 WHERE scenario_id IN (SELECT id FROM test_scenario_master WHERE scenario_code = '{scenario_code}');
DELETE FROM test_scenario_master WHERE scenario_code = '{scenario_code}';
DELETE FROM api_request_templates
 WHERE api_id IN (SELECT api_id FROM api_master WHERE api_name LIKE '{prefix}%');
DELETE FROM api_headers
 WHERE api_id IN (SELECT api_id FROM api_master WHERE api_name LIKE '{prefix}%');
DELETE FROM api_master WHERE api_name LIKE '{prefix}%';
DELETE FROM product_module_mapping
 WHERE product_id = (SELECT id FROM loan_product_master WHERE product_code = '{product}')
   AND module_code NOT IN (SELECT module_code FROM product_module_mapping
                            WHERE product_id = (SELECT id FROM loan_product_master WHERE product_code = '{product}')
                              AND sequence_no <= 200
                              AND module_code IN ('QDE','ES','HHIE','AD','DDE','GFM','GCSA','SBET','ABET','RSBET','CBET','CUWRTR','HFCM','CMD','DOTAC','DACVM','ESGN','PLD','PSGN','CPDC_M'));
"""


def emit_sql(samplers: List[Dict], modules: "OrderedDict[str, str]",
             product: str, scenario_code: str, scenario_name: str,
             api_prefix: str) -> str:
    out: List[str] = []
    out.append(HEADER_DOC.format(samplers=len(samplers), modules=len(modules),
                                  product=product, scenario_code=scenario_code))
    out.append(CLEANUP_SQL.format(scenario_code=scenario_code, prefix=api_prefix, product=product))

    # Module mappings.
    out.append("\n-- Modules derived from JMeter TransactionControllers")
    for seq, (code, display) in enumerate(modules.items(), start=1):
        out.append(
            "INSERT INTO product_module_mapping (product_id, module_code, module_name, sequence_no, active) "
            "SELECT id, {code}, {name}, {seq}, TRUE FROM loan_product_master WHERE product_code = {product} "
            "ON CONFLICT (product_id, module_code) DO UPDATE SET module_name = EXCLUDED.module_name, "
            "sequence_no = EXCLUDED.sequence_no, active = TRUE;".format(
                code=sql_escape(code), name=sql_escape(display[:250]),
                seq=seq * 10, product=sql_escape(product)))

    # Scenario.
    out.append("")
    out.append(
        "INSERT INTO test_scenario_master (scenario_code, scenario_name, module_code, description, active, cross_module) "
        "VALUES ({code}, {name}, {module}, {desc}, TRUE, TRUE);".format(
            code=sql_escape(scenario_code), name=sql_escape(scenario_name),
            module=sql_escape(next(iter(modules.keys())) if modules else "DEFAULT"),
            desc=sql_escape("End-to-end JMX replay for SHG product")))

    # APIs.
    used_names: set = set()
    out.append("\n-- Per-sampler api_master + body + headers + scenario mapping")
    for sampler in samplers:
        api_name = api_prefix + safe_api_name(
            f"{sampler['execution_order']:04d}_{sampler['testname']}", used_names,
            sampler['execution_order'])
        sampler["api_name"] = api_name

        out.append(
            "INSERT INTO api_master (api_name, module_name, base_url, endpoint, http_method, "
            "auth_type, content_type, active) VALUES ({name}, {module}, {url}, {endp}, {meth}, "
            "'NONE', 'application/json', TRUE);".format(
                name=sql_escape(api_name), module=sql_escape(sampler["module_code"]),
                url=sql_escape(sampler["base_url"]),
                endp=sql_escape(sampler["endpoint"][:500]),
                meth=sql_escape(sampler["method"])))

        if sampler["body"]:
            body_clean = sampler["body"].replace("\r\n", "\n").replace("\r", "\n")
            # api_request_templates.request_template is JSONB; if body is not
            # valid JSON we wrap it as a JSON string so the insert succeeds.
            try:
                json.loads(body_clean)
                jsonb_literal = sql_escape(body_clean) + "::jsonb"
            except Exception:
                jsonb_literal = "to_jsonb(" + sql_escape(body_clean) + "::text)"
            out.append(
                "INSERT INTO api_request_templates (api_id, request_template) "
                "SELECT api_id, {body} FROM api_master WHERE api_name = {name};".format(
                    body=jsonb_literal, name=sql_escape(api_name)))

        for seq, (k, v) in enumerate(sampler["headers"].items(), start=1):
            is_dyn = "TRUE" if "${" in (v or "") else "FALSE"
            out.append(
                "INSERT INTO api_headers (api_id, header_key, header_value, is_dynamic, sequence_no) "
                "SELECT api_id, {k}, {v}, {dyn}, {seq} FROM api_master WHERE api_name = {name};".format(
                    k=sql_escape(k[:200]), v=sql_escape((v or "")[:500]),
                    dyn=is_dyn, seq=seq, name=sql_escape(api_name)))

        extraction_json = json.dumps(sampler["extractions"]) if sampler["extractions"] else None
        loop = sampler["loop_count"]
        out.append(
            "INSERT INTO api_scenario_mapping (api_id, scenario_id, product_id, execution_order, "
            "loop_count, delay_ms, extraction_mappings, active) "
            "SELECT a.api_id, s.id, p.id, {ord}, {loop}, 0, {extr}, TRUE "
            "FROM api_master a, test_scenario_master s, loan_product_master p "
            "WHERE a.api_name = {name} AND s.scenario_code = {scen} AND p.product_code = {prod};".format(
                ord=sampler["execution_order"], loop=loop,
                extr=(sql_escape(extraction_json) + "::jsonb") if extraction_json else "NULL",
                name=sql_escape(api_name), scen=sql_escape(scenario_code),
                prod=sql_escape(product)))

    return "\n".join(out) + "\n"


def harvest_user_constants(root: ET.Element) -> None:
    """Pull every <elementProp elementType='Argument'> with a non-empty value
    into JMX_CONSTANTS so URLs / IDs / login codes resolve at parse time
    instead of leaking through as ${CTX:...} that nothing populates."""
    for ap in root.iter("elementProp"):
        if ap.get("elementType") != "Argument":
            continue
        name = None
        value = None
        for c in ap:
            if c.get("name") == "Argument.name":
                name = (c.text or "").strip()
            elif c.get("name") == "Argument.value":
                value = c.text or ""
        if not name:
            continue
        # Skip image-blob constants (huge embedded base64 strings).
        if value and len(value) > 4096:
            continue
        # Don't override an explicit URL/path constant that was already typed in.
        if name in JMX_CONSTANTS and not JMX_CONSTANTS[name]:
            JMX_CONSTANTS[name] = (value or "").strip()
        elif name not in JMX_CONSTANTS and value is not None:
            JMX_CONSTANTS[name] = value.strip()


def main(argv: List[str]) -> int:
    if len(argv) != 4:
        print("usage: jmx_to_sql.py <input.jmx> <product_code> <out.sql>", file=sys.stderr)
        return 2

    jmx_path = Path(argv[1])
    product = argv[2]
    out_path = Path(argv[3])

    tree = ET.parse(jmx_path)
    root = tree.getroot()  # <jmeterTestPlan>
    harvest_user_constants(root)

    # JMeter root has hashTree -> TestPlan, hashTree (children of TestPlan)
    top_hash = root.find("hashTree")
    if top_hash is None:
        print("Malformed JMX: no top-level hashTree", file=sys.stderr)
        return 1

    walker = Walker()
    walker.walk(top_hash, [], 1, None, None, True, OrderedDict())

    if not walker.samplers:
        print("No enabled HTTP samplers found in JMX", file=sys.stderr)
        return 1

    sql = emit_sql(
        samplers=walker.samplers,
        modules=walker.modules,
        product=product,
        scenario_code=f"{product}_E2E_FLOW",
        scenario_name=f"{product} end-to-end JMX replay",
        api_prefix=f"jmx_{product.lower()}_",
    )
    out_path.write_text(sql)
    print(
        f"OK: wrote {len(walker.samplers)} samplers across {len(walker.modules)} modules to {out_path}",
        file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
