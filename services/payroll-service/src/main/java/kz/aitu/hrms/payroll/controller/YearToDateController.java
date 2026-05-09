package kz.aitu.hrms.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.payroll.dto.YtdDtos;
import kz.aitu.hrms.payroll.service.YearToDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Year-to-Date", description = "Cumulative payroll totals per employee per year")
@RestController
@RequestMapping("/v1/payroll/ytd")
@RequiredArgsConstructor
public class YearToDateController {

    private final YearToDateService ytdService;

    @Operation(summary = "Year-to-date totals for an employee")
    @GetMapping("/employee/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<YtdDtos.Response>> employee(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(ytdService.ytd(id, year)));
    }
}