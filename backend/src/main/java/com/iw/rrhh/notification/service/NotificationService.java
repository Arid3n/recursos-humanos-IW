package com.iw.rrhh.notification.service;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /**
     * Crea una notificación persistida para el destinatario. Participa en la
     * transacción del llamante (Art. 13.7): si esta se revierte, la notificación también.
     */
    void createNotification(Employee recipient, String title, String message);

    /** Notificaciones del empleado en sesión, más recientes primero. */
    PageResponse<NotificationResponse> getMyNotifications(Pageable pageable);

    /** Número de notificaciones no leídas del empleado en sesión. */
    long countMyUnread();

    /** Marca como leída una notificación propia (idempotente). */
    void markAsRead(UUID notificationId);
}
