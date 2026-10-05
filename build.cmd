@echo off
setlocal EnableDelayedExpansion
set "JAR=%~dp0target\vickywaranalyzer-1.2.1-jfx.jar"
set "PACKAGEJAR=%~dp0target\vickywaranalyzer.jar"
set "EXE=%~dp0target\VickyWarAnalyzer.exe"
set "CLASSES=%~dp0target\classes"
set "MAIN=ee.tkasekamp.vickywaranalyzer.gui.GUI"

set "JDK=%~dp0tools\jdk8fx"
if not exist "%JDK%\bin\javac.exe" set "JDK=%~dp0tools\zulu-fx8"
if not exist "%JDK%\bin\javac.exe" set "JDK=%JAVA_HOME%"

if not exist "%JDK%\bin\javac.exe" (
	echo No JDK 8 with JavaFX found.
	echo Unzip a JavaFX enabled Java 8 JDK into tools\jdk8fx, or set JAVA_HOME to one.
	if not "%NOPAUSE%"=="1" pause
	exit /b 1
)
if not exist "%JDK%\jre\lib\ext\jfxrt.jar" (
	echo "%JDK%" has no JavaFX, this program will not compile without it.
	if not "%NOPAUSE%"=="1" pause
	exit /b 1
)

echo Using JDK "%JDK%"
if exist "%CLASSES%" rmdir /s /q "%CLASSES%"
mkdir "%CLASSES%"

set "SOURCES=%TEMP%\vickywaranalyzer-sources.txt"
set "MANIFEST=%TEMP%\vickywaranalyzer-manifest.txt"
if exist "%SOURCES%" del /q "%SOURCES%"
for /r "%~dp0src\main\java" %%F in (*.java) do (
	set "SRC=%%F"
	echo "!SRC:\=/!">>"%SOURCES%"
)
"%JDK%\bin\javac.exe" -encoding UTF-8 -nowarn -d "%CLASSES%" @"%SOURCES%"
if errorlevel 1 goto failed
del /q "%SOURCES%"

xcopy /y /e /q /i "%~dp0src\main\resources\*" "%CLASSES%" > nul

> "%MANIFEST%" (
	echo Manifest-Version: 1.0
	echo Main-Class: %MAIN%
)

if exist "%JAR%" del /q "%JAR%"
pushd "%CLASSES%"
"%JDK%\bin\jar.exe" cfm "%JAR%" "%MANIFEST%" *
"%JDK%\bin\jar.exe" cfm "%PACKAGEJAR%" "%MANIFEST%" *
popd
if not exist "%JAR%" goto failed

del /q "%MANIFEST%"
echo Built "%JAR%"

set "GCC="
for %%G in (gcc.exe) do if not "%%~$PATH:G"=="" set "GCC=%%~$PATH:G"
if "%GCC%"=="" (
	echo.
	echo WARNING: gcc.exe not found, skipping VickyWarAnalyzer.exe.
	echo Install MinGW-w64 to build the launcher, see the README.
	echo.
	exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\make-icon.ps1" -Output "%~dp0target\vickywaranalyzer.ico" > nul
if errorlevel 1 goto failed

windres --include-dir "%~dp0target" "%~dp0src\launcher\launcher.rc" -O coff -o "%~dp0target\launcher_res.o"
if errorlevel 1 goto failed
"%GCC%" -Wall -O2 -s -municode "-Wl,--subsystem,windows" -o "%EXE%" "%~dp0src\launcher\launcher.c" "%~dp0target\launcher_res.o"
if errorlevel 1 goto failed

echo Built "%EXE%"
echo Run package.cmd to build the portable release.
exit /b 0

:failed
echo Build failed.
if not "%NOPAUSE%"=="1" pause
exit /b 1