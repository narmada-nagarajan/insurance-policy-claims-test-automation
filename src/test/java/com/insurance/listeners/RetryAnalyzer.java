package com.insurance.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed UI test once (-Dretry=N to change). A retry is a safety net for environment blips, NOT a fix
 * for flaky tests: every test that passes only on retry must be logged and root-caused.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = Integer.getInteger("retry", 1);
    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempts < MAX_RETRIES) {
            attempts++;
            System.out.println("[RETRY] " + result.getMethod().getMethodName() + " attempt " + (attempts + 1));
            return true;
        }
        return false;
    }
}
