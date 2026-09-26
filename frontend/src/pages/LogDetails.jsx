import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";

import api from "../api";

import {
  PageHeader,
  Panel,
  Badge,
  Loading,
  ErrorBox,
  InfoRow,
  ReviewField,
} from "../components/UI";

import {
  formatDate,
  reviewMap,
} from "../lib/utils";

export default function LogDetails() {
  const { id } = useParams();

  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadDetails() {
      try {
        const response =
          await api.get(`/api/logs/${id}`);

        if (!active) {
          return;
        }

        setData(response.data);
        setError("");
        setLoading(false);

      } catch (err) {

        if (!active) {
          return;
        }

        setError(
          err.response?.data?.message ||
          err.message ||
          "Unable to load event"
        );

        setLoading(false);
      }
    }

    loadDetails();

    /*
     * Important for Kafka / AI processing:
     *
     * An event can start as AI_QUEUED and later
     * become AI_NORMALIZING -> NORMALIZED.
     *
     * Keep polling so this page updates automatically.
     */
    const interval =
      setInterval(
        loadDetails,
        2000
      );

    return () => {
      active = false;
      clearInterval(interval);
    };

  }, [id]);

  const parsed =
    useMemo(
      () => parseNormalized(
        data?.normalized
      ),
      [data]
    );

  const reviews =
    useMemo(
      () => reviewMap(
        parsed.aiReview
      ),
      [parsed.aiReview]
    );

  if (loading) {
    return (
      <div className="page">
        <Loading />
      </div>
    );
  }

  const raw =
    data?.raw;

  const normalizedDetails =
    data?.normalized;

  const event =
    parsed.event;

  const review =
    (field) =>
      reviews.get(field);

  const status =
    raw?.processingStatus;

  const aiPending =
    status === "AI_QUEUED" ||
    status === "AI_NORMALIZING";

  return (
    <div className="page">

      <PageHeader
        eyebrow="FORENSICS"
        title="Event investigation"
        subtitle="
          Raw evidence, processing state,
          normalized event and AI confidence
          in one trace.
        "
      />

      <ErrorBox
        message={error}
      />

      {/* =========================
          TOP TRACE BAR
         ========================= */}

      <div className="trace-strip">

        <div>
          <span>RAW LOG ID</span>

          <code>
            {raw?.id || id}
          </code>
        </div>

        <div>
          <span>FORMAT</span>

          <Badge
            value={
              raw?.detectedFormat
            }
          />
        </div>

        <div>
          <span>STATUS</span>

          <Badge
            value={status}
          />
        </div>

        <div>
          <span>AI REVIEW</span>

          <strong>
            {
              parsed.aiReview
                ? `${Math.round(
                    parsed.aiReview
                      .overallConfidence *
                    100
                  )}%`
                : aiPending
                ? "PROCESSING"
                : "N/A"
            }
          </strong>
        </div>

      </div>

      {/* =========================
          MAIN TWO COLUMN VIEW
         ========================= */}

      <div className="grid-2">

        {/* RAW LOG */}

        <Panel
          title="Immutable raw event"
          subtitle="Original forensic content"
        >

          <pre className="codebox">
            {
              raw?.content ||
              "Raw content unavailable"
            }
          </pre>

          <InfoRow
            label="Source"
            value={
              raw?.sourceName
            }
          />

          <InfoRow
            label="Source type"
            value={
              raw?.sourceType
            }
          />

          <InfoRow
            label="Detected format"
            value={
              <Badge
                value={
                  raw?.detectedFormat
                }
              />
            }
          />

          <InfoRow
            label="Processing status"
            value={
              <Badge
                value={
                  raw?.processingStatus
                }
              />
            }
          />

          <InfoRow
            label="Received"
            value={
              formatDate(
                raw?.receivedAt
              )
            }
          />

          <InfoRow
            label="SHA-256"
            value={
              <code>
                {raw?.sha256Hash}
              </code>
            }
          />

        </Panel>

        {/* NORMALIZED LOG */}

        <Panel
          title="Normalized event"
          subtitle={
            normalizedDetails
              ? `${normalizedDetails.parserUsed} · ${normalizedDetails.validationStatus}`
              : aiPending
              ? "AI processing in progress"
              : "Normalized output unavailable"
          }
        >

          {
            event ? (

              <div className="review-grid">

                <ReviewField
                  label="timestamp"
                  value={
                    event.timestamp
                  }
                  review={
                    review(
                      "timestamp"
                    )
                  }
                />

                <ReviewField
                  label="host"
                  value={
                    event.source?.host
                  }
                  review={
                    review(
                      "host"
                    )
                  }
                />

                <ReviewField
                  label="source_ip"
                  value={
                    event.source?.ip
                  }
                  review={
                    review(
                      "source_ip"
                    )
                  }
                />

                <ReviewField
                  label="username"
                  value={
                    event.user?.name
                  }
                  review={
                    review(
                      "username"
                    )
                  }
                />

                <ReviewField
                  label="action"
                  value={
                    event.event?.action
                  }
                  review={
                    review(
                      "action"
                    )
                  }
                />

                <ReviewField
                  label="outcome"
                  value={
                    event.event?.outcome
                  }
                  review={
                    review(
                      "outcome"
                    )
                  }
                />

                <ReviewField
                  label="severity"
                  value={
                    event.event?.severity
                  }
                  review={
                    review(
                      "severity"
                    )
                  }
                />

                <ReviewField
                  label="destination_ip"
                  value={
                    event.destination?.ip
                  }
                  review={
                    review(
                      "destination_ip"
                    )
                  }
                />

                <ReviewField
                  label="destination_port"
                  value={
                    event.destination?.port
                  }
                  review={
                    review(
                      "destination_port"
                    )
                  }
                />

                <ReviewField
                  label="category"
                  value={
                    event.event?.category
                  }
                  review={
                    review(
                      "category"
                    )
                  }
                />

              </div>

            ) : aiPending ? (

              <div className="ai-processing-card">

                <div className="spinner" />

                <strong>
                  {
                    status ===
                    "AI_QUEUED"
                      ? "Waiting for AI worker"
                      : "AI normalization running"
                  }
                </strong>

                <span>
                  Kafka has accepted the
                  event. This view refreshes
                  automatically.
                </span>

                <div className="ai-progress">

                  <div
                    className={
                      status ===
                      "AI_QUEUED"
                        ? "active"
                        : "complete"
                    }
                  >
                    Kafka queued
                  </div>

                  <div
                    className={
                      status ===
                      "AI_NORMALIZING"
                        ? "active"
                        : ""
                    }
                  >
                    Ollama processing
                  </div>

                  <div>
                    Normalized
                  </div>

                </div>

              </div>

            ) : (

              <div className="empty">

                <strong>
                  No normalized event
                </strong>

                <span>
                  This event has not produced
                  a normalized representation.
                </span>

              </div>
            )
          }

        </Panel>

      </div>

      {/* =========================
          AI REVIEW
         ========================= */}

      {
        parsed.aiReview && (

          <Panel
            title="AI field review"
            subtitle="
              Suspicious fields remain valid
              but are highlighted for analyst
              inspection.
            "
          >

            <div className="review-list">

              {
                parsed.aiReview
                  .fields
                  ?.map(
                    (item) => (

                      <div
                        key={
                          item.field
                        }
                        className={
                          `review-row ${
                            item.suspicious
                              ? "suspect"
                              : ""
                          }`
                        }
                      >

                        <strong>
                          {item.field}
                        </strong>

                        <span>
                          {
                            Math.round(
                              item.confidence *
                              100
                            )
                          }%
                        </span>

                        <p>
                          {item.reason}
                        </p>

                        <Badge
                          value={
                            item.suspicious
                              ? "SUSPICIOUS"
                              : "SUPPORTED"
                          }
                        />

                      </div>
                    )
                  )
              }

            </div>

          </Panel>
        )
      }

      {/* =========================
          FULL NORMALIZED JSON
         ========================= */}

      {
        normalizedDetails && (

          <Panel
            title="Stored normalized JSON"
            subtitle="
              Exact normalized object
              returned by the backend
            "
          >

            <pre className="codebox">
              {
                JSON.stringify(
                  normalizedDetails.data,
                  null,
                  2
                )
              }
            </pre>

          </Panel>
        )
      }

    </div>
  );
}


/*
 * =====================================================
 * NORMALIZED DATA ADAPTER
 * =====================================================
 *
 * Backend currently returns:
 *
 * normalized: {
 *   id,
 *   data,
 *   parserUsed,
 *   validationStatus,
 *   processedAt
 * }
 *
 * data can either be:
 *
 * old format:
 * {
 *   source,
 *   event,
 *   ...
 * }
 *
 * or new AI format:
 *
 * {
 *   normalized: {...},
 *   aiReview: {...}
 * }
 */

function parseNormalized(
  normalizedDetails
) {

  const data =
    normalizedDetails?.data;

  if (
    !data ||
    typeof data !== "object"
  ) {

    return {
      event: null,
      aiReview: null,
    };
  }

  if (data.normalized) {

    return {
      event:
        data.normalized,

      aiReview:
        data.aiReview ||
        null,
    };
  }

  return {
    event: data,

    aiReview:
      data.aiReview ||
      null,
  };
}