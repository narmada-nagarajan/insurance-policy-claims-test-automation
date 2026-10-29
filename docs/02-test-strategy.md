# Test strategy

## What to automate (and what not)

| Automate | Do not automate (yet) |
|---|---|
| Stable, repeatable, high-risk paths (quote, underwriting, claim decisions) | One-off or rarely run checks |
| Business rules with many data combinations (API level, data providers) | Features still changing every sprint |
| Regression on every release; smoke on every commit | Visual/usability judgement, exploratory testing |
| Data/DB integrity checks | Third-party systems you cannot control (mock them) |

**Pyramid in this project:** 21 API runs + 3 DB checks + 8 UI tests. Rules are tested at API level (fast, stable); UI tests only prove the pages, the integration and the end-to-end journey.

## Regression planning

| Tier | When | Content | Target time |
|---|---|---|---|
| Smoke | every commit | `smoke.xml`: 1 UI happy path + API health | ~1-2 min |
| API + DB gate | every pull request | `api.xml` | < 1 min |
| Full regression | nightly + release candidate | `regression.xml` | ~10 min |
| Exploratory / manual | each release | new features, usability, edge cases | time-boxed |

Selection rule: new or changed area first, then high-risk (money: premium, claim approval), then the rest.

## Coverage matrix (workflow step x layer)

| Step | UI | API | DB |
|---|---|---|---|
| 1 Customer | TC_E2E_01-04, TC_UI_NEG_01, `customerForm_handlesDropdownFrameAndWindow` | `customer_createFetchAndUnknownId`, `duplicateEmail`, `invalidInput` x3, `deleteRules` | joins |
| 2 Quote | `quote_explicitWaitHandlesSlowPremium`, premium in E2E data | `quote_premiumIsCalculatedCorrectly` x3 | premium compare |
| 3 Policy | TC_E2E_03 (rejected), others issued | `policy_underwritingRules` x4, `secondPolicy`, `cancelThenClaim` | invariants (age 18-75) |
| 4 Claim | TC_E2E_01, 02, 04 | `claim_decisionRules` x4 (boundary 50,000 / 50,001) | no auto-approve > 50,000 |
| 5 Outcome | `ValidateOutcome` (UI + API + DB) | status checks | `joinAcrossAllTables`, `aggregation...` |

Techniques used: boundary values (50,000/50,001; age 17/18), equivalence classes (age bands), negative tests (400/404/409), state transitions (ISSUED -> CANCELLED -> claim rejected).

## Release readiness (go / no-go)

- 100% smoke and API+DB pass; regression >= 95% pass with every failure triaged (product bug, test bug, environment).
- No open Critical/High defects; Medium with an agreed workaround and owner.
- No test that "passes only on retry" without a ticket.
- New features have automated or explicitly accepted manual coverage.
- Residual risks listed and signed off by product owner.

## Test data and environments

Data is created per test through the API with unique emails; dates use placeholders so age rules never go stale. Environment values (`baseUrl`, browser, timeout) come from `-D` properties. Production-like data must be masked.
