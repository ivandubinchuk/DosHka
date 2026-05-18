@echo off
title DoshKa Server Tests

echo ========================================
echo     DoshKa Server Tests
echo ========================================
echo.

:: Check venv
if not exist "venv" (
    echo [ERROR] Virtual environment not found!
    echo Run start.bat first to create venv
    pause
    exit /b 1
)

:: Activate venv
call venv\Scripts\activate.bat

:: Install test dependencies
echo [INFO] Installing test dependencies...
pip install -r requirements-test.txt -q

:: Create reports folder
if not exist "reports" mkdir reports

:: Run tests
echo.
echo [INFO] Running tests...
echo.

pytest

echo.
echo ========================================
echo Test report: reports\test_report.html
echo Coverage report: reports\coverage\index.html
echo ========================================

:: Open report in browser
start "" "reports\test_report.html"

pause
