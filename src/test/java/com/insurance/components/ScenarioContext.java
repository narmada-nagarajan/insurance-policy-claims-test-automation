package com.insurance.components;

import com.insurance.pages.ClaimPage;
import com.insurance.pages.CustomerPage;
import com.insurance.pages.PolicyPage;
import com.insurance.pages.QuotePage;
import org.openqa.selenium.WebDriver;

import java.util.UUID;

/** State shared by the keywords of ONE scenario: the browser, the current pages and the ids/values created so far. */
public class ScenarioContext {

    public final WebDriver driver;
    /** Makes emails unique per scenario ({{unique}} placeholder in the data files). */
    public final String unique = UUID.randomUUID().toString().substring(0, 8);

    public CustomerPage customerPage;
    public QuotePage quotePage;
    public PolicyPage policyPage;
    public ClaimPage claimPage;

    public long customerId;
    public long quoteId;
    public long policyId;
    public long claimId;

    public String premiumText;
    public String policyStatus;
    public String policyMessage;
    public String claimStatus;
    public String claimMessage;

    public ScenarioContext(WebDriver driver) {
        this.driver = driver;
    }
}
