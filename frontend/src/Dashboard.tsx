import { useEffect, useState } from 'react'

type ErrorCodeCount = {
  code: string
  count: number
}

type MinutePoint = {
  minute: string
  count: number
}

type MetricsSnapshot = {
  startedAt: string
  generatedAt: string
  totalRequests: number
  twoXx: number
  threeXx: number
  fourXx: number
  fiveXx: number
  errorCodes: ErrorCodeCount[]
  trafficByMinute: MinutePoint[]
}

const POLL_INTERVAL_MS = 5000

export default function Dashboard({ apiBase }: { apiBase: string }) {
  const [metrics, setMetrics] = useState<MetricsSnapshot | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const response = await fetch(`${apiBase}/api/observability/metrics`)
        if (!response.ok) throw new Error(`Request failed (${response.status})`)
        const data = (await response.json()) as MetricsSnapshot
        if (!cancelled) {
          setMetrics(data)
          setError('')
        }
      } catch (reason) {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : 'Could not load operations metrics')
        }
      }
    }

    load()
    const timer = window.setInterval(load, POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [apiBase])

  if (error) {
    return (
      <section className="dashboard-panel">
        <div className="error" role="alert">{error}</div>
      </section>
    )
  }

  if (!metrics) {
    return (
      <section className="dashboard-panel">
        <p className="empty">Loading operations dashboard...</p>
      </section>
    )
  }

  const sortedErrorCodes = [...metrics.errorCodes].sort((a, b) => b.count - a.count)
  const maxErrorCount = sortedErrorCodes[0]?.count ?? 0
  const maxTraffic = Math.max(1, ...metrics.trafficByMinute.map((point) => point.count))

  return (
    <section className="dashboard-panel">
      <p className="eyebrow">
        Live counters since {new Date(metrics.startedAt).toLocaleString()} · updates every 5s
      </p>

      <div className="metric-cards">
        <div className="metric-card">
          <span>Total requests</span>
          <strong>{metrics.totalRequests.toLocaleString()}</strong>
        </div>
        <div className="metric-card ok">
          <span>2xx success</span>
          <strong>{metrics.twoXx.toLocaleString()}</strong>
        </div>
        <div className="metric-card warn">
          <span>4xx client errors</span>
          <strong>{metrics.fourXx.toLocaleString()}</strong>
        </div>
        <div className="metric-card danger">
          <span>5xx server errors</span>
          <strong>{metrics.fiveXx.toLocaleString()}</strong>
        </div>
      </div>

      <div className="dashboard-columns">
        <div className="dashboard-card">
          <h3>Traffic — last {metrics.trafficByMinute.length} minutes</h3>
          <div className="traffic-chart" aria-label="Requests per minute">
            {metrics.trafficByMinute.map((point) => (
              <div
                key={point.minute}
                className="traffic-bar-wrap"
                title={`${new Date(point.minute).toLocaleTimeString()} — ${point.count} request${point.count === 1 ? '' : 's'}`}
              >
                <div
                  className="traffic-bar"
                  style={{ height: `${Math.max(2, (point.count / maxTraffic) * 100)}%` }}
                />
              </div>
            ))}
          </div>
        </div>

        <div className="dashboard-card">
          <h3>Error codes</h3>
          {sortedErrorCodes.length === 0 ? (
            <p className="empty">No errors recorded yet.</p>
          ) : (
            <ul className="error-code-list">
              {sortedErrorCodes.map((entry) => (
                <li key={entry.code}>
                  <span className="error-code">{entry.code}</span>
                  <div className="error-bar-track">
                    <div
                      className="error-bar"
                      style={{ width: `${(entry.count / maxErrorCount) * 100}%` }}
                    />
                  </div>
                  <span className="error-count">{entry.count}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <p className="dashboard-note">
        In-memory counters for this instance only — they cover application traffic (health
        checks are excluded), reset on restart or redeploy, and never include note titles,
        content, team names, or identities.
      </p>
    </section>
  )
}
