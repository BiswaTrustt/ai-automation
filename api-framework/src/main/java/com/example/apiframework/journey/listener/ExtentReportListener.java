package com.example.apiframework.journey.listener;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.example.apiframework.journey.dto.ApiStepResult;
import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.dto.ValidationOutcome;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * TestNG listener that opens an ExtentReports HTML report at {@code reports/extent-*.html}
 * and adds a section per JourneyResult exposed via TestNG attribute "journeyResult".
 */
@Slf4j
public class ExtentReportListener implements ITestListener {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ExtentReports extent;

    @Override
    public void onStart(ITestContext context) {
        try {
            Files.createDirectories(Paths.get("reports"));
        } catch (Exception ignore) { }
        String path = "reports/extent-" + java.time.LocalDateTime.now().format(TS) + ".html";
        ExtentSparkReporter spark = new ExtentSparkReporter(path);
        spark.config().setDocumentTitle("API Journey Report");
        spark.config().setReportName("API Journey Automation");
        extent = new ExtentReports();
        extent.attachReporter(spark);
        log.info("Extent report: {}", path);
    }

    @Override
    public void onTestSuccess(ITestResult result) { record(result); }

    @Override
    public void onTestFailure(ITestResult result) { record(result); }

    @Override
    public void onTestSkipped(ITestResult result) { record(result); }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) extent.flush();
    }

    private void record(ITestResult result) {
        Object attr = result.getAttribute("journeyResult");
        if (extent == null || !(attr instanceof JourneyResult jr)) return;

        ExtentTest test = extent.createTest(jr.getModuleCode() + " / " + jr.getScenarioCode());
        test.info("Run ID: " + jr.getJourneyRunId());
        test.info("Total: " + jr.getTotalDurationMs() + " ms");

        for (ApiStepResult s : jr.getSteps()) {
            ExtentTest node = test.createNode(s.getApiName() + " (#" + s.getExecutionOrder() + ")");
            node.info("HTTP " + s.getHttpStatusCode() + " in " + s.getExecutionTimeMs() + " ms");
            for (ValidationOutcome v : s.getDbValidations())  appendValidation(node, "DB", v);
            for (ValidationOutcome v : s.getCsvValidations()) appendValidation(node, "CSV", v);
            if (s.getErrorMessage() != null) node.fail(s.getErrorMessage());
            switch (s.getStatus()) {
                case PASS    -> node.pass("PASS");
                case FAIL    -> node.fail("FAIL");
                case ERROR   -> node.fail("ERROR");
                case SKIPPED -> node.skip("SKIPPED");
            }
        }
        switch (jr.getStatus()) {
            case PASS    -> test.log(Status.PASS, "Journey passed");
            case FAIL    -> test.log(Status.FAIL, "Journey failed");
            case ERROR   -> test.log(Status.FAIL, "Journey errored");
            case SKIPPED -> test.log(Status.SKIP, "Journey skipped");
        }
    }

    private void appendValidation(ExtentTest node, String label, ValidationOutcome v) {
        String line = String.format("[%s] %s: expected=%s, actual=%s",
                label, v.getValidationKey(), v.getExpectedValue(), v.getActualValue());
        if (v.isPassed()) node.pass(line); else node.fail(line);
    }
}
