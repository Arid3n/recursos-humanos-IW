package com.iw.rrhh.performance.service;

import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.performance.domain.PerformanceReview;
import com.iw.rrhh.performance.domain.PerformanceReviewStatus;
import com.iw.rrhh.performance.dto.AcknowledgePerformanceReviewRequest;
import com.iw.rrhh.performance.dto.CreatePerformanceReviewRequest;
import com.iw.rrhh.performance.dto.PerformanceReviewEmployeeResponse;
import com.iw.rrhh.performance.dto.PerformanceReviewResponse;
import com.iw.rrhh.performance.repository.PerformanceReviewRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class PerformanceReviewServiceImpl implements PerformanceReviewService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PerformanceReviewServiceImpl(
            PerformanceReviewRepository reviewRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            Clock clock) {
        this.reviewRepository = reviewRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceReviewResponse> getMyReviews() {
        Employee employee = getAuthenticatedEmployee();
        return reviewRepository.findByEmployeeIdOrderByPeriodDescCreatedAtDesc(employee.getId())
                .stream()
                .map(PerformanceReviewResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceReviewResponse> getTeamReviews() {
        Employee manager = getAuthenticatedEmployee();
        return reviewRepository.findByEmployeeManagerIdOrderByCreatedAtDesc(manager.getId())
                .stream()
                .map(PerformanceReviewResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PerformanceReviewEmployeeResponse> getTeamEmployees() {
        Employee manager = getAuthenticatedEmployee();
        return employeeRepository.findByManagerIdAndActiveTrue(manager.getId()).stream()
                .sorted(Comparator.comparing(Employee::getLastName).thenComparing(Employee::getFirstName))
                .map(employee -> new PerformanceReviewEmployeeResponse(
                        employee.getId(),
                        employee.getFirstName() + " " + employee.getLastName()))
                .toList();
    }

    @Override
    public PerformanceReviewResponse createReview(UUID employeeId, CreatePerformanceReviewRequest request) {
        Employee manager = getAuthenticatedEmployee();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado"));
        if (!employee.isActive()
                || employee.getManager() == null
                || !employee.getManager().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Solo puede evaluar a personas activas de su equipo");
        }

        PerformanceReview review = new PerformanceReview(
                employee,
                manager,
                request.period().strip(),
                request.objectives().strip(),
                request.rating(),
                request.comments().strip());
        return PerformanceReviewResponse.from(reviewRepository.save(review));
    }

    @Override
    public PerformanceReviewResponse acknowledge(
            UUID reviewId,
            AcknowledgePerformanceReviewRequest request) {
        Employee employee = getAuthenticatedEmployee();
        PerformanceReview review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada"));
        if (!review.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("Solo puede dar conformidad a sus propias evaluaciones");
        }
        if (review.getStatus() != PerformanceReviewStatus.PENDING_ACKNOWLEDGMENT) {
            throw new PerformanceReviewAlreadyFinalizedException();
        }

        String employeeComments = request.employeeComments();
        review.acknowledge(
                employeeComments == null || employeeComments.isBlank() ? null : employeeComments.strip(),
                Instant.now(clock));
        return PerformanceReviewResponse.from(review);
    }

    private Employee getAuthenticatedEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NoSuchElementException("Usuario autenticado no encontrado"));
        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("No existe ficha de empleado para el usuario autenticado"));
    }
}
