import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { KeyRound, Moon, ShieldCheck, Sun, UserRound } from "lucide-react";

import Avatar from "../components/ui/Avatar";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";
import PageHeader from "../components/ui/PageHeader";
import Spinner from "../components/ui/Spinner";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { useToast } from "../context/ToastContext";
import { api, apiErrorMessage, fieldErrors } from "../services/api";
import { formatDate } from "../utils/format";

export default function ProfilePage() {
  const { user, updateUser, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const toast = useToast();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(!user);
  const [profile, setProfile] = useState({
    fullName: user?.fullName || "",
    phoneNumber: user?.phoneNumber || ""
  });
  const [profileErrors, setProfileErrors] = useState({});
  const [profileMessage, setProfileMessage] = useState("");
  const [savingProfile, setSavingProfile] = useState(false);

  const [passwords, setPasswords] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [passwordErrors, setPasswordErrors] = useState({});
  const [passwordMessage, setPasswordMessage] = useState("");
  const [savingPassword, setSavingPassword] = useState(false);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const { data } = await api.get("/users/me");
        if (!active) return;
        setProfile({
          fullName: data.fullName || "",
          phoneNumber: data.phoneNumber || ""
        });
        updateUser(data);
      } catch (requestError) {
        if (active) toast.error(apiErrorMessage(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }

    load();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function saveProfile(event) {
    event.preventDefault();
    const nextErrors = {};

    if (profile.fullName.trim().length < 2) {
      nextErrors.fullName = "Full name must be at least 2 characters.";
    }
    if (profile.phoneNumber.trim()) {
      const phone = profile.phoneNumber.trim();
      if (!/^\+?[0-9().\-\s]{7,30}$/.test(phone)) {
        nextErrors.phoneNumber = "Enter a valid phone number (7 to 30 characters).";
      }
    }

    if (Object.keys(nextErrors).length > 0) {
      setProfileErrors(nextErrors);
      return;
    }

    setSavingProfile(true);
    setProfileErrors({});
    setProfileMessage("");
    try {
      const { data } = await api.put("/users/me", {
        fullName: profile.fullName.trim(),
        phoneNumber: profile.phoneNumber.trim() || null
      });
      updateUser(data);
      setProfile({ fullName: data.fullName, phoneNumber: data.phoneNumber || "" });
      toast.success("Profile updated.");
    } catch (requestError) {
      setProfileErrors(fieldErrors(requestError));
      setProfileMessage(apiErrorMessage(requestError));
    } finally {
      setSavingProfile(false);
    }
  }

  async function changePassword(event) {
    event.preventDefault();
    const nextErrors = {};

    if (!passwords.currentPassword) {
      nextErrors.currentPassword = "Enter your current password.";
    }
    if (passwords.newPassword.length < 8) {
      nextErrors.newPassword = "New password must be at least 8 characters.";
    } else if (!/[A-Za-z]/.test(passwords.newPassword) || !/\d/.test(passwords.newPassword)) {
      nextErrors.newPassword = "Use at least one letter and one number.";
    } else if (passwords.newPassword === passwords.currentPassword) {
      nextErrors.newPassword = "Choose a password different from your current one.";
    }
    if (passwords.confirmPassword !== passwords.newPassword) {
      nextErrors.confirmPassword = "Passwords do not match.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setPasswordErrors(nextErrors);
      return;
    }

    setSavingPassword(true);
    setPasswordErrors({});
    setPasswordMessage("");
    try {
      await api.post("/users/me/password", {
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword
      });
      toast.success("Password changed. Please sign in again.");
      await logout();
      navigate("/login", { replace: true });
    } catch (requestError) {
      setPasswordErrors(fieldErrors(requestError));
      setPasswordMessage(apiErrorMessage(requestError));
    } finally {
      setSavingPassword(false);
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <Spinner className="h-8 w-8" label="Loading profile" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Profile"
        description="Manage your account details and security settings."
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-card border border-line bg-surface p-5 shadow-card lg:col-span-2">
          <div className="flex items-center gap-4 border-b border-line pb-5">
            <Avatar name={user?.fullName} size="lg" />
            <div className="min-w-0">
              <h2 className="truncate text-lg font-semibold text-ink">{user?.fullName}</h2>
              <p className="truncate text-sm text-muted">{user?.email}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <Badge tone={user?.role === "ADMIN" ? "brand" : "neutral"}>
                  {user?.role === "ADMIN" ? "Administrator" : "Member"}
                </Badge>
                <span className="text-[11px] text-muted">
                  Joined {formatDate(user?.createdAt)}
                </span>
              </div>
            </div>
          </div>

          <form className="mt-5 space-y-4" onSubmit={saveProfile} noValidate>
            <h2 className="flex items-center gap-2 text-sm font-semibold text-ink">
              <UserRound className="h-4 w-4 text-brand" aria-hidden="true" />
              Account details
            </h2>

            {profileMessage ? (
              <p className="rounded-lg border border-danger/25 bg-danger/5 px-3 py-2 text-sm text-danger">
                {profileMessage}
              </p>
            ) : null}

            <Input
              label="Full name"
              autoComplete="name"
              value={profile.fullName}
              onChange={(event) => {
                setProfile((current) => ({ ...current, fullName: event.target.value }));
                setProfileErrors((current) => ({ ...current, fullName: undefined }));
              }}
              error={profileErrors.fullName}
            />

            <Input
              label="Email address"
              value={user?.email || ""}
              disabled
              hint="Email addresses cannot be changed in this version."
            />

            <Input
              label="Phone number"
              type="tel"
              autoComplete="tel"
              placeholder="Optional"
              value={profile.phoneNumber}
              onChange={(event) => {
                setProfile((current) => ({ ...current, phoneNumber: event.target.value }));
                setProfileErrors((current) => ({ ...current, phoneNumber: undefined }));
              }}
              error={profileErrors.phoneNumber}
            />

            <div className="flex justify-end">
              <Button type="submit" loading={savingProfile}>
                Save profile
              </Button>
            </div>
          </form>
        </section>

        <aside className="space-y-4">
          <section className="rounded-card border border-line bg-surface p-5 shadow-card">
            <h2 className="text-sm font-semibold text-ink">Appearance</h2>
            <p className="mt-1 text-xs text-muted">
              Your theme preference is stored on this device.
            </p>
            <Button variant="secondary" className="mt-4 w-full" onClick={toggleTheme}>
              {isDark ? (
                <Sun className="h-4 w-4" aria-hidden="true" />
              ) : (
                <Moon className="h-4 w-4" aria-hidden="true" />
              )}
              {isDark ? "Switch to light theme" : "Switch to dark theme"}
            </Button>
          </section>

          <section className="rounded-card border border-line bg-surface p-5 shadow-card">
            <h2 className="flex items-center gap-2 text-sm font-semibold text-ink">
              <ShieldCheck className="h-4 w-4 text-brand" aria-hidden="true" />
              Session security
            </h2>
            <ul className="mt-3 space-y-2 text-xs text-muted">
              <li>Access tokens expire after 15 minutes.</li>
              <li>Refresh sessions rotate on every use.</li>
              <li>Changing your password ends all sessions.</li>
            </ul>
          </section>
        </aside>
      </div>

      <section className="rounded-card border border-line bg-surface p-5 shadow-card lg:max-w-2xl">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-ink">
          <KeyRound className="h-4 w-4 text-brand" aria-hidden="true" />
          Change password
        </h2>
        <p className="mt-1 text-xs text-muted">
          For your security, you will be signed out of every device after changing your password.
        </p>

        <form className="mt-5 space-y-4" onSubmit={changePassword} noValidate>
          {passwordMessage ? (
            <p className="rounded-lg border border-danger/25 bg-danger/5 px-3 py-2 text-sm text-danger">
              {passwordMessage}
            </p>
          ) : null}

          <Input
            label="Current password"
            type="password"
            autoComplete="current-password"
            value={passwords.currentPassword}
            onChange={(event) => {
              setPasswords((current) => ({ ...current, currentPassword: event.target.value }));
              setPasswordErrors((current) => ({ ...current, currentPassword: undefined }));
            }}
            error={passwordErrors.currentPassword}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="New password"
              type="password"
              autoComplete="new-password"
              value={passwords.newPassword}
              onChange={(event) => {
                setPasswords((current) => ({ ...current, newPassword: event.target.value }));
                setPasswordErrors((current) => ({ ...current, newPassword: undefined }));
              }}
              error={passwordErrors.newPassword}
              hint="At least 8 characters with a letter and a number."
            />
            <Input
              label="Confirm new password"
              type="password"
              autoComplete="new-password"
              value={passwords.confirmPassword}
              onChange={(event) => {
                setPasswords((current) => ({ ...current, confirmPassword: event.target.value }));
                setPasswordErrors((current) => ({ ...current, confirmPassword: undefined }));
              }}
              error={passwordErrors.confirmPassword}
            />
          </div>

          <div className="flex justify-end">
            <Button type="submit" variant="secondary" loading={savingPassword}>
              Change password
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
}
