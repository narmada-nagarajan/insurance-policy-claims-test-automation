# Test Case Document: Insurance Policy and Claims

**Application Under Test:** Insurance Portal (bundled sample app: UI + REST API + database)
**Modules:** Customer, Quote, Policy (underwriting), Claim, Data consistency
**Prepared by:** Narmada Nagarajan
**Test Environment:** Web (Chrome), REST API, H2 database
**Test types:** Functional, negative, boundary value, integration (UI + API + DB)

Each case is linked to a business rule (Req ID) and, where automated, to the test that covers it. "Manual only" means it is documented but not automated.

## Business rules (requirements)

| Req ID | Rule |
|---|---|
| REQ-CUS-01 | Customer needs a name, a valid email and a date of birth in the past |
| REQ-CUS-02 | Email must be unique |
| REQ-CUS-03 | Terms and conditions must be accepted before saving |
| REQ-QUO-01 | Premium = coverage x rate (Health 2%, Auto 3%, Home 1.5%) x age factor (under 25: 1.2, 25-50: 1.0, over 50: 1.3) |
| REQ-POL-01 | Policy is issued only for applicants aged 18 to 75 |
| REQ-POL-02 | Policy is rejected when coverage exceeds 1,000,000 |
| REQ-POL-03 | A quote can become a policy only once |
| REQ-POL-04 | Only ISSUED policies can be cancelled |
| REQ-CLM-01 | Claims on a non-active policy are rejected |
| REQ-CLM-02 | Claim amount above the coverage amount is rejected |
| REQ-CLM-03 | Claims up to 50,000 are auto-approved; above 50,000 go to manual review |
| REQ-DAT-01 | UI, API and database must show the same outcome |

## 1. Customer

| Test Case ID | Test Scenario | Test Steps | Test Data | Expected Result | Priority | Req ID | Automated in |
|---|---|---|---|---|---|---|---|
| TC_CUS_001 | Save a customer with valid details | 1. Open Customer Details page<br>2. Fill all fields, select state<br>3. Accept terms<br>4. Click Save customer | Ravi Kumar, age 35, unique email, Tamil Nadu | Message "Customer saved successfully" with a customer ID; record exists in DB | High | REQ-CUS-01 | `CraftE2ETest` TC_E2E_01; `ApiTests.customer_createFetchAndUnknownId` |
| TC_CUS_002 | Reject invalid input | 1. Submit customer with one invalid field | Blank name; email "not-an-email"; date of birth in the future | Name: "fullName is required"; email: "A valid email is required"; date: "dob must be in the past" (API 400) | High | REQ-CUS-01 | `ApiTests.customer_invalidInput_returns400` (3 data rows) |
| TC_CUS_003 | Reject a duplicate email | 1. Create a customer<br>2. Create another with the same email | Same email twice | Second request fails: "Email already registered" (409) | High | REQ-CUS-02 | `ApiTests.customer_duplicateEmail_returns409` |
| TC_CUS_004 | Block save when terms are not accepted | 1. Fill valid details<br>2. Do not tick terms checkbox<br>3. Click Save | Kavya Nair, age 30 | Message "Please accept the terms and conditions"; no customer created in DB | Medium | REQ-CUS-03 | `CraftE2ETest` TC_UI_NEG_01 |
| TC_CUS_005 | Dropdown, iframe and new window behave correctly | 1. Click "View privacy policy" (new window)<br>2. Close it, select state<br>3. Save without terms, then tick terms inside the frame and save | State: Kerala | Privacy heading shown; state kept; save blocked, then succeeds | Medium | REQ-CUS-03 | `UiConceptsTest.customerForm_handlesDropdownFrameAndWindow` |

## 2. Quote

| Test Case ID | Test Scenario | Test Steps | Test Data | Expected Result | Priority | Req ID | Automated in |
|---|---|---|---|---|---|---|---|
| TC_QUO_001 | Health premium for an adult | 1. Open quote page<br>2. Select Health, enter coverage<br>3. Click Calculate premium | Age 35, Health, 100,000 | Premium 2000.00 | High | REQ-QUO-01 | `ApiTests.quote_premiumIsCalculatedCorrectly`; TC_E2E_01 |
| TC_QUO_002 | Premium reflects product and age band | 1. Create quotes for each combination | Age 60, Auto, 50,000 -> 1950.00; Age 22, Home, 200,000 -> 3600.00 | Premium follows the formula | High | REQ-QUO-01 | `ApiTests.quote_premiumIsCalculatedCorrectly` (3 data rows); TC_E2E_02 |
| TC_QUO_003 | Premium appears after the calculation finishes | 1. Click Calculate<br>2. Observe loader and result | Age 35, Health, 100,000 | Premium not shown immediately; shown after loader ends | Medium | REQ-QUO-01 | `UiConceptsTest.quote_explicitWaitHandlesSlowPremium` |

## 3. Policy (underwriting)

