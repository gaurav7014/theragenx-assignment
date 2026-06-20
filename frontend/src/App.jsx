import { useState, useEffect } from 'react'
import './App.css'

const API_BASE = '/api/v1'
const CASE_ID = 'PV-2026-0451'

function getConfidenceLevel(confidence) {
  if (confidence < 0.80) return 'low'
  if (confidence <= 0.90) return 'medium'
  return 'high'
}

function ConfidenceBadge({ confidence }) {
  const level = getConfidenceLevel(confidence)
  return (
    <span className={`confidence-badge confidence-${level}`}>
      {(confidence * 100).toFixed(0)}%
    </span>
  )
}

function StatusPill({ status }) {
  const labels = {
    overridden: 'Overridden',
    new: 'New',
    missing_in_followup: 'Missing in follow-up',
    unchanged: 'Unchanged',
  }
  if (!status || status === 'unchanged') return null
  return <span className={`status-pill status-${status}`}>{labels[status] || status}</span>
}

function QueryModal({ caseId, fieldPath, onClose }) {
  const [question, setQuestion] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!question.trim()) return
    setSubmitting(true)
    fetch(`${API_BASE}/queries`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ caseId, fieldPath, question: question.trim() }),
    })
      .then(res => {
        if (!res.ok) throw new Error('Failed to submit query')
        setSuccess(true)
        setTimeout(onClose, 1200)
      })
      .catch(() => setSubmitting(false))
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Raise Query</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <p className="modal-field-path">{fieldPath}</p>
        {success ? (
          <div className="modal-success">Query submitted successfully</div>
        ) : (
          <form onSubmit={handleSubmit}>
            <textarea
              className="modal-textarea"
              placeholder="Describe the issue with this field..."
              value={question}
              onChange={e => setQuestion(e.target.value)}
              rows={4}
              autoFocus
            />
            <div className="modal-actions">
              <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
              <button type="submit" className="btn-submit" disabled={submitting || !question.trim()}>
                {submitting ? 'Submitting...' : 'Submit Query'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

function FieldCard({ fieldKey, field, sectionKey, caseId }) {
  const [showQuery, setShowQuery] = useState(false)
  const label = fieldKey.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
  const cardClass = [
    'field-card',
    field.status === 'overridden' && 'field-overridden',
    field.status === 'new' && 'field-new',
    field.status === 'missing_in_followup' && 'field-missing',
  ].filter(Boolean).join(' ')
  const fieldPath = `${sectionKey}.${fieldKey}`

  return (
    <div className={cardClass}>
      <div className="field-header">
        <span className="field-label">{label}</span>
        <div className="field-header-right">
          <StatusPill status={field.status} />
          <ConfidenceBadge confidence={field.confidence} />
        </div>
      </div>
      <div className="field-body">
        <div className="field-value">{field.value}</div>
        {field.status === 'overridden' && field.previous_value && (
          <div className="field-previous">
            <span className="previous-label">Was:</span>
            <span className="previous-value">{field.previous_value}</span>
          </div>
        )}
      </div>
      <div className="field-footer">
        <span className="field-source">{field.source}</span>
        {field.status === 'overridden' && (
          <button className="btn-raise-query" onClick={() => setShowQuery(true)}>Raise Query</button>
        )}
      </div>
      {showQuery && (
        <QueryModal caseId={caseId} fieldPath={fieldPath} onClose={() => setShowQuery(false)} />
      )}
    </div>
  )
}

function Section({ title, sectionKey, fields, caseId }) {
  if (!fields) return null
  return (
    <div className="section">
      <h2 className="section-title">{title}</h2>
      <div className="fields-grid">
        {Object.entries(fields).map(([key, field]) => (
          <FieldCard key={key} fieldKey={key} field={field} sectionKey={sectionKey} caseId={caseId} />
        ))}
      </div>
    </div>
  )
}

function App() {
  const [caseData, setCaseData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [classification, setClassification] = useState('non-significant')

  useEffect(() => {
    fetch(`${API_BASE}/cases/${CASE_ID}`)
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
        return res.json()
      })
      .then(data => {
        setCaseData(data)
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  if (loading) {
    return (
      <div className="app">
        <header className="app-header">
          <h1>PV Case Review</h1>
        </header>
        <main className="loading-state">
          <div className="spinner"></div>
          <p>Loading case data...</p>
        </main>
      </div>
    )
  }

  if (error) {
    return (
      <div className="app">
        <header className="app-header">
          <h1>PV Case Review</h1>
        </header>
        <main className="error-state">
          <p className="error-icon">⚠</p>
          <p className="error-message">Failed to load case</p>
          <p className="error-detail">{error}</p>
        </main>
      </div>
    )
  }

  const sections = caseData?.sections || {}

  return (
    <div className="app">
      <header className="app-header">
        <h1>PV Case Review</h1>
        <div className="case-meta">
          <span className="case-id">{caseData.case_id}</span>
          <span className="case-version">v{caseData.version}</span>
          <select
            className="classification-select"
            value={classification}
            onChange={e => setClassification(e.target.value)}
          >
            <option value="significant">Significant</option>
            <option value="non-significant">Non-significant</option>
            <option value="null">Null</option>
          </select>
        </div>
      </header>

      <main className="case-content">
        {caseData.missing_fields && caseData.missing_fields.length > 0 && (
          <div className="missing-fields-banner">
            <strong>Missing fields:</strong> {caseData.missing_fields.join(', ')}
          </div>
        )}

        <Section title="Patient" sectionKey="patient" fields={sections.patient} caseId={caseData.case_id} />
        <Section title="Suspect Drug" sectionKey="suspect_drug" fields={sections.suspect_drug} caseId={caseData.case_id} />
        <Section title="Adverse Event" sectionKey="adverse_event" fields={sections.adverse_event} caseId={caseData.case_id} />
        <Section title="Reporter" sectionKey="reporter" fields={sections.reporter} caseId={caseData.case_id} />
      </main>
    </div>
  )
}

export default App
