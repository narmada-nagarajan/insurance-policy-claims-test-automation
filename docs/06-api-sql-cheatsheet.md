# API and SQL cheatsheet (with this project's endpoints)

## HTTP methods and status codes used

| Method | Endpoint | Success | Errors in tests |
|---|---|---|---|
| GET | `/api/health`, `/api/customers/{id}`, `/api/quotes/{id}`, `/api/policies/{id}`, `/api/claims/{id}`, `/api/reports/claims-summary` | 200 | 404 unknown id |
| POST | `/api/customers`, `/api/quotes`, `/api/policies`, `/api/claims` | 201 | 400 invalid input, 409 duplicate |
| PUT | `/api/policies/{id}/cancel` | 200 | 409 not ISSUED |
| DELETE | `/api/customers/{id}` | 204 | 409 has quotes, then 404 after delete |

Meaning: 200 OK, 201 Created, 204 No Content, 400 Bad Request (client data), 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 500 Server Error. PUT replaces/updates and is idempotent; POST creates and is not.

## REST Assured pattern

```java
given().baseUri(url).contentType(JSON).body(map)
.when().post("/api/customers")
.then().statusCode(201).body("email", equalTo(email));
long id = response.jsonPath().getLong("id");
```
Also see JSON array filtering: `getList("findAll { it.policyId == 5 }")` in `ApiTests`.

## SQL to practise (schema: customers, quotes, policies, claims)

```sql
-- INNER JOIN: policy with customer and premium
SELECT c.full_name, q.coverage_type, q.premium, p.status
FROM customers c JOIN quotes q ON q.customer_id = c.id
JOIN policies p ON p.quote_id = q.id;

-- LEFT JOIN: every policy, with claims if any
SELECT p.id, cl.id AS claim_id, cl.status
FROM policies p LEFT JOIN claims cl ON cl.policy_id = p.id;

-- Policies WITHOUT claims
SELECT p.id FROM policies p LEFT JOIN claims cl ON cl.policy_id = p.id WHERE cl.id IS NULL;

-- GROUP BY + HAVING: policies with more than one claim
SELECT policy_id, COUNT(*) AS n, SUM(amount) AS total
FROM claims GROUP BY policy_id HAVING COUNT(*) > 1;

-- Subquery: customers whose total claimed is above average claim
SELECT DISTINCT p.customer_id FROM policies p JOIN claims cl ON cl.policy_id = p.id
WHERE cl.amount > (SELECT AVG(amount) FROM claims);

-- Duplicate emails (should return none because of UNIQUE)
SELECT email, COUNT(*) FROM customers GROUP BY email HAVING COUNT(*) > 1;

-- Second highest claim
SELECT MAX(amount) FROM claims WHERE amount < (SELECT MAX(amount) FROM claims);
```
Know: INNER vs LEFT vs RIGHT vs FULL, WHERE vs HAVING, DELETE vs TRUNCATE, primary vs foreign key, PreparedStatement (parameters, no injection).
