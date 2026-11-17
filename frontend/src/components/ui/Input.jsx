import { useId } from "react";

import { cn } from "../../utils/format";

const FIELD_CLASS =
  "w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm text-ink shadow-sm placeholder:text-muted/70 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20 disabled:cursor-not-allowed disabled:opacity-60";

export default function Input({
  label,
  error,
  hint,
  id,
  className,
  wrapperClassName,
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
      <input
        id={inputId}
        className={cn(FIELD_CLASS, error && "border-danger focus:border-danger", className)}
        aria-invalid={error ? "true" : undefined}
        aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
        {...props}
      />
      {error ? (
        <p id={`${inputId}-error`} className="text-xs font-medium text-danger">
          {error}
        </p>
      ) : hint ? (
        <p id={`${inputId}-hint`} className="text-xs text-muted">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
