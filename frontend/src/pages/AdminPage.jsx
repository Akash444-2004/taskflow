import { useCallback, useEffect, useState } from "react";
import {
  CheckCircle2,
  FolderKanban,
  ListTodo,
  Search,
  ShieldCheck,
  UserCheck,
  Users,
  UserX
} from "lucide-react";

import Avatar from "../components/ui/Avatar";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";
import PageHeader from "../components/ui/PageHeader";
import Spinner from "../components/ui/Spinner";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, apiErrorMessage } from "../services/api";
import { formatDate } from "../utils/format";

function StatCard({ label, value, icon: Icon, tone }) {
  const tones = {
    brand: "bg-brand-soft/60 text-brand",
    info: "bg-info/10 text-info",
    success: "bg-success/10 text-success",
    warning: "bg-warning/10 text-warning"
  };

  return (
    <div className="rounded-card border border-line bg-surface p-5 shadow-card">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">{label}</p>
          <p className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-ink">{value}</p>
        </div>
        <span className={`flex h-10 w-10 items-center justify-center rounded-xl ${tones[tone]}`}>
          <Icon className="h-5 w-5" aria-hidden="true" />
        </span>
      </div>
    </div>
  );
}

export default function AdminPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [users, setUsers] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  const loadStats = useCallback(async () => {
    try {
      const { data } = await api.get("/admin/stats");
      setStats(data);
    } catch (requestError) {
      toast.error(apiErrorMessage(requestError));
    } finally {
      setStatsLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const params = { page, size: 15, sort: "createdAt,desc" };
        if (search.trim()) params.search = search.trim();
        const response = await api.get("/admin/users", { params });
        if (active) setUsers(response.data);
      } catch (requestError) {
        if (active) setError(apiErrorMessage(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }

    const timeout = window.setTimeout(load, search ? 300 : 0);
    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [page, search]);

  async function toggleEnabled(target) {
    setBusyId(target.id);
    try {
      const { data } = await api.patch(`/admin/users/${target.id}/enabled`, {
        enabled: !target.enabled
      });
      setUsers((current) =>
        current
          ? {
              ...current,
              content: current.content.map((item) => (item.id === data.id ? data : item))
            }
          : current
      );
      toast.success(data.enabled ? "Account enabled." : "Account disabled.");
      loadStats();
    } catch (requestError) {
      toast.error(apiErrorMessage(requestError));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Administration"
        description="Platform health and account access management."
        actions={
          <span className="inline-flex items-center gap-2 rounded-lg border border-line bg-surface px-3 py-2 text-xs font-semibold text-muted">
            <ShieldCheck className="h-4 w-4 text-brand" aria-hidden="true" />
            Administrator access
          </span>
        }
      />

      {statsLoading ? (
        <div className="flex justify-center py-10">
          <Spinner className="h-7 w-7" label="Loading platform statistics" />
        </div>
      ) : stats ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <StatCard label="Total users" value={stats.totalUsers} icon={Users} tone="brand" />
          <StatCard label="Enabled users" value={stats.enabledUsers} icon={UserCheck} tone="success" />
          <StatCard label="Projects" value={stats.totalProjects} icon={FolderKanban} tone="info" />
          <StatCard label="Tasks" value={stats.totalTasks} icon={ListTodo} tone="warning" />
          <StatCard
            label="Completed tasks"
            value={stats.completedTasks}
            icon={CheckCircle2}
            tone="success"
          />
        </div>
      ) : null}

      <section className="rounded-card border border-line bg-surface shadow-card">
        <div className="flex flex-col gap-3 border-b border-line px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold text-ink">Accounts</h2>
            <p className="text-xs text-muted">
              Disabling an account immediately ends its refresh sessions.
            </p>
          </div>
          <div className="relative sm:w-72">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
              aria-hidden="true"
            />
            <Input
              aria-label="Search accounts"
              placeholder="Search by name or email"
              className="pl-9"
              value={search}
              onChange={(event) => {
                setPage(0);
                setSearch(event.target.value);
              }}
            />
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-16">
            <Spinner className="h-7 w-7" label="Loading accounts" />
          </div>
        ) : error ? (
          <div className="m-5 rounded-lg border border-danger/25 bg-danger/5 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        ) : !users || users.content.length === 0 ? (
          <div className="px-5 py-16 text-center">
            <p className="text-sm font-semibold text-ink">No accounts found</p>
            <p className="mt-1 text-xs text-muted">
              {search ? "Try a different name or email." : "No users are registered yet."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-line text-sm">
              <thead className="bg-surface-muted/60 text-left text-xs uppercase tracking-wide text-muted">
                <tr>
                  <th className="px-5 py-3 font-semibold">User</th>
                  <th className="px-5 py-3 font-semibold">Role</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Joined</th>
                  <th className="px-5 py-3 text-right font-semibold">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {users.content.map((account) => {
                  const isSelf = account.id === user?.id;
                  return (
                    <tr key={account.id} className="hover:bg-surface-muted/40">
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <Avatar name={account.fullName} size="sm" />
                          <div className="min-w-0">
                            <p className="truncate font-semibold text-ink">
                              {account.fullName}
                              {isSelf ? (
                                <span className="ml-2 text-xs font-normal text-muted">(you)</span>
                              ) : null}
                            </p>
                            <p className="truncate text-xs text-muted">{account.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3">
                        <Badge tone={account.role === "ADMIN" ? "brand" : "neutral"}>
                          {account.role === "ADMIN" ? "Admin" : "User"}
                        </Badge>
                      </td>
                      <td className="px-5 py-3">
                        <Badge tone={account.enabled ? "success" : "danger"}>
                          {account.enabled ? "Enabled" : "Disabled"}
                        </Badge>
                      </td>
                      <td className="px-5 py-3 text-xs text-muted">
                        {formatDate(account.createdAt)}
                      </td>
                      <td className="px-5 py-3 text-right">
                        {account.enabled ? (
                          <Button
                            variant="secondary"
                            size="sm"
                            disabled={isSelf}
                            loading={busyId === account.id}
                            onClick={() => toggleEnabled(account)}
                            title={isSelf ? "You cannot disable your own account" : undefined}
                          >
                            <UserX className="h-3.5 w-3.5" aria-hidden="true" />
                            Disable
                          </Button>
                        ) : (
                          <Button
                            variant="secondary"
                            size="sm"
                            loading={busyId === account.id}
                            onClick={() => toggleEnabled(account)}
                          >
                            <UserCheck className="h-3.5 w-3.5" aria-hidden="true" />
                            Enable
                          </Button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {users && users.totalPages > 1 ? (
          <div className="flex items-center justify-between border-t border-line px-5 py-3 text-xs text-muted">
            <span>
              Page {users.page + 1} of {users.totalPages} · {users.totalElements} accounts
            </span>
            <div className="flex gap-2">
              <Button
                variant="secondary"
                size="sm"
                disabled={users.first}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Previous
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={users.last}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        ) : null}
      </section>
    </div>
  );
}
