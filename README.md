# TaskFlow – Project & Task Management

[![Live Demo](https://img.shields.io/badge/Live-Demo-2ea44f?style=flat-square)](https://taskflow-delta-henna.vercel.app)
![Database](https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
[![Backend CI](https://github.com/Akash444-2004/taskflow/actions/workflows/backend.yml/badge.svg?branch=main)](https://github.com/Akash444-2004/taskflow/actions/workflows/backend.yml)
[![Frontend CI](https://github.com/Akash444-2004/taskflow/actions/workflows/frontend.yml/badge.svg?branch=main)](https://github.com/Akash444-2004/taskflow/actions/workflows/frontend.yml)

**Live app:** https://taskflow-delta-henna.vercel.app

TaskFlow is a project and task manager for small teams. You create a project, add members, create tasks with priorities and due dates, assign them, and move them through a fixed status flow. There is a personal dashboard and an admin area for managing accounts.

The app is a React single-page frontend, a Spring Boot API, and a PostgreSQL database. It runs on Vercel, Render and Neon.

## Features

**Accounts and sessions**

- Register, log in and log out. Access tokens last 15 minutes and stay in memory, not in `localStorage`.
- Refresh tokens are stored hashed in PostgreSQL, rotate on every use, and can be revoked on logout, password change, account disable, or reuse detection.
- CSRF protection on the cookie-based auth endpoints.
- Passwords hashed with BCrypt at strength 12.
- Roles and project access are read from the database on every request. The token only identifies the user.

**Projects**

- Create a project with a description and schedule. The creator becomes the owner.
- Only the owner can edit or delete the project and add or remove members.
- Members can view the project, work on its tasks, and leave whenever they want.
- Project list with search, status filter and pagination.

**Tasks**

- Kanban board and table view, with priority, assignee, due date, search and filters.
- Status flow is enforced: `TODO` → `IN_PROGRESS` → `COMPLETED`. A completed task can be reopened, and an in-progress task can go back to `TODO`.
- Tasks can only be assigned to project members.
- A task can be deleted by the project owner or the person who created it.
- "My tasks" page collects everything assigned to you across projects, including overdue items.

**Comments and dashboard**

- Comments on each task. The author, the project owner or an admin can delete a comment.
- Dashboard with assigned task counts, overdue and due-soon work, a status chart, upcoming deadlines and recent projects.

**Admin**

- Platform stats, account search, and enable/disable toggles.
- Disabling an account revokes its refresh sessions and blocks new logins.

**Interface**

- Responsive layout with a mobile navigation.
- Light and dark theme, saved per device, loaded before first paint.
- Lazy-loaded routes, skeleton loaders, toasts and keyboard-accessible dialogs.

## Tech stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA, Hibernate 7, Spring Security 7 |
| Database | PostgreSQL 18, Flyway migrations |
| Auth | BCrypt (12 rounds), HS512 JWT access tokens, opaque rotating refresh tokens |
| Frontend | React 19, Vite 8, React Router 7, Tailwind CSS 3.4, Axios, Recharts, Lucide |
| Rate limiting | Bucket4j token buckets, per client IP |
| Tests | JUnit 5, MockMvc, Vitest, React Testing Library |
| Build | Maven Wrapper, npm, Docker, GitHub Actions |

## Project layout

```text
taskflow/
├── backend/
│   ├── src/main/java/com/taskflow/
│   │   ├── config/        # security, CORS, CSRF, JWT, cookies, rate-limit properties
│   │   ├── controller/    # REST controllers
│   │   ├── dto/           # request and response records, grouped by feature
│   │   ├── entity/        # JPA entities
│   │   ├── enums/         # roles, statuses, priorities
│   │   ├── exception/     # domain exceptions and the global error handler
│   │   ├── mapper/        # entity to DTO mapping
│   │   ├── repository/    # Spring Data repositories and projections
│   │   ├── security/      # JWT filter, rate limiter, refresh cookies
│   │   └── service/       # business logic and authorization
│   ├── src/main/resources/
│   │   ├── db/migration/  # V1 schema, V2 single-owner constraint
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── src/test/java/com/taskflow/
│   │   ├── admin/         # admin and dashboard tests
│   │   ├── comment/       # comment permissions
│   │   ├── persistence/   # schema and repository tests
│   │   ├── project/       # project and membership rules
│   │   ├── security/      # authentication and session tests
│   │   └── task/          # task rules and transitions
│   ├── Dockerfile         # used by Render
│   └── pom.xml
├── frontend/
│   ├── public/            # favicon and the pre-paint theme script
│   ├── src/
│   │   ├── components/    # layout, projects, tasks, UI primitives
│   │   ├── context/       # auth, theme and toast providers
│   │   ├── hooks/         # count-up animation hook
│   │   ├── pages/         # dashboard, projects, tasks, profile, admin, auth
│   │   ├── services/      # axios client, refresh interceptor, CSRF helpers
│   │   ├── test/          # Vitest setup
│   │   └── utils/         # formatting and enum labels
│   ├── vercel.json        # /api proxy to Render, SPA fallback, security headers
│   └── package.json
├── scripts/               # local run scripts (PowerShell)
├── .github/workflows/     # backend and frontend CI
├── .vscode/tasks.json     # one-click backend/frontend tasks
├── render.yaml            # optional Render blueprint
└── README.md
```

## Running locally

You need Java 21, Node 26 (or another Vite-supported version), PostgreSQL 15 or newer, and Git. Maven is not required; the wrapper is included. Docker is only needed if you want to build the backend image yourself.

Create the databases once:

```sql
CREATE DATABASE taskflow;
CREATE DATABASE taskflow_test;
```

`taskflow` is for development, `taskflow_test` is used by the integration tests.

Start the app with the run scripts. They ask for the PostgreSQL password in a masked prompt and keep it in memory only, so nothing is written to disk.

```powershell
npm run backend      # API on http://localhost:8080
npm run frontend     # SPA on http://localhost:5173
npm run fullstack    # both, with pre-flight checks
npm run stop         # stop whatever is listening on 8080 and 5173
```

The same three tasks are available in VS Code under **Tasks: Run Task**:

- `TaskFlow: Run Backend`
- `TaskFlow: Run Frontend`
- `TaskFlow: Run Full Stack`

Backend logs go to `.dev/backend.out.log` and frontend logs to `.dev/frontend.out.log`. The JWT secret is generated on first run and stored DPAPI-encrypted in `.dev/jwt.secret`, readable only by your Windows user.

Flyway creates the schema on startup. Useful URLs:

- App: `http://localhost:5173`
- API: `http://localhost:8080`
- Health: `GET http://localhost:8080/api/health`

The Vite dev server proxies `/api` to port 8080, so the frontend needs no API URL configured locally.

### Tests

Backend tests run against `taskflow_test`, never against development data:

```powershell
cd backend
$env:DB_PASSWORD = "your-postgres-password"
.\mvnw.cmd test
```

Frontend tests and a production build:

```powershell
cd frontend
npm test
npm run build
npm audit --audit-level=moderate
```

## Environment variables

### Backend

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/taskflow` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | required | Database password |
| `PORT` | `8080` | HTTP port (Render sets this) |
| `FRONTEND_URL` | `http://localhost:5173` | Allowed CORS origin, comma-separated for more than one |
| `JWT_SECRET` | required | Base64 secret, at least 512 bits |
| `JWT_ACCESS_TTL` | `PT15M` | Access token lifetime |
| `JWT_REFRESH_TTL` | `P7D` | Refresh session lifetime |
| `COOKIE_SECURE` | `false` | Set to `true` in production |
| `DB_POOL_MAX_SIZE` | `10` | Hikari pool size (5 is a good value on Neon) |
| `RATE_LIMIT_ENABLED` | `true` | Turns the limiter on or off |
| `RATE_LIMIT_GLOBAL_PER_MINUTE` | `120` | Requests per minute per IP on `/api/*` |
| `RATE_LIMIT_LOGIN_PER_MINUTE` | `10` | Login attempts per minute per IP |
| `RATE_LIMIT_REGISTER_PER_MINUTE` | `3` | Registrations per minute per IP |
| `RATE_LIMIT_PASSWORD_CHANGE_PER_MINUTE` | `5` | Password changes per minute per IP |
| `RATE_LIMIT_TRUST_FORWARDED_FOR` | `true` | Use the first `X-Forwarded-For` hop behind Render |

### Frontend

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_URL` | `/api` | API base URL. Leave it alone in production; the Vercel rewrite handles routing |

Production values live in the Render dashboard, not in the repository.

## API

All responses are JSON, except `204 No Content`. Errors return `timestamp`, `status`, `code`, `message`, `path` and, for validation failures, `fieldErrors`.

### Auth and profile

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create an account and start a session |
| `POST` | `/api/auth/login` | Public | Log in |
| `GET` | `/api/auth/csrf` | Public | Issue the CSRF cookie |
| `POST` | `/api/auth/refresh` | Refresh cookie + CSRF | Rotate the refresh token and issue a new access token |
| `POST` | `/api/auth/logout` | Refresh cookie + CSRF | Revoke the session and clear the cookie |
| `GET` | `/api/users/me` | Authenticated | Current profile |
| `PUT` | `/api/users/me` | Authenticated | Update name and phone number |
| `POST` | `/api/users/me/password` | Authenticated | Change password and revoke all sessions |

### Projects and members

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/projects` | Authenticated | List projects (`status`, `search`, `page`, `size`, `sort`) |
| `POST` | `/api/projects` | Authenticated | Create a project; the creator becomes owner |
| `GET` | `/api/projects/{id}` | Project member | Project details and counts |
| `PUT` | `/api/projects/{id}` | Owner | Update the project |
| `DELETE` | `/api/projects/{id}` | Owner | Delete the project, its tasks, comments and members |
| `GET` | `/api/projects/{id}/members` | Project member | List members |
| `POST` | `/api/projects/{id}/members` | Owner | Add an existing account by email |
| `DELETE` | `/api/projects/{id}/members/me` | Member (not owner) | Leave the project |
| `DELETE` | `/api/projects/{id}/members/{userId}` | Owner | Remove a member |

### Tasks

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/projects/{id}/tasks` | Project member | Filter by status, priority, assignee or search; paginated |
| `POST` | `/api/projects/{id}/tasks` | Project member | Create a task |
| `GET` | `/api/tasks/assigned-to-me` | Authenticated | Your tasks across projects |
| `GET` | `/api/tasks/{id}` | Project member | Task details |
| `PUT` | `/api/tasks/{id}` | Project member | Update a task; status changes must follow the flow |
| `PATCH` | `/api/tasks/{id}/status` | Project member | Move a task to the next allowed status |
| `DELETE` | `/api/tasks/{id}` | Owner or creator | Delete the task and its comments |

### Comments, dashboard and admin

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/tasks/{id}/comments` | Project member | List comments |
| `POST` | `/api/tasks/{id}/comments` | Project member | Add a comment |
| `DELETE` | `/api/comments/{id}` | Author, owner or admin | Delete a comment |
| `GET` | `/api/dashboard` | Authenticated | Personal counts, chart data, upcoming tasks, recent projects |
| `GET` | `/api/admin/users` | Admin | Search and page accounts |
| `PATCH` | `/api/admin/users/{id}/enabled` | Admin | Enable or disable an account (not yourself) |
| `GET` | `/api/admin/stats` | Admin | Platform totals |
| `GET` | `/api/health` | Public | Health check |

## Access rules

| Action | Who can do it |
| --- | --- |
| View a project, its tasks and comments | Owner or project member |
| Edit or delete a project, add or remove members | Project owner |
| Leave a project | Any member except the owner |
| Create or edit a task, change its status, comment | Project member |
| Assign a task | Any enabled project member |
| Delete a task | Project owner or task creator |
| Delete a comment | Comment author, project owner or admin |
| Edit your profile or password | The account owner |
| Admin endpoints | Users with the `ADMIN` role, checked against the database |

Authorization is enforced in the service layer and in Spring Security, never only in the React UI. A modified or stale token does not grant access, because the user is reloaded from the database on each request.

## Security notes

- Access tokens are short-lived and kept in memory. The refresh token is the only credential in a cookie.
- Refresh tokens are hashed with SHA-256, rotate on every use, and are revoked when reused.
- The refresh cookie is `HttpOnly`, `SameSite=Strict`, scoped to `/api/auth`, and `Secure` in production.
- `/api/auth/refresh`, `/api/auth/logout` and `/api/auth/csrf` run on a dedicated chain protected by an `XSRF-TOKEN` cookie and an `X-XSRF-TOKEN` header.
- CORS only allows the configured frontend origin, with credentials.
- Bean Validation, database constraints and service checks all guard the same rules.
- Controllers never return entities; only explicit DTOs.
- The production profile fails fast if `JWT_SECRET` is missing or too short.
- Requests are rate limited before Spring Security, so floods never reach password hashing or the database. Exceeding a limit returns `429` with `Retry-After`. Buckets are dropped after 10 idle minutes and capped at 20,000 clients, which keeps memory bounded.
- Vercel adds CSP, HSTS, frame denial, MIME-sniffing protection, a referrer policy and a permissions policy.

## Database

| Table | Purpose | Main rules |
| --- | --- | --- |
| `users` | Accounts | Case-insensitive unique email, role check |
| `projects` | Projects | Owner foreign key, status and date checks |
| `project_members` | Memberships | Unique project/user pair, exactly one owner |
| `tasks` | Tasks | Project and creator foreign keys, enum and date checks |
| `comments` | Task comments | Cascade from task, author foreign key |
| `refresh_tokens` | Sessions | Unique token hash, revocation timestamp |

Deleting a project cascades to members, tasks and comments. Deleting a task cascades to its comments. Users with history are disabled instead of deleted. Associations are lazy, and list endpoints use entity graphs and batch count queries to avoid N+1 queries.

## Tests and CI

Two GitHub Actions workflows run on pushes and pull requests that touch their own paths.

| Workflow | Steps | Typical time |
| --- | --- | --- |
| `backend.yml` | Starts PostgreSQL 18, runs all backend tests with the Maven Wrapper, packages the jar, uploads surefire reports on failure | ~1.5 min |
| `frontend.yml` | `npm ci`, Vitest, production build, `npm audit` | ~30 s |

Both run with a read-only token, cancel older runs on the same branch, and have timeouts. Backend pushes do not rebuild the frontend and the other way around.

Current results:

- 49 backend tests pass (46 integration tests plus 3 rate-limiter unit tests).
- 12 frontend tests pass.
- `npm audit` reports 0 vulnerabilities.
- Rate limiting was checked in production: the fourth registration from one IP returns `429`.
- The production bundle loads about 87 KB of gzipped JavaScript on first visit. The dashboard chart is split into its own chunk and only loads on the dashboard.

## Deploying

```text
Browser
  │  https://taskflow-delta-henna.vercel.app
  ▼
Vercel (static SPA)  ──/api/*──▶  Render (Docker, Spring Boot)  ──▶  Neon (PostgreSQL)
```

Vercel proxies `/api/*` to Render, so the browser sees a single origin. This is needed because the refresh cookie is `SameSite=Strict`; a direct call to `onrender.com` would be cross-site and the cookie would be dropped.

### 1. Neon

1. Create a project on [neon.tech](https://neon.tech) and open **Connection Details**.
2. Copy the pooled connection string and convert it to JDBC form:

   ```text
   jdbc:postgresql://<host>-pooler.<region>.aws.neon.tech/<database>?sslmode=require
   ```

3. Note the database name, role and password. They become `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. Flyway creates the tables on first start.

### 2. Render

1. Create a **Web Service** from the repository.
2. Set **Root Directory** to `backend` and **Dockerfile Path** to `./Dockerfile`.
3. Add these environment variables:

   ```text
   SPRING_PROFILES_ACTIVE=prod
   DB_URL=jdbc:postgresql://<host>-pooler.<region>.aws.neon.tech/<database>?sslmode=require
   DB_USERNAME=<neon role>
   DB_PASSWORD=<neon password>
   JWT_SECRET=<base64 secret, at least 512 bits>
   FRONTEND_URL=https://<your-vercel-domain>
   COOKIE_SECURE=true
   DB_POOL_MAX_SIZE=5
   ```

4. Deploy and check `https://<service>.onrender.com/api/health`.

`render.yaml` is included if you prefer a Blueprint deployment instead. It sets the same values, with the secrets marked as `sync: false`.

### 3. Vercel

1. Import the repository and set **Root Directory** to `frontend`. The Vite preset builds to `dist`.
2. Check that `frontend/vercel.json` points at your Render URL:

   ```json
   { "source": "/api/:path*", "destination": "https://<your-service>.onrender.com/api/:path*" }
   ```

3. Deploy, then set Render's `FRONTEND_URL` to the Vercel domain and redeploy the API.

On the free tier, Render sleeps after about 15 minutes without traffic and the next request can take a minute to wake. Neon also scales to zero, which adds a short delay to the first query.

## Limitations

- No password reset or email verification yet.
- No file attachments and no real-time updates.
- Rate-limit buckets are in memory, so limits apply per API instance. Multiple instances would need a shared store such as Redis.
- Refresh sessions are stored in PostgreSQL, so they work across instances but not offline.
- Email delivery and metrics are left to the hosting platform.
