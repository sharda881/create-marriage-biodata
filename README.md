---
title: MadeToDevelop
emoji: 💍
colorFrom: pink
colorTo: red
sdk: docker
app_port: 7860
pinned: false
---

# Marriage Bio-Data Maker — REST API

This repository is the **backend** for the Marriage Bio-Data Maker. It is a stateless
Spring Boot JSON API (JWT auth). The user interface lives in a separate React (Vite + TS)
repository and talks to this API over HTTP.

## Running locally

```bash
JAVA_HOME=<path-to-jdk-21> mvn spring-boot:run
# API on http://localhost:8081  (dev profile, H2 file DB)
```

Seed accounts (dev profile): `admin@biodatamaker.app` / `Admin@123`,
`demo@biodatamaker.app` / `Demo@123`.

## Key environment variables

| Var | Purpose | Default |
|---|---|---|
| `JWT_SECRET` | HS256 signing secret (≥32 bytes) | dev placeholder |
| `JWT_EXPIRATION` | Access-token lifetime (ms) | `604800000` (7 days) |
| `FRONTEND_URL` | SPA origin — CORS + OAuth2 redirect target | `http://localhost:5173` |
| `APP_BASE_URL` | Public URL of this API (absolute photo/QR URLs) | `http://localhost:8081` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | enable Google login | disabled |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | PostgreSQL (prod profile) | — |

## API overview

- `POST /api/auth/register`, `POST /api/auth/login` → `{ token, user }`; `GET /api/auth/me`
- Google login: browser hits `GET /oauth2/authorization/google`; on success the API
  redirects to `${FRONTEND_URL}/oauth/callback#token=<jwt>`
- `/api/biodata` CRUD (+ `/photo`, `/template`, `/complete`, `/preview-data`,
  `/needs-payment`, `/download`) — create/read-by-id/update/preview/download allow
  anonymous callers
- `/api/templates` — design catalogue
- `/api/dashboard`, `/api/profile`
- `/api/payments/*` — UPI QR checkout, manual transaction submission, status polling
- `/api/admin/*` — payment verification, users, system config (`ROLE_ADMIN`)

PDFs are still rendered server-side (Thymeleaf templates in
`src/main/resources/templates/biodata/pdf/` + Playwright/Chromium).
