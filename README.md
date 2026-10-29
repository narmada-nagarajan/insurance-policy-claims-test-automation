# Insurance Policy and Claims Test Automation Framework

A compact, interview-sized project covering the full insurance workflow through **UI (Selenium), API (REST Assured) and database (SQL)**, built with Java 17, TestNG and Maven in a CRAFT-style hybrid framework (keyword-driven + data-driven + modular).

The application under test is bundled (static pages + REST API + in-memory H2 database), so nothing external has to be installed or deployed. The test suite starts it automatically.

## Workflow covered

1. **Customer details** - applicant form (text, dropdown, iframe terms, privacy window)
2. **Quote** - choose coverage, slow premium calculation
3. **Issue / reject policy** - underwriting (age 18-75, coverage up to 1,000,000)
4. **Claim** - policy must be ISSUED, amount within coverage, auto-approve up to 50,000 else manual review
5. **Validate outcome** - UI message, API response and DB row checked together

## Prerequisites

JDK 17+, Maven 3.9+, Chrome (Firefox/Edge optional). Selenium Manager downloads the matching driver on first run.

## Run

| Goal | Command |
|---|---|
| API + DB only (no browser, ~seconds) | `mvn test -DsuiteFile=api.xml` |
| Smoke (1 UI path + API health) | `mvn test -DsuiteFile=smoke.xml -Dheadless=true` |
| Full regression (default) | `mvn test` |
| Regression, headless, Firefox | `mvn test -Dheadless=true -Dbrowser=firefox` |
| Change wait timeout / retries / port | `-Dtimeout=20 -Dretry=0 -Dapp.port=8090` |
| Explore the app by hand | `mvn compile exec:java -Dexec.mainClass=com.insurance.app.InsuranceApp` then open http://localhost:8089 |

Reports: `target/surefire-reports/index.html`; failure screenshots: `target/screenshots/`.

## Test inventory (kept deliberately small)

| Layer | Tests | Where |
|---|---|---|
| UI end-to-end (CRAFT scenarios) | 5 scenarios from `run_manager.csv` | `CraftE2ETest` |
| UI Selenium concepts | 3 (frame/window/dropdown, explicit wait, stale element) | `UiConceptsTest` |
| API | 11 methods, 21 data-driven runs | `ApiTests` |
| Database | 3 (joins, GROUP BY/HAVING, invariants) | `DbValidationTests` |
| Smoke | 1 UI + 1 API | `SmokeTest`, `ApiTests` (group `smoke`) |

## Structure

```
src/main/java/com/insurance/app      App under test: InsuranceApp (REST), Rules (business rules), Db (H2)
src/main/resources/web               The 5 HTML pages
src/test/java/com/insurance
  base/        BaseTest, ApiBase, DriverFactory (ThreadLocal), AppServer
  pages/       Page Object Model: BasePage (all waits) + Customer/Quote/Policy/Claim pages
  components/  BusinessComponents = the keywords; ScenarioContext = shared state of one scenario
  engine/      ScenarioRunner (keyword engine), RunManager (Execute Yes/No)
  utils/       ApiClient (REST Assured), DbUtils (JDBC), TestDataReader (CSV + placeholders), Config
  listeners/   TestListener (screenshot on failure), RetryAnalyzer/RetryTransformer (UI only)
  tests/       ui/ api/ db/
src/test/resources/suites              smoke.xml, api.xml, regression.xml
src/test/resources/testdata            run_manager, scenarios, customer/quote/claim/expected data (CSV)
Jenkinsfile                            Compile -> API+DB gate -> UI tests -> reports/artifacts
docs/                                  Concept map, strategy, troubleshooting, leadership, SQL/API cheatsheet, 2-day plan
```

## How the hybrid (CRAFT-style) framework works

`run_manager.csv` switches scenarios on/off -> `scenarios.csv` lists ordered **keywords** per scenario -> each keyword (`CreateCustomer`, `GetQuote`, `IssuePolicy`, `SubmitClaim`, `ValidateOutcome`) is a reusable method in `BusinessComponents` that drives page objects -> inputs come from the **data sheets** via `DataRef` (`customer_data/CUST_ADULT_35`). Adding a scenario = adding CSV rows; Java changes only for a new keyword. This mirrors the publicly described CRAFT ideas (run manager, data sheets, business components, driver); check the exact CRAFT terminology against what you know before the interview.

## Design decisions worth explaining

- **No implicit wait, no `Thread.sleep`** - every interaction in `BasePage` uses an explicit wait.
- **Test data via API, not UI** - faster and independent of earlier UI steps.
- **Dates never go stale** - `{{age:35}}` and `{{unique}}` placeholders resolve at run time.
- **Retry only UI tests**, once, and report retried passes - a retry is a safety net, not a fix.
- **SoftAssert in `ValidateOutcome`** - shows UI, API and DB mismatches in one run.
- **Fail-fast pipeline** - API+DB gate before browsers.

## Verification status

This project was authored in a sandbox without access to Maven Central, so the full `mvn test` run could **not** be executed there. What was verified: the application code compiles; premium and underwriting rules match every expected value in the test data; all 35 page-object locators exist in the HTML; and the pages were driven in real Chromium (iframe, new window, slow premium, table re-render, rejected policy) against a stand-in API. **Run `mvn test -DsuiteFile=api.xml` and then the smoke suite first on your machine** and fix anything environmental (JDK version, Chrome).
