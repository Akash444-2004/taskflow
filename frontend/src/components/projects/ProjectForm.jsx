import { useState } from "react";

import Button from "../ui/Button";
import Input from "../ui/Input";
import Select from "../ui/Select";
import Textarea from "../ui/Textarea";
import { PROJECT_STATUSES, PROJECT_STATUS_LABELS, toDateInput } from "../../utils/format";

export default function ProjectForm({
  initialValues,
  onSubmit,
  onCancel,
  submitting = false,
  serverErrors = {},
  submitLabel = "Create project"
}) {
  const [values, setValues] = useState({
    name: initialValues?.name || "",
    description: initialValues?.description || "",
    status: initialValues?.status || "ACTIVE",
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

    if (values.name.trim().length < 2) {
      nextErrors.name = "Project name must be at least 2 characters.";
    }
    if (values.startDate && values.dueDate && values.dueDate < values.startDate) {
      nextErrors.dueDate = "Due date cannot be before the start date.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    onSubmit({
      name: values.name.trim(),
      description: values.description.trim() || null,
      status: values.status,
      startDate: values.startDate || null,
      dueDate: values.dueDate || null
    });
  }

  const fieldError = (field) => errors[field] || serverErrors[field];

  return (
    <form className="space-y-4" onSubmit={handleSubmit} noValidate>
      <Input
        label="Project name"
        placeholder="Website redesign"
        value={values.name}
        onChange={update("name")}
        error={fieldError("name")}
        autoFocus
      />

      <Textarea
        label="Description"
        placeholder="What is this project about?"
        value={values.description}
        onChange={update("description")}
        error={fieldError("description")}
        rows={3}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Select label="Status" value={values.status} onChange={update("status")}>
          {PROJECT_STATUSES.map((status) => (
            <option key={status} value={status}>
              {PROJECT_STATUS_LABELS[status]}
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
