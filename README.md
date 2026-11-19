# Bank Ledger Reconciliation

Spring Boot service for reconciling an internal bank ledger against imported bank
statement lines. It provides CRUD for accounts and ledger entries, statement
import, and a deterministic matching engine that records each reconciliation
run for audit.

## Requirements

| Tool | Version |
|------|---------|
| Java | 17+ (JDK 21 also works) |
| MySQL | 8.0+ |
| Gradle | via the bundled wrapper (`./gradlew`) |

## Local configuration

No credentials are committed to the repository. Supply them as environment
variables, or in an untracked `application-local.properties` at the project
root. Both mechanisms override the values in
[`application.properties`](src/main/resources/application.properties).

```bash
cp .env.example .env            # edit the placeholder values
set -a; source .env; set +a     # export them into the shell
```

Or create `application-local.properties`:

```properties
spring.datasource.username=your-db-user
spring.datasource.password=your-db-password

app.jwt.secret=your-long-random-signing-secret

app.security.admin.username=choose-a-username
app.security.admin.password=choose-a-strong-password

# Additional users (optional). Roles: ADMIN, ACCOUNTANT, AUDITOR.
app.security.users[0].username=accountant
app.security.users[0].password=choose-a-strong-password
app.security.users[0].roles=ACCOUNTANT

app.security.users[1].username=auditor
app.security.users[1].password=choose-a-strong-password
app.security.users[1].roles=AUDITOR
```

`.env` and `application-local.properties` are gitignored. The application
refuses to start until the admin credentials and JWT signing secret are
supplied. Generate the secret with `openssl rand -base64 64`; it must be at
least 32 bytes.

## Database

The application expects a schema named `bank_ledger`:

```sql
CREATE DATABASE IF NOT EXISTS bank_ledger
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

The connection URL lives in
[`src/main/resources/application.properties`](src/main/resources/application.properties);
the username and password come from `DB_USERNAME` / `DB_PASSWORD` (see
[Local configuration](#local-configuration)).

Hibernate owns the schema (`spring.jpa.hibernate.ddl-auto=update`), so tables are
created on first start. Demo data is inserted when the table is empty; disable it
with `APP_SEED_ENABLED=false`.

## Build and run

```bash
./gradlew build          # compiles, runs tests, packages the jar
./gradlew bootRun        # start on http://localhost:8080
./gradlew test           # tests only (uses in-memory H2, no MySQL needed)
```

Tests run against H2 via the `test` profile, so `./gradlew build` does not need a
running MySQL server.

## Authentication and roles

Every endpoint except `/api/auth/login`, `/api/auth/refresh`,
`/actuator/health` and `/actuator/info` requires a JWT Bearer access token.

### Tokens

| Token | Lifetime | Notes |
|-------|----------|-------|
| Access | 15 minutes | Sent as `Authorization: Bearer <token>` |
| Refresh | 7 days | `jti` stored server-side; rotated and revoked on use |

Obtain tokens, then call the API:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"'"$APP_SECURITY_ADMIN_USERNAME"'","password":"'"$APP_SECURITY_ADMIN_PASSWORD"'"}'

export ACCESS_TOKEN=...   # accessToken from the response
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/accounts
```

Refresh tokens are single-use: `POST /api/auth/refresh` returns a new pair and
revokes the presented token. `POST /api/auth/logout` revokes the supplied
refresh token.

### Roles

| Role | Permissions |
|------|-------------|
| `ADMIN` | Everything, including deleting accounts, closing accounts, and reading the audit trail |
| `ACCOUNTANT` | Create/update accounts, post ledger entries, import statements, run reconciliations; cannot delete or close accounts |
| `AUDITOR` | Read-only across accounts, ledger, statements, reconciliations, and the audit trail |

Authorization is enforced with method security (`@PreAuthorize`), and closing
an account is additionally checked in the service layer.

### Rate limiting

Bucket4j applies per-instance token buckets:

| Scope | Default |
|-------|---------|
| `POST /api/auth/login` per IP | 5 requests / minute |
| Other `POST`/`PUT`/`PATCH`/`DELETE` under `/api` | 60 requests / minute per user (per IP when anonymous) |

Exceeding a limit returns `429 Too Many Requests` with a Problem Details body.
With multiple instances, move the limits to a shared store or the gateway.

### Audit trail

Every mutation is recorded in the append-only `audit_log` table with the actor,
action, entity, details and timestamp. Failed logins and token-refresh failures
are recorded too. `BankAccount`, `LedgerEntry`, `BankStatementEntry` and
`ReconciliationRun` also carry `created_by` / `updated_by` columns filled by
Spring Data JPA auditing.

`GET /api/audit` (ADMIN and AUDITOR only) supports filters:
`action`, `actor`, `entityType`, `entityId`, `from`, `to`, plus `page` and
`size`.

## API

