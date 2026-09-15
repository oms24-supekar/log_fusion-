import axios from "axios";

const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL ||
    (import.meta.env.PROD
      ? "https://log-fusion.onrender.com"
      : "http://localhost:8080"),

  headers: {
    "Content-Type": "application/json",
  },
});

export default api;