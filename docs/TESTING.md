# TESTING — Strategy

What we test, with what tools, and where each layer lives. Read this before
adding tests to a new service.

---

## 1. Layers

| Layer | Tool | Scope | Where |
|---|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | One class, mocks for collaborators | `services/{name}/src/test/java/.../service/*Test.java` |
| Web slice | `@WebMvcTest` + MockMvc + `SecurityMockMvcRequestPostProcessors` | Controller + serialization + permission gates, no JPA | `services/{name}/src/test/java/.../controller/*Test.java` |
| Repository | `@DataJpaTest` + H2 (PostgreSQL mode) | Repositories against in-memory DB | `services/{name}/src/test/java/.../repository/*Test.java` |
| Integration | `@SpringBootTest` + MockMvc | Full controller→service→repo wiring on H2 | `services/{name}/src/test/java/.../*FlowIntegrationTest.java` |
| Cross-service contract | Feign WireMock | Verify our Feign clients match what upstream actually returns | TBD — add as services come online |

H2 caveats already encountered (codified in payroll-service):
- Use `MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=MONTH,YEAR,DAY,VALUE`
- Disable Flyway in tests; let Hibernate `create-drop` from entities
- Repository tests use `@AutoConfigureTestDatabase(replace = NONE)` to
  preserve the test `application.yml` datasource

---

## 2. Conventions

### 2.1 `@WebMvcTest` slice

- App-class-level annotations leak into the slice and break it.
  Keep `@SpringBootApplication` clean — push `@EnableJpaAuditing`,
  `@EnableJpaRepositories`, `@EnableFeignClients` into per-feature
  `@Configuration` classes.
- Inject auth via `MockMvcBuilders.with(authentication(<Authentication>))`
  — never via `SecurityContextHolder.setContext(...)` (Spring Security 6's
  `SecurityContextHolderFilter` overwrites it per request).
- Order matters: `@Valid` runs before `@PreAuthorize`. A test that expects
  403 must send a body that passes validation, otherwise it gets 400.

### 2.2 Repository tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FooRepositoryTest {
    @Autowired private FooRepository repo;
    // ...
}
```

### 2.3 Naming

- Class: `FooServiceTest`, `FooControllerTest`, `FooRepositoryTest`,
  `FooFlowIntegrationTest`.
- Method: `whenX_thenY` or `methodUnderTest_state_expectedOutcome`.
  Both are accepted; pick one per file.

### 2.4 Money

- Always `BigDecimal`. Compare with `assertThat(...).isEqualByComparingTo("123.45")`
  — never `.isEqualTo(new BigDecimal("123.45"))` (scale differs).

---

## 3. Coverage targets

| Service | Target line coverage | Critical-path coverage |
|---|---|---|
| user-service | ≥ 70% | 100% on `AuthService.login/refresh/logout` |
| employee-service | ≥ 70% | 100% on `EmployeeService.terminate`, biometric enrollment |
| attendance-service | ≥ 70% | 100% on face check-in flow, holiday/weekend gating |
| leave-service | ≥ 70% | 100% on balance arithmetic, carryover, approval state machine |
| payroll-service | ≥ 80% | **100% on `KazakhstanPayrollCalculator` — non-negotiable** |
| reporting-service (pending) | ≥ 60% | Dashboard role-section logic |
| notification-service (pending) | ≥ 60% | Channel routing rules |
| integration-hub (pending) | ≥ 60% | 1C contract serialization, bank file format |
| ai-ml-service (pending) | ≥ 70% (pytest) | Face similarity threshold, anomaly threshold |

These are aspirational, not gates — let CI fail on test errors, not on
coverage % drops below target.

---

## 4. Where the bodies are buried

Things to remember when writing tests, learned the hard way:

- **`@EnableJpaAuditing` on the app class breaks `@WebMvcTest`.** It pulls
  in JPA. Move it to `AuditorConfig`. (Same for `@EnableJpaRepositories`,
  `@EnableFeignClients`.)
- **H2 reserves `MONTH`, `YEAR`, `DAY`, `VALUE`.** Add
  `NON_KEYWORDS=MONTH,YEAR,DAY,VALUE` to test JDBC URL.
- **`SecurityContextHolder.setContext()` is overwritten per request** by
  Spring Security 6. Use `.with(authentication(...))` on each `mvc.perform`.
- **`@PreAuthorize` runs after `@Valid`.** Empty body gets 400, not 403.
- **Schema-aware Hibernate config bites tests.** Drop
  `default_schema: hrms_<x>` from test `application.yml` so H2 stops
  looking for the schema namespace.

---

## 5. Running locally

```bash
# Single service
cd services/payroll-service
mvn verify

# All services (slow)
for d in services/*/; do
  [ -f "$d/pom.xml" ] && (cd "$d" && mvn verify --no-transfer-progress) || true
done

# Just one test class
mvn -pl services/payroll-service -Dtest=PayrollFlowIntegrationTest test
```

---

## 6. Running in CI

Each service has `.github/workflows/{name}-ci.yml` that runs `mvn verify`
on every PR. Failed tests block merge. See `docs/MIGRATIONS.md` §8 for the
DB migration verification step.

---

## 7. Pending: cross-service contract testing

Once notification-service starts consuming `PayrollAnomalyDetectedEvent`
and reporting-service starts consuming `PayrollJobCompletedEvent`, we need
to lock down the payload shapes. Two options:

1. **Pact-broker style:** consumer publishes expected schema, producer's
   CI verifies against it. Heavy infra.
2. **Shared DTO in hrms-common (current approach):** both producer and
   consumer import the class — compile-time agreement. Lighter, but only
   works because we control both sides.

Decision: stay with the hrms-common approach until we onboard a
partner team that doesn't share our common module.

---

## 8. End-to-end / acceptance

No e2e suite today. When the frontend stabilizes:
- Cypress or Playwright running against `staging`
- One golden-path scenario per role (login → typical task → logout)
- Run nightly, not on every PR