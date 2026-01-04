# Data Flows

## Bounty Discovery Flow

```
┌─────────────┐
│   Scheduler │
└──────┬──────┘
       │ (every 5 min)
       ▼
┌──────────────────┐
│ BountyPolling    │
│ Service          │
└──────┬───────────┘
       │
       ├─────────────────┬─────────────────┐
       ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Algora API   │  │  Polar API   │  │  (Future)   │
│ Client       │  │  Client      │  │  Other APIs │
└──────┬───────┘  └──────┬───────┘  └──────────────┘
       │                 │
       └─────────┬───────┘
                 │ (Flux<Bounty>)
                 ▼
        ┌────────────────┐
        │ Filter: New?   │
        │ Filter: Amount │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │ Save to DB     │
        │ (PostgreSQL)   │
        └────────┬───────┘
                 │
                 ▼
        ┌────────────────┐
        │ LLM Filtering │
        │ Service        │
        └────────┬───────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
   ┌─────────┐      ┌──────────┐
   │ Reject  │      │ Enqueue  │
   │ (Log)   │      │ (Redis)  │
   └─────────┘      └──────────┘
```

**Key Steps:**
1. Scheduled polling triggers `BountyPollingService`
2. Parallel API calls to Algora and Polar (reactive merge)
3. Filtering: deduplication (DB check) and minimum amount threshold
4. Persistence: save new bounties to PostgreSQL
5. LLM evaluation: `BountyFilteringService` uses Ollama to assess viability
6. Queueing: accepted bounties enqueued to Redis priority queue

## GitHub Webhook Flow

```
┌──────────────┐
│   GitHub     │
│  (Push Event)│
└──────┬───────┘
       │ HTTP POST
       ▼
┌──────────────────┐
│ GitHubWebhook    │
│ Controller       │
└──────┬───────────┘
       │
       ├─► Verify Signature
       │
       ▼
┌──────────────────┐
│ GitHubWebhook    │
│ Service           │
└──────┬───────────┘
       │
       ├─► Parse Repository URL
       │
       ▼
┌──────────────────┐
│ Repository      │
│ Service         │
└──────┬──────────┘
       │
       ├─► Check if Cloned
       │
       ├─► Clone (if new)
       │   OR
       └─► Pull Updates (if exists)
```

**Key Steps:**
1. GitHub sends push event to webhook endpoint
2. Signature verification ensures authenticity
3. Repository URL extracted and parsed
4. Repository service checks local clone status
5. Clones new repositories or updates existing ones

## CVE Monitoring Flow

```
┌─────────────┐
│   Scheduler │
└──────┬──────┘
       │ (every 1 hour)
       ▼
┌──────────────────┐
│ CVEMonitoring    │
│ Service          │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ NVD API Client   │
│ (Last 24h CVEs)  │
└──────┬───────────┘
       │
       ├─► Filter: CRITICAL/HIGH severity
       │
       ▼
┌──────────────────┐
│ Save to DB       │
│ (PostgreSQL)     │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Extract          │
│ Languages        │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Find Matching    │
│ Repositories     │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Repository       │
│ Scanning Service │
└──────┬───────────┘
       │
       ├─► Clone/Update Repo
       │
       └─► Scan Dependencies
           (pom.xml, package.json, etc.)
```

**Key Steps:**
1. Scheduled polling queries NVD API for recent CVEs
2. Filters by severity (CRITICAL/HIGH by default)
3. Saves new CVEs to database
4. Language extraction: maps CVE affected products to programming languages
5. Repository matching: finds repositories using affected languages
6. Scanning: clones/updates repos and scans dependency files

## Triage Queue Flow

```
┌──────────────────┐
│ Triage Queue     │
│ (Redis ZSet)     │
└──────┬───────────┘
       │
       │ Priority: Based on
       │ - Bounty amount
       │ - Platform
       │ - Time sensitivity
       │
       ▼
┌──────────────────┐
│ Dequeue          │
│ (Highest Priority)│
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Issue Triage     │
│ Service          │
│ (Future)         │
└──────────────────┘
       │
       ├─► Analyze Issue
       ├─► Locate Bug
       ├─► Generate Fix
       └─► Create PR
```

**Current State:**
- Bounties are enqueued with priority scores
- Priority calculation based on amount and platform
- Dequeue operations ready for triage processing (future implementation)

