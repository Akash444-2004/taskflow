import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FolderKanban, ListTodo, Plus, Search, Users } from "lucide-react";

import ProjectForm from "../components/projects/ProjectForm";
import { ProjectStatusBadge } from "../components/ui/Badge";
import Button from "../components/ui/Button";
import EmptyState from "../components/ui/EmptyState";
import Input from "../components/ui/Input";
import Modal from "../components/ui/Modal";
import PageHeader from "../components/ui/PageHeader";
import Select from "../components/ui/Select";
import Spinner from "../components/ui/Spinner";
import { useToast } from "../context/ToastContext";
import { api, apiErrorMessage, fieldErrors } from "../services/api";
import { PROJECT_STATUSES, PROJECT_STATUS_LABELS, percentage } from "../utils/format";

export default function ProjectsPage() {
  const toast = useToast();
  const [filters, setFilters] = useState({ search: "", status: "" });
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [serverErrors, setServerErrors] = useState({});
  const [modalError, setModalError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = { page, size: 12, sort: "updatedAt,desc" };
      if (filters.search.trim()) params.search = filters.search.trim();
      if (filters.status) params.status = filters.status;
      const response = await api.get("/projects", { params });
      setData(response.data);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page, filters.search, filters.status]);

  useEffect(() => {
    const timeout = window.setTimeout(load, filters.search ? 300 : 0);
    return () => window.clearTimeout(timeout);
  }, [load, filters.search]);

  function updateFilter(field) {
    return (event) => {
      setPage(0);
      setFilters((current) => ({ ...current, [field]: event.target.value }));
    };
  }

  async function handleCreate(values) {
    setSubmitting(true);
    setServerErrors({});
    setModalError("");
    try {
      await api.post("/projects", values);
      toast.success("Project created.");
      setCreateOpen(false);
      setPage(0);
      await load();
    } catch (requestError) {
      setServerErrors(fieldErrors(requestError));
      setModalError(apiErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  const projects = data?.content || [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Projects"
        description="Everything you own or collaborate on."
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" aria-hidden="true" />
            New project
          </Button>
        }
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
            aria-hidden="true"
          />
          <Input
            aria-label="Search projects"
            placeholder="Search by name or description"
            className="pl-9"
            value={filters.search}
            onChange={updateFilter("search")}
          />
        </div>
        <Select
          aria-label="Filter by status"
          value={filters.status}
          onChange={updateFilter("status")}
          wrapperClassName="sm:w-52"
        >
          <option value="">All statuses</option>
          {PROJECT_STATUSES.map((status) => (
            <option key={status} value={status}>
              {PROJECT_STATUS_LABELS[status]}
            </option>
          ))}
        </Select>
      </div>

      {loading ? (
        <div className="flex justify-center py-20">
          <Spinner className="h-7 w-7" label="Loading projects" />
        </div>
      ) : error ? (
        <div className="rounded-card border border-danger/25 bg-danger/5 p-6 text-sm text-danger">
          {error}
        </div>
      ) : projects.length === 0 ? (
        <EmptyState
          icon={FolderKanban}
          title={filters.search || filters.status ? "No matching projects" : "No projects yet"}
          description={
            filters.search || filters.status
              ? "Try adjusting the search or status filter."
              : "Create a project to start organizing tasks and inviting teammates."
          }
          action={
            !filters.search && !filters.status ? (
              <Button onClick={() => setCreateOpen(true)}>
                <Plus className="h-4 w-4" aria-hidden="true" />
                Create project
              </Button>
            ) : null
          }
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {projects.map((project) => {
            const progress = percentage(project.completedTaskCount, project.taskCount);
            return (
              <Link
                key={project.id}
                to={`/projects/${project.id}`}
                className="group flex flex-col rounded-card border border-line bg-surface p-5 shadow-card transition-all hover:-translate-y-0.5 hover:border-brand/30 hover:shadow-float"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="truncate text-base font-semibold text-ink group-hover:text-brand">
                      {project.name}
                    </h2>
                    <p className="mt-1 line-clamp-2 min-h-[2.5rem] text-sm text-muted">
                      {project.description || "No description provided."}
                    </p>
                  </div>
                  <ProjectStatusBadge status={project.status} />
                </div>

                <div className="mt-4">
                  <div className="flex items-center justify-between text-xs font-medium text-muted">
                    <span>
                      {project.completedTaskCount} of {project.taskCount} tasks complete
                    </span>
                    <span>{progress}%</span>
                  </div>
                  <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-line">
                    <div
                      className="h-full rounded-full bg-brand"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>

                <div className="mt-auto flex items-center justify-between pt-5 text-xs text-muted">
                  <span className="inline-flex items-center gap-1.5">
                    <Users className="h-3.5 w-3.5" aria-hidden="true" />
                    {project.memberCount} {project.memberCount === 1 ? "member" : "members"}
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    <ListTodo className="h-3.5 w-3.5" aria-hidden="true" />
                    {project.myRole === "OWNER" ? "Owner" : "Member"}
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between border-t border-line pt-4 text-sm text-muted">
          <span>
            Page {data.page + 1} of {data.totalPages} · {data.totalElements} projects
          </span>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              size="sm"
              disabled={data.first}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
            >
              Previous
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((current) => current + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      ) : null}

      <Modal
        open={createOpen}
        onClose={() => (submitting ? null : setCreateOpen(false))}
        title="Create a project"
        description="Projects group tasks, discussions, and members."
      >
        {modalError ? (
          <div className="mb-4 rounded-lg border border-danger/25 bg-danger/5 px-3 py-2 text-sm text-danger">
            {modalError}
          </div>
        ) : null}
        <ProjectForm
          onSubmit={handleCreate}
          onCancel={() => setCreateOpen(false)}
          submitting={submitting}
          serverErrors={serverErrors}
        />
      </Modal>
    </div>
  );
}
