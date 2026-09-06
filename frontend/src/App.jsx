import {
  NavLink,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";

import {
  useEffect,
  useState,
} from "react";

import api from "./api";


/* =========================================================
   APP
========================================================= */

export default function App() {
  return (
    <div className="app-shell">

      <Sidebar />

      <main className="main-content">
        <Routes>

          <Route
            path="/"
            element={<Dashboard />}
          />

          <Route
            path="/ingest"
            element={<LogIngestion />}
          />

          <Route
            path="/logs"
            element={<LogExplorer />}
          />

          <Route
            path="/logs/:id"
            element={<LogDetails />}
          />

          <Route
            path="/unknown"
            element={<UnknownLogs />}
          />

          <Route
            path="/unknown/:id"
            element={<UnknownAnalysis />}
          />

          <Route
            path="/parsers"
            element={<ParserRegistry />}
          />

        </Routes>
      </main>

    </div>
  );
}


/* =========================================================
   SIDEBAR
========================================================= */

function Sidebar() {

  const items = [
    {
      path: "/",
      icon: "◈",
      label: "Dashboard",
    },
    {
      path: "/ingest",
      icon: "+",
      label: "Log Ingestion",
    },
    {
      path: "/logs",
      icon: "☷",
      label: "Log Explorer",
    },
    {
      path: "/unknown",
      icon: "?",
      label: "Unknown Logs",
    },
    {
      path: "/parsers",
      icon: "⚙",
      label: "Parser Registry",
    },
  ];

  return (
    <aside className="sidebar">

      <div className="brand">

        <div className="brand-logo">
          U
        </div>

        <div>
          <div className="brand-name">
            ULPF
          </div>

          <div className="brand-subtitle">
            Universal Log Framework
          </div>
        </div>

      </div>

      <div className="sidebar-label">
        PLATFORM
      </div>

      <nav className="nav">

        {items.map((item) => (

          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === "/"}
            className={({ isActive }) =>
              isActive
                ? "nav-item active"
                : "nav-item"
            }
          >

            <span className="nav-icon">
              {item.icon}
            </span>

            <span>
              {item.label}
            </span>

          </NavLink>

        ))}

      </nav>

      <div className="sidebar-bottom">

        <div className="engine-status">

          <span className="green-dot" />

          <div>
            <strong>
              Processing Engine
            </strong>

            <small>
              Online
            </small>
          </div>

        </div>

      </div>

    </aside>
  );
}


/* =========================================================
   DASHBOARD
========================================================= */

function Dashboard() {

  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadLogs();
  }, []);

  async function loadLogs() {

    try {

      const response =
        await api.get("/api/logs");

      setLogs(
        Array.isArray(response.data)
          ? response.data
          : []
      );

    } catch (error) {

      console.error(
        "Failed to load logs:",
        error
      );

    } finally {

      setLoading(false);
    }
  }

  const normalized =
    logs.filter(
      (log) =>
        log.processingStatus === "NORMALIZED"
    ).length;

  const review =
    logs.filter(
      (log) =>
        log.processingStatus === "NEEDS_REVIEW"
    ).length;

  const failed =
    logs.filter(
      (log) =>
        log.processingStatus === "FAILED"
    ).length;

  return (
    <Page>

      <PageHeader
        title="Security Event Overview"
        subtitle="Unified visibility across heterogeneous log sources"
      />

      <div className="stats-grid">

        <StatCard
          title="Total Events"
          value={logs.length}
          description="Raw forensic events"
        />

        <StatCard
          title="Normalized"
          value={normalized}
          description="Universal schema"
        />

        <StatCard
          title="Needs Review"
          value={review}
          description="Unknown formats"
        />

        <StatCard
          title="Failed"
          value={failed}
          description="Processing failures"
        />

      </div>

      <section className="panel">

        <PanelHeader
          title="Recent Events"
          subtitle="Latest logs received by ULPF"
        />

        {loading
          ? <Loading />
          : (
            <LogTable
              logs={logs.slice(0, 10)}
            />
          )
        }

      </section>

    </Page>
  );
}


function StatCard({
  title,
  value,
  description,
}) {

  return (
    <div className="stat-card">

      <div className="stat-title">
        {title}
      </div>

      <div className="stat-value">
        {value}
      </div>

      <div className="stat-description">
        {description}
      </div>

    </div>
  );
}


/* =========================================================
   INGESTION
========================================================= */

