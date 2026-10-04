@echo off
setlocal
set "JAR=%~dp0target\vickywaranalyzer-1.2.1-jfx.jar"
set "PORTABLE=%~dp0tools\zulu-fx8\bin\javaw.exe"
if exist "%PORTABLE%" (
	set "JAVAW=%PORTABLE%"
) else (
	set "JAVAW=javaw.exe"
)
if not exist "%JAR%" (
	echo Analyzer jar not found: "%JAR%"
	echo Build it first, for example with: mvn install and mvn jfx:jar
	pause
	exit /b 1
)
cd /d "%~dp0"
start "" "%JAVAW%" -jar "%JAR%"