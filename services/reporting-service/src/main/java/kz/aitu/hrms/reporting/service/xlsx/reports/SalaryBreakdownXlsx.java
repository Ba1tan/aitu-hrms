package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.PayrollClient;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.client.dto.PayrollPeriodDto;
import kz.aitu.hrms.reporting.client.dto.PayslipDto;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SalaryBreakdownXlsx {

    private final PayrollClient payrollClient;

    public void write(UUID departmentId, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Зарплатная ведомость");
            w.header("Сотрудник", "Фамилия", "Период", "Брутто", "ОПВ", "ВОСМС", "ИПН", "Нетто");

            PayrollPeriodDto period = null;
            try { period = payrollClient.getLatestPeriod(); } catch (Exception ignored) {}
            if (period == null) {
                PageResponse<PayrollPeriodDto> pg = payrollClient.listPeriods(0, 1);
                if (pg != null && pg.getContent() != null && !pg.getContent().isEmpty()) {
                    period = pg.getContent().get(0);
                }
            }
            if (period == null) {
                w.writeTo(out);
                return;
            }

            int page = 0;
            PageResponse<PayslipDto> resp;
            do {
                resp = payrollClient.listPayslips(period.getId(), page++, 200);
                if (resp == null || resp.getContent() == null) break;
                for (PayslipDto s : resp.getContent()) {
                    w.row(s.getEmployeeFirstName(), s.getEmployeeLastName(), s.getPeriodName(),
                            s.getGrossSalary(), s.getOpv(), s.getVosms(), s.getIpn(), s.getNetSalary());
                }
            } while (!resp.isLast());
            w.writeTo(out);
        }
    }
}
