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

export default function App() {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/ingest" element={<LogIngestion />} />
          <Route path="/logs" element={<LogExplorer />} />
          <Route path="/logs/:id" element={<LogDetails />} />
          <Route path="/unknown" element={<UnknownLogs />} />
          <Route path="/unknown/:id" element={<UnknownAnalysis />} />
          <Route path="/parsers" element={<ParserRegistry />} />
        </Routes>
      </main>
    </div>
  );
}

function Sidebar() {
  const items = [
    { path: "/", icon: "◈", label: "Dashboard" },
    { path: "/ingest", icon: "+", label: "Log Ingestion" },
    { path: "/logs", icon: "☷", label: "Log Explorer" },
    { path: "/unknown", icon: "?", label: "Unknown Logs" },
    { path: "/parsers", icon: "⚙", label: "Parser Registry" },
  ];

  return (
    <aside className="sidebar">
      <div className="brand">
        <img
          src="/logfusion-logo.jpeg"
          alt="LogFusion - Universal Log Pre-processing Framework"
          className="brand-logo-image"
        />
      </div>

      <div className="sidebar-label">SECURITY PLATFORM</div>

      <nav className="nav">
        {items.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === "/"}
            className={({ isActive }) =>
              isActive ? "nav-item active" : "nav-item"
            }
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-bottom">
        <div className="engine-status">
          <span className="status-pulse">
            <span className="green-dot" />
          </span>
          <div>
            <strong>Processing Engine</strong>
            <small>All systems operational</small>
          </div>
        </div>
      </div>
    </aside>
  );
}

function Dashboard() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadLogs();
  }, []);

  async function loadLogs() {
    try {
      const response = await api.get("/api/logs");
      setLogs(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error("Failed to load logs:", error);
    } finally {
      setLoading(false);
    }
  }

  const normalized = logs.filter(
    (log) => log.processingStatus === "NORMALIZED"
  ).length;

  const review = logs.filter(
    (log) => log.processingStatus === "NEEDS_REVIEW"
  ).length;

  const failed = logs.filter(
    (log) => log.processingStatus === "FAILED"
  ).length;

  const normalizationRate = logs.length
    ? Math.round((normalized / logs.length) * 100)
    : 0;

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION SECURITY OPERATIONS"
        title="Security Event Overview"
        subtitle="Unified visibility across heterogeneous log sources"
      />

      <div className="hero-strip">
        <div>
          <span className="hero-kicker">UNIVERSAL LOG PIPELINE</span>
          <h2>From raw telemetry to investigation-ready events.</h2>
          <p>
            LogFusion preserves, detects, parses, normalizes and investigates
            heterogeneous events through one extensible pipeline.
          </p>
        </div>

        <div className="hero-rate">
          <strong>{normalizationRate}%</strong>
          <span>normalization rate</span>
        </div>
      </div>

      <div className="stats-grid">
        <StatCard icon="◎" title="Total Events" value={logs.length} description="Raw forensic events" />
        <StatCard icon="✓" title="Normalized" value={normalized} description="Universal schema" tone="success" />
        <StatCard icon="?" title="Needs Review" value={review} description="Unknown formats" tone="warning" />
        <StatCard icon="!" title="Failed" value={failed} description="Processing failures" tone="danger" />
      </div>

      <section className="panel">
        <PanelHeader
          title="Recent Events"
          subtitle="Latest logs received by LogFusion"
        />
        {loading ? <Loading /> : <LogTable logs={logs.slice(0, 10)} />}
      </section>
    </Page>
  );
}

function StatCard({ icon, title, value, description, tone = "" }) {
  return (
    <div className={`stat-card ${tone}`}>
      <div className="stat-top">
        <div className="stat-icon">{icon}</div>
        <div className="stat-title">{title}</div>
      </div>
      <div className="stat-value">{value}</div>
      <div className="stat-description">{description}</div>
    </div>
  );
}

