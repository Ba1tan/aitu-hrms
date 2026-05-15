package kz.aitu.hrms.reporting.service.pdf.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import kz.aitu.hrms.reporting.client.AiMlClient;
import kz.aitu.hrms.reporting.client.dto.AttritionRiskDto;
import kz.aitu.hrms.reporting.client.dto.PayrollForecastDto;
import kz.aitu.hrms.reporting.service.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiInsightsPdf {

    private final AiMlClient aiMlClient;
    private final PdfWriter pdfWriter;

    public void write(OutputStream out) throws DocumentException, IOException {
        Document doc = pdfWriter.open(out);
        pdfWriter.addTitle(doc, "AI Аналитика — Риски и прогнозы");

        try {
            PayrollForecastDto forecast = aiMlClient.payrollForecast();
            if (forecast != null) {
                doc.add(new Paragraph("Прогноз ФОТ на период: " + forecast.getPeriod(), pdfWriter.bodyFont()));
                doc.add(new Paragraph("Брутто прогноз: " + forecast.getForecastedGross(), pdfWriter.bodyFont()));
                doc.add(new Paragraph("Нетто прогноз: " + forecast.getForecastedNet(), pdfWriter.bodyFont()));
                doc.add(new Paragraph("Уверенность: " + String.format("%.0f%%", forecast.getConfidenceScore() * 100), pdfWriter.bodyFont()));
            }
        } catch (Exception e) {
            doc.add(new Paragraph("Прогноз ФОТ недоступен.", pdfWriter.bodyFont()));
        }

        try {
            List<AttritionRiskDto> risks = aiMlClient.attritionRisks(null);
            if (risks != null && !risks.isEmpty()) {
                doc.add(new Paragraph(" ", pdfWriter.bodyFont()));
                doc.add(new Paragraph("Риски увольнения сотрудников:", pdfWriter.titleFont()));
                PdfPTable table = pdfWriter.createTable(4, "Сотрудник", "Отдел", "Риск", "Причина");
                for (AttritionRiskDto r : risks) {
                    pdfWriter.addCell(table, r.getEmployeeName());
                    pdfWriter.addCell(table, r.getDepartment());
                    pdfWriter.addCell(table, r.getRiskLevel());
                    pdfWriter.addCell(table, r.getReason());
                }
                doc.add(table);
            }
        } catch (Exception e) {
            doc.add(new Paragraph("Данные о рисках недоступны.", pdfWriter.bodyFont()));
        }

        doc.close();
    }
}
