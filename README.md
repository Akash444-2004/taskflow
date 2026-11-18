# TaskFlow

TaskFlow is a collaborative project and task management application for small teams. It is a production-quality portfolio project built as a Spring Boot API, a PostgreSQL database, and a responsive React single-page application.

> **Status: all ten phases complete.** Registration, authentication, projects, membership, tasks, comments, dashboards, administration, the full React interface, integration tests, and container/deployment configuration are implemented and verified. See [Verification](#verification).

## Features

**Accounts and security**

- Registration, login, logout, and session bootstrap with short-lived JWT access tokens.
- Rotating, hashed, revocable refresh sessions stored in PostgreSQL.
- CSRF-protected cookie endpoints, BCrypt password hashing, password change with global session revocation.
- Server-side role and project authorization on every request; JWT claims are never trusted for permissions.

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
| Build | Maven Wrapper, npm, Docker / Docker Compose |

## Repository Layout

```text
taskflow/
├── backend/
│   ├── src/main/java/com/taskflow/
│   │   ├── config/          # Security, CORS, CSRF, JWT and cookie properties
│   │   ├── controller/      # Thin REST controllers
│   │   ├── dto/             # Request/response records grouped by feature
│   │   ├── entity/          # JPA entities
│   │   ├── enums/           # Roles, project/task status, priorities
│   │   ├── exception/       # Domain exceptions and global error handler
│   │   ├── repository/      # Spring Data repositories and projections
│   │   ├── security/        # JWT filter, refresh cookies, entry points
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
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── .env.example
│   └── pom.xml
├── frontend/
│   ├── public/              # Favicon and pre-paint theme bootstrap
│   ├── src/
│   │   ├── components/      # Layout, projects, tasks, UI primitives
│   │   ├── context/         # Auth, theme, toast providers
│   │   ├── pages/           # Dashboard, projects, tasks, profile, admin, auth
│   │   ├── services/        # Axios client, refresh interceptor, CSRF helpers
│   │   ├── test/            # Vitest setup
│   │   ├── utils/           # Formatting and enum labels
│   │   ├── App.jsx
│   │   ├── index.css
│   │   └── main.jsx
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── scripts/                 # Local PostgreSQL start/stop helpers
├── docker-compose.yml
├── .env.example             # Docker Compose environment template
└── README.md
```

## Prerequisites

- Java 21
- Node.js 26 LTS (or a Vite-compatible supported Node release)
- PostgreSQL 15+ for local development (PostgreSQL 18 is used in verification)
- Git
- Docker and Docker Compose for container deployment (optional)

Maven does not need to be installed globally; the Maven Wrapper is included.

## Local Development

### 1. Database

Development defaults use an isolated PostgreSQL cluster on `localhost:5433`.

The repository includes setup helpers for Windows. If the cluster does not exist yet, follow the initialization steps in the script header or create a normal PostgreSQL database:

```sql
CREATE ROLE taskflow WITH LOGIN PASSWORD 'choose-a-strong-password';
CREATE DATABASE taskflow OWNER taskflow;
```

After initial setup:

```powershell
.\scripts\start-dev-db.ps1
.\scripts\stop-dev-db.ps1
```

The `.dev/` directory holds local database state and is git-ignored.

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
| `DB_URL` | `jdbc:postgresql://localhost:5433/taskflow` | JDBC connection URL |
| `DB_USERNAME` | `taskflow` | Database user |
| `DB_PASSWORD` | required | Database password |
| `DB_POOL_MAX_SIZE` | `10` | Hikari maximum pool size |
| `DB_POOL_MIN_IDLE` | `2` | Hikari minimum idle connections |
| `SERVER_PORT` | `8080` | HTTP port |
| `FRONTEND_URL` | `http://localhost:5173` | Allowed CORS origin(s), comma-separated |
| `JWT_SECRET` | required | Base64 secret, at least 512 bits |
| `JWT_ISSUER` | `taskflow-api` | JWT issuer claim |
| `JWT_ACCESS_TTL` | `PT15M` | Access token lifetime |
| `JWT_REFRESH_TTL` | `P7D` | Refresh session lifetime |
| `COOKIE_SECURE` | `false` (local), `true` (prod profile) | Sets `Secure` on auth cookies |
| `REFRESH_COOKIE_NAME` | `taskflow_refresh` | Refresh cookie name |
| `REFRESH_COOKIE_PATH` | `/api/auth` | Refresh cookie path |

### Frontend

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_URL` | `/api` in the container build | Public API base URL; never place secrets in `VITE_*` |

### Docker Compose

The root `.env.example` documents `APP_PORT`, `POSTGRES_*`, `JWT_SECRET`, `FRONTEND_URL`, and `COOKIE_SECURE`.

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
- The bundled nginx configuration adds CSP, HSTS-compatible deployment guidance, frame denial, MIME sniffing protection, and a permissions policy.

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

### Verification results

- **46 backend tests pass** across authentication, persistence/schema, projects, tasks, comments, dashboard, and administration.
- **12 frontend tests pass** across routing/guest guards, formatting utilities, badges, and modal behavior.
- Production frontend build succeeds with lazy-loaded route chunks.
- `npm audit --audit-level=moderate` reports **0 vulnerabilities**.
- Live runtime smoke test against the running API verified: health check, registration, project creation, task creation/assignment, comment creation, status transition, dashboard counts, and CSRF-protected refresh-token rotation.
- Flyway migrations and Hibernate schema validation succeed against PostgreSQL 18.6.

## Docker Deployment

The repository includes a multi-stage backend image, an nginx-served frontend image, and Docker Compose orchestration.

1. Create the environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Set strong values for `POSTGRES_PASSWORD` and `JWT_SECRET` in `.env`. Generate `JWT_SECRET` with the PowerShell snippet above.

3. Build and start:

   ```powershell
   docker compose up --build
   ```

4. Open `http://localhost:8080`. nginx serves the SPA and proxies `/api` to the backend over the private Compose network.

5. Data persists in the `taskflow-db` volume.

Production notes:

- Terminate TLS at an edge proxy or load balancer, set `COOKIE_SECURE=true`, and set `FRONTEND_URL` to the public HTTPS origin.
- Backend and database ports are not published by default; only nginx is exposed.
- Set `SPRING_PROFILES_ACTIVE=prod` (already set by Compose) to enable secure cookies by default and forwarded-header handling.
- Apply rate limiting for `/api/auth/login` and `/api/auth/register` at the edge proxy. Application-level distributed rate limiting is intentionally out of scope for this monolith.
- Container definitions are included in the repository, but Docker was not installed on the verification workstation, so image builds were not executed here.

## Known Limitations

- No email verification, password reset, file attachments, or real-time updates.
- Switching between board and list views is client-driven; tasks are loaded with a bounded page size for project boards.
- Refresh sessions are stored in PostgreSQL; this design targets a single API deployment or a shared database, not per-node in-memory state.
- Email delivery, observability exporters, and edge rate limiting are deployment concerns rather than bundled features.

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
