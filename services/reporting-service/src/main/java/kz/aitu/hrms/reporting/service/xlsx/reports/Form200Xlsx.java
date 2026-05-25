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
import java.util.List;

@Component
@RequiredArgsConstructor
public class Form200Xlsx {

    private final PayrollClient payrollClient;

    public void write(int year, int quarter, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Форма 200.00");
            w.header("Год", "Квартал", "Сотрудник", "Оклад брутто", "ОПВ", "ИПН", "Нетто");

            PageResponse<PayrollPeriodDto> periods = payrollClient.listPeriods(0, 50);
            if (periods == null || periods.getContent() == null) {
                w.writeTo(out);
                return;
            }

            List<PayrollPeriodDto> filtered = periods.getContent().stream()
                    .filter(p -> {
                        if (p.getStartDate() == null) return false;
                        int pYear = p.getStartDate().getYear();
                        int pQuarter = (p.getStartDate().getMonthValue() - 1) / 3 + 1;
                        return pYear == year && pQuarter == quarter;
                    })
                    .toList();

            for (PayrollPeriodDto period : filtered) {
                int page = 0;
                PageResponse<PayslipDto> slips;
                do {
                    slips = payrollClient.listPayslips(period.getId(), page++, 200);
                    if (slips == null || slips.getContent() == null) break;
                    for (PayslipDto s : slips.getContent()) {
                        w.row(year, quarter, s.getEmployeeName(),
                                s.getGrossSalary(), s.getOpvAmount(), s.getIpnAmount(), s.getNetSalary());
                    }
                } while (!slips.isLast());
            }
            w.writeTo(out);
        }
    }
}
