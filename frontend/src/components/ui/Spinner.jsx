import { Loader2 } from "lucide-react";

import { cn } from "../../utils/format";

export default function Spinner({ className, label = "Loading" }) {
  return (
    <span role="status" aria-label={label} className="inline-flex">
      <Loader2 className={cn("h-5 w-5 animate-spin text-brand", className)} aria-hidden="true" />
    </span>
  );
}

export function FullPageLoader({ label = "Loading workspace" }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas">
      <div className="flex flex-col items-center gap-3 text-sm font-medium text-muted">
        <Spinner className="h-7 w-7" label={label} />
        {label}
      </div>
    </div>
  );
}
