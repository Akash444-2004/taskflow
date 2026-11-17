import { CheckCircle2 } from "lucide-react";

import Brand from "../../components/Brand";

const HIGHLIGHTS = [
  "Organize projects with clear ownership and membership",
  "Track work on a focused Kanban board",
  "Keep decisions close to the task with comments",
  "Protect every action with role-aware authorization"
];

export default function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="min-h-screen bg-canvas lg:grid lg:grid-cols-2">
      <aside className="relative hidden overflow-hidden bg-brand px-12 py-12 text-brand-contrast lg:flex lg:flex-col">
        <div className="absolute inset-0 opacity-20">
          <div className="absolute -left-16 top-24 h-72 w-72 rounded-full bg-white/30 blur-3xl" />
          <div className="absolute bottom-0 right-0 h-80 w-80 rounded-full bg-black/20 blur-3xl" />
        </div>

        <div className="relative">
          <Brand inverted />
          <h2 className="mt-16 max-w-md text-4xl font-semibold leading-tight tracking-[-0.03em]">
            Calm, focused collaboration for small teams.
          </h2>
          <p className="mt-4 max-w-md text-sm leading-6 text-white/75">
            TaskFlow keeps projects, tasks, and conversations in one secure workspace
            without the usual process overhead.
          </p>

          <ul className="mt-10 space-y-3">
            {HIGHLIGHTS.map((item) => (
              <li key={item} className="flex items-start gap-3 text-sm text-white/85">
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-white/70" aria-hidden="true" />
                {item}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative mt-auto pt-12 text-xs text-white/50">
          Short-lived access tokens · rotating refresh sessions · CSRF-protected cookies
        </p>
      </aside>

      <main className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-8">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <Brand />
          </div>

          <div className="rounded-card border border-line bg-surface p-6 shadow-card sm:p-8">
            <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">{title}</h1>
            {subtitle ? <p className="mt-2 text-sm text-muted">{subtitle}</p> : null}
            <div className="mt-6">{children}</div>
          </div>

          {footer ? <div className="mt-6 text-center text-sm text-muted">{footer}</div> : null}
        </div>
      </main>
    </div>
  );
}
