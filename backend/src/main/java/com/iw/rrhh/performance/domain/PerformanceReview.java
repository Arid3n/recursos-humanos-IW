package com.iw.rrhh.performance.domain;

import com.iw.rrhh.employee.domain.Employee;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "performance_reviews")
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Employee evaluator;

    @Column(nullable = false, length = 50)
    private String period;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objectives;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "employee_comments", columnDefinition = "TEXT")
    private String employeeComments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PerformanceReviewStatus status = PerformanceReviewStatus.PENDING_ACKNOWLEDGMENT;

    @Column(name = "employee_acknowledged_at")
    private Instant employeeAcknowledgedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected PerformanceReview() {}

    public PerformanceReview(
            Employee employee,
            Employee evaluator,
            String period,
            String objectives,
            Integer rating,
            String comments) {
        this.employee = employee;
        this.evaluator = evaluator;
        this.period = period;
        this.objectives = objectives;
        this.rating = rating;
        this.comments = comments;
        this.status = PerformanceReviewStatus.PENDING_ACKNOWLEDGMENT;
    }

    public void acknowledge(String comments, Instant acknowledgedAt) {
        if (status != PerformanceReviewStatus.PENDING_ACKNOWLEDGMENT) {
            throw new IllegalStateException("La evaluación ya está formalizada");
        }
        this.employeeComments = comments;
        this.employeeAcknowledgedAt = acknowledgedAt;
        this.finalizedAt = acknowledgedAt;
        this.status = PerformanceReviewStatus.FINALIZED;
    }

    public UUID getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Employee getEvaluator() {
        return evaluator;
    }

    public String getPeriod() {
        return period;
    }

    public String getObjectives() {
        return objectives;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComments() {
        return comments;
    }

    public String getEmployeeComments() {
        return employeeComments;
    }

    public PerformanceReviewStatus getStatus() {
        return status;
    }

    public Instant getEmployeeAcknowledgedAt() {
        return employeeAcknowledgedAt;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
