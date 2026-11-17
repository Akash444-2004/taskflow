import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  Columns3,
  ListTodo,
  LogOut,
  Plus,
  Search,
  Settings,
  Trash2,
  UserPlus,
  Users
} from "lucide-react";

import ProjectForm from "../components/projects/ProjectForm";
import TaskDetailModal from "../components/tasks/TaskDetailModal";
import TaskForm from "../components/tasks/TaskForm";
import Avatar from "../components/ui/Avatar";
import { ProjectStatusBadge, TaskPriorityBadge } from "../components/ui/Badge";
import Button from "../components/ui/Button";
import EmptyState from "../components/ui/EmptyState";
import Input from "../components/ui/Input";
import Modal from "../components/ui/Modal";
import PageHeader from "../components/ui/PageHeader";
import Select from "../components/ui/Select";
import Spinner from "../components/ui/Spinner";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, apiErrorMessage, fieldErrors } from "../services/api";
import {
  TASK_PRIORITIES,
  TASK_PRIORITY_LABELS,
  TASK_STATUSES,
  TASK_STATUS_LABELS,
  cn,
  formatDate,
  percentage
} from "../utils/format";

const TABS = [
  { id: "board", label: "Board", icon: Columns3 },
  { id: "list", label: "List", icon: ListTodo },
  { id: "members", label: "Members", icon: Users },
  { id: "settings", label: "Settings", icon: Settings }
];

