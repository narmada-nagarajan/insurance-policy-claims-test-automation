package com.insurance.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Thin REST Assured wrapper shared by API tests, UI business components (API validation) and test set-up. */
public final class ApiClient {

    private ApiClient() {
    }

    private static RequestSpecification spec() {
        return RestAssured.given().baseUri(Config.baseUrl()).contentType(ContentType.JSON).accept(ContentType.JSON);
    }

    public static Response get(String path) {
        return spec().get(path);
    }

    public static Response post(String path, Object body) {
        return spec().body(body).post(path);
    }

    public static Response put(String path) {
        return spec().put(path);
    }

    public static Response delete(String path) {
        return spec().delete(path);
    }

    // ---- request builders -------------------------------------------------

    /** A valid customer whose age is exactly {@code age} years, with a unique email. */
    public static Map<String, Object> customerBody(int age) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fullName", "Test User");
        m.put("dob", LocalDate.now().minusYears(age).minusDays(100).toString());
        m.put("email", "user." + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        m.put("phone", "9876543210");
        m.put("state", "Tamil Nadu");
        return m;
    }

    // ---- set-up helpers: create data fast through the API (not the UI) ------

    public static long newCustomer(int age) {
        return post("/api/customers", customerBody(age)).then().statusCode(201).extract().jsonPath().getLong("id");
    }

    public static long newQuote(long customerId, String type, double coverage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerId", customerId);
        body.put("coverageType", type);
        body.put("coverageAmount", coverage);
        return post("/api/quotes", body).then().statusCode(201).extract().jsonPath().getLong("quoteId");
    }

    public static Response issuePolicy(long quoteId) {
        return post("/api/policies", Map.of("quoteId", quoteId));
    }

    /** Customer aged 35 + HEALTH quote + policy; returns the policy id of an ISSUED policy. */
    public static long newIssuedPolicy(double coverage) {
        long quoteId = newQuote(newCustomer(35), "HEALTH", coverage);
        return issuePolicy(quoteId).then().statusCode(201).extract().jsonPath().getLong("policyId");
    }

    public static Response submitClaim(long policyId, double amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policyId", policyId);
        body.put("amount", amount);
        body.put("description", "Automated test claim");
        return post("/api/claims", body);
    }
}
