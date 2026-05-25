# OPERATIONS — Run Book

How HRMS is deployed, configured, observed, and recovered. Read this before
touching production.

---

## 1. Environments

| Env | Host | Compose project | Gateway port | Notes |
|---|---|---|---|---|
| Production | `hrms.nursnerv.uk` | `hrms` | `8090` (proxied by nginx on 80/443) | `--profile production` enables nginx |
| Staging | same host, separate dirs | `hrms-staging` | `8091` | No nginx; gateway exposed directly. Frontend on `:3000`. |
| Local | developer machine | `hrms-dev` | `8080` | `docker compose up -d postgres redis rabbitmq` + `mvn spring-boot:run` per service |

Only **one** Postgres/Redis/RabbitMQ cluster runs per host. Services are
schema-isolated, not database-isolated.

---

## 2. Configuration

### 2.1 Source of truth

- `env.example` — template, checked in.
- `.env` (prod) and `.env` (staging, on a different dir) — derived from template,
  **not** checked in. Today's `.env` IS checked in by mistake — see §6 below.
- `docker-compose.microservices.yml` reads env at compose time, passes to
  each service as container env vars.
- Each service's `application.yml` reads `${VAR:default}`. Defaults assume
  local dev; production overrides via env.

### 2.2 Secret management

Today: plain env vars in `.env` files. Acceptable for current scale (single
host) but the medium-term plan is one of:

1. **Docker swarm secrets** (zero new infra) — promote `.env` values to
   secrets, mount to containers under `/run/secrets/<name>`, change
   application.yml to read those paths.
2. **HashiCorp Vault** (extra service) — only worth it once we exceed one
   host or onboard external integrations with their own credentials.

Until that migration:
- `.env` files have file mode `600`.
- `GITHUB_TOKEN` is a deploy-only PAT scoped to `read:packages` — rotate
  quarterly. **Do not check it in.**
- `JWT_SECRET` is regenerated per-environment (`openssl rand -base64 32`).

### 2.3 Required env vars

Authoritative list lives in `env.example`. Pending services need:

| Service | Required new vars |
|---|---|
| reporting-service | `PDF_FONT_PATH`, `REPORT_TMP_DIR` |
| notification-service | `MAIL_HOST/PORT/USERNAME/PASSWORD`, `FCM_CREDENTIALS_JSON` (optional), `NOTIFICATION_FROM_EMAIL` |
| integration-hub | `ONE_C_BASE_URL`, `ONE_C_USERNAME`, `ONE_C_PASSWORD`, `BANK_FILE_FORMAT` |

---

## 3. Deployment

### 3.1 Per-service rolling deploy (the normal flow)

CI handles this. Per the workflow files in `.github/workflows/`:

1. Push to `develop` → CI builds + pushes `:develop` tag → SSH deploys to
   staging via `docker compose up -d --no-deps <service>`.
2. Push to `main` → same dance against prod.

The `--no-deps` flag is critical — it stops compose from restarting
postgres/redis/rabbit just because one service rolled.

### 3.2 Full stack (re)bootstrap

```bash
# On host
cd ~/hrms
git fetch origin main && git reset --hard origin/main

# Production (with nginx)
docker compose --profile production -f docker-compose.microservices.yml up -d

# Staging (no nginx, gateway directly exposed)
docker compose -f docker-compose.microservices.yml up -d
```

### 3.3 Order of bringup

`postgres` → `redis` → `rabbitmq` → backend services in any order
(healthchecks gate the gateway) → `gateway` → `frontend` → `nginx` (prod only).
Compose's `depends_on: { condition: service_healthy }` enforces this.

---

## 4. Observability

### 4.1 Logs

Today: `docker logs <container>` and `docker compose logs -f <service>`.
No central aggregation yet.

Planned: ship to Loki via the docker driver:

```yaml
logging:
  driver: loki
  options:
    loki-url: "https://loki.internal/loki/api/v1/push"
    loki-batch-size: "400"
```

