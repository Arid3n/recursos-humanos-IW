package com.iw.rrhh.performance.controller;

import com.iw.rrhh.performance.dto.AcknowledgePerformanceReviewRequest;
import com.iw.rrhh.performance.dto.CreatePerformanceReviewRequest;
import com.iw.rrhh.performance.dto.PerformanceReviewEmployeeResponse;
import com.iw.rrhh.performance.dto.PerformanceReviewResponse;
import com.iw.rrhh.performance.service.PerformanceReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/performance-reviews")
@Tag(name = "Performance Reviews", description = "Evaluación de desempeño con conformidad (HU-08)")
public class PerformanceReviewController {

    private final PerformanceReviewService reviewService;

    public PerformanceReviewController(PerformanceReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLEADO')")
    @Operation(summary = "Consultar las evaluaciones propias")
    public ResponseEntity<List<PerformanceReviewResponse>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getMyReviews());
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")
    @Operation(summary = "Consultar las evaluaciones del equipo actual")
    public ResponseEntity<List<PerformanceReviewResponse>> getTeamReviews() {
        return ResponseEntity.ok(reviewService.getTeamReviews());
    }

    @GetMapping("/team/employees")
    @PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")
    @Operation(summary = "Listar miembros activos del equipo para crear una evaluación")
    public ResponseEntity<List<PerformanceReviewEmployeeResponse>> getTeamEmployees() {
        return ResponseEntity.ok(reviewService.getTeamEmployees());
    }

    @PostMapping("/team/{employeeId}")
    @PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")
    @Operation(summary = "Crear una evaluación pendiente de conformidad para un miembro del equipo")
    public ResponseEntity<PerformanceReviewResponse> createReview(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreatePerformanceReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(employeeId, request));
    }

    @PostMapping("/{reviewId}/acknowledge")
    @PreAuthorize("hasRole('EMPLEADO')")
    @Operation(summary = "Dar conformidad y formalizar una evaluación propia")
    public ResponseEntity<PerformanceReviewResponse> acknowledge(
            @PathVariable UUID reviewId,
            @Valid @RequestBody AcknowledgePerformanceReviewRequest request) {
        return ResponseEntity.ok(reviewService.acknowledge(reviewId, request));
    }
}
