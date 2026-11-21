# Implementation Plan: spec-008 — Evaluación de Desempeño con Conformidad

**Branch**: `spec-008` | **Date**: 2026-10-27 | **Spec**: [specs/spec-008/spec.md](spec.md)  
**Input**: Especificación HU-08 (clarificada con 4 decisiones cerradas)

---

## 1. Summary

Implementar el módulo de **evaluación de desempeño** con circuito bilateral de conformidad, garantizando:

- El responsable de equipo crea evaluaciones (objetivos, valoración 1–5, comentarios) para los miembros de su equipo. La evaluación nace en estado `PENDING_ACKNOWLEDGMENT`.
- El empleado evalúado revisa la evaluación, puede añadir comentarios opcionales y registra su conformidad. La evaluación pasa a `FINALIZED` con la marca temporal del servidor.
- **Inmutabilidad absoluta**: ninguna de las dos partes puede modificar una evaluación `FINALIZED`, garantizado a nivel de aplicación **y** de base de datos (trigger PostgreSQL).
- **Aislamiento estricto**: los empleados solo consultan sus propias evaluaciones; los responsables solo consultan y crean evaluaciones para su equipo actual.
- La valoración está restringida a enteros 1–5 con validación en cliente, servidor y restricción `CHECK` en BD.

---

## 2. Technical Context

- **Language/Version**: Java 21 LTS (Backend) · TypeScript 5.x estricto / Angular 21 (Frontend)
- **Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA, Validation) · Angular Material 22
- **Storage**: PostgreSQL 16 — tabla `performance_reviews` creada en `V1__init_shared_schema.sql`; trigger de inmutabilidad en `V4__immutable_finalized_performance_reviews.sql`.
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL real), Vitest (Frontend)
- **Target Platform**: Aplicación Web SPA responsive
- **Project Type**: Monolito modular por capas desacopladas
- **Performance Goals**: Circuito completo de evaluación en < 3 pasos guiados en la UI (SC-001)
- **Constraints**: `Clock` inyectado para determinismo en tests; bloqueo pesimista en conformidad; Art. 18 (inmutabilidad constitucional)

---

## 3. Constitution Check

| Invariante Constitucional | Estado | Evidencia en el Diseño |
| :--- | :--- | :--- |
| **Art. 1 (Spec como verdad)** | **PASS** | Todas las funcionalidades derivan de HU-08 y sus 4 clarificaciones registradas. |
| **Art. 5 (4 capas desacopladas)** | **PASS** | `PerformanceReviewController` → `PerformanceReviewService` → `PerformanceReviewRepository` → PostgreSQL. |
| **Art. 8 (PostgreSQL real)** | **PASS** | Restricción `CHECK` y trigger validados con Testcontainers en `PerformanceReviewIntegrationTest`. |
| **Art. 10 (Autorización en servidor)** | **PASS** | Rol verificado por `@PreAuthorize`; alcance derivado siempre del `Principal` en sesión, nunca del cliente. |
| **Art. 18 (Inmutabilidad evaluaciones)** | **PASS** | Doble garantía: lógica en `PerformanceReviewServiceImpl.acknowledge()` + trigger `trg_performance_reviews_immutable_finalized`. |
| **Art. 19 (Errores RFC 9457)** | **PASS** | `PerformanceReviewAlreadyFinalizedException` → `409 Conflict`; `AccessDeniedException` → `403 Forbidden`. |
| **Art. 22 (Test-First con Testcontainers)** | **PASS** | Test de integración contra PostgreSQL real con escenarios de conformidad e inmutabilidad. |
| **Art. 23 (Simplicidad)** | **PASS** | Sin librerías externas; entidad de dominio con método `acknowledge()` encapsulado; cuatro endpoints. |

---

## 4. Project Structure

### Documentation (`specs/spec-008/`)
```text
specs/spec-008/
├── spec.md              # Especificación funcional clarificada
├── plan.md              # Este plan de implementación técnico
├── research.md          # Decisiones arquitectónicas y alternativas
├── data-model.md        # Entidad JPA, DDL, DTOs y repositorio
├── quickstart.md        # Guía de ejecución y validación
└── contracts/
    └── performance-reviews-api.yaml  # Contrato OpenAPI 3.0
```

### Source Code
```text
backend/src/main/java/com/iw/rrhh/
└── performance/
    ├── controller/
    │   └── PerformanceReviewController.java
    ├── service/
    │   ├── PerformanceReviewService.java
    │   ├── PerformanceReviewServiceImpl.java
    │   └── PerformanceReviewAlreadyFinalizedException.java
    ├── domain/
    │   ├── PerformanceReview.java
    │   └── PerformanceReviewStatus.java
    ├── repository/
    │   └── PerformanceReviewRepository.java
    └── dto/
        ├── CreatePerformanceReviewRequest.java
        ├── AcknowledgePerformanceReviewRequest.java
        ├── PerformanceReviewResponse.java
        └── PerformanceReviewEmployeeResponse.java

backend/src/main/resources/db/migration/
├── V1__init_shared_schema.sql   # Tabla performance_reviews (existente)
└── V4__immutable_finalized_performance_reviews.sql  # Trigger inmutabilidad

backend/src/test/java/com/iw/rrhh/performance/
└── PerformanceReviewIntegrationTest.java

frontend/src/app/features/performance/
├── components/
│   └── performance-reviews/
│       └── performance-reviews.component.ts
├── models/
│   └── performance-review.models.ts
└── services/
    ├── performance-review.service.ts
    └── performance-review.service.spec.ts
```

---

## 5. Complexity Tracking

> **Sin violaciones**: El diseño usa Spring Data JPA con bloqueo pesimista puntual. El trigger PostgreSQL es la única pieza de lógica fuera de la JVM, y está justificada por el Art. 18 (inmutabilidad constitucional irrenunciable). No se introducen dependencias nuevas ni patrones arquitectónicos adicionales.
