package com.insurance.tests.db;

import com.insurance.base.ApiBase;
import com.insurance.utils.ApiClient;
import com.insurance.utils.DbUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** SQL checks: joins, aggregation with GROUP BY/HAVING, and business-rule invariants. Data is created through the API. */
public class DbValidationTests extends ApiBase {

    @Test(groups = "db", description = "INNER and LEFT JOIN across customers, quotes, policies and claims")
    public void joinAcrossAllTables_matchesWhatTheApiCreated() {
        long customerId = ApiClient.newCustomer(35);
        long quoteId = ApiClient.newQuote(customerId, "HEALTH", 100000);
        long policyId = ApiClient.issuePolicy(quoteId).then().statusCode(201).extract().jsonPath().getLong("policyId");
        long claimId = ApiClient.submitClaim(policyId, 20000).then().statusCode(201).extract().jsonPath().getLong("claimId");

        List<Map<String, Object>> rows = DbUtils.query(
                "SELECT c.full_name, q.coverage_type, q.premium, p.status AS policy_status, "
                        + "cl.id AS claim_id, cl.amount, cl.status AS claim_status "
                        + "FROM customers c "
                        + "JOIN quotes q ON q.customer_id = c.id "
                        + "JOIN policies p ON p.quote_id = q.id "
                        + "LEFT JOIN claims cl ON cl.policy_id = p.id "
                        + "WHERE c.id = ?", customerId);

        Assert.assertEquals(rows.size(), 1, "One customer, one quote, one policy, one claim -> one joined row");
        Map<String, Object> row = rows.get(0);
        Assert.assertEquals(row.get("full_name"), "Test User");
        Assert.assertEquals(row.get("coverage_type"), "HEALTH");
        Assert.assertEquals(((BigDecimal) row.get("premium")).compareTo(new BigDecimal("2000.00")), 0);
        Assert.assertEquals(row.get("policy_status"), "ISSUED");
        Assert.assertEquals(((Number) row.get("claim_id")).longValue(), claimId);
        Assert.assertEquals(row.get("claim_status"), "APPROVED");
    }

    @Test(groups = "db", description = "GROUP BY / HAVING: claims per policy agree with the API report")
    public void aggregation_claimsPerPolicyAgreesWithApiReport() {
        long policyId = ApiClient.newIssuedPolicy(200000);
        ApiClient.submitClaim(policyId, 10000).then().statusCode(201);
        ApiClient.submitClaim(policyId, 15000).then().statusCode(201);

        List<Map<String, Object>> rows = DbUtils.query(
                "SELECT policy_id, COUNT(*) AS claim_count, SUM(amount) AS total "
                        + "FROM claims WHERE policy_id = ? GROUP BY policy_id HAVING COUNT(*) > 1", policyId);
        Assert.assertEquals(rows.size(), 1, "HAVING COUNT(*) > 1 keeps this policy");
        Assert.assertEquals(((Number) rows.get(0).get("claim_count")).intValue(), 2);
        Assert.assertEquals(((BigDecimal) rows.get(0).get("total")).compareTo(new BigDecimal("25000")), 0);

        List<Map<String, Object>> report = ApiClient.get("/api/reports/claims-summary").then().statusCode(200)
                .extract().jsonPath().getList("findAll { it.policyId == " + policyId + " }");
        Assert.assertEquals(((Number) report.get(0).get("claimCount")).intValue(), 2);
    }

    @Test(groups = "db", description = "Invariants that must hold for ALL data: no orphans, no ISSUED policy for ineligible age")
    public void businessRuleInvariants_holdForAllRows() {
        // make sure both an ISSUED and a REJECTED policy exist before checking the rules
        ApiClient.newIssuedPolicy(100000);
        long minorQuote = ApiClient.newQuote(ApiClient.newCustomer(17), "HOME", 100000);
        ApiClient.issuePolicy(minorQuote).then().statusCode(201);

        List<Map<String, Object>> orphans = DbUtils.query(
                "SELECT p.id FROM policies p LEFT JOIN quotes q ON q.id = p.quote_id WHERE q.id IS NULL");
        Assert.assertTrue(orphans.isEmpty(), "Policies without a quote: " + orphans);

        List<Map<String, Object>> invalidIssued = DbUtils.query(
                "SELECT p.id, c.dob FROM policies p JOIN customers c ON c.id = p.customer_id "
                        + "WHERE p.status = 'ISSUED' "
                        + "AND (c.dob > DATEADD(YEAR, -18, CURRENT_DATE) OR c.dob <= DATEADD(YEAR, -76, CURRENT_DATE))");
        Assert.assertTrue(invalidIssued.isEmpty(), "ISSUED policies for customers outside age 18-75: " + invalidIssued);

        long approvedOverLimit = DbUtils.count(
                "SELECT COUNT(*) FROM claims WHERE status = 'APPROVED' AND amount > 50000");
        Assert.assertEquals(approvedOverLimit, 0L, "Claims above 50000 must never be auto-approved");
    }
}
