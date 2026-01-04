# CVE Modules - Future Use

## Status

The CVE monitoring and scanning modules are **currently not aligned with the requirements** but are **kept in place for future implementation**.

## Current Implementation

The current CVE modules (`com.bugbounty.cve.*`) are designed to:
- Monitor the NVD (National Vulnerability Database) for new CVEs
- Scan Git repositories for existing CVEs in commits
- Match CVEs to programming languages
- Create CVE catalog entries

## Why This Doesn't Match Requirements

According to the requirements, the CVE/bug bounty system should:

1. **Scan for brand new CVEs** that have only just been reported (before they're widely known)
2. **Use tools like nuclei** to scan actual websites/endpoints for vulnerabilities
3. **Exploit the 23-day gap** between exploit publication and formal CVE disclosure
4. **Automated reconnaissance** of mid-tier crypto protocols or legacy API endpoints

The current implementation:
- ❌ Scans commits for **existing** CVEs (not new ones)
- ❌ Doesn't use nuclei or other scanning tools
- ❌ Doesn't scan actual websites/endpoints
- ❌ Focuses on Git repositories instead of live systems

## Future Implementation Plan

When implementing the CVE scanning system, we will need to:

1. **Nuclei Integration**
   - Integrate nuclei for vulnerability scanning
   - Create custom YAML templates for new CVEs
   - Scan newly discovered assets (subdomains, IPs)

2. **Asset Discovery**
   - Use subfinder, amass, shodan APIs
   - Track "seen" vs. "new" assets
   - Only scan newly discovered assets

3. **CVE Monitoring**
   - Monitor for brand new CVEs (within 24 hours of disclosure)
   - Use the existing `CVEMonitoringService` as a foundation
   - Trigger scans immediately when new CVEs are discovered

4. **Repository Scanning**
   - Keep the existing repository scanning infrastructure
   - But pivot to scanning for **new** CVEs, not existing ones
   - Use the CVE catalog to understand vulnerability patterns

## Current Modules (Kept for Future Use)

### CVEMonitoringService
- **Current:** Polls NVD for new CVEs and scans repositories
- **Future:** Will trigger nuclei scans when new CVEs are discovered

### RepositoryScanningService
- **Current:** Scans Git repos for vulnerable dependencies
- **Future:** May be used for dependency scanning, but primary focus will be on live endpoint scanning

### CommitAnalysisService
- **Current:** Analyzes commits for CVE patterns
- **Future:** May be repurposed or removed depending on final architecture

### NvdApiClient
- **Current:** Fetches CVEs from NVD
- **Future:** Will continue to be used for CVE monitoring

### LanguageMappingService
- **Current:** Maps CVEs to programming languages
- **Future:** Will be used to determine which scanning templates to use

## Migration Path

When ready to implement CVE scanning:

1. Keep the NVD monitoring infrastructure
2. Add nuclei integration layer
3. Add asset discovery services (subfinder, amass, etc.)
4. Create scanning orchestration service
5. Integrate with bug bounty platforms (HackerOne, Bugcrowd, etc.)
6. Add AI filtering for false positives

## References

See requirements documents:
- `docs/requirements/vulnerability bountry finder.md`
- `docs/requirements/fix pr bountry hunter.md`