function LogIngestion() {

  const [rawContent, setRawContent] =
    useState("");

  const [sourceName, setSourceName] =
    useState("");

  const [sourceType, setSourceType] =
    useState("FIREWALL");

  const [result, setResult] =
    useState(null);

  const [loading, setLoading] =
    useState(false);

  async function submitLog(event) {

    event.preventDefault();

    setLoading(true);
    setResult(null);

    try {

      const response =
        await api.post(
          "/api/logs/process",
          {
            rawContent,
            sourceName,
            sourceType,
          }
        );

      setResult(response.data);

    } catch (error) {

      console.error(error);

      alert(
        error.response?.data?.message ||
        "Log processing failed"
      );

    } finally {

      setLoading(false);
    }
  }


  function loadExample() {

    setSourceName(
      "orion-firewall-demo"
    );

    setSourceType(
      "FIREWALL"
    );

    setRawContent(
      "ORION|ACCESS_ALLOW|CLIENT_IP=192.168.50.99|ACCOUNT=admin|TARGET_IP=10.0.0.20|DPT=22"
    );
  }


  return (
    <Page>

      <PageHeader
        title="Log Ingestion"
        subtitle="Submit heterogeneous logs into the preprocessing pipeline"
      />

      <div className="two-columns">

        <section className="panel">

          <div className="panel-header">

            <div>
              <h2>
                Submit Event
              </h2>

              <p>
                Original content is preserved before processing.
              </p>
            </div>

            <button
              type="button"
              className="secondary-button"
              onClick={loadExample}
            >
              Load ORION Example
            </button>

          </div>

          <form onSubmit={submitLog}>

            <label>
              Source Name
            </label>

            <input
              value={sourceName}
              onChange={(event) =>
                setSourceName(
                  event.target.value
                )
              }
              placeholder="firewall-01"
              required
            />

            <label>
              Source Type
            </label>

            <select
              value={sourceType}
              onChange={(event) =>
                setSourceType(
                  event.target.value
                )
              }
            >

              <option value="FIREWALL">
                Firewall
              </option>

              <option value="LINUX">
                Linux
              </option>

              <option value="WINDOWS">
                Windows
              </option>

              <option value="APPLICATION">
                Application
              </option>

              <option value="NETWORK">
                Network Device
              </option>

              <option value="OTHER">
                Other
              </option>

            </select>

            <label>
              Raw Log
            </label>

            <textarea
              className="raw-input"
              value={rawContent}
              onChange={(event) =>
                setRawContent(
                  event.target.value
                )
              }
              placeholder="Paste raw log here..."
              required
            />

            <button
              className="primary-button"
              disabled={loading}
            >
              {loading
                ? "Processing..."
                : "Process Log"
              }
            </button>

          </form>

        </section>


        <section className="panel">

          <PanelHeader
            title="Processing Result"
            subtitle="Detection and forensic metadata"
          />

          {!result ? (

            <EmptyState>
              Submit a log to see the result.
            </EmptyState>

          ) : (

            <div>

              <InfoRow
                label="Raw Log ID"
                value={result.id}
              />

              <InfoRow
                label="Detected Format"
                value={
                  <Badge
                    value={result.detectedFormat}
                  />
                }
              />

              <InfoRow
                label="Status"
                value={
                  <Badge
                    value={result.processingStatus}
                  />
                }
              />

              <InfoRow
                label="Source"
                value={result.sourceName}
              />

              <InfoRow
                label="Source Type"
                value={result.sourceType}
              />

              <InfoRow
                label="SHA-256"
                value={result.sha256Hash}
              />

              <InfoRow
                label="Received"
                value={
                  formatDate(
                    result.receivedAt
                  )
                }
              />

            </div>

          )}

        </section>

      </div>

    </Page>
  );
}


/* =========================================================
   LOG EXPLORER
========================================================= */

function LogExplorer() {

  const [logs, setLogs] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    loadLogs();
  }, []);

  async function loadLogs() {

    try {

      const response =
        await api.get("/api/logs");

      setLogs(
        Array.isArray(response.data)
          ? response.data
          : []
      );

    } catch (error) {

      console.error(error);

    } finally {

      setLoading(false);
    }
  }

  return (
    <Page>

      <PageHeader
        title="Log Explorer"
        subtitle="Investigate raw and normalized security events"
      />

      <section className="panel">

        {loading
          ? <Loading />
          : <LogTable logs={logs} />
        }

      </section>

    </Page>
  );
}


