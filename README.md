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
app.security.username=choose-a-username
app.security.password=choose-a-strong-password
```

`.env` and `application-local.properties` are gitignored. The application
refuses to start until the Basic-auth username and password are supplied.

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

## Security

HTTP Basic is enabled on every endpoint except `/actuator/health` and
`/actuator/info`. Credentials are required at startup (see
[Local configuration](#local-configuration)); there are no built-in defaults.

```bash
curl -u "$APP_SECURITY_USERNAME:$APP_SECURITY_PASSWORD" http://localhost:8080/api/accounts
```

## API

All paths are under `/api/accounts/{accountId}`.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/accounts` | Create an account |
| `GET` | `/api/accounts?status=&page=&size=` | List accounts (paged) |
| `GET` | `/api/accounts/{id}` | Get an account |
| `PUT` | `/api/accounts/{id}` | Update an account |
| `PATCH` | `/api/accounts/{id}/status` | Change account status |
| `DELETE` | `/api/accounts/{id}` | Delete an account (only when it has no history) |
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
curl -u "$APP_SECURITY_USERNAME:$APP_SECURITY_PASSWORD" -X POST http://localhost:8080/api/accounts \
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
curl -u "$APP_SECURITY_USERNAME:$APP_SECURITY_PASSWORD" -X POST http://localhost:8080/api/accounts/1/statements \
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
curl -u "$APP_SECURITY_USERNAME:$APP_SECURITY_PASSWORD" -X POST http://localhost:8080/api/accounts/1/reconciliations \
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
├── config/        # typed properties, security, demo seed
├── controller/    # REST endpoints
├── domain/        # JPA entities, enums, Money value rules
├── dto/           # request/response records
├── exception/     # API exceptions + RFC 9457 error handling
├── repository/    # Spring Data JPA repositories
├── service/       # service interfaces (contracts)
└── service/impl/  # service implementations (business logic)
```

## Configuration reference

| Property | Default | Purpose |
|----------|---------|---------|
| `DB_USERNAME` | — (required) | MySQL user |
| `DB_PASSWORD` | — (required) | MySQL password |
| `APP_SEED_ENABLED` | `true` | Load demo data on first start |
| `APP_SECURITY_USERNAME` | — (required) | Basic-auth user |
| `APP_SECURITY_PASSWORD` | — (required) | Basic-auth password |
| `app.cors.allowed-origins` | localhost:3000 / 5173 | Allowed browser origins |
| `server.port` | `8080` | HTTP port |

## Next steps

- Replace `ddl-auto=update` with Flyway or Liquibase migrations before production.
- Store users in the database and add role-based method security.
- Add statement CSV/OFX import and fuzzy matching (reference, description, split
  transactions) on top of the deterministic matcher.