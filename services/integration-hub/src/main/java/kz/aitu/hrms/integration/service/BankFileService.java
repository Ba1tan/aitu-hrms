package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.EmployeeClient;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.EmployeeIinDto;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.service.bank.BankFileContext;
import kz.aitu.hrms.integration.service.bank.BankFileGenerator;
import kz.aitu.hrms.integration.service.bank.BankPaymentRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankFileService {

    private final List<BankFileGenerator> generators;
    private final PayrollClient payrollClient;
    private final EmployeeClient employeeClient;
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

        Map<UUID, EmployeeIinDto> bankByEmployee = fetchBankDetails(payslips);

        int missingIban = 0;
        List<BankPaymentRow> rows = new java.util.ArrayList<>(payslips.size());
        for (PayslipDetailDto p : payslips) {
            EmployeeIinDto emp = bankByEmployee.get(p.getEmployeeId());
            String iban = emp == null ? null : emp.getBankAccount();
            if (iban == null || iban.isBlank()) missingIban++;
            rows.add(new BankPaymentRow(
                    iban,
                    p.getEmployeeIin(),
                    p.getFullName(),
                    p.getNetSalary(),
                    emp == null ? null : emp.getBankName()));
        }
        if (missingIban > 0) {
            // Non-blocking: the accountant reviews the file, but flag it loudly.
            log.warn("Bank file for period {}: {} of {} employees have no bank account on file",
                    periodId, missingIban, payslips.size());
        }

        BankFileContext ctx = buildContext(payslips);
        generator.write(rows, ctx, out);

        auditPublisher.audit("BANK_FILE", "BANK_FILE", periodId, null,
                Map.of("periodId", periodId, "format", format,
                        "rows", rows.size(), "missingIban", missingIban));
    }

    /** One employee-service call per distinct employee (no bulk endpoint yet). */
    private Map<UUID, EmployeeIinDto> fetchBankDetails(List<PayslipDetailDto> payslips) {
        List<UUID> ids = payslips.stream()
                .map(PayslipDetailDto::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, EmployeeIinDto> map = new HashMap<>();
        ids.parallelStream().forEach(id -> {
            try {
                EmployeeIinDto dto = employeeClient.getById(id);
                if (dto != null) {
                    synchronized (map) {
                        map.put(id, dto);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch bank details for employee {}: {}", id, e.getMessage());
            }
        });
        return map;
    }

    private BankFileContext buildContext(List<PayslipDetailDto> payslips) {
        CompanySettingSnapshot company = settings.snapshot();
        String knp = settings.getOrDefault("integration.bank_knp", "010");
        String kbe = settings.getOrDefault("integration.bank_kbe", "19");

        int year = 0, month = 0;
        if (!payslips.isEmpty() && payslips.get(0).getPeriod() != null) {
            var period = payslips.get(0).getPeriod();
            year = period.getYear() == null ? 0 : period.getYear();
            month = period.getMonth() == null ? 0 : period.getMonth();
        }

        String purpose = settings.getOrDefault("integration.bank_payment_purpose", "");
        if (purpose.isBlank()) {
            purpose = month > 0
                    ? String.format("Заработная плата за %02d.%d", month, year)
                    : "Заработная плата";
        }
        return new BankFileContext(company.bin(), company.name(), knp, kbe, purpose, year, month);
    }
}
