package kz.aitu.hrms.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.payroll.batch.PayrollBatchService;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payroll Jobs", description = "Spring Batch job status for async payslip generation")
@RestController
@RequestMapping("/v1/payroll/jobs")
@RequiredArgsConstructor
public class PayrollJobController {

    private final PayrollBatchService batchService;

    @Operation(summary = "Status of a payslip generation batch job")
    @GetMapping("/{jobId}/status")
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<PeriodDtos.JobStatus>> status(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.ok(batchService.getStatus(jobId)));
    }
}