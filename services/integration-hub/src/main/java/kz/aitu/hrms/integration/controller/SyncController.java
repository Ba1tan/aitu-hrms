package kz.aitu.hrms.integration.controller;

import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import kz.aitu.hrms.integration.dto.sync.SyncJobDto;
import kz.aitu.hrms.integration.dto.sync.SyncTriggerResponseDto;
import kz.aitu.hrms.integration.service.SyncJobService;
import kz.aitu.hrms.integration.service.SyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/integration")
@RequiredArgsConstructor
public class SyncController {

    private final SyncOrchestrator syncOrchestrator;
    private final SyncJobService syncJobService;

    @PostMapping("/sync/{periodId}")
    @PreAuthorize("hasAnyAuthority('INTEGRATION_MANAGE', 'SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<SyncTriggerResponseDto>> triggerSync(
            @PathVariable UUID periodId,
            @AuthenticationPrincipal AuthenticatedUser me) {
        SyncTriggerResponseDto result = syncOrchestrator.trigger(periodId, me.getUserId());
        return ResponseEntity.accepted().body(ApiResponse.ok("Sync triggered", result));
    }

    @GetMapping("/sync/status/{jobId}")
    @PreAuthorize("hasAnyAuthority('INTEGRATION_MANAGE', 'SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<SyncJobDto>> status(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok(syncJobService.getById(jobId)));
    }

    @GetMapping("/sync/history")
    @PreAuthorize("hasAnyAuthority('INTEGRATION_MANAGE', 'SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<Page<SyncJobDto>>> history(
            @RequestParam(required = false) SyncTarget target,
            @RequestParam(required = false) SyncStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable p) {
        return ResponseEntity.ok(ApiResponse.ok(syncJobService.list(target, status, p)));
    }

    @PostMapping("/retry/{jobId}")
    @PreAuthorize("hasAnyAuthority('INTEGRATION_MANAGE', 'SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<SyncJobDto>> retry(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal AuthenticatedUser me) {
        return ResponseEntity.ok(ApiResponse.ok(syncOrchestrator.retryJob(jobId, me.getUserId())));
    }
}
