import { cn } from "../utils/format";

export default function Brand({ compact = false, inverted = false, className }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span
        className={cn(
          "flex h-9 w-9 items-center justify-center rounded-xl shadow-sm",
          inverted ? "bg-white/15 text-white" : "bg-brand text-brand-contrast"
        )}
      >
        <svg viewBox="0 0 32 32" className="h-5 w-5" fill="none" aria-hidden="true">
          <path
            d="M8 10.5h16M8 16h10M8 21.5h6"
            stroke="currentColor"
            strokeWidth="2.4"
            strokeLinecap="round"
          />
        </svg>
      </span>
      {!compact ? (
        <span className="flex flex-col leading-none">
          <span
            className={cn(
              "text-base font-semibold tracking-[-0.02em]",
              inverted ? "text-white" : "text-ink"
            )}
          >
            TaskFlow
          </span>
          <span
            className={cn(
              "mt-1 text-[10px] font-semibold uppercase tracking-[0.18em]",
              inverted ? "text-white/60" : "text-muted"
            )}
          >
            Team workspace
          </span>
        </span>
      ) : null}
    </span>
  );
}