function LogTable({ logs }) {

  const navigate = useNavigate();

  if (!logs?.length) {

    return (
      <EmptyState>
        No logs found.
      </EmptyState>
    );
  }

  return (
    <div className="table-wrapper">

      <table>

        <thead>

          <tr>
            <th>TIME</th>
            <th>SOURCE</th>
            <th>TYPE</th>
            <th>FORMAT</th>
            <th>STATUS</th>
          </tr>

        </thead>

        <tbody>

          {logs.map((log) => (

            <tr
              key={log.id}
              onClick={() =>
                navigate(
                  `/logs/${log.id}`
                )
              }
            >

              <td>
                {formatDate(
                  log.receivedAt
                )}
              </td>

              <td>
                {log.sourceName}
              </td>

              <td>
                {log.sourceType || "-"}
              </td>

              <td>
                <Badge
                  value={log.detectedFormat}
                />
              </td>

              <td>
                <Badge
                  value={log.processingStatus}
                />
              </td>

            </tr>

          ))}

        </tbody>

      </table>

    </div>
  );
}


/* =========================================================
   LOG DETAILS
========================================================= */

function LogDetails() {

  const { id } = useParams();

  const [details, setDetails] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    loadDetails();
  }, [id]);

  async function loadDetails() {

    try {

      const response =
        await api.get(
          `/api/logs/${id}`
        );

      setDetails(response.data);

    } catch (error) {

      console.error(error);

    } finally {

      setLoading(false);
    }
  }


  if (loading) {

    return (
      <Page>
        <Loading />
      </Page>
    );
  }


  if (!details) {

    return (
      <Page>
        <EmptyState>
          Log not found.
        </EmptyState>
      </Page>
    );
  }


  const raw =
    details.raw ||
    details.rawLog ||
    details;

  const normalized =
    details.normalized ||
    details.normalizedLog ||
    null;


  return (
    <Page>

      <PageHeader
        title="Event Investigation"
        subtitle="Original forensic evidence compared with normalized representation"
      />


      <div className="trace-bar">

        <div>

          <span>
            RAW LOG ID
          </span>

          <code>
            {raw?.id || id}
          </code>

        </div>


        <div>

          <span>
            FORMAT
          </span>

          <Badge
            value={raw?.detectedFormat}
          />

        </div>


        <div>

          <span>
            STATUS
          </span>

          <Badge
            value={raw?.processingStatus}
          />

        </div>

      </div>


      <div className="comparison-grid">

        <section className="comparison-panel">

          <div className="comparison-header">

            <strong>
              ORIGINAL / RAW LOG
            </strong>

            <span>
              FORENSIC EVIDENCE
            </span>

          </div>


          <pre className="raw-log">

            {raw?.rawContent ||
              "Raw content unavailable"}

          </pre>


          <div className="comparison-info">

            <InfoRow
              label="Source"
              value={raw?.sourceName}
            />

            <InfoRow
              label="Source Type"
              value={raw?.sourceType}
            />

            <InfoRow
              label="SHA-256"
              value={raw?.sha256Hash}
            />

            <InfoRow
              label="Received"
              value={
                formatDate(
                  raw?.receivedAt
                )
              }
            />

          </div>

        </section>


        <section className="comparison-panel">

          <div className="comparison-header">

            <strong>
              NORMALIZED EVENT
            </strong>

            <span>
              UNIVERSAL SCHEMA
            </span>

          </div>


          {!normalized ? (

            <EmptyState>
              Event has not been normalized.
            </EmptyState>

          ) : (

            <NormalizedViewer
              normalized={normalized}
            />

          )}

        </section>

      </div>

    </Page>
  );
}


function NormalizedViewer({
  normalized,
}) {

  let data =
    normalized.normalizedData ||
    normalized.data ||
    normalized;

  if (typeof data === "string") {

    try {

      data = JSON.parse(data);

    } catch {

      // Keep string if not JSON.
    }
  }


  return (
    <div>

      <div className="human-grid">

        <HumanCard
          label="Action"
          value={
            data?.event?.action ||
            "-"
          }
        />

        <HumanCard
          label="Outcome"
          value={
            data?.event?.outcome ||
            "-"
          }
        />

        <HumanCard
          label="Source IP"
          value={
            data?.source?.ip ||
            data?.source?.address ||
            "-"
          }
        />

        <HumanCard
          label="Destination"
          value={
            data?.destination?.ip ||
            data?.destination?.host ||
            "-"
          }
        />

        <HumanCard
          label="User"
          value={
            data?.user?.name ||
            data?.user?.username ||
            "-"
          }
        />

        <HumanCard
          label="Severity"
          value={
            data?.event?.severity ||
            "-"
          }
        />

      </div>


      <div className="json-label">
        NORMALIZED JSON
      </div>


      <pre className="normalized-json">
        {JSON.stringify(
          data,
          null,
          2
        )}
      </pre>


      {normalized.parserUsed && (

        <div className="parser-used">
          Parser:
          {" "}
          <strong>
            {normalized.parserUsed}
          </strong>
        </div>

      )}

    </div>
  );
}


