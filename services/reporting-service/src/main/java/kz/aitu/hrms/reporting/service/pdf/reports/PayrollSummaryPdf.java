package kz.aitu.hrms.reporting.service.pdf.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import kz.aitu.hrms.reporting.client.PayrollClient;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.client.dto.PayslipDto;
import kz.aitu.hrms.reporting.service.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayrollSummaryPdf {

    private final PayrollClient payrollClient;
    private final PdfWriter pdfWriter;

    public void write(UUID periodId, OutputStream out) throws DocumentException, IOException {
        Document doc = pdfWriter.open(out);
        pdfWriter.addTitle(doc, "Сводка по зарплате");
        PdfPTable table = pdfWriter.createTable(6,
                "Сотрудник", "Брутто", "ОПВ", "ВОСМС", "ИПН", "Нетто");
        int page = 0;
        PageResponse<PayslipDto> resp;
        do {
            resp = payrollClient.listPayslips(periodId, page++, 200);
            if (resp == null || resp.getContent() == null) break;
            for (PayslipDto s : resp.getContent()) {
                pdfWriter.addCell(table, s.getEmployeeName());
                pdfWriter.addCell(table, s.getGrossSalary() != null ? s.getGrossSalary().toPlainString() : "");
                pdfWriter.addCell(table, s.getOpvAmount() != null ? s.getOpvAmount().toPlainString() : "");
                pdfWriter.addCell(table, s.getVosmsAmount() != null ? s.getVosmsAmount().toPlainString() : "");
                pdfWriter.addCell(table, s.getIpnAmount() != null ? s.getIpnAmount().toPlainString() : "");
                pdfWriter.addCell(table, s.getNetSalary() != null ? s.getNetSalary().toPlainString() : "");
            }
        } while (!resp.isLast());
        doc.add(table);
        doc.close();
    }
}
