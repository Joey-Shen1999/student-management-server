@echo off
REM inspect-git.cmd - 查看当前仓库的待提交与已暂存文件

echo Repository: %CD%
echo.

:: 检查 git 是否可用
git --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo Git 未安装或不在 PATH 中。
  pause
  exit /b 1
)

:: 检查是否在 git 仓库
git rev-parse --is-inside-work-tree >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo 当前目录不是 git 仓库。请在项目根运行或先运行: git init
  pause
  exit /b 1
)

echo --- git status (porcelain) ---
git status --porcelain
echo.

echo --- 工作区修改（未暂存） ---
git --no-pager diff --name-only
echo.

echo --- 已暂存（staged）文件 ---
git diff --cached --name-only
echo.

echo --- git add --dry-run -v -A (将被添加的文件) ---
git add --dry-run -v -A
echo.

echo 说明:
echo - 若想取消暂存: git reset HEAD -- <file>
echo - 若要提交: git commit -m "Apply recent backend changes" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
echo.
pause
