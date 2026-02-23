@echo off
setlocal

echo [1/2] Running JUnit tests...
call .\mvnw.cmd test
if errorlevel 1 (
  echo [ERROR] Tests failed. Startup canceled.
  exit /b %errorlevel%
)

echo [2/2] Starting Spring Boot application...
call .\mvnw.cmd spring-boot:run
