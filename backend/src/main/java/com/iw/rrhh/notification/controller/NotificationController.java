package com.iw.rrhh.notification.controller;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.notification.dto.NotificationResponse;
import com.iw.rrhh.notification.dto.UnreadCountResponse;
import com.iw.rrhh.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "Notificaciones internas del empleado en sesión (HU-05)")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Notificaciones propias, más recientes primero")
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Número de notificaciones propias no leídas")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.countMyUnread()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Marcar una notificación propia como leída")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
