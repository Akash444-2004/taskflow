import { useEffect, useState } from "react";
import { ArrowLeft, ArrowRight, Check, MessageSquare, Pencil, RotateCcw, Send, Trash2 } from "lucide-react";

import Avatar from "../ui/Avatar";
import Badge, { TaskPriorityBadge, TaskStatusBadge } from "../ui/Badge";
import Button from "../ui/Button";
import Modal from "../ui/Modal";
import Spinner from "../ui/Spinner";
import Textarea from "../ui/Textarea";
import { useToast } from "../../context/ToastContext";
import { api, apiErrorMessage, fieldErrors } from "../../services/api";
import { TASK_STATUS_LABELS, formatDate } from "../../utils/format";
import TaskForm from "./TaskForm";

const NEXT_ACTIONS = {
  TODO: [{ status: "IN_PROGRESS", label: "Start task", icon: ArrowRight }],
  IN_PROGRESS: [
    { status: "COMPLETED", label: "Mark complete", icon: Check },
    { status: "TODO", label: "Move to To do", icon: ArrowLeft }
  ],
  COMPLETED: [{ status: "IN_PROGRESS", label: "Reopen task", icon: RotateCcw }]
};

function MetaRow({ label, children }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-line py-2.5 last:border-0">
      <span className="text-xs font-medium text-muted">{label}</span>
      <span className="text-right text-xs font-semibold text-ink">{children}</span>
    </div>
  );
}

