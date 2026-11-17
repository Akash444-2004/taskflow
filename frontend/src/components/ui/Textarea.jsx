import { useId } from "react";

import { cn } from "../../utils/format";

export default function Textarea({
  label,
  error,
  hint,
  id,
  className,
  wrapperClassName,
  rows = 4,
  ...props
}) {
  const generatedId = useId();
  const inputId = id || generatedId;

  return (
    <div className={cn("space-y-1.5", wrapperClassName)}>
      {label ? (
        <label htmlFor={inputId} className="block text-xs font-semibold text-ink">
          {label}
        </label>
      ) : null}
      <textarea
        id={inputId}
        rows={rows}
        className={cn(
          "w-full resize-y rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink shadow-sm placeholder:text-muted/70 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20",
          error && "border-danger",
          className
        )}
        aria-invalid={error ? "true" : undefined}
        {...props}
      />
      {error ? (
        <p className="text-xs font-medium text-danger">{error}</p>
      ) : hint ? (
        <p className="text-xs text-muted">{hint}</p>
      ) : null}
    </div>
  );
}
