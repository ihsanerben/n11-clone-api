# n11 Clone API

Spring Boot backend for a multi-seller e-commerce marketplace clone.

## Requirements

- Java 21
- Docker (integration tests)
- PostgreSQL (local application runtime)

## Local development

Create the local environment file once, then edit it with your credentials:

```bash
cp .env.example .env
```

Start PostgreSQL in Docker:

```bash
docker compose up -d
```

Then run the application. The script exports the variables from `.env` and
activates the Spring `local` profile:

```bash
./run-local.sh
```

Stop PostgreSQL while preserving its data with `docker compose down`. To also
delete the local database volume, use `docker compose down -v`.

The local profile defaults to:

- PostgreSQL: `jdbc:postgresql://localhost:5433/n11_clone`
- Database username/password: `postgres` / `postgres`
- API: `http://localhost:8081`
- Frontend origin: `http://localhost:5173`

`.env` is ignored by Git and must never be committed. `.env.example` documents
the required variables without containing real secrets. Production has no
default database credentials or allowed origin.

Auth and email delivery additionally require `JWT_SECRET`, `MAIL_USERNAME`,
`MAIL_PASSWORD`, `MAIL_FROM`, and `FRONTEND_BASE_URL`. Gmail SMTP uses a Google
App Password rather than the account's regular password. Optional lifetime settings are
`JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_DAYS`, and
`EMAIL_VERIFICATION_EXPIRATION_HOURS`, and `PASSWORD_RESET_EXPIRATION_MINUTES`.

## Tests

```bash
./mvnw test
./mvnw verify
```

`test` runs unit tests. `verify` also runs `*IT` integration tests against a real
PostgreSQL instance managed by Testcontainers.

## API

- Health: `GET /api/health`
- Swagger UI: `/swagger-ui.html`
- OpenAPI: `/v3/api-docs`

See `PROJECT_PLAN.md`, `DATABASE_SCHEMA.md`, and `BE-STANDARDS.md` for the full
scope and engineering rules.