export default function TaskDetailModal({
  task,
  members,
  currentUserId,
  canDelete,
  canModerate,
  onClose,
  onChanged,
  onDeleted
}) {
  const toast = useToast();
  const [detail, setDetail] = useState(task);
  const [comments, setComments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(true);
  const [comment, setComment] = useState("");
  const [posting, setPosting] = useState(false);
  const [statusSaving, setStatusSaving] = useState(false);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [serverErrors, setServerErrors] = useState({});

  useEffect(() => {
    let active = true;
    setLoadingComments(true);
    api
      .get(`/tasks/${task.id}/comments`)
      .then((response) => {
        if (active) setComments(response.data);
      })
      .catch((error) => {
        if (active) toast.error(apiErrorMessage(error));
      })
      .finally(() => {
        if (active) setLoadingComments(false);
      });

    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [task.id]);

  async function changeStatus(status) {
    setStatusSaving(true);
    try {
      const { data } = await api.patch(`/tasks/${detail.id}/status`, { status });
      setDetail(data);
      onChanged(data);
      toast.success(`Moved to ${TASK_STATUS_LABELS[status]}.`);
    } catch (error) {
      toast.error(apiErrorMessage(error));
    } finally {
      setStatusSaving(false);
    }
  }

  async function saveTask(values) {
    setSaving(true);
    setServerErrors({});
    try {
      const { data } = await api.put(`/tasks/${detail.id}`, values);
      setDetail(data);
      onChanged(data);
      setEditing(false);
      toast.success("Task updated.");
    } catch (error) {
      setServerErrors(fieldErrors(error));
      toast.error(apiErrorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  async function addComment(event) {
    event.preventDefault();
    const content = comment.trim();
    if (!content) return;

    setPosting(true);
    try {
      const { data } = await api.post(`/tasks/${detail.id}/comments`, { content });
      setComments((current) => [...current, data]);
      setComment("");
    } catch (error) {
      toast.error(apiErrorMessage(error));
    } finally {
      setPosting(false);
    }
  }

  async function deleteComment(commentId) {
    if (!window.confirm("Delete this comment?")) return;
    try {
      await api.delete(`/comments/${commentId}`);
      setComments((current) => current.filter((item) => item.id !== commentId));
    } catch (error) {
      toast.error(apiErrorMessage(error));
    }
  }

  async function deleteTask() {
    if (!window.confirm("Delete this task and all of its comments?")) return;
    try {
      await api.delete(`/tasks/${detail.id}`);
      toast.success("Task deleted.");
      onDeleted(detail);
    } catch (error) {
      toast.error(apiErrorMessage(error));
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={editing ? "Edit task" : detail.title}
      description={editing ? "Update the task fields below." : detail.projectName}
      size="lg"
    >
      {editing ? (
        <TaskForm
          initialValues={detail}
          members={members}
          onSubmit={saveTask}
          onCancel={() => setEditing(false)}
          submitting={saving}
          serverErrors={serverErrors}
          submitLabel="Save changes"
        />
      ) : (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-5 lg:col-span-2">
            <div className="flex flex-wrap items-center gap-2">
              <TaskStatusBadge status={detail.status} />
              <TaskPriorityBadge priority={detail.priority} />
              {detail.dueDate ? (
                <Badge tone="neutral">Due {formatDate(detail.dueDate)}</Badge>
              ) : null}
            </div>

            <div>
              <h3 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">
                Description
              </h3>
              <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-ink">
                {detail.description || "No description provided."}
              </p>
            </div>

            <div className="rounded-xl border border-line bg-surface-muted/30 p-4">
              <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
                <MessageSquare className="h-4 w-4 text-brand" aria-hidden="true" />
                Comments ({comments.length})
              </h3>

              {loadingComments ? (
                <div className="flex justify-center py-6">
                  <Spinner label="Loading comments" />
                </div>
              ) : comments.length === 0 ? (
                <p className="mt-3 text-sm text-muted">
                  No comments yet. Start the conversation below.
                </p>
              ) : (
                <ul className="mt-4 space-y-4">
                  {comments.map((item) => {
                    const canRemove =
                      item.author.id === currentUserId || canModerate;
                    return (
                      <li key={item.id} className="flex gap-3">
                        <Avatar name={item.author.fullName} size="sm" />
                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="text-sm font-semibold text-ink">
                              {item.author.fullName}
                            </span>
                            <span className="text-[11px] text-muted">
                              {formatDate(item.createdAt)}
                            </span>
                            {canRemove ? (
                              <button
                                type="button"
                                onClick={() => deleteComment(item.id)}
                                className="ml-auto rounded p-1 text-muted hover:bg-danger/10 hover:text-danger"
                                aria-label="Delete comment"
                              >
                                <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                              </button>
                            ) : null}
                          </div>
                          <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-ink/90">
                            {item.content}
                          </p>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}

              <form className="mt-4 space-y-2" onSubmit={addComment}>
                <Textarea
                  aria-label="Write a comment"
                  placeholder="Write a comment..."
                  rows={3}
                  value={comment}
                  onChange={(event) => setComment(event.target.value)}
                />
                <div className="flex justify-end">
                  <Button type="submit" size="sm" loading={posting} disabled={!comment.trim()}>
                    <Send className="h-3.5 w-3.5" aria-hidden="true" />
                    Post comment
                  </Button>
                </div>
              </form>
            </div>
          </div>

          <aside className="space-y-5">
            <div className="rounded-xl border border-line bg-surface p-4">
              <h3 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">
                Details
              </h3>
              <div className="mt-2">
                <MetaRow label="Assignee">
                  {detail.assignedTo ? (
                    <span className="inline-flex items-center gap-2">
                      <Avatar name={detail.assignedTo.fullName} size="xs" />
                      {detail.assignedTo.fullName}
                    </span>
                  ) : (
                    "Unassigned"
                  )}
                </MetaRow>
                <MetaRow label="Created by">{detail.createdBy.fullName}</MetaRow>
                <MetaRow label="Start">{formatDate(detail.startDate)}</MetaRow>
                <MetaRow label="Due">{formatDate(detail.dueDate)}</MetaRow>
                <MetaRow label="Updated">{formatDate(detail.updatedAt)}</MetaRow>
              </div>
            </div>

            <div className="rounded-xl border border-line bg-surface p-4">
              <h3 className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">
                Move task
              </h3>
              <div className="mt-3 space-y-2">
                {(NEXT_ACTIONS[detail.status] || []).map(({ status, label, icon: Icon }) => (
                  <Button
                    key={status}
                    variant={status === "COMPLETED" ? "primary" : "secondary"}
                    size="sm"
                    className="w-full justify-start"
                    loading={statusSaving}
                    onClick={() => changeStatus(status)}
                  >
                    <Icon className="h-3.5 w-3.5" aria-hidden="true" />
                    {label}
                  </Button>
                ))}
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <Button variant="secondary" size="sm" onClick={() => setEditing(true)}>
                <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                Edit task
              </Button>
              {canDelete ? (
                <Button variant="danger" size="sm" onClick={deleteTask}>
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  Delete task
                </Button>
              ) : null}
            </div>
          </aside>
        </div>
      )}
    </Modal>
  );
}
