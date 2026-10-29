package com.insurance.pages;

import com.insurance.utils.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Step 2 - quote: dropdown plus a premium that only appears after a simulated slow calculation. */
public class QuotePage extends BasePage {

    private static final By COVERAGE_TYPE = By.id("coverageType");
    private static final By COVERAGE_AMOUNT = By.cssSelector("input#coverageAmount");
    private static final By CALCULATE = By.id("calculate");
    private static final By LOADER = By.id("loader");
    private static final By PREMIUM = By.id("premium");
    private static final By QUOTE_ID = By.id("quoteId");
    private static final By MESSAGE = By.id("msg");
    private static final By CONTINUE = By.id("continueToPolicy");

    public QuotePage(WebDriver driver) {
        super(driver);
    }

    public QuotePage open(long customerId) {
        driver.get(Config.baseUrl() + "/quote.html?customerId=" + customerId);
        return waitUntilLoaded();
    }

    public QuotePage waitUntilLoaded() {
        visible(COVERAGE_TYPE);
        return this;
    }

    public QuotePage selectCoverageType(String value) {
        selectByValue(COVERAGE_TYPE, value);
        return this;
    }

    public QuotePage enterCoverageAmount(String amount) {
        type(COVERAGE_AMOUNT, amount);
        return this;
    }

    public QuotePage calculate() {
        click(CALCULATE);
        return this;
    }

    public boolean isPremiumDisplayed() {
        return isDisplayed(PREMIUM);
    }

    /** Explicit wait: returns as soon as the premium is shown, however long the calculation takes (up to the timeout). */
    public String premium() {
        return text(PREMIUM);
    }

    public void waitForLoaderToDisappear() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
    }

    public String quoteId() {
        return text(QUOTE_ID);
    }

    public String validationMessage() {
        return nonEmptyText(MESSAGE);
    }

    public PolicyPage proceedToPolicy() {
        click(CONTINUE);
        return new PolicyPage(driver).waitUntilLoaded();
    }
}
