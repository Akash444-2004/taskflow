/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "rgb(var(--tf-canvas) / <alpha-value>)",
        surface: "rgb(var(--tf-surface) / <alpha-value>)",
        "surface-raised": "rgb(var(--tf-surface-raised) / <alpha-value>)",
        "surface-muted": "rgb(var(--tf-surface-muted) / <alpha-value>)",
        ink: "rgb(var(--tf-ink) / <alpha-value>)",
        muted: "rgb(var(--tf-muted) / <alpha-value>)",
        line: "rgb(var(--tf-line) / <alpha-value>)",
        brand: "rgb(var(--tf-brand) / <alpha-value>)",
        "brand-hover": "rgb(var(--tf-brand-hover) / <alpha-value>)",
        "brand-soft": "rgb(var(--tf-brand-soft) / <alpha-value>)",
        "brand-contrast": "rgb(var(--tf-brand-contrast) / <alpha-value>)",
        success: "rgb(var(--tf-success) / <alpha-value>)",
        warning: "rgb(var(--tf-warning) / <alpha-value>)",
        danger: "rgb(var(--tf-danger) / <alpha-value>)",
        info: "rgb(var(--tf-info) / <alpha-value>)"
      },
      fontFamily: {
        sans: [
          "Inter",
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "sans-serif"
        ]
      },
      borderRadius: {
        card: "0.875rem"
      },
      boxShadow: {
        card: "0 1px 2px rgb(16 24 40 / 0.04), 0 1px 3px rgb(16 24 40 / 0.06)",
        float: "0 12px 32px -12px rgb(16 24 40 / 0.18)"
      },
      keyframes: {
        "fade-up": {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" }
        }
      },
      animation: {
        "fade-up": "fade-up 420ms cubic-bezier(0.22, 1, 0.36, 1) both"
      }
    }
  },
  plugins: []
};
