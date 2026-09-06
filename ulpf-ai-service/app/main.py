import re

from fastapi import FastAPI

from app.mapping_engine import MappingEngine
from app.models import (
    FormatRequest,
    FormatResponse,
    MappingRequest,
    MappingResponse,
    MappingSuggestion,
)

app = FastAPI(
    title="ULPF AI Service",
    version="2.0.0"
)

mapping_engine = MappingEngine()


@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "ULPF AI Service",
        "version": "2.0.0"
    }


@app.post("/ai/suggest-mapping", response_model=MappingResponse)
def suggest_mapping(request: MappingRequest):
    suggestions = []

    for field in request.extracted_fields:
        predicted_field, confidence = mapping_engine.predict(
            field.field_name,
            field.sample_value,
            field.context or request.raw_log
        )

        suggestions.append(
            MappingSuggestion(
                source_field=field.field_name,
                sample_value=field.sample_value,
                suggested_universal_field=predicted_field,
                confidence=confidence,
                method="TFIDF_CHAR_WORD_LOGISTIC_REGRESSION"
            )
        )

    return MappingResponse(suggestions=suggestions)


@app.post("/ai/classify-format", response_model=FormatResponse)
def classify_format(request: FormatRequest):
    raw = request.raw_log.strip()

    if raw.startswith("CEF:"):
        return FormatResponse(predicted_format="CEF", confidence=0.99)

    if raw.startswith("LEEF:"):
        return FormatResponse(predicted_format="LEEF", confidence=0.99)

    if raw.startswith("{") and raw.endswith("}"):
        return FormatResponse(predicted_format="JSON", confidence=0.97)

    if re.match(
        r"^(?:<\d+>)?(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+\d{1,2}\s+\d{2}:\d{2}:\d{2}\s+.+",
        raw
    ):
        return FormatResponse(predicted_format="SYSLOG", confidence=0.95)

    if re.match(
        r"^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}.*\b(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\b",
        raw
    ):
        return FormatResponse(predicted_format="APPLICATION_LOG", confidence=0.93)

    kv_count = len(re.findall(r"(?:^|[\s,|;])[^=\s,|;]+=[^=\s,|;]+", raw))
    if kv_count >= 2:
        return FormatResponse(predicted_format="KEY_VALUE", confidence=0.90)

    if "|" in raw and raw.count("|") >= 2:
        return FormatResponse(predicted_format="DELIMITED", confidence=0.82)

    if "," in raw and raw.count(",") >= 2:
        return FormatResponse(predicted_format="CSV_LIKE", confidence=0.78)

    return FormatResponse(predicted_format="UNKNOWN", confidence=0.50)
