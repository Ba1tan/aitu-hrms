package kz.aitu.hrms.notification.controller;

import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.dto.NotificationDto;
import kz.aitu.hrms.notification.dto.ReadAllResponseDto;
import kz.aitu.hrms.notification.dto.UnreadCountDto;
import kz.aitu.hrms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> list(
            @AuthenticationPrincipal AuthenticatedUser me,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        var result = service.list(me.getUserId(), type, onlyUnread,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> unreadCount(
            @AuthenticationPrincipal AuthenticatedUser me) {
        return ResponseEntity.ok(ApiResponse.ok(
                new UnreadCountDto(service.unreadCount(me.getUserId()))));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markRead(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.markRead(me.getUserId(), id)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<ReadAllResponseDto>> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser me) {
        int n = service.markAllRead(me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(new ReadAllResponseDto(n)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser me,
            @PathVariable UUID id) {
        service.softDelete(me.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.noContent("Notification deleted"));
    }
}
