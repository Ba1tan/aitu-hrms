package kz.aitu.hrms.integration.controller;

import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.dto.settings.SettingDto;
import kz.aitu.hrms.integration.dto.settings.SettingUpdateRequest;
import kz.aitu.hrms.integration.dto.settings.SetupStatusDto;
import kz.aitu.hrms.integration.service.SetupService;
import kz.aitu.hrms.integration.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final SetupService setupService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<List<SettingDto>>> getAll(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getAll(category)));
    }

    /**
     * Non-sensitive settings any authenticated user can read. Backs the
     * dashboard's AttendanceWidget (check-in methods, schedule) and the
     * notification preferences page (sms_provider availability).
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<SettingDto>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.getPublic()));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<SettingDto>> update(
            @PathVariable String key,
            @RequestBody @Valid SettingUpdateRequest req,
            @AuthenticationPrincipal AuthenticatedUser me) {
        return ResponseEntity.ok(ApiResponse.ok(settingsService.update(key, req.getValue(), me)));
    }

    @GetMapping("/setup-status")
    public ResponseEntity<ApiResponse<SetupStatusDto>> setupStatus() {
        return ResponseEntity.ok(ApiResponse.ok(setupService.getStatus()));
    }

    @PostMapping("/complete-setup")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<ApiResponse<Void>> completeSetup(
            @AuthenticationPrincipal AuthenticatedUser me) {
        setupService.complete(me);
        return ResponseEntity.ok(ApiResponse.noContent("Setup completed"));
    }
}
