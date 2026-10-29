package com.insurance.app;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * The application under test: a small insurance system (static UI + REST API + H2 database)
 * served by the JDK's built-in HTTP server. Start it with {@link #start(int)}; the test suite
 * does this in a @BeforeSuite so no external setup is needed.
 */
public class InsuranceApp {

    private static final Gson GSON = new Gson();
    private HttpServer server;

    /** Maps to an HTTP error response. */
    static class ApiException extends RuntimeException {
        final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    public void start(int port) throws IOException {
        Db.reset();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", this::api);
        server.createContext("/", this::staticFile);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.getInteger("app.port", 8089);
        new InsuranceApp().start(port);
        System.out.println("Insurance app running at http://localhost:" + port + "/");
    }

    // ------------------------------------------------------------------ routing

    private void api(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String[] p = ex.getRequestURI().getPath().substring(5).split("/");
            switch (p[0]) {
                case "health" -> send(ex, 200, Map.of("status", "UP"));
                case "customers" -> customers(ex, method, p);
                case "quotes" -> quotes(ex, method, p);
                case "policies" -> policies(ex, method, p);
                case "claims" -> claims(ex, method, p);
                case "reports" -> reports(ex, method, p);
                default -> throw new ApiException(404, "Unknown resource: " + p[0]);
            }
        } catch (ApiException e) {
            send(ex, e.status, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            send(ex, 500, Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    // ---------------------------------------------------------------- customers

    private void customers(HttpExchange ex, String method, String[] p) throws IOException {
        if (p.length == 1 && method.equals("POST")) {
            Map<String, Object> b = body(ex);
            String name = str(b, "fullName");
            String email = str(b, "email");
            String dobText = str(b, "dob");
            if (name == null || name.isEmpty()) {
                throw new ApiException(400, "fullName is required");
            }
            if (email == null || !email.contains("@")) {
                throw new ApiException(400, "A valid email is required");
            }
            LocalDate dob = parseDob(dobText);
            try {
                long id = Db.insert("INSERT INTO customers(full_name, dob, email, phone, state) VALUES (?,?,?,?,?)",
                        name, java.sql.Date.valueOf(dob), email, str(b, "phone"), str(b, "state"));
                send(ex, 201, customerJson(findCustomer(id)));
            } catch (Db.DbException e) {
                if (e.isDuplicate()) {
                    throw new ApiException(409, "Email already registered");
                }
                throw e;
            }
        } else if (p.length == 2 && method.equals("GET")) {
            send(ex, 200, customerJson(findCustomer(id(p[1]))));
        } else if (p.length == 2 && method.equals("DELETE")) {
            long id = id(p[1]);
            findCustomer(id);
            if (!Db.query("SELECT id FROM quotes WHERE customer_id = ?", id).isEmpty()) {
                throw new ApiException(409, "Customer has quotes or policies and cannot be deleted");
            }
            Db.update("DELETE FROM customers WHERE id = ?", id);
            ex.sendResponseHeaders(204, -1);
            ex.close();
        } else {
            throw new ApiException(405, "Method not allowed");
        }
    }

    private LocalDate parseDob(String dobText) {
        if (dobText == null) {
            throw new ApiException(400, "dob is required (yyyy-MM-dd)");
        }
        try {
            LocalDate dob = LocalDate.parse(dobText);
            if (!dob.isBefore(LocalDate.now())) {
                throw new ApiException(400, "dob must be in the past");
            }
            return dob;
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "dob must be in format yyyy-MM-dd");
        }
    }

    private Map<String, Object> findCustomer(long id) {
        List<Map<String, Object>> rows = Db.query("SELECT * FROM customers WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Customer " + id + " not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> customerJson(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.get("id"));
        m.put("fullName", r.get("full_name"));
        m.put("dob", r.get("dob"));
        m.put("email", r.get("email"));
        m.put("phone", r.get("phone"));
        m.put("state", r.get("state"));
        return m;
    }

    // ------------------------------------------------------------------- quotes

    private void quotes(HttpExchange ex, String method, String[] p) throws IOException {
        if (p.length == 1 && method.equals("POST")) {
            Map<String, Object> b = body(ex);
            long customerId = reqLong(b, "customerId");
            String type = str(b, "coverageType");
            BigDecimal coverage = reqDecimal(b, "coverageAmount");
            Map<String, Object> customer = findCustomer(customerId);
            if (!Rules.isValidCoverageType(type)) {
                throw new ApiException(400, "coverageType must be HEALTH, AUTO or HOME");
            }
            if (coverage.signum() <= 0) {
                throw new ApiException(400, "coverageAmount must be greater than 0");
            }
            int age = Rules.ageOn(LocalDate.parse(customer.get("dob").toString()), LocalDate.now());
            BigDecimal premium = Rules.premium(type, coverage, age);
            long id = Db.insert("INSERT INTO quotes(customer_id, coverage_type, coverage_amount, premium) VALUES (?,?,?,?)",
                    customerId, type, coverage, premium);
            send(ex, 201, quoteJson(findQuote(id)));
        } else if (p.length == 2 && method.equals("GET")) {
            send(ex, 200, quoteJson(findQuote(id(p[1]))));
        } else {
            throw new ApiException(405, "Method not allowed");
        }
    }

    private Map<String, Object> findQuote(long id) {
        List<Map<String, Object>> rows = Db.query("SELECT * FROM quotes WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Quote " + id + " not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> quoteJson(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("quoteId", r.get("id"));
        m.put("customerId", r.get("customer_id"));
        m.put("coverageType", r.get("coverage_type"));
        m.put("coverageAmount", r.get("coverage_amount"));
        m.put("premium", r.get("premium"));
        return m;
    }

    // ----------------------------------------------------------------- policies

    private void policies(HttpExchange ex, String method, String[] p) throws IOException {
        if (p.length == 1 && method.equals("POST")) {
            long quoteId = reqLong(body(ex), "quoteId");
            Map<String, Object> quote = findQuote(quoteId);
            if (!Db.query("SELECT id FROM policies WHERE quote_id = ?", quoteId).isEmpty()) {
                throw new ApiException(409, "A policy already exists for quote " + quoteId);
            }
            Map<String, Object> customer = findCustomer(((Number) quote.get("customer_id")).longValue());
            int age = Rules.ageOn(LocalDate.parse(customer.get("dob").toString()), LocalDate.now());
            String rejection = Rules.underwritingRejection(age, (BigDecimal) quote.get("coverage_amount"));
            String status = rejection == null ? "ISSUED" : "REJECTED";
            String reason = rejection == null ? "Underwriting passed" : rejection;
            long id = Db.insert("INSERT INTO policies(quote_id, customer_id, status, reason) VALUES (?,?,?,?)",
                    quoteId, quote.get("customer_id"), status, reason);
            send(ex, 201, policyJson(findPolicy(id)));
        } else if (p.length == 2 && method.equals("GET")) {
            send(ex, 200, policyJson(findPolicy(id(p[1]))));
        } else if (p.length == 3 && p[2].equals("cancel") && method.equals("PUT")) {
            long id = id(p[1]);
            Map<String, Object> policy = findPolicy(id);
            if (!"ISSUED".equals(policy.get("status"))) {
                throw new ApiException(409, "Only ISSUED policies can be cancelled");
            }
            Db.update("UPDATE policies SET status = 'CANCELLED', reason = 'Cancelled by customer' WHERE id = ?", id);
            send(ex, 200, policyJson(findPolicy(id)));
        } else if (p.length == 3 && p[2].equals("claims") && method.equals("GET")) {
            long id = id(p[1]);
            findPolicy(id);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map<String, Object> r : Db.query("SELECT * FROM claims WHERE policy_id = ? ORDER BY id", id)) {
                list.add(claimJson(r));
            }
            send(ex, 200, list);
        } else {
            throw new ApiException(405, "Method not allowed");
        }
    }

    /** Policy joined with its quote so the response carries coverage and premium. */
    private Map<String, Object> findPolicy(long id) {
        List<Map<String, Object>> rows = Db.query(
                "SELECT p.id, p.quote_id, p.customer_id, p.status, p.reason, q.coverage_type, q.coverage_amount, q.premium "
                        + "FROM policies p JOIN quotes q ON q.id = p.quote_id WHERE p.id = ?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Policy " + id + " not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> policyJson(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("policyId", r.get("id"));
        m.put("quoteId", r.get("quote_id"));
        m.put("customerId", r.get("customer_id"));
        m.put("status", r.get("status"));
        m.put("reason", r.get("reason"));
        m.put("coverageType", r.get("coverage_type"));
        m.put("coverageAmount", r.get("coverage_amount"));
        m.put("premium", r.get("premium"));
        return m;
    }

    // ------------------------------------------------------------------- claims

    private void claims(HttpExchange ex, String method, String[] p) throws IOException {
        if (p.length == 1 && method.equals("POST")) {
            Map<String, Object> b = body(ex);
            long policyId = reqLong(b, "policyId");
            BigDecimal amount = reqDecimal(b, "amount");
            Map<String, Object> policy = findPolicy(policyId);
            if (amount.signum() <= 0) {
                throw new ApiException(400, "amount must be greater than 0");
            }
            String status;
            String reason;
            if (!"ISSUED".equals(policy.get("status"))) {
                status = "REJECTED";
                reason = "Policy is not active (status: " + policy.get("status") + ")";
            } else if (amount.compareTo((BigDecimal) policy.get("coverage_amount")) > 0) {
                status = "REJECTED";
                reason = "Claim amount exceeds coverage amount";
            } else if (amount.compareTo(Rules.AUTO_APPROVE_LIMIT) <= 0) {
                status = "APPROVED";
                reason = "Auto-approved";
            } else {
                status = "UNDER_REVIEW";
                reason = "Claim above 50000 requires manual review";
            }
            long id = Db.insert("INSERT INTO claims(policy_id, amount, description, status, reason) VALUES (?,?,?,?,?)",
                    policyId, amount, str(b, "description"), status, reason);
            send(ex, 201, claimJson(findClaim(id)));
        } else if (p.length == 2 && method.equals("GET")) {
            send(ex, 200, claimJson(findClaim(id(p[1]))));
        } else {
            throw new ApiException(405, "Method not allowed");
        }
    }

    private Map<String, Object> findClaim(long id) {
        List<Map<String, Object>> rows = Db.query("SELECT * FROM claims WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Claim " + id + " not found");
        }
        return rows.get(0);
    }

    private Map<String, Object> claimJson(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("claimId", r.get("id"));
        m.put("policyId", r.get("policy_id"));
        m.put("amount", r.get("amount"));
        m.put("description", r.get("description"));
        m.put("status", r.get("status"));
        m.put("reason", r.get("reason"));
        return m;
    }

    // ------------------------------------------------------------------ reports

    private void reports(HttpExchange ex, String method, String[] p) throws IOException {
        if (p.length == 2 && p[1].equals("claims-summary") && method.equals("GET")) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> r : Db.query(
                    "SELECT c.full_name, p.id AS policy_id, p.status AS policy_status, "
                            + "COUNT(cl.id) AS claim_count, COALESCE(SUM(cl.amount), 0) AS total_claimed "
                            + "FROM customers c JOIN policies p ON p.customer_id = c.id "
                            + "LEFT JOIN claims cl ON cl.policy_id = p.id "
                            + "GROUP BY c.full_name, p.id, p.status ORDER BY p.id")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("customerName", r.get("full_name"));
                m.put("policyId", r.get("policy_id"));
                m.put("policyStatus", r.get("policy_status"));
                m.put("claimCount", r.get("claim_count"));
                m.put("totalClaimed", r.get("total_claimed"));
                out.add(m);
            }
            send(ex, 200, out);
        } else {
            throw new ApiException(405, "Method not allowed");
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> body(HttpExchange ex) throws IOException {
        String text = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (text.isBlank()) {
            throw new ApiException(400, "Request body is required");
        }
        try {
            Map<String, Object> m = GSON.fromJson(text, new TypeToken<Map<String, Object>>() { }.getType());
            if (m == null) {
                throw new ApiException(400, "Request body is required");
            }
            return m;
        } catch (JsonSyntaxException e) {
            throw new ApiException(400, "Malformed JSON");
        }
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString().trim();
    }

    private static long reqLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new ApiException(400, key + " is required");
        }
        try {
            return new BigDecimal(v.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ApiException(400, key + " must be a whole number");
        }
    }

    private static BigDecimal reqDecimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new ApiException(400, key + " is required");
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            throw new ApiException(400, key + " must be a number");
        }
    }

    private static long id(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid id: " + text);
        }
    }

    private void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void staticFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        InputStream in = path.contains("..") ? null : InsuranceApp.class.getResourceAsStream("/web" + path);
        if (in == null) {
            byte[] nf = "Not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, nf.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(nf);
            }
            return;
        }
        try (in) {
            byte[] data = in.readAllBytes();
            String type = path.endsWith(".html") ? "text/html; charset=utf-8"
                    : path.endsWith(".js") ? "application/javascript; charset=utf-8"
                    : path.endsWith(".css") ? "text/css; charset=utf-8" : "application/octet-stream";
            ex.getResponseHeaders().set("Content-Type", type);
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        }
    }
}
