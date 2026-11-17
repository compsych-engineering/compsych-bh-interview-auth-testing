package com.compsych.interview;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpResponseException;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.staticfiles.Location;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builds the Javalin app: static page, login, and account read/update. Not started here. */
public final class AppServer {

    record LoginRequest(String username, String password) {
    }

    record AccountUpdate(String displayName, String email) {
    }

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("access");

    private AppServer() {
    }

    public static Javalin build() {
        UserStore users = UserStore.fromClasspath();
        AccountStore accounts = AccountStore.fromClasspath();
        SessionStore sessions = new SessionStore();
        Latency latency = Latency.fromEnv();

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add("/public", Location.CLASSPATH);
            // One line per request: method, path, status, time. Headers (tokens) are never logged.
            config.requestLogger.http((ctx, ms) -> ACCESS_LOG.info("{} {} -> {} in {} ms",
                    ctx.method(), ctx.path(), ctx.status().getCode(), Math.round(ms)));
        });

        // Every error leaves as {"error": "..."}.
        app.exception(HttpResponseException.class, (e, ctx) ->
                ctx.status(e.getStatus()).json(Map.of("error", e.getMessage())));

        // Malformed or wrongly shaped JSON bodies are the client's fault.
        app.exception(JsonProcessingException.class, (e, ctx) ->
                ctx.status(400).json(Map.of("error", "Request body is not valid JSON")));

        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        app.post("/api/login", ctx -> {
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            UserStore.User user = users.authenticate(req.username(), req.password())
                    .orElseThrow(() -> new UnauthorizedResponse("Invalid username or password"));
            ctx.json(Map.of(
                    "token", sessions.create(user.username()),
                    "username", user.username(),
                    "role", user.role(),
                    "accountId", user.accountId()));
        });

        // Authentication, plus the admin-only list check. Update authorization is in the PUT handler,
        // where the target account is known.
        Handler requireAuth = ctx -> {
            String header = ctx.header("Authorization");
            String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
            // Javalin routes "/api/accounts/" to the list endpoint too, so compare the path
            // without trailing slashes.
            boolean isList = ctx.path().replaceAll("/+$", "").equals("/api/accounts");
            boolean isLookup = ctx.method() == HandlerType.GET && !isList;

            Optional<String> username = sessions.usernameFor(token);
            boolean wellFormed = token != null && !token.isBlank();
            if (username.isEmpty() && !(isLookup && wellFormed)) {
                throw new UnauthorizedResponse("Missing or invalid token");
            }
            username.ifPresent(u -> ctx.attribute("username", u));

            if (isList && !username.flatMap(users::find).map(AppServer::isAdmin).orElse(false)) {
                throw new ForbiddenResponse("Only admins can list accounts");
            }
        };
        app.before("/api/accounts", requireAuth);
        app.before("/api/accounts/*", requireAuth);

        app.get("/api/accounts", ctx -> ctx.json(accounts.list()));

        app.get("/api/accounts/{id}", ctx -> {
            latency.pause();
            ctx.json(accounts.find(ctx.pathParam("id")).orElseThrow(AppServer::accountNotFound));
        });

        app.put("/api/accounts/{id}", ctx -> {
            UserStore.User actor = users.find(ctx.attribute("username"))
                    .orElseThrow(() -> new UnauthorizedResponse("Missing or invalid token"));
            String owner = accounts.find(ctx.pathParam("id")).map(AccountStore.Account::owner).orElse(null);
            if (!mayUpdate(actor, owner)) {
                throw new ForbiddenResponse("Only admins can update accounts");
            }
            AccountUpdate update = ctx.bodyAsClass(AccountUpdate.class);
            if (isBlank(update.displayName()) || isBlank(update.email())) {
                throw new BadRequestResponse("displayName and email are required");
            }
            ctx.json(accounts.update(ctx.pathParam("id"), update.displayName().trim(), update.email().trim())
                    .orElseThrow(AppServer::accountNotFound));
        });

        return app;
    }

    private static boolean isAdmin(UserStore.User user) {
        return "admin".equals(user.role());
    }

    /** Admins may update any account; anyone else needs a matching grant. */
    private static boolean mayUpdate(UserStore.User actor, String targetOwner) {
        if (isAdmin(actor)) {
            return true;
        }
        boolean ownsTarget = actor.username().equals(targetOwner);
        return ownsTarget
                ? actor.grants().contains("update:self")
                : actor.grants().contains("update:others");
    }

    private static NotFoundResponse accountNotFound() {
        return new NotFoundResponse("Account not found");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
