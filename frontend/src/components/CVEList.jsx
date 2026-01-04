import React, { useState, useEffect } from 'react'

function CVEList() {
  const [cves, setCves] = useState([])
  const [repositories, setRepositories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      // Fetch both repositories and CVEs
      const [reposRes, cvesRes] = await Promise.all([
        fetch('/api/repositories').catch(err => {
          console.error('Error fetching repositories:', err)
          return { ok: false, error: err }
        }),
        fetch('/api/cves').catch(err => {
          console.error('Error fetching CVEs:', err)
          return { ok: false, error: err }
        })
      ])

      let repos = []
      if (reposRes.ok) {
        repos = await reposRes.json()
        setRepositories(repos)
      } else {
        console.warn('Failed to fetch repositories, continuing with empty list')
      }

      if (!cvesRes.ok) {
        const errorText = cvesRes.status ? `HTTP ${cvesRes.status}` : 'Network error'
        console.error('Failed to fetch CVEs:', errorText, cvesRes)
        throw new Error(`Failed to fetch CVEs: ${errorText}`)
      }

      const allCVEs = await cvesRes.json().catch(err => {
        console.error('Error parsing CVE response:', err)
        throw new Error('Failed to parse CVE response')
      })
      
      // Get unique languages from repositories
      const repoLanguages = new Set()
      repos.forEach(repo => {
        if (repo.language) {
          repoLanguages.add(repo.language.toLowerCase())
        }
      })

      // Filter CVEs that match at least one repository language
      const matchingCVEs = allCVEs.filter(cve => {
        if (!cve.affectedLanguages || cve.affectedLanguages.length === 0) {
          return false
        }
        return cve.affectedLanguages.some(lang => 
          repoLanguages.has(lang.toLowerCase())
        )
      })

      // Sort by published date (newest first) and then by severity
      matchingCVEs.sort((a, b) => {
        // First sort by severity (CRITICAL > HIGH > MEDIUM > LOW)
        const severityOrder = { 'CRITICAL': 0, 'HIGH': 1, 'MEDIUM': 2, 'LOW': 3 }
        const severityDiff = (severityOrder[a.severity] || 99) - (severityOrder[b.severity] || 99)
        if (severityDiff !== 0) return severityDiff
        
        // Then by published date (newest first)
        if (a.publishedDate && b.publishedDate) {
          return new Date(b.publishedDate) - new Date(a.publishedDate)
        }
        return 0
      })

      setCves(matchingCVEs)
    } catch (err) {
      setError(err.message)
      console.error('Error fetching CVEs:', err)
    } finally {
      setLoading(false)
    }
  }

  const getSeverityColor = (severity) => {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL':
        return '#d32f2f'
      case 'HIGH':
        return '#f57c00'
      case 'MEDIUM':
        return '#fbc02d'
      case 'LOW':
        return '#388e3c'
      default:
        return '#666'
    }
  }

  const getSeverityBgColor = (severity) => {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL':
        return '#ffebee'
      case 'HIGH':
        return '#fff3e0'
      case 'MEDIUM':
        return '#fffde7'
      case 'LOW':
        return '#e8f5e9'
      default:
        return '#f5f5f5'
    }
  }

  if (loading) {
    return (
      <div className="section" style={{ fontSize: '0.75rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Relevant CVEs</h2>
        <div style={{ color: '#666' }}>Loading CVEs...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="section" style={{ fontSize: '0.75rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Relevant CVEs</h2>
        <div style={{ color: '#d32f2f' }}>
          <div style={{ marginBottom: '8px' }}>Error: {error}</div>
          {error.includes('404') || error.includes('Failed to fetch') ? (
            <div style={{ fontSize: '0.7rem', color: '#666', marginTop: '4px' }}>
              The CVE endpoint may not be available. Please restart the backend server to load the new CVEController.
            </div>
          ) : null}
        </div>
      </div>
    )
  }

  if (cves.length === 0) {
    return (
      <div className="section" style={{ fontSize: '0.75rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Relevant CVEs</h2>
        <div style={{ color: '#666' }}>No CVEs found matching watched repository languages.</div>
      </div>
    )
  }

  return (
    <div className="section" style={{ fontSize: '0.75rem' }}>
      <h2 style={{ fontSize: '1rem', marginBottom: '8px' }}>Relevant CVEs ({cves.length})</h2>
      <div style={{ 
        display: 'flex', 
        flexWrap: 'wrap', 
        gap: '6px',
        lineHeight: '1.4'
      }}>
        {cves.map((cve) => (
          <div
            key={cve.id}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '2px 6px',
              backgroundColor: getSeverityBgColor(cve.severity),
              borderRadius: '3px',
              border: `1px solid ${getSeverityColor(cve.severity)}`,
              flexWrap: 'wrap'
            }}
          >
            <span style={{ 
              fontSize: '0.65rem', 
              color: getSeverityColor(cve.severity),
              fontWeight: '700',
              textTransform: 'uppercase'
            }}>
              {cve.severity || 'UNKNOWN'}
            </span>
            <a
              href={`https://nvd.nist.gov/vuln/detail/${cve.cveId}`}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                fontSize: '0.75rem',
                textDecoration: 'none',
                color: '#667eea',
                fontWeight: '600'
              }}
              onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
              onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
            >
              {cve.cveId}
            </a>
            {cve.affectedLanguages && cve.affectedLanguages.length > 0 && (
              <span style={{ 
                fontSize: '0.65rem', 
                color: '#666',
                marginLeft: '2px'
              }}>
                ({cve.affectedLanguages.join(', ')})
              </span>
            )}
            {cve.cvssScore && (
              <span style={{ 
                fontSize: '0.65rem', 
                color: '#666',
                marginLeft: '2px'
              }}>
                CVSS: {cve.cvssScore.toFixed(1)}
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

export default CVEList

