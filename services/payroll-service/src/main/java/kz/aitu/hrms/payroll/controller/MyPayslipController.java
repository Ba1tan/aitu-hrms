package kz.aitu.hrms.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.payroll.dto.PayslipDtos;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "My Payslips", description = "Employee self-service payslips")
@RestController
@RequestMapping("/v1/payroll/my-payslips")
@RequiredArgsConstructor
public class MyPayslipController {

    private final PayslipService payslipService;
    private final PayslipPdfService pdfService;

    @Operation(summary = "List my payslips, newest period first")
    @GetMapping
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW_OWN')")
    public ResponseEntity<ApiResponse<Page<PayslipDtos.Response>>> list(
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(payslipService.myPayslips(pageable)));
    }

    @Operation(summary = "My payslip for a specific period")
    @GetMapping("/period/{periodId}")
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW_OWN')")
    public ResponseEntity<ApiResponse<PayslipDtos.Response>> forPeriod(@PathVariable UUID periodId) {
        return ResponseEntity.ok(ApiResponse.ok(payslipService.myPayslipForPeriod(periodId)));
    }

    @Operation(summary = "Download my payslip as PDF")
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW_OWN')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        payslipService.requireViewablePayslip(id);
        byte[] body = pdfService.render(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}