from fastapi import FastAPI
from app.models import (
    MappingRequest,
    MappingResponse,
    MappingSuggestion,
    FormatRequest,
    FormatResponse,
)
from app.mapping_engine import MappingEngine

app = FastAPI(
    title="ULPF AI Service",
    version="1.0.0"
)

mapping_engine = MappingEngine()


@app.get("/health")
def health():
    return {
        "status": "UP"
    }


@app.post(
    "/ai/suggest-mapping",
    response_model=MappingResponse
)
def suggest_mapping(
    request: MappingRequest
):
    suggestions = []

    for field in request.extracted_fields:

        predicted_field, confidence = (
            mapping_engine.predict(
                field.field_name,
                field.sample_value
            )
        )

        suggestions.append(
            MappingSuggestion(
                source_field=field.field_name,
                sample_value=field.sample_value,
                suggested_universal_field=predicted_field,
                confidence=confidence,
                method="TFIDF_LOGISTIC_REGRESSION"
            )
        )

    return MappingResponse(
        suggestions=suggestions
    )


@app.post(
    "/ai/classify-format",
    response_model=FormatResponse
)
def classify_format(
    request: FormatRequest
):
    raw_log = request.raw_log.strip()

    if (
        raw_log.startswith("{")
        and raw_log.endswith("}")
    ):
        return FormatResponse(
            predicted_format="JSON",
            confidence=0.95
        )

    if raw_log.startswith("CEF:"):
        return FormatResponse(
            predicted_format="CEF",
            confidence=0.99
        )

    if "=" in raw_log:
        return FormatResponse(
            predicted_format="STRUCTURED_KEY_VALUE",
            confidence=0.80
        )

    return FormatResponse(
        predicted_format="UNKNOWN",
        confidence=0.50
    )