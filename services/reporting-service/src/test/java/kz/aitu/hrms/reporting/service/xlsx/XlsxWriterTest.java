package kz.aitu.hrms.reporting.service.xlsx;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxWriterTest {

    @TempDir
    Path tmp;

    @Test
    void writesHeaderAndRows() throws Exception {
        File out = tmp.resolve("test.xlsx").toFile();
        try (XlsxWriter w = new XlsxWriter(); OutputStream os = new FileOutputStream(out)) {
            w.sheet("Test");
            w.header("Name", "Amount", "Date");
            w.row("Alice", BigDecimal.valueOf(1234.56), LocalDate.of(2026, 1, 15));
            w.row("Bob", BigDecimal.valueOf(9876.00), null);
            w.writeTo(os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Amount");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Alice");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(1234.56);
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Bob");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void multipleSheets_rowIndexResets() throws Exception {
        File out = tmp.resolve("multi.xlsx").toFile();
        try (XlsxWriter w = new XlsxWriter(); OutputStream os = new FileOutputStream(out)) {
            w.sheet("Sheet1").header("A").row("x");
            w.sheet("Sheet2").header("B").row("y");
            w.writeTo(os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("A");
            assertThat(wb.getSheetAt(1).getRow(0).getCell(0).getStringCellValue()).isEqualTo("B");
        }
    }

    @Test
    void nullValue_writesBlankCell() throws Exception {
        File out = tmp.resolve("null.xlsx").toFile();
        try (XlsxWriter w = new XlsxWriter(); OutputStream os = new FileOutputStream(out)) {
            w.sheet("S").header("X").row((Object) null);
            w.writeTo(os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isBlank();
        }
    }
}
