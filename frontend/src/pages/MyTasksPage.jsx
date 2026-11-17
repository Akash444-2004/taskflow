import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarClock, ListTodo, Search } from "lucide-react";

import TaskDetailModal from "../components/tasks/TaskDetailModal";
import { TaskPriorityBadge, TaskStatusBadge } from "../components/ui/Badge";
import EmptyState from "../components/ui/EmptyState";
import Input from "../components/ui/Input";
import PageHeader from "../components/ui/PageHeader";
import Spinner from "../components/ui/Spinner";
import { useAuth } from "../context/AuthContext";
import { api, apiErrorMessage } from "../services/api";
import { TASK_STATUSES, TASK_STATUS_LABELS, cn, formatDate } from "../utils/format";

const FILTERS = [{ id: "", label: "All" }, ...TASK_STATUSES.map((status) => ({
  id: status,
  label: TASK_STATUS_LABELS[status]
}))];

export default function MyTasksPage() {
  const { user, isAdmin } = useAuth();
  const [status, setStatus] = useState("");
  const [search, setSearch] = useState("");
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedTask, setSelectedTask] = useState(null);
  const [members, setMembers] = useState([]);

  useEffect(() => {
    let active = true;

    async function run() {
      setLoading(true);
      setError("");
      try {
        const params = { size: 100, sort: "dueDate,asc" };
        if (status) params.status = status;
        const response = await api.get("/tasks/assigned-to-me", { params });
        if (active) setData(response.data);
      } catch (requestError) {
        if (active) setError(apiErrorMessage(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }

    run();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  async function openTask(task) {
    setSelectedTask(task);
    setMembers([]);
    try {
      const { data: memberList } = await api.get(`/projects/${task.projectId}/members`);
      setMembers(memberList);
    } catch {
      // Members are only needed for reassignment; the task remains usable.
    }
  }

  const tasks = (data?.content || []).filter((task) => {
    if (!search.trim()) return true;
    const term = search.trim().toLowerCase();
    return (
      task.title.toLowerCase().includes(term) ||
      (task.projectName || "").toLowerCase().includes(term) ||
      (task.description || "").toLowerCase().includes(term)
    );
  });

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const overdue = tasks.filter(
    (task) => task.dueDate && task.status !== "COMPLETED" && new Date(task.dueDate) < today
  ).length;

  return (
    <div className="space-y-6">
      <PageHeader
        title="My tasks"
        description="Everything assigned to you, across every project."
        actions={
          <div className="flex items-center gap-2 rounded-lg border border-line bg-surface px-3 py-2 text-xs font-medium text-muted">
            <CalendarClock className="h-4 w-4 text-brand" aria-hidden="true" />
            {overdue > 0 ? `${overdue} overdue` : "Nothing overdue"}
          </div>
        }
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-1 rounded-lg border border-line bg-surface p-1">
          {FILTERS.map((filter) => (
            <button
              key={filter.id || "all"}
              type="button"
              onClick={() => setStatus(filter.id)}
              className={cn(
                "rounded-md px-3 py-1.5 text-xs font-semibold transition-colors",
                status === filter.id
                  ? "bg-brand-soft/70 text-brand"
                  : "text-muted hover:bg-surface-muted hover:text-ink"
              )}
            >
              {filter.label}
            </button>
          ))}
        </div>

        <div className="relative sm:w-72">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
            aria-hidden="true"
          />
          <Input
            aria-label="Search my tasks"
            placeholder="Search tasks or projects"
            className="pl-9"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-20">
          <Spinner className="h-7 w-7" label="Loading your tasks" />
        </div>
      ) : error ? (
        <div className="rounded-card border border-danger/25 bg-danger/5 p-6 text-sm text-danger">
          {error}
        </div>
      ) : tasks.length === 0 ? (
        <EmptyState
          icon={ListTodo}
          title={search ? "No matching tasks" : "Nothing assigned to you"}
          description={
            search
              ? "Try a different search term or status filter."
              : status
                ? "No tasks in this status. Try another filter."
                : "When a teammate assigns you a task, it will show up here."
          }
          action={
            <Link
              to="/projects"
              className="inline-flex h-9 items-center rounded-lg bg-brand px-3 text-sm font-semibold text-brand-contrast hover:bg-brand-hover"
            >
              Browse projects
            </Link>
          }
        />
      ) : (
        <div className="overflow-hidden rounded-card border border-line bg-surface shadow-card">
          <ul className="divide-y divide-line">
            {tasks.map((task) => {
              const isOverdue =
                task.dueDate && task.status !== "COMPLETED" && new Date(task.dueDate) < today;
              return (
                <li key={task.id}>
                  <button
                    type="button"
                    onClick={() => openTask(task)}
                    className="flex w-full flex-wrap items-center gap-3 px-4 py-3.5 text-left transition-colors hover:bg-surface-muted/50 sm:px-5"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-ink">{task.title}</p>
                      <p className="mt-0.5 truncate text-xs text-muted">
                        <Link
                          to={`/projects/${task.projectId}`}
                          onClick={(event) => event.stopPropagation()}
                          className="font-medium hover:text-brand hover:underline"
                        >
                          {task.projectName}
                        </Link>
                        {task.description ? ` · ${task.description}` : ""}
                      </p>
                    </div>
                    <TaskPriorityBadge priority={task.priority} />
                    <TaskStatusBadge status={task.status} />
                    <span
                      className={cn(
                        "w-24 text-right text-xs font-medium",
                        isOverdue ? "text-danger" : "text-muted"
                      )}
                    >
                      {task.dueDate ? formatDate(task.dueDate) : "No due date"}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}

      {selectedTask ? (
        <TaskDetailModal
          task={selectedTask}
          members={members}
          currentUserId={user?.id}
          canModerate={isAdmin}
          canDelete={selectedTask.createdBy?.id === user?.id}
          onClose={() => setSelectedTask(null)}
          onChanged={(updated) => {
            setSelectedTask(updated);
            setData((current) =>
              current
                ? {
                    ...current,
                    content: current.content.map((item) =>
                      item.id === updated.id ? updated : item
                    )
                  }
                : current
            );
          }}
          onDeleted={() => {
            setData((current) =>
              current
                ? {
                    ...current,
                    content: current.content.filter((item) => item.id !== selectedTask.id),
                    totalElements: Math.max(0, current.totalElements - 1)
                  }
                : current
            );
            setSelectedTask(null);
          }}
        />
      ) : null}
    </div>
  );
}
