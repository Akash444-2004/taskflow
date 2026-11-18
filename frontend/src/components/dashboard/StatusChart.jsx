import { useState } from "react";
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

function StatusTooltip({ active, payload }) {
  if (!active || !payload || payload.length === 0) return null;
  const entry = payload[0].payload;
  return (
    <div className="rounded-lg border border-line bg-surface px-3 py-2 text-xs shadow-float">
      <p className="flex items-center gap-2 font-semibold text-ink">
        <span className="h-2 w-2 rounded-full" style={{ backgroundColor: entry.color }} />
        {entry.label}
      </p>
      <p className="mt-1 text-muted">
        {entry.value} {entry.value === 1 ? "task" : "tasks"} · {entry.percent}%
      </p>
    </div>
  );
}

export default function StatusChart({ chartData, total }) {
  const [activeStatus, setActiveStatus] = useState(null);
  const activeEntry = chartData.find((entry) => entry.name === activeStatus) || null;

  return (
    <>
      <div className="relative mx-auto mt-4 h-44 w-44">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              dataKey="value"
              nameKey="label"
              innerRadius={52}
              outerRadius={76}
              paddingAngle={3}
              stroke="none"
              onMouseEnter={(_, index) => setActiveStatus(chartData[index].name)}
              onMouseLeave={() => setActiveStatus(null)}
            >
              {chartData.map((entry) => (
                <Cell
                  key={entry.name}
                  fill={entry.color}
                  fillOpacity={!activeStatus || activeStatus === entry.name ? 1 : 0.3}
                  style={{ cursor: "pointer", outline: "none", transition: "fill-opacity 150ms" }}
                />
              ))}
            </Pie>
            <Tooltip content={<StatusTooltip />} />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          {activeEntry ? (
            <>
              <span className="text-2xl font-semibold text-ink">{activeEntry.value}</span>
              <span
                className="text-[11px] font-medium uppercase tracking-wide"
                style={{ color: activeEntry.color }}
              >
                {activeEntry.label} · {activeEntry.percent}%
              </span>
            </>
          ) : (
            <>
              <span className="text-2xl font-semibold text-ink">{total}</span>
              <span className="text-[11px] font-medium uppercase tracking-wide text-muted">
                total
              </span>
            </>
          )}
        </div>
      </div>

      <ul className="mt-5 space-y-1.5">
        {chartData.map((entry) => (
          <li key={entry.name}>
            <button
              type="button"
              onMouseEnter={() => setActiveStatus(entry.name)}
              onMouseLeave={() => setActiveStatus(null)}
              onFocus={() => setActiveStatus(entry.name)}
              onBlur={() => setActiveStatus(null)}
              className={`flex w-full items-center justify-between gap-3 rounded-lg px-2 py-1.5 text-sm transition-colors ${
                activeStatus === entry.name ? "bg-surface-muted" : "hover:bg-surface-muted/60"
              }`}
            >
              <span className="flex items-center gap-2 text-muted">
                <span
                  className="h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: entry.color }}
                />
                {entry.label}
              </span>
              <span className="flex items-baseline gap-2">
                <span className="text-xs font-medium text-muted">{entry.percent}%</span>
                <span className="font-semibold text-ink">{entry.value}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}
