import { cn } from "../../utils/format";

export default function Skeleton({ className }) {
  return (
    <div aria-hidden="true" className={cn("animate-pulse rounded-lg bg-surface-muted", className)} />
  );
}

export function SkeletonStatCards({ count = 4 }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="rounded-card border border-line bg-surface p-5 shadow-card">
          <Skeleton className="h-3 w-20" />
          <Skeleton className="mt-4 h-8 w-16" />
          <Skeleton className="mt-4 h-3 w-32" />
        </div>
      ))}
    </div>
  );
}

export function SkeletonCards({ count = 6 }) {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="rounded-card border border-line bg-surface p-5 shadow-card">
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="mt-2 h-3 w-full" />
          <Skeleton className="mt-1 h-3 w-4/5" />
          <Skeleton className="mt-5 h-1.5 w-full rounded-full" />
          <Skeleton className="mt-4 h-3 w-24" />
        </div>
      ))}
    </div>
  );
}

export function SkeletonRows({ count = 6 }) {
  return (
    <div className="overflow-hidden rounded-card border border-line bg-surface shadow-card">
      <ul className="divide-y divide-line">
        {Array.from({ length: count }).map((_, index) => (
          <li key={index} className="flex items-center gap-3 px-5 py-4">
            <Skeleton className="h-9 w-9 shrink-0 rounded-full" />
            <div className="min-w-0 flex-1 space-y-2">
              <Skeleton className="h-3.5 w-1/3" />
              <Skeleton className="h-3 w-1/2" />
            </div>
            <Skeleton className="h-5 w-16 rounded-full" />
            <Skeleton className="h-5 w-16 rounded-full" />
          </li>
        ))}
      </ul>
    </div>
  );
}

export function SkeletonColumns({ count = 3 }) {
  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {Array.from({ length: count }).map((_, columnIndex) => (
        <div
          key={columnIndex}
          className="rounded-card border border-line bg-surface-muted/30 p-3"
        >
          <Skeleton className="mx-1 mb-3 h-4 w-24" />
          <div className="space-y-2.5">
            {Array.from({ length: 3 - (columnIndex % 2) }).map((__, cardIndex) => (
              <div
                key={cardIndex}
                className="rounded-xl border border-line bg-surface p-3.5"
              >
                <Skeleton className="h-3.5 w-3/4" />
                <Skeleton className="mt-2 h-3 w-1/2" />
                <div className="mt-3 flex items-center justify-between border-t border-line pt-3">
                  <Skeleton className="h-6 w-6 rounded-full" />
                  <Skeleton className="h-3 w-14" />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
