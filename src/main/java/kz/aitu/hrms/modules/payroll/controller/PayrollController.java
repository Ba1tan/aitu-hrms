package kz.aitu.hrms.modules.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.response.ApiResponse;
import kz.aitu.hrms.modules.auth.entity.User;
import kz.aitu.hrms.modules.payroll.dto.PayrollDtos;
import kz.aitu.hrms.modules.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Payroll", description = "Payroll periods, payslip generation and management")
@RestController
@RequestMapping("/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;


    // PERIODS

    @Operation(summary = "Create a new payroll period")
    @PostMapping("/periods")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollDtos.PeriodResponse>> createPeriod(
            @Valid @RequestBody PayrollDtos.CreatePeriodRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(payrollService.createPeriod(request)));
    }

    @Operation(summary = "List all payroll periods")
    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'ACCOUNTANT', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<PayrollDtos.PeriodResponse>>> getPeriods(
            @PageableDefault(size = 12, sort = "year") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPeriods(pageable)));
    }

    @Operation(summary = "Get a specific payroll period")
    @GetMapping("/periods/{periodId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'ACCOUNTANT', 'MANAGER')")
    public ResponseEntity<ApiResponse<PayrollDtos.PeriodResponse>> getPeriod(
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPeriod(periodId)));
    }


    // PAYSLIP GENERATION


    @Operation(summary = "Generate payslips for all (or selected) active employees in a period")
    @PostMapping("/periods/{periodId}/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollDtos.GeneratePayslipsResponse>> generatePayslips(
            @PathVariable UUID periodId,
            @RequestBody(required = false) PayrollDtos.GeneratePayslipsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Payslips generated successfully",
                payrollService.generatePayslips(periodId, request)));
    }


    // PAYSLIP MANAGEMENT

    @Operation(summary = "Get all payslips for a period")
    @GetMapping("/periods/{periodId}/payslips")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<PayrollDtos.PayslipResponse>>> getPayslipsByPeriod(
            @PathVariable UUID periodId,
            @PageableDefault(size = 50, sort = "employee.lastName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.getPayslipsByPeriod(periodId, pageable)));
    }

    @Operation(summary = "Get a specific payslip by ID")
    @GetMapping("/payslips/{payslipId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PayrollDtos.PayslipResponse>> getPayslip(
            @PathVariable UUID payslipId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPayslip(payslipId)));
    }

    @Operation(summary = "Adjust a DRAFT payslip (allowances, deductions, worked days)")
    @PatchMapping("/payslips/{payslipId}/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollDtos.PayslipResponse>> adjustPayslip(
            @PathVariable UUID payslipId,
            @Valid @RequestBody PayrollDtos.AdjustPayslipRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.adjustPayslip(payslipId, request)));
    }


    // STATUS TRANSITIONS

    @Operation(summary = "Approve a period — moves PROCESSING → APPROVED, approves all DRAFT payslips")
    @PostMapping("/periods/{periodId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<PayrollDtos.PeriodResponse>> approvePeriod(
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Period approved successfully",
                payrollService.approvePeriod(periodId)));
    }

    @Operation(summary = "Mark a period as paid — moves APPROVED → PAID")
    @PostMapping("/periods/{periodId}/mark-paid")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PayrollDtos.PeriodResponse>> markPeriodPaid(
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Period marked as paid",
                payrollService.markPeriodPaid(periodId)));
    }

    @Operation(summary = "Lock a period — moves PAID → LOCKED (immutable archive)")
    @PostMapping("/periods/{periodId}/lock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PayrollDtos.PeriodResponse>> lockPeriod(
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Period locked successfully",
                payrollService.lockPeriod(periodId)));
    }


    // EMPLOYEE SELF-SERVICE

    @Operation(summary = "Get my payslips (authenticated employee)")
    @GetMapping("/my-payslips")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PayrollDtos.PayslipResponse>>> getMyPayslips(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 12) Pageable pageable) {

        UUID employeeId = resolveEmployeeId(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getMyPayslips(employeeId, pageable)));
    }

    @Operation(summary = "Get my payslip for a specific period")
    @GetMapping("/my-payslips/period/{periodId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PayrollDtos.PayslipResponse>> getMyPayslipForPeriod(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID periodId) {

        UUID employeeId = resolveEmployeeId(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.getMyPayslipForPeriod(employeeId, periodId)));
    }

    // HELPERS

    private UUID resolveEmployeeId(User user) {
        if (user.getEmployeeId() == null) {
            throw new kz.aitu.hrms.common.exception.BusinessException(
                    "Your account is not linked to an employee profile. Contact HR.");
        }
        return user.getEmployeeId();
    }
}