| Test Case ID | Test Scenario | Test Steps | Test Data | Expected Result | Priority | Req ID | Automated in |
|---|---|---|---|---|---|---|---|
| TC_POL_001 | Issue a policy for an eligible applicant | 1. Create customer and quote<br>2. Click Issue policy | Age 35, 100,000 | Status ISSUED, "Underwriting passed"; claim link shown | High | REQ-POL-01 | TC_E2E_01; `ApiTests.policy_underwritingRules` |
| TC_POL_002 | Reject an applicant below 18 | 1. Create customer and quote<br>2. Click Issue policy | Age 17, Home, 200,000 | Status REJECTED, "below minimum age"; no claim link | High | REQ-POL-01 | TC_E2E_03; `ApiTests.policy_underwritingRules` |
| TC_POL_003 | Reject an applicant above 75 | Same as above | Age 80, 100,000 | Status REJECTED, "above maximum age" | Medium | REQ-POL-01 | `ApiTests.policy_underwritingRules` |
| TC_POL_004 | Reject coverage above the limit | Same as above | Age 35, coverage 2,000,000 | Status REJECTED, "underwriting limit" | Medium | REQ-POL-02 | `ApiTests.policy_underwritingRules` |
| TC_POL_005 | One policy per quote | 1. Issue a policy<br>2. Issue again for the same quote | Same quote ID | Second request fails (409) | Medium | REQ-POL-03 | `ApiTests.policy_secondPolicyForSameQuote_returns409` |
| TC_POL_006 | Cancelled policy cannot be claimed on | 1. Issue policy<br>2. Cancel it (and cancel again)<br>3. Submit a claim | Coverage 100,000, claim 1,000 | Cancel 200, second cancel 409; claim REJECTED "not active" | High | REQ-POL-04, REQ-CLM-01 | `ApiTests.policy_cancelThenClaimIsRejected` |

## 4. Claim

| Test Case ID | Test Scenario | Test Steps | Test Data | Expected Result | Priority | Req ID | Automated in |
|---|---|---|---|---|---|---|---|
| TC_CLM_001 | Small claim is auto-approved | 1. Open claim page for an issued policy<br>2. Enter amount and submit | Coverage 100,000, claim 20,000 | APPROVED, "Auto-approved"; row shown in claims table | High | REQ-CLM-03 | TC_E2E_01; `ApiTests.claim_decisionRules` |
| TC_CLM_002 | Boundary: claim of exactly 50,000 | Submit claim | Coverage 200,000, claim 50,000 | APPROVED | High | REQ-CLM-03 | `ApiTests.claim_decisionRules` |
| TC_CLM_003 | Boundary: claim just above 50,000 | Submit claim | Coverage 200,000, claim 50,001 (UI: 60,000 on 100,000 cover) | UNDER_REVIEW, "requires manual review" | High | REQ-CLM-03 | TC_E2E_02; `ApiTests.claim_decisionRules` |
| TC_CLM_004 | Claim above coverage is rejected | Submit claim | Coverage 200,000, claim 250,000 | REJECTED, "exceeds coverage" | High | REQ-CLM-02 | TC_E2E_04; `ApiTests.claim_decisionRules` |
| TC_CLM_005 | Claims table updates after each submit | 1. Submit two claims on one policy | 1,000 then 2,000 | Table shows 2 rows with correct statuses | Medium | REQ-CLM-03 | `UiConceptsTest.claimsTable_staleElementIsRefound` |
| TC_CLM_006 | Invalid claim amount | Submit negative amount | -5 | Request rejected (400) | Medium | REQ-CLM-02 | `ApiTests.policy_cancelThenClaimIsRejected` |

## 5. Data consistency

| Test Case ID | Test Scenario | Test Steps | Test Data | Expected Result | Priority | Req ID | Automated in |
|---|---|---|---|---|---|---|---|
| TC_DAT_001 | Same outcome in UI, API and DB | 1. Complete the full workflow in the UI<br>2. Compare UI messages, API responses and DB rows | Scenarios TC_E2E_01 to 04 | Policy and claim status, and premium, match in all three layers | High | REQ-DAT-01 | `ValidateOutcome` keyword; `DbValidationTests.joinAcrossAllTables...` |
| TC_DAT_002 | Business-rule invariants hold for all data | 1. Run SQL checks across all records | All rows | No orphan policies; no ISSUED policy outside age 18-75; no APPROVED claim above 50,000 | High | REQ-POL-01, REQ-CLM-03 | `DbValidationTests.businessRuleInvariants_holdForAllRows` |
| TC_DAT_003 | Claims report matches submitted claims | 1. Submit two claims<br>2. Read the claims summary report and SQL totals | 10,000 + 15,000 | Claim count 2 and total agree between report and SQL | Medium | REQ-DAT-01 | `ApiTests.report_claimsSummaryMatchesSubmittedClaims`; `DbValidationTests.aggregation...` |

## Traceability matrix (requirement -> test cases)

| Req ID | Test cases | Layers |
|---|---|---|
| REQ-CUS-01 | TC_CUS_001, TC_CUS_002 | UI, API |
| REQ-CUS-02 | TC_CUS_003 | API |
| REQ-CUS-03 | TC_CUS_004, TC_CUS_005 | UI |
| REQ-QUO-01 | TC_QUO_001, TC_QUO_002, TC_QUO_003 | UI, API |
| REQ-POL-01 | TC_POL_001, TC_POL_002, TC_POL_003, TC_DAT_002 | UI, API, DB |
| REQ-POL-02 | TC_POL_004 | API |
| REQ-POL-03 | TC_POL_005 | API |
| REQ-POL-04 | TC_POL_006 | API |
| REQ-CLM-01 | TC_POL_006 | API |
| REQ-CLM-02 | TC_CLM_004, TC_CLM_006 | UI, API |
| REQ-CLM-03 | TC_CLM_001, TC_CLM_002, TC_CLM_003, TC_CLM_005, TC_DAT_002 | UI, API, DB |
| REQ-DAT-01 | TC_DAT_001, TC_DAT_003 | UI, API, DB |

Every requirement has at least one test case. Customer, quote and policy rules are checked mainly at API level (fast, stable); the UI covers the end-to-end journey and page behaviour.

## Notes

- This is a sample document for portfolio purposes. The application is a bundled practice app, not a client system.
- Test data uses generated names and emails only.
- Manual-only candidates to add later: browser compatibility (Firefox, Edge), accessibility, and exploratory testing of the claim description field.
