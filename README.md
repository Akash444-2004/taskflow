# TaskFlow

TaskFlow is a collaborative project and task management application for small teams. It is being built as a production-quality portfolio project with a Spring Boot API, PostgreSQL persistence, JWT authentication, and a responsive React interface.

> **Current status: Phase 1 complete.** The repository, toolchain, database connection, theme foundation, tests, and production frontend build are verified. Domain features arrive in the next phases.

## Architecture

```text
Browser
  │
  │ HTTPS / JSON / JWT access token
  ▼
React 19 SPA (Vite, Tailwind, React Router)
  │  /api
  ▼
Spring Boot 4.1 monolith
  ├── Controller → DTO validation → Service → Repository
  ├── Spring Security / JWT filter
  ├── Flyway migrations
  └── JPA / Hibernate
        │
        ▼
PostgreSQL 18
```

This is deliberately a modular monolith with a separate SPA. There are no microservices, message brokers, caches, or distributed-system components in scope.

## Technology

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA, Hibernate 7, Spring Security |
| Database | PostgreSQL 18, Flyway |
| Authentication | BCrypt, short-lived JWT access tokens, rotating refresh tokens |
| Frontend | React 19, Vite 8, JavaScript, Tailwind CSS 3.4, React Router 7, Axios, Recharts, Lucide |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, Vitest, React Testing Library |
| Build | Maven Wrapper, npm |

Dependency versions are pinned and the frontend production dependency audit currently reports **0 vulnerabilities**.

## Repository Layout

```text
taskflow/
├── backend/
│   ├── .mvn/wrapper/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/taskflow/
│   │   │   │   ├── config/          # Phase 3+
│   │   │   │   ├── controller/      # HealthController now
│   │   │   │   ├── dto/             # Phase 3+
│   │   │   │   ├── entity/          # Phase 2+
│   │   │   │   ├── enums/           # Phase 2+
│   │   │   │   ├── exception/       # Phase 3+
│   │   │   │   ├── repository/      # Phase 2+
│   │   │   │   ├── security/        # Phase 3+
│   │   │   │   ├── service/         # Phase 3+
│   │   │   │   └── TaskFlowApplication.java
│   │   │   └── resources/
│   │   │       ├── db/migration/    # Phase 2+
│   │   │       └── application.yml
│   │   └── test/
│   ├── .env.example
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── assets/                  # Phase 7+
│   │   ├── components/              # Phase 7+
│   │   ├── context/                 # Phase 7+
│   │   ├── hooks/                   # Phase 7+
│   │   ├── layouts/                 # Phase 7+
│   │   ├── pages/                   # Phase 7+
│   │   ├── services/                # Phase 8+
│   │   ├── utils/                   # Phase 8+
│   │   ├── test/
│   │   ├── App.jsx
│   │   ├── index.css
│   │   └── main.jsx
│   ├── .env.example
│   ├── package.json
│   └── vite.config.js
├── scripts/
│   ├── start-dev-db.ps1
│   └── stop-dev-db.ps1
├── .gitignore
└── README.md
```

The folders marked with a phase are part of the approved roadmap and will be added as those phases are implemented. Empty placeholder directories are intentionally not committed.

## Prerequisites

- Java 21
- Node.js 26 LTS (or a Vite-compatible supported Node release)
- PostgreSQL 18 (or a compatible PostgreSQL 15+ server)
- Git

Maven does not need to be installed globally; the repository includes the Maven Wrapper.

## Database Setup

The API expects a PostgreSQL database named `taskflow`. Development defaults use `localhost:5433` to keep an isolated local cluster separate from any existing PostgreSQL service.

### Option A — Existing PostgreSQL server

Create the role and database with an administrative account:

```sql
CREATE ROLE taskflow WITH LOGIN PASSWORD 'choose-a-strong-password';
CREATE DATABASE taskflow OWNER taskflow;
```

Then set `DB_URL` to that server, for example `jdbc:postgresql://localhost:5432/taskflow`.

### Option B — Isolated local development cluster

From the repository root in PowerShell:

```powershell
$pgBin = "C:\Program Files\PostgreSQL\18\bin"
$project = (Get-Location).Path
New-Item -ItemType Directory -Force "$project\.dev" | Out-Null

Set-Content "$project\.dev\pgpass.txt" "taskflow_dev_2026" -NoNewline
& "$pgBin\initdb.exe" -D "$project\.dev\pgdata" -U taskflow `
  --pwfile="$project\.dev\pgpass.txt" `
  --auth-host=scram-sha-256 --auth-local=scram-sha-256 `
  --encoding=UTF8 --locale=C
Remove-Item "$project\.dev\pgpass.txt"

& "$pgBin\pg_ctl.exe" -D "$project\.dev\pgdata" `
  -l "$project\.dev\postgres.log" -o "-p 5433" start

$env:PGPASSWORD = "taskflow_dev_2026"
& "$pgBin\createdb.exe" -h localhost -p 5433 -U taskflow taskflow
```

After initial setup, use:

```powershell
.\scripts\start-dev-db.ps1
.\scripts\stop-dev-db.ps1
```

The `.dev/` directory contains local database state and is git-ignored.

## Environment Configuration

Create `backend/.env` from `backend/.env.example`. It is automatically imported for local backend runs and is git-ignored.

```properties
DB_URL=jdbc:postgresql://localhost:5433/taskflow
DB_USERNAME=taskflow
DB_PASSWORD=replace-with-your-local-password
JWT_SECRET=replace-with-a-generated-secret
```

Generate a 512-bit JWT secret in PowerShell:

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Production must supply secrets as real environment variables. Do not commit `.env` files. The frontend `.env` may contain only public values such as `VITE_API_URL`; never place backend secrets in `VITE_*` variables.

## Run Locally

Terminal 1 — database:

```powershell
.\scripts\start-dev-db.ps1
```

Terminal 2 — API:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Terminal 3 — web app:

```powershell
cd frontend
npm install
npm run dev
```

- Frontend: http://localhost:5173
- API: http://localhost:8080
- Health endpoint: `GET /api/health`

Spring Security is active from Phase 1, so all endpoints currently require authentication until the JWT configuration is implemented in Phase 3.

## Verification

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm test
npm run build
npm audit --audit-level=moderate
```

Phase 1 verification results:

- Backend compiles and `TaskFlowApplicationTests.contextLoads` passes.
- Hibernate connects to PostgreSQL 18.6 through HikariCP.
- Flyway validates an empty migration history.
- Frontend test passes under Vitest.
- Vite production build succeeds.
- Frontend and backend development servers both respond over HTTP.
- `npm audit` reports zero vulnerabilities.

## Roadmap

- [x] **Phase 1** — Repository, Spring Boot, React/Vite, PostgreSQL, environment setup, runtime verification
- [ ] **Phase 2** — Flyway schema, entities, enums, repositories, indexes, relationship tests
- [ ] **Phase 3** — Registration, login, JWT, rotating refresh tokens, logout, authorization, error handling
- [ ] **Phase 4** — Project CRUD, membership, ownership and member permissions
- [ ] **Phase 5** — Task CRUD, assignment, status transitions, filters, pagination
- [ ] **Phase 6** — Comments, collaboration rules, security integration tests
- [ ] **Phase 7** — Design system, responsive shell, light/dark themes, auth screens
- [ ] **Phase 8** — Dashboard, projects, Kanban, tasks, team, profile, admin UI
- [ ] **Phase 9** — Integration testing, security review, query tuning, accessibility, Lighthouse
- [ ] **Phase 10** — Production configuration, Docker support, final documentation

## Security Baseline

- Passwords will be BCrypt-hashed and never returned by APIs.
- Access tokens will be short-lived; refresh tokens will be rotated and revoked server-side.
- Cookie-based refresh/logout endpoints will be protected against CSRF.
- Project and task authorization will be enforced in services, not only in the UI.
- Entities will not be exposed directly; request and response DTOs are required.
- Database constraints and backend validation enforce data integrity.
- Secrets are environment-driven and `.env` files are git-ignored.
