import { useState } from "react";

import Button from "../ui/Button";
import Input from "../ui/Input";
import Select from "../ui/Select";
import Textarea from "../ui/Textarea";
import {
  TASK_PRIORITIES,
  TASK_PRIORITY_LABELS,
  TASK_STATUSES,
  TASK_STATUS_LABELS,
  toDateInput
} from "../../utils/format";

export default function TaskForm({
  initialValues,
  members = [],
  onSubmit,
  onCancel,
  submitting = false,
  serverErrors = {},
  submitLabel = "Create task"
}) {
  const [values, setValues] = useState({
    title: initialValues?.title || "",
    description: initialValues?.description || "",
    assignedToId: initialValues?.assignedTo?.id ? String(initialValues.assignedTo.id) : "",
    priority: initialValues?.priority || "MEDIUM",
    status: initialValues?.status || "TODO",
    startDate: toDateInput(initialValues?.startDate),
    dueDate: toDateInput(initialValues?.dueDate)
  });
  const [errors, setErrors] = useState({});

  function update(field) {
    return (event) => {
      setValues((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
    };
  }

  function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = {};

    if (values.title.trim().length < 2) {
      nextErrors.title = "Task title must be at least 2 characters.";
    }
    if (values.startDate && values.dueDate && values.dueDate < values.startDate) {
      nextErrors.dueDate = "Due date cannot be before the start date.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    onSubmit({
      title: values.title.trim(),
      description: values.description.trim() || null,
      assignedToId: values.assignedToId ? Number(values.assignedToId) : null,
      priority: values.priority,
      status: values.status,
      startDate: values.startDate || null,
      dueDate: values.dueDate || null
    });
  }

  const fieldError = (field) => errors[field] || serverErrors[field];

  return (
    <form className="space-y-4" onSubmit={handleSubmit} noValidate>
      <Input
        label="Task title"
        placeholder="Draft the launch checklist"
        value={values.title}
        onChange={update("title")}
        error={fieldError("title")}
        autoFocus
      />

      <Textarea
        label="Description"
        placeholder="Add context, acceptance criteria, or links."
        value={values.description}
        onChange={update("description")}
        error={fieldError("description")}
        rows={3}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Select
          label="Assignee"
          value={values.assignedToId}
          onChange={update("assignedToId")}
          error={fieldError("assignedToId")}
        >
          <option value="">Unassigned</option>
          {members.map((member) => (
            <option key={member.user.id} value={member.user.id}>
              {member.user.fullName}
            </option>
          ))}
        </Select>

        <Select label="Priority" value={values.priority} onChange={update("priority")}>
          {TASK_PRIORITIES.map((priority) => (
            <option key={priority} value={priority}>
              {TASK_PRIORITY_LABELS[priority]}
            </option>
          ))}
        </Select>

        <Select label="Status" value={values.status} onChange={update("status")}>
          {TASK_STATUSES.map((status) => (
            <option key={status} value={status}>
              {TASK_STATUS_LABELS[status]}
            </option>
          ))}
        </Select>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Start date"
            type="date"
            value={values.startDate}
            onChange={update("startDate")}
            error={fieldError("startDate")}
          />
          <Input
            label="Due date"
            type="date"
            value={values.dueDate}
            onChange={update("dueDate")}
            error={fieldError("dueDate")}
          />
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-2">
        {onCancel ? (
          <Button variant="secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
        ) : null}
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
