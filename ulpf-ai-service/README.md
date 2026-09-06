# ULPF AI Service

## Local run

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Health:
`GET /health`

Mapping:
`POST /ai/suggest-mapping`

Format classification:
`POST /ai/classify-format`
