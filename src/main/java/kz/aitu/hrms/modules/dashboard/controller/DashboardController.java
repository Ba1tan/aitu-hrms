package kz.aitu.hrms.modules.dashboard.controller;

import kz.aitu.hrms.common.response.ApiResponse;
import kz.aitu.hrms.modules.auth.entity.User;
import kz.aitu.hrms.modules.dashboard.dto.DashboardDtos;
import kz.aitu.hrms.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DashboardDtos.DashboardStatsResponse>> getStats(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getStats(currentUser))
        );
    }
}
