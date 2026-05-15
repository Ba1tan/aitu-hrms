package kz.aitu.hrms.reporting.controller;

import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.reporting.dto.dashboard.DashboardStatsDto;
import kz.aitu.hrms.reporting.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> stats(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        DashboardStatsDto stats = dashboardService.build(
                principal.getUserId(),
                principal.getRole(),
                principal.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
