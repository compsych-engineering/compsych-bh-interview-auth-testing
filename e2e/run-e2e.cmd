@echo off
@rem The Windows entry point. Twin of run-e2e.sh -- keep the two in step.
@rem
@rem   run-e2e.cmd                       start ..\app on PORT (default 8099), run the suite, stop the app
@rem   set BASE_URL=https://host         run against an app that is already running; start nothing
@rem   run-e2e.cmd "-Dtest=AccountApiTest"   extra arguments go to Maven (quote them)
@rem
@rem Exit code is the test result. Failure artifacts land in target\artifacts\.
@rem Needs curl.exe (ships with Windows 10 1803 and later).
setlocal
cd /d "%~dp0"

@rem Installs Chromium if it is missing; a quick no-op otherwise. The tests never download browsers.
call "%~dp0mvnw.cmd" -q --batch-mode exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test "-Dexec.args=install chromium"
if errorlevel 1 exit /b 1

set "STARTED_APP="
if defined BASE_URL goto :run_tests

if not defined PORT set "PORT=8099"
set "BASE_URL=http://localhost:%PORT%"
if not exist target mkdir target
echo Starting app on %BASE_URL% ...

@rem The app's folder is a sibling, so the log path below is relative to it and needs no quoting.
pushd "%~dp0..\app"
start "e2e-app" /min cmd /c "mvnw.cmd -q --batch-mode compile exec:java > ..\e2e\target\app.log 2>&1"
popd
set "STARTED_APP=1"

for /l %%i in (1,1,90) do (
  curl -sf "%BASE_URL%/health" >nul 2>&1 && goto :run_tests
  ping -n 2 127.0.0.1 >nul
)
echo App did not become healthy; see target\app.log 1>&2
if exist target\app.log type target\app.log 1>&2
call :stop_app
exit /b 1

:run_tests
if not defined STARTED_APP echo Using existing app at %BASE_URL%
call "%~dp0mvnw.cmd" --batch-mode test %*
set "RESULT=%ERRORLEVEL%"
if defined STARTED_APP call :stop_app
exit /b %RESULT%

@rem Stops whatever is listening on PORT, and its child processes. The app runs as a JVM
@rem under a cmd window, so finding it by port is more reliable than tracking a PID.
:stop_app
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":%PORT% .*LISTENING"') do taskkill /PID %%p /T /F >nul 2>&1
exit /b 0
