package kz.aitu.hrms.reporting.service.xlsx;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class XlsxWriter implements AutoCloseable {

    private final SXSSFWorkbook workbook;
    private SXSSFSheet sheet;
    private XlsxStyles styles;
    private int rowIdx = 0;

    public XlsxWriter() {
        this.workbook = new SXSSFWorkbook(100);
        this.workbook.setCompressTempFiles(true);
    }

    public XlsxWriter sheet(String name) {
        this.sheet = workbook.createSheet(name);
        this.styles = new XlsxStyles(workbook);
        this.rowIdx = 0;
        return this;
    }

    public XlsxWriter header(String... names) {
        Row row = sheet.createRow(rowIdx++);
        CellStyle style = styles.header();
        for (int i = 0; i < names.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(names[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 20 * 256);
        }
        return this;
    }

    public XlsxWriter row(Object... values) {
        Row row = sheet.createRow(rowIdx++);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object val = values[i];
            if (val == null) {
                cell.setBlank();
            } else if (val instanceof BigDecimal bd) {
                cell.setCellValue(bd.doubleValue());
                cell.setCellStyle(styles.money());
            } else if (val instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else if (val instanceof LocalDate ld) {
                Date date = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
                cell.setCellValue(date);
                cell.setCellStyle(styles.date());
            } else if (val instanceof Boolean b) {
                cell.setCellValue(b);
            } else {
                cell.setCellValue(val.toString());
            }
        }
        return this;
    }

    public void writeTo(OutputStream out) throws IOException {
        workbook.write(out);
    }

    @Override
    public void close() {
        workbook.dispose();
    }
}
