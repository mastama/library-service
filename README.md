# Library Service – ITSEC Backend Technical Test

This repository contains my submission for the **ITSEC Backend Technical Test**. It demonstrates secure API design with MFA (OTP email), JWT-based authentication with refresh tokens, account lockout, request rate limiting, RBAC, and full audit logging across key resources.

> **Demo stack:** Java 21 • Spring Boot 3 • Spring Security • Spring Data JPA • PostgreSQL • (Optional) Redis • springdoc-openapi

---

## ✨ Features

* **Authentication & MFA**

  * Login with **username or email + password**
  * **OTP via email** (request & login endpoints)
  * **Refresh token** flow & **Logout** (access token must be present in `Authorization: Bearer <token>`)
* **Security controls**

  * **Rate limit** endpoints (collections provided to test)
  * **Account lockout:** 5 failed attempts in 10 minutes → block for 30 minutes
  * **RBAC**

    * **SUPER\_ADMIN**: CRUD users, CRUD any article, access all audit logs
    * **EDITOR**: CRUD own articles, read others
    * **CONTRIBUTOR**: Create & update own articles
    * **VIEWER**: Read only
* **Resources**

  * **Users** (CRUD, set role)
  * **Articles** (CRUD)
  * **Library** (CRUD)
  * **Audit Logs**: every significant action (CRUD, login, etc.) with timestamp & device/browser data
* **Documentation & Testing**

  * **OpenAPI/Swagger** (`itsec.openapi.yml`)
  * **Postman collections & environment**
  * **Coverage snapshot** (`coveragetest.pdf`)

---

## 🧱 Architecture

