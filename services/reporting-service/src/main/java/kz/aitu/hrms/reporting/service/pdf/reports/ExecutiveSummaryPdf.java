package kz.aitu.hrms.reporting.service.pdf.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import kz.aitu.hrms.reporting.client.*;
import kz.aitu.hrms.reporting.client.dto.*;
import kz.aitu.hrms.reporting.service.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ExecutiveSummaryPdf {

    private final EmployeeClient employeeClient;
    private final PayrollClient payrollClient;
    private final PdfWriter pdfWriter;

    public void write(int year, int month, OutputStream out) throws DocumentException, IOException {
        Document doc = pdfWriter.open(out);
        pdfWriter.addTitle(doc, "Исполнительный отчёт — " + year + "/" + String.format("%02d", month));

        try {
            EmployeeCountsDto counts = employeeClient.getCounts();
            doc.add(new Paragraph("Сотрудники всего: " + counts.getTotal(), pdfWriter.bodyFont()));
            doc.add(new Paragraph("Активных: " + counts.getActive(), pdfWriter.bodyFont()));
            doc.add(new Paragraph("В отпуске: " + counts.getOnLeave(), pdfWriter.bodyFont()));
        } catch (Exception e) {
            doc.add(new Paragraph("Данные о сотрудниках недоступны.", pdfWriter.bodyFont()));
        }

        try {
            PayrollPeriodDto period = latestPeriod();
            if (period != null) {
                doc.add(new Paragraph("Последний период: " + period.getName(), pdfWriter.bodyFont()));
                doc.add(new Paragraph("Статус: " + period.getStatus(), pdfWriter.bodyFont()));

                BigDecimal gross = BigDecimal.ZERO;
                BigDecimal net = BigDecimal.ZERO;
                int page = 0;
                PageResponse<PayslipDto> slips;
                do {
                    slips = payrollClient.listPayslips(period.getId(), page++, 200);
                    if (slips == null || slips.getContent() == null) break;
                    for (PayslipDto s : slips.getContent()) {
                        if (s.getGrossSalary() != null) gross = gross.add(s.getGrossSalary());
                        if (s.getNetSalary() != null) net = net.add(s.getNetSalary());
                    }
                } while (!slips.isLast());

                doc.add(new Paragraph("Брутто итого: " + gross, pdfWriter.bodyFont()));
                doc.add(new Paragraph("Нетто итого: " + net, pdfWriter.bodyFont()));
            }
        } catch (Exception e) {
            doc.add(new Paragraph("Данные по зарплате недоступны.", pdfWriter.bodyFont()));
        }

        doc.close();
    }

    /**
     * Latest period via the paginated list (sorted year/month desc upstream),
     * preferring the newest non-locked one. payroll-service exposes no
     * dedicated "latest period" endpoint.
     */
    private PayrollPeriodDto latestPeriod() {
        PageResponse<PayrollPeriodDto> page = payrollClient.listPeriods(0, 5);
        if (page == null || page.getContent() == null || page.getContent().isEmpty()) return null;
        return page.getContent().stream()
                .filter(p -> !p.isLocked())
                .findFirst()
                .orElse(page.getContent().get(0));
    }
}
