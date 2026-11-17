import { Link } from "react-router-dom";

import Brand from "../components/Brand";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-canvas px-6 text-center">
      <Brand className="mb-8" />
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand">404</p>
      <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">
        This page does not exist
      </h1>
      <p className="mt-2 max-w-md text-sm text-muted">
        The link may be outdated, or the page may have moved. Head back to your workspace.
      </p>
      <Link
        to="/dashboard"
        className="mt-6 inline-flex h-10 items-center rounded-lg bg-brand px-4 text-sm font-semibold text-brand-contrast hover:bg-brand-hover"
      >
        Return to dashboard
      </Link>
    </div>
  );
}
