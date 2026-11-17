import {
  PROJECT_STATUS_LABELS,
  TASK_PRIORITY_LABELS,
  TASK_STATUS_LABELS,
  cn
} from "../../utils/format";

const TONES = {
  neutral: "bg-surface-muted text-muted ring-line",
  brand: "bg-brand-soft/60 text-brand ring-brand/20",
  success: "bg-success/10 text-success ring-success/20",
  warning: "bg-warning/10 text-warning ring-warning/20",
  danger: "bg-danger/10 text-danger ring-danger/20",
  info: "bg-info/10 text-info ring-info/20"
};

const TASK_STATUS_TONES = {
  TODO: "neutral",
  IN_PROGRESS: "info",
  COMPLETED: "success"
};

const PRIORITY_TONES = {
  LOW: "neutral",
  MEDIUM: "warning",
  HIGH: "danger"
};

const PROJECT_STATUS_TONES = {
  PLANNING: "info",
  ACTIVE: "brand",
  ON_HOLD: "warning",
  COMPLETED: "success"
};

export default function Badge({ tone = "neutral", className, children }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ring-1 ring-inset",
        TONES[tone],
        className
      )}
    >
      {children}
    </span>
  );
}

export function TaskStatusBadge({ status }) {
  return (
    <Badge tone={TASK_STATUS_TONES[status] || "neutral"}>
      {TASK_STATUS_LABELS[status] || status}
    </Badge>
  );
}

export function TaskPriorityBadge({ priority }) {
  return (
    <Badge tone={PRIORITY_TONES[priority] || "neutral"}>
      {TASK_PRIORITY_LABELS[priority] || priority}
    </Badge>
  );
}

export function ProjectStatusBadge({ status }) {
  return (
    <Badge tone={PROJECT_STATUS_TONES[status] || "neutral"}>
      {PROJECT_STATUS_LABELS[status] || status}
    </Badge>
  );
}
