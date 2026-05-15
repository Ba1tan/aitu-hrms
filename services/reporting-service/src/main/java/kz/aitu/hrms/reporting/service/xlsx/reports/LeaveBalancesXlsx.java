package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.LeaveClient;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.LeaveBalanceDto;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LeaveBalancesXlsx {

    private final EmployeeClient employeeClient;
    private final LeaveClient leaveClient;

    public void write(int year, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Остатки отпусков");
            w.header("Сотрудник", "Фамилия", "Тип отпуска", "Всего дней", "Использовано", "Остаток");

            List<EmployeeSummaryDto> employees = employeeClient.list(null, "ACTIVE", 0, 500).getContent();
            if (employees == null) {
                w.writeTo(out);
                return;
            }

            for (EmployeeSummaryDto e : employees) {
                try {
                    List<LeaveBalanceDto> balances = leaveClient.balancesFor(e.getId(), year);
                    if (balances == null || balances.isEmpty()) {
                        w.row(e.getFirstName(), e.getLastName(), "—", null, null, null);
                    } else {
                        for (LeaveBalanceDto b : balances) {
                            w.row(e.getFirstName(), e.getLastName(), b.getLeaveType(),
                                    b.getTotalDays(), b.getUsedDays(), b.getRemainingDays());
                        }
                    }
                } catch (Exception ex) {
                    w.row(e.getFirstName(), e.getLastName(), "Ошибка", null, null, null);
                }
            }
            w.writeTo(out);
        }
    }
}
