# Employee Service

**Port:** 8082 | **Schema:** hrms_employee | **Owner:** Askar

## Responsibility
Employee lifecycle (hire→terminate), departments, positions, org chart, salary history, documents, emergency contacts, bulk import/export.

## Tables (in hrms_employee schema)
- `employees` — profiles with IIN, salary, dept/position/manager FKs, disability_group, resident/pensioner flags
- `departments` — org units with parent_id (tree), manager_id FK to employees
- `positions` — job titles with salary bands, dept FK
- `salary_history` — every salary change with reason, effective date, approver
- `employee_documents` — uploaded files (contracts, IDs, certificates) with type, expiry
- `emergency_contacts` — next-of-kin with phone, relationship, is_primary

## File Storage
```
/data/hrms/uploads/employees/{employeeId}/
├── photo/
│   └── profile.jpg                    # Profile photo
├── documents/
│   ├── contract_2026.pdf
│   └── id_card.jpg
└── biometric/
    ├── face_1.jpg                     # Enrollment photos (3-5 angles)
    ├── face_2.jpg
    └── face_3.jpg
```

Config: `app.storage.base-path=/data/hrms/uploads` in application.yml

## Endpoints (38)

```
# Employees
POST   /v1/employees                              Create (auto-generates EMP-YYYYMM-NNN)
GET    /v1/employees                              List (?search=&departmentId=&status=&type=&page=&size=)
GET    /v1/employees/{id}                         Detail
PUT    /v1/employees/{id}                         Update
PATCH  /v1/employees/{id}/status                  Change status {status, reason}
DELETE /v1/employees/{id}                         Soft delete

# Onboarding & Offboarding
POST   /v1/employees/{id}/create-account          Publish EmployeeCreatedEvent → user-service creates account
POST   /v1/employees/{id}/terminate               {terminationDate, reason} → TERMINATED, delete face enrollment

# Salary History
GET    /v1/employees/{id}/salary-history
POST   /v1/employees/{id}/salary-change           {newSalary, effectiveDate, reason}

# Documents
GET    /v1/employees/{id}/documents
POST   /v1/employees/{id}/documents               Multipart: file + documentType + expiryDate?
GET    /v1/employees/{id}/documents/{docId}/download
DELETE /v1/employees/{id}/documents/{docId}

# Emergency Contacts
GET    /v1/employees/{id}/emergency-contacts
POST   /v1/employees/{id}/emergency-contacts
PUT    /v1/employees/{id}/emergency-contacts/{cId}
DELETE /v1/employees/{id}/emergency-contacts/{cId}


## Termination — Auto-Cleanup

When an employee is terminated, also clean up biometric:
```java
@Transactional
public EmployeeResponse terminate(UUID id, TerminateRequest request) {
    Employee emp = findOrThrow(id);
    emp.setStatus(EmploymentStatus.TERMINATED);
    emp.setTerminationDate(request.getTerminationDate());
    emp.setTerminationReason(request.getReason());
    employeeRepo.save(emp);

    // Delete face enrollment

    // Publish event
    rabbitTemplate.convertAndSend("hrms.events", "employee.terminated",
        EmployeeTerminatedEvent.builder()
            .employeeId(id)
            .terminationDate(request.getTerminationDate())
            .reason(request.getReason())
            .build());

    return mapToResponse(emp);
}
```

## Data-Level Access Control
```java
if (hasAuthority("EMPLOYEE_VIEW_ALL"))  → return all
if (hasAuthority("EMPLOYEE_VIEW_TEAM")) → return WHERE manager_id = currentUser.employeeId
if (hasAuthority("EMPLOYEE_VIEW_OWN"))  → return WHERE id = currentUser.employeeId
```

## IIN Validation
```java
public static boolean isValidIIN(String iin) {
    if (iin == null || !iin.matches("\\d{12}")) return false;
    int[] w1 = {1,2,3,4,5,6,7,8,9,10,11};
    int[] w2 = {3,4,5,6,7,8,9,10,11,1,2};
    int[] d = iin.chars().map(c -> c - '0').toArray();
    int s = IntStream.range(0,11).map(i -> d[i]*w1[i]).sum() % 11;
    if (s == 10) s = IntStream.range(0,11).map(i -> d[i]*w2[i]).sum() % 11;
    return s == d[11];
}
```

## Employee Number Generation
```java
String number = String.format("EMP-%s-%03d",
    YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM")),
    employeeRepository.countByDeletedFalse() + 1);
```

## Events Published
- `EmployeeCreatedEvent` {employeeId, name, email, salary, departmentId}
- `EmployeeTerminatedEvent` {employeeId, terminationDate, reason}
- `SalaryChangedEvent` {employeeId, previousSalary, newSalary, effectiveDate}

## Events Consumed
None — employee-service is a leaf node.

## Feign Clients

## Docker Volume
```yaml
employee-service:
  volumes:
    - hrms-uploads:/data/hrms/uploads    # read/write photos + documents
```
