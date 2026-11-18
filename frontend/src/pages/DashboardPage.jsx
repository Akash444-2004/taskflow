import { lazy, Suspense, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  ArrowRight,
  ArrowUpRight,
  CalendarClock,
  CheckCircle2,
  FolderKanban,
  ListTodo,
  Plus,
  Timer,
  Users
} from "lucide-react";
import PageHeader from "../components/ui/PageHeader";
import Badge, { ProjectStatusBadge, TaskPriorityBadge, TaskStatusBadge } from "../components/ui/Badge";
import EmptyState from "../components/ui/EmptyState";
import Skeleton, { SkeletonStatCards } from "../components/ui/Skeleton";
import { useAuth } from "../context/AuthContext";
import { api, apiErrorMessage } from "../services/api";
import useCountUp from "../hooks/useCountUp";
import { formatShortDate, percentage, TASK_STATUS_LABELS } from "../utils/format";

// Recharts is heavy (~100 KB gzipped). Loading it only when the dashboard chart
// renders keeps it out of the initial bundle used by every other route.
const StatusChart = lazy(() => import("../components/dashboard/StatusChart"));

const STATUS_COLORS = {
  TODO: "#94a3b8",
  IN_PROGRESS: "#2f6897",
  COMPLETED: "#1e7a57"
};

const STATUS_ORDER = ["TODO", "IN_PROGRESS", "COMPLETED"];

