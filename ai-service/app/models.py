from typing import Any

from pydantic import BaseModel, Field


class FieldCandidate(BaseModel):
    field_name: str
    sample_value: str | None = None


class MappingRequest(BaseModel):
    raw_log: str
    extracted_fields: list[FieldCandidate]


class MappingSuggestion(BaseModel):
    source_field: str
    sample_value: str | None = None
    suggested_universal_field: str
    confidence: float = Field(ge=0.0, le=1.0)
    method: str


class MappingResponse(BaseModel):
    suggestions: list[MappingSuggestion]


class FormatRequest(BaseModel):
    raw_log: str


class FormatResponse(BaseModel):
    predicted_format: str
    confidence: float = Field(ge=0.0, le=1.0)