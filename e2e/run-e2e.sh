#!/usr/bin/env bash
# Runs the e2e suite. This is the one command CI calls.
#
#   ./run-e2e.sh                      start ../app on PORT (default 8099), run the suite, stop the app
#   BASE_URL=https://host ./run-e2e.sh   run against an app that is already running; start nothing
#   ./run-e2e.sh -Dtest=AccountApiTest   extra arguments go to Maven
#
# Exit code is the test result. Failure artifacts (screenshots, Playwright traces) land in target/artifacts/.
set -euo pipefail
cd "$(dirname "$0")"

# Job control gives the background app its own process group, so cleanup can stop
# Maven *and* the JVM it launches, not just the wrapper script.
set -m
APP_PID=""
cleanup() {
  if [[ -n "$APP_PID" ]]; then
    kill -- "-$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# Installs Chromium if it is missing; a quick no-op otherwise. The tests themselves never download browsers.
./mvnw -q --batch-mode exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install chromium"

if [[ -z "${BASE_URL:-}" ]]; then
  PORT="${PORT:-8099}"
  export BASE_URL="http://localhost:${PORT}"
  mkdir -p target
  echo "Starting app on ${BASE_URL} ..."
  (cd ../app && PORT="$PORT" ./mvnw -q --batch-mode compile exec:java) >target/app.log 2>&1 &
  APP_PID=$!
  for _ in $(seq 1 90); do
    if curl -sf "${BASE_URL}/health" >/dev/null 2>&1; then break; fi
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      echo "App exited during startup; see target/app.log" >&2
      tail -20 target/app.log >&2 || true
      exit 1
    fi
    sleep 1
  done
  curl -sf "${BASE_URL}/health" >/dev/null || { echo "App did not become healthy; see target/app.log" >&2; exit 1; }
else
  echo "Using existing app at ${BASE_URL}"
fi

./mvnw --batch-mode test "$@"
