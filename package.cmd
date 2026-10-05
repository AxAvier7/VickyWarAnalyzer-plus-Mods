@echo off
setlocal EnableDelayedExpansion
set "VERSION=1.2.1"
set "RELEASE=%~dp0release"
set "STAGE=%RELEASE%\VickyWarAnalyzer-%VERSION%-win64"
set "ZIP=%RELEASE%\VickyWarAnalyzer-%VERSION%-win64.zip"
set "JAR=%~dp0target\vickywaranalyzer.jar"
set "EXE=%~dp0target\VickyWarAnalyzer.exe"

set "NOPAUSE=1"
call "%~dp0build.cmd"
if errorlevel 1 exit /b 1

if not exist "%RELEASE%" mkdir "%RELEASE%"
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"

copy /y "%EXE%" "%STAGE%\VickyWarAnalyzer.exe" > nul
copy /y "%JAR%" "%STAGE%\vickywaranalyzer.jar" > nul
copy /y "%~dp0src\launcher\README-pack.txt" "%STAGE%\README.txt" > nul
if not exist "%STAGE%\vickywaranalyzer.jar" goto failed

echo Copying the runtime, this takes a moment...
mkdir "%STAGE%\jre"
robocopy "%~dp0tools\jdk8fx\jre" "%STAGE%\jre" /e /nfl /ndl /njh /njs /np > nul
if errorlevel 8 goto failed

echo Compressing...
if exist "%ZIP%" del /q "%ZIP%"
tar -a -c -f "%ZIP%" -C "%RELEASE%" "VickyWarAnalyzer-%VERSION%-win64"
if errorlevel 1 goto failed

copy /y "%~dp0target\vickywaranalyzer-%VERSION%-jfx.jar" "%RELEASE%\vickywaranalyzer-%VERSION%-jfx.jar" > nul

echo.
echo Ready for a GitHub release:
dir /b "%RELEASE%"
echo.
echo Nothing in "%RELEASE%" is committed, it holds build output only.
exit /b 0

:failed
echo Packaging failed.
if not "%NOPAUSE%"=="1" pause
exit /b 1