import React from 'react'

function Statistics({ data }) {
  if (!data) {
    return (
      <div className="section">
        <h2>Statistics</h2>
        <div className="loading">Loading statistics...</div>
      </div>
    )
  }

  return (
    <div className="stats-grid">
      <div className="stat-card">
        <h3>{data.reposWatched || 0}</h3>
        <p>Repositories Watched</p>
      </div>
      <div className="stat-card">
        <h3>{data.cvesTracked || 0}</h3>
        <p>CVEs Tracked</p>
      </div>
      <div className="stat-card">
        <h3>{data.commitsProcessedToday || 0}</h3>
        <p>Commits Processed Today</p>
      </div>
    </div>
  )
}

export default Statistics

