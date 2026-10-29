package com.insurance.pages;

import com.insurance.utils.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

/** Step 4 - submit a claim; the claims table is rebuilt after every submit (stale-element practice). */
public class ClaimPage extends BasePage {

    private static final By AMOUNT = By.id("claimAmount");
    private static final By DESCRIPTION = By.id("claimDescription");
    private static final By SUBMIT = By.id("submitClaim");
    private static final By CLAIM_ID = By.id("claimId");
    private static final By CLAIM_STATUS = By.id("claimStatus");
    private static final By CLAIM_MESSAGE = By.id("claimMessage");
    private static final By ERROR_MESSAGE = By.id("msg");
    private static final By ROWS = By.cssSelector("#claimsTable tbody tr");

    public ClaimPage(WebDriver driver) {
        super(driver);
    }

    public ClaimPage open(long policyId) {
        driver.get(Config.baseUrl() + "/claim.html?policyId=" + policyId);
        return waitUntilLoaded();
    }

    public ClaimPage waitUntilLoaded() {
        visible(AMOUNT);
        return this;
    }

    public ClaimPage submitClaim(String amount, String description) {
        type(AMOUNT, amount);
        type(DESCRIPTION, description);
        click(SUBMIT);
        return this;
    }

    public String claimId() {
        return text(CLAIM_ID);
    }

    public String claimStatus() {
        return text(CLAIM_STATUS);
    }

    public String claimMessage() {
        return text(CLAIM_MESSAGE);
    }

    public String errorMessage() {
        return nonEmptyText(ERROR_MESSAGE);
    }

    /** Waits for the table to reach the expected size (a re-find on every poll, so it is immune to stale rows). */
    public ClaimPage waitForRowCount(int expected) {
        wait.until(ExpectedConditions.numberOfElementsToBe(ROWS, expected));
        return this;
    }

    /** Raw element reference, exposed only so a test can demonstrate StaleElementReferenceException. */
    public org.openqa.selenium.WebElement firstRowElement() {
        return driver.findElement(ROWS);
    }

    /** Relative XPath: find the row whose first cell is the claim id, return its 3rd cell (status). */
    public String statusInTable(String claimId) {
        return text(By.xpath("//table[@id='claimsTable']//tr[td[1][normalize-space()='" + claimId + "']]/td[3]"));
    }

    public List<String> allRowTexts() {
        return driver.findElements(ROWS).stream().map(e -> e.getText().trim()).collect(Collectors.toList());
    }
}
