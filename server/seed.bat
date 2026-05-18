@echo off
cd /d "%~dp0"
echo Seeding database...
call venv\Scripts\activate
python -m app.seeder
pause
