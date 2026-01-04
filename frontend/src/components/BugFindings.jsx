import React from 'react'

function BugFindings({ findings, lowConfidenceFixes }) {
  const formatConfidence = (confidence) => {
    if (confidence === null || confidence === undefined) {
      return 'N/A'
    }
    return `${(confidence * 100).toFixed(1)}%`
  }

  const getConfidenceColor = (confidence) => {
    if (confidence === null || confidence === undefined) {
      return '#999'
    }
    if (confidence >= 0.8) return '#4caf50'
    if (confidence >= 0.5) return '#ff9800'
    return '#f44336'
  }

  const getStatusBadge = (status) => {
    const statusMap = {
      DETECTED: { class: 'badge-info', text: 'Detected' },
      VERIFIED: { class: 'badge-success', text: 'Verified' },
      FIX_GENERATED: { class: 'badge-info', text: 'Fix Generated' },
      FIX_CONFIRMED: { class: 'badge-success', text: 'Fix Confirmed' },
      HUMAN_REVIEW: { class: 'badge-warning', text: 'Human Review' },
      CONFIRMED: { class: 'badge-success', text: 'Confirmed' },
      REJECTED: { class: 'badge-danger', text: 'Rejected' },
      PR_CREATED: { class: 'badge-success', text: 'PR Created' },
      PR_MERGED: { class: 'badge-success', text: 'PR Merged' }
    }
    const statusInfo = statusMap[status] || { class: 'badge-info', text: status }
    return <span className={`badge ${statusInfo.class}`}>{statusInfo.text}</span>
  }

  const renderFindingRow = (finding) => (
    <tr key={finding.id}>
      <td>
        <a 
          href={finding.repositoryUrl} 
          target="_blank" 
          rel="noopener noreferrer"
          className="link"
        >
          {finding.repositoryUrl?.split('/').pop() || finding.repositoryUrl}
        </a>
      </td>
      <td>
        <code>{finding.commitId?.substring(0, 8) || 'N/A'}</code>
      </td>
      <td>
        <strong>{finding.cveId}</strong>
      </td>
      <td>{getStatusBadge(finding.status)}</td>
      <td>
        <div>
          {formatConfidence(finding.presenceConfidence)}
          <div className="confidence-bar">
            <div 
              className="confidence-fill"
              style={{
                width: `${(finding.presenceConfidence || 0) * 100}%`,
                backgroundColor: getConfidenceColor(finding.presenceConfidence)
              }}
            />
          </div>
        </div>
      </td>
      <td>
        <div>
          {formatConfidence(finding.fixConfidence)}
          <div className="confidence-bar">
            <div 
              className="confidence-fill"
              style={{
                width: `${(finding.fixConfidence || 0) * 100}%`,
                backgroundColor: getConfidenceColor(finding.fixConfidence)
              }}
            />
          </div>
        </div>
      </td>
      <td>
        {finding.requiresHumanReview ? (
          <span className="badge badge-warning">Yes</span>
        ) : (
          <span>No</span>
        )}
      </td>
      <td>
        {finding.createdAt ? new Date(finding.createdAt).toLocaleString() : 'N/A'}
      </td>
    </tr>
  )

  return (
    <>
      <div className="section">
        <h2>Bug Findings Needing Review</h2>
        {findings.length === 0 ? (
          <div className="empty-state">
            <p>No bug findings require review at this time.</p>
          </div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Repository</th>
                <th>Commit</th>
                <th>CVE ID</th>
                <th>Status</th>
                <th>Presence Confidence</th>
                <th>Fix Confidence</th>
                <th>Human Review</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {findings.map(renderFindingRow)}
            </tbody>
          </table>
        )}
      </div>

      <div className="section">
        <h2>Low Confidence Fixes</h2>
        <p style={{ marginBottom: '15px', color: '#666' }}>
          Bugs verified as present but with low-confidence fixes
        </p>
        {lowConfidenceFixes.length === 0 ? (
          <div className="empty-state">
            <p>No low-confidence fixes found.</p>
          </div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Repository</th>
                <th>Commit</th>
                <th>CVE ID</th>
                <th>Status</th>
                <th>Presence Confidence</th>
                <th>Fix Confidence</th>
                <th>Human Review</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {lowConfidenceFixes.map(renderFindingRow)}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}

export default BugFindings

