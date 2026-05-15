package kz.aitu.hrms.reporting.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import kz.aitu.hrms.reporting.service.xlsx.reports.*;
import kz.aitu.hrms.reporting.service.pdf.reports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ReportController {

    private final PayrollSummaryXlsx payrollSummaryXlsx;
    private final PayrollSummaryPdf payrollSummaryPdf;
    private final Form200Xlsx form200Xlsx;
    private final SalaryBreakdownXlsx salaryBreakdownXlsx;
    private final AttendanceMonthlyXlsx attendanceMonthlyXlsx;
    private final AttendanceSummaryXlsx attendanceSummaryXlsx;
    private final LeaveBalancesXlsx leaveBalancesXlsx;
    private final EmployeeDirectoryXlsx employeeDirectoryXlsx;
    private final TurnoverXlsx turnoverXlsx;
    private final HeadcountXlsx headcountXlsx;
    private final ExecutiveSummaryPdf executiveSummaryPdf;
    private final AiInsightsPdf aiInsightsPdf;

    @GetMapping("/payroll-summary")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public void payrollSummary(@RequestParam UUID periodId, HttpServletResponse response) throws IOException {
        String name = "payroll-summary-" + periodId + ".xlsx";
        setXlsxHeaders(response, name);
        payrollSummaryXlsx.write(periodId, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/payroll-summary/pdf")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public void payrollSummaryPdf(@RequestParam UUID periodId, HttpServletResponse response) throws Exception {
        String name = "payroll-summary-" + periodId + ".pdf";
        setPdfHeaders(response, name);
        payrollSummaryPdf.write(periodId, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/form200")
    @PreAuthorize("hasAuthority('REPORT_FORM_200')")
    public void form200(
            @RequestParam @Min(2020) @Max(2100) int year,
            @RequestParam @Min(1) @Max(4) int quarter,
            HttpServletResponse response) throws IOException {
        String name = "form200-" + year + "-Q" + quarter + ".xlsx";
        setXlsxHeaders(response, name);
        form200Xlsx.write(year, quarter, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/salary-breakdown")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public void salaryBreakdown(
            @RequestParam(required = false) UUID departmentId,
            HttpServletResponse response) throws IOException {
        String name = "salary-breakdown.xlsx";
        setXlsxHeaders(response, name);
        salaryBreakdownXlsx.write(departmentId, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/attendance-monthly")
    @PreAuthorize("hasAuthority('REPORT_OPERATIONAL')")
    public void attendanceMonthly(
            @RequestParam @Min(2020) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month,
            HttpServletResponse response) throws IOException {
        String name = "attendance-monthly-" + year + "-" + String.format("%02d", month) + ".xlsx";
        setXlsxHeaders(response, name);
        attendanceMonthlyXlsx.write(year, month, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/attendance-summary")
    @PreAuthorize("hasAuthority('REPORT_OPERATIONAL')")
    public void attendanceSummary(
            @RequestParam @Min(2020) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month,
            HttpServletResponse response) throws IOException {
        String name = "attendance-summary-" + year + "-" + String.format("%02d", month) + ".xlsx";
        setXlsxHeaders(response, name);
        attendanceSummaryXlsx.write(year, month, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/leave-balances")
    @PreAuthorize("hasAuthority('REPORT_OPERATIONAL')")
    public void leaveBalances(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,
            HttpServletResponse response) throws IOException {
        String name = "leave-balances-" + year + ".xlsx";
        setXlsxHeaders(response, name);
        leaveBalancesXlsx.write(year, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/employee-directory")
    @PreAuthorize("hasAuthority('REPORT_OPERATIONAL')")
    public void employeeDirectory(HttpServletResponse response) throws IOException {
        String name = "employee-directory.xlsx";
        setXlsxHeaders(response, name);
        employeeDirectoryXlsx.write(response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/turnover")
    @PreAuthorize("hasAuthority('REPORT_EXECUTIVE')")
    public void turnover(
            @RequestParam @Min(2020) @Max(2100) int year,
            HttpServletResponse response) throws IOException {
        String name = "turnover-" + year + ".xlsx";
        setXlsxHeaders(response, name);
        turnoverXlsx.write(year, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/headcount")
    @PreAuthorize("hasAuthority('REPORT_EXECUTIVE')")
    public void headcount(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletResponse response) throws IOException {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        String name = "headcount-" + from + "-" + to + ".xlsx";
        setXlsxHeaders(response, name);
        headcountXlsx.write(fromDate, toDate, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/executive-summary")
    @PreAuthorize("hasAuthority('REPORT_EXECUTIVE')")
    public void executiveSummary(
            @RequestParam @Min(2020) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month,
            HttpServletResponse response) throws Exception {
        String name = "executive-summary-" + year + "-" + String.format("%02d", month) + ".pdf";
        setPdfHeaders(response, name);
        executiveSummaryPdf.write(year, month, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/ai-insights")
    @PreAuthorize("hasAuthority('AI_ANOMALY')")
    public void aiInsights(HttpServletResponse response) throws Exception {
        String name = "ai-insights.pdf";
        setPdfHeaders(response, name);
        aiInsightsPdf.write(response.getOutputStream());
        response.flushBuffer();
    }

    private void setXlsxHeaders(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }

    private void setPdfHeaders(HttpServletResponse response, String filename) {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }
}
