# /add-migration

Create a Flyway migration for: `$ARGUMENTS`

## Step 1 — Find the next version
```bash
ls src/main/resources/db/migration/ | sort
# Current state: V1 (schema), V2 (admin seed)
# V3__add_manager_role.sql is in docs/dev-phases/mvp/ — copy it to migrations if not yet applied
# Next new migration: V4 or higher depending on what's applied
```

## Step 2 — Rules (violations break production)
- `UUID PRIMARY KEY DEFAULT gen_random_uuid()` — never SERIAL/BIGSERIAL
- Audit columns on every new table: `created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted`
- Enum columns: `VARCHAR(50) NOT NULL` — never PostgreSQL native ENUM
- Money: `NUMERIC(15,2)` — never FLOAT/DOUBLE/DECIMAL without precision
- FK naming: `fk_{table}_{referenced_table}`
- Index naming: `idx_{table}_{column}`
- Adding column to existing table: always provide DEFAULT or make nullable
- **NEVER** edit or delete an already-applied migration

## Step 3 — Template
```sql
-- V{N}__{description_with_underscores}.sql
-- Purpose: {what this does and why}

CREATE TABLE {table_name} (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- domain columns
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_{table}_{col} ON {table_name}({col});

ALTER TABLE {table_name}
    ADD CONSTRAINT fk_{table}_{ref} FOREIGN KEY ({col}) REFERENCES {ref}(id);
```

---

# /add-endpoint

Add endpoint: `$ARGUMENTS`

## Steps

1. **Read existing controller** for this module — understand current pattern
2. **DTO**: add to module's DTOs class (request with `@Valid` + response)
3. **Repository method**: use JPQL `@Query` for complex queries, Spring Data for simple ones
4. **Service interface**: add method signature first
5. **Service impl**: `@Transactional` for writes, `readOnly=true` for reads
   - Business errors: `throw new BusinessException("message")`
   - Not found: `throw new ResourceNotFoundException("Entity", id)`
6. **Controller method**:
   - `@Operation(summary = "...")` for Swagger
   - `@PreAuthorize("hasAnyRole('...')")` — mandatory, see role list in CLAUDE.md
   - `@Valid` on `@RequestBody`
   - Return `ResponseEntity<ApiResponse<T>>`
7. **Test**: happy path + wrong role (403) in controller test

## Context path reminder
Controller mapping `/v1/employees/{id}` → effective URL `/api/v1/employees/{id}`
