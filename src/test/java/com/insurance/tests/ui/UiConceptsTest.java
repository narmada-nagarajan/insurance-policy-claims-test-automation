package com.insurance.tests.ui;

import com.insurance.base.BaseTest;
import com.insurance.pages.ClaimPage;
import com.insurance.pages.CustomerPage;
import com.insurance.pages.QuotePage;
import com.insurance.utils.ApiClient;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;

/** Focused Selenium concept tests. Test data is created through the API so each test starts on the page it practises. */
public class UiConceptsTest extends BaseTest {

    @Test(groups = "regression", description = "Dropdown, iframe, new window, and form validation on the customer page")
    public void customerForm_handlesDropdownFrameAndWindow() {
        CustomerPage page = new CustomerPage(driver).open();

        // New window: switch to it, read it, close it, come back to the parent
        Assert.assertEquals(page.readPrivacyHeadingFromNewWindow(), "Privacy Policy");
        Assert.assertEquals(driver.getTitle(), "Customer Details", "Back on the parent window");

        // Dropdown
        page.enterDetails("Divya Menon", ApiClient.customerBody(40).get("dob").toString(),
                "divya." + UUID.randomUUID().toString().substring(0, 8) + "@example.com", "9876500009", "Kerala");
        Assert.assertEquals(page.selectedState(), "Kerala");

        // Validation: submitting without accepting the terms (checkbox is inside the iframe) is blocked
        Assert.assertEquals(page.save().message(), "Please accept the terms and conditions");

        // iframe: switch in, tick, switch out, then the save succeeds
        // The old validation message is still on screen, so wait for the NEW text instead of reading immediately.
        page.acceptTerms().save().waitForMessageContaining("Customer saved successfully");
        Assert.assertTrue(page.message().startsWith("Customer saved successfully"), page.message());
    }

    @Test(groups = "regression", description = "Explicit wait handles a slow, asynchronous premium calculation")
    public void quote_explicitWaitHandlesSlowPremium() {
        long customerId = ApiClient.newCustomer(35);
        QuotePage page = new QuotePage(driver).open(customerId)
                .selectCoverageType("HEALTH").enterCoverageAmount("100000").calculate();

        // Right after the click the premium is NOT there yet (the app takes about a second) ...
        Assert.assertFalse(page.isPremiumDisplayed(), "Premium should appear only after the calculation");
        // ... the explicit wait returns the moment it shows up, with no Thread.sleep.
        Assert.assertEquals(page.premium(), "2000.00");
        page.waitForLoaderToDisappear();
    }

    @Test(groups = "regression", description = "A re-rendered table makes old element references stale")
    public void claimsTable_staleElementIsRefound() {
        long policyId = ApiClient.newIssuedPolicy(200000);
        ClaimPage page = new ClaimPage(driver).open(policyId);

        page.submitClaim("1000", "First claim").waitForRowCount(1);
        WebElement oldRow = page.firstRowElement();

        page.submitClaim("2000", "Second claim").waitForRowCount(2);

        // The page rebuilt the table body, so the reference we kept now points at a removed element.
        Assert.expectThrows(StaleElementReferenceException.class, oldRow::getText);

        // The fix: never keep element references across a page update - locate again.
        Assert.assertEquals(page.allRowTexts().size(), 2);
        Assert.assertEquals(page.statusInTable(page.claimId()), "APPROVED");
    }
}
