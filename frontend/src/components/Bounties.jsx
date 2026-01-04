import React from 'react'

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
                <td>
                  <strong>{bounty.title || 'N/A'}</strong>
                  {bounty.description && (
                    <div style={{ fontSize: '0.9rem', color: '#666', marginTop: '5px' }}>
                      {bounty.description.substring(0, 100)}
                      {bounty.description.length > 100 ? '...' : ''}
                    </div>
                  )}
                </td>
                <td>
                  <a 
                    href={bounty.repositoryUrl} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="link"
                  >
                    {bounty.repositoryUrl?.split('/').pop() || bounty.repositoryUrl}
                  </a>
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

