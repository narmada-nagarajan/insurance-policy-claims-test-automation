# Leadership and stakeholder management

Use your own real experience for the stories; this page gives structures and examples tied to this project.

## Code review checklist (automation)

- Locators: id/name/CSS first, no absolute XPath, no locators in tests.
- Waits: explicit only, no `Thread.sleep`, no implicit wait.
- Page objects hold no assertions; tests hold no locators.
- Test independence: own data, no order dependence, cleanup not required for correctness.
- Assertions meaningful (message included); `SoftAssert` only where several checks belong together.
- No hard-coded dates, URLs, credentials; config via properties.
- Reuse existing components before adding new ones; naming states the behaviour.
- Failure output tells you what went wrong without a debugger.

Review etiquette: ask questions, explain the why, separate must-fix from suggestions, praise good work, keep reviews small (< 300 lines) and turn them around within a day.

## Mentoring plan (new automation engineer)

| Week | Focus | Output |
|---|---|---|
| 1 | Run the suite, read a page object and a test, pair on one fix | First small PR |
| 2 | Write a page object + test for a new field | Reviewed PR |
| 3 | Waits and flaky-test diagnosis | One flaky test root-caused |
| 4 | API + SQL checks for the same feature | Data-driven API test |
Practices: weekly 1:1, pairing, review comments as teaching, rotating "flaky test owner".

## Effort estimation (three-point / PERT)

Estimate = (Optimistic + 4 x Likely + Pessimistic) / 6, plus a buffer for review and reruns.

| Task (new "add rider to policy" feature) | O | L | P | Estimate (days) |
|---|---|---|---|---|
| Analyse and design test cases | 0.5 | 1 | 2 | 1.1 |
| Page object + keyword + data rows | 1 | 1.5 | 3 | 1.7 |
| API tests (5 cases) | 0.5 | 1 | 1.5 | 1.0 |
| DB checks | 0.25 | 0.5 | 1 | 0.6 |
| Stabilise, review, CI run | 0.5 | 1 | 2 | 1.1 |
| **Total** | | | | **~5.5 days** (+10-15% risk buffer) |

State assumptions (stable UI, test data available) and re-estimate when they change.

## Prioritisation

Score each candidate test or defect by **business impact x likelihood of failure x change frequency**. Money paths (premium, claim approval) first. Use MoSCoW for the sprint backlog: Must (smoke, regression of changed areas), Should (new feature automation), Could (extra browsers), Won't now (rare screens).

## Team decisions (record them)

Keep short decision records: context, options, decision, consequences. Examples from this project:

1. Explicit waits only - predictable timeouts.
2. CSV data instead of Excel - diffable in Git; swap in `TestDataReader` if Excel is mandated.
3. API for test setup - speed and independence.
4. Retry UI tests once - protects the pipeline; retried passes are tracked.

## Defect escalation

| Severity | Example | Action |
|---|---|---|
| Critical | Claims over 50,000 auto-approved | Notify dev lead + product owner immediately; blocks release |
| High | Premium wrong for an age band | Same day triage; fix before release |
| Medium | Wrong message text | Next sprint unless it affects users' decisions |
| Low | Cosmetic | Backlog |

Escalation message: **What** (one line), **Impact** (who/how many/money), **Evidence** (steps, screenshot, API response, SQL), **Environment/build**, **Ask** (owner and by when), **Risk if not fixed**.

## Risks (sample register)

| Risk | Mitigation |
|---|---|
| Flaky UI tests erode trust | Quarantine policy, flakiness metric, root-cause owner |
| Test environment unstable or data missing | Health check stage, API-created data, environment owner named |
| Late requirement changes | Automate stable areas first, keep tests modular |
| Key-person dependency | Pairing, documentation, rotation |
| Pipeline too slow | API gate first, parallel UI, trim low-value UI tests |

## Cross-team coordination and process improvement

- RACI: QA lead (accountable for quality report), dev lead (defect fixes), DevOps (pipeline/agents), product owner (risk acceptance), business analyst (rules clarification).
- Rituals: three-amigos before development, daily defect triage, release readiness meeting with the metrics below.
- Metrics: pass rate, flakiness rate, defect leakage to production, automation coverage of regression, build duration, mean time to fix a red build.
- Improvements to propose: shift-left API tests in pull requests, definition of done includes automation, contract checks with dev teams, monthly flaky-test clean-up.