Until Loki is up, stick with `docker logs`. Logs are JSON-structured per
service via `logback-spring.xml`.

### 4.2 Metrics

Each service exposes `/actuator/prometheus` (when
`management.endpoints.web.exposure.include` adds `prometheus`).
Currently only `health,info,metrics` is enabled — flip to
`health,info,metrics,prometheus` when the Prometheus instance is provisioned.

Key metrics to dashboard:
- HTTP p50/p95/p99 per service (`http_server_requests_seconds`)
- Hibernate connection pool (`hikaricp_connections_active`)
- RabbitMQ unacked messages per queue (broker-side)
- JVM heap, GC pause time
- Payroll job duration (`payroll.batch.duration` — custom metric)

### 4.3 Tracing

Reserved: OpenTelemetry → Jaeger. Not wired today. When the time comes,
add the OTel agent to each service's Docker image and point at a central
collector.

### 4.4 Health checks

Every service has a Spring Actuator `/actuator/health` endpoint. Compose
healthchecks call it; nginx 502 if any backend is unhealthy. The
gateway's healthcheck is the load balancer's health probe.

---

## 5. Backup & disaster recovery

### 5.1 Postgres

- **Logical backup:** nightly `pg_dump` via cron on the host, output to
  `/var/backups/hrms/$(date +%F).sql.gz`.
- **Retention:** 30 days locally, weekly snapshot offsited to S3-compatible
  storage (TBD provider).
- **Restore:** `gunzip -c <date>.sql.gz | psql -U hrms_user hrms` on a
  fresh container after stopping services.

### 5.2 Redis

Stateless cache + JWT blacklist. The JWT blacklist is the only thing that
matters; max value of losing it is "users have to log in again." Acceptable.
No backup needed.

### 5.3 RabbitMQ

Queues are durable but contents are operational-only (events). On total
loss: events that were in flight are dropped. Publishers are non-blocking
(`try/catch + log + drop`), so the producing services keep working.
Restart consumers after RMQ recovers — they re-bind queues automatically.

### 5.4 File storage

`/data/hrms/uploads/` (employee documents) and
`/data/hrms/payslips/` (PDFs) live on a docker-managed volume. The compose
volumes are persistent across container recreate. Off-host backup: same
nightly cron, `tar.gz` to the same offsite.

### 5.5 RTO / RPO targets

- **RTO** (recovery time): < 1 hour for full-stack outage.
- **RPO** (data loss tolerance): < 24 hours (the nightly backup window).

These are SME-grade targets. Tighten to RPO < 1 hour by enabling Postgres
WAL archiving when budget allows.

---

## 6. Known operational risks (open items)

- **`.env` file with real `GITHUB_TOKEN` is checked into the repo.** Rotate
  the token and remove from history before any external review.
- **No structured log aggregation** — incident response means SSH + `docker logs`.
- **No alerting** — pages don't fire on outages; checking is manual.
- **No DLQ on RabbitMQ queues** — failed consumer messages are silently
  dropped after retries.
- **Single host** — no failover. Acceptable for MVP scale, but document
  the migration path before signing the first 100-employee customer.

---

## 7. Common runbook commands

```bash
# Tail one service
docker compose -f docker-compose.microservices.yml logs -f --tail=200 payroll-service

# Restart one service after env change
docker compose -f docker-compose.microservices.yml up -d --no-deps --force-recreate payroll-service

# Postgres shell
docker compose -f docker-compose.microservices.yml exec -it postgres psql -U hrms_user -d hrms

# Redis shell
docker compose -f docker-compose.microservices.yml exec -it redis redis-cli -a "$REDIS_PASSWORD"

# RabbitMQ management UI (forward from prod host)
ssh -L 15672:localhost:15672 hrms.nursnerv.uk

# Show health of every backend
for svc in user-service employee-service attendance-service leave-service payroll-service gateway; do
  printf "%-22s " "$svc"
  docker inspect --format='{{.State.Health.Status}}' "hrms-$svc-1" 2>/dev/null || echo "not running"
done
```