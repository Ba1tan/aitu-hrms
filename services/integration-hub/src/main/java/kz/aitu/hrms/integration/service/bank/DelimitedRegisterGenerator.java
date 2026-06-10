package kz.aitu.hrms.integration.service.bank;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Base for the per-bank salary registers. They share the canonical KZ
 * salary-register columns and differ only in the field delimiter; the exact
 * template (column order, headers, encoding) must still be confirmed against
 * each bank's current upload spec — see {@link BankFileGenerator}.
 *
 * Columns: № · IBAN · ИИН · ФИО · Сумма · КНП · КБе · Назначение.
 * A trailing total row is appended so the accountant can reconcile the batch.
 * The file is UTF-8 with a BOM so Excel opens Cyrillic correctly.
 */
abstract class DelimitedRegisterGenerator implements BankFileGenerator {

    private static final String[] HEADER =
            {"№", "IBAN", "ИИН", "ФИО", "Сумма", "КНП", "КБе", "Назначение"};

    /** Field separator for this bank's template. */
    protected abstract char delimiter();

    @Override
    public void write(List<BankPaymentRow> rows, BankFileContext ctx, OutputStream out) throws IOException {
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            w.write('﻿'); // UTF-8 BOM for Excel
            writeRow(w, HEADER);

            BigDecimal total = BigDecimal.ZERO;
            int i = 1;
            for (BankPaymentRow r : rows) {
                BigDecimal amount = r.amount() == null ? BigDecimal.ZERO : r.amount();
                total = total.add(amount);
                writeRow(w, new String[]{
                        String.valueOf(i++),
                        nullToEmpty(r.iban()),
                        nullToEmpty(r.iin()),
                        nullToEmpty(r.fullName()),
                        money(amount),
                        nullToEmpty(ctx.knp()),
                        nullToEmpty(ctx.kbe()),
                        nullToEmpty(ctx.purpose())
                });
            }
            // Total line: blank cells except a label and the summed amount.
            writeRow(w, new String[]{"", "", "", "ИТОГО", money(total), "", "", ""});
        }
    }

    private void writeRow(Writer w, String[] cells) throws IOException {
        char d = delimiter();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(d);
            sb.append(escape(cells[i], d));
        }
        sb.append("\r\n");
        w.write(sb.toString());
    }

    private static String escape(String value, char delimiter) {
        String v = nullToEmpty(value);
        if (v.indexOf(delimiter) >= 0 || v.indexOf('"') >= 0
                || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
