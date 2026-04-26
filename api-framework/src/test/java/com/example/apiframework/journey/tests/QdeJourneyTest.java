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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@SpringBootTest
@Listeners(ExtentReportListener.class)
public class QdeJourneyTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private JourneyExecutor journeyExecutor;

    @Test
    public void executeQdeValidCustomer(ITestResult result) {
        JourneyResult jr = journeyExecutor.execute("QDE", "VALID_CUSTOMER");
        result.setAttribute("journeyResult", jr);
        Assert.assertEquals(jr.getStatus(), JourneyStatus.PASS,
                "QDE / VALID_CUSTOMER journey did not pass");
    }
}
