package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.PayrollClient;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.client.dto.PayslipDto;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayrollSummaryXlsx {

    private final PayrollClient payrollClient;

    public void write(UUID periodId, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Payroll Summary");
            w.header("Сотрудник", "Оклад брутто", "ОПВ", "ВОСМС", "ИПН", "Оклад нетто");
            int page = 0;
            PageResponse<PayslipDto> resp;
            do {
                resp = payrollClient.listPayslips(periodId, page++, 200);
                if (resp == null || resp.getContent() == null) break;
                for (PayslipDto s : resp.getContent()) {
                    w.row(s.getEmployeeName(),
                            s.getGrossSalary(), s.getOpvAmount(), s.getVosmsAmount(),
                            s.getIpnAmount(), s.getNetSalary());
                }
            } while (!resp.isLast());
            w.writeTo(out);
        }
    }
}
