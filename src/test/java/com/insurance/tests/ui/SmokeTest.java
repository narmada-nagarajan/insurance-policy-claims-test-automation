package com.insurance.tests.ui;

import com.insurance.base.BaseTest;
import com.insurance.components.ScenarioContext;
import com.insurance.engine.ScenarioRunner;
import org.testng.annotations.Test;

/** One UI happy path through all five steps of the workflow: the fast gate for every commit. */
public class SmokeTest extends BaseTest {

    @Test(groups = "smoke", description = "Customer -> quote -> policy -> claim -> validate UI/API/DB")
    public void policyIssueAndClaimHappyPath() {
        new ScenarioRunner(new ScenarioContext(driver)).run("TC_E2E_01");
    }
}
