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
        fetch('/api/statistics'),
        fetch('/api/bug-findings/needs-review'),
        fetch('/api/bug-findings/low-confidence-fix'),
        fetch('/api/bounties/claimed'),
        fetch('/api/prs/history')
      ])

      if (!statsRes.ok || !bugFindingsRes.ok || !lowConfidenceRes.ok || !bountiesRes.ok || !prsRes.ok) {
        throw new Error('Failed to fetch data')
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
      setError(err.message)
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
          <strong>Error:</strong> {error}
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

