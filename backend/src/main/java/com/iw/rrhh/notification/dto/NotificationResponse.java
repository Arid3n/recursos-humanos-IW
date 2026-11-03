package com.iw.rrhh.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String title,
    String message,
    boolean read,
    Instant createdAt
) {}
