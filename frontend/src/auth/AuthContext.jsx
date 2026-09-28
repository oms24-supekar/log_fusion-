import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import api from "../api";

const AuthContext = createContext(null);

const TOKEN_KEY = "logfusion_access_token";
const USER_KEY = "logfusion_user";

/*
 * Deployment:
 *   VITE_AUTH_MODE=api
 *
 * Local UI preview only:
 *   VITE_AUTH_MODE=demo
 *
 * Never deploy production with demo mode enabled.
 */
const AUTH_MODE =
  import.meta.env.VITE_AUTH_MODE || "api";

export function AuthProvider({ children }) {
  const [token, setToken] = useState(
    () => localStorage.getItem(TOKEN_KEY)
  );

  const [user, setUser] = useState(() => {
    try {
      const value = localStorage.getItem(USER_KEY);
      return value ? JSON.parse(value) : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    if (token) {
      api.defaults.headers.common.Authorization =
        `Bearer ${token}`;
    } else {
      delete api.defaults.headers.common.Authorization;
    }
  }, [token]);

  async function login({ email, password }) {
    if (AUTH_MODE === "demo") {
      if (!email || !password) {
        throw new Error(
          "Email and password are required."
        );
      }

      const demoToken = "demo-local-session";
      const demoUser = {
        name: email.split("@")[0] || "Analyst",
        email,
        role: "Security Analyst",
      };

      persistSession(
        demoToken,
        demoUser
      );

      return demoUser;
    }

    const response =
      await api.post(
        "/api/auth/login",
        {
          email,
          password,
        }
      );

    const nextToken =
      response.data?.accessToken ||
      response.data?.token;

    if (!nextToken) {
      throw new Error(
        "Authentication server did not return an access token."
      );
    }

    const nextUser =
      response.data?.user || {
        name:
          response.data?.name ||
          email.split("@")[0],
        email,
        role:
          response.data?.role ||
          "Security Analyst",
      };

    persistSession(
      nextToken,
      nextUser
    );

    return nextUser;
  }

  function persistSession(
    nextToken,
    nextUser
  ) {
    localStorage.setItem(
      TOKEN_KEY,
      nextToken
    );

    localStorage.setItem(
      USER_KEY,
      JSON.stringify(nextUser)
    );

    setToken(nextToken);
    setUser(nextUser);
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);

    setToken(null);
    setUser(null);

    delete api.defaults.headers.common.Authorization;
  }

  const value = useMemo(
    () => ({
      token,
      user,
      authenticated: Boolean(token),
      login,
      logout,
      authMode: AUTH_MODE,
    }),
    [token, user]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context =
    useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider."
    );
  }

  return context;
}