const COLUMN_TONES = {
  TODO: "border-t-muted/40",
  IN_PROGRESS: "border-t-info/60",
  COMPLETED: "border-t-success/60"
};

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();
  const toast = useToast();

  const [project, setProject] = useState(null);
  const [members, setMembers] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState("board");
  const [filters, setFilters] = useState({ search: "", priority: "", assignedToId: "" });

  const [taskModalOpen, setTaskModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [selectedTask, setSelectedTask] = useState(null);
  const [taskSubmitting, setTaskSubmitting] = useState(false);
  const [taskServerErrors, setTaskServerErrors] = useState({});
  const [taskModalError, setTaskModalError] = useState("");

  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editServerErrors, setEditServerErrors] = useState({});
  const [editError, setEditError] = useState("");

  const [memberEmail, setMemberEmail] = useState("");
  const [memberSubmitting, setMemberSubmitting] = useState(false);
  const [memberError, setMemberError] = useState("");
  const [actionBusy, setActionBusy] = useState("");

  const loadedTaskParam = useRef(null);

  const loadProject = useCallback(async () => {
    const { data } = await api.get(`/projects/${projectId}`);
    setProject(data);
    return data;
  }, [projectId]);

  const loadMembers = useCallback(async () => {
    const { data } = await api.get(`/projects/${projectId}/members`);
    setMembers(data);
    return data;
  }, [projectId]);

  const loadTasks = useCallback(async () => {
    const params = { size: 200, sort: "createdAt,asc" };
    if (filters.priority) params.priority = filters.priority;
    if (filters.assignedToId) params.assignedToId = filters.assignedToId;
    if (filters.search.trim()) params.search = filters.search.trim();
    const { data } = await api.get(`/projects/${projectId}/tasks`, { params });
    setTasks(data.content);
    return data.content;
  }, [projectId, filters.priority, filters.assignedToId, filters.search]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    Promise.all([loadProject(), loadMembers(), loadTasks()])
      .then(() => {
        if (active) setError("");
      })
      .catch((requestError) => {
        if (active) setError(apiErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
    // Initial load only; filters are handled by a dedicated effect below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);

  useEffect(() => {
    let active = true;

    async function refreshTasks() {
      try {
        const params = { size: 200, sort: "createdAt,asc" };
        if (filters.priority) params.priority = filters.priority;
        if (filters.assignedToId) params.assignedToId = filters.assignedToId;
        if (filters.search.trim()) params.search = filters.search.trim();
        const { data } = await api.get(`/projects/${projectId}/tasks`, { params });
        if (active) setTasks(data.content);
      } catch (requestError) {
        if (active) toast.error(apiErrorMessage(requestError));
      }
    }

    const timeout = window.setTimeout(refreshTasks, filters.search ? 300 : 0);
    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, filters.priority, filters.assignedToId, filters.search]);

  const taskParam = searchParams.get("task");

  useEffect(() => {
    if (!taskParam || loading || selectedTask) return;
    if (loadedTaskParam.current === taskParam) return;

    const existing = tasks.find((item) => String(item.id) === taskParam);
    loadedTaskParam.current = taskParam;

    if (existing) {
      setSelectedTask(existing);
      return;
    }

    let active = true;
    api
      .get(`/tasks/${taskParam}`)
      .then((response) => {
        if (active) setSelectedTask(response.data);
      })
      .catch(() => {
        if (active) toast.error("That task could not be opened.");
      });

    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [taskParam, loading, tasks]);

  const isOwner = project?.myRole === "OWNER";
  const canModerate = isOwner || isAdmin;

  const groupedTasks = useMemo(() => {
    const groups = { TODO: [], IN_PROGRESS: [], COMPLETED: [] };
    tasks.forEach((task) => {
      (groups[task.status] || groups.TODO).push(task);
    });
    return groups;
  }, [tasks]);

  const progress = percentage(project?.completedTaskCount || 0, project?.taskCount || 0);

  function refreshTaskInList(updated) {
    setTasks((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    setSelectedTask(updated);
    loadProject().catch(() => {});
  }

  async function reloadAll() {
    try {
      await Promise.all([loadProject(), loadMembers(), loadTasks()]);
    } catch (requestError) {
      toast.error(apiErrorMessage(requestError));
    }
  }

  function openCreateTask() {
    setEditingTask(null);
    setTaskServerErrors({});
    setTaskModalError("");
    setTaskModalOpen(true);
  }

  function openEditTask(task) {
    setEditingTask(task);
    setTaskServerErrors({});
    setTaskModalError("");
    setTaskModalOpen(true);
  }

  async function submitTask(values) {
    setTaskSubmitting(true);
    setTaskServerErrors({});
    setTaskModalError("");
    try {
      if (editingTask) {
        const { data } = await api.put(`/tasks/${editingTask.id}`, values);
        toast.success("Task updated.");
        if (selectedTask?.id === data.id) setSelectedTask(data);
      } else {
        await api.post(`/projects/${projectId}/tasks`, values);
        toast.success("Task created.");
      }
      setTaskModalOpen(false);
      setEditingTask(null);
      await reloadAll();
    } catch (requestError) {
      setTaskServerErrors(fieldErrors(requestError));
      setTaskModalError(apiErrorMessage(requestError));
    } finally {
      setTaskSubmitting(false);
    }
  }

  async function saveProject(values) {
    setEditSubmitting(true);
    setEditServerErrors({});
    setEditError("");
    try {
      const { data } = await api.put(`/projects/${projectId}`, values);
      setProject(data);
      toast.success("Project updated.");
    } catch (requestError) {
      setEditServerErrors(fieldErrors(requestError));
      setEditError(apiErrorMessage(requestError));
    } finally {
      setEditSubmitting(false);
    }
  }

  async function addMember(event) {
    event.preventDefault();
    const email = memberEmail.trim();
    if (!email) {
      setMemberError("Enter the email address of an existing account.");
      return;
    }

    setMemberSubmitting(true);
    setMemberError("");
    try {
      const { data } = await api.post(`/projects/${projectId}/members`, { email });
      setMembers((current) => [...current, data]);
      setMemberEmail("");
      setProject((current) =>
        current ? { ...current, memberCount: current.memberCount + 1 } : current
      );
      toast.success(`${data.user.fullName} joined the project.`);
    } catch (requestError) {
      setMemberError(apiErrorMessage(requestError));
    } finally {
      setMemberSubmitting(false);
    }
  }

  async function removeMember(member) {
    if (!window.confirm(`Remove ${member.user.fullName} from this project?`)) return;
    setActionBusy(`member-${member.user.id}`);
    try {
      await api.delete(`/projects/${projectId}/members/${member.user.id}`);
      setMembers((current) => current.filter((item) => item.id !== member.id));
      setProject((current) =>
        current ? { ...current, memberCount: Math.max(1, current.memberCount - 1) } : current
      );
      toast.success("Member removed.");
    } catch (requestError) {
      toast.error(apiErrorMessage(requestError));
    } finally {
      setActionBusy("");
    }
  }

  async function leaveProject() {
    if (!window.confirm("Leave this project? You will lose access to its tasks.")) return;
    setActionBusy("leave");
    try {
      await api.delete(`/projects/${projectId}/members/me`);
      toast.success("You left the project.");
      navigate("/projects", { replace: true });
    } catch (requestError) {
      toast.error(apiErrorMessage(requestError));
      setActionBusy("");
    }
  }

  async function deleteProject() {
    if (
      !window.confirm(
        `Delete "${project.name}"? All tasks and comments will be permanently removed.`
      )
    ) {
      return;
    }
    setActionBusy("delete");
    try {
      await api.delete(`/projects/${projectId}`);
      toast.success("Project deleted.");
      navigate("/projects", { replace: true });
    } catch (requestError) {
      toast.error(apiErrorMessage(requestError));
      setActionBusy("");
    }
  }

  function closeTaskDetail() {
    setSelectedTask(null);
    if (taskParam) {
      const next = new URLSearchParams(searchParams);
      next.delete("task");
      setSearchParams(next, { replace: true });
    }
    loadedTaskParam.current = null;
  }

  function updateFilter(field) {
    return (event) => {
      setFilters((current) => ({ ...current, [field]: event.target.value }));
    };
  }

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-8 w-8" label="Loading project" />
      </div>
    );
  }

  if (error || !project) {
    return (
      <div className="space-y-4">
        <Link
          to="/projects"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-ink"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Back to projects
        </Link>
        <div className="rounded-card border border-danger/25 bg-danger/5 p-8 text-center">
          <h1 className="text-lg font-semibold text-danger">Unable to open this project</h1>
          <p className="mt-1 text-sm text-danger/80">
            {error || "The project may have been deleted, or you no longer have access."}
          </p>
        </div>
      </div>
    );
  }

  const taskCard = (task) => {
    const canDelete = isOwner || task.createdBy.id === user?.id;
    return (
      <article
        key={task.id}
        className="group rounded-xl border border-line bg-surface p-3.5 shadow-sm transition-all hover:border-brand/30 hover:shadow-card"
      >
        <button
          type="button"
          onClick={() => setSelectedTask(task)}
          className="block w-full text-left"
        >
          <p className="text-sm font-semibold leading-5 text-ink group-hover:text-brand">
            {task.title}
          </p>
          {task.description ? (
            <p className="mt-1 line-clamp-2 text-xs leading-5 text-muted">{task.description}</p>
          ) : null}
        </button>

        <div className="mt-3 flex flex-wrap items-center gap-1.5">
          <TaskPriorityBadge priority={task.priority} />
          {task.dueDate ? (
            <span className="inline-flex items-center gap-1 text-[11px] text-muted">
              <CalendarDays className="h-3 w-3" aria-hidden="true" />
              {formatDate(task.dueDate)}
            </span>
          ) : null}
        </div>

        <div className="mt-3 flex items-center justify-between border-t border-line pt-3">
          <div className="flex items-center gap-2">
            {task.assignedTo ? (
              <Avatar name={task.assignedTo.fullName} size="xs" />
            ) : (
              <span className="flex h-6 w-6 items-center justify-center rounded-full border border-dashed border-line text-[10px] text-muted">
                ?
              </span>
            )}
            <span className="max-w-[9rem] truncate text-[11px] text-muted">
              {task.assignedTo ? task.assignedTo.fullName : "Unassigned"}
            </span>
          </div>
          <div className="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
            <button
              type="button"
              onClick={() => openEditTask(task)}
              className="rounded p-1 text-xs font-semibold text-brand hover:bg-brand-soft/50"
            >
              Edit
            </button>
            {canDelete ? (
              <button
                type="button"
                onClick={async () => {
                  if (!window.confirm("Delete this task and its comments?")) return;
                  try {
                    await api.delete(`/tasks/${task.id}`);
                    setTasks((current) => current.filter((item) => item.id !== task.id));
                    if (selectedTask?.id === task.id) setSelectedTask(null);
                    toast.success("Task deleted.");
                    loadProject().catch(() => {});
                  } catch (requestError) {
                    toast.error(apiErrorMessage(requestError));
                  }
                }}
                className="rounded p-1 text-muted hover:bg-danger/10 hover:text-danger"
                aria-label={`Delete ${task.title}`}
              >
                <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
              </button>
            ) : null}
          </div>
        </div>
      </article>
    );
  };

  return (
    <div className="space-y-6">
      <Link
        to="/projects"
        className="inline-flex items-center gap-2 text-sm font-medium text-muted hover:text-ink"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to projects
      </Link>

      <PageHeader
        title={project.name}
        description={project.description || "No description provided."}
        actions={
          <>
            <Button onClick={openCreateTask}>
              <Plus className="h-4 w-4" aria-hidden="true" />
              New task
            </Button>
          </>
        }
        className="border-b-0 pb-0"
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-card border border-line bg-surface p-4 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">Status</p>
          <div className="mt-2">
            <ProjectStatusBadge status={project.status} />
          </div>
        </div>
        <div className="rounded-card border border-line bg-surface p-4 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">Progress</p>
          <p className="mt-2 text-lg font-semibold text-ink">{progress}%</p>
          <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-line">
            <div className="h-full rounded-full bg-brand" style={{ width: `${progress}%` }} />
          </div>
        </div>
        <div className="rounded-card border border-line bg-surface p-4 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">Schedule</p>
          <p className="mt-2 text-sm font-semibold text-ink">
            {formatDate(project.startDate)} → {formatDate(project.dueDate)}
          </p>
        </div>
        <div className="rounded-card border border-line bg-surface p-4 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">Team</p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-sm font-semibold text-ink">
              {project.memberCount} {project.memberCount === 1 ? "member" : "members"}
            </p>
            <div className="flex -space-x-2">
              {members.slice(0, 4).map((member) => (
                <Avatar
                  key={member.id}
                  name={member.user.fullName}
                  size="xs"
                  className="ring-2 ring-surface"
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line">
        <nav className="-mb-px flex gap-1 overflow-x-auto" aria-label="Project sections">
          {TABS.map(({ id, label, icon: Icon }) => {
            const disabled = id === "settings" && !isOwner;
            return (
              <button
                key={id}
                type="button"
                disabled={disabled}
                onClick={() => setTab(id)}
                className={cn(
                  "inline-flex shrink-0 items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium transition-colors",
                  tab === id
                    ? "border-brand text-brand"
                    : "border-transparent text-muted hover:text-ink",
                  disabled && "cursor-not-allowed opacity-40"
                )}
              >
                <Icon className="h-4 w-4" aria-hidden="true" />
                {label}
              </button>
            );
          })}
        </nav>

        {tab === "board" || tab === "list" ? (
          <div className="flex w-full flex-col gap-2 pb-2 sm:w-auto sm:flex-row">
            <div className="relative">
              <Search
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
                aria-hidden="true"
              />
              <Input
                aria-label="Search tasks"
                placeholder="Search tasks"
                className="pl-9 sm:w-56"
                value={filters.search}
                onChange={updateFilter("search")}
              />
            </div>
            <Select
              aria-label="Filter priority"
              value={filters.priority}
              onChange={updateFilter("priority")}
              wrapperClassName="sm:w-36"
            >
              <option value="">All priorities</option>
              {TASK_PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {TASK_PRIORITY_LABELS[priority]}
                </option>
              ))}
            </Select>
            <Select
              aria-label="Filter assignee"
              value={filters.assignedToId}
              onChange={updateFilter("assignedToId")}
              wrapperClassName="sm:w-44"
            >
              <option value="">Everyone</option>
              {members.map((member) => (
                <option key={member.id} value={member.user.id}>
                  {member.user.fullName}
                </option>
              ))}
            </Select>
          </div>
        ) : null}
      </div>

      {tab === "board" ? (
        <div className="grid gap-4 lg:grid-cols-3">
          {TASK_STATUSES.map((status) => (
            <section
              key={status}
              className={cn(
                "rounded-card border border-t-2 border-line bg-surface-muted/30 p-3",
                COLUMN_TONES[status]
              )}
            >
              <header className="flex items-center justify-between px-1 pb-3">
                <h2 className="text-sm font-semibold text-ink">{TASK_STATUS_LABELS[status]}</h2>
                <span className="rounded-full bg-surface px-2 py-0.5 text-[11px] font-semibold text-muted">
                  {groupedTasks[status].length}
                </span>
              </header>
              <div className="space-y-2.5">
                {groupedTasks[status].length === 0 ? (
                  <p className="rounded-xl border border-dashed border-line px-3 py-6 text-center text-xs text-muted">
                    No tasks here.
                  </p>
                ) : (
                  groupedTasks[status].map(taskCard)
                )}
              </div>
            </section>
          ))}
        </div>
      ) : null}

      {tab === "list" ? (
        tasks.length === 0 ? (
          <EmptyState
            icon={ListTodo}
            title="No tasks found"
            description={
              filters.search || filters.priority || filters.assignedToId
                ? "Try clearing the filters to see all tasks."
                : "Create the first task for this project."
            }
            action={
              <Button onClick={openCreateTask}>
                <Plus className="h-4 w-4" aria-hidden="true" />
                Create task
              </Button>
            }
          />
        ) : (
          <div className="overflow-hidden rounded-card border border-line bg-surface shadow-card">
            <table className="min-w-full divide-y divide-line text-sm">
              <thead className="bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                <tr>
                  <th className="px-4 py-3 font-semibold">Task</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Priority</th>
                  <th className="px-4 py-3 font-semibold">Assignee</th>
                  <th className="px-4 py-3 font-semibold">Due</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {tasks.map((task) => (
                  <tr key={task.id} className="hover:bg-surface-muted/40">
                    <td className="max-w-xs px-4 py-3">
                      <button
                        type="button"
                        onClick={() => setSelectedTask(task)}
                        className="truncate text-left font-semibold text-ink hover:text-brand"
                      >
                        {task.title}
                      </button>
                      <p className="truncate text-xs text-muted">
                        {task.description || "No description"}
                      </p>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-xs font-semibold text-ink">
                        {TASK_STATUS_LABELS[task.status]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <TaskPriorityBadge priority={task.priority} />
                    </td>
                    <td className="px-4 py-3">
                      {task.assignedTo ? (
                        <span className="inline-flex items-center gap-2 text-xs text-ink">
                          <Avatar name={task.assignedTo.fullName} size="xs" />
                          {task.assignedTo.fullName}
                        </span>
                      ) : (
                        <span className="text-xs text-muted">Unassigned</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-xs text-muted">{formatDate(task.dueDate)}</td>
                    <td className="px-4 py-3 text-right">
                      <Button variant="ghost" size="sm" onClick={() => openEditTask(task)}>
                        Edit
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      {tab === "members" ? (
        <div className="grid gap-6 lg:grid-cols-3">
          <section className="rounded-card border border-line bg-surface shadow-card lg:col-span-2">
            <div className="border-b border-line px-5 py-4">
              <h2 className="text-sm font-semibold text-ink">Project members</h2>
              <p className="text-xs text-muted">Everyone who can view and work on this project.</p>
            </div>
            <ul className="divide-y divide-line">
              {members.map((member) => (
                <li key={member.id} className="flex items-center gap-3 px-5 py-3.5">
                  <Avatar name={member.user.fullName} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-ink">
                      {member.user.fullName}
                      {member.user.id === user?.id ? (
                        <span className="ml-2 text-xs font-normal text-muted">(you)</span>
                      ) : null}
                    </p>
                    <p className="truncate text-xs text-muted">{member.user.email}</p>
                  </div>
                  <span className="rounded-full bg-surface-muted px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-muted">
                    {member.projectRole === "OWNER" ? "Owner" : "Member"}
                  </span>
                  {isOwner && member.projectRole !== "OWNER" ? (
                    <Button
                      variant="ghost"
                      size="sm"
                      loading={actionBusy === `member-${member.user.id}`}
                      onClick={() => removeMember(member)}
                    >
                      Remove
                    </Button>
                  ) : null}
                </li>
              ))}
            </ul>
          </section>

          <aside className="space-y-4">
            {isOwner ? (
              <section className="rounded-card border border-line bg-surface p-5 shadow-card">
                <h2 className="flex items-center gap-2 text-sm font-semibold text-ink">
                  <UserPlus className="h-4 w-4 text-brand" aria-hidden="true" />
                  Add a member
                </h2>
                <p className="mt-1 text-xs text-muted">
                  Enter the email of an existing TaskFlow account.
                </p>
                <form className="mt-4 space-y-3" onSubmit={addMember}>
                  <Input
                    aria-label="Member email"
                    type="email"
                    placeholder="teammate@example.com"
                    value={memberEmail}
                    onChange={(event) => {
                      setMemberEmail(event.target.value);
                      setMemberError("");
                    }}
                    error={memberError}
                  />
                  <Button type="submit" className="w-full" loading={memberSubmitting}>
                    Add member
                  </Button>
                </form>
              </section>
            ) : null}

            <section className="rounded-card border border-line bg-surface p-5 shadow-card">
              <h2 className="text-sm font-semibold text-ink">Membership</h2>
              <p className="mt-1 text-xs text-muted">
                {isOwner
                  ? "As the owner you can delete the project, but you cannot leave it."
                  : "You can leave this project at any time."}
              </p>
              {!isOwner ? (
                <Button
                  variant="secondary"
                  className="mt-4 w-full"
                  loading={actionBusy === "leave"}
                  onClick={leaveProject}
                >
                  <LogOut className="h-4 w-4" aria-hidden="true" />
                  Leave project
                </Button>
              ) : null}
            </section>
          </aside>
        </div>
      ) : null}

      {tab === "settings" && isOwner ? (
        <div className="grid gap-6 lg:grid-cols-3">
          <section className="rounded-card border border-line bg-surface p-5 shadow-card lg:col-span-2">
            <h2 className="text-sm font-semibold text-ink">Project details</h2>
            <p className="text-xs text-muted">
              Only the project owner can update these details.
            </p>
            <div className="mt-5">
              <ProjectForm
                initialValues={project}
                onSubmit={saveProject}
                submitting={editSubmitting}
                serverErrors={editServerErrors}
                submitLabel="Save changes"
              />
            </div>
            {editError ? (
              <p className="mt-3 rounded-lg border border-danger/25 bg-danger/5 px-3 py-2 text-sm text-danger">
                {editError}
              </p>
            ) : null}
          </section>

          <aside className="space-y-4">
            <section className="rounded-card border border-line bg-surface p-5 shadow-card">
              <h2 className="text-sm font-semibold text-ink">Ownership</h2>
              <div className="mt-3 flex items-center gap-3">
                <Avatar name={project.owner.fullName} size="md" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-ink">
                    {project.owner.fullName}
                  </p>
                  <p className="truncate text-xs text-muted">{project.owner.email}</p>
                </div>
              </div>
              <dl className="mt-4 space-y-2 border-t border-line pt-3 text-xs">
                <div className="flex justify-between gap-3">
                  <dt className="text-muted">Created</dt>
                  <dd className="font-medium text-ink">{formatDate(project.createdAt)}</dd>
                </div>
                <div className="flex justify-between gap-3">
                  <dt className="text-muted">Updated</dt>
                  <dd className="font-medium text-ink">{formatDate(project.updatedAt)}</dd>
                </div>
              </dl>
            </section>

            {isOwner ? (
              <section className="rounded-card border border-danger/30 bg-danger/5 p-5">
                <h2 className="text-sm font-semibold text-danger">Danger zone</h2>
                <p className="mt-1 text-xs text-danger/80">
                  Deleting a project permanently removes all of its tasks and comments.
                </p>
                <Button
                  variant="danger"
                  className="mt-4 w-full"
                  loading={actionBusy === "delete"}
                  onClick={deleteProject}
                >
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                  Delete project
                </Button>
              </section>
            ) : null}
          </aside>
        </div>
      ) : null}

      <Modal
        open={taskModalOpen}
        onClose={() => (taskSubmitting ? null : setTaskModalOpen(false))}
        title={editingTask ? "Edit task" : "Create a task"}
        description={
          editingTask
            ? "Update the task, assignment, or schedule."
            : "Tasks can be assigned to any project member."
        }
      >
        {taskModalError ? (
          <div className="mb-4 rounded-lg border border-danger/25 bg-danger/5 px-3 py-2 text-sm text-danger">
            {taskModalError}
          </div>
        ) : null}
        <TaskForm
          key={editingTask?.id || "new"}
          initialValues={editingTask}
          members={members}
          onSubmit={submitTask}
          onCancel={() => setTaskModalOpen(false)}
          submitting={taskSubmitting}
          serverErrors={taskServerErrors}
          submitLabel={editingTask ? "Save changes" : "Create task"}
        />
      </Modal>

      {selectedTask ? (
        <TaskDetailModal
          task={selectedTask}
          members={members}
          currentUserId={user?.id}
          canModerate={canModerate}
          canDelete={isOwner || selectedTask.createdBy?.id === user?.id}
          onClose={closeTaskDetail}
          onChanged={refreshTaskInList}
          onDeleted={(deleted) => {
            setTasks((current) => current.filter((item) => item.id !== deleted.id));
            setSelectedTask(null);
            loadProject().catch(() => {});
          }}
        />
      ) : null}

    </div>
  );
}
