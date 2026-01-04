import React, { useState, useEffect } from 'react'
import Statistics from './components/Statistics'
import BugFindings from './components/BugFindings'
import Bounties from './components/Bounties'
import PRs from './components/PRs'
import './App.css'

function App() {
  const [statistics, setStatistics] = useState(null)
  const [bugFindings, setBugFindings] = useState([])
  const [lowConfidenceFixes, setLowConfidenceFixes] = useState([])
  const [bounties, setBounties] = useState([])
  const [prs, setPRs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchAllData()
    // Refresh data every 30 seconds
    const interval = setInterval(fetchAllData, 30000)
    return () => clearInterval(interval)
  }, [])

  const fetchAllData = async () => {
    try {
      setLoading(true)
      setError(null)

      const [statsRes, bugFindingsRes, lowConfidenceRes, bountiesRes, prsRes] = await Promise.all([
        fetch('/api/statistics').catch(err => ({ error: err })),
        fetch('/api/bug-findings/needs-review').catch(err => ({ error: err })),
        fetch('/api/bug-findings/low-confidence-fix').catch(err => ({ error: err })),
        fetch('/api/bounties/claimed').catch(err => ({ error: err })),
        fetch('/api/prs/history').catch(err => ({ error: err }))
      ])

      // Check for connection errors
      const hasConnectionError = [statsRes, bugFindingsRes, lowConfidenceRes, bountiesRes, prsRes]
        .some(res => res && res.error)
      
      if (hasConnectionError) {
        throw new Error('Cannot connect to backend API. Please ensure the Spring Boot application is running on http://localhost:8080')
      }

      if (!statsRes.ok || !bugFindingsRes.ok || !lowConfidenceRes.ok || !bountiesRes.ok || !prsRes.ok) {
        throw new Error('Failed to fetch data from backend')
      }

      const [stats, bugFindingsData, lowConfidenceData, bountiesData, prsData] = await Promise.all([
        statsRes.json(),
        bugFindingsRes.json(),
        lowConfidenceRes.json(),
        bountiesRes.json(),
        prsRes.json()
      ])

      setStatistics(stats)
      setBugFindings(bugFindingsData)
      setLowConfidenceFixes(lowConfidenceData)
      setBounties(bountiesData)
      setPRs(prsData)
    } catch (err) {
      const errorMessage = err.message || 'Unknown error occurred'
      setError(errorMessage)
      console.error('Error fetching data:', err)
    } finally {
      setLoading(false)
    }
  }

  if (loading && !statistics) {
    return (
      <div className="container">
        <div className="loading">Loading...</div>
      </div>
    )
  }

  return (
    <div className="container">
      <div className="header">
        <h1>Bug Bounty Finder</h1>
        <p>Monitor and manage CVE bug findings, bounties, and pull requests</p>
      </div>

      {error && (
        <div className="error">
          <strong>Connection Error:</strong> {error}
          <div style={{ marginTop: '10px', fontSize: '0.9rem' }}>
            <p>To start the backend, run:</p>
            <code style={{ display: 'block', padding: '10px', background: '#f0f0f0', borderRadius: '4px', marginTop: '5px' }}>
              ./gradlew bootRun
            </code>
            <p style={{ marginTop: '10px' }}>Or on Windows:</p>
            <code style={{ display: 'block', padding: '10px', background: '#f0f0f0', borderRadius: '4px', marginTop: '5px' }}>
              .\scripts\gradle-run.ps1
            </code>
          </div>
        </div>
      )}

      <Statistics data={statistics} />

      <BugFindings 
        findings={bugFindings} 
        lowConfidenceFixes={lowConfidenceFixes}
      />

      <Bounties bounties={bounties} />

      <PRs prs={prs} />
    </div>
  )
}

export default App

