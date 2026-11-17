# e2e suite

Black-box Playwright (Java) + JUnit 5 tests for the account portal in `../app`. They drive a
**running** app over HTTP and in a real browser; they share no code with it.

## Run

```
./run-e2e.sh          # macOS, Linux, WSL
run-e2e.cmd           # native Windows (PowerShell or cmd); needs curl.exe, which ships with Windows 10 1803+
```

On native Windows, set environment variables with `set NAME=value` before the command (for example
`set BASE_URL=http://localhost:8080`), and quote extra Maven arguments: `run-e2e.cmd "-Dtest=AccountApiTest"`.
Everything below applies to both scripts, with `mvnw.cmd` in place of `./mvnw`.

`run-e2e.sh` installs Chromium if it is missing (a one-time download of a few hundred MB, a no-op
afterwards), starts `../app` on port 8099, runs the tests, and stops the app. The tests themselves
never download browsers. To test an app that is already running (or a different one), skip the
app start:

```
BASE_URL=http://localhost:8080 ./run-e2e.sh
```

| Variable | Default | Meaning |
|---|---|---|
| `BASE_URL` | starts the app on `http://localhost:8099` | App under test |
| `PORT` | `8099` | Port when the script starts the app |
| `TEST_ADMIN_USER` / `TEST_ADMIN_PASSWORD` | `admin` / `password` | Credentials of an administrator |
| `HEADED` | `false` | `true` to watch the browser |
| `PW_CHANNEL` | _(bundled Chromium)_ | e.g. `chrome` to use an installed Chrome and skip the download |

## What it covers

- **`AccountApiTest`**: health; login (success, wrong password, unknown user, empty and malformed
  bodies); read, list and update accounts as an administrator; blank/malformed/unknown-account
  update errors; requests with no `Authorization` header are rejected.
- **`AccountUiTest`**: login screen (welcome text, required-field markers, empty submit, bad
  password); the administrator's account table; opening, editing, saving and navigating back;
  logout.

Everything here signs in as the administrator. Tests that change data restore it afterwards.

## Failure evidence

When a UI test fails, a screenshot and a Playwright trace are written to `target/artifacts/`
(`<Class>.<test>.png` and `.trace.zip`). Open a trace with
`./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="show-trace target/artifacts/<file>.trace.zip"`.
The app's own log is `target/app.log` when the script started it.
