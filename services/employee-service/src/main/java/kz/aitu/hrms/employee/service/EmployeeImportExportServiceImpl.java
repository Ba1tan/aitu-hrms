package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeImportExportServiceImpl implements EmployeeImportExportService {

    private static final String[] EXPORT_HEADERS = {
            "employee_number", "last_name", "first_name", "middle_name", "iin",
            "email", "phone", "hire_date", "status", "employment_type", "base_salary",
            "department", "position"
    };

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    @Override
    @Transactional
    public EmployeeDtos.ImportResult importFromXlsx(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }
        int total = 0;
        int imported = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) continue;
                total++;
                try {
                    EmployeeDtos.CreateEmployeeRequest req = new EmployeeDtos.CreateEmployeeRequest();
                    req.setLastName(stringCell(row, 0));
                    req.setFirstName(stringCell(row, 1));
                    req.setMiddleName(stringCell(row, 2));
                    req.setIin(stringCell(row, 3));
                    req.setEmail(stringCell(row, 4));
                    req.setPhone(stringCell(row, 5));
                    req.setHireDate(dateCell(row, 6));
                    req.setBaseSalary(new BigDecimal(stringCell(row, 7)));
                    employeeService.create(req);
                    imported++;
                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BusinessException("Failed to read XLSX: " + ex.getMessage());
        }

        return EmployeeDtos.ImportResult.builder()
                .totalRows(total)
                .imported(imported)
                .skipped(total - imported)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportToXlsx() {
        List<Employee> employees = employeeRepository.findAllByDeletedFalse();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int r = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.getEmployeeNumber());
                row.createCell(1).setCellValue(nullSafe(e.getLastName()));
                row.createCell(2).setCellValue(nullSafe(e.getFirstName()));
                row.createCell(3).setCellValue(nullSafe(e.getMiddleName()));
                row.createCell(4).setCellValue(nullSafe(e.getIin()));
                row.createCell(5).setCellValue(nullSafe(e.getEmail()));
                row.createCell(6).setCellValue(nullSafe(e.getPhone()));
                row.createCell(7).setCellValue(e.getHireDate() != null ? e.getHireDate().toString() : "");
                row.createCell(8).setCellValue(e.getStatus() != null ? e.getStatus().name() : "");
                row.createCell(9).setCellValue(e.getEmploymentType() != null ? e.getEmploymentType().name() : "");
                row.createCell(10).setCellValue(e.getBaseSalary() != null ? e.getBaseSalary().toPlainString() : "");
                row.createCell(11).setCellValue(e.getDepartment() != null ? nullSafe(e.getDepartment().getName()) : "");
                row.createCell(12).setCellValue(e.getPosition() != null ? nullSafe(e.getPosition().getTitle()) : "");
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException ex) {
            throw new BusinessException("Failed to write XLSX: " + ex.getMessage());
        }
    }

    private boolean isBlankRow(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String s = stringCell(row, c);
                if (s != null && !s.isBlank()) return false;
            }
        }
        return true;
    }

    private String stringCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private LocalDate dateCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = stringCell(row, col);
        return s == null || s.isBlank() ? null : LocalDate.parse(s);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}