function HumanCard({
  label,
  value,
}) {

  return (
    <div className="human-card">

      <span>
        {label}
      </span>

      <strong>
        {String(value)}
      </strong>

    </div>
  );
}


/* =========================================================
   UNKNOWN LOGS
========================================================= */

function UnknownLogs() {

  const navigate = useNavigate();

  const [logs, setLogs] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    loadUnknownLogs();
  }, []);

  async function loadUnknownLogs() {

    try {

      const response =
        await api.get(
          "/api/logs/unknown"
        );

      setLogs(
        Array.isArray(response.data)
          ? response.data
          : []
      );

    } catch (error) {

      console.error(error);

    } finally {

      setLoading(false);
    }
  }


  return (
    <Page>

      <PageHeader
        title="Unknown Log Review"
        subtitle="Onboard unsupported sources without creating Java parser classes"
      />


      <section className="panel">

        {loading ? (

          <Loading />

        ) : !logs.length ? (

          <EmptyState>
            No logs require review.
          </EmptyState>

        ) : (

          <div className="card-grid">

            {logs.map((log) => (

              <div
                key={log.id}
                className="unknown-card"
                onClick={() =>
                  navigate(
                    `/unknown/${log.id}`
                  )
                }
              >

                <div className="card-header">

                  <strong>
                    {log.sourceName}
                  </strong>

                  <Badge
                    value={
                      log.processingStatus
                    }
                  />

                </div>

                <code>
                  {log.id}
                </code>

                <div className="card-footer">

                  <span>
                    {log.detectedFormat}
                  </span>

                  <strong>
                    Analyze →
                  </strong>

                </div>

              </div>

            ))}

          </div>

        )}

      </section>

    </Page>
  );
}


/* =========================================================
   UNKNOWN ANALYSIS
========================================================= */

