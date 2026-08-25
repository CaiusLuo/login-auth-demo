# Login authorization demo

A compact Java 21/Spring Boot and Vue 3 demo using server-side sessions, BCrypt,
MySQL/Flyway, two roles (`USER`, `ADMIN`), and LLM-assisted username review.

## Build and run locally

Prerequisites: Java 21. MySQL must contain an empty `login_auth_demo` database and
credentials matching the `DB_*` variables. Non-production runs use a local
allow-all moderation client so no LLM key is needed.

```bash
cp .env.example .env
./mvnw clean verify
java -jar target/login-auth-demo.jar
```

Open `http://localhost:8080`. The Maven build runs the Spring integration tests,
builds Vue, embeds `frontend/dist` into the Jar, and packages the application.

## Production on Debian 12

The supplied configuration assumes Java 21, MySQL 8, Nginx, and systemd. It does
not require Caddy or Docker.

1. Create the system user and directories:

   ```bash
   sudo useradd --system --home /opt/login-auth-demo --shell /usr/sbin/nologin login-auth-demo
   sudo install -d -o login-auth-demo -g login-auth-demo /opt/login-auth-demo
   sudo install -d -m 0750 /etc/login-auth-demo /etc/nginx/tls
   ```

2. Copy `.env.example` to `/etc/login-auth-demo/login-auth-demo.env`, replace every
   placeholder, set mode `0600`, and create the MySQL database/user. The file is
   systemd `EnvironmentFile` syntax (plain `KEY=value`, no `export`).
3. Install `deploy/login-auth-demo.service` under `/etc/systemd/system/` and
   `deploy/login-auth-demo.nginx.conf` under `/etc/nginx/sites-available/`, then
   enable the Nginx site. For an IP-only demo, create a self-signed TLS certificate
   at the paths in the Nginx file; visitors will need to accept the certificate warning.
4. Build on a trusted machine or the server with `./mvnw clean verify`, then deploy:

   ```bash
   sudo ./scripts/deploy.sh target/login-auth-demo.jar
   ./scripts/health.sh
   ALLOW_INSECURE_TLS=true ./scripts/smoke-test.sh https://119.91.22.199
   ```

The application listens only on `127.0.0.1:18080`; expose ports 80/443 and keep
18080 private. Production cookies are `HttpOnly`, `SameSite=Lax`, and `Secure`.

## Moderation behavior

Registration applies NFKC normalization and deterministic validation before the
LLM call. `ALLOW` creates a `USER`; `REJECT` and `REVIEW` return 422. Timeouts,
network/5xx failures, or malformed model output return 503 and create no user.
Existing login and authorization never call the LLM.
