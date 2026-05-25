package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Component
@RequiredArgsConstructor
public class EmployeeDirectoryXlsx {

    private final EmployeeClient employeeClient;

    public void write(OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Справочник сотрудников");
            w.header("ФИО", "Должность", "Отдел", "Статус", "Дата приёма");

            int page = 0;
            PageResponse<EmployeeSummaryDto> resp;
            do {
                resp = employeeClient.list(null, null, page++, 200);
                if (resp == null || resp.getContent() == null) break;
                for (EmployeeSummaryDto e : resp.getContent()) {
                    w.row(e.getFullName(),
                            e.getPosition(), e.getDepartment(),
                            e.getStatus(), e.getHireDate());
                }
            } while (!resp.isLast());
            w.writeTo(out);
        }
    }
}
