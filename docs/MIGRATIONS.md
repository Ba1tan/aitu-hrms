# MIGRATIONS — Schema Evolution

**Tool:** Flyway (already in use across all 5 done services). Liquibase is **not** used.
**Status:** authoritative. New services MUST follow this convention.

---

## 1. Decision

Every Spring service owns its own Postgres **schema** (not database). Schema
creation, table DDL, seeds, and forward migrations are all driven by **Flyway
Core** loaded as a Spring Boot starter dependency. Hibernate's `ddl-auto`
is set to `validate` in production (`none` in tests with H2).

Why Flyway over Liquibase:
- Already wired in all services (`flyway-core` + `flyway-database-postgresql` in pom).
- Plain SQL files — easier code review than XML/YAML changelogs.
- Versioned + repeatable migrations cover everything we need.

---

## 2. Per-service layout

```
services/{service-name}/src/main/resources/
└── db/migration/
    ├── V1__init_{service}_schema.sql       ← creates the schema + base tables
    ├── V2__{description}.sql               ← additive change
    ├── V3__{description}.sql               ← additive change
    └── R__{description}.sql                ← (optional) repeatable migration (views/seed data)
```

Filename grammar: `V<n>__<description>.sql` where `<n>` is monotonically
increasing inside that service. `__` (double underscore) is required.

---

## 3. Configuration

Every service's `application.yml` carries:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # NEVER 'create' or 'update' in prod
  flyway:
    enabled: true
    default-schema: hrms_<service>
    schemas:        hrms_<service>
    locations:      classpath:db/migration
    baseline-on-migrate: true
```

The `default-schema` and `schemas` settings tell Flyway to create the schema
if missing and to install its bookkeeping table (`flyway_schema_history`)
inside that schema. **Do not share `flyway_schema_history` across services.**

For tests, `application.yml` under `src/test/resources` overrides to:

```yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        default_schema: ""   # test H2 has no schema namespacing
  datasource:
    url: jdbc:h2:mem:{service}-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MONTH,YEAR,DAY,VALUE
```

Flyway is disabled in tests because H2's PostgreSQL mode does not faithfully
support every Postgres construct used in our migrations (JSONB, GIN indexes).
Hibernate `create-drop` from JPA entities is sufficient for repository tests.

---

## 4. Schemas in production

| Schema | Owner | First migration |
|---|---|---|
| `hrms_user` | user-service | `V1__init_user_schema.sql` |
| `hrms_employee` | employee-service | `V1__init_employee_schema.sql` |
| `hrms_attendance` | attendance-service | `V1__init_attendance_schema.sql` |
| `hrms_leave` | leave-service | `V1__init_leave_schema.sql` |
| `hrms_payroll` | payroll-service | `V1__init_payroll_schema.sql` |
| `hrms_reporting` | reporting-service (pending) | `V1__init_reporting_schema.sql` (TBD — materialized views only) |
| `hrms_notification` | notification-service (pending) | `V1__init_notification_schema.sql` (TBD) |
| `hrms_integration` | integration-hub (pending) | `V1__init_integration_schema.sql` (TBD) |

> **ai-ml-service has no schema** — it is stateless. Models and embeddings
> live on disk under `/data/hrms/ai-models/`.

---

## 5. Rules (read this before writing your first migration)

1. **Never edit a checked-in migration.** Once `V3__foo.sql` is on `main`,
   it is frozen forever. Mistakes are corrected by a new `V4__fix_foo.sql`.
2. **No DROP without a follow-up migration on every environment.** Flyway
   will not let you skip migrations, so a destructive migration on prod
   means writing the rescue migration before deploying.
3. **One concern per file.** "V5__add_payslip_index_and_seed_holidays.sql"
   is a code-review smell. Split.
4. **Cross-schema FKs are allowed but discouraged.** Each service should
   treat foreign UUIDs as opaque references. If an FK is genuinely needed
   (only inside the user-service for the role/permission tables), declare
   it inside the same schema.
5. **No application logic in `R__` repeatable migrations.** They run on
   every checksum change — only views, seed data, and procedures belong here.
6. **Time travel must be safe.** Pick column nullability so an in-flight
   service running the old code can still write rows after the new column
   is added (i.e. always nullable on add; tighten in a later migration).
7. **Test migrations against a real Postgres.** Spin up via
   `docker compose up -d postgres` and `mvn flyway:info` before merging.

---

## 6. Seeding

Seed data goes in versioned migrations, not in `R__`:

- `V2__seed_permissions.sql` (user-service) — system permission codes
- `V2__seed_holidays_2026.sql` (attendance-service) — KZ public holidays
- `V3__seed_super_admin.sql` (user-service) — first SUPER_ADMIN account

Seed migrations should be **idempotent in spirit** — use
`INSERT ... ON CONFLICT DO NOTHING` so a re-run on a partially-populated
schema doesn't fail. Flyway won't let it re-run, but local dev resets do.

---

## 7. Permission catalog seed

When permissions in `docs/PERMISSIONS.md` change, write a new migration in
`user-service`:

```sql
-- V<n>__add_{code}_permission.sql
INSERT INTO hrms_user.permissions (code, description)
VALUES ('NEW_CODE', 'Human description')
ON CONFLICT (code) DO NOTHING;

INSERT INTO hrms_user.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM hrms_user.roles r
CROSS JOIN hrms_user.permissions p
WHERE r.code IN ('SUPER_ADMIN', 'HR_MANAGER')
  AND p.code = 'NEW_CODE'
ON CONFLICT DO NOTHING;
```

The default seed in `V2__seed_permissions.sql` is the source of truth for
the **initial** state; subsequent permissions are appended via new migrations.

---

## 8. CI gate

Each service's CI runs `mvn verify` which boots the service against a
Testcontainers Postgres and applies all migrations. A migration that fails
to apply is a hard CI failure — the PR cannot merge.

---

## 9. Production rollout

```
1. PR adds V<n>__foo.sql + dependent code.
2. CI applies migrations against Testcontainers Postgres.
3. Merge to develop → staging deploy applies migrations on staging Postgres.
4. Soak for 24h; QA validates.
5. Merge to main → production deploy applies migrations on prod Postgres.
6. Flyway records V<n> in hrms_<service>.flyway_schema_history.
```

Rollback: forward-only. Write a corrective `V<n+1>__rollback_foo.sql`. Never
manually edit `flyway_schema_history`.