* **Hexagonal (Ports & Adapters) + CQRS (lightweight)**

  * **domain/**: entities, value objects, repository ports, domain services
  * **application/**: use cases (commands/queries), DTOs, validators
  * **infrastructure/**

    * **persistence** (JPA repositories, mappings)
    * **security** (JWT/filters, password encoders, rate limiter, lockout)
    * **web** (controllers, exception handlers, request context)
    * **config** (OpenAPI, properties)

> Package root: `com.yolifay.libraryservice` (e.g., `infrastructure.web`, `domain.service`, etc.)

---

## 🚀 Quick Start

### Prerequisites

* JDK 21
* Maven 3.9+
* Docker (for local PostgreSQL / Redis)

### Environment

Copy the example and adjust the values:

```bash
cp .env.example .env
```

Common variables (adjust to your setup):

```properties
# Database
DB_HOST=localhost
DB_PORT=15432
DB_NAME=library
DB_USER=postgres
DB_PASS=postgres

# JWT
JWT_SECRET=replace-with-256bit-secret
JWT_ISSUER=library-service
JWT_ACCESS_EXPIRATION_MINUTES=60
JWT_REFRESH_EXPIRATION_MINUTES=10080

# OTP & Auth
OTP_TTL_SECONDS=300
OTP_RESEND_THROTTLE_SECONDS=60
AUTH_MAX_FAILED=5
AUTH_WINDOW_MINUTES=10
AUTH_LOCK_MINUTES=30

# Rate limit (example defaults)
RATELIMIT_DEFAULT_REQUESTS=20
RATELIMIT_DEFAULT_WINDOW_SECONDS=60
```

> The application reads from Spring `application-*.yml/properties`; map the above accordingly.

### Start dependencies (Docker)

If you use the provided `docker-compose.yml` (or your own):

```bash
docker compose up -d
```

This typically provides **PostgreSQL** (and optionally **Redis** for token blacklist, counters, OTP, etc.).

### Run the service

```bash
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

Default port: **`8088`** (change in configuration if needed).

---

## 📘 API Documentation (Swagger)

* **Swagger UI (local):** `http://localhost:8088/swagger-ui/index.html` or `/swagger-ui.html`
* **OpenAPI JSON:** `http://localhost:8088/v3/api-docs`
* **OpenAPI file (attached):** `itsec.openapi.yml`
  *(Import into [https://editor.swagger.io](https://editor.swagger.io) for the interactive UI)*

> springdoc is configured to emit **OpenAPI 3.0.x** to maintain validator compatibility.

---

## 🔐 Authentication Flow

### Register

`POST /api/v1/auth/register`

```json
{
  "fullName": "Jane Doe",
  "username": "jane",
  "email": "jane@example.com",
  "password": "Secret123!",
  "role": "VIEWER"
}
```

### Request OTP

`POST /api/v1/auth/request-otp`

```json
{ "usernameOrEmail": "jane@example.com" }
```

### Login (password)

`POST /api/v1/auth/login`

```json
{ "usernameOrEmail": "jane@example.com", "password": "Secret123!" }
```

### Login (OTP)

`POST /api/v1/auth/login-otp`

```json
{ "usernameOrEmail": "jane@example.com", "password": "Secret123!", "otp": "123456" }
```

### Refresh token

`POST /api/v1/auth/refresh`

```json
{ "refreshToken": "<refresh-token>" }
```

### Logout

* Requires **Bearer access token** in header
* Requires **refresh token** in body

`POST /api/v1/auth/logout`

```http
Authorization: Bearer <access-token>
```

```json
{ "refreshToken": "<refresh-token>" }
```

Response:

```json
{ "accessRevoked": true, "refreshRevoked": true }
```

---

## 👥 RBAC Matrix (summary)

| Role         | Users CRUD | Articles (any)                        | Articles (own) | Audit Logs |
| ------------ | ---------- | ------------------------------------- | -------------- | ---------- |
| SUPER\_ADMIN | ✅          | ✅                                     | ✅              | ✅          |
| EDITOR       | ❌          | ✅ (create/update on own; read others) | ✅              | ❌          |
| CONTRIBUTOR  | ❌          | ❌ (read)                              | ✅              | ❌          |
| VIEWER       | ❌          | ❌ (read only)                         | ❌              | ❌          |

> Authorization checks are enforced via Spring Security method/URL rules.

---

## 🧪 Postman Collections

Import the following (provided in the root or `/docs`):

* `ITSEC – Full (Auth + OTP + RBAC + Audit).postman_collection.json`
* `ITSEC – RateLimit Tests.postman_collection.json`
* `ITSEC – local.postman_environment.json`

Order suggestion:

1. Register → Request OTP → Login (password/OTP) → Refresh → Logout
2. CRUD Users/Articles/Library based on role
3. Audit Logs read (`GET /api/v1/audit-logs`)
4. Run **RateLimit** suite to see 429 responses

---

## 🧾 Endpoints (high-level)

* **Auth:** `/api/v1/auth/register`, `/login`, `/login-otp`, `/request-otp`, `/refresh`, `/logout`
* **Users:** `/api/v1/users` (GET, POST), `/api/v1/users/{id}` (GET, PUT, DELETE), `/api/v1/users/{id}/role` (PATCH)
* **Articles:** `/api/v1/articles` (GET, POST), `/api/v1/articles/{id}` (GET, PUT, DELETE)
* **Library:** `/api/v1/library` (GET, POST), `/api/v1/library/{id}` (GET, PUT, DELETE)
* **Audit Logs:** `/api/v1/audit-logs` (GET, paging via `page`, `size`)

See the **OpenAPI** file for detailed request/response schemas.

---

## 🛡️ Security Notes

* **OpenAPI** emits `security: [{ bearerAuth: [] }]` for protected endpoints (no manual `Authorization` header parameter declared).
* **Account lockout** & **rate limit** windows are configurable via properties.
* **Logout** revokes both access & refresh tokens (revocation storage can use DB/Redis as configured).

---

## 🧰 Development

### Build & Test

```bash
mvn clean verify
# or
mvn test
```

* Coverage report: `target/site/jacoco/index.html`
* Snapshot included: `coveragetest.pdf`

### Static analysis (optional)

If enabled, you can run Checkstyle/SpotBugs via Maven plugins.

---

## 🗃️ Seed Data

You can insert an initial **SUPER\_ADMIN** manually for quick testing:

```sql
-- Example only – adjust table/columns to your schema
INSERT INTO users (full_name, username, email, password_hash, role)
VALUES ('Admin', 'admin', 'admin@example.com', '<bcrypt-hash>', 'SUPER_ADMIN');
```

Generate a bcrypt hash using your preferred tool/library.

---

## 📦 Deliverables in this submission

* `itsec.openapi.yml`
* `ITSEC – Full (Auth + OTP + RBAC + Audit).postman_collection.json`
* `ITSEC – RateLimit Tests.postman_collection.json`
* `ITSEC – local.postman_environment.json`
* `coveragetest.pdf`

---

## 🐞 Known Limitations / Next Steps

* Email/OTP delivery may be stubbed or logged for local development
* Production-grade token revocation, key rotation, and multi-tenant concerns are out of scope
* Add integration tests & contract tests for critical flows

---

## 📄 License

MIT (see `LICENSE`).

---

**Author**: Singgih Pratama
**Contact**: please see repository profile/commits.
