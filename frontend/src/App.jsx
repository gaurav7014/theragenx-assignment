import { useState, useEffect, useMemo } from 'react'
import './App.css'

function confidenceLevel(score) {
  if (score < 0.80) return 'low'
  if (score <= 0.90) return 'medium'
  return 'high'
}

function formatName(key) {
  return key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function FieldCard({ name, field, onRaiseQuery }) {
  const level = confidenceLevel(field.confidence)
  const isOverridden = field.status === 'overridden'

  return (
    <div className={`field-card ${isOverridden ? 'field-card-conflict' : ''}`}>
      <div className="field-header">
        <span className="field-name">{formatName(name)}</span>
        {field.status && field.status !== 'unchanged' && (
          <span className={`status-pill status-${field.status}`}>{field.status}</span>
        )}
      </div>

      {isOverridden && field.previous_value ? (
        <div className="conflict-view">
          <div className="conflict-new">
            <span className="conflict-label">Current</span>
            <span className="conflict-value">{field.value}</span>
          </div>
          <div className="conflict-previous">
            <span className="conflict-label">Previous</span>
            <span className="conflict-value">{field.previous_value}</span>
          </div>
        </div>
      ) : (
        <div className="field-value">{field.value}</div>
      )}

      <div className="field-meta">
        <span className={`confidence confidence-${level}`}>
          {Math.round(field.confidence * 100)}%
        </span>
        <span className="source">{field.source}</span>
      </div>

      {isOverridden && (
        <button className="raise-query-btn" onClick={() => onRaiseQuery(name)}>
          Raise Query
        </button>
      )}
    </div>
  )
}

function Section({ name, fields, sortByConfidence, filterConflicts, onRaiseQuery }) {
  const processedFields = useMemo(() => {
    let entries = Object.entries(fields)

    if (filterConflicts) {
      entries = entries.filter(([, f]) => f.status === 'overridden')
    }

    if (sortByConfidence) {
      entries.sort((a, b) => a[1].confidence - b[1].confidence)
    }

    return entries
  }, [fields, sortByConfidence, filterConflicts])

  if (processedFields.length === 0) return null

  return (
    <section className="case-section">
      <h2 className="section-title">{formatName(name)}</h2>
      <div className="fields-grid">
        {processedFields.map(([fieldName, fieldData]) => (
          <FieldCard
            key={fieldName}
            name={fieldName}
            field={fieldData}
            onRaiseQuery={(fName) => onRaiseQuery(name, fName)}
          />
        ))}
      </div>
    </section>
  )
}

function QueryModal({ caseId, sectionName, fieldName, onClose }) {
  const [question, setQuestion] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [submitError, setSubmitError] = useState(null)

  const fieldPath = `${sectionName}.${fieldName}`

  function handleSubmit(e) {
    e.preventDefault()
    if (!question.trim()) return

    setSubmitting(true)
    setSubmitError(null)
    fetch('/api/v1/queries', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ case_id: caseId, field_path: fieldPath, question: question.trim() }),
    })
      .then(res => {
        if (!res.ok) return res.text().then(body => { throw new Error(body || `HTTP ${res.status}`) })
        setSubmitted(true)
      })
      .catch(err => setSubmitError(err.message || 'Failed to submit query'))
      .finally(() => setSubmitting(false))
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Raise Query</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <p className="modal-field-path">{formatName(sectionName)} &rarr; {formatName(fieldName)}</p>

        {submitted ? (
          <div className="modal-success">Query submitted successfully.</div>
        ) : (
          <form onSubmit={handleSubmit}>
            <textarea
              className="modal-textarea"
              placeholder="Describe the issue or question about this field..."
              value={question}
              onChange={e => setQuestion(e.target.value)}
              rows={4}
              autoFocus
            />
            {submitError && <div className="modal-error">{submitError}</div>}
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={submitting || !question.trim()}>
                {submitting ? 'Submitting...' : 'Submit Query'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

function App() {
  const [caseData, setCaseData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [classification, setClassification] = useState(null)
  const [sortByConfidence, setSortByConfidence] = useState(false)
  const [filterConflicts, setFilterConflicts] = useState(false)
  const [queryTarget, setQueryTarget] = useState(null)

  useEffect(() => {
    fetch('/api/v1/cases/PV-2026-0451')
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => {
        setCaseData(data)
        setClassification(data.case_classification || null)
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  if (loading) return <div className="state-message">Loading case...</div>
  if (error) return <div className="state-message error">Failed to load case: {error}</div>

  return (
    <div className="app">
      <header className="case-header">
        <div className="header-top">
          <h1>{caseData.case_id}</h1>
          <select
            className="classification-select"
            value={classification || ''}
            onChange={e => setClassification(e.target.value || null)}
          >
            <option value="">Null</option>
            <option value="significant">Significant</option>
            <option value="non-significant">Non-significant</option>
          </select>
        </div>
        <div className="header-meta">
          <span>Version {caseData.version}</span>
          <span>Extracted {new Date(caseData.extracted_at).toLocaleDateString()}</span>
          <span>{caseData.source_document}</span>
        </div>
      </header>

      {caseData.missing_fields && caseData.missing_fields.length > 0 && (
        <div className="missing-fields-banner">
          Missing fields: {caseData.missing_fields.join(', ')}
        </div>
      )}

      <div className="toolbar">
        <label className="toolbar-toggle">
          <input
            type="checkbox"
            checked={sortByConfidence}
            onChange={e => setSortByConfidence(e.target.checked)}
          />
          Sort by confidence (low first)
        </label>
        <label className="toolbar-toggle">
          <input
            type="checkbox"
            checked={filterConflicts}
            onChange={e => setFilterConflicts(e.target.checked)}
          />
          Show conflicts only
        </label>
      </div>

      <main className="sections">
        {Object.entries(caseData.sections).map(([sectionName, fields]) => (
          <Section
            key={sectionName}
            name={sectionName}
            fields={fields}
            sortByConfidence={sortByConfidence}
            filterConflicts={filterConflicts}
            onRaiseQuery={(section, field) => setQueryTarget({ section, field })}
          />
        ))}
      </main>

      {queryTarget && (
        <QueryModal
          caseId={caseData.case_id}
          sectionName={queryTarget.section}
          fieldName={queryTarget.field}
          onClose={() => setQueryTarget(null)}
        />
      )}
    </div>
  )
}

export default App
