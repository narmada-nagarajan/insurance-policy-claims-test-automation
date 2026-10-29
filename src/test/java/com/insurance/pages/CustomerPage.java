package com.insurance.pages;

import com.insurance.utils.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Step 1 - customer details: text fields, dropdown, iframe (terms) and a new browser window (privacy policy). */
public class CustomerPage extends BasePage {

    // Locator variety on purpose: name, id, linkText
    private static final By FULL_NAME = By.name("fullName");
    private static final By DOB = By.id("dob");
    private static final By EMAIL = By.id("email");
    private static final By PHONE = By.id("phone");
    private static final By STATE = By.id("state");
    private static final By TERMS_FRAME = By.id("termsFrame");
    private static final By ACCEPT_TERMS = By.id("acceptTerms");
    private static final By PRIVACY_LINK = By.linkText("View privacy policy");
    private static final By PRIVACY_HEADING = By.id("privacyHeading");
    private static final By SAVE = By.id("saveCustomer");
    private static final By MESSAGE = By.id("msg");
    private static final By CUSTOMER_ID = By.id("customerId");
    private static final By CONTINUE = By.id("continueToQuote");

    public CustomerPage(WebDriver driver) {
        super(driver);
    }

    public CustomerPage open() {
        driver.get(Config.baseUrl() + "/customer.html");
        visible(FULL_NAME);
        return this;
    }

    public CustomerPage enterDetails(String name, String dob, String email, String phone, String state) {
        type(FULL_NAME, name);
        type(DOB, dob);
        type(EMAIL, email);
        type(PHONE, phone);
        if (state != null && !state.isEmpty()) {
            selectByVisibleText(STATE, state);
        }
        return this;
    }

    public String selectedState() {
        return selectedText(STATE);
    }

    /** The checkbox lives inside an iframe: switch in, click, ALWAYS switch back to the main page. */
    public CustomerPage acceptTerms() {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(TERMS_FRAME));
        try {
            click(ACCEPT_TERMS);
        } finally {
            driver.switchTo().defaultContent();
        }
        return this;
    }

    public CustomerPage save() {
        click(SAVE);
        return this;
    }

    public String message() {
        return nonEmptyText(MESSAGE);
    }

    /** Synchronises on the NEW message when an earlier one (e.g. a validation error) is still on screen. */
    public CustomerPage waitForMessageContaining(String fragment) {
        waitForTextToContain(MESSAGE, fragment);
        return this;
    }

    public String customerId() {
        return text(CUSTOMER_ID);
    }

    /** The link opens a new window/tab: remember the parent, switch to the new handle, read, close, switch back. */
    public String readPrivacyHeadingFromNewWindow() {
        String parent = driver.getWindowHandle();
        click(PRIVACY_LINK);
        wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(parent)) {
                driver.switchTo().window(handle);
                break;
            }
        }
        String heading = text(PRIVACY_HEADING);
        driver.close();
        driver.switchTo().window(parent);
        return heading;
    }

    public QuotePage continueToQuote() {
        click(CONTINUE);
        return new QuotePage(driver).waitUntilLoaded();
    }
}
