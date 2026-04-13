@echo off
REM git-commit.cmd - Commit all changes with Co-authored-by trailer
REM Usage: scripts\git-commit.cmd "Commit message"

setlocal enabledelayedexpansion

if "%~1"=="" (
  set "MSG=Apply recent backend changes"
) else (
  set "MSG=%~1"
)

:: Check git available
git --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo Git is not installed or not in PATH.
  pause
  exit /b 1
)

:: Ensure inside git repo (init if needed)
git rev-parse --is-inside-work-tree >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo Not a git repository. Initializing...
  git init || (echo git init failed & pause & exit /b 1)
)

echo --- git status (porcelain) ---
git status --porcelain
echo.
echo --- Unstaged changes ---
git diff --name-only
echo.
echo --- Staged files ---
git diff --cached --name-only
echo.

:: Set repo-local git user if missing
for /f "usebackq delims=" %%a in (`git config --get user.name 2^>nul`) do set "GITUSER=%%a"
if "%GITUSER%"=="" (
  git config user.name "Copilot"
  git config user.email "copilot@users.noreply.github.com"
  echo Set repo-local git user to Copilot.
)

echo --- git add --dry-run -v -A ---
git add --dry-run -v -A
echo.

:: Perform add
git add -A

:: If nothing staged, nothing to commit
git diff --cached --quiet
if %ERRORLEVEL% NEQ 0 (
  echo Committing with message: %MSG%
  git commit -m "%MSG%" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
  if %ERRORLEVEL% NEQ 0 (
    echo Commit failed.
    pause
    exit /b 1
  ) else (
    echo Commit succeeded. HEAD:
    git rev-parse --short HEAD
    git show --name-only --pretty=format:"" HEAD
  )
) else (
  echo No changes to commit.
)

pause
endlocal
