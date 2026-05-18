@echo off
cd /d "%~dp0"
echo Starting DoshKa Server...
echo.
call venv\Scripts\activate
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
pause
