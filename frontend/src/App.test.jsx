import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import App from "./App";

describe("App", () => {
  it("renders the TaskFlow foundation status", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: /clean foundation/i })).toBeInTheDocument();
    expect(screen.getByText("TaskFlow")).toBeInTheDocument();
    expect(screen.getByText(/foundation verified/i)).toBeInTheDocument();
    expect(screen.getByText(/environment and database checks passed/i)).toBeInTheDocument();
  });
});
