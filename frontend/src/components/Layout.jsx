import {
  NavLink,
  Outlet,
  useNavigate,
} from "react-router-dom";

import { useAuth } from "../auth/AuthContext";
import ThemeToggle from "./ThemeToggle";

const nav = [
  ["/", "Overview"],
  ["/ingest", "Ingest"],
  ["/logs", "Live Logs"],
  ["/ai-review", "AI Review"],
  ["/parsers", "Parser Intelligence"],
  ["/ai-engine", "AI Engine"],
  ["/pipeline", "Kafka Pipeline"],
  ["/analytics", "Analytics"],
  ["/health", "System Health"],
  ["/settings", "Settings"],
];

export default function Layout() {
  const {
    user,
    logout,
  } = useAuth();

  const navigate =
    useNavigate();

  function signOut() {
    logout();
    navigate(
      "/login",
      {
        replace: true,
      }
    );
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            LF
          </div>

          <div>
            <strong>
              LogFusion
            </strong>
            <span>
              ULPF
            </span>
          </div>
        </div>

        <nav>
          {nav.map(
            ([to, label]) => (
              <NavLink
                key={to}
                to={to}
                end={to === "/"}
                className={({
                  isActive,
                }) =>
                  isActive
                    ? "nav-link active"
                    : "nav-link"
                }
              >
                {label}
              </NavLink>
            )
          )}
        </nav>

        <div className="sidebar-foot">
          <span className="live-dot" />

          <div>
            <strong>
              Local Stack
            </strong>

            <small>
              Spring · Kafka · Ollama
            </small>
          </div>
        </div>
      </aside>

      <main className="content">
        <div className="topbar">
          <div className="lf-topbar-left">
            <span className="live-dot" />
            Security processing workspace
          </div>

          <div className="lf-topbar-actions">
            <ThemeToggle />

            <div className="lf-user-chip">
              <div className="lf-user-avatar">
                {String(
                  user?.name ||
                  user?.email ||
                  "U"
                )
                  .slice(0, 1)
                  .toUpperCase()}
              </div>

              <div>
                <strong>
                  {user?.name ||
                    "Analyst"}
                </strong>

                <small>
                  {user?.role ||
                    "Security Analyst"}
                </small>
              </div>
            </div>

            <button
              type="button"
              className="lf-signout"
              onClick={signOut}
            >
              Sign out
            </button>
          </div>
        </div>

        <Outlet />
      </main>
    </div>
  );
}
