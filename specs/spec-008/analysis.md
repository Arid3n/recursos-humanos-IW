# Specification Analysis Report: spec-008 — Evaluación de Desempeño con Conformidad

**Feature**: `spec-008` · HU-08  
**Fecha de análisis**: 2026-11-20  
**Estado**: Completado (implementación verificada contra código real)

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [data-model.md](data-model.md)
- [contracts/performance-reviews-api.yaml](contracts/performance-reviews-api.yaml)
- Código implementado: `backend/src/main/java/com/iw/rrhh/performance/`
- Migraciones: `V1__init_shared_schema.sql`, `V4__immutable_finalized_performance_reviews.sql`

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **LOW** | `spec.md Key Entities` vs implementación | La spec lista `employeeAcknowledgedAt` y `finalizedAt` como campos separados, pero en el método `acknowledge()` ambos se asignan al mismo `Instant.now(clock)`. | Comportamiento correcto y documentado en `research.md §2`; en el caso de conformidad inmediata coinciden por diseño. |
| **C1** | Coverage Gap | **LOW** | `spec.md Edge Case` | La spec menciona «Auditoría temporal: fecha generada con `Clock` inyectado». No había tarea explícita para test unitario con `Clock.fixed`. | **CUBIERTO**: `PerformanceReviewIntegrationTest` usa `Clock` fijo para verificar `finalizedAt`. |
| **U1** | Underspecification | **LOW** | `spec.md AS-003 Inmutabilidad` | El escenario no especifica el código HTTP devuelto al intentar dar conformidad a una evaluación ya formalizada. | **RESUELTO en implementación**: `409 Conflict` vía `PerformanceReviewAlreadyFinalizedException`; documentado en contrato. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Responsable crea solo para su equipo) | Sí | T009, T010, T011 | Validación de `employee.manager.id == authenticatedEmployee.id` en servicio; test negativo. |
| **FR-002** (Campos obligatorios de la evaluación) | Sí | T006, T009 | `@NotBlank` y `@Min/@Max` en `CreatePerformanceReviewRequest`; `@Valid` en controlador. |
| **FR-003** (Estado inicial `PENDING_ACKNOWLEDGMENT`) | Sí | T004, T009 | Constructor de `PerformanceReview` asigna el estado; test verifica el estado devuelto en `201`. |
| **FR-004** (Empleado puede revisar, comentar y dar conformidad) | Sí | T009, T010, T013 | Endpoint `POST /acknowledge` para `EMPLEADO`; campo `employeeComments` opcional. |
| **FR-005** (Estado → `FINALIZED` + `finalizedAt` del servidor) | Sí | T009, T011 | `Instant.now(clock)` en `acknowledge()`; test con `Clock.fixed` verifica `finalizedAt`. |
| **FR-006** (Inmutabilidad absoluta en `FINALIZED`) | Sí | T007, T009, T011 | `PerformanceReviewAlreadyFinalizedException` → `409`; trigger `trg_performance_reviews_immutable_finalized` en BD; test directo al trigger. |
| **FR-007** (Aislamiento: empleado → propias; responsable → equipo actual) | Sí | T009, T010, T011 | `findByEmployeeIdOrderByPeriodDescCreatedAtDesc` y `findByEmployeeManagerIdOrderByCreatedAtDesc`; tests de aislamiento con usuarios distintos. |
| **FR-008** (Valoración 1–5) | Sí | T006, T011 | `@Min(1) @Max(5)` en DTO; `CHECK` en BD; test con `rating=0` y `rating=6` → `400`. |
| **SC-001** (Circuito ≤ 3 pasos en la UI) | Sí | T013 | Flujo guiado: selector de empleado → formulario → submit (1 paso); conformidad: detalle → comentarios → botón (1 paso por vista). |
| **SC-002** (100% inmutabilidad) | Sí | T011 | Test verifica que segundo `acknowledge` → `409` y que el trigger BD rechaza UPDATE directo. |
| **SC-003** (0% violaciones de privacidad) | Sí | T009, T011 | Tests de aislamiento: empleado A no ve evaluaciones de empleado B; responsable X no ve equipo de responsable Y. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*

- **Art. 5 (Capas desacopladas)**: Respetado — Controller → Service → Repository → PostgreSQL.
- **Art. 10 (Autorización en servidor)**: Respetado — `@PreAuthorize` por método; alcance derivado siempre del `Principal`.
- **Art. 18 (Inmutabilidad evaluaciones)**: Respetado — doble garantía en aplicación y BD (trigger).
- **Art. 19 (RFC 9457 Problem Details)**: Respetado — `409` para evaluaciones ya formalizadas; `403` para accesos no autorizados.
- **Art. 22 (Test-First y Testcontainers)**: Respetado — test de integración contra PostgreSQL real con `Clock` fijo.
- **Art. 23 (Simplicidad)**: Respetado — sin librerías externas; entidad de dominio con método `acknowledge()` encapsulado.

---

## 4. Unmapped Tasks

*Ninguna.* Todas las tareas mapean a requisitos funcionales, criterios de éxito o setup/pulido del módulo.

---

## 5. Métricas

- **Total Requirements (FR + SC):** 11
- **Total Tasks:** 19
- **Coverage %:** **100%** (11/11 requisitos con al menos 1 tarea asociada)
- **Ambiguity Count:** 0
- **Critical Issues Count:** 0
- **High Issues Count:** 0

---

## 6. Próximas Acciones Recomendadas

1. *(Opcional)* Aclarar en `spec.md AS-003` que el intento de segunda conformidad retorna `409 Conflict`.
2. *(Opcional)* Documentar en `spec.md Edge Cases` el comportamiento del trigger de BD como capa adicional de protección (Art. 18).
