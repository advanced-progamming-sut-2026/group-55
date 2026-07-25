@echo off
setlocal
cd /d "%~dp0"

echo.
echo === PVZ2 pre-commit check ===
echo.

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo ERROR: Put this file in the repository root and run it there.
    exit /b 1
)

git config --local core.autocrlf false
git config --local core.eol lf
git config --local core.safecrlf warn

echo Checking unstaged changes...
git diff --check
if errorlevel 1 (
    echo.
    echo ERROR: Unstaged whitespace or line-ending problems exist.
    exit /b 1
)

echo Checking staged changes...
git diff --cached --check
if errorlevel 1 (
    echo.
    echo ERROR: Staged whitespace or line-ending problems exist.
    exit /b 1
)

echo.
echo SUCCESS: No Git whitespace or line-ending problems were found.
echo.
git status --short
exit /b 0
