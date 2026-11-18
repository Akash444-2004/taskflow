import { useEffect, useState } from "react";

// Animates a number from 0 to `target` with an ease-out curve.
// Jumps straight to the target when the user prefers reduced motion.
export default function useCountUp(target, duration = 700) {
  const [value, setValue] = useState(0);

  useEffect(() => {
    const reduce =
      typeof window.matchMedia === "function" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    if (reduce) {
      setValue(target);
      return undefined;
    }

    let frame;
    const start = performance.now();

    function tick(now) {
      const progress = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(Math.round(target * eased));
      if (progress < 1) {
        frame = requestAnimationFrame(tick);
      }
    }

    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target, duration]);

  return value;
}