function LogIngestion() {
  const [rawContent, setRawContent] = useState("");
  const [sourceName, setSourceName] = useState("");
  const [sourceType, setSourceType] = useState("FIREWALL");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function submitLog(event) {
    event.preventDefault();
    setLoading(true);
    setResult(null);

    try {
      const response = await api.post("/api/logs/process", {
        rawContent,
        sourceName,
        sourceType,
      });
      setResult(response.data);
    } catch (error) {
      console.error(error);
      alert(error.response?.data?.message || "Log processing failed");
    } finally {
      setLoading(false);
    }
  }

  function loadExample() {
    setSourceName("orion-firewall-demo");
    setSourceType("FIREWALL");
    setRawContent(
      "ORION|ACCESS_ALLOW|CLIENT_IP=192.168.50.99|ACCOUNT=admin|TARGET_IP=10.0.0.20|DPT=22"
    );
  }

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION INGESTION"
        title="Log Ingestion"
        subtitle="Submit heterogeneous security telemetry into the preprocessing pipeline"
      />

      <div className="two-columns">
        <section className="panel ingest-panel">
          <div className="panel-header">
            <div>
              <div className="panel-title-row">
                <span className="panel-icon">+</span>
                <h2>Submit Event</h2>
              </div>
              <p>Original content is preserved before any parsing or normalization occurs.</p>
            </div>
            <button type="button" className="secondary-button" onClick={loadExample}>
              Load ORION Example
            </button>
          </div>

          <form onSubmit={submitLog}>
            <label>Source Name</label>
            <input
              value={sourceName}
              onChange={(event) => setSourceName(event.target.value)}
              placeholder="firewall-01"
              required
            />

            <label>Source Type</label>
            <select value={sourceType} onChange={(event) => setSourceType(event.target.value)}>
              <option value="FIREWALL">Firewall</option>
              <option value="LINUX">Linux</option>
              <option value="WINDOWS">Windows</option>
              <option value="APPLICATION">Application</option>
              <option value="NETWORK">Network Device</option>
              <option value="OTHER">Other</option>
            </select>

            <label>Raw Log</label>
            <textarea
              className="raw-input"
              value={rawContent}
              onChange={(event) => setRawContent(event.target.value)}
              placeholder="Paste raw log here..."
              required
            />

            <button className="primary-button" disabled={loading}>
              {loading ? "Processing..." : "Process Log"}
            </button>
          </form>
        </section>

        <section className="panel">
          <PanelHeader
            title="Processing Result"
            subtitle="Detection, integrity and processing metadata"
          />

          {!result ? (
            <EmptyState icon="◈" title="Awaiting event">
              Submit a log to inspect its detection and forensic metadata.
            </EmptyState>
          ) : (
            <div>
              <div className="result-banner">
                <div>
                  <span>EVENT ACCEPTED</span>
                  <strong>{result.sourceName}</strong>
                </div>
                <Badge value={result.processingStatus} />
              </div>

              <InfoRow label="Raw Log ID" value={result.id} />
              <InfoRow label="Detected Format" value={<Badge value={result.detectedFormat} />} />
              <InfoRow label="Status" value={<Badge value={result.processingStatus} />} />
              <InfoRow label="Source" value={result.sourceName} />
              <InfoRow label="Source Type" value={result.sourceType} />
              <InfoRow label="SHA-256" value={result.sha256Hash} />
              <InfoRow label="Received" value={formatDate(result.receivedAt)} />

              <button
                type="button"
                className="secondary-button full-button"
                onClick={() => navigate(`/logs/${result.id}`)}
              >
                Investigate Event →
              </button>
            </div>
          )}
        </section>
      </div>
    </Page>
  );
}

