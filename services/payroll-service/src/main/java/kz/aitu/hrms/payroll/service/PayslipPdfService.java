package kz.aitu.hrms.payroll.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Renders a payslip to PDF using OpenPDF. Output is returned as a byte array
 * so the controller can stream it back to the client; we deliberately do NOT
 * persist PDFs to disk by default to avoid stale renders after adjustments.
 * Payslips can be regenerated at any time from the canonical numeric fields.
 */
@Service
@RequiredArgsConstructor
public class PayslipPdfService {

    private final PayslipRepository payslipRepo;

    @Transactional(readOnly = true)
    public byte[] render(UUID payslipId) {
        Payslip p = payslipRepo.findByIdAndDeletedFalse(payslipId)
                .orElseThrow(() -> new IllegalArgumentException("Payslip not found: " + payslipId));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(PageSize.A4)) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            Paragraph t = new Paragraph("Payslip — " + p.getPeriod().getName(), title);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingAfter(12);
            doc.add(t);

            doc.add(line("Employee:    ", p.getEmployeeName(), body));
            doc.add(line("ID:          ", p.getEmployeeNumber(), body));
            doc.add(line("IIN:         ", p.getEmployeeIin(), body));
            doc.add(line("Department:  ", p.getDepartmentName(), body));
            doc.add(line("Position:    ", p.getPositionTitle(), body));
            doc.add(line("Worked days: ", p.getWorkedDays() + " / " + p.getTotalWorkingDays(), body));
            doc.add(new Paragraph(" "));

            PdfPTable tbl = new PdfPTable(2);
            tbl.setWidthPercentage(100);
            tbl.addCell(headerCell("Earnings & deductions", header));
            tbl.addCell(headerCell("Amount (₸)", header));

            row(tbl, "Gross salary", p.getGrossSalary(), body);
            row(tbl, "Earned (prorated)", p.getEarnedSalary(), body);
            row(tbl, "Allowances", p.getAllowances(), body);
            row(tbl, "Other deductions", p.getOtherDeductions(), body);
            row(tbl, "ОПВ (10%)", p.getOpvAmount(), body);
            row(tbl, "ВОСМС (2%)", p.getVosmsAmount(), body);
            row(tbl, "Taxable income", p.getTaxableIncome(), body);
            row(tbl, "ИПН (income tax)", p.getIpnAmount(), body);
            row(tbl, "Total deductions", p.getTotalDeductions(), header);
            row(tbl, "Net salary", p.getNetSalary(), header);

            doc.add(tbl);
            doc.add(new Paragraph(" "));

            PdfPTable er = new PdfPTable(2);
            er.setWidthPercentage(100);
            er.addCell(headerCell("Employer obligations", header));
            er.addCell(headerCell("Amount (₸)", header));
            row(er, "СО (5%)", p.getSoAmount(), body);
            row(er, "СН (6%)", p.getSnAmount(), body);
            row(er, "ОПВР (3.5%)", p.getOpvrAmount(), body);
            doc.add(er);

            doc.add(new Paragraph(" "));
            Paragraph footer = new Paragraph(
                    "MRP used: " + p.getMrpUsed() +
                    " | Resident: " + (p.isResident() ? "yes" : "no") +
                    " | Status: " + p.getStatus(),
                    body);
            footer.setAlignment(Element.ALIGN_RIGHT);
            doc.add(footer);
        }
        return out.toByteArray();
    }

    private static Paragraph line(String label, String value, Font font) {
        return new Paragraph(label + (value == null ? "—" : value), font);
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(new Color(230, 230, 230));
        c.setPadding(6f);
        return c;
    }

    private static void row(PdfPTable t, String label, BigDecimal value, Font font) {
        t.addCell(new PdfPCell(new Phrase(label, font)));
        PdfPCell amt = new PdfPCell(new Phrase(format(value), font));
        amt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(amt);
    }

    private static String format(BigDecimal v) {
        if (v == null) return "—";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }
}