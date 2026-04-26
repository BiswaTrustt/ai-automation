package com.example.apiframework.journey.tests;

import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.enums.JourneyStatus;
import com.example.apiframework.journey.executor.JourneyExecutor;
import com.example.apiframework.journey.listener.ExtentReportListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@SpringBootTest
@Listeners(ExtentReportListener.class)
public class QdeJourneyTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private JourneyExecutor journeyExecutor;

    private JourneyResult lastResult;

    @Test(description = "Legacy 2-arg flow: module + scenario")
    public void executeQdeValidCustomer() {
        lastResult = journeyExecutor.execute("QDE", "VALID_CUSTOMER");
        Assert.assertEquals(lastResult.getStatus(), JourneyStatus.PASS,
                "QDE / VALID_CUSTOMER (no product) journey did not pass");
    }

    @Test(description = "Product-aware 3-arg flow: JLG + QDE + VALID_CUSTOMER")
    public void executeJlgQdeValidCustomer() {
        lastResult = journeyExecutor.execute("JLG", "QDE", "VALID_CUSTOMER");
        Assert.assertEquals(lastResult.getLoanProductCode(), "JLG");
        Assert.assertEquals(lastResult.getStatus(), JourneyStatus.PASS,
                "JLG / QDE / VALID_CUSTOMER journey did not pass");
    }

    @Test(description = "Product-aware 3-arg flow: SHG runs a different sequence")
    public void executeShgQdeValidCustomer() {
        lastResult = journeyExecutor.execute("SHG", "QDE", "VALID_CUSTOMER");
        Assert.assertEquals(lastResult.getLoanProductCode(), "SHG");
        Assert.assertEquals(lastResult.getStatus(), JourneyStatus.PASS,
                "SHG / QDE / VALID_CUSTOMER journey did not pass");
        Assert.assertEquals(lastResult.getSteps().size(), 1,
                "SHG sequence should be a single API per V6 seed");
    }

    @AfterMethod
    public void publishToListener(ITestResult result) {
        if (lastResult != null) {
            result.setAttribute("journeyResult", lastResult);
        }
    }
}
