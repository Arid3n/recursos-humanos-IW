package com.iw.rrhh.performance.service;

public class PerformanceReviewAlreadyFinalizedException extends RuntimeException {
    public PerformanceReviewAlreadyFinalizedException() {
        super("La evaluación ya está formalizada y no admite cambios");
    }
}
