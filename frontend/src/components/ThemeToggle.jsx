import {
  useEffect,
  useState,
} from "react";

const THEME_KEY =
  "logfusion_theme";

export default function ThemeToggle() {
  const [theme, setTheme] =
    useState(() => {
      const stored =
        localStorage.getItem(THEME_KEY);

      if (stored) {
        return stored;
      }

      return window.matchMedia(
        "(prefers-color-scheme: dark)"
      ).matches
        ? "dark"
        : "light";
    });

  useEffect(() => {
    const root =
      document.documentElement;

    root.classList.toggle(
      "dark",
      theme === "dark"
    );

    localStorage.setItem(
      THEME_KEY,
      theme
    );
  }, [theme]);

  return (
    <button
      type="button"
      className="lf-theme-toggle"
      onClick={() =>
        setTheme(
          theme === "dark"
            ? "light"
            : "dark"
        )
      }
      aria-label="Toggle color theme"
      title="Toggle theme"
    >
      <span>
        {theme === "dark"
          ? "☀"
          : "☾"}
      </span>

      {theme === "dark"
        ? "Light"
        : "Dark"}
    </button>
  );
}
