package com.insurance.base;

import org.testng.annotations.BeforeClass;

/** Parent of API and DB tests: no browser, just make sure the app is running. */
public abstract class ApiBase {

    @BeforeClass(alwaysRun = true)
    public void startApplication() {
        AppServer.ensureStarted();
    }
}
