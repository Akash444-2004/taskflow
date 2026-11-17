import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import {
  ProjectStatusBadge,
  TaskPriorityBadge,
  TaskStatusBadge
} from "./Badge";

describe("badges", () => {
  it("renders human readable task statuses", () => {
    render(<TaskStatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText("In progress")).toBeInTheDocument();
  });

  it("renders human readable priorities", () => {
    render(<TaskPriorityBadge priority="HIGH" />);
    expect(screen.getByText("High")).toBeInTheDocument();
  });

  it("renders human readable project statuses", () => {
    render(<ProjectStatusBadge status="ON_HOLD" />);
    expect(screen.getByText("On hold")).toBeInTheDocument();
  });
});
