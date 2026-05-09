# Attendance Service

**Port:** 8083 | **Schema:** hrms_attendance | **Owner:** Askar

## Responsibility
Check-in/out via face recognition (primary) or manual entry, work schedules, holiday calendar, fraud detection via AI, overtime tracking, daily/monthly summaries.

## Tables
- `attendance_records` — daily record per employee with check_in/out times, status, method (FACE/MANUAL/WEB/MOBILE), location, fraud_score
- `work_schedules` — configurable shifts (start/end time, late threshold)
- `holidays` — KZ public holidays (16 seeded for 2026, is_annual flag)
- `biometric_data` — enrollment status (method, embedding_path, photo_urls, enrolled_at)

## Endpoints (20)

```
# Face Recognition Check-in (ATTENDANCE_CHECKIN)
POST /v1/attendance/check-in/face          # Multipart: face photo → AI verifies → records attendance
POST /v1/attendance/check-out/face         # Same — captures face on exit
POST /v1/attendance/check-in               # Manual/web: {employeeId?, method?} — fallback when AI unavailable
POST /v1/attendance/check-out              # Manual/web check-out
GET  /v1/attendance/today                  # My status today

# Records
GET  /v1/attendance/records                # Own records (?from=&to=, paginated)
GET  /v1/attendance/records/employee/{id}  # Specific employee (ATTENDANCE_VIEW_*)
GET  /v1/attendance/records/department/{id} # Department view (?date=)
GET  /v1/attendance/records/daily          # Company-wide today (?date=)

# Manual Management (ATTENDANCE_MANAGE)
POST /v1/attendance/records                # Manual entry {employeeId, workDate, checkIn, checkOut, status}
PUT  /v1/attendance/records/{id}           # Correct record
POST /v1/attendance/records/bulk-absent    # Mark all no-shows {date}

# Summaries
GET  /v1/attendance/summary/employee/{id}  # ?year=&month=
GET  /v1/attendance/summary/department/{id}
GET  /v1/attendance/summary/company

# Holidays (ATTENDANCE_MANAGE)
GET  /v1/attendance/holidays               # ?year=
POST /v1/attendance/holidays
PUT  /v1/attendance/holidays/{id}
DELETE /v1/attendance/holidays/{id}

# Work Schedules (ATTENDANCE_MANAGE)
GET  /v1/attendance/schedules
POST /v1/attendance/schedules
PUT  /v1/attendance/schedules/{id}
```

## Face Recognition Check-in Flow

This is the primary check-in method. A tablet/kiosk at the office entrance captures the employee's face.

```
1. Employee approaches kiosk/tablet
2. Camera captures face image
3. Frontend sends image to POST /v1/attendance/check-in/face (multipart)
4. Attendance service forwards image to AI service:
   → POST /v1/ai/biometric/verify (multipart: face photo)
   → AI returns: {matched, employeeId, confidence, livenessScore}
5. If NOT matched:
   → Return 401 {error: "Face not recognized", reason: ai_response.reason}
   → If reason is "liveness_failed" → possible spoofing, log attempt
6. If matched (confidence > 0.85):
   → employeeId = ai_response.employeeId
   → Continue with normal check-in logic (steps 7-13)
7. Check no existing record for today → 400 "Already checked in"
8. Check if today is a holiday → reject "Today is a holiday: {name}"
9. Check if today is weekend (Sat/Sun) → reject
10. Get work schedule (default or department-specific)
11. Determine status:
    → check_in <= work_start + late_threshold → PRESENT
    → check_in > work_start + late_threshold → LATE
12. Check for suspicious behavioral patterns:
    → Get recent check-ins for this employee
    → If time_diff < 30min from last check-in, or unusual patterns:
      → Call AI: POST /v1/ai/attendance/fraud-detect {behavioral features}
      → If fraud_score > 0.65 → BLOCK, save fraud_score + flags, alert HR
      → If fraud_score 0.3-0.65 → ALLOW but save fraud_score for review
13. Save attendance_record with:
    → check_in_method = 'FACE'
    → fraud_score (if checked)
    → Return success with employee name + time
```

### Controller Implementation
```java
@PostMapping("/check-in/face")
@PreAuthorize("permitAll()") // Kiosk doesn't have user JWT — face IS the auth
public ResponseEntity<ApiResponse<AttendanceDtos.CheckInResponse>> checkInWithFace(
        @RequestParam("photo") MultipartFile photo) {

    // 1. Call AI service for face verification
    AiMlClient.VerifyResponse verification;
    try {
        verification = aiMlClient.verifyFace(photo);
    } catch (Exception e) {
        log.warn("AI service unavailable for face check-in: {}", e.getMessage());
        return ResponseEntity.status(503).body(
            ApiResponse.error("Face recognition unavailable. Use manual check-in."));
    }

    if (!verification.isMatched()) {
        return ResponseEntity.status(401).body(
            ApiResponse.error("Face not recognized: " + verification.getReason()));
    }

    // 2. Proceed with normal check-in using verified employeeId
    UUID employeeId = UUID.fromString(verification.getEmployeeId());
    var result = attendanceService.checkIn(employeeId, "FACE",
        verification.getConfidence(), null, null);

    return ResponseEntity.ok(ApiResponse.ok(result));
}

@PostMapping("/check-in")
@PreAuthorize("hasAuthority('ATTENDANCE_CHECKIN')")
public ResponseEntity<ApiResponse<AttendanceDtos.CheckInResponse>> checkIn(
        @AuthenticationPrincipal User currentUser,
        @RequestBody(required = false) AttendanceDtos.ManualCheckInRequest request) {
    // Fallback: manual/web check-in using JWT identity
    UUID employeeId = request != null && request.getEmployeeId() != null
        ? request.getEmployeeId()
        : currentUser.getEmployeeId();
    String method = request != null && request.getMethod() != null
        ? request.getMethod() : "WEB";

    var result = attendanceService.checkIn(employeeId, method, null, null, null);
    return ResponseEntity.ok(ApiResponse.ok(result));
}
```

