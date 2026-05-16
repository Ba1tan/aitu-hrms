package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HeadcountXlsx {

    private final EmployeeClient employeeClient;

    public void write(LocalDate from, LocalDate to, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Численность");
            w.header("Дата", "Численность на дату");

            Map<LocalDate, Long> counts = new LinkedHashMap<>();
            LocalDate current = from;
            while (!current.isAfter(to)) {
                counts.put(current, 0L);
                current = current.plusMonths(1).withDayOfMonth(1);
            }

            int page = 0;
            PageResponse<EmployeeSummaryDto> resp;
            do {
                resp = employeeClient.list(null, null, page++, 200);
                if (resp == null || resp.getContent() == null) break;
                for (EmployeeSummaryDto e : resp.getContent()) {
                    if (e.getHireDate() == null) continue;
                    for (LocalDate d : counts.keySet()) {
                        if (!e.getHireDate().isAfter(d)) {
                            counts.merge(d, 1L, Long::sum);
                        }
                    }
                }
            } while (!resp.isLast());

            counts.forEach((d, cnt) -> w.row(d, cnt));
            w.writeTo(out);
        }
    }
}
