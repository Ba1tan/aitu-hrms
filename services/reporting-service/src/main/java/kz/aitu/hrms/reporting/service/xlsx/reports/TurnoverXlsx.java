package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TurnoverXlsx {

    private final EmployeeClient employeeClient;

    public void write(int year, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Текучесть кадров");
            w.header("Месяц", "Принято", "Уволено");

            Map<Integer, long[]> byMonth = new HashMap<>();
            for (int m = 1; m <= 12; m++) byMonth.put(m, new long[2]);

            int page = 0;
            PageResponse<EmployeeSummaryDto> resp;
            do {
                resp = employeeClient.list(null, null, page++, 200);
                if (resp == null || resp.getContent() == null) break;
                for (EmployeeSummaryDto e : resp.getContent()) {
                    if (e.getHireDate() != null && e.getHireDate().getYear() == year) {
                        byMonth.get(e.getHireDate().getMonthValue())[0]++;
                    }
                }
            } while (!resp.isLast());

            String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
            for (int m = 1; m <= 12; m++) {
                long[] c = byMonth.get(m);
                w.row(months[m - 1], c[0], c[1]);
            }
            w.writeTo(out);
        }
    }
}
