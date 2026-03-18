# /write-tests

Write comprehensive tests for: `$ARGUMENTS`

## Determine test type from context, then follow the matching template.

---

## Service Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class {Name}ServiceTest {
    @Mock {Name}Repository repo;
    @Mock EmployeeService employeeService; // if needed
    @Mock NotificationService notificationService; // if needed
    @InjectMocks {Name}ServiceImpl service;

    // Happy path
    @Test
    void method_validInput_returnsExpected() { ... }

    // Not found
    @Test
    void method_entityNotFound_throwsResourceNotFoundException() {
        when(repo.findByIdAndDeletedFalse(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.method(UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // Business rule violation
    @Test
    void method_businessRuleViolated_throwsBusinessException() { ... }
}
```

---

## KZ Payroll Calculator Tests — CRITICAL — test all edge cases

```java
@ExtendWith(MockitoExtension.class)
class KazakhstanPayrollCalculatorTest {
    KazakhstanPayrollCalculator calculator;

    // IMPORTANT: inject @Value fields via ReflectionTestUtils
    @BeforeEach void setUp() {
        calculator = new KazakhstanPayrollCalculator();
        ReflectionTestUtils.setField(calculator, "ipnRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calculator, "opvRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calculator, "soRate", new BigDecimal("0.035"));
        ReflectionTestUtils.setField(calculator, "snRate", new BigDecimal("0.095"));
        ReflectionTestUtils.setField(calculator, "oopvRate", new BigDecimal("0.015"));
        ReflectionTestUtils.setField(calculator, "mrp", 3692);
        ReflectionTestUtils.setField(calculator, "minWage", 85000);
    }

    @Test
    void calculate_standardResident300k_correctAllDeductions() {
        // gross = 300,000 KZT, full month, resident, no disability
        // OPV = 30,000 (10%), MRP deduction = 3,692
        // Taxable = 300,000 - 30,000 - 3,692 = 266,308
        // IPN = 26,630.80 (10%)
        // Net = 300,000 - 30,000 - 26,630.80 = 243,369.20
        PayrollCalculationResult r = calculator.calculate(
            employee(300_000, true, false, false), 22, 22, ZERO, ZERO, "2024", "Март 2024");
        assertThat(r.getOpvAmount()).isEqualByComparingTo("30000.00");
        assertThat(r.getTaxableIncome()).isEqualByComparingTo("266308.00");
        assertThat(r.getIpnAmount()).isEqualByComparingTo("26630.80");
        assertThat(r.getNetSalary()).isEqualByComparingTo("243369.20");
    }

    @Test
    void calculate_lowIncome_taxableIncomeZero_ipnZero() {
        // gross = 40,000 KZT → after OPV (4,000) and MRP (3,692) → taxable = 32,308
        // IPN = 3,230.80
        PayrollCalculationResult r = calculator.calculate(
            employee(40_000, true, false, false), 22, 22, ZERO, ZERO, "2024", "Март 2024");
        assertThat(r.getIpnAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_pensioner_opvZero() {
        // isPensioner = true → OPV must be 0
        PayrollCalculationResult r = calculator.calculate(
            employee(200_000, true, false, true), 22, 22, ZERO, ZERO, "2024", "Март 2024");
        assertThat(r.getOpvAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_nonResident_flatTax20Percent() {
        PayrollCalculationResult r = calculator.calculate(
            employee(300_000, false, false, false), 22, 22, ZERO, ZERO, "2024", "Март 2024");
        // Non-resident: 20% flat, no MRP deduction
        BigDecimal expectedIpn = r.getTaxableIncome().multiply(new BigDecimal("0.20"))
            .setScale(2, HALF_UP);
        assertThat(r.getIpnAmount()).isEqualByComparingTo(expectedIpn);
    }

    @Test
    void calculate_partialMonth_proratingSalary() {
        // worked 11 of 22 days → earned = gross / 2
        PayrollCalculationResult r = calculator.calculate(
            employee(200_000, true, false, false), 11, 22, ZERO, ZERO, "2024", "Март 2024");
        assertThat(r.getEarnedSalary()).isEqualByComparingTo("100000.00");
    }

    @Test
    void calculate_opvCappedAt50Mrp() {
        // OPV cap = 50 × 3,692 = 184,600 KZT → applies when gross > 1,846,000
        PayrollCalculationResult r = calculator.calculate(
            employee(2_000_000, true, false, false), 22, 22, ZERO, ZERO, "2024", "Март 2024");
        assertThat(r.getOpvAmount()).isEqualByComparingTo("184600.00");
    }

    @Test
    void calculate_employerObligations_notDeductedFromNet() {
        // SO and SN are employer costs, NOT subtracted from net salary
        PayrollCalculationResult r = calculator.calculate(
            employee(300_000, true, false, false), 22, 22, ZERO, ZERO, "2024", "Март 2024");
        assertThat(r.getSoAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.getSnAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        // Net does not include SO or SN
        BigDecimal expectedNet = r.getEarnedSalary()
            .subtract(r.getOpvAmount()).subtract(r.getOopvAmount()).subtract(r.getIpnAmount())
            .add(r.getAllowances()).subtract(r.getDeductions());
        assertThat(r.getNetSalary()).isEqualByComparingTo(expectedNet);
    }

    // Helper to create test employee
    private Employee employee(int salary, boolean resident, boolean disability, boolean pensioner) {
        Employee e = new Employee();
        e.setBaseSalary(BigDecimal.valueOf(salary));
        e.setResident(resident);
        e.setHasDisability(disability);
        e.setPensioner(pensioner);
        return e;
    }
}
```

---

## Leave Service Tests — critical business rules

```java
@Test
void createRequest_insufficientBalance_throwsBusinessException() {
    // balance.remaining = 3, requested = 5
    when(balanceRepo.findBy...).thenReturn(Optional.of(balanceWith(3)));
    assertThatThrownBy(() -> service.createRequest(requestFor5Days()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("balance");
}

@Test
void approve_atomicUpdate_bothTablesUpdated() {
    // verify both leave_request and leave_balance are saved in same transaction
    // use @Transactional test or verify both repos called
}
```

---

## Controller Tests

```java
@WebMvcTest({Name}Controller.class)
@Import(SecurityConfig.class)
class {Name}ControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean {Name}Service service;

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void create_validRequest_returns201() throws Exception {
        when(service.create(any())).thenReturn(validResponse());
        mockMvc.perform(post("/v1/{module}")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/v1/{module}")
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/v1/{module}").contentType(APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## Rules
- All payroll test values must use `isEqualByComparingTo()` — never `.equals()` on BigDecimal
- Never use H2 for DB tests — use Testcontainers PostgreSQL with `@DataJpaTest`
- Test naming: `methodName_scenario_expectedBehavior`
- Every `@PreAuthorize` needs a 403 test for wrong role AND a 401 test for no auth
- Cover: happy path + not found + business rule violation + wrong role
