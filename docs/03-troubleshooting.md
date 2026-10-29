# Troubleshooting playbook

**Triage order:** read the failure message -> look at the screenshot (`target/screenshots`) -> rerun that one test locally -> decide: product bug, test bug, data issue or environment issue -> fix the cause, not the symptom.

## Slow tests

| Cause | Fix (as done here) |
|---|---|
| Setup through the UI | Create data via API (`ApiClient.newIssuedPolicy`) |
| Too many UI tests | Test rules at API level; keep UI to journeys |
| `Thread.sleep` | Explicit waits that return the moment the condition is true |
| Sequential everything | `parallel="classes"` in the suite; `ThreadLocal` driver and per-scenario context already support it |
| New browser per test is costly | Keep isolation for reliability; if needed, per class with cleanup |
| Unknown hot spots | Check TestNG report timings, fix the slowest first |

## Flaky scripts

1. Reproduce: run the test 10-20 times (`invocationCount`) to prove it is flaky.
2. Classify: timing, shared data, order dependence, environment, real intermittent product bug.
3. Fix the root cause. Retry is limited to UI tests, once, and only to protect the pipeline.
4. Any test that passes only on retry gets a ticket; flaky tests are quarantined (excluded from the gate, still run nightly) with an owner and a deadline.
5. Track flakiness rate per week; target below 2%.

## Synchronisation failures (examples in this code)

| Symptom | Cause | Fix |
|---|---|---|
| `NoSuchElementException` right after click | Async content not there yet | `visible(by)` explicit wait (`QuotePage.premium`) |
| `StaleElementReferenceException` | Table body rebuilt | Re-locate, wait for row count (`ClaimPage.waitForRowCount`), retry in `BasePage.click` |
| `ElementClickInterceptedException` | Loader/overlay on top | Wait for loader to go (`waitForLoaderToDisappear`), `elementToBeClickable` |
| Assertion sees an OLD message | Element still shows previous text | Wait for expected text (`waitForTextToContain`) instead of "not empty". **This was a real bug found in this project's review (`UiConceptsTest`).** |
| Works with sleep, fails without | Wrong condition waited on | Find the true "done" signal (element, text, URL, count) |
| Wrong frame/window | Context not switched back | `finally { switchTo().defaultContent() }`; restore parent handle |

## CI pipeline failures

| Symptom | Likely cause | Fix |
|---|---|---|
| Passes locally, fails in Jenkins | Headless differences, small window, no sandbox | `--headless=new --window-size=1366,768 --no-sandbox --disable-dev-shm-usage` (in `DriverFactory`) |
| Browser/driver mismatch | Agent Chrome updated | Selenium Manager; pin agent image versions |
| Port already in use | Concurrent builds | `disableConcurrentBuilds`, `-Dapp.port` |
| Time-dependent failures | Hard-coded dates, time zone | `{{age:N}}` placeholders; fixed clock/time zone on agent |
| Hangs | No timeouts | Pipeline `timeout`, page-load timeout, wait timeout |
| Red build, no evidence | No artifacts | Archive screenshots and surefire reports (`post always`) |
| Compile fails only in CI | Different JDK | Tool block pins JDK 17; `maven.compiler.release=17` |
| Shared test data collisions | Reused emails/IDs | Unique data per test (`{{unique}}`, UUID) |
