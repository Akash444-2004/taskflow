const DATE_FORMAT = new Intl.DateTimeFormat("en", {
  month: "short",
  day: "numeric",
  year: "numeric"
});

const SHORT_DATE_FORMAT = new Intl.DateTimeFormat("en", {
  month: "short",
  day: "numeric"
});

export function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return DATE_FORMAT.format(date);
}

export function formatShortDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return SHORT_DATE_FORMAT.format(date);
}

export function toDateInput(value) {
  if (!value) return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toISOString().slice(0, 10);
}

export function initials(name) {
  return (name || "?")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

export function cn(...values) {
  return values.filter(Boolean).join(" ");
}

export function percentage(value, total) {
  if (!total) return 0;
  return Math.round((value / total) * 100);
}

export const PROJECT_STATUSES = ["PLANNING", "ACTIVE", "ON_HOLD", "COMPLETED"];
export const TASK_STATUSES = ["TODO", "IN_PROGRESS", "COMPLETED"];
export const TASK_PRIORITIES = ["LOW", "MEDIUM", "HIGH"];

export const TASK_STATUS_LABELS = {
  TODO: "To do",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed"
};

export const TASK_PRIORITY_LABELS = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High"
};

export const PROJECT_STATUS_LABELS = {
  PLANNING: "Planning",
  ACTIVE: "Active",
  ON_HOLD: "On hold",
  COMPLETED: "Completed"
};
