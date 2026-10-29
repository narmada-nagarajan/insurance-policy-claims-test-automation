# Concept map: interview topic -> where it lives -> what to say

## Selenium with Java

| Concept | Where | Say this |
|---|---|---|
| Locators | `CustomerPage` (name, id, linkText), `QuotePage` (css `input#coverageAmount`), `ClaimPage.statusInTable` (relative XPath) | Prefer id/name; CSS next; XPath only for relationships (row by cell text). Never absolute XPath. |
| Explicit waits | `BasePage` (`visible`, `click`, `nonEmptyText`, `waitForTextToContain`) | Wait for a condition, not a time. No implicit wait mixed in - it makes timeouts unpredictable. |
| Fluent/custom condition | `BasePage.nonEmptyText` (lambda `ExpectedCondition`) | Write a custom condition when no built-in one fits. |
| WebElements | `getText`, `sendKeys`, `clear`, `isDisplayed` in pages | Re-find elements after the page changes; never store them across updates. |
| Dropdown | `Select` in `BasePage`; `CustomerPage.enterDetails` (by text), `QuotePage` (by value) | `Select` works only on `<select>`; custom dropdowns need click + click option. |
| Frames | `CustomerPage.acceptTerms` | Switch in, act, **always** switch back (`finally`). |
| Windows | `CustomerPage.readPrivacyHeadingFromNewWindow` | Remember parent handle, wait for 2 windows, switch, close, switch back. |
| Exceptions | `BasePage.click` (Stale, ClickIntercepted), `UiConceptsTest.claimsTable_staleElementIsRefound` | Stale = DOM re-rendered; ClickIntercepted = overlay/loader; NoSuchElement = locator or timing; Timeout = condition never met. |
| Java coding | `TestDataReader` (streams, regex, collections), `DriverFactory` (switch expression, ThreadLocal) | Be ready to write: reverse string, duplicates, anagram, map sort - your PDF notes cover them. |

## Automation framework

| Concept | Where | Say this |
|---|---|---|
| POM | `pages/` | One class per page, locators private, methods return pages or values, no assertions in pages. |
| Keyword-driven | `scenarios.csv`, `ScenarioRunner`, `BusinessComponents` | Test steps are data; each keyword is a reusable component. |
| Data-driven | `*_data.csv`, `TestDataReader`; TestNG `@DataProvider` in `ApiTests` | Same logic, many data sets. Placeholders keep data fresh. |
| Modular | `BusinessComponents` keywords independent via `ScenarioContext` | Any keyword order works if prerequisites hold (guarded by assertions). |
| Run Manager | `run_manager.csv` + `RunManager` | Enable/disable scenarios without code changes. |
| TestNG | suites in `resources/suites`, groups, listeners, data providers, retry transformer | Suites = what runs; groups = slices (smoke/api/db); listeners = cross-cutting (screenshots). |
| Maven | `pom.xml`: `-DsuiteFile`, `-Dheadless`, `-Dbrowser` | Profiles/properties select suite and environment; surefire runs the suite XML. |
| Reusable components | `BasePage`, `ApiClient`, `DbUtils`, `DriverFactory`, `Config` | One place for waits, HTTP, SQL, driver creation, settings. |

## Test strategy, troubleshooting, API/DB, CI/CD, leadership, stakeholders

See `02-test-strategy.md`, `03-troubleshooting.md`, `06-api-sql-cheatsheet.md`, the `Jenkinsfile`, and `04-leadership-and-stakeholders.md`.

## 30-second project pitch

"I built an insurance reference framework covering customer, quote, underwriting and claims. UI tests use Selenium with a page-object layer and explicit waits; a CRAFT-style engine runs keyword scenarios from CSV with a run manager. REST Assured and SQL validate the same outcomes, and a Jenkins pipeline runs the fast API gate first, then the UI suite headless, archiving reports and screenshots. I kept the UI suite small and pushed most checks to API level."
