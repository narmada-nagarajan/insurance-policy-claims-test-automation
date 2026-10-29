# Two-day practice plan

Rule: run it, break it, fix it, explain it aloud in two minutes.

## Day 1 - build and understand

| Block | Do |
|---|---|
| 1 Setup (30 min) | `mvn test -DsuiteFile=api.xml`, then `-DsuiteFile=smoke.xml`. Fix environment issues. |
| 2 Selenium core (90 min) | Read `BasePage`, `CustomerPage`, `QuotePage`, `ClaimPage`. Re-type one page object from scratch. Explain frame, window, dropdown, stale element. |
| 3 Break things (45 min) | Replace an explicit wait with `Thread.sleep`, then remove it; change a locator; watch the exception and screenshot. |
| 4 Framework (60 min) | Add a CSV scenario: CUST_SENIOR_60 + HOME quote + claim. Run via `run_manager.csv`. Explain keyword/data/modular. |
| 5 API + DB (60 min) | Write one new `@Test` (e.g. claim on cancelled policy) and one SQL join. Use `06-api-sql-cheatsheet.md`. |

## Day 2 - operate and lead

| Block | Do |
|---|---|
| 1 CI/CD (45 min) | Read the `Jenkinsfile`; explain each stage, parameters, cron, `post` actions. Practise: `git branch`, commit, PR, merge, resolve a conflict. |
| 2 Troubleshooting (60 min) | Walk through `03-troubleshooting.md`; for each table row give one example from this code. |
| 3 Strategy (45 min) | Explain what you automated and what you did not, release readiness, regression tiers (`02-test-strategy.md`). |
| 4 Leadership and stakeholders (60 min) | Prepare 4 STAR stories from your real work: review, mentoring, escalation, process improvement. Use `04-...`. |
| 5 Mock interview (60 min) | Answer aloud: 30-second pitch, explicit vs implicit vs fluent wait, POM benefits, flaky test handling, status codes, INNER vs LEFT JOIN, how to estimate, how to escalate a critical defect. |

## Be honest in the interview

Present this as a reference framework you built to demonstrate the approach; describe your hands-on and review experience accurately and show you can learn and lead from it.
