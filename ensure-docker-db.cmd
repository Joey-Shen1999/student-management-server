@echo off
setlocal enabledelayedexpansion

echo [Docker] Checking engine...
docker info >nul 2>&1
if errorlevel 1 (
  echo [Docker] Engine is not ready. Starting Docker Desktop...
  start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
  set /a retries=0
  :wait_docker
  ping -n 3 127.0.0.1 >nul
  docker info >nul 2>&1
  if not errorlevel 1 goto docker_ready
  set /a retries+=1
  if !retries! GEQ 45 (
    echo [ERROR] Docker Desktop did not become ready in time.
    exit /b 1
  )
  goto wait_docker
)

:docker_ready
echo [Docker] Starting database container...
docker compose up -d db
if errorlevel 1 (
  echo [ERROR] Failed to start Docker Compose service "db".
  exit /b %errorlevel%
)

echo [Docker] Database container is ready.
exit /b 0
