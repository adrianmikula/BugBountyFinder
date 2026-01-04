import React from 'react'
import Tooltip from './Tooltip'

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

  const renderRepositoryTooltip = (repositoryUrl) => {
    if (!repositoryUrl) return null
    const parts = repositoryUrl.split('/')
    const owner = parts[parts.length - 2]
    const repo = parts[parts.length - 1]
    
    return (
      <div>
        <div className="tooltip-title">Repository Details</div>
        <div className="tooltip-content">
          <div className="tooltip-item">
            <span className="tooltip-label">Full URL:</span>
            <span className="tooltip-value">{repositoryUrl}</span>
          </div>
          <div className="tooltip-item">
            <span className="tooltip-label">Owner:</span>
            <span className="tooltip-value">{owner}</span>
          </div>
          <div className="tooltip-item">
            <span className="tooltip-label">Repository:</span>
            <span className="tooltip-value">{repo}</span>
          </div>
        </div>
      </div>
    )
  }

  const renderCommitTooltip = (commitId, affectedFiles, repositoryUrl) => {
    if (!commitId) return null
    
    return (
      <div>
        <div className="tooltip-title">Commit Details</div>
        <div className="tooltip-content">
          <div className="tooltip-item">
            <span className="tooltip-label">Full Commit ID:</span>
            <span className="tooltip-value">{commitId}</span>
          </div>
          {affectedFiles && affectedFiles.length > 0 && (
            <div className="tooltip-item">
              <span className="tooltip-label">Affected Files:</span>
              <div style={{ marginTop: '4px' }}>
                {affectedFiles.slice(0, 5).map((file, idx) => (
                  <div key={idx} style={{ fontSize: '0.85rem', color: '#ccc', marginTop: '2px' }}>
                    • {file}
                  </div>
                ))}
                {affectedFiles.length > 5 && (
                  <div style={{ fontSize: '0.85rem', color: '#999', marginTop: '2px', fontStyle: 'italic' }}>
                    +{affectedFiles.length - 5} more
                  </div>
                )}
              </div>
            </div>
          )}
          {repositoryUrl && (
            <div className="tooltip-item" style={{ marginTop: '8px' }}>
              <a 
                href={`${repositoryUrl}/commit/${commitId}`}
                target="_blank"
                rel="noopener noreferrer"
                className="tooltip-link"
                onClick={(e) => e.stopPropagation()}
              >
                View on GitHub →
              </a>
            </div>
          )}
        </div>
      </div>
    )
  }

  const renderCVETooltip = (cveId, finding) => {
    if (!cveId) return null
    
    return (
      <div>
        <div className="tooltip-title">CVE Details</div>
        <div className="tooltip-content">
          <div className="tooltip-item">
            <span className="tooltip-label">CVE ID:</span>
            <span className="tooltip-value">{cveId}</span>
          </div>
          {finding && (
            <>
              {finding.presenceConfidence !== null && finding.presenceConfidence !== undefined && (
                <div className="tooltip-item">
                  <span className="tooltip-label">Presence Confidence:</span>
                  <span className="tooltip-value">
                    {(finding.presenceConfidence * 100).toFixed(1)}%
                  </span>
                </div>
              )}
              {finding.fixConfidence !== null && finding.fixConfidence !== undefined && (
                <div className="tooltip-item">
                  <span className="tooltip-label">Fix Confidence:</span>
                  <span className="tooltip-value">
                    {(finding.fixConfidence * 100).toFixed(1)}%
                  </span>
                </div>
              )}
              {finding.status && (
                <div className="tooltip-item">
                  <span className="tooltip-label">Status:</span>
                  <span className="tooltip-value">{finding.status}</span>
                </div>
              )}
            </>
          )}
          <div className="tooltip-item" style={{ marginTop: '8px' }}>
            <a 
              href={`https://nvd.nist.gov/vuln/detail/${cveId}`}
              target="_blank"
              rel="noopener noreferrer"
              className="tooltip-link"
              onClick={(e) => e.stopPropagation()}
            >
              View on NVD →
            </a>
          </div>
        </div>
      </div>
    )
  }

  const renderFindingRow = (finding) => (
    <tr key={finding.id}>
      <td style={{ position: 'relative', padding: '12px' }}>
        <Tooltip 
          content={renderRepositoryTooltip(finding.repositoryUrl)}
          id={`repo-tooltip-${finding.id}`}
          className="stat-card"
        >
          <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
            <a 
              href={finding.repositoryUrl} 
              target="_blank" 
              rel="noopener noreferrer"
              className="link"
            >
              {finding.repositoryUrl?.split('/').pop() || finding.repositoryUrl}
            </a>
          </div>
        </Tooltip>
      </td>
      <td style={{ position: 'relative', padding: '12px' }}>
        <Tooltip 
          content={renderCommitTooltip(finding.commitId, finding.affectedFiles, finding.repositoryUrl)}
          id={`commit-tooltip-${finding.id}`}
          className="stat-card"
        >
          <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
            <code>{finding.commitId?.substring(0, 8) || 'N/A'}</code>
          </div>
        </Tooltip>
      </td>
      <td style={{ position: 'relative', padding: '12px' }}>
        <Tooltip 
          content={renderCVETooltip(finding.cveId, finding)}
          id={`cve-tooltip-${finding.id}`}
          className="stat-card"
        >
          <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
            <strong>{finding.cveId}</strong>
          </div>
        </Tooltip>
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

