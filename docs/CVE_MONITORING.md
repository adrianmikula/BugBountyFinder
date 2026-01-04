# CVE Monitoring and Repository Scanning

This document describes the CVE (Common Vulnerabilities and Exposures) monitoring system that automatically detects new security vulnerabilities and scans repositories using affected programming languages.

## Overview

The CVE monitoring system provides:
1. **Automatic CVE Polling**: Scheduled polling of the National Vulnerability Database (NVD) API for new CVEs
2. **Webhook Support**: Accepts CVE notifications from external services (anyCVE, TrackCVE, CVEWatch, etc.)
3. **Language Mapping**: Intelligently maps CVE affected products to programming languages
4. **Repository Scanning**: Automatically scans repositories when CVEs affecting their languages are detected

## Architecture

### Components

1. **CVE Domain Model** (`com.bugbounty.cve.domain.CVE`)
   - Represents a CVE with ID, description, severity, CVSS score, affected languages/products

2. **NVD API Client** (`NvdApiClient`, `NvdApiClientImpl`)
   - Polls the National Vulnerability Database API for new CVEs
   - Supports filtering by severity and date range
   - Handles pagination and rate limiting (NVD allows 5 requests per 30 seconds)

3. **CVE Monitoring Service** (`CVEMonitoringService`)
   - Scheduled task that polls NVD API hourly (configurable)
   - Processes new CVEs and triggers repository scans
   - Handles webhook notifications

4. **Language Mapping Service** (`LanguageMappingService`)
   - Maps CVE affected products (e.g., "Spring Framework") to programming languages (e.g., "Java")
   - Supports common frameworks and libraries across multiple languages

5. **Repository Scanning Service** (`RepositoryScanningService`)
   - Clones/updates repositories when CVEs are detected
   - Scans dependency files (pom.xml, package.json, requirements.txt, etc.)
   - Language-specific scanning logic

6. **CVE Webhook Controller** (`CVEWebhookController`)
   - REST endpoint: `POST /api/webhooks/cve`
   - Accepts CVE notifications from external services
   - Supports flexible JSON payload formats

## Configuration

### Application Configuration (`application.yml`)

```yaml
app:
  cve:
    monitoring:
      enabled: true
      poll-interval-ms: 3600000  # 1 hour (default)
      min-severity: HIGH  # CRITICAL, HIGH, MEDIUM, LOW
    nvd:
      api-base-url: https://services.nvd.nist.gov/rest/json
      api-key: ${NVD_API_KEY:}  # Optional: Get free API key from https://nvd.nist.gov/developers/request-an-api-key
      rate-limit-delay-ms: 6000  # 6 seconds between requests (NVD rate limit)
```

### Environment Variables

