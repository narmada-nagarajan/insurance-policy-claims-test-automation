package com.insurance.base;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/** Parent of every UI test: starts the app once, then gives each test method its own fresh browser. */
public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void startApplication() {
        AppServer.ensureStarted();
    }

    @BeforeMethod(alwaysRun = true)
    public void openBrowser() {
        driver = DriverFactory.create();
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        DriverFactory.quit();
    }
}
