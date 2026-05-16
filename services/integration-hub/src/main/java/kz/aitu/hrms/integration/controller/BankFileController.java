package kz.aitu.hrms.integration.controller;

import jakarta.servlet.http.HttpServletResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.service.BankFileService;
import kz.aitu.hrms.integration.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/v1/integration")
@RequiredArgsConstructor
public class BankFileController {

    private final BankFileService bankFileService;
    private final SettingsService settingsService;

    @GetMapping("/bank-file/{periodId}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_SETTINGS', 'PAYROLL_APPROVE')")
    public void downloadBankFile(
            @PathVariable UUID periodId,
            @AuthenticationPrincipal AuthenticatedUser me,
            HttpServletResponse response) throws IOException {
        String format = settingsService.getOrDefault("integration.bank_default_format", "KASPI_TSV");
        String filename = "payment-" + periodId + "." + fileExtension(format);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        bankFileService.generate(periodId, format, response.getOutputStream());
        response.flushBuffer();
    }

    private String fileExtension(String format) {
        return switch (format) {
            case "KASPI_TSV"  -> "tsv";
            case "HALYK_MT940" -> "mt940";
            case "JUSAN_CSV"  -> "csv";
            default -> "dat";
        };
    }
}
