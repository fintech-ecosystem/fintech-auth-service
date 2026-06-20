# fintech-auth-service

Authentication service for the FinTech Core Platform.

This service owns credentials, password hashes, roles, refresh tokens, login
attempts, and token issuing. It does not own customer profile, KYC, wallet,
ledger, payment, risk, or loan business logic.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/auth/register` | Register a new auth user |
| `POST` | `/auth/login` | Validate credentials and issue tokens |
| `POST` | `/auth/refresh` | Rotate refresh token and issue a new access token |
| `POST` | `/auth/logout` | Revoke a refresh token |
| `GET` | `/auth/me` | Return claims from the Bearer access token |
| `GET` | `/auth/ping` | Service smoke check |
| `GET` | `/actuator/health` | Health check |

## Local Database

`fintech-local-dev` creates the database used by this service:

```txt
host=localhost
port=5432
database=fintech_auth
username=fintech
password=fintech
```

Flyway creates:

- `auth_users`
- `roles`
- `user_roles`
- `refresh_tokens`
- `login_attempts`

## JWT/OAuth2 Flow

- `POST /auth/login` returns a short-lived HS256 JWT access token plus an opaque refresh token.
- Access tokens include `iss`, `sub`, `email`, `roles`, `scope`, and `token_use=access` claims.
- Refresh tokens are stored only as SHA-256 hashes and are rotated on every `/auth/refresh` call.
- `/auth/logout` revokes the supplied refresh token.
- `/auth/me` is protected by the OAuth2 resource server and only accepts valid access tokens.

## Run Locally

Build the common library once:

```bash
cd ../fintech-common-java
./mvnw install
```

Start infrastructure:

```bash
cd ../fintech-local-dev
docker compose up -d postgres
```

Run auth-service:

```bash
cd ../fintech-auth-service
./mvnw spring-boot:run
```

## Register

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: test-register-001" \
  -d '{"email":"customer1@example.com","password":"Password@123"}'
```

## Login

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: test-login-001" \
  -d '{"email":"customer1@example.com","password":"Password@123"}'
```

## Refresh

```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh-token>"}'
```

## Me

```bash
curl http://localhost:8081/auth/me \
  -H "Authorization: Bearer <access-token>"
```

## Through API Gateway

The gateway strips `/api`, so:

```txt
/api/auth/register -> /auth/register
/api/auth/login    -> /auth/login
```

## Docker Build

Build from the `fintech-ecosystem` root because this service depends on
`fintech-common-java`:

```bash
docker build -f fintech-auth-service/Dockerfile -t fintech-auth-service .
```
