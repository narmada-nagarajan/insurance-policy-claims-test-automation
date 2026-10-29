package com.insurance.engine;

import com.insurance.components.BusinessComponents;
import com.insurance.components.ScenarioContext;
import com.insurance.utils.TestDataReader;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The keyword-driven engine. scenarios.csv holds ordered steps: TestCaseId, Step, Keyword, DataRef.
 * DataRef = "<datafile>/<DataSetId>" (e.g. customer_data/CUST_ADULT_35) points at the data-driven input for the step.
 * A new scenario is just new rows - the Java below never changes unless a NEW keyword is added.
 */
public class ScenarioRunner {

    private final ScenarioContext ctx;
    private final BusinessComponents components;

    public ScenarioRunner(ScenarioContext ctx) {
        this.ctx = ctx;
        this.components = new BusinessComponents(ctx);
    }

    public void run(String testCaseId) {
        List<Map<String, String>> steps = TestDataReader.readAll("scenarios").stream()
                .filter(row -> testCaseId.equals(row.get("TestCaseId")))
                .sorted(Comparator.comparingInt((Map<String, String> row) -> Integer.parseInt(row.get("Step"))))
                .collect(Collectors.toList());
        if (steps.isEmpty()) {
            throw new IllegalStateException("No steps found in scenarios.csv for " + testCaseId);
        }
        for (Map<String, String> step : steps) {
            String keyword = step.get("Keyword");
            System.out.println("  [" + testCaseId + "] step " + step.get("Step") + ": " + keyword + " " + step.get("DataRef"));
            execute(keyword, loadData(step.get("DataRef")));
        }
    }

    private void execute(String keyword, Map<String, String> data) {
        switch (keyword) {
            case "CreateCustomer" -> components.createCustomer(data);
            case "GetQuote" -> components.getQuote(data);
            case "IssuePolicy" -> components.issuePolicy(data);
            case "SubmitClaim" -> components.submitClaim(data);
            case "ValidateOutcome" -> components.validateOutcome(data);
            default -> throw new IllegalArgumentException("Unknown keyword '" + keyword + "' in scenarios.csv");
        }
    }

    private Map<String, String> loadData(String dataRef) {
        if (dataRef == null || dataRef.isBlank()) {
            return Map.of();
        }
        String[] parts = dataRef.split("/");
        return TestDataReader.dataSet(parts[0], parts[1], ctx.unique);
    }
}
