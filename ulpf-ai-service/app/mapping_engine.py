import ipaddress
import re
from pathlib import Path

import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import FeatureUnion


class MappingEngine:
    def __init__(self) -> None:
        data_path = (
            Path(__file__).resolve().parent.parent
            / "data"
            / "field_mapping_training.csv"
        )

        df = pd.read_csv(data_path).fillna("")

        self.vectorizer = FeatureUnion([
            (
                "word",
                TfidfVectorizer(
                    analyzer="word",
                    ngram_range=(1, 2),
                    lowercase=True,
                    sublinear_tf=True
                )
            ),
            (
                "char",
                TfidfVectorizer(
                    analyzer="char_wb",
                    ngram_range=(2, 5),
                    lowercase=True,
                    sublinear_tf=True
                )
            )
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

        heuristic = self._heuristic(
            field_name,
            sample_value
        )

        if heuristic is not None:
            return heuristic

        semantic_text = self._build_text(
            field_name,
            sample_value,
            context
        )

        features = self.vectorizer.transform(
            [semantic_text]
        )

        probabilities = self.model.predict_proba(
            features
        )[0]

        best_index = probabilities.argmax()

        predicted = self.model.classes_[best_index]
        confidence = float(
            probabilities[best_index]
        )

        return predicted, round(confidence, 4)

    def _build_text(
        self,
        field_name: str,
        sample_value: str | None,
        context: str | None
    ) -> str:

        normalized_name = re.sub(
            r"[^a-zA-Z0-9]+",
            " ",
            field_name
        )

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
        value_lower = value.lower()

        # ---------------------------------------------
        # ACTION VALUES
        # ---------------------------------------------

        action_values = {
            "allow",
            "allowed",
            "deny",
            "denied",
            "block",
            "blocked",
            "drop",
            "dropped",
            "accept",
            "accepted",
            "reject",
            "rejected",
            "permit",
            "permitted",
            "login",
            "logout",
            "connect",
            "disconnect",
            "auth_fail",
            "auth_failed",
            "login_fail",
            "login_failed",
            "access_allow",
            "access_allowed",
            "access_deny",
            "access_denied",
            "intrusion_block",
            "intrusion_detect",
            "malware_detect",
            "malware_block"
        }

        if (
            name == "token1"
            and value_lower in action_values
        ):
            return "action", 0.99

        # ---------------------------------------------
        # IP ADDRESS FIELDS
        # ---------------------------------------------

        if self._is_ip(value):

            destination_names = [
                "dst",
                "dest",
                "destination",
                "destination_ip",
                "dst_ip",
                "target",
                "target_ip",
                "remote",
                "remote_host",
                "remote_ip",
                "server",
                "server_ip",
                "peer_dst"
            ]

            source_names = [
                "src",
                "source",
                "source_ip",
                "src_ip",
                "client",
                "client_ip",
                "origin",
                "origin_addr",
                "origin_ip",
                "sender",
                "peer_src"
            ]

            if any(
                x in name
                for x in destination_names
            ):
                return "destination_ip", 0.995

            if any(
                x in name
                for x in source_names
            ):
                return "source_ip", 0.995

        # ---------------------------------------------
        # USERNAME
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "username",
                "user",
                "usr",
                "account",
                "identity",
                "principal",
                "login",
                "uid",
                "actor",
                "subject_user"
            ]
        ):
            return "username", 0.985

        # ---------------------------------------------
        # SEVERITY
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "severity",
                "sev",
                "priority",
                "level",
                "risk_level",
                "risk",
                "threat_level",
                "threat_severity"
            ]
        ):
            return "severity", 0.985

        # ---------------------------------------------
        # DESTINATION PORT
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "dpt",
                "dst_port",
                "destination_port",
                "target_port",
                "server_port",
                "service_port",
                "remote_port"
            ]
        ):
            return "destination_port", 0.985

        # ---------------------------------------------
        # SOURCE PORT
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "spt",
                "src_port",
                "source_port",
                "client_port",
                "origin_port",
                "local_port"
            ]
        ):
            return "source_port", 0.985

        # ---------------------------------------------
        # TIMESTAMP
        # ---------------------------------------------

        if (
            any(
                x in name
                for x in [
                    "timestamp",
                    "event_time",
                    "log_time",
                    "datetime",
                    "created_at",
                    "created_time",
                    "event_timestamp"
                ]
            )
            or name in {"ts", "time"}
        ):
            return "timestamp", 0.985

        # ---------------------------------------------
        # ACTION FIELD NAME
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "action",
                "operation",
                "event_action",
                "activity",
                "command"
            ]
        ):
            return "action", 0.975

        # ---------------------------------------------
        # PROTOCOL
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "protocol",
                "proto",
                "transport_protocol"
            ]
        ):
            return "protocol", 0.975

        # ---------------------------------------------
        # PROCESS
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "process",
                "process_name",
                "proc",
                "pid"
            ]
        ):
            return "process", 0.965

        # ---------------------------------------------
        # OUTCOME
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "status",
                "result",
                "outcome",
                "success",
                "failure"
            ]
        ):
            return "outcome", 0.965

        # ---------------------------------------------
        # HOST
        # ---------------------------------------------

        if any(
            x in name
            for x in [
                "hostname",
                "device_name",
                "device_host",
                "local_host"
            ]
        ):
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

        if re.match(
            r"^\d{4}-\d{2}-\d{2}[T\s]",
            value
        ):
            return "timestamp"

        if value.lower() in {
            "true",
            "false",
            "yes",
            "no"
        }:
            return "boolean"

        return "text"