### Service Implementation
```java
@Transactional
public CheckInResponse checkIn(UUID employeeId, String method,
                                Double faceConfidence,
                                Double locationLat, Double locationLng) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Almaty"));

    // Already checked in?
    if (recordRepo.existsByEmployeeIdAndWorkDate(employeeId, today)) {
        throw new BusinessException("Already checked in today");
    }

    // Holiday check
    holidayRepo.findByDate(today).ifPresent(h -> {
        throw new BusinessException("Today is a holiday: " + h.getName());
    });

    // Weekend check
    DayOfWeek dow = today.getDayOfWeek();
    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
        throw new BusinessException("Today is a weekend");
    }

    // Determine status
    LocalTime now = LocalTime.now(ZoneId.of("Asia/Almaty"));
    WorkSchedule schedule = scheduleRepo.findByIsDefaultTrue()
        .orElse(WorkSchedule.defaultSchedule());
    LocalTime threshold = schedule.getWorkStartTime()
        .plusMinutes(schedule.getLateThresholdMin());
    AttendanceStatus status = now.isAfter(threshold) ? LATE : PRESENT;

    // Behavioral fraud check (if face method — already past face match)
    Double fraudScore = null;
    String fraudFlags = null;
    if ("FACE".equals(method) || "BIOMETRIC".equals(method)) {
        var recent = recordRepo.findTopByEmployeeIdOrderByCreatedAtDesc(employeeId);
        if (recent.isPresent() && isSuspicious(recent.get(), today, now)) {
            try {
                var fraud = aiMlClient.detectFraud(buildFraudRequest(recent.get(), employeeId, now));
                fraudScore = fraud.getFraudProbability();
                fraudFlags = String.join(",", fraud.getFlags());
                if (fraud.isFraud()) {
                    // Save blocked record for audit
                    saveBlockedRecord(employeeId, today, method, fraudScore, fraudFlags);
                    throw new BusinessException("Check-in blocked: suspicious activity detected. Contact HR.");
                }
            } catch (BusinessException e) {
                throw e; // re-throw fraud block
            } catch (Exception e) {
                log.warn("Fraud detection unavailable: {}", e.getMessage());
                // Continue — fraud detection is non-critical
            }
        }
    }

    // Save record
    AttendanceRecord record = AttendanceRecord.builder()
        .employeeId(employeeId)
        .workDate(today)
        .checkIn(LocalDateTime.now(ZoneId.of("Asia/Almaty")))
        .status(status)
        .checkInMethod(method)
        .locationLat(locationLat != null ? BigDecimal.valueOf(locationLat) : null)
        .locationLng(locationLng != null ? BigDecimal.valueOf(locationLng) : null)
        .fraudScore(fraudScore != null ? BigDecimal.valueOf(fraudScore) : null)
        .fraudFlags(fraudFlags)
        .build();

    record = recordRepo.save(record);

    // Publish event
    rabbitTemplate.convertAndSend("hrms.events", "attendance.recorded",
        AttendanceRecordedEvent.builder()
            .employeeId(employeeId).workDate(today)
            .status(status.name()).build());

    return mapToCheckInResponse(record, faceConfidence);
}
```

## Two-Layer AI Protection

```
Layer 1: Face Recognition (identity verification)
├── Is this a real face? (liveness detection)
├── Whose face is this? (embedding comparison)
└── Confidence > 0.85? (threshold check)

Layer 2: Behavioral Fraud Detection (pattern analysis)
├── Same person checked in 15 min ago from different location?
├── Check-in at unusual hour for this employee?
├── Suspicious device/location pattern?
└── Isolation Forest anomaly score > 0.65 → BLOCK
```

Layer 1 runs on EVERY face check-in. Layer 2 only runs when suspicious patterns are detected (recent check-in exists, unusual timing, etc.) to avoid unnecessary AI calls.

## Events Published
- `AttendanceRecordedEvent` {employeeId, workDate, status, workedHours, method}
- `FraudAttemptDetectedEvent` {employeeId, fraudScore, flags, method, deviceId}

## Events Consumed
- `LeaveApprovedEvent` → mark leave dates as ON_LEAVE in attendance_records

## Feign Clients
- `employee-service` → verify employee exists, get department, get schedule
- `ai-ml-service` → POST /v1/ai/biometric/verify (face recognition)
- `ai-ml-service` → POST /v1/ai/attendance/fraud-detect (behavioral, non-critical try/catch)

## Environment Variables
```
DB_URL=jdbc:postgresql://postgres:5432/hrms?currentSchema=hrms_attendance
REDIS_HOST=redis
RABBITMQ_HOST=rabbitmq
AI_ML_SERVICE_URL=http://ai-ml-service:8086
EMPLOYEE_SERVICE_URL=http://employee-service:8082
```
