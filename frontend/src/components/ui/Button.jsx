import { Loader2 } from "lucide-react";

import { cn } from "../../utils/format";

const VARIANTS = {
  primary:
    "bg-brand text-brand-contrast hover:bg-brand-hover shadow-sm disabled:bg-brand/60",
  secondary:
    "border border-line bg-surface text-ink hover:bg-surface-muted disabled:opacity-60",
  ghost: "text-muted hover:bg-surface-muted hover:text-ink",
  danger: "bg-danger text-white hover:opacity-90 disabled:opacity-60",
  subtle: "bg-brand-soft text-brand hover:bg-brand-soft/70"
};

const SIZES = {
  sm: "h-8 px-3 text-xs",
  md: "h-10 px-4 text-sm",
  lg: "h-11 px-5 text-sm"
};

export default function Button({
  variant = "primary",
  size = "md",
  loading = false,
  className,
  children,
  type = "button",
  disabled,
  ...props
}) {
  return (
    <button
      type={type}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors focus-visible:outline-none disabled:cursor-not-allowed",
        VARIANTS[variant],
        SIZES[size],
        className
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : null}
      {children}
    </button>
  );
}
