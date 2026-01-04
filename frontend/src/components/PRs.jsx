import React from 'react'
import Tooltip from './Tooltip'

function PRs({ prs }) {
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

  const formatConfidence = (confidence) => {
    if (confidence === null || confidence === undefined) {
      return 'N/A'
    }
    return `${(confidence * 100).toFixed(1)}%`
  }

  const getPRUrl = (repositoryUrl, prId) => {
    if (!repositoryUrl || !prId) return null
    // Convert repository URL to PR URL
    if (repositoryUrl.includes('github.com')) {
      return `${repositoryUrl}/pull/${prId}`
    }
    return null
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

  const renderCommitTooltip = (commitId, repositoryUrl) => {
    if (!commitId) return null
    
    return (
      <div>
        <div className="tooltip-title">Commit Details</div>
        <div className="tooltip-content">
          <div className="tooltip-item">
            <span className="tooltip-label">Full Commit ID:</span>
            <span className="tooltip-value">{commitId}</span>
          </div>
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

  const renderCVETooltip = (cveId, pr) => {
    if (!cveId) return null
    
    return (
      <div>
        <div className="tooltip-title">CVE Details</div>
        <div className="tooltip-content">
          <div className="tooltip-item">
            <span className="tooltip-label">CVE ID:</span>
            <span className="tooltip-value">{cveId}</span>
          </div>
          {pr && (
            <>
              {pr.presenceConfidence !== null && pr.presenceConfidence !== undefined && (
                <div className="tooltip-item">
                  <span className="tooltip-label">Presence Confidence:</span>
                  <span className="tooltip-value">
                    {(pr.presenceConfidence * 100).toFixed(1)}%
                  </span>
                </div>
              )}
              {pr.fixConfidence !== null && pr.fixConfidence !== undefined && (
                <div className="tooltip-item">
                  <span className="tooltip-label">Fix Confidence:</span>
                  <span className="tooltip-value">
                    {(pr.fixConfidence * 100).toFixed(1)}%
                  </span>
                </div>
              )}
              {pr.status && (
                <div className="tooltip-item">
                  <span className="tooltip-label">Status:</span>
                  <span className="tooltip-value">{pr.status}</span>
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

  return (
    <div className="section">
      <h2>Historical Pull Requests</h2>
      {prs.length === 0 ? (
        <div className="empty-state">
          <p>No pull requests have been created yet.</p>
        </div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Repository</th>
              <th>Commit</th>
              <th>CVE ID</th>
              <th>PR ID</th>
              <th>Status</th>
              <th>Presence Confidence</th>
              <th>Fix Confidence</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {prs.map((pr) => {
              const prUrl = getPRUrl(pr.repositoryUrl, pr.pullRequestId)
              return (
                <tr key={pr.id}>
                  <td style={{ position: 'relative', padding: '12px' }}>
                    <Tooltip 
                      content={renderRepositoryTooltip(pr.repositoryUrl)}
                      id={`pr-repo-tooltip-${pr.id}`}
                      className="stat-card"
                    >
                      <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
                        <a 
                          href={pr.repositoryUrl} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="link"
                        >
                          {pr.repositoryUrl?.split('/').pop() || pr.repositoryUrl}
                        </a>
                      </div>
                    </Tooltip>
                  </td>
                  <td style={{ position: 'relative', padding: '12px' }}>
                    <Tooltip 
                      content={renderCommitTooltip(pr.commitId, pr.repositoryUrl)}
                      id={`pr-commit-tooltip-${pr.id}`}
                      className="stat-card"
                    >
                      <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
                        <code>{pr.commitId?.substring(0, 8) || 'N/A'}</code>
                      </div>
                    </Tooltip>
                  </td>
                  <td style={{ position: 'relative', padding: '12px' }}>
                    <Tooltip 
                      content={renderCVETooltip(pr.cveId, pr)}
                      id={`pr-cve-tooltip-${pr.id}`}
                      className="stat-card"
                    >
                      <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
                        <strong>{pr.cveId}</strong>
                      </div>
                    </Tooltip>
                  </td>
                  <td>
                    {prUrl ? (
                      <a 
                        href={prUrl} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="link"
                      >
                        <code>{pr.pullRequestId}</code>
                      </a>
                    ) : (
                      <code>{pr.pullRequestId}</code>
                    )}
                  </td>
                  <td>{getStatusBadge(pr.status)}</td>
                  <td>{formatConfidence(pr.presenceConfidence)}</td>
                  <td>{formatConfidence(pr.fixConfidence)}</td>
                  <td>
                    {pr.createdAt ? new Date(pr.createdAt).toLocaleString() : 'N/A'}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default PRs

