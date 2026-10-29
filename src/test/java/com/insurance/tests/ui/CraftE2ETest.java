package com.insurance.tests.ui;

import com.insurance.base.BaseTest;
import com.insurance.components.ScenarioContext;
import com.insurance.engine.RunManager;
import com.insurance.engine.ScenarioRunner;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * The hybrid (keyword + data + modular) test. ONE Java test method runs EVERY scenario switched on in
 * run_manager.csv; the steps come from scenarios.csv and the inputs from the *_data.csv files.
 */
public class CraftE2ETest extends BaseTest {

    @DataProvider(name = "runManager")
    public Object[][] runManager() {
        List<Map<String, String>> scenarios = RunManager.scenariosToRun();
        Object[][] data = new Object[scenarios.size()][2];
        for (int i = 0; i < scenarios.size(); i++) {
            data[i][0] = scenarios.get(i).get("TestCaseId");
            data[i][1] = scenarios.get(i).get("Description");
        }
        return data;
    }

    @Test(dataProvider = "runManager", groups = "regression")
    public void executeScenario(String testCaseId, String description) {
        System.out.println("Scenario " + testCaseId + ": " + description);
        new ScenarioRunner(new ScenarioContext(driver)).run(testCaseId);
    }
}