function UnknownAnalysis() {

  const { id } = useParams();
  const navigate = useNavigate();

  const [analysis, setAnalysis] =
    useState(null);

  const [loading, setLoading] =
    useState(true);

  const [parserName, setParserName] =
    useState("");

  const [mappings, setMappings] =
    useState({});

  const [approving, setApproving] =
    useState(false);


  useEffect(() => {
    loadAnalysis();
  }, [id]);


  async function loadAnalysis() {

    try {

      const response =
        await api.get(
          `/api/unknown-logs/${id}/analysis`
        );

      const data =
        response.data;

      setAnalysis(data);


      const prefix =
        data.suggestedSignaturePrefix
          ?.replace(
            /[^a-zA-Z0-9]/g,
            ""
          ) || "Custom";


      setParserName(
        `${prefix} Parser ${Date.now()}`
      );


      const initial = {};


      // ==================================================
      // AI SUGGESTIONS
      // ==================================================
      // AI fills fields that deterministic rules
      // do not already understand.
      //
      // We only auto-select predictions with
      // reasonable confidence.
      // ==================================================

      data.aiSuggestions
        ?.forEach((suggestion) => {

          const field =
            suggestion.source_field;

          const universalField =
            suggestion
              .suggested_universal_field;

          const confidence =
            suggestion.confidence || 0;


          if (
            field &&
            universalField &&
            confidence >= 0.70
          ) {

            initial[field] =
              universalField;

          }

        });


      // ==================================================
      // DETERMINISTIC SUGGESTIONS
      // ==================================================
      // Deterministic rules override AI suggestions.
      // This keeps known/high-confidence rules preferred.
      // ==================================================

      data.deterministicSuggestions
        ?.forEach((suggestion) => {

          initial[
            suggestion.sourceField
          ] =
            suggestion
              .suggestedUniversalField;

        });


      setMappings(initial);

    } catch (error) {

      console.error(error);

      alert(
        "Unable to analyze unknown log"
      );

    } finally {

      setLoading(false);

    }
  }


  function changeMapping(
    field,
    value
  ) {

    setMappings((previous) => ({
      ...previous,
      [field]: value,
    }));

  }


  function findAiSuggestion(field) {

    return analysis
      ?.aiSuggestions
      ?.find(
        (suggestion) =>
          suggestion.source_field === field
      );

  }


  function findDeterministicSuggestion(field) {

    return analysis
      ?.deterministicSuggestions
      ?.find(
        (suggestion) =>
          suggestion.sourceField === field
      );

  }


  async function approve() {

    setApproving(true);

    try {

      const cleanMappings =
        Object.fromEntries(
          Object.entries(mappings)
            .filter(
              ([, value]) =>
                value !== ""
            )
        );


      const response =
        await api.post(
          `/api/unknown-logs/${id}/approve`,
          {
            name: parserName,

            signaturePrefix:
              analysis
                .suggestedSignaturePrefix,

            delimiter:
              analysis
                .detectedDelimiter,

            keyValueSeparator:
              analysis
                .detectedKeyValueSeparator,

            fieldMappings:
              cleanMappings,

            enabled: true,
          }
        );


      const status =
        response.data
          ?.reprocessedLog
          ?.processingStatus;


      alert(
        `Parser created successfully. Status: ${
          status || "Processed"
        }`
      );


      navigate(
        `/logs/${id}`
      );

    } catch (error) {

      console.error(error);

      alert(
        error.response?.data?.message ||
        "Parser creation failed"
      );

    } finally {

      setApproving(false);

    }
  }


  if (loading) {

    return (
      <Page>
        <Loading />
      </Page>
    );

  }


  if (!analysis) {

    return (
      <Page>
        <EmptyState>
          Analysis unavailable.
        </EmptyState>
      </Page>
    );

  }


  return (
    <Page>

      <PageHeader
        title="Unknown Log Analyzer"
        subtitle="Review detected structure and approve reusable field mappings"
      />


      <section className="panel">

        <div className="section-label">
          ORIGINAL EVENT
        </div>

        <pre className="raw-log">
          {analysis.rawContent}
        </pre>

      </section>


      <div className="analysis-grid">

        <HumanCard
          label="Delimiter"
          value={
            analysis.detectedDelimiter
          }
        />

        <HumanCard
          label="KV Separator"
          value={
            analysis
              .detectedKeyValueSeparator
          }
        />

        <HumanCard
          label="Signature"
          value={
            analysis
              .suggestedSignaturePrefix
          }
        />

      </div>


      <section className="panel">

        <PanelHeader
          title="AI Assisted Field Mapping"
          subtitle="Deterministic rules and AI suggestions are combined before human approval"
        />


        <div
          style={{
            marginBottom: "18px",
            fontSize: "13px",
            opacity: 0.8,
          }}
        >
          AI Status:{" "}
          <strong>
            {analysis.aiStatus || "UNKNOWN"}
          </strong>
        </div>


        <div className="mapping-list">

          {Object.entries(
            analysis.extractedFields || {}
          )
            .filter(
              ([field]) =>
                !field.startsWith("token") ||
                field === "token1"
            )
            .map(
              ([field, value]) => {

                const aiSuggestion =
                  findAiSuggestion(field);

                const deterministicSuggestion =
                  findDeterministicSuggestion(
                    field
                  );

                return (

                  <div
                    className="mapping-row"
                    key={field}
                  >

                    <div>

                      <strong>
                        {field}
                      </strong>

                      <small>
                        {String(value)}
                      </small>


                      {deterministicSuggestion && (

                        <small
                          style={{
                            display: "block",
                            marginTop: "6px",
                          }}
                        >
                          Rule:{" "}
                          <strong>
                            {
                              deterministicSuggestion
                                .suggestedUniversalField
                            }
                          </strong>
                          {" · "}
                          {Math.round(
                            (
                              deterministicSuggestion
                                .confidence || 0
                            ) * 100
                          )}
                          %
                        </small>

                      )}


                      {aiSuggestion && (

                        <small
                          style={{
                            display: "block",
                            marginTop: "4px",
                          }}
                        >
                          AI:{" "}
                          <strong>
                            {
                              aiSuggestion
                                .suggested_universal_field
                            }
                          </strong>
                          {" · "}
                          {Math.round(
                            (
                              aiSuggestion
                                .confidence || 0
                            ) * 100
                          )}
                          %
                        </small>

                      )}

                    </div>


                    <span className="mapping-arrow">
                      →
                    </span>


                    <select
                      value={
                        mappings[field] || ""
                      }
                      onChange={(event) =>
                        changeMapping(
                          field,
                          event.target.value
                        )
                      }
                    >

                      <option value="">
                        Ignore
                      </option>

                      <option value="source_ip">
                        source_ip
                      </option>

                      <option value="destination_ip">
                        destination_ip
                      </option>

                      <option value="destination_host">
                        destination_host
                      </option>

                      <option value="source_port">
                        source_port
                      </option>

                      <option value="destination_port">
                        destination_port
                      </option>

                      <option value="username">
                        username
                      </option>

                      <option value="action">
                        action
                      </option>

                      <option value="severity">
                        severity
                      </option>

                      <option value="protocol">
                        protocol
                      </option>

                      <option value="process">
                        process
                      </option>

                      <option value="outcome">
                        outcome
                      </option>

                      <option value="host">
                        host
                      </option>

                      <option value="timestamp">
                        timestamp
                      </option>

                      <option value="message">
                        message
                      </option>

                    </select>

                  </div>

                );

              }
            )}

        </div>

      </section>


      <section className="panel">

        <label>
          Parser Name
        </label>

        <input
          value={parserName}
          onChange={(event) =>
            setParserName(
              event.target.value
            )
          }
        />


        <button
          className="primary-button"
          onClick={approve}
          disabled={approving}
        >

          {approving
            ? "Creating Parser..."
            : "Create Parser & Reprocess"
          }

        </button>

      </section>

    </Page>
  );
}

