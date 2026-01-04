# Starting the Backend

The React frontend requires the Spring Boot backend to be running on `http://localhost:8080`.

## Quick Start

### Windows (PowerShell)
```powershell
.\scripts\gradle-run.ps1
```

### Linux/Mac
```bash
./gradlew bootRun
```

### Using Mise (if configured)
```bash
mise run run
```

## Prerequisites

Before starting the backend, ensure:

1. **Docker services are running** (PostgreSQL and Redis):
   ```bash
   docker-compose up -d
   ```

2. **Database is set up** - The application will use Liquibase to manage the database schema automatically.

3. **Environment variables** (optional):
   - `DB_USERNAME` (default: postgres)
   - `DB_PASSWORD` (default: postgres)
   - `REDIS_HOST` (default: localhost)
   - `REDIS_PORT` (default: 6379)

## Verifying the Backend is Running

Once started, you should see:
- Spring Boot banner in the console
- Application logs showing the server starting
- No errors about database connection

You can also verify by visiting:
- `http://localhost:8080/api/statistics` - Should return JSON statistics
- `http://localhost:8080/actuator/health` - Should return health status

## Troubleshooting

### Port 8080 Already in Use
If port 8080 is already in use, you can change it in `src/main/resources/application.yml`:
```yaml
server:
  port: 8081
```

Then update `frontend/vite.config.js` to proxy to the new port.

### Database Connection Errors
- Ensure PostgreSQL is running: `docker ps`
- Check database credentials in `application.yml`
- Verify Docker Compose services: `docker-compose ps`

### CORS Errors
The backend is configured to allow CORS from `http://localhost:3000` and `http://localhost:5173`. 
If you're using a different port, update `src/main/java/com/bugbounty/web/config/WebConfig.java`.

