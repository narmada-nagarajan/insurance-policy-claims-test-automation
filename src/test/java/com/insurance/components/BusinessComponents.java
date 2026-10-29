package com.insurance.components;

import com.insurance.pages.CustomerPage;
import com.insurance.utils.ApiClient;
import com.insurance.utils.DbUtils;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The reusable "business components" (functional library) of the hybrid framework.
 * Each public method is one KEYWORD: it takes a data row, drives the pages, and records results in the context.
 * Keywords know nothing about each other except through ScenarioContext, so scenarios can mix and reorder them freely.
 */
public class BusinessComponents {

    private final ScenarioContext ctx;

    public BusinessComponents(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    /** Keyword CreateCustomer. Optional data column expectedMessage turns it into a negative (validation) check. */
    public void createCustomer(Map<String, String> d) {
        ctx.customerPage = new CustomerPage(ctx.driver).open()
                .enterDetails(d.get("fullName"), d.get("dob"), d.get("email"), d.get("phone"), d.get("state"));
        if (!"No".equalsIgnoreCase(value(d, "acceptTerms", "Yes"))) {
            ctx.customerPage.acceptTerms();
        }
        ctx.customerPage.save();

        String message = ctx.customerPage.message();
        String expected = value(d, "expectedMessage", "Customer saved successfully");
        Assert.assertTrue(message.contains(expected),
                "Customer form message was '" + message + "' but expected it to contain '" + expected + "'");

        if (message.startsWith("Customer saved")) {
            ctx.customerId = Long.parseLong(ctx.customerPage.customerId());
        } else {
            long created = DbUtils.count("SELECT COUNT(*) FROM customers WHERE email = ?", d.get("email"));
            Assert.assertEquals(created, 0L, "A rejected form must not create a customer record");
        }
    }

    /** Keyword GetQuote: choose coverage, calculate (waits for the slow premium) and check the premium. */
    public void getQuote(Map<String, String> d) {
        Assert.assertTrue(ctx.customerId > 0, "GetQuote needs a customer: run CreateCustomer first");
        ctx.quotePage = ctx.customerPage.continueToQuote()
                .selectCoverageType(d.get("coverageType"))
                .enterCoverageAmount(d.get("coverageAmount"))
                .calculate();
        ctx.premiumText = ctx.quotePage.premium();
        ctx.quoteId = Long.parseLong(ctx.quotePage.quoteId());
        if (!value(d, "expectedPremium", "").isEmpty()) {
            Assert.assertEquals(ctx.premiumText, d.get("expectedPremium"), "Premium shown on the UI");
        }
    }

    /** Keyword IssuePolicy: runs underwriting; ISSUED or REJECTED is verified later by ValidateOutcome. */
    public void issuePolicy(Map<String, String> d) {
        Assert.assertTrue(ctx.quoteId > 0, "IssuePolicy needs a quote: run GetQuote first");
        ctx.policyPage = ctx.quotePage.proceedToPolicy().issuePolicy();
        ctx.policyId = Long.parseLong(ctx.policyPage.policyId());
        ctx.policyStatus = ctx.policyPage.status();
        ctx.policyMessage = ctx.policyPage.message();
    }

    /** Keyword SubmitClaim: only possible when the policy was issued (the UI offers no claim link otherwise). */
    public void submitClaim(Map<String, String> d) {
        Assert.assertEquals(ctx.policyStatus, "ISSUED", "A claim can only be started for an ISSUED policy");
        ctx.claimPage = ctx.policyPage.proceedToClaim().submitClaim(d.get("amount"), d.get("description"));
        ctx.claimId = Long.parseLong(ctx.claimPage.claimId());
        ctx.claimStatus = ctx.claimPage.claimStatus();
        ctx.claimMessage = ctx.claimPage.claimMessage();
    }

    /**
     * Keyword ValidateOutcome: the same outcome checked in three layers - UI messages, API responses, DB rows.
     * SoftAssert reports ALL mismatches at once instead of stopping at the first.
     */
    public void validateOutcome(Map<String, String> d) {
        SoftAssert soft = new SoftAssert();
        String expectedPolicy = d.get("policyStatus");
        String expectedClaim = value(d, "claimStatus", "");

        // 1. UI
        soft.assertEquals(ctx.policyStatus, expectedPolicy, "UI policy status");
        soft.assertTrue(ctx.policyMessage.contains(d.get("policyMessage")), "UI policy message: " + ctx.policyMessage);
        if (!expectedClaim.isEmpty()) {
            soft.assertEquals(ctx.claimStatus, expectedClaim, "UI claim status");
            soft.assertTrue(ctx.claimMessage.contains(d.get("claimMessage")), "UI claim message: " + ctx.claimMessage);
        }

        // 2. API
        JsonPath policy = ApiClient.get("/api/policies/" + ctx.policyId).then().statusCode(200).extract().jsonPath();
        soft.assertEquals(policy.getString("status"), expectedPolicy, "API policy status");
        if (!expectedClaim.isEmpty()) {
            JsonPath claim = ApiClient.get("/api/claims/" + ctx.claimId).then().statusCode(200).extract().jsonPath();
            soft.assertEquals(claim.getString("status"), expectedClaim, "API claim status");
        }

        // 3. Database
        soft.assertEquals(DbUtils.single("SELECT status FROM policies WHERE id = ?", ctx.policyId), expectedPolicy,
                "DB policy status");
        BigDecimal dbPremium = (BigDecimal) DbUtils.single("SELECT premium FROM quotes WHERE id = ?", ctx.quoteId);
        soft.assertEquals(dbPremium.compareTo(new BigDecimal(ctx.premiumText)), 0, "DB premium vs UI premium");
        if (!expectedClaim.isEmpty()) {
            soft.assertEquals(DbUtils.single("SELECT status FROM claims WHERE id = ?", ctx.claimId), expectedClaim,
                    "DB claim status");
        }
        soft.assertAll();
    }

    /** A data cell that is missing or empty falls back to a default. */
    private static String value(Map<String, String> data, String key, String fallback) {
        String v = data.get(key);
        return v == null || v.isEmpty() ? fallback : v;
    }
}
