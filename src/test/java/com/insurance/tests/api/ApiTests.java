package com.insurance.tests.api;

import com.insurance.base.ApiBase;
import com.insurance.utils.ApiClient;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/** REST Assured tests: HTTP methods, status codes, JSON validation and business rules of the insurance API. */
public class ApiTests extends ApiBase {

    @Test(groups = {"smoke", "api"})
    public void health_returns200AndUp() {
        ApiClient.get("/api/health").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test(groups = "api", description = "POST creates (201), GET reads (200), unknown id gives 404")
    public void customer_createFetchAndUnknownId() {
        Map<String, Object> body = ApiClient.customerBody(35);
        Response created = ApiClient.post("/api/customers", body);
        created.then().statusCode(201)
                .body("id", notNullValue())
                .body("fullName", equalTo("Test User"))
                .body("email", equalTo(body.get("email")));

        long id = created.jsonPath().getLong("id");
        ApiClient.get("/api/customers/" + id).then().statusCode(200)
                .body("email", equalTo(body.get("email")))
                .body("state", equalTo("Tamil Nadu"));

        ApiClient.get("/api/customers/999999").then().statusCode(404).body("error", containsString("not found"));
    }

    @Test(groups = "api", description = "Unique email rule gives 409 Conflict")
    public void customer_duplicateEmail_returns409() {
        Map<String, Object> body = ApiClient.customerBody(30);
        ApiClient.post("/api/customers", body).then().statusCode(201);
        ApiClient.post("/api/customers", body).then().statusCode(409).body("error", equalTo("Email already registered"));
    }

    @DataProvider(name = "invalidCustomers")
    public Object[][] invalidCustomers() {
        return new Object[][]{
                {"fullName", "", "fullName is required"},
                {"email", "not-an-email", "A valid email is required"},
                {"dob", LocalDate.now().plusDays(5).toString(), "dob must be in the past"}
        };
    }

    @Test(groups = "api", dataProvider = "invalidCustomers", description = "Bad input gives 400 with a clear message")
    public void customer_invalidInput_returns400(String field, Object badValue, String expectedError) {
        Map<String, Object> body = ApiClient.customerBody(30);
        body.put(field, badValue);
        ApiClient.post("/api/customers", body).then().statusCode(400).body("error", equalTo(expectedError));
    }

    @DataProvider(name = "premiums")
    public Object[][] premiums() {
        // age, product, coverage, expected premium (coverage x rate x age factor)
        return new Object[][]{
                {35, "HEALTH", 100000.0, 2000.00},
                {60, "AUTO", 50000.0, 1950.00},
                {22, "HOME", 200000.0, 3600.00}
        };
    }

    @Test(groups = "api", dataProvider = "premiums", description = "Premium calculation per product and age band")
    public void quote_premiumIsCalculatedCorrectly(int age, String type, double coverage, double expectedPremium) {
        long customerId = ApiClient.newCustomer(age);
        long quoteId = ApiClient.newQuote(customerId, type, coverage);
        double premium = ApiClient.get("/api/quotes/" + quoteId).then().statusCode(200)
                .extract().jsonPath().getDouble("premium");
        Assert.assertEquals(premium, expectedPremium, 0.001);
    }

    @DataProvider(name = "underwriting")
    public Object[][] underwriting() {
        // age, coverage, expected status, expected reason fragment
        return new Object[][]{
                {35, 100000.0, "ISSUED", "Underwriting passed"},
                {17, 100000.0, "REJECTED", "below minimum age"},
                {80, 100000.0, "REJECTED", "above maximum age"},
                {35, 2000000.0, "REJECTED", "underwriting limit"}
        };
    }

    @Test(groups = "api", dataProvider = "underwriting", description = "Underwriting rules: issue or reject")
    public void policy_underwritingRules(int age, double coverage, String expectedStatus, String reasonFragment) {
        long quoteId = ApiClient.newQuote(ApiClient.newCustomer(age), "HEALTH", coverage);
        ApiClient.issuePolicy(quoteId).then().statusCode(201)
                .body("status", equalTo(expectedStatus))
                .body("reason", containsString(reasonFragment));
    }

    @Test(groups = "api", description = "A quote can become a policy only once")
    public void policy_secondPolicyForSameQuote_returns409() {
        long quoteId = ApiClient.newQuote(ApiClient.newCustomer(35), "HEALTH", 100000);
        ApiClient.issuePolicy(quoteId).then().statusCode(201);
        ApiClient.issuePolicy(quoteId).then().statusCode(409);
    }

    @DataProvider(name = "claims")
    public Object[][] claims() {
        // claim amount against a policy with coverage 200000 -> expected status (includes boundary values)
        return new Object[][]{
                {20000.0, "APPROVED"},
                {50000.0, "APPROVED"},
                {50001.0, "UNDER_REVIEW"},
                {250000.0, "REJECTED"}
        };
    }

    @Test(groups = "api", dataProvider = "claims", description = "Claim decision rules with boundary values")
    public void claim_decisionRules(double amount, String expectedStatus) {
        long policyId = ApiClient.newIssuedPolicy(200000);
        Response created = ApiClient.submitClaim(policyId, amount);
        created.then().statusCode(201).body("status", equalTo(expectedStatus));

        long claimId = created.jsonPath().getLong("claimId");
        ApiClient.get("/api/claims/" + claimId).then().statusCode(200).body("status", equalTo(expectedStatus));
    }

    @Test(groups = "api", description = "PUT cancels a policy; a cancelled policy cannot be claimed on")
    public void policy_cancelThenClaimIsRejected() {
        long policyId = ApiClient.newIssuedPolicy(100000);

        ApiClient.put("/api/policies/" + policyId + "/cancel").then().statusCode(200).body("status", equalTo("CANCELLED"));
        ApiClient.put("/api/policies/" + policyId + "/cancel").then().statusCode(409);

        ApiClient.submitClaim(policyId, 1000).then().statusCode(201)
                .body("status", equalTo("REJECTED"))
                .body("reason", containsString("not active"));
        ApiClient.submitClaim(policyId, -5).then().statusCode(400);
    }

    @Test(groups = "api", description = "DELETE: 204 when free, 409 when the customer has a quote")
    public void customer_deleteRules() {
        long free = ApiClient.newCustomer(40);
        ApiClient.delete("/api/customers/" + free).then().statusCode(204);
        ApiClient.get("/api/customers/" + free).then().statusCode(404);

        long inUse = ApiClient.newCustomer(40);
        ApiClient.newQuote(inUse, "AUTO", 50000);
        ApiClient.delete("/api/customers/" + inUse).then().statusCode(409);
    }

    @Test(groups = "api", description = "JSON array report built from a SQL join")
    public void report_claimsSummaryMatchesSubmittedClaims() {
        long policyId = ApiClient.newIssuedPolicy(200000);
        ApiClient.submitClaim(policyId, 20000).then().statusCode(201);
        ApiClient.submitClaim(policyId, 30000).then().statusCode(201);

        JsonPath report = ApiClient.get("/api/reports/claims-summary").then().statusCode(200).extract().jsonPath();
        List<Map<String, Object>> rows = report.getList("findAll { it.policyId == " + policyId + " }");
        Assert.assertEquals(rows.size(), 1, "Exactly one report row per policy");
        Assert.assertEquals(((Number) rows.get(0).get("claimCount")).intValue(), 2);
        Assert.assertEquals(((Number) rows.get(0).get("totalClaimed")).doubleValue(), 50000.0, 0.001);
        Assert.assertEquals(rows.get(0).get("policyStatus"), "ISSUED");
    }
}
