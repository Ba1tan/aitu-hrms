package kz.aitu.hrms.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.payroll.batch.PayrollBatchService;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import kz.aitu.hrms.payroll.service.PayrollPeriodService;
import kz.aitu.hrms.payroll.service.PayslipGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Payroll Periods", description = "Create, list, generate, approve and lock payroll periods")
@RestController
@RequestMapping("/v1/payroll/periods")
@RequiredArgsConstructor
public class PayrollPeriodController {

    private final PayrollPeriodService periodService;
    private final PayslipGenerationService generationService;
    private final PayrollBatchService batchService;

    @Operation(summary = "Create a new payroll period")
    @PostMapping
    @PreAuthorize("hasAuthority('PAYROLL_PROCESS')")
    public ResponseEntity<ApiResponse<PeriodDtos.Response>> create(
            @Valid @RequestBody PeriodDtos.CreateRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(periodService.create(req)));
    }

    @Operation(summary = "List payroll periods (paginated, newest first)")
    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<Page<PeriodDtos.Response>>> list(
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(periodService.list(pageable)));
    }

    @Operation(summary = "Period detail with totals summary")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<PeriodDtos.Response>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(periodService.detail(id)));
    }

    @Operation(summary = "Generate payslips for the period (sync or async via Spring Batch)")
    @PostMapping("/{id}/generate")
    @PreAuthorize("hasAuthority('PAYROLL_PROCESS')")
    public ResponseEntity<ApiResponse<PeriodDtos.GenerateResponse>> generate(
            @PathVariable UUID id,
            @RequestBody(required = false) PeriodDtos.GenerateRequest req) {
        int targetCount = generationService.resolveEmployees(req).size();
        Boolean asyncOverride = req == null ? null : req.getAsync();
        boolean async = generationService.shouldUseBatch(targetCount, asyncOverride);

        if (async) {
            Long jobId = batchService.startGenerateJob(id);
            PeriodDtos.GenerateResponse resp = PeriodDtos.GenerateResponse.builder()
                    .async(true)
                    .jobId(jobId)
                    .build();
            return ResponseEntity.accepted().body(ApiResponse.ok("Payslip generation started", resp));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Payslips generated", generationService.generate(id, req)));
    }

    @Operation(summary = "Approve a COMPLETED period")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public ResponseEntity<ApiResponse<PeriodDtos.Response>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Period approved", periodService.approve(id)));
    }

    @Operation(summary = "Mark an APPROVED period as paid")
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAuthority('PAYROLL_PAY')")
    public ResponseEntity<ApiResponse<PeriodDtos.Response>> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Period marked as paid", periodService.markPaid(id)));
    }

    @Operation(summary = "Lock a PAID period (immutable archive)")
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<PeriodDtos.Response>> lock(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Period locked", periodService.lock(id)));
    }
}