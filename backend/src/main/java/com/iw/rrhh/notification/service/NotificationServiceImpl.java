package com.iw.rrhh.notification.service;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.notification.domain.Notification;
import com.iw.rrhh.notification.dto.NotificationResponse;
import com.iw.rrhh.notification.repository.NotificationRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void createNotification(Employee recipient, String title, String message) {
        notificationRepository.save(new Notification(recipient, title, message));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Employee employee = getAuthenticatedEmployee();
        return PageResponse.from(
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employee.getId(), pageable)
                        .map(this::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyUnread() {
        return notificationRepository.countByRecipientIdAndReadFalse(getAuthenticatedEmployee().getId());
    }

    @Override
    public void markAsRead(UUID notificationId) {
        Employee employee = getAuthenticatedEmployee();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Notificación no encontrada"));

        if (!notification.getRecipient().getId().equals(employee.getId())) {
            log.warn("Acceso denegado: el empleado {} intentó marcar la notificación {} ajena", employee.getId(), notificationId);
            throw new AccessDeniedException("No puede modificar notificaciones de otras personas");
        }
        notification.markAsRead();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private Employee getAuthenticatedEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Usuario de sesión no encontrado"));

        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("No se encontró ficha de empleado para el usuario autenticado"));
    }
}