- `NVD_API_KEY`: Optional NVD API key for higher rate limits (free from https://nvd.nist.gov/developers/request-an-api-key)

## Usage

### Automatic Polling

The system automatically polls NVD API for new CVEs on a scheduled interval (default: 1 hour). Only CRITICAL and HIGH severity CVEs are processed by default.

To change the polling interval or severity threshold, update `application.yml`:

```yaml
app:
  cve:
    monitoring:
      poll-interval-ms: 1800000  # 30 minutes
      min-severity: CRITICAL  # Only CRITICAL CVEs
```

### Webhook Integration

#### Setting Up External CVE Monitoring Services

1. **anyCVE** (https://anycve.com)
   - Configure webhook URL: `https://your-domain.com/api/webhooks/cve`
   - Set up monitoring for vendors/products relevant to your repositories

2. **TrackCVE** (https://trackcve.com)
   - Add webhook endpoint in settings
   - Configure keyword-based notifications

3. **CVEWatch** (https://cvewatch.carrd.co)
   - Set up webhook or Telegram notifications
   - Configure for your technology stack

#### Webhook Payload Format

The webhook endpoint accepts flexible JSON formats. Example:

```json
{
  "cveId": "CVE-2024-1234",
  "description": "Vulnerability in Spring Framework",
  "severity": "CRITICAL",
  "cvssScore": 9.8,
  "publishedDate": "2024-01-01T00:00:00",
  "affectedLanguages": ["Java"],
  "affectedProducts": ["Spring Framework", "Spring Boot"]
}
```

Alternative formats supported:
- `cve_id` instead of `cveId`
- `cvss_severity` instead of `severity`
- `published_date` instead of `publishedDate`

### Repository Setup

Repositories must be stored in the database with their programming language specified. The system will automatically scan repositories when CVEs affecting their language are detected.

Example repository entity:
```java
RepositoryEntity repo = RepositoryEntity.builder()
    .url("https://github.com/owner/repo")
    .language("Java")
    .build();
```

## Language Mapping

The system automatically maps CVE affected products to programming languages:

### Supported Mappings

- **Java**: Spring Framework, Spring Boot, Apache Log4j, Apache Struts, Jackson, Hibernate, Maven, Gradle
- **Python**: Django, Flask, FastAPI, NumPy, Pandas, Requests
- **JavaScript/TypeScript**: Node.js, Express, React, Vue, Angular, npm, Yarn, Webpack
- **C#**: ASP.NET, Entity Framework, NuGet
- **Go**: Golang, Go modules
- **Ruby**: Rails, Bundler
- **PHP**: Laravel, Symfony, Composer
- **Rust**: Rust, Cargo
- **C/C++**: OpenSSL, libcurl

### Adding Custom Mappings

Edit `LanguageMappingService.java` to add custom product-to-language mappings:

```java
Map.entry("your-product", Arrays.asList("YourLanguage"))
```

## Repository Scanning

When a CVE is detected that affects a repository's language, the system:

1. Clones the repository (if not already cloned)
2. Updates the repository (if already cloned)
3. Scans dependency files for vulnerable versions:
   - **Java**: `pom.xml`, `build.gradle`, `build.gradle.kts`
   - **Python**: `requirements.txt`, `pyproject.toml`, `setup.py`
   - **JavaScript**: `package.json`, `package-lock.json`, `yarn.lock`
   - **Ruby**: `Gemfile`, `Gemfile.lock`
   - **PHP**: `composer.json`, `composer.lock`
   - **Go**: `go.mod`, `go.sum`
   - **Rust**: `Cargo.toml`, `Cargo.lock`

### Extending Scanning Logic

The current implementation provides a framework for scanning. To add actual vulnerability detection:

1. Integrate with vulnerability scanning tools (e.g., OWASP Dependency-Check, Snyk, Trivy)
2. Parse dependency files and check versions against CVE database
3. Store scan results in database
4. Generate reports or alerts

## API Endpoints

### CVE Webhook
- **POST** `/api/webhooks/cve`
  - Accepts CVE notifications from external services
  - Returns 200 OK on success

### Health Check
- **GET** `/api/webhooks/cve/health`
  - Returns "CVE webhook endpoint is active"

## Monitoring and Logging

The system logs:
- CVE polling events
- New CVE detections
- Repository scan triggers
- Webhook notifications
- Errors and exceptions

Log levels are configured in `application.yml`:
```yaml
logging:
  level:
    com.bugbounty.cve: DEBUG
```

## Performance Considerations

1. **Rate Limiting**: NVD API allows 5 requests per 30 seconds. The system includes built-in delays.
2. **Pagination**: Large CVE queries are automatically paginated.
3. **Concurrency**: Repository scans run asynchronously using virtual threads (Project Loom).
4. **Database**: CVEs are stored to avoid duplicate processing.

## Future Enhancements

- [ ] Integration with actual vulnerability scanning tools (OWASP Dependency-Check, Snyk)
- [ ] CVE severity-based prioritization
- [ ] Email/Slack notifications for critical CVEs
- [ ] Dashboard for viewing CVE scan results
- [ ] Support for additional CVE sources (GitHub Security Advisories, OSV)
- [ ] Automated dependency update suggestions

## Troubleshooting

### CVEs Not Being Detected

1. Check if CVE monitoring is enabled: `app.cve.monitoring.enabled: true`
2. Verify NVD API connectivity
3. Check logs for errors
4. Ensure repositories have language specified

### Webhook Not Working

1. Verify endpoint is accessible: `GET /api/webhooks/cve/health`
2. Check webhook payload format matches expected structure
3. Review logs for parsing errors

### Repository Scans Not Triggering

1. Verify repository language matches CVE affected languages
2. Check language mapping service mappings
3. Ensure repositories are stored in database with language field

## References

- [NVD API Documentation](https://nvd.nist.gov/developers/vulnerabilities)
- [NVD API Key Request](https://nvd.nist.gov/developers/request-an-api-key)
- [CVE Database](https://cve.mitre.org/)

