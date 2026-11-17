# Account Portal (mock auth app)

A small Javalin service plus one static HTML page. Mock data only.

## Run

Requires JDK 17+. The Maven Wrapper downloads Maven and dependencies on first run (network needed once).

```
./mvnw compile exec:java          # http://localhost:8080
PORT=9000 ./mvnw compile exec:java  # different port
```

(`mvnw.cmd` on native Windows.)

## Users

All passwords are `password`.

| Username | Role | Account |
|---|---|---|
| alice | member | acct-1 |
| bob | member | acct-2 |
| carol | admin | acct-3 |
| admin | admin | acct-4 |

## API

| Route | Notes |
|---|---|
| `POST /api/login` | `{username, password}` -> `{token, username, role, accountId}` |
| `GET /api/accounts` | list all accounts. `Authorization: Bearer <token>`. **Admins only** |
| `GET /api/accounts/{id}` | `Authorization: Bearer <token>` |
| `PUT /api/accounts/{id}` | same auth; body `{displayName, email}`. **Admins only** |
| `GET /health` | `{"status":"ok"}` |

Errors are `{"error": "..."}` with 400, 401, 403 or 404.

Permissions: any logged-in user can read a single account; only users with role `admin` can
list accounts or update one. Members get 403 on both, even on `PUT` to their own account.

The page shows admins a table of all accounts after login; selecting a row opens the edit form
for that account. Members go straight to their own account's form.

Account updates are held in memory; restarting the service resets the data.
