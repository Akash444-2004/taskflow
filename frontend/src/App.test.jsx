import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import { ToastProvider } from "./context/ToastContext";

vi.mock("./services/api", () => ({
  api: {
    get: vi.fn().mockRejectedValue(new Error("No active session")),
    post: vi.fn().mockRejectedValue(new Error("No active session"))
  },
  csrfConfig: vi.fn(async (config) => config || {}),
  setAccessToken: vi.fn(),
  setRefreshHandler: vi.fn(),
  setUnauthorizedHandler: vi.fn(),
  apiErrorMessage: () => "Something went wrong.",
  fieldErrors: () => ({})
}));

function renderApp(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

describe("App routing", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("renders the login page for guests", async () => {
    renderApp("/login");

    expect(
      await screen.findByRole("heading", { name: /welcome back/i })
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
  });

  it("redirects protected routes to login when unauthenticated", async () => {
    renderApp("/dashboard");

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /welcome back/i })
      ).toBeInTheDocument();
    });
  });

  it("shows the not found page for unknown routes", async () => {
    renderApp("/does-not-exist");

    expect(
      await screen.findByRole("heading", { name: /this page does not exist/i })
    ).toBeInTheDocument();
  });
});
