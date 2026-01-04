# Bug Bounty Finder UI

A simple React web UI for monitoring and managing CVE bug findings, bounties, and pull requests.

## Features

- **Statistics Dashboard**: View real-time statistics including:
  - Number of repositories currently watched
  - Number of CVEs currently being tracked
  - Number of commits processed today

- **Bug Findings Review**: View bug findings that need human review:
  - Low confidence detections
  - Bugs verified as present but with low-confidence fixes

- **Claimed Bounties**: View all bounties that have been claimed or completed

- **Historical PRs**: View all pull requests that have been created for bug fixes

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- The backend Spring Boot application running on `http://localhost:8080`

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

The UI will be available at `http://localhost:3000`

### Building for Production

```bash
npm run build
```

The built files will be in the `dist` directory.

## API Endpoints

The frontend expects the following API endpoints to be available:

- `GET /api/statistics` - Get system statistics
- `GET /api/bug-findings/needs-review` - Get bug findings needing review
- `GET /api/bug-findings/low-confidence-fix` - Get low confidence fixes
- `GET /api/bounties/claimed` - Get claimed bounties
- `GET /api/prs/history` - Get historical PRs

## Development

The frontend uses:
- **React 18** for the UI framework
- **Vite** for fast development and building
- Modern ES6+ JavaScript

The UI automatically refreshes data every 30 seconds to keep information up-to-date.

