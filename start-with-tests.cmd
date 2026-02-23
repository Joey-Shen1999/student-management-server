@echo off
setlocal

echo [0/3] Ensuring Docker is available...
docker info >nul 2>&1
if errorlevel 1 (
  echo [INFO] Docker is not ready. Starting Docker Desktop...
  start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
  set /a retries=0
  :wait_docker
  docker info >nul 2>&1
  if not errorlevel 1 goto docker_ready
  set /a retries+=1
  if %retries% GEQ 30 (
    echo [ERROR] Docker Desktop did not become ready in time.
    exit /b 1
  )
  timeout /t 2 /nobreak >nul
  goto wait_docker
)

:docker_ready
echo [1/3] Starting PostgreSQL container...
docker compose up -d db
if errorlevel 1 (
  echo [ERROR] Failed to start Docker Compose service "db".
  exit /b %errorlevel%
)

echo [2/3] Running JUnit tests...
call .\mvnw.cmd test
if errorlevel 1 (
  echo [ERROR] Tests failed. Startup canceled.
  exit /b %errorlevel%
)

echo [3/3] Starting Spring Boot application...
call .\mvnw.cmd spring-boot:run
