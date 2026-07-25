@echo off
setlocal
cd /d "%~dp0"

echo.
echo === PVZ2 line-ending setup ===
echo.

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo ERROR: Put this file in the repository root and run it there.
    exit /b 1
)

git diff --quiet
if errorlevel 1 (
    echo ERROR: Tracked working-tree changes exist.
    echo Commit or stash them before the one-time normalization.
    exit /b 1
)

git diff --cached --quiet
if errorlevel 1 (
    echo ERROR: Staged changes exist.
    echo Commit or unstage them before the one-time normalization.
    exit /b 1
)

echo Configuring Git for LF in this repository...
git config --local core.autocrlf false
git config --local core.eol lf
git config --local core.safecrlf warn

echo Staging policy files...
git add .gitattributes .editorconfig setup-line-endings.bat check-before-commit.bat

echo Renormalizing all tracked files using .gitattributes...
git add --renormalize .

echo Rewriting tracked working-tree files with configured line endings...
git restore --worktree -- .

echo Checking staged content...
git diff --cached --check
if errorlevel 1 (
    echo.
    echo ERROR: Git still found whitespace problems.
    echo Review the output above before committing.
    exit /b 1
)

echo.
echo SUCCESS: Line endings are normalized in the Git index.
echo Review with:
echo   git status --short
echo   git diff --cached --stat
echo.
echo Then commit once, for example:
echo   git commit -m "chore: enforce LF line endings"
echo.
exit /b 0
