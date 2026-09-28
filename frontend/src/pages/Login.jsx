import {
  useEffect,
  useState,
} from "react";

import {
  Navigate,
  useLocation,
  useNavigate,
} from "react-router-dom";

import { useAuth } from "../auth/AuthContext";
import ThemeToggle from "../components/ThemeToggle";

export default function Login() {
  const {
    authenticated,
    login,
    authMode,
  } = useAuth();

  const navigate =
    useNavigate();

  const location =
    useLocation();

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const [showPassword, setShowPassword] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  useEffect(() => {
    document.title =
      "Sign in · LogFusion";
  }, []);

  if (authenticated) {
    return (
      <Navigate
        to="/"
        replace
      />
    );
  }

  async function submit(event) {
    event.preventDefault();

    setLoading(true);
    setError("");

    try {
      await login({
        email: email.trim(),
        password,
      });

      navigate(
        location.state?.from || "/",
        {
          replace: true,
        }
      );

    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.message ||
        "Unable to sign in."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="lf-login-shell">
      <div className="lf-login-theme">
        <ThemeToggle />
      </div>

      <section className="lf-login-hero">
        <div className="lf-login-brand">
          <div className="lf-login-logo">
            LF
          </div>

          <span>
            LogFusion
          </span>
        </div>

        <div className="lf-login-hero-copy">
          <span className="lf-login-kicker">
            UNIVERSAL LOG PRE-PROCESSING FRAMEWORK
          </span>

          <h1>
            Security telemetry,
            <br />
            finally speaking one language.
          </h1>

          <p>
            Normalize heterogeneous logs, route unknown
            formats through local AI, learn reusable parsers,
            and investigate every event from one workspace.
          </p>

          <div className="lf-login-feature-list">
            <div>
              <strong>01</strong>
              Deterministic parsing first
            </div>

            <div>
              <strong>02</strong>
              Kafka-backed AI fallback
            </div>

            <div>
              <strong>03</strong>
              Adaptive parser learning
            </div>
          </div>
        </div>

        <div className="lf-login-orbit" aria-hidden="true">
          <span className="orbit orbit-a" />
          <span className="orbit orbit-b" />
          <span className="orbit orbit-c" />

          <div className="orbit-core">
            <span>LF</span>
          </div>
        </div>
      </section>

      <section className="lf-login-panel">
        <div className="lf-login-card">
          <div className="lf-login-card-head">
            <span className="lf-login-kicker">
              SECURE ACCESS
            </span>

            <h2>
              Sign in to LogFusion
            </h2>

            <p>
              Continue to your security processing workspace.
            </p>
          </div>

          {authMode === "demo" && (
            <div className="lf-demo-banner">
              Demo authentication mode is enabled.
              Do not use this mode for deployment.
            </div>
          )}

          {error && (
            <div className="lf-login-error">
              {error}
            </div>
          )}

          <form
            className="lf-login-form"
            onSubmit={submit}
          >
            <label>
              <span>Email</span>

              <input
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) =>
                  setEmail(event.target.value)
                }
                placeholder="analyst@logfusion.local"
                required
              />
            </label>

            <label>
              <span>Password</span>

              <div className="lf-password-field">
                <input
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) =>
                    setPassword(
                      event.target.value
                    )
                  }
                  placeholder="Enter your password"
                  required
                />

                <button
                  type="button"
                  onClick={() =>
                    setShowPassword(
                      (value) => !value
                    )
                  }
                >
                  {showPassword
                    ? "Hide"
                    : "Show"}
                </button>
              </div>
            </label>

            <div className="lf-login-options">
              <label className="lf-checkbox">
                <input
                  type="checkbox"
                />

                <span>
                  Keep me signed in
                </span>
              </label>

              <button
                type="button"
                className="lf-text-button"
              >
                Forgot password?
              </button>
            </div>

            <button
              className="lf-login-submit"
              disabled={loading}
            >
              {loading
                ? "Signing in..."
                : "Sign in"}
            </button>
          </form>

          <div className="lf-login-divider">
            <span />
            <small>
              LOGFUSION
            </small>
            <span />
          </div>

          <div className="lf-login-foot">
            <span className="lf-status-dot" />
            Local-first security processing
          </div>
        </div>
      </section>
    </div>
  );
}
