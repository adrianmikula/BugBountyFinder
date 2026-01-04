import React, { useState } from 'react'

function AddRepository({ onRepositoryAdded }) {
  const [url, setUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setSuccess(false)

    // Basic URL validation
    const githubUrlPattern = /^https:\/\/github\.com\/[^/]+\/[^/]+$/
    if (!githubUrlPattern.test(url)) {
      setError('Please enter a valid GitHub repository URL (e.g., https://github.com/owner/repo)')
      setLoading(false)
      return
    }

    try {
      const response = await fetch('/api/repositories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          url: url.trim(),
        }),
      })

      if (response.status === 409) {
        setError('This repository is already being monitored.')
        setLoading(false)
        return
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.message || 'Failed to add repository')
      }

      const data = await response.json()
      setSuccess(true)
      setUrl('')
      
      // Call callback to refresh repository list if provided
      if (onRepositoryAdded) {
        onRepositoryAdded()
      }

      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(false), 3000)
    } catch (err) {
      setError(err.message || 'An error occurred while adding the repository')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="section">
      <h2>Add Repository</h2>
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '10px', alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <input
            type="text"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://github.com/owner/repository"
            disabled={loading}
            style={{
              width: '100%',
              padding: '12px',
              fontSize: '1rem',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontFamily: 'inherit',
            }}
          />
          {error && (
            <div style={{ 
              color: '#721c24', 
              backgroundColor: '#f8d7da', 
              padding: '10px', 
              borderRadius: '4px', 
              marginTop: '10px',
              fontSize: '0.9rem'
            }}>
              {error}
            </div>
          )}
          {success && (
            <div style={{ 
              color: '#155724', 
              backgroundColor: '#d4edda', 
              padding: '10px', 
              borderRadius: '4px', 
              marginTop: '10px',
              fontSize: '0.9rem'
            }}>
              Repository added successfully!
            </div>
          )}
        </div>
        <button
          type="submit"
          disabled={loading || !url.trim()}
          style={{
            padding: '12px 24px',
            fontSize: '1rem',
            backgroundColor: loading ? '#ccc' : '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading || !url.trim() ? 'not-allowed' : 'pointer',
            fontWeight: '600',
            whiteSpace: 'nowrap',
            opacity: loading || !url.trim() ? 0.6 : 1,
          }}
        >
          {loading ? 'Adding...' : 'Add Repository'}
        </button>
      </form>
      <p style={{ 
        marginTop: '10px', 
        fontSize: '0.9rem', 
        color: '#666',
        fontStyle: 'italic'
      }}>
        Example: https://github.com/microsoft/vscode
      </p>
    </div>
  )
}

export default AddRepository