function StatCard({ label, value, hint, icon: Icon, tone = "brand", to }) {
  const tones = {
    brand: "bg-brand-soft/60 text-brand",
    info: "bg-info/10 text-info",
    success: "bg-success/10 text-success",
    danger: "bg-danger/10 text-danger"
  };

  const body = (
    <>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">{label}</p>
          <p className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-ink">{value}</p>
        </div>
        <span className={`flex h-10 w-10 items-center justify-center rounded-xl ${tones[tone]}`}>
          <Icon className="h-5 w-5" aria-hidden="true" />
        </span>
      </div>
      {hint ? <p className="mt-3 text-xs text-muted">{hint}</p> : null}
    </>
  );

  if (to) {
    return (
      <Link
        to={to}
        title={`Open ${label.toLowerCase()}`}
        className="group block rounded-card border border-line bg-surface p-5 shadow-card transition-all hover:-translate-y-0.5 hover:border-brand/40 hover:shadow-float"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0 flex-1">{body}</div>
          <ArrowUpRight
            className="mt-1 h-4 w-4 shrink-0 text-muted opacity-0 transition-opacity group-hover:opacity-100"
            aria-hidden="true"
          />
        </div>
      </Link>
    );
  }

  return (
    <div className="rounded-card border border-line bg-surface p-5 shadow-card">
      {body}
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const response = await api.get("/dashboard");
        if (active) setData(response.data);
      } catch (requestError) {
        if (active) setError(apiErrorMessage(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }

    load();
    return () => {
      active = false;
    };
  }, []);

  const total = data?.assignedTaskCount || 0;
  const chartData = STATUS_ORDER.map((status) => {
    const value = data?.tasksByStatus?.[status] || 0;
    return {
      name: status,
      label: TASK_STATUS_LABELS[status],
      value,
      color: STATUS_COLORS[status]
    };
  })
    .map((entry) => ({ ...entry, percent: percentage(entry.value, total) }))
    .filter((entry) => entry.value > 0);

  const completionRate = percentage(data?.completedTaskCount || 0, total);
  const projectCount = useCountUp(data?.projectCount || 0);
  const assignedCount = useCountUp(total);
  const dueSoonCount = useCountUp(data?.dueSoonTaskCount || 0);
  const overdueCount = useCountUp(data?.overdueTaskCount || 0);

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Good to see you, ${user?.fullName?.split(" ")[0] || "there"}`}
        description="Here is what is moving across your projects today."
        actions={
          <Link
            to="/projects"
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-brand px-4 text-sm font-semibold text-brand-contrast hover:bg-brand-hover"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            New project
          </Link>
        }
      />

      {loading ? (
        <div className="space-y-6">
          <SkeletonStatCards />
          <div className="grid gap-6 lg:grid-cols-3">
            <Skeleton className="h-72 lg:col-span-2" />
            <Skeleton className="h-72" />
          </div>
        </div>
      ) : error ? (
        <div className="rounded-card border border-danger/25 bg-danger/5 p-6 text-sm text-danger">
          {error}
        </div>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="My projects"
              value={projectCount}
              hint="Projects you own or belong to"
              icon={FolderKanban}
              tone="brand"
              to="/projects"
            />
            <StatCard
              label="Assigned tasks"
              value={assignedCount}
              hint={`${data.completedTaskCount} completed · ${completionRate}% done`}
              icon={ListTodo}
              tone="info"
              to="/tasks"
            />
            <StatCard
              label="Due this week"
              value={dueSoonCount}
              hint="Tasks due in the next seven days"
              icon={CalendarClock}
              tone="success"
              to="/tasks?due=soon"
            />
            <StatCard
              label="Overdue"
              value={overdueCount}
              hint="Needs attention soon"
              icon={AlertTriangle}
              tone="danger"
              to="/tasks?due=overdue"
            />
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <section className="rounded-card border border-line bg-surface shadow-card lg:col-span-2">
              <div className="flex items-center justify-between border-b border-line px-5 py-4">
                <div>
                  <h2 className="text-sm font-semibold text-ink">Upcoming deadlines</h2>
                  <p className="text-xs text-muted">Assigned to you across every project</p>
                </div>
                <Link
                  to="/tasks"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-brand hover:underline"
                >
                  View all
                  <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
                </Link>
              </div>

              {data.upcomingTasks.length === 0 ? (
                <div className="p-5">
                  <EmptyState
                    icon={CheckCircle2}
                    title="No upcoming deadlines"
                    description="Tasks assigned to you with a due date will appear here."
                  />
                </div>
              ) : (
                <ul className="divide-y divide-line">
                  {data.upcomingTasks.map((task) => (
                    <li key={task.id}>
                      <Link
                        to={`/projects/${task.projectId}?task=${task.id}`}
                        className="flex flex-wrap items-center gap-3 px-5 py-3.5 transition-colors hover:bg-surface-muted/50"
                      >
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-muted text-muted">
                          <Timer className="h-4 w-4" aria-hidden="true" />
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-semibold text-ink">{task.title}</p>
                          <p className="truncate text-xs text-muted">{task.projectName}</p>
                        </div>
                        <TaskPriorityBadge priority={task.priority} />
                        <TaskStatusBadge status={task.status} />
                        <span className="w-20 text-right text-xs font-medium text-muted">
                          {formatShortDate(task.dueDate)}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-card border border-line bg-surface p-5 shadow-card">
              <h2 className="text-sm font-semibold text-ink">Tasks by status</h2>
              <p className="text-xs text-muted">Assigned work distribution</p>

              {chartData.length === 0 ? (
                <div className="flex h-56 items-center justify-center text-sm text-muted">
                  No assigned tasks yet.
                </div>
              ) : (
                <Suspense
                  fallback={
                    <div className="mt-4 space-y-3">
                      <Skeleton className="mx-auto h-44 w-44 rounded-full" />
                      <Skeleton className="h-4 w-full" />
                      <Skeleton className="h-4 w-2/3" />
                    </div>
                  }
                >
                  <StatusChart chartData={chartData} total={total} />
                </Suspense>
              )}
            </section>
          </div>

          <section className="rounded-card border border-line bg-surface shadow-card">
            <div className="flex items-center justify-between border-b border-line px-5 py-4">
              <div>
                <h2 className="text-sm font-semibold text-ink">Recent projects</h2>
                <p className="text-xs text-muted">Most recently updated work</p>
              </div>
              <Link
                to="/projects"
                className="inline-flex items-center gap-1 text-xs font-semibold text-brand hover:underline"
              >
                All projects
                <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
              </Link>
            </div>

            {data.recentProjects.length === 0 ? (
              <div className="p-5">
                <EmptyState
                  icon={FolderKanban}
                  title="No projects yet"
                  description="Create your first project to start planning work."
                  action={
                    <Link
                      to="/projects"
                      className="inline-flex h-9 items-center rounded-lg bg-brand px-3 text-sm font-semibold text-brand-contrast hover:bg-brand-hover"
                    >
                      Create project
                    </Link>
                  }
                />
              </div>
            ) : (
              <div className="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-3">
                {data.recentProjects.map((project) => {
                  const progress = percentage(project.completedTaskCount, project.taskCount);
                  return (
                    <Link
                      key={project.id}
                      to={`/projects/${project.id}`}
                      className="group rounded-xl border border-line bg-surface-muted/30 p-4 transition-colors hover:border-brand/30 hover:bg-surface-muted/60"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-ink group-hover:text-brand">
                            {project.name}
                          </p>
                          <p className="mt-0.5 line-clamp-2 text-xs text-muted">
                            {project.description || "No description"}
                          </p>
                        </div>
                        <ProjectStatusBadge status={project.status} />
                      </div>

                      <div className="mt-4">
                        <div className="flex items-center justify-between text-[11px] font-medium text-muted">
                          <span>
                            {project.completedTaskCount} of {project.taskCount} tasks
                          </span>
                          <span>{progress}%</span>
                        </div>
                        <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-line">
                          <div
                            className="h-full rounded-full bg-brand transition-all duration-700 ease-out"
                            style={{ width: `${progress}%` }}
                          />
                        </div>
                      </div>

                      <div className="mt-3 flex items-center gap-4 text-[11px] text-muted">
                        <span className="inline-flex items-center gap-1">
                          <Users className="h-3.5 w-3.5" aria-hidden="true" />
                          {project.memberCount} {project.memberCount === 1 ? "member" : "members"}
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <ListTodo className="h-3.5 w-3.5" aria-hidden="true" />
                          {project.taskCount} tasks
                        </span>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
