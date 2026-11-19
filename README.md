# TaskFlow – Collaborative Project & Task Management

[![Live Demo](https://img.shields.io/badge/Live-Demo-2ea44f?style=flat-square)](https://taskflow-delta-henna.vercel.app)
![Database](https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
[![Backend CI](https://github.com/Akash444-2004/taskflow/actions/workflows/backend.yml/badge.svg?branch=main)](https://github.com/Akash444-2004/taskflow/actions/workflows/backend.yml)
[![Frontend CI](https://github.com/Akash444-2004/taskflow/actions/workflows/frontend.yml/badge.svg?branch=main)](https://github.com/Akash444-2004/taskflow/actions/workflows/frontend.yml)

🌐 **Live Application:** https://taskflow-delta-henna.vercel.app

TaskFlow is a production-deployed, full-stack project and task management application for small teams: a Spring Boot API, a PostgreSQL database, and a responsive React single-page application.

> **Status: all ten phases complete.** Registration, authentication, projects, membership, tasks, comments, dashboards, administration, the full React interface, integration tests, application-level rate limiting, and the Neon/Render/Vercel deployment configuration are implemented. See [Verification results](#verification-results).

## Features

**Accounts and security**

- Registration, login, logout, and session bootstrap with short-lived JWT access tokens.
- Rotating, hashed, revocable refresh sessions stored in PostgreSQL.
- CSRF-protected cookie endpoints, BCrypt password hashing, password change with global session revocation.
- Server-side role and project authorization on every request; JWT claims are never trusted for permissions.
- Application-level rate limiting on login, registration, password change, and all API routes.

**Projects and collaboration**

- Create projects with an owner, status, schedule, and description.
- Owner-only project editing, deletion, and member management.
- Members can view the project, work on tasks, comment, and leave voluntarily.
- Search, status filtering, pagination, member counts, and task progress.

**Tasks**

- Kanban board and table views with priority, assignee, due date, search, and filters.
- Any project member can create or edit tasks; assignments are restricted to project members.
- Enforced status flow: `TODO → IN_PROGRESS → COMPLETED`, with reopening from `COMPLETED` and returning to `TODO` from `IN_PROGRESS`.
- Task deletion limited to the project owner or task creator.
- "My tasks" view across every project, including overdue indicators.

**Comments, dashboards, and administration**

- Task-scoped comment threads. Authors, project owners, and global administrators can delete comments.
- Personal dashboard with assigned task counts, overdue and due-soon work, status distribution chart, upcoming deadlines, and recent projects.
- Admin console with platform statistics, account search, and enable/disable controls. Disabling an account revokes its refresh sessions and blocks future logins.

**Interface**

- Responsive application shell with desktop and mobile navigation.
- Light and dark themes persisted per device, with no flash of the wrong theme.
- Reusable UI primitives, accessible dialogs, toast notifications, loading and empty states.
- Lazy-loaded routes and a clean production build.

## Architecture

```text
Browser
  │
  │ HTTPS / JSON / short-lived bearer token
  ▼
React 19 SPA (Vite, Tailwind, React Router)
  │  /api
  ▼
Spring Boot 4.1 monolith
  ├── Controller → Bean Validation → Service → Repository
  ├── Rate-limit filter (Bucket4j, per-IP)
  ├── Spring Security / JWT filter / CSRF chain
  ├── Flyway migrations
  └── JPA / Hibernate
        │
        ▼
PostgreSQL 18
```

This is deliberately a modular monolith with a separate SPA. There are no microservices, message brokers, caches, or distributed-system components.

## Technology

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA, Hibernate 7, Spring Security 7 |
| Database | PostgreSQL 18, Flyway |
| Authentication | BCrypt (strength 12), HS512 JWT access tokens, rotating opaque refresh tokens |
| Frontend | React 19, Vite 8, JavaScript, Tailwind CSS 3.4, React Router 7, Axios, Recharts, Lucide |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Vitest, React Testing Library |
| Security | Bucket4j token-bucket rate limiting, Spring Security, CSRF, BCrypt |
| Build | Maven Wrapper, npm, Docker (Render), GitHub Actions |

## Repository Layout

```text
taskflow/
├── backend/
│   ├── src/main/java/com/taskflow/
│   │   ├── config/          # Security, CORS, CSRF, JWT, cookie and rate-limit properties
│   │   ├── controller/      # Thin REST controllers
│   │   ├── dto/             # Request/response records grouped by feature
│   │   ├── entity/          # JPA entities
│   │   ├── enums/           # Roles, project/task status, priorities
│   │   ├── exception/       # Domain exceptions and global error handler
│   │   ├── repository/      # Spring Data repositories and projections
│   │   ├── security/        # JWT filter, rate limiter, refresh cookies, entry points
│   │   └── service/         # Business logic and authorization
│   ├── src/main/resources/
│   │   ├── db/migration/    # V1 schema, V2 single-owner constraint
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── src/test/java/com/taskflow/
│   │   ├── admin/           # Admin + dashboard integration tests
│   │   ├── comment/         # Comment permissions
│   │   ├── persistence/     # Schema and repository integration tests
│   │   ├── project/         # Project and membership rules
│   │   ├── security/        # Authentication and session security
│   │   └── task/            # Task rules and transitions
│   ├── Dockerfile           # Render builds this image
│   ├── .dockerignore
│   ├── .env.example
│   └── pom.xml
├── frontend/
│   ├── public/              # Favicon and pre-paint theme bootstrap
│   ├── src/
│   │   ├── components/      # Layout, projects, tasks, UI primitives
│   │   ├── context/         # Auth, theme, toast providers
│   │   ├── hooks/           # Count-up animation hook
│   │   ├── pages/           # Dashboard, projects, tasks, profile, admin, auth
│   │   ├── services/        # Axios client, refresh interceptor, CSRF helpers
│   │   ├── test/            # Vitest setup
│   │   ├── utils/           # Formatting and enum labels
│   │   ├── App.jsx
│   │   ├── index.css
│   │   └── main.jsx
│   ├── vercel.json          # Vercel: /api proxy to Render + SPA fallback
│   └── package.json
├── .github/workflows/       # Backend and frontend CI pipelines
├── scripts/                 # Password-safe local run helpers
├── .vscode/tasks.json       # One-click backend/frontend tasks
├── render.yaml              # Render blueprint for the API
└── README.md
```

## Prerequisites

- Java 21
- Node.js 26 LTS (or a Vite-compatible supported Node release)
- PostgreSQL 15+ for local development (PostgreSQL 18 is used in verification)
- Git
- Docker is only needed to build the backend image locally; Render builds it for you

Maven does not need to be installed globally; the Maven Wrapper is included.

## Local Development

### 1. Database

Development uses your local PostgreSQL instance on `localhost:5432` (database `taskflow`, user `postgres`). Create the database once:

```sql
CREATE DATABASE taskflow;
```

The backend reads credentials from environment variables or from `backend/.env`, which is git-ignored. The run scripts prompt for the password, so it is never written to disk.

Integration tests use a separate `taskflow_test` database (see below) so real data is never touched.

### 2. Run the stack

Your PostgreSQL database is used directly (`localhost:5432`, database `taskflow`, user `postgres`). No local files ever store the database password.

**From VS Code (recommended):** open the Command Palette (`Ctrl+Shift+P`) → **Tasks: Run Task** → pick one of:

| Task | What it does |
| --- | --- |
| `TaskFlow: Run Backend` | Prompts for your PostgreSQL password in the quick input, then starts the API on `http://localhost:8080` |
| `TaskFlow: Run Frontend` | Starts the SPA on `http://localhost:5173` |
| `TaskFlow: Run Full Stack` | Asks for the password once, runs pre-flight checks (PostgreSQL reachable, ports free, dependencies), then starts the backend and the frontend each in its own window and waits until the API reports healthy |

**From a terminal:**

```powershell
npm run backend      # prompts (masked) for the PostgreSQL password
npm run frontend     # or:
npm run fullstack    # backend + frontend together
```

**Stopping:** run `scripts\stop-all.ps1` (or `npm run` equivalent) — it stops whatever is listening on ports 8080 and 5173 and never touches PostgreSQL. Output of both processes is also appended to `.dev\backend.out.log` and `.dev\frontend.out.log` for troubleshooting.

Credential handling:

- The database password is prompted at runtime (masked) and kept only in process memory; it is never written to disk.
- The JWT signing secret is generated on first run and stored **DPAPI-encrypted** in `.dev/jwt.secret`, readable only by your Windows user on this machine.
- The API starts on `http://localhost:8080`. Flyway applies migrations automatically on startup.

Useful endpoints:

- Web app: `http://localhost:5173`
- API: `http://localhost:8080`
- Health: `GET http://localhost:8080/api/health`

### Integration tests

Backend tests run against a separate `taskflow_test` database so your real data is never touched. Create it once:

```sql
CREATE DATABASE taskflow_test;
```

Then run with your password supplied through the environment:

```powershell
cd backend
$secure = Read-Host "PostgreSQL password" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
.\mvnw.cmd test
```

## Environment Variables

### Backend

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/taskflow` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | required | Database password |
| `DB_POOL_MAX_SIZE` | `10` (local), `5` recommended on Neon | Hikari maximum pool size |
| `DB_POOL_MIN_IDLE` | `2` (`1` on Render) | Hikari minimum idle connections |
| `PORT` | `8080` | HTTP port injected by Render (`SERVER_PORT` also honoured) |
| `FRONTEND_URL` | `http://localhost:5173` | Allowed CORS origin(s), comma-separated |
| `JWT_SECRET` | required | Base64 secret, at least 512 bits |
| `JWT_ISSUER` | `taskflow-api` | JWT issuer claim |
| `JWT_ACCESS_TTL` | `PT15M` | Access token lifetime |
| `JWT_REFRESH_TTL` | `P7D` | Refresh session lifetime |
| `COOKIE_SECURE` | `false` (local), `true` (prod profile) | Sets `Secure` on auth cookies |
| `REFRESH_COOKIE_NAME` | `taskflow_refresh` | Refresh cookie name |
| `REFRESH_COOKIE_PATH` | `/api/auth` | Refresh cookie path |
| `RATE_LIMIT_ENABLED` | `true` | Master switch for application rate limiting |
| `RATE_LIMIT_GLOBAL_PER_MINUTE` | `120` | Requests per minute per IP for all other `/api/*` routes |
| `RATE_LIMIT_LOGIN_PER_MINUTE` | `10` | Login attempts per minute per IP |
| `RATE_LIMIT_REGISTER_PER_MINUTE` | `3` | Registrations per minute per IP |
| `RATE_LIMIT_PASSWORD_CHANGE_PER_MINUTE` | `5` | Password changes per minute per IP |
| `RATE_LIMIT_TRUST_FORWARDED_FOR` | `true` | Use the first `X-Forwarded-For` hop (correct behind Render's proxy) |

### Frontend

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_URL` | `/api` in the container build | Public API base URL; never place secrets in `VITE_*` |

### Deployment secrets

Production values live in the Render dashboard, never in the repository: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL`, and the rate-limit overrides if you need them. `JWT_SECRET` is generated by `render.yaml` on first deploy.

## API Reference

All responses are JSON except `204 No Content` responses. Errors use a consistent payload with `timestamp`, `status`, `code`, `message`, `path`, and optional `fieldErrors`.

### Authentication and profile

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create an account and start a session |
| `POST` | `/api/auth/login` | Public | Authenticate and start a session |
| `GET` | `/api/auth/csrf` | Public | Issue a CSRF cookie/token |
| `POST` | `/api/auth/refresh` | Refresh cookie + CSRF | Rotate the refresh token and issue a new access token |
| `POST` | `/api/auth/logout` | Refresh cookie + CSRF | Revoke the session and clear the cookie |
| `GET` | `/api/users/me` | Authenticated | Current profile |
| `PUT` | `/api/users/me` | Authenticated | Update name and phone number |
| `POST` | `/api/users/me/password` | Authenticated | Change password and revoke all sessions |

### Projects and members

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/projects` | Authenticated | List accessible projects with `status`, `search`, `page`, `size`, `sort` |
| `POST` | `/api/projects` | Authenticated | Create a project; creator becomes owner |
| `GET` | `/api/projects/{id}` | Project member | Project details and counts |
| `PUT` | `/api/projects/{id}` | Project owner | Update project |
| `DELETE` | `/api/projects/{id}` | Project owner | Delete project, tasks, comments, and memberships |
| `GET` | `/api/projects/{id}/members` | Project member | List members |
| `POST` | `/api/projects/{id}/members` | Project owner | Add an existing account by email |
| `DELETE` | `/api/projects/{id}/members/me` | Project member (not owner) | Leave project |
| `DELETE` | `/api/projects/{id}/members/{userId}` | Project owner | Remove a member |

### Tasks

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/projects/{id}/tasks` | Project member | Filter by `status`, `priority`, `assignedToId`, `search`; paginated |
| `POST` | `/api/projects/{id}/tasks` | Project member | Create task |
| `GET` | `/api/tasks/assigned-to-me` | Authenticated | Current user's assigned tasks, optional `status` |
| `GET` | `/api/tasks/{id}` | Project member | Task details |
| `PUT` | `/api/tasks/{id}` | Project member | Update task; status changes must follow the transition rules |
| `PATCH` | `/api/tasks/{id}/status` | Project member | Move task through an allowed transition |
| `DELETE` | `/api/tasks/{id}` | Project owner or creator | Delete task and comments |

### Comments, dashboard, and administration

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/tasks/{id}/comments` | Project member | List comments |
| `POST` | `/api/tasks/{id}/comments` | Project member | Add comment |
| `DELETE` | `/api/comments/{id}` | Author, project owner, or admin | Delete comment |
| `GET` | `/api/dashboard` | Authenticated | Personal counts, status chart data, upcoming tasks, recent projects |
| `GET` | `/api/admin/users` | Admin | Search and page accounts |
| `PATCH` | `/api/admin/users/{id}/enabled` | Admin | Enable or disable an account; cannot disable self |
| `GET` | `/api/admin/stats` | Admin | Platform totals |
| `GET` | `/api/health` | Public | Liveness check |

## Authorization Model

| Capability | Rule |
| --- | --- |
| View project or tasks | Owner or project member |
| Edit/delete project, add/remove members | Project owner |
| Leave project | Any member except the owner |
| Create/edit task, move status, comment | Any project member |
| Assign task | Assignee must be an enabled project member |
| Delete task | Project owner or task creator |
| Delete comment | Comment author, project owner, or global admin |
| Profile and password | Current user only |
| Admin endpoints | `ADMIN` role loaded from the database on every request |

Security is enforced in services and Spring Security, never only in the React interface. A stale or tampered JWT does not grant access because the user is reloaded from the database for each request.

## Security Design

- Access tokens are short-lived (15 minutes by default) and kept in frontend memory, not `localStorage`.
- Refresh tokens are opaque, hashed with SHA-256 at rest, rotated on every use, and revoked on reuse detection, logout, password change, or account disable.
- The refresh cookie is `HttpOnly`, `SameSite=Strict`, scoped to `/api/auth`, and optionally `Secure`.
- `/api/auth/refresh`, `/api/auth/logout`, and `/api/auth/csrf` run on a dedicated CSRF-protected chain using an `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header.
- CORS allows only configured origins and credentials; other origins are rejected.
- Passwords use BCrypt strength 12, with a 72-byte UTF-8 limit enforced before hashing.
- Bean Validation, database constraints, and service checks protect data integrity.
- Entity objects are never returned from controllers; request and response DTOs are explicit.
- Production configuration fails fast if `JWT_SECRET` is missing or too short, and Flyway clean is disabled.
- In-process token-bucket rate limiting (Bucket4j) protects every `/api/*` route: 120 req/min per IP globally, 10/min on login, 3/min on register, and 5/min on password change. Exceeding a limit returns `429` with a `Retry-After` header and the standard JSON error body. Buckets are evicted after 10 minutes idle and capped at 20,000 tracked clients, so memory stays bounded under attack.
- The limiter reads the first `X-Forwarded-For` hop, which is what Render's edge proxy provides, so limits apply per real client instead of per proxy.
- Refusals happen before the Spring Security chain, so unauthenticated floods never reach password hashing or the database.
- Vercel serves the SPA with a Content Security Policy, HSTS, frame denial, MIME-sniffing protection and a permissions policy, configured in `frontend/vercel.json`.

## Database

| Table | Purpose | Key integrity rules |
| --- | --- | --- |
| `users` | Registered accounts | Case-insensitive unique email, role check |
| `projects` | Projects and ownership | Owner foreign key, status/date checks |
| `project_members` | Collaboration | Unique project/user pair, exactly one owner |
| `tasks` | Project tasks | Project/creator foreign keys, enum and date checks |
| `comments` | Task discussion | Task cascade, author foreign key |
| `refresh_tokens` | Hashed refresh sessions | Unique token hash, revocation timestamp |

- Deleting a project cascades memberships, tasks, and task comments.
- Deleting a task cascades its comments.
- Users with project, task, or comment history cannot be hard-deleted; accounts are disabled instead.
- All associations are lazy; API paths use entity graphs and batch count queries to avoid N+1 patterns.

## Testing and Quality

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm test
npm run build
npm audit --audit-level=moderate
```

### Continuous integration

Two GitHub Actions workflows run on every push to `main` and on pull requests that touch the relevant paths. Both use the read-only `GITHUB_TOKEN`, cancel superseded runs on the same branch, and have hard timeouts.

| Workflow | What it does | Typical duration |
| --- | --- | --- |
| `.github/workflows/backend.yml` | Starts a throwaway PostgreSQL 18 service container, runs all backend integration and unit tests with the Maven Wrapper, packages the jar, and uploads surefire reports when a test fails | ~1.5 min warm |
| `.github/workflows/frontend.yml` | `npm ci`, Vitest unit tests, production Vite build, and `npm audit --audit-level=moderate` | ~30 s warm |

Path filters keep the pipelines fast: backend pushes do not rebuild the frontend and vice versa. The Maven dependency cache and the npm cache are keyed off `pom.xml` and `package-lock.json`, so repeat runs start warm. The Maven wrapper is committed with the executable bit set, so `./mvnw` works on Linux runners without extra setup.

### Verification results

- **49 backend tests pass** (46 integration tests across authentication, persistence/schema, projects, tasks, comments, dashboard, and administration, plus 3 rate-limit unit tests).
- **12 frontend tests pass** across routing/guest guards, formatting utilities, badges, and modal behavior.
- Live Render + Neon deployment verified: `/api/health` reports `UP`.
- Rate limiting verified in production: three registrations from one IP returned `400`, the fourth returned `429`.
- CI runs the full backend suite against a disposable PostgreSQL 18 service and the frontend tests, production build, and dependency audit on every relevant push.
- Production frontend build succeeds with route-level code splitting; the Recharts dashboard chart is its own chunk (~97 KB gzipped) and never loads on other routes.
- `npm audit --audit-level=moderate` reports **0 vulnerabilities**.
- Live runtime smoke test against the running API verified: health check, registration, project creation, task creation/assignment, comment creation, status transition, dashboard counts, and CSRF-protected refresh-token rotation.
- Flyway migrations and Hibernate schema validation succeed against PostgreSQL 18.6.

## Deployment: Neon → Render → Vercel

The production topology is three managed services:

```text
Browser
  │  https://taskflow.vercel.app        (static SPA, same-origin /api calls)
  ▼
Vercel CDN ── rewrite /api/:path* ──▶ Render (Docker, Spring Boot) ──▶ Neon PostgreSQL
```

Vercel proxies `/api/*` to Render so the browser only ever sees one origin. This is required, not cosmetic: the refresh cookie is `SameSite=Strict`, and a direct `vercel.app` → `onrender.com` request is cross-site, so the browser would drop the cookie and `XSRF-TOKEN` after the 15-minute access token expires. The rewrite keeps auth working with no code change.

### 1. Database — Neon

1. Create a project at [neon.tech](https://neon.tech) and open **Connection Details**.
2. Copy the **pooled** connection string (the host contains `-pooler`) and convert it to JDBC form:

   ```text
   jdbc:postgresql://<host>-pooler.<region>.aws.neon.tech/taskflow?sslmode=require
   ```

3. Keep the database, role, and password Neon gives you — they are the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` values for Render. Flyway creates the schema on first boot.

### 2. Backend — Render

1. Push this repository to GitHub, then in Render choose **New → Blueprint** and select it. Render reads `render.yaml` (Docker service, `/api/health` health check, generated `JWT_SECRET`, `COOKIE_SECURE=true`, pool size 5).
2. Fill the `sync: false` values: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `FRONTEND_URL` (your Vercel URL, e.g. `https://taskflow.vercel.app`).
3. Deploy and confirm `https://<service>.onrender.com/api/health` returns `{"status":"UP", ...}`.
4. On the free plan the service sleeps after ~15 minutes idle; the next request takes roughly a minute to wake.

### 3. Frontend — Vercel

1. Import the same repository in Vercel. Set **Root Directory** to `frontend`; the Vite preset builds to `dist`.
2. `frontend/vercel.json` already proxies `/api/*` to the deployed Render service. If that service URL ever changes, update the `destination` (keep the `/api/:path*` suffix):

   ```json
   {
     "rewrites": [
       { "source": "/api/:path*", "destination": "https://taskflow-api-ycpp.onrender.com/api/:path*" },
       { "source": "/(.*)", "destination": "/index.html" }
     ]
   }
   ```

3. `VITE_API_URL` stays `/api`, so requests are same-origin and the proxy handles the rest.
4. Deploy, then set Render's `FRONTEND_URL` to the final Vercel domain and redeploy the API so CORS and cookie policy match.

### Verifying the security controls in production

- Register and log in, then wait past the access-token lifetime and confirm the session refreshes (proves the same-origin proxy works).
- The rate limiter needs no edge configuration. From one machine, eleven rapid `POST /api/auth/login` calls return `429` with `Retry-After`; registrations are capped at three per minute. Limits are overridable through the `RATE_LIMIT_*` environment variables documented above.

### Free-tier notes

- **Neon** scales to zero; the first query after idle adds a few hundred milliseconds.
- **Render** free instances sleep after inactivity (~1 minute cold start). A paid instance removes this.
- **Vercel Hobby** is sufficient for a static SPA plus rewrites.

## Known Limitations

- No email verification, password reset, file attachments, or real-time updates.
- Switching between board and list views is client-driven; tasks are loaded with a bounded page size for project boards.
- Refresh sessions are stored in PostgreSQL; this design targets a single API deployment or a shared database, not per-node in-memory state.
- Rate-limit buckets are in-memory, so they apply per API instance. Moving to multiple API replicas would require a shared store such as Redis.
- Email delivery and observability exporters are deployment concerns rather than bundled features.

## Roadmap

- [x] **Phase 1** — Repository, Spring Boot, React/Vite, PostgreSQL, environment setup, runtime verification
- [x] **Phase 2** — Flyway schema, entities, enums, repositories, indexes, relationship tests
- [x] **Phase 3** — Registration, login, JWT, rotating refresh tokens, logout, authorization, error handling
- [x] **Phase 4** — Project CRUD, membership, ownership and member permissions
- [x] **Phase 5** — Task CRUD, assignment, status transitions, filters, pagination
- [x] **Phase 6** — Comments, collaboration rules, security integration tests
- [x] **Phase 7** — Design system, responsive shell, light/dark themes, auth screens
- [x] **Phase 8** — Dashboard, projects, Kanban, tasks, team, profile, admin UI
- [x] **Phase 9** — Integration testing, security review, query tuning, accessibility, production build checks
- [x] **Phase 10** — Production configuration, Docker support, final documentation
