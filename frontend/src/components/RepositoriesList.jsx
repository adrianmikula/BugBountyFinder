import React, { useState, useEffect } from 'react'

function RepositoriesList() {
  const [repositories, setRepositories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchRepositories()
  }, [])

  const fetchRepositories = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await fetch('/api/repositories')
      
      if (!response.ok) {
        throw new Error('Failed to fetch repositories')
      }
      
      const data = await response.json()
      setRepositories(data)
    } catch (err) {
      setError(err.message)
      console.error('Error fetching repositories:', err)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="section" style={{ fontSize: '0.75rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Monitored Repositories</h2>
        <div style={{ color: '#666' }}>Loading repositories...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="section" style={{ fontSize: '0.75rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Monitored Repositories</h2>
        <div style={{ color: '#d32f2f' }}>Error: {error}</div>
      </div>
    )
  }

  if (repositories.length === 0) {
    return (
      <div className="section" style={{ fontSize: '0.75rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Monitored Repositories</h2>
        <div style={{ color: '#666' }}>No repositories being monitored yet.</div>
      </div>
    )
  }

  return (
    <div className="section" style={{ fontSize: '0.75rem' }}>
      <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Monitored Repositories ({repositories.length})</h2>
      <div style={{ 
        display: 'flex', 
        flexWrap: 'wrap', 
        gap: '6px',
        lineHeight: '1.4'
      }}>
        {repositories.map((repo) => {
          const repoName = repo.url?.split('/').pop() || repo.url
          const owner = repo.url?.split('/').slice(-2)[0] || ''
          
          return (
            <div
              key={repo.id}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                padding: '2px 6px',
                backgroundColor: '#f5f5f5',
                borderRadius: '3px',
                border: '1px solid #ddd'
              }}
            >
              {repo.language && (
                <span style={{ 
                  fontSize: '0.7rem', 
                  color: '#666',
                  fontWeight: '600'
                }}>
                  {repo.language}
                </span>
              )}
              <a
                href={repo.url}
                target="_blank"
                rel="noopener noreferrer"
                className="link"
                style={{
                  fontSize: '0.75rem',
                  textDecoration: 'none',
                  color: '#667eea'
                }}
                onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
                onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
              >
                {owner}/{repoName}
              </a>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default RepositoriesList

