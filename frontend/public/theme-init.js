(function () {
  try {
    var key = "taskflow-theme";
    var saved = localStorage.getItem(key);
    var prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    var isDark = saved === "dark" || (!saved && prefersDark);
    document.documentElement.classList.toggle("dark", isDark);
    document.documentElement.style.colorScheme = isDark ? "dark" : "light";
    document
      .querySelector('meta[name="theme-color"]')
      .setAttribute("content", isDark ? "#111827" : "#f7f8fa");
  } catch (_) {
    // Theme persistence is optional; the system preference remains the fallback.
  }
})();
