package kz.aitu.hrms.reporting.service.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

@Component
@RequiredArgsConstructor
public class PdfWriter {

    private final BaseFont cyrillicFont;

    public Document open(OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4.rotate());
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
        doc.open();
        return doc;
    }

    public Font titleFont() {
        return new Font(cyrillicFont, 14, Font.BOLD);
    }

    public Font headerFont() {
        return new Font(cyrillicFont, 10, Font.BOLD, Color.WHITE);
    }

    public Font bodyFont() {
        return new Font(cyrillicFont, 9);
    }

    public void addTitle(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, titleFont());
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(12);
        doc.add(p);
    }

    public PdfPTable createTable(int columns, String... headers) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont()));
            cell.setBackgroundColor(new Color(64, 64, 64));
            cell.setPadding(5);
            table.addCell(cell);
        }
        return table;
    }

    public void addCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", bodyFont()));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
