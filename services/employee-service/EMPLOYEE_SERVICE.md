# Employee Service

**Port:** 8082 | **Schema:** hrms_employee | **Owner:** Askar

## Responsibility
Employee lifecycle (hire→terminate), departments, positions, org chart, salary history, documents, emergency contacts, biometric enrollment (face photos), bulk import/export.

## Tables (in hrms_employee schema)
- `employees` — profiles with IIN, salary, dept/position/manager FKs, disability_group, resident/pensioner flags
- `departments` — org units with parent_id (tree), manager_id FK to employees
- `positions` — job titles with salary bands, dept FK
- `salary_history` — every salary change with reason, effective date, approver
- `employee_documents` — uploaded files (contracts, IDs, certificates) with type, expiry
- `emergency_contacts` — next-of-kin with phone, relationship, is_primary
- `biometric_data` — enrollment status: method (FACE/FINGERPRINT), embedding_path, photo_urls, enrolled_at

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

# Biometric Enrollment (Face Recognition)
POST   /v1/employees/{id}/biometric/enroll                  EMPLOYEE_BIOMETRIC — multipart: 3-5 face photos, AI-first then persist
GET    /v1/employees/{id}/biometric/status                  EMPLOYEE_VIEW_OWN | EMPLOYEE_VIEW_ALL — {enrolled, method, enrolledAt, photoUrls}
DELETE /v1/employees/{id}/biometric                         EMPLOYEE_BIOMETRIC — soft-delete row + call AI to drop embedding
GET    /v1/employees/{id}/biometric/photos/{filename}       EMPLOYEE_VIEW_OWN | EMPLOYEE_VIEW_ALL — download stored enrollment photo (filename whitelisted against photoUrls)

# Organization
GET    /v1/employees/org-chart
GET    /v1/employees/org-chart/{id}
POST   /v1/employees/import                        Bulk import XLSX
GET    /v1/employees/export                        Bulk export XLSX

# Departments
POST   /v1/departments
GET    /v1/departments
GET    /v1/departments/{id}
PUT    /v1/departments/{id}
DELETE /v1/departments/{id}

# Positions
POST   /v1/positions
GET    /v1/positions                               ?departmentId=
GET    /v1/positions/{id}
PUT    /v1/positions/{id}
DELETE /v1/positions/{id}
```

## Biometric Enrollment Flow

HR enrolls an employee's face through the employee detail page.

```
1. HR navigates to Employee Detail → clicks "Enroll Biometric"
2. Frontend activates webcam → captures 3-5 photos from different angles
   OR HR uploads 3-5 pre-taken photos
3. Frontend sends: POST /v1/employees/{id}/biometric/enroll (multipart: photos[])
4. Employee-service (AI-first, no partial state):
   a. Validates employee exists and is not TERMINATED
   b. Validates 3-5 photos with allowed content types (image/jpeg, image/png)
   c. Forwards photos to AI service: POST /v1/ai/biometric/enroll/{employeeId} (multipart)
      → AI extracts face embeddings, averages, stores .npy file
      → Returns {enrolled, photosProcessed, embeddingPath}
      → If AI fails, throw — no photos ever written to disk
   d. Only after AI success, saves photos to /data/hrms/uploads/employees/{id}/biometric/
   e. Upserts record in biometric_data table:
      → method = FACE
      → embedding_path = returned by AI
      → photo_urls = ['/employees/{id}/biometric/face_1.jpg', ...]
      → enrolled_at = now(), enrolled_by = currentUser.userId
   f. Returns BiometricStatusResponse {enrolled, method, enrolledAt, photoUrls}
5. Employee can now check in via face at the kiosk
```

### Controller
```java
@PostMapping("/{id}/biometric/enroll")
@PreAuthorize("hasAuthority('EMPLOYEE_BIOMETRIC')")
public ResponseEntity<ApiResponse<BiometricStatusResponse>> enrollBiometric(
        @PathVariable UUID id,
        @RequestParam("photos") List<MultipartFile> photos) {

    if (photos.size() < 3 || photos.size() > 5) {
        throw new BusinessException("3-5 photos required for enrollment");
    }

    var result = employeeService.enrollBiometric(id, photos);
    return ResponseEntity.status(201).body(ApiResponse.created(result));
}

@GetMapping("/{id}/biometric/status")
@PreAuthorize("hasAuthority('EMPLOYEE_VIEW_OWN') or hasAuthority('EMPLOYEE_VIEW_ALL')")
public ResponseEntity<ApiResponse<BiometricStatusResponse>> getBiometricStatus(
        @PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(employeeService.getBiometricStatus(id)));
}

@DeleteMapping("/{id}/biometric")
@PreAuthorize("hasAuthority('EMPLOYEE_BIOMETRIC')")
public ResponseEntity<ApiResponse<Void>> deleteBiometric(@PathVariable UUID id) {
    employeeService.deleteBiometric(id);
    return ResponseEntity.ok(ApiResponse.ok("Biometric enrollment removed", null));
}
```

### Service
```java
@Transactional
public BiometricStatusResponse enrollBiometric(UUID employeeId, List<MultipartFile> photos) {
    Employee emp = findOrThrow(employeeId);
    if (emp.getStatus() == EmploymentStatus.TERMINATED) {
        throw new BusinessException("Cannot enroll terminated employee");
    }

    // 1. Save photos to disk
    List<String> photoUrls = new ArrayList<>();
    Path biometricDir = Paths.get(storagePath, "employees", employeeId.toString(), "biometric");
    Files.createDirectories(biometricDir);
    for (int i = 0; i < photos.size(); i++) {
        String filename = "face_" + (i + 1) + ".jpg";
        Path dest = biometricDir.resolve(filename);
        photos.get(i).transferTo(dest);
        photoUrls.add("/employees/" + employeeId + "/biometric/" + filename);
    }

    // 2. Call AI service to create embedding
    try {
        aiMlClient.enrollFace(employeeId.toString(), photos);
    } catch (Exception e) {
        throw new BusinessException("Face enrollment failed: " + e.getMessage());
    }

    // 3. Save to biometric_data table
    BiometricData bio = biometricRepo.findByEmployeeId(employeeId)
        .orElse(new BiometricData());
    bio.setEmployeeId(employeeId);
    bio.setMethod("FACE");
    bio.setEmbeddingPath("/data/hrms/ai-models/embeddings/" + employeeId + ".npy");
    bio.setPhotoUrls(photoUrls.toArray(new String[0]));
    bio.setEnrolledAt(LocalDateTime.now());
    bio.setEnrolledBy(getCurrentUserId());
    bio.setActive(true);
    biometricRepo.save(bio);

    return new BiometricStatusResponse(true, "FACE", bio.getEnrolledAt());
}

@Transactional
public void deleteBiometric(UUID employeeId) {
    biometricRepo.findByEmployeeId(employeeId).ifPresent(bio -> {
        bio.setActive(false);
        biometricRepo.save(bio);
    });
    // Tell AI service to remove embedding
    try {
        aiMlClient.deleteEnrollment(employeeId.toString());
    } catch (Exception e) {
        log.warn("Failed to delete AI embedding: {}", e.getMessage());
    }
}
```

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
    deleteBiometric(id);

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
- `ai-ml-service` → POST /v1/ai/biometric/enroll, DELETE /v1/ai/biometric/{id}

## Docker Volume
```yaml
employee-service:
  volumes:
    - hrms-uploads:/data/hrms/uploads    # read/write photos + documents
```