Account-scoped paths are under `/api/accounts/{accountId}`; authentication and
audit endpoints are listed first.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Exchange username/password for access + refresh tokens |
| `POST` | `/api/auth/refresh` | Rotate a refresh token |
| `POST` | `/api/auth/logout` | Revoke a refresh token |
| `GET` | `/api/auth/me` | Current username and roles |
| `GET` | `/api/audit?action=&actor=&entityType=&entityId=&from=&to=` | Search the audit trail (ADMIN/AUDITOR) |
| `POST` | `/api/accounts` | Create an account |
| `GET` | `/api/accounts?status=&page=&size=` | List accounts (paged) |
| `GET` | `/api/accounts/{id}` | Get an account |
| `PUT` | `/api/accounts/{id}` | Update an account |
| `PATCH` | `/api/accounts/{id}/status` | Change account status (CLOSED is ADMIN-only) |
| `DELETE` | `/api/accounts/{id}` | Delete an account (ADMIN only; only when it has no history) |
| `POST` | `/{accountId}/ledger` | Post a ledger entry (updates the running balance) |
| `GET` | `/{accountId}/ledger?page=&size=` | List ledger entries |
| `GET` | `/{accountId}/ledger/{entryId}` | Get a ledger entry |
| `DELETE` | `/{accountId}/ledger/{entryId}` | Delete an unreconciled entry |
| `POST` | `/{accountId}/statements` | Bulk-import statement lines |
| `GET` | `/{accountId}/statements?status=&page=&size=` | List statement lines |
| `GET` | `/{accountId}/statements/{entryId}` | Get a statement line |
| `POST` | `/{accountId}/statements/{entryId}/exclude` | Exclude a line from reconciliation |
| `DELETE` | `/{accountId}/statements/{entryId}` | Delete a statement line |
| `POST` | `/{accountId}/reconciliations` | Run a reconciliation over a period |
| `GET` | `/{accountId}/reconciliations?page=&size=` | List reconciliation runs |
| `GET` | `/{accountId}/reconciliations/{runId}` | Get one reconciliation run |

### Example: create an account

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "accountNumber": "000987654321",
        "accountName": "Payroll Account",
        "bankName": "First National Bank",
        "branchName": "Main",
        "currency": "USD",
        "accountType": "CHECKING",
        "openingBalance": 5000.00
      }'
```

### Example: import statement lines

```bash
curl -X POST http://localhost:8080/api/accounts/1/statements \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "entries": [
          {"statementDate": "2026-01-10", "reference": "INV-1001",
           "description": "Customer payment", "type": "DEBIT", "amount": 2500.00}
        ]
      }'
```

### Example: run reconciliation

```bash
curl -X POST http://localhost:8080/api/accounts/1/reconciliations \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"periodStart": "2026-01-01", "periodEnd": "2026-01-31"}'
```

## Money handling

All monetary values are `BigDecimal` with a fixed scale of 4 decimal places and
`RoundingMode.HALF_UP`, defined once in
[`domain/Money.java`](src/main/java/com/banking/ledger/reconciliation/domain/Money.java).
`double` / `float` are never used for amounts or balances.

## Matching rules

A statement line is matched to an unused ledger entry when:

1. the entry type is identical (`DEBIT` / `CREDIT`), and
2. the amount is identical after normalising both sides to 4 decimal places
   (`HALF_UP`), and
3. the dates are within ±3 days.

When several ledger entries qualify, the closest by date wins. The run records
matched/unmatched counts, statement debit and credit totals, and the net
difference between the statement and the matched ledger lines. Unmatched lines
stay `UNMATCHED` for review; use the `exclude` endpoint to remove lines from
future runs.

## Project layout

```
src/main/java/com/banking/ledger/reconciliation/
├── config/        # typed properties, security, JPA auditing, demo seed
├── controller/    # REST endpoints
├── domain/        # JPA entities, enums, Money value rules
├── dto/           # request/response records
├── exception/     # API exceptions + RFC 9457 error handling
├── repository/    # Spring Data JPA repositories
├── security/      # JWT service/filter, rate limiting, current-user helpers
├── service/       # service interfaces (contracts)
└── service/impl/  # service implementations (business logic)
```

## Configuration reference

| Property | Default | Purpose |
|----------|---------|---------|
| `DB_USERNAME` | — (required) | MySQL user |
| `DB_PASSWORD` | — (required) | MySQL password |
| `APP_SEED_ENABLED` | `true` | Load demo data on first start |
| `APP_JWT_SECRET` | — (required, 32+ bytes) | JWT signing key |
| `APP_JWT_ISSUER` | `bank-ledger` | JWT `iss` claim |
| `APP_SECURITY_ADMIN_USERNAME` | — (required) | Primary admin username |
| `APP_SECURITY_ADMIN_PASSWORD` | — (required) | Primary admin password |
| `APP_SECURITY_USERS_0_USERNAME` etc. | — | Additional users via env vars (or `app.security.users[...]` in local properties) |
| `APP_RATE_LIMIT_LOGIN_CAPACITY` | `5` | Login attempts per IP per period |
| `APP_RATE_LIMIT_WRITE_CAPACITY` | `60` | Write requests per user/IP per period |
| `app.jwt.access-token-ttl` | `PT15M` | Access-token lifetime |
| `app.jwt.refresh-token-ttl` | `P7D` | Refresh-token lifetime |
| `app.rate-limit.login.period` | `PT1M` | Login limit window |
| `app.rate-limit.write.period` | `PT1M` | Write limit window |
| `app.cors.allowed-origins` | localhost:3000 / 5173 | Allowed browser origins |
| `server.port` | `8080` | HTTP port |

## Next steps

- Replace `ddl-auto=update` with Flyway or Liquibase migrations before production.
- Store users in the database (with hashed passwords) instead of typed configuration.
- Move rate-limit buckets to a shared store (Redis) when running multiple instances.
- Add token revocation for access tokens (for example a short-lived `jti` denylist).
- Add statement CSV/OFX import and fuzzy matching (reference, description, split
  transactions) on top of the deterministic matcher.