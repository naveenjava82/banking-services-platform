# Banking Services Platform

Java 21 / Spring Boot backend implementing **all three** brief descriptions:

1. **Transaction Aggregation**: imports deterministic mock events from two sources, normalizes categories, deduplicates imports, paginates searches, and reports income, expenses, cash flow and category totals.
2. **Fraud Rule Engine**: configurable high-value debit, risk-merchant and transaction-velocity rules, with durable findings and idempotent rule assessment.
3. **Secure Statement Delivery**: validates and stores PDF statements, hashes stored contents, and generates customer-bound HMAC-signed, short-lived download URLs.

> The evaluation invitation says to **submit only one project**. This combined repository is a demonstration covering all three requests, not a recommendation to submit all three. For an actual application, select the one project matching the recruiter’s instructions or split the three domains into separate submissions as required.

## Quick start (Docker)

Requires Docker with Compose and an internet connection to download images/dependencies.

```bash
cp .env.banking .env
# Edit .env with strong local development passwords and a random signing secret
docker compose up --build
```

- API base URL: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health
- Authentication: HTTP Basic (demo-only identity provider; replace with OIDC/JWT for real deployment)
- Sample customer principals: `customer-1001`, `customer-1002` with values from `.env`

For local Java development, run `mvn clean test` then `mvn spring-boot:run` using Java 21. By default, the in-memory H2 database is used; the Docker profile uses PostgreSQL 16. Flyway creates the schema in both environments.

## Endpoints

All `/api/v1/**` endpoints require HTTP Basic authentication as a customer. Identity is derived **only** from the authenticated principal, not from client-supplied `customerId` parameters.

| Verb | Path | Operation |
|---|---|---|
| POST | `/transactions/sync` | Import events from two deterministic mock banks. Repeated calls do not duplicate transactions. |
| GET | `/transactions?from=...&to=...&page=0&size=20` | Customer-scoped paginated transactions, UTC ISO 8601 time range |
| GET | `/transactions/{id}` | Customer-owned transaction |
| GET | `/transactions/summary?currency=ZAR` | Aggregated income, expenses, cash flow and spending categories |
| GET | `/transactions/categories?currency=ZAR` | Category totals for debits |
| POST | `/fraud/assess-all` | Assess all customer transactions against configured rules |
| POST | `/fraud/assess/{transactionId}` | Assess a single customer-owned transaction |
| GET | `/fraud/findings` | Retrieve recorded rule matches |
| POST | `/statements` | Multipart PDF upload (`file` form field), up to 10 MiB |
| GET | `/statements` | List customer statements (metadata) |
| GET | `/statements/{id}` | Customer statement metadata |
| POST | `/statements/{id}/download-link?ttlSeconds=300` | Issue HMAC-signed path with expiration (max 900 seconds) |
| GET | `/statements/{id}/download?expires=...&signature=...` | Authenticated, ownership-checked PDF download |

## Example curl workflow

For Docker Compose using the `.env.banking` values, substitute the configured credentials:

```bash
curl -u customer-1001:YOUR_CUSTOMER1_PASSWORD -X POST http://localhost:8080/api/v1/transactions/sync
curl -u customer-1001:YOUR_CUSTOMER1_PASSWORD 'http://localhost:8080/api/v1/transactions/summary?currency=ZAR'
curl -u customer-1001:YOUR_CUSTOMER1_PASSWORD -X POST http://localhost:8080/api/v1/fraud/assess-all
curl -u customer-1001:YOUR_CUSTOMER1_PASSWORD http://localhost:8080/api/v1/fraud/findings
curl -u customer-1001:YOUR_CUSTOMER1_PASSWORD -F 'file=@statement.pdf;type=application/pdf' http://localhost:8080/api/v1/statements
# Replace UUID with the id from upload/list
curl -u customer-1001:YOUR_CUSTOMER1_PASSWORD -X POST 'http://localhost:8080/api/v1/statements/UUID/download-link?ttlSeconds=300'
# Use the resulting downloadPath (still requires Basic auth)
```

## Fraud rules

- `HIGH_AMOUNT`: negative transaction absolute amount greater than configured amount threshold.
- `HIGH_RISK_MERCHANT`: negative transaction at configured risk-list merchant.
- `HIGH_VELOCITY`: three or more debits within configured 10-minute lookback window, assessed at each transaction's timestamp.

Rules can be adjusted in `application.yml`. Rule matches are uniquely keyed by transaction and rule, so re-evaluation does not duplicate findings. The service assesses on request; importing alone does not automatically run fraud rules.

## Testing

```bash
mvn clean test
mvn clean package
```

Includes Spring MVC integration tests for authentication, customer isolation, idempotent sync, basic fraud detection and rejecting invalid PDF uploads. Extend with boundary-value, expiry, concurrency and persistence tests for a larger production assessment.

## Design and scope notes

- Amounts are `BigDecimal`; costs and income are aggregated **only within the requested currency**. Debit amounts are negative, credits positive. Currency conversion is intentionally out of scope.
- `BANK_ALPHA` and `BANK_BETA` are deterministic mock providers. In a production system each would be a dedicated adapter with network timeouts, backoff, circuit breaker and provider-specific mapping.
- The demo protects customer endpoints with HTTP Basic over **HTTPS when deployed behind a TLS proxy**. Do not expose this sample on public HTTP. There is no customer self-registration or password persistence.
- Files are stored in a persistent Docker volume by random UUID filename; metadata is stored in PostgreSQL. Files are syntax-checked by PDFBox and checked again with SHA-256 at download. Real-world systems should add malware scanning, object storage, retention policies and audit trail.
- A signed download URL is **not** a bearer-only public link: the owner must still authenticate. This adds protection against leaked URLs. Links expire, are HMAC-SHA256 signed, and are bound to authenticated owner and statement ID. Download URLs may appear in logs and browser history, so avoid logging query strings in production.
- Demo credentials and signing secret default values are **not suitable for deployment**. Rotate all demo secrets, use a secrets manager, enforce TLS, and replace the in-memory user store with an enterprise identity provider.
- Database transaction deduplication has both an application check and a unique constraint. Concurrent imports can still raise a duplicate-key race; production ingestion should use an atomic upsert and retry strategy.
- API payloads use straightforward entity serialization for brevity; a production change should introduce dedicated immutable response DTOs and versioning policies.
- No real bank accounts, payment rails or personally identifiable records are used.

## Java package and project coordinates

- Base package: `com.banking.platform`
- Maven coordinates: `com.banking:banking-services-platform:1.0.0`
- Environment template: `.env.banking` (copy to `.env` and replace placeholder secrets)

## Project structure

`transaction/` mock providers, categorization, transaction query and aggregation

`fraud/` rule processing and durable fraud findings

`statement/` PDF storage and cryptographically signed access

`config/` authentication and endpoint access control

`common/` error responses

## Important evaluation note

The code was authored in an environment without Maven or Docker installed and without access to Maven Central. **Compilation, automated tests and container startup have not been verified here**. Run the documented commands in a Java 21 + Maven + Docker environment and resolve any dependency or integration issues before submitting it as a production-grade technical evaluation.
