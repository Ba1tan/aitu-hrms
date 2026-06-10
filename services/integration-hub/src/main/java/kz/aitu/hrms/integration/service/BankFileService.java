package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.service.bank.BankFileGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankFileService {

    private final List<BankFileGenerator> generators;
    private final PayrollClient payrollClient;
    private final SettingsService settings;
    private final AuditPublisher auditPublisher;

    public void generate(UUID periodId, String format, OutputStream out) throws IOException {
        BankFileGenerator generator = generators.stream()
                .filter(g -> g.format().equals(format))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Unsupported bank format: " + format));

        List<PayslipDetailDto> payslips = payrollClient
                .listPayslipsForPeriod(periodId, 0, 5000)
                .getContent();

        CompanySettingSnapshot snapshot = settings.snapshot();
        generator.write(payslips, snapshot, out);

        auditPublisher.audit("BANK_FILE", "BANK_FILE", periodId, null,
                java.util.Map.of("periodId", periodId, "format", format, "payslips", payslips.size()));
    }
}
