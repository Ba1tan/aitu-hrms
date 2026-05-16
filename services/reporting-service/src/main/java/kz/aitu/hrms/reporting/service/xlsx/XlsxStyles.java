package kz.aitu.hrms.reporting.service.xlsx;

import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;

public class XlsxStyles {

    private final Map<String, CellStyle> cache = new HashMap<>();
    private final Workbook wb;

    public XlsxStyles(Workbook wb) {
        this.wb = wb;
    }

    public CellStyle header() {
        return cache.computeIfAbsent("header", k -> {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            s.setFont(f);
            s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setBorderBottom(BorderStyle.THIN);
            return s;
        });
    }

    public CellStyle money() {
        return cache.computeIfAbsent("money", k -> {
            CellStyle s = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            s.setDataFormat(fmt.getFormat("#,##0.00"));
            return s;
        });
    }

    public CellStyle date() {
        return cache.computeIfAbsent("date", k -> {
            CellStyle s = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            s.setDataFormat(fmt.getFormat("dd.mm.yyyy"));
            return s;
        });
    }

    public CellStyle text() {
        return cache.computeIfAbsent("text", k -> wb.createCellStyle());
    }
}
