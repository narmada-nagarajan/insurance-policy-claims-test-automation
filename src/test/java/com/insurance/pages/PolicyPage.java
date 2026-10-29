package com.insurance.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Step 3 - issue or reject the policy (underwriting rules run on the server). */
public class PolicyPage extends BasePage {

    private static final By ISSUE = By.id("issuePolicy");
    private static final By RESULT = By.id("policyResult");
    private static final By POLICY_ID = By.id("policyId");
    private static final By STATUS = By.id("policyStatus");
    private static final By MESSAGE = By.id("policyMessage");
    private static final By CONTINUE_TO_CLAIM = By.id("continueToClaim");

    public PolicyPage(WebDriver driver) {
        super(driver);
    }

    public PolicyPage waitUntilLoaded() {
        visible(ISSUE);
        return this;
    }

    /** Clicks and waits for the underwriting result panel to appear. */
    public PolicyPage issuePolicy() {
        click(ISSUE);
        visible(RESULT);
        return this;
    }

    public String policyId() {
        return text(POLICY_ID);
    }

    public String status() {
        return text(STATUS);
    }

    public String message() {
        return text(MESSAGE);
    }

    public boolean isClaimLinkDisplayed() {
        return isDisplayed(CONTINUE_TO_CLAIM);
    }

    public ClaimPage proceedToClaim() {
        click(CONTINUE_TO_CLAIM);
        return new ClaimPage(driver).waitUntilLoaded();
    }
}
