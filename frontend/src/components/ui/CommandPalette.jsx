import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import {
  FolderKanban,
  LayoutDashboard,
  ListTodo,
  Search,
  ShieldCheck,
  UserRound
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";
import { api } from "../../services/api";
import { cn } from "../../utils/format";

export default function CommandPalette({ open, onClose }) {
  const navigate = useNavigate();
  const { isAdmin } = useAuth();
  const [term, setTerm] = useState("");
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  // Reset state each time the palette opens.
  useEffect(() => {
    if (!open) return undefined;

    setTerm("");
    setProjects([]);
    setTasks([]);
    setActiveIndex(0);

    const timer = window.setTimeout(() => inputRef.current?.focus(), 10);
    return () => window.clearTimeout(timer);
  }, [open]);

  // Projects are searched server-side (debounced).
  useEffect(() => {
    if (!open) return undefined;

    let active = true;
    const timeout = window.setTimeout(async () => {
      try {
        const params = { size: 8, sort: "updatedAt,desc" };
        if (term.trim()) params.search = term.trim();
        const { data } = await api.get("/projects", { params });
        if (active) setProjects(data.content || []);
      } catch {
        // The palette degrades gracefully; navigation actions still work.
      }
    }, 250);

    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [open, term]);

  // Tasks come from the assigned-to-me list and are filtered client-side.
  useEffect(() => {
    if (!open) return undefined;

    let active = true;
    api
      .get("/tasks/assigned-to-me", { params: { size: 100, sort: "dueDate,asc" } })
      .then(({ data }) => {
        if (active) setTasks(data.content || []);
      })
      .catch(() => {
        // Non-critical.
      });

    return () => {
      active = false;
    };
  }, [open]);

  const actions = useMemo(() => {
    const items = [
      {
        id: "action-dashboard",
        label: "Dashboard",
        hint: "Overview and deadlines",
        icon: LayoutDashboard,
        run: () => navigate("/dashboard")
      },
      {
        id: "action-projects",
        label: "Projects",
        hint: "All projects",
        icon: FolderKanban,
        run: () => navigate("/projects")
      },
      {
        id: "action-tasks",
        label: "My tasks",
        hint: "Everything assigned to you",
        icon: ListTodo,
        run: () => navigate("/tasks")
      },
      {
        id: "action-profile",
        label: "Profile",
        hint: "Account settings",
        icon: UserRound,
        run: () => navigate("/profile")
      }
    ];

    if (isAdmin) {
      items.push({
        id: "action-admin",
        label: "Administration",
        hint: "Platform accounts",
        icon: ShieldCheck,
        run: () => navigate("/admin")
      });
    }

    return items;
  }, [isAdmin, navigate]);

  const searchTerm = term.trim().toLowerCase();

  const filteredProjects = useMemo(() => {
    const matches = searchTerm
      ? projects.filter((project) => project.name.toLowerCase().includes(searchTerm))
      : projects;
    return matches.slice(0, 5);
  }, [projects, searchTerm]);

  const filteredTasks = useMemo(() => {
    const matches = searchTerm
      ? tasks.filter(
          (task) =>
            task.title.toLowerCase().includes(searchTerm) ||
            (task.projectName || "").toLowerCase().includes(searchTerm)
        )
      : tasks;
    return matches.slice(0, 5);
  }, [tasks, searchTerm]);

  const flatItems = useMemo(
    () => [
      ...actions.map((action) => ({ ...action, group: "Actions" })),
      ...filteredProjects.map((project) => ({
        id: `project-${project.id}`,
        group: "Projects",
        label: project.name,
        hint: project.myRole === "OWNER" ? "Owned by you" : "Project",
        icon: FolderKanban,
        run: () => navigate(`/projects/${project.id}`)
      })),
      ...filteredTasks.map((task) => ({
        id: `task-${task.id}`,
        group: "Tasks",
        label: task.title,
        hint: task.projectName || "Task",
        icon: ListTodo,
        run: () => navigate(`/projects/${task.projectId}?task=${task.id}`)
      }))
    ],
    [actions, filteredProjects, filteredTasks, navigate]
  );

  useEffect(() => {
    setActiveIndex(0);
  }, [term, flatItems.length]);

  useEffect(() => {
    if (!open) return undefined;

    function onKeyDown(event) {
      if (event.key === "Escape") {
        onClose?.();
        return;
      }
      if (event.key === "ArrowDown") {
        event.preventDefault();
        setActiveIndex((current) => (flatItems.length ? (current + 1) % flatItems.length : 0));
      }
      if (event.key === "ArrowUp") {
        event.preventDefault();
        setActiveIndex((current) =>
          flatItems.length ? (current - 1 + flatItems.length) % flatItems.length : 0
        );
      }
      if (event.key === "Enter") {
        event.preventDefault();
        const item = flatItems[activeIndex];
        if (item) {
          onClose?.();
          item.run();
        }
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, flatItems, activeIndex, onClose]);

  // Keep the highlighted row in view.
  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    const active = list.querySelector('[data-active="true"]');
    active?.scrollIntoView({ block: "nearest" });
  }, [activeIndex]);

  if (!open) return null;

  let lastGroup = null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[12vh] sm:p-6 sm:pt-[12vh]">
      <div
        className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Quick search"
        className="relative z-10 w-full max-w-xl overflow-hidden rounded-card border border-line bg-surface shadow-float"
      >
        <div className="flex items-center gap-3 border-b border-line px-4 py-3">
          <Search className="h-4 w-4 shrink-0 text-muted" aria-hidden="true" />
          <input
            ref={inputRef}
            type="text"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            placeholder="Search projects, tasks, or jump to a page…"
            className="w-full bg-transparent text-sm text-ink outline-none placeholder:text-muted"
            aria-label="Search"
            autoComplete="off"
            spellCheck="false"
          />
          <kbd className="hidden rounded border border-line bg-surface-muted px-1.5 py-0.5 text-[10px] font-semibold text-muted sm:block">
            Esc
          </kbd>
        </div>

        <div ref={listRef} className="max-h-[50vh] overflow-y-auto p-2">
          {flatItems.length === 0 ? (
            <p className="px-3 py-8 text-center text-sm text-muted">
              Nothing matches "{term}".
            </p>
          ) : (
            flatItems.map((item, index) => {
              const showGroup = item.group !== lastGroup;
              lastGroup = item.group;
              const Icon = item.icon;
              return (
                <div key={item.id}>
                  {showGroup ? (
                    <p className="px-3 pb-1 pt-3 text-[10px] font-semibold uppercase tracking-[0.12em] text-muted">
                      {item.group}
                    </p>
                  ) : null}
                  <button
                    type="button"
                    data-active={index === activeIndex}
                    onMouseEnter={() => setActiveIndex(index)}
                    onClick={() => {
                      onClose?.();
                      item.run();
                    }}
                    className={cn(
                      "flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm transition-colors",
                      index === activeIndex
                        ? "bg-brand-soft/60 text-brand"
                        : "text-ink hover:bg-surface-muted"
                    )}
                  >
                    <Icon className="h-4 w-4 shrink-0 text-muted" aria-hidden="true" />
                    <span className="min-w-0 flex-1 truncate font-medium">{item.label}</span>
                    <span className="shrink-0 text-xs text-muted">{item.hint}</span>
                  </button>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