function LogExplorer() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    loadLogs();
  }, []);

  async function loadLogs() {
    try {
      const response = await api.get("/api/logs");
      setLogs(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  const filteredLogs = logs.filter((log) => {
    const query = search.toLowerCase();
    return (
      log.sourceName?.toLowerCase().includes(query) ||
      log.sourceType?.toLowerCase().includes(query) ||
      log.detectedFormat?.toLowerCase().includes(query) ||
      log.processingStatus?.toLowerCase().includes(query) ||
      log.id?.toLowerCase().includes(query)
    );
  });

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION INVESTIGATION"
        title="Log Explorer"
        subtitle="Investigate raw and normalized security events with complete traceability"
      />

      <section className="panel">
        <div className="explorer-toolbar">
          <div>
            <strong>Event Stream</strong>
            <span>{filteredLogs.length} events visible</span>
          </div>

          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search source, format, status or ID..."
          />
        </div>

        {loading ? <Loading /> : <LogTable logs={filteredLogs} />}
      </section>
    </Page>
  );
}

function LogTable({ logs }) {
  const navigate = useNavigate();

  if (!logs?.length) {
    return (
      <EmptyState icon="☷" title="No events found">
        No logs match the current view.
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
            <th />
          </tr>
        </thead>

        <tbody>
          {logs.map((log) => (
            <tr key={log.id} onClick={() => navigate(`/logs/${log.id}`)}>
              <td>{formatDate(log.receivedAt)}</td>
              <td>
                <div className="source-cell">
                  <span className="source-dot" />
                  <strong>{log.sourceName || "-"}</strong>
                </div>
              </td>
              <td>{log.sourceType || "-"}</td>
              <td><Badge value={log.detectedFormat} /></td>
              <td><Badge value={log.processingStatus} /></td>
              <td className="row-arrow">→</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function LogDetails() {
  const { id } = useParams();
  const [details, setDetails] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDetails();
  }, [id]);

  async function loadDetails() {
    try {
      const response = await api.get(`/api/logs/${id}`);
      setDetails(response.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return <Page><Loading /></Page>;
  }

  if (!details) {
    return (
      <Page>
        <EmptyState icon="!" title="Event unavailable">
          The requested log could not be loaded.
        </EmptyState>
      </Page>
    );
  }

  const raw = details.raw || details.rawLog || details;
  const normalized = details.normalized || details.normalizedLog || null;
  const rawContent = raw?.rawContent || raw?.content || "";

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION FORENSICS"
        title="Event Investigation"
        subtitle="Compare immutable source evidence with its universal normalized representation"
      />

      <div className="trace-bar">
        <div className="trace-main">
          <span>RAW LOG ID</span>
          <code>{raw?.id || id}</code>
        </div>
        <div>
          <span>FORMAT</span>
          <Badge value={raw?.detectedFormat} />
        </div>
        <div>
          <span>STATUS</span>
          <Badge value={raw?.processingStatus} />
        </div>
      </div>

      <div className="comparison-grid">
        <section className="comparison-panel raw-panel">
          <div className="comparison-header">
            <div>
              <div className="comparison-heading">ORIGINAL / RAW LOG</div>
              <div className="comparison-subtitle">Immutable forensic evidence</div>
            </div>
            <div className="evidence-chip">FORENSIC EVIDENCE</div>
          </div>

          <div className="code-window">
            <div className="code-window-bar">
              <div className="window-dots"><span /><span /><span /></div>
              <span>raw-event.log</span>
            </div>
            <pre className="raw-log">{rawContent || "Raw content unavailable"}</pre>
          </div>

          <div className="comparison-info">
            <InfoRow label="Source" value={raw?.sourceName} />
            <InfoRow label="Source Type" value={raw?.sourceType} />
            <InfoRow label="Detected Format" value={<Badge value={raw?.detectedFormat} />} />
            <InfoRow label="SHA-256" value={<code className="hash-value">{raw?.sha256Hash || "-"}</code>} />
            <InfoRow label="Received" value={formatDate(raw?.receivedAt)} />
          </div>
        </section>

        <section className="comparison-panel normalized-panel">
          <div className="comparison-header">
            <div>
              <div className="comparison-heading">NORMALIZED EVENT</div>
              <div className="comparison-subtitle">Human-readable universal schema</div>
            </div>
            <div className="universal-chip">UNIVERSAL SCHEMA</div>
          </div>

          {!normalized ? (
            <EmptyState icon="◇" title="Not normalized">
              This event does not yet have a normalized representation.
            </EmptyState>
          ) : (
            <NormalizedViewer normalized={normalized} />
          )}
        </section>
      </div>
    </Page>
  );
}

function NormalizedViewer({ normalized }) {
  let data = normalized.normalizedData || normalized.data || {};

  if (typeof data === "string") {
    try {
      data = JSON.parse(data);
    } catch {
      data = { message: data };
    }
  }

  const event = data?.event || {};
  const source = data?.source || {};
  const destination = data?.destination || {};
  const user = data?.user || {};
  const metadata = data?.metadata || {};

  return (
    <div className="normalized-viewer">
      <div className="normalized-summary">
        <div>
          <span>PARSER</span>
          <strong>{normalized.parserUsed || metadata.parserUsed || "Unknown"}</strong>
        </div>
        <div>
          <span>VALIDATION</span>
          <Badge value={normalized.validationStatus || "VALID"} />
        </div>
        <div>
          <span>PROCESSED</span>
          <strong>{formatDate(normalized.processedAt || metadata.processedAt)}</strong>
        </div>
      </div>

      <div className="human-grid">
        <HumanCard icon="⚡" label="Action" value={event.action || data.action || "-"} />
        <HumanCard icon="✓" label="Outcome" value={event.outcome || data.outcome || "-"} />
        <HumanCard icon="!" label="Severity" value={event.severity || data.severity || "-"} />
        <HumanCard icon="◈" label="Category" value={event.category || data.category || "-"} />
        <HumanCard icon="↗" label="Source IP" value={source.ip || source.address || data.source_ip || "-"} />
        <HumanCard icon="▣" label="Source Host" value={source.host || data.host || "-"} />
        <HumanCard icon="↘" label="Destination" value={destination.ip || destination.host || data.destination_ip || data.destination_host || "-"} />
        <HumanCard icon=":" label="Destination Port" value={destination.port ?? data.destination_port ?? "-"} />
        <HumanCard icon="●" label="User" value={user.name || user.username || data.username || "-"} />
      </div>

      <div className="json-section-header">
        <div>
          <span className="section-label">NORMALIZED JSON</span>
          <strong>Universal event representation</strong>
        </div>
        <span className="json-ready">JSON</span>
      </div>

      <div className="code-window normalized-code-window">
        <div className="code-window-bar">
          <div className="window-dots"><span /><span /><span /></div>
          <span>universal-event.json</span>
        </div>
        <pre className="normalized-json">{JSON.stringify(data, null, 2)}</pre>
      </div>
    </div>
  );
}

function HumanCard({ icon, label, value }) {
  return (
    <div className="human-card">
      <div className="human-card-top">
        <span className="human-icon">{icon}</span>
        <span>{label}</span>
      </div>
      <strong>
        {value === null || value === undefined || value === "" ? "-" : String(value)}
      </strong>
    </div>
  );
}

function UnknownLogs() {
  const navigate = useNavigate();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUnknownLogs();
  }, []);

  async function loadUnknownLogs() {
    try {
      const response = await api.get("/api/logs/unknown");
      setLogs(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION ADAPTIVE ONBOARDING"
        title="Unknown Log Review"
        subtitle="Analyze unsupported formats and convert them into reusable dynamic parsers"
      />

      <section className="panel">
        <PanelHeader
          title="Review Queue"
          subtitle="Events awaiting structure analysis and field mapping"
        />

        {loading ? (
          <Loading />
        ) : !logs.length ? (
          <EmptyState icon="✓" title="Review queue clear">
            No logs currently require analyst review.
          </EmptyState>
        ) : (
          <div className="card-grid">
            {logs.map((log) => (
              <div
                key={log.id}
                className="unknown-card"
                onClick={() => navigate(`/unknown/${log.id}`)}
              >
                <div className="card-header">
                  <div>
                    <span className="card-kicker">UNKNOWN SOURCE</span>
                    <strong>{log.sourceName}</strong>
                  </div>
                  <Badge value={log.processingStatus} />
                </div>
                <code>{log.id}</code>
                <div className="card-footer">
                  <span>{log.detectedFormat}</span>
                  <strong>Analyze →</strong>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </Page>
  );
}

function UnknownAnalysis() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);
  const [parserName, setParserName] = useState("");
  const [mappings, setMappings] = useState({});
  const [approving, setApproving] = useState(false);

  useEffect(() => {
    loadAnalysis();
  }, [id]);

  async function loadAnalysis() {
    try {
      const response = await api.get(`/api/unknown-logs/${id}/analysis`);
      const data = response.data;
      setAnalysis(data);

      const prefix = data.suggestedSignaturePrefix?.replace(/[^a-zA-Z0-9]/g, "") || "Custom";
      setParserName(`${prefix} Parser ${Date.now()}`);

      const initial = {};

      data.aiSuggestions?.forEach((suggestion) => {
        const field = suggestion.source_field;
        const universalField = suggestion.suggested_universal_field;
        const confidence = suggestion.confidence || 0;

        if (field && universalField && confidence >= 0.70) {
          initial[field] = universalField;
        }
      });

      data.deterministicSuggestions?.forEach((suggestion) => {
        initial[suggestion.sourceField] = suggestion.suggestedUniversalField;
      });

      setMappings(initial);
    } catch (error) {
      console.error(error);
      alert("Unable to analyze unknown log");
    } finally {
      setLoading(false);
    }
  }

  function changeMapping(field, value) {
    setMappings((previous) => ({ ...previous, [field]: value }));
  }

  function findAiSuggestion(field) {
    return analysis?.aiSuggestions?.find(
      (suggestion) => suggestion.source_field === field
    );
  }

  function findDeterministicSuggestion(field) {
    return analysis?.deterministicSuggestions?.find(
      (suggestion) => suggestion.sourceField === field
    );
  }

  async function approve() {
    setApproving(true);

    try {
      const cleanMappings = Object.fromEntries(
        Object.entries(mappings).filter(([, value]) => value !== "")
      );

      const response = await api.post(`/api/unknown-logs/${id}/approve`, {
        name: parserName,
        signaturePrefix: analysis.suggestedSignaturePrefix,
        delimiter: analysis.detectedDelimiter,
        keyValueSeparator: analysis.detectedKeyValueSeparator,
        fieldMappings: cleanMappings,
        enabled: true,
      });

      const status = response.data?.reprocessedLog?.processingStatus;
      alert(`Parser created successfully. Status: ${status || "Processed"}`);
      navigate(`/logs/${id}`);
    } catch (error) {
      console.error(error);
      alert(error.response?.data?.message || "Parser creation failed");
    } finally {
      setApproving(false);
    }
  }

  if (loading) {
    return <Page><Loading /></Page>;
  }

  if (!analysis) {
    return (
      <Page>
        <EmptyState icon="!" title="Analysis unavailable">
          Structure analysis could not be loaded.
        </EmptyState>
      </Page>
    );
  }

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION AI ASSISTED ONBOARDING"
        title="Unknown Log Analyzer"
        subtitle="Review detected structure, AI suggestions and reusable field mappings"
      />

      <section className="panel">
        <PanelHeader
          title="Original Event"
          subtitle="Immutable source content awaiting parser onboarding"
        />

        <div className="code-window">
          <div className="code-window-bar">
            <div className="window-dots"><span /><span /><span /></div>
            <span>unknown-event.log</span>
          </div>
          <pre className="raw-log">{analysis.rawContent}</pre>
        </div>
      </section>

      <div className="analysis-grid">
        <HumanCard icon="|" label="Delimiter" value={analysis.detectedDelimiter} />
        <HumanCard icon="=" label="KV Separator" value={analysis.detectedKeyValueSeparator} />
        <HumanCard icon="◈" label="Signature" value={analysis.suggestedSignaturePrefix} />
      </div>

      <section className="panel">
        <div className="mapping-header">
          <PanelHeader
            title="AI Assisted Field Mapping"
            subtitle="AI fills gaps, deterministic rules take priority, analyst keeps final control"
          />

          <div className={`ai-status ${analysis.aiStatus === "AVAILABLE" ? "online" : ""}`}>
            <span className="green-dot" />
            AI {analysis.aiStatus || "UNKNOWN"}
          </div>
        </div>

        <div className="mapping-list">
          {Object.entries(analysis.extractedFields || {})
            .filter(([field]) => !field.startsWith("token") || field === "token1")
            .map(([field, value]) => {
              const aiSuggestion = findAiSuggestion(field);
              const deterministicSuggestion = findDeterministicSuggestion(field);

              return (
                <div className="mapping-row" key={field}>
                  <div className="mapping-source">
                    <strong>{field}</strong>
                    <code>{String(value)}</code>

                    <div className="suggestion-stack">
                      {deterministicSuggestion && (
                        <span className="suggestion-chip rule">
                          RULE <strong>{deterministicSuggestion.suggestedUniversalField}</strong>
                          {Math.round((deterministicSuggestion.confidence || 0) * 100)}%
                        </span>
                      )}

                      {aiSuggestion && (
                        <span className="suggestion-chip ai">
                          AI <strong>{aiSuggestion.suggested_universal_field}</strong>
                          {Math.round((aiSuggestion.confidence || 0) * 100)}%
                        </span>
                      )}
                    </div>
                  </div>

                  <span className="mapping-arrow">→</span>

                  <select
                    value={mappings[field] || ""}
                    onChange={(event) => changeMapping(field, event.target.value)}
                  >
                    <option value="">Ignore</option>
                    <option value="source_ip">source_ip</option>
                    <option value="destination_ip">destination_ip</option>
                    <option value="destination_host">destination_host</option>
                    <option value="source_port">source_port</option>
                    <option value="destination_port">destination_port</option>
                    <option value="username">username</option>
                    <option value="action">action</option>
                    <option value="severity">severity</option>
                    <option value="protocol">protocol</option>
                    <option value="process">process</option>
                    <option value="outcome">outcome</option>
                    <option value="host">host</option>
                    <option value="timestamp">timestamp</option>
                    <option value="message">message</option>
                  </select>
                </div>
              );
            })}
        </div>
      </section>

      <section className="panel parser-create-panel">
        <div>
          <span className="card-kicker">DYNAMIC PARSER</span>
          <h2>Register reusable parser</h2>
          <p>Future events matching this signature can be normalized automatically.</p>
        </div>

        <div className="parser-create-form">
          <input value={parserName} onChange={(event) => setParserName(event.target.value)} />
          <button className="primary-button" onClick={approve} disabled={approving}>
            {approving ? "Creating Parser..." : "Create Parser & Reprocess"}
          </button>
        </div>
      </section>
    </Page>
  );
}

function ParserRegistry() {
  const [parsers, setParsers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadParsers();
  }, []);

  async function loadParsers() {
    try {
      const response = await api.get("/api/parser-definitions");
      setParsers(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Page>
      <PageHeader
        eyebrow="LOGFUSION PARSER INTELLIGENCE"
        title="Parser Registry"
        subtitle="Reusable source definitions dynamically onboarded without backend code changes"
      />

      <section className="panel">
        <PanelHeader
          title="Registered Parsers"
          subtitle={`${parsers.length} dynamic parser definitions available`}
        />

        {loading ? (
          <Loading />
        ) : !parsers.length ? (
          <EmptyState icon="⚙" title="No dynamic parsers">
            Approve an unknown event to register the first reusable parser definition.
          </EmptyState>
        ) : (
          <div className="card-grid">
            {parsers.map((parser) => (
              <div className="parser-card" key={parser.id}>
                <div className="card-header">
                  <div>
                    <span className="card-kicker">DYNAMIC PARSER</span>
                    <strong>{parser.name}</strong>
                  </div>
                  <Badge value={parser.enabled ? "ENABLED" : "DISABLED"} />
                </div>

                <InfoRow label="Signature" value={<code>{parser.signaturePrefix}</code>} />
                <InfoRow label="Delimiter" value={parser.delimiter} />
                <InfoRow label="KV Separator" value={parser.keyValueSeparator} />
                <InfoRow label="Created" value={formatDate(parser.createdAt)} />
              </div>
            ))}
          </div>
        )}
      </section>
    </Page>
  );
}

function Page({ children }) {
  return <div className="page">{children}</div>;
}

function PageHeader({ eyebrow, title, subtitle }) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <div className="page-eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>

      <div className="environment">
        <span className="green-dot" />
        LOGFUSION / AIR-GAP READY
      </div>
    </header>
  );
}

function PanelHeader({ title, subtitle }) {
  return (
    <div className="panel-header">
      <div>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
    </div>
  );
}

function InfoRow({ label, value }) {
  const empty = value === null || value === undefined || value === "";
  return (
    <div className="info-row">
      <span>{label}</span>
      <div>{empty ? "-" : value}</div>
    </div>
  );
}

function Badge({ value }) {
  const text = value || "UNKNOWN";
  const className = String(text).toLowerCase().replaceAll("_", "-");
  return <span className={`badge badge-${className}`}>{text}</span>;
}

function EmptyState({ icon = "◇", title, children }) {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      {title && <strong>{title}</strong>}
      <span>{children}</span>
    </div>
  );
}

function Loading() {
  return (
    <div className="loading">
      <span className="loader-ring" />
      <span>Loading security telemetry...</span>
    </div>
  );
}

function formatDate(value) {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);

  return date.toLocaleString();
}
