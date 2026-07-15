# n11 Clone API

Spring Boot backend for a multi-seller e-commerce marketplace clone.

## Requirements

- Java 21
- Docker (integration tests)
- PostgreSQL (local application runtime)

## Local development

Start PostgreSQL, then run the application with the local profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile defaults to:

- PostgreSQL: `jdbc:postgresql://localhost:5432/n11_clone`
- Database username/password: `postgres` / `postgres`
- Frontend origin: `http://localhost:5173`

All defaults can be overridden with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and
`CORS_ALLOWED_ORIGIN` environment variables. Production has no default database
credentials or allowed origin.

Auth and email delivery additionally require `JWT_SECRET`, `RESEND_API_KEY`,
`RESEND_FROM_EMAIL`, and `FRONTEND_BASE_URL`. Optional lifetime settings are
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