/* =========================================================
   PARSER REGISTRY
========================================================= */

function ParserRegistry() {

  const [parsers, setParsers] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    loadParsers();
  }, []);


  async function loadParsers() {

    try {

      const response =
        await api.get(
          "/api/parser-definitions"
        );

      setParsers(
        Array.isArray(response.data)
          ? response.data
          : []
      );

    } catch (error) {

      console.error(error);

    } finally {

      setLoading(false);
    }
  }


  return (
    <Page>

      <PageHeader
        title="Parser Registry"
        subtitle="Reusable dynamically onboarded source definitions"
      />


      <section className="panel">

        {loading ? (

          <Loading />

        ) : !parsers.length ? (

          <EmptyState>
            No dynamic parsers found.
          </EmptyState>

        ) : (

          <div className="card-grid">

            {parsers.map((parser) => (

              <div
                className="parser-card"
                key={parser.id}
              >

                <div className="card-header">

                  <strong>
                    {parser.name}
                  </strong>

                  <Badge
                    value={
                      parser.enabled
                        ? "ENABLED"
                        : "DISABLED"
                    }
                  />

                </div>


                <InfoRow
                  label="Signature"
                  value={
                    parser.signaturePrefix
                  }
                />

                <InfoRow
                  label="Delimiter"
                  value={
                    parser.delimiter
                  }
                />

                <InfoRow
                  label="KV Separator"
                  value={
                    parser.keyValueSeparator
                  }
                />

                <InfoRow
                  label="Created"
                  value={
                    formatDate(
                      parser.createdAt
                    )
                  }
                />

              </div>

            ))}

          </div>

        )}

      </section>

    </Page>
  );
}


/* =========================================================
   COMMON
========================================================= */

function Page({ children }) {

  return (
    <div className="page">
      {children}
    </div>
  );
}


function PageHeader({
  title,
  subtitle,
}) {

  return (
    <header className="page-header">

      <div>

        <h1>
          {title}
        </h1>

        <p>
          {subtitle}
        </p>

      </div>


      <div className="environment">

        <span className="green-dot" />

        LOCAL / AIR-GAPPED

      </div>

    </header>
  );
}


function PanelHeader({
  title,
  subtitle,
}) {

  return (
    <div className="panel-header">

      <div>

        <h2>
          {title}
        </h2>

        <p>
          {subtitle}
        </p>

      </div>

    </div>
  );
}


function InfoRow({
  label,
  value,
}) {

  return (
    <div className="info-row">

      <span>
        {label}
      </span>

      <div>
        {value || "-"}
      </div>

    </div>
  );
}


function Badge({ value }) {

  const text =
    value || "UNKNOWN";

  const className =
    text
      .toLowerCase()
      .replaceAll("_", "-");

  return (
    <span
      className={
        `badge badge-${className}`
      }
    >
      {text}
    </span>
  );
}


function EmptyState({
  children,
}) {

  return (
    <div className="empty-state">
      {children}
    </div>
  );
}


function Loading() {

  return (
    <div className="loading">
      Loading...
    </div>
  );
}


function formatDate(value) {

  if (!value) {
    return "-";
  }

  return new Date(
    value
  ).toLocaleString();
}