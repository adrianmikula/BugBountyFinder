import React from 'react'
import Tooltip from './Tooltip'

function Bounties({ bounties }) {
  const getStatusBadge = (status) => {
    const statusMap = {
      OPEN: { class: 'badge-info', text: 'Open' },
      IN_PROGRESS: { class: 'badge-warning', text: 'In Progress' },
      COMPLETED: { class: 'badge-success', text: 'Completed' },
      CLAIMED: { class: 'badge-success', text: 'Claimed' },
      FAILED: { class: 'badge-danger', text: 'Failed' },
      EXPIRED: { class: 'badge-danger', text: 'Expired' }
    }
    const statusInfo = statusMap[status] || { class: 'badge-info', text: status }
    return <span className={`badge ${statusInfo.class}`}>{statusInfo.text}</span>
  }

  const formatAmount = (amount, currency) => {
    if (!amount) return 'N/A'
    const currencySymbol = currency === 'USD' ? '$' : currency || ''
    return `${currencySymbol}${amount.toFixed(2)}`
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

  const renderBountyTooltip = (bounty) => {
    return (
      <div>
        <div className="tooltip-title">Bounty Details</div>
        <div className="tooltip-content">
          {bounty.title && (
            <div className="tooltip-item">
              <span className="tooltip-label">Title:</span>
              <span className="tooltip-value">{bounty.title}</span>
            </div>
          )}
          {bounty.description && (
            <div className="tooltip-item">
              <span className="tooltip-label">Description:</span>
              <div style={{ marginTop: '4px', fontSize: '0.85rem', color: '#ccc' }}>
                {bounty.description.length > 200 
                  ? bounty.description.substring(0, 200) + '...'
                  : bounty.description}
              </div>
            </div>
          )}
          {bounty.amount && (
            <div className="tooltip-item">
              <span className="tooltip-label">Amount:</span>
              <span className="tooltip-value">
                {formatAmount(bounty.amount, bounty.currency)}
              </span>
            </div>
          )}
          {bounty.platform && (
            <div className="tooltip-item">
              <span className="tooltip-label">Platform:</span>
              <span className="tooltip-value">{bounty.platform}</span>
            </div>
          )}
          {bounty.status && (
            <div className="tooltip-item">
              <span className="tooltip-label">Status:</span>
              <span className="tooltip-value">{bounty.status}</span>
            </div>
          )}
          {bounty.createdAt && (
            <div className="tooltip-item">
              <span className="tooltip-label">Created:</span>
              <span className="tooltip-value">
                {new Date(bounty.createdAt).toLocaleString()}
              </span>
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="section">
      <h2>Claimed Bounties</h2>
      {bounties.length === 0 ? (
        <div className="empty-state">
          <p>No claimed bounties found.</p>
        </div>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Repository</th>
              <th>Platform</th>
              <th>Amount</th>
              <th>Status</th>
              <th>PR ID</th>
              <th>Created</th>
              <th>Completed</th>
            </tr>
          </thead>
          <tbody>
            {bounties.map((bounty) => (
              <tr key={bounty.id}>
                <td style={{ position: 'relative', padding: '12px' }}>
                  <Tooltip 
                    content={renderBountyTooltip(bounty)}
                    id={`bounty-tooltip-${bounty.id}`}
                    className="stat-card"
                  >
                    <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
                      <strong>{bounty.title || 'N/A'}</strong>
                      {bounty.description && (
                        <div style={{ fontSize: '0.9rem', color: '#666', marginTop: '5px' }}>
                          {bounty.description.substring(0, 100)}
                          {bounty.description.length > 100 ? '...' : ''}
                        </div>
                      )}
                    </div>
                  </Tooltip>
                </td>
                <td style={{ position: 'relative', padding: '12px' }}>
                  <Tooltip 
                    content={renderRepositoryTooltip(bounty.repositoryUrl)}
                    id={`bounty-repo-tooltip-${bounty.id}`}
                    className="stat-card"
                  >
                    <div style={{ padding: '4px 0', minHeight: '24px', width: '100%' }}>
                      <a 
                        href={bounty.repositoryUrl} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="link"
                      >
                        {bounty.repositoryUrl?.split('/').pop() || bounty.repositoryUrl}
                      </a>
                    </div>
                  </Tooltip>
                </td>
                <td>
                  <span className="badge badge-info">{bounty.platform || 'N/A'}</span>
                </td>
                <td>
                  <strong>{formatAmount(bounty.amount, bounty.currency)}</strong>
                </td>
                <td>{getStatusBadge(bounty.status)}</td>
                <td>
                  {bounty.pullRequestId ? (
                    <code>{bounty.pullRequestId}</code>
                  ) : (
                    'N/A'
                  )}
                </td>
                <td>
                  {bounty.createdAt ? new Date(bounty.createdAt).toLocaleString() : 'N/A'}
                </td>
                <td>
                  {bounty.completedAt ? new Date(bounty.completedAt).toLocaleString() : 'N/A'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default Bounties

