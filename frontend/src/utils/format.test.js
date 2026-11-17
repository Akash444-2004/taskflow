import { describe, expect, it } from "vitest";

import {
  formatDate,
  initials,
  percentage,
  toDateInput
} from "./format";

describe("format utilities", () => {
  it("formats dates and handles missing or invalid values", () => {
    const localNoon = new Date(2026, 0, 5, 12, 0, 0);

    expect(formatDate(localNoon.toISOString())).toBe("Jan 5, 2026");
    expect(formatDate(null)).toBe("—");
    expect(formatDate("not-a-date")).toBe("—");
  });

  it("normalises dates for date inputs", () => {
    expect(toDateInput("2026-03-09")).toBe("2026-03-09");
    expect(toDateInput(new Date(2026, 2, 9, 12).toISOString())).toBe("2026-03-09");
    expect(toDateInput("")).toBe("");
    expect(toDateInput("invalid")).toBe("");
  });

  it("derives initials from names", () => {
    expect(initials("Avery Stone")).toBe("AS");
    expect(initials("madonna")).toBe("M");
    expect(initials("")).toBe("?");
    expect(initials(null)).toBe("?");
  });

  it("computes rounded percentages and guards division by zero", () => {
    expect(percentage(3, 4)).toBe(75);
    expect(percentage(1, 3)).toBe(33);
    expect(percentage(2, 0)).toBe(0);
  });
});
