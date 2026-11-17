import { CheckCircle2, Database, Layers3, Server, Sparkles } from "lucide-react";

const foundations = [
  {
    label: "Backend",
    value: "Spring Boot 4.1 · Java 21",
    icon: Server
  },
  {
    label: "Frontend",
    value: "React 18 · Vite 5 · Tailwind",
    icon: Layers3
  },
  {
    label: "Database",
    value: "PostgreSQL 18 · Flyway",
    icon: Database
  }
];

export default function App() {
  return (
    <main className="relative isolate flex min-h-screen items-center justify-center overflow-hidden px-6 py-16">
      <div className="absolute inset-0 bg-grid" aria-hidden="true" />
      <div
        className="absolute left-1/2 top-0 -z-10 h-80 w-[38rem] -translate-x-1/2 rounded-full bg-brand-soft/40 blur-3xl"
        aria-hidden="true"
      />

      <section className="w-full max-w-3xl animate-fade-up">
        <div className="mb-8 flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand text-brand-contrast shadow-card">
            <svg
              viewBox="0 0 32 32"
              className="h-6 w-6"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M8 10.5h16M8 16h10M8 21.5h6"
                stroke="currentColor"
                strokeWidth="2.4"
                strokeLinecap="round"
              />
            </svg>
          </span>
          <div>
            <p className="text-lg font-semibold tracking-[-0.02em]">TaskFlow</p>
            <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted">
              Collaborative project management
            </p>
          </div>
        </div>

        <div className="rounded-card border border-line bg-surface/95 p-7 shadow-card backdrop-blur sm:p-10">
          <span className="inline-flex items-center gap-2 rounded-full border border-brand/20 bg-brand-soft/50 px-3 py-1 text-xs font-semibold text-brand">
            <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
            Phase 1 · Foundation verified
          </span>

          <h1 className="mt-6 text-4xl font-semibold tracking-[-0.035em] text-ink sm:text-5xl">
            A clean foundation for focused teamwork.
          </h1>
          <p className="mt-4 max-w-xl text-base leading-7 text-muted">
            The TaskFlow workspace is initialized. Application architecture, environment
            configuration, database connectivity, theme tokens, and production builds are
            ready for the next development phase.
          </p>

          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            {foundations.map(({ label, value, icon: Icon }) => (
              <div
                key={label}
                className="rounded-xl border border-line bg-surface-muted/60 p-4"
              >
                <Icon className="h-4 w-4 text-brand" aria-hidden="true" />
                <p className="mt-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-muted">
                  {label}
                </p>
                <p className="mt-1 text-sm font-medium leading-5 text-ink">{value}</p>
              </div>
            ))}
          </div>

          <div className="mt-8 flex flex-col gap-3 border-t border-line pt-6 text-sm sm:flex-row sm:items-center sm:justify-between">
            <span className="inline-flex items-center gap-2 font-medium text-ink">
              <CheckCircle2 className="h-4 w-4 text-success" aria-hidden="true" />
              Environment and database checks passed
            </span>
            <span className="text-xs text-muted">
              Next: database schema and domain model
            </span>
          </div>
        </div>

        <p className="mt-6 text-center text-xs text-muted">
          TaskFlow · Built for secure, collaborative delivery
        </p>
      </section>
    </main>
  );
}
