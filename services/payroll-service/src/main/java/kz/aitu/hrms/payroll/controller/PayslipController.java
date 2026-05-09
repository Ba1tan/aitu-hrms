package kz.aitu.hrms.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.payroll.dto.PayslipDtos;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.service.PayslipPdfService;
import kz.aitu.hrms.payroll.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Payslips", description = "Per-period payslip listing, adjustment, recalculation and PDF download")
@RestController
@RequestMapping("/v1/payroll")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;
    private final PayslipPdfService pdfService;

    @Operation(summary = "All payslips for a period (paginated)")
    @GetMapping("/periods/{id}/payslips")
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<Page<PayslipDtos.Response>>> byPeriod(
            @PathVariable UUID id,
            @RequestParam(required = false) PayslipStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                payslipService.findByPeriod(id, status, search, pageable)));
    }

    @Operation(summary = "Single payslip detail")
    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<PayslipDtos.Response>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payslipService.detail(id)));
    }

    @Operation(summary = "Adjust a DRAFT or FLAGGED payslip")
    @PatchMapping("/payslips/{id}/adjust")
    @PreAuthorize("hasAuthority('PAYSLIP_ADJUST')")
    public ResponseEntity<ApiResponse<PayslipDtos.Response>> adjust(
            @PathVariable UUID id,
            @Valid @RequestBody PayslipDtos.AdjustRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(payslipService.adjust(id, req)));
    }

    @Operation(summary = "Recalculate a payslip from current canonical inputs")
    @PostMapping("/payslips/{id}/recalculate")
    @PreAuthorize("hasAuthority('PAYSLIP_ADJUST')")
    public ResponseEntity<ApiResponse<PayslipDtos.Response>> recalculate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payslipService.recalculate(id)));
    }

    @Operation(summary = "Approve a FLAGGED payslip after manual review")
    @PostMapping("/payslips/{id}/approve-flagged")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public ResponseEntity<ApiResponse<PayslipDtos.Response>> approveFlagged(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Flagged payslip approved", payslipService.approveFlagged(id)));
    }

    @Operation(summary = "Download payslip as PDF (HR/finance)")
    @GetMapping("/payslips/{id}/pdf")
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        // require visibility check before rendering
        payslipService.requireViewablePayslip(id);
        byte[] body = pdfService.render(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}