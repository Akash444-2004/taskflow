import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { UserPlus } from "lucide-react";

import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { apiErrorMessage, fieldErrors } from "../../services/api";
import AuthShell from "./AuthShell";

const INITIAL_FORM = {
  fullName: "",
  email: "",
  phoneNumber: "",
  password: "",
  confirmPassword: ""
};

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState(INITIAL_FORM);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function update(field) {
    return (event) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
    };
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (form.fullName.trim().length < 2) {
      nextErrors.fullName = "Full name must be at least 2 characters.";
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      nextErrors.email = "Enter a valid email address.";
    }
    if (form.password.length < 8) {
      nextErrors.password = "Password must be at least 8 characters.";
    } else if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
      nextErrors.password = "Password must include a letter and a number.";
    }
    if (form.confirmPassword !== form.password) {
      nextErrors.confirmPassword = "Passwords do not match.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      return;
    }

    setSubmitting(true);
    setServerError("");
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        phoneNumber: form.phoneNumber.trim() || null
      });
      toast.success("Your workspace is ready.");
      navigate("/dashboard", { replace: true });
    } catch (error) {
      setErrors(fieldErrors(error));
      setServerError(apiErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Start organizing projects and tasks in a few seconds."
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-semibold text-brand hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form className="space-y-4" onSubmit={handleSubmit} noValidate>
        {serverError ? (
          <div
            role="alert"
            className="rounded-lg border border-danger/25 bg-danger/5 px-3 py-2 text-sm text-danger"
          >
            {serverError}
          </div>
        ) : null}

        <Input
          label="Full name"
          autoComplete="name"
          placeholder="Avery Stone"
          value={form.fullName}
          onChange={update("fullName")}
          error={errors.fullName}
        />

        <Input
          label="Email address"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={form.email}
          onChange={update("email")}
          error={errors.email}
        />

        <Input
          label="Phone number"
          type="tel"
          autoComplete="tel"
          placeholder="Optional"
          value={form.phoneNumber}
          onChange={update("phoneNumber")}
          error={errors.phoneNumber}
          hint="Optional. Include 7 to 30 characters."
        />

        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          placeholder="At least 8 characters"
          value={form.password}
          onChange={update("password")}
          error={errors.password}
          hint="Use at least one letter and one number."
        />

        <Input
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          placeholder="Repeat your password"
          value={form.confirmPassword}
          onChange={update("confirmPassword")}
          error={errors.confirmPassword}
        />

        <Button type="submit" className="w-full" loading={submitting}>
          <UserPlus className="h-4 w-4" aria-hidden="true" />
          Create account
        </Button>
      </form>
    </AuthShell>
  );
}
