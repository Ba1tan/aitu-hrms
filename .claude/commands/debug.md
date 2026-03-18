# /debug

Diagnose and fix: `$ARGUMENTS`

## Systematic diagnosis — work through these in order

### 1. JWT / Auth 401
```bash
# Is endpoint in PUBLIC_ENDPOINTS? (SecurityConfig.java)
grep -n "PUBLIC_ENDPOINTS" src/main/java/kz/aitu/hrms/config/SecurityConfig.java
# Is token blacklisted in Redis?
redis-cli GET jwt:blacklist:{token}
# Token expired? (access = 15min, refresh = 7d)
# Header format? Must be: Authorization: Bearer {token}
```

### 2. Flyway migration failure on startup
```
1. Check logs for "Migration checksum mismatch" → you edited an applied migration (never do this)
2. Check "No database found to handle jdbc:postgresql://" → missing flyway-database-postgresql dep
3. Check SQL syntax errors in the failing V{n}__ file
4. Dev fix: flyway.cleanOnValidationError=false (already set) — don't use clean on prod
5. Fix: create a NEW corrective migration, never edit applied ones
```

### 3. BigDecimal / payroll calculation wrong
```java
// ALWAYS check:
// 1. Is arithmetic using BigDecimal.multiply().setScale(2, HALF_UP)? Never double arithmetic
// 2. Is the 9-step order preserved? (Step 5 taxable BEFORE Step 6 IPN)
// 3. Is MRP value from @Value injected? Test it with ReflectionTestUtils
// 4. Run KazakhstanPayrollCalculatorTest with the failing scenario
```

### 4. Cross-module NullPointerException
```
1. Are you injecting another module's @Repository directly? → Replace with @Service interface
2. Circular dependency? A injects B, B injects A → add @Lazy to one
3. LazyInitializationException on entity? → Add @Transactional to the calling service method
```

### 5. Docker container won't start
```bash
docker compose logs hrms-app --tail=50
# Common causes:
# - DB not ready: add healthcheck to postgres service + depends_on condition
# - Env var missing: check .env file has all required vars
# - Port conflict: lsof -i :8080
# - Flyway failure: see point 2 above
```

### 6. Pageable serialization broken (Page<T> returns wrong structure)
```
Ensure HrmsApplication has:
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
```

### 7. CORS error from frontend
```yaml
# application.yml — verify:
app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
```

## Fix format
After diagnosing, provide:
1. Root cause (1-2 sentences)
2. Code change (before/after)
3. Test case to prevent regression
