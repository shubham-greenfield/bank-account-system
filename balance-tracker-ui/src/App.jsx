import { useState, useEffect } from "react";

const ACCOUNT_ID_LABEL = "ACC-00123456";
const API_URL = "http://localhost:8081/balance";
const POLL_INTERVAL_MS = 2000;

function formatCurrency(value) {
  if (value === null) return "—";
  return new Intl.NumberFormat("en-GB", {
    style: "currency",
    currency: "GBP",
    minimumFractionDigits: 2,
  }).format(value);
}

export default function App() {
  const [balance, setBalance] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchBalance = async () => {
      try {
        const res = await fetch(API_URL);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        setBalance(data.balance);
        setLastUpdated(new Date());
        setError(null);
      } catch (err) {
        setError("Unable to reach Balance Tracker — is it running?");
      }
    };

    fetchBalance();
    const interval = setInterval(fetchBalance, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, []);

  const isNegative = balance !== null && balance < 0;

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <h1 style={styles.heading}>Account Overview</h1>

        <div style={styles.field}>
          <label style={styles.label}>Bank Account Number</label>
          <div style={styles.value}>{ACCOUNT_ID_LABEL}</div>
        </div>

        <div style={styles.field}>
          <label style={styles.label}>Balance</label>
          <div
            style={{
              ...styles.balance,
              color: isNegative ? "#c0392b" : "#1a6b3a",
            }}
          >
            {formatCurrency(balance)}
          </div>
        </div>

        {error && <div style={styles.error}>{error}</div>}

        {lastUpdated && (
          <div style={styles.timestamp}>
            Last updated: {lastUpdated.toLocaleTimeString("en-GB")}
          </div>
        )}

        <div style={styles.indicator}>
          <span style={styles.dot} />
          Refreshing every {POLL_INTERVAL_MS / 1000}s
        </div>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background: "#f4f6f8",
    fontFamily: "'Segoe UI', sans-serif",
  },
  card: {
    background: "#ffffff",
    borderRadius: 12,
    padding: "40px 48px",
    boxShadow: "0 4px 24px rgba(0,0,0,0.08)",
    minWidth: 360,
  },
  heading: {
    fontSize: 22,
    fontWeight: 600,
    color: "#1a1a2e",
    marginBottom: 32,
  },
  field: {
    marginBottom: 24,
  },
  label: {
    display: "block",
    fontSize: 12,
    fontWeight: 600,
    color: "#6b7280",
    textTransform: "uppercase",
    letterSpacing: "0.08em",
    marginBottom: 6,
  },
  value: {
    fontSize: 18,
    color: "#111827",
    fontWeight: 500,
  },
  balance: {
    fontSize: 36,
    fontWeight: 700,
    letterSpacing: "-0.5px",
  },
  error: {
    background: "#fef2f2",
    border: "1px solid #fca5a5",
    borderRadius: 8,
    padding: "10px 14px",
    color: "#b91c1c",
    fontSize: 14,
    marginTop: 16,
  },
  timestamp: {
    fontSize: 13,
    color: "#9ca3af",
    marginTop: 24,
  },
  indicator: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    fontSize: 12,
    color: "#9ca3af",
    marginTop: 8,
  },
  dot: {
    display: "inline-block",
    width: 8,
    height: 8,
    borderRadius: "50%",
    background: "#22c55e",
    animation: "pulse 2s infinite",
  },
};