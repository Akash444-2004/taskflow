import { cn, initials } from "../../utils/format";

const SIZES = {
  xs: "h-6 w-6 text-[10px]",
  sm: "h-8 w-8 text-xs",
  md: "h-10 w-10 text-sm",
  lg: "h-12 w-12 text-base"
};

const PALETTES = [
  "bg-brand-soft text-brand",
  "bg-info/15 text-info",
  "bg-warning/15 text-warning",
  "bg-success/15 text-success",
  "bg-danger/15 text-danger"
];

function paletteFor(name) {
  const sum = [...(name || "")].reduce((total, char) => total + char.charCodeAt(0), 0);
  return PALETTES[sum % PALETTES.length];
}

export default function Avatar({ name, size = "md", className }) {
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full font-semibold uppercase",
        SIZES[size],
        paletteFor(name),
        className
      )}
      title={name}
      aria-label={name}
    >
      {initials(name)}
    </span>
  );
}
