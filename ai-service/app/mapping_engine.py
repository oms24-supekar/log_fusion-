import re
from dataclasses import dataclass

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression


@dataclass
class Prediction:
    universal_field: str
    confidence: float


TRAINING_DATA = [
    # source IP
    ("src source ip", "source_ip"),
    ("src_ip source address", "source_ip"),
    ("source_ip client ip", "source_ip"),
    ("client_ip remote address", "source_ip"),
    ("remote_ip source host address", "source_ip"),

    # destination IP
    ("dst destination ip", "destination_ip"),
    ("dst_ip destination address", "destination_ip"),
    ("destination_ip server ip", "destination_ip"),
    ("dest_ip target address", "destination_ip"),

    # username
    ("usr user username", "username"),
    ("username account user", "username"),
    ("account_name login user", "username"),
    ("user_name principal", "username"),

    # action
    ("action event action", "action"),
    ("event_action operation", "action"),
    ("event type activity", "action"),
    ("operation command action", "action"),

    # severity
    ("severity level priority", "severity"),
    ("sev event severity", "severity"),
    ("priority log level", "severity"),
    ("level critical warning", "severity"),

    # destination port
    ("dpt destination port", "destination_port"),
    ("dst_port target port", "destination_port"),
    ("destination_port server port", "destination_port"),
    ("port service port", "destination_port"),

    # host
    ("host hostname machine", "host"),
    ("hostname source host", "host"),
    ("device_host machine name", "host"),

    # destination host
    ("destination_host target host", "destination_host"),
    ("dst_host destination hostname", "destination_host"),
    ("server_host target server", "destination_host"),

    # timestamp
    ("timestamp event time", "timestamp"),
    ("event_time time date", "timestamp"),
    ("ts timestamp datetime", "timestamp"),
    ("log_time occurrence time", "timestamp"),
]


class MappingEngine:

    def __init__(self) -> None:
        texts = [item[0] for item in TRAINING_DATA]
        labels = [item[1] for item in TRAINING_DATA]

        self.vectorizer = TfidfVectorizer(
            ngram_range=(1, 2),
            lowercase=True
        )

        features = self.vectorizer.fit_transform(texts)

        self.model = LogisticRegression(
            max_iter=1000
        )

        self.model.fit(features, labels)

    def predict(
        self,
        field_name: str,
        sample_value: str | None
    ) -> Prediction:

        semantic_text = self._build_semantic_text(
            field_name,
            sample_value
        )

        features = self.vectorizer.transform(
            [semantic_text]
        )

        probabilities = self.model.predict_proba(
            features
        )[0]

        best_index = probabilities.argmax()

        predicted_class = self.model.classes_[
            best_index
        ]

        confidence = float(
            probabilities[best_index]
        )

        heuristic = self._value_heuristic(
            field_name,
            sample_value
        )

        if heuristic is not None:
            return heuristic

        return Prediction(
            universal_field=predicted_class,
            confidence=round(confidence, 4)
        )

    def _build_semantic_text(
        self,
        field_name: str,
        sample_value: str | None
    ) -> str:

        normalized_name = re.sub(
            r"[^a-zA-Z0-9]+",
            " ",
            field_name
        )

        value_hint = ""

        if sample_value:
            if self._is_ip(sample_value):
                value_hint = " ip address"
            elif sample_value.isdigit():
                value_hint = " numeric value"

        return (
            normalized_name
            + " "
            + field_name
            + value_hint
        )

    def _value_heuristic(
        self,
        field_name: str,
        sample_value: str | None
    ) -> Prediction | None:

        name = field_name.lower()

        if sample_value and self._is_ip(sample_value):

            if any(
                token in name
                for token in [
                    "src",
                    "source",
                    "client",
                    "remote"
                ]
            ):
                return Prediction(
                    "source_ip",
                    0.99
                )

            if any(
                token in name
                for token in [
                    "dst",
                    "dest",
                    "destination",
                    "server",
                    "target"
                ]
            ):
                return Prediction(
                    "destination_ip",
                    0.99
                )

        if any(
            token in name
            for token in [
                "user",
                "usr",
                "account"
            ]
        ):
            return Prediction(
                "username",
                0.98
            )

        if any(
            token in name
            for token in [
                "severity",
                "sev",
                "priority",
                "level"
            ]
        ):
            return Prediction(
                "severity",
                0.98
            )

        if "port" in name or name == "dpt":
            return Prediction(
                "destination_port",
                0.97
            )

        if any(
            token in name
            for token in [
                "action",
                "operation",
                "event_action"
            ]
        ):
            return Prediction(
                "action",
                0.96
            )

        if any(
            token in name
            for token in [
                "timestamp",
                "event_time",
                "log_time"
            ]
        ) or name == "ts":
            return Prediction(
                "timestamp",
                0.96
            )

        return None

    @staticmethod
    def _is_ip(value: str) -> bool:

        ipv4_pattern = (
            r"^(?:"
            r"(?:25[0-5]|2[0-4]\d|1?\d?\d)"
            r"\.){3}"
            r"(?:25[0-5]|2[0-4]\d|1?\d?\d)$"
        )

        return bool(
            re.match(
                ipv4_pattern,
                value.strip()
            )
        )