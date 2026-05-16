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
            PayrollPeriodDto period = payrollClient.getLatestPeriod();
            if (period != null) {
                doc.add(new Paragraph("Последний период: " + period.getName(), pdfWriter.bodyFont()));
                doc.add(new Paragraph("Статус: " + period.getStatus(), pdfWriter.bodyFont()));
                try {
                    PayrollTotalsDto totals = payrollClient.getPeriodTotals(period.getId());
                    if (totals != null) {
                        doc.add(new Paragraph("Брутто итого: " + totals.getTotalGross(), pdfWriter.bodyFont()));
                        doc.add(new Paragraph("Нетто итого: " + totals.getTotalNet(), pdfWriter.bodyFont()));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            doc.add(new Paragraph("Данные по зарплате недоступны.", pdfWriter.bodyFont()));
        }

        doc.close();
    }
}
