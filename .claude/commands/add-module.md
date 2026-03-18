# /add-module

Implement the `$ARGUMENTS` module in the HRMS modular monolith.

## Step 0 — Read the guide first
```
Read docs/dev-phases/mvp/$ARGUMENTS.md before writing any code.
If that file doesn't exist, read docs/dev-phases/mvp/ to find the closest guide.
```

## Step 1 — Check what already exists

```bash
# Check if DB table already exists in V1 migration
grep -i "$ARGUMENTS" src/main/resources/db/migration/V1__initial_schema.sql

# Check next Flyway version
ls src/main/resources/db/migration/ | sort | tail -3

# Check existing files in module directory
find src -path "*modules/$ARGUMENTS*" -name "*.java"
```

## Step 2 — Create directory structure

```
src/main/java/kz/aitu/hrms/modules/$ARGUMENTS/
├── controller/
├── service/          # interface + impl
├── repository/
├── entity/
├── dto/              # or single DTOs.java class
└── enums/
```

## Step 3 — Entity rules
- MUST extend `BaseEntity` — never re-declare id/createdAt/updatedAt/createdBy
- All PKs: UUID via `@GeneratedValue(strategy = GenerationType.UUID)`
- Enum columns: `@Enumerated(EnumType.STRING)` + `VARCHAR(50)` in DB
- Money: `BigDecimal` with `@Column(precision = 15, scale = 2)`
- Boolean soft delete: inherited from `BaseEntity.isDeleted`
- If table exists in V1: map exactly to existing column names

## Step 4 — Migration rules (only if NEW tables needed)
- Check existing migrations: `ls src/main/resources/db/migration/`
- Next version after V2 is **V3** (V3__add_manager_role.sql is for MANAGER role)
- Naming: `V{n}__{description}.sql`
- **NEVER** modify an already-applied migration
- Use `UUID PRIMARY KEY DEFAULT gen_random_uuid()` — never SERIAL
- Always include audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted`

## Step 5 — Service rules
- Define interface first, then impl with `@Service @Slf4j @RequiredArgsConstructor`
- Write methods are `@Transactional`, reads are `@Transactional(readOnly = true)`
- Business errors: throw `BusinessException("message")` — caught by GlobalExceptionHandler → 400
- Not found: throw `ResourceNotFoundException("Entity", id)` → 404
- Cross-module calls: inject the OTHER module's `@Service` interface only, never its `@Repository`

## Step 6 — Controller rules
- `@RestController @RequestMapping("/v1/{module}") @RequiredArgsConstructor`
- `@PreAuthorize` on EVERY method — never leave endpoints unprotected
- Role reference: SUPER_ADMIN, HR_MANAGER, ACCOUNTANT, MANAGER, EMPLOYEE
- Return `ResponseEntity<ApiResponse<T>>`:
  - Create: `ResponseEntity.status(201).body(ApiResponse.created(data))`
  - Read/Update: `ResponseEntity.ok(ApiResponse.ok(data))`
  - Delete: `ResponseEntity.ok(ApiResponse.noContent("Deleted"))`
- Use `@PageableDefault(size=20)` for paginated endpoints

## Step 7 — Notification wiring
If this module should notify users on events:
- Inject `NotificationService` (from `modules/notification/`)
- Call `notificationService.notify(userId, type, title, body, referenceId, referenceType)`
- Wrap in try-catch — notifications must never fail a business transaction

## Step 8 — Verify
```bash
# Compile check
mvn compile -q

# Verify no cross-module repository imports
grep -rn "import kz.aitu.hrms.modules" src/main/java/kz/aitu/hrms/modules/$ARGUMENTS/ | \
  grep "repository\." | grep -v ".$ARGUMENTS."
# Should return empty — if not, fix by using service interface instead
```
