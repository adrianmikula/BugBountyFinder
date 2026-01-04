import React from 'react'

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
                  <td>
                    <a 
                      href={pr.repositoryUrl} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="link"
                    >
                      {pr.repositoryUrl?.split('/').pop() || pr.repositoryUrl}
                    </a>
                  </td>
                  <td>
                    <code>{pr.commitId?.substring(0, 8) || 'N/A'}</code>
                  </td>
                  <td>
                    <strong>{pr.cveId}</strong>
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

