@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "APP_LAUNCHER=%SCRIPT_DIR%build\image\bin\Vic2-SGEA.bat"

if not exist "%APP_LAUNCHER%" (
    echo Runtime image not found. Building...
    call "%SCRIPT_DIR%gradlew.bat" jpackageImage
    if errorlevel 1 goto :build_failed
)

call "%APP_LAUNCHER%"
exit /b %errorlevel%

:build_failed
echo Build failed. See errors above.
pause
exit /b 1
