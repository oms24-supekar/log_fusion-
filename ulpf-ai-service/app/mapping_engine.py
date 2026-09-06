import ipaddress
import re
from pathlib import Path

import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import FeatureUnion


class MappingEngine:
    def __init__(self) -> None:
        data_path = Path(__file__).resolve().parent.parent / "data" / "field_mapping_training.csv"
        df = pd.read_csv(data_path).fillna("")

        self.vectorizer = FeatureUnion([
            ("word", TfidfVectorizer(
                analyzer="word",
                ngram_range=(1, 2),
                lowercase=True,
                sublinear_tf=True
            )),
            ("char", TfidfVectorizer(
                analyzer="char_wb",
                ngram_range=(2, 5),
                lowercase=True,
                sublinear_tf=True
            ))
        ])

        features = self.vectorizer.fit_transform(df["text"])
        labels = df["label"]

        self.model = LogisticRegression(
            max_iter=2500,
            class_weight="balanced"
        )
        self.model.fit(features, labels)

    def predict(
        self,
        field_name: str,
        sample_value: str | None,
        context: str | None = None
    ) -> tuple[str, float]:
        heuristic = self._heuristic(field_name, sample_value)
        if heuristic is not None:
            return heuristic

        semantic_text = self._build_text(field_name, sample_value, context)
        features = self.vectorizer.transform([semantic_text])

        probabilities = self.model.predict_proba(features)[0]
        best_index = probabilities.argmax()

        predicted = self.model.classes_[best_index]
        confidence = float(probabilities[best_index])

        return predicted, round(confidence, 4)

    def _build_text(
        self,
        field_name: str,
        sample_value: str | None,
        context: str | None
    ) -> str:
        normalized_name = re.sub(r"[^a-zA-Z0-9]+", " ", field_name)
        value = sample_value or ""
        ctx = context or ""

        value_type = self._value_type(value)

        return (
            f"field {normalized_name} "
            f"original {field_name} "
            f"value {value} "
            f"type {value_type} "
            f"context {ctx}"
        )

    def _heuristic(
        self,
        field_name: str,
        sample_value: str | None
    ) -> tuple[str, float] | None:
        name = field_name.lower().strip()
        value = (sample_value or "").strip()

        if self._is_ip(value):
            if any(x in name for x in ["src", "source", "client", "remote", "peer"]):
                return "source_ip", 0.995
            if any(x in name for x in ["dst", "dest", "destination", "target", "server"]):
                return "destination_ip", 0.995

        if any(x in name for x in ["user", "usr", "account", "principal", "login", "uid"]):
            return "username", 0.985

        if any(x in name for x in ["severity", "sev", "priority", "level"]):
            return "severity", 0.985

        if name in {"dpt", "dst_port", "destination_port", "target_port", "server_port"}:
            return "destination_port", 0.985

        if name in {"spt", "src_port", "source_port", "client_port"}:
            return "source_port", 0.985

        if any(x in name for x in ["timestamp", "event_time", "log_time", "datetime"]) or name == "ts":
            return "timestamp", 0.985

        if any(x in name for x in ["action", "operation", "event_action", "activity"]):
            return "action", 0.975

        if any(x in name for x in ["protocol", "proto"]):
            return "protocol", 0.975

        if any(x in name for x in ["process", "proc", "pid"]):
            return "process", 0.965

        if any(x in name for x in ["status", "result", "outcome"]):
            return "outcome", 0.965

        if any(x in name for x in ["host", "hostname", "device"]):
            return "host", 0.955

        return None

    @staticmethod
    def _is_ip(value: str) -> bool:
        try:
            ipaddress.ip_address(value)
            return True
        except ValueError:
            return False

    @staticmethod
    def _value_type(value: str) -> str:
        if not value:
            return "empty"
        try:
            ipaddress.ip_address(value)
            return "ip_address"
        except ValueError:
            pass

        if value.isdigit():
            return "numeric"

        if re.match(r"^\d{4}-\d{2}-\d{2}[T\s]", value):
            return "timestamp"

        if value.lower() in {"true", "false", "yes", "no"}:
            return "boolean"

        return "text"
