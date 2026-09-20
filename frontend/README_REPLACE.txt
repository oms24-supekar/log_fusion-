LOGFUSION PRODUCTION FRONTEND

Replace your existing frontend with these files, or copy this package over it.

Commands:
  cd frontend
  npm install
  npm run dev

Default API: http://localhost:8080
Optional .env:
  VITE_API_BASE_URL=http://localhost:8080

Key behavior:
- Live Logs refresh every 3s.
- Log investigation refreshes every 2.5s, so AI_QUEUED -> AI_NORMALIZING -> NORMALIZED updates automatically.
- AI Review reads your stored aiReview metadata and underlines suspicious fields.
- Kafka/Ollama/System Health never invents live telemetry that the backend does not expose.
