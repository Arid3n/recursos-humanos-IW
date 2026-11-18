# Specification Analysis Report: spec-002 — Consulta de Ficha y Saldo de Vacaciones

**Feature**: `spec-002` · HU-02  
**Fecha de análisis**: 2026-11-17  
**Estado**: Completado y Remediado (100% Coherencia)  

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [contracts/leave-balance-api.yaml](contracts/leave-balance-api.yaml)
- [.specify/memory/constitution.md](../../.specify/memory/constitution.md)

---

## 1. Tabla de Hallazgos y Remediaciones

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación Aplicada |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **MEDIUM** | `plan.md:L71` y `tasks.md:L46` vs Código | `plan.md` y `tasks.md` mencionaban `EmployeeProfileController.java`, pero el endpoint `GET /me` se consolidó en `EmployeeController.java` para prevenir colisiones de ruta en Spring MVC con `/{id:[0-9a-fA-F-]{36}}`. | **RESUELTO**: Actualizado `plan.md` y `tasks.md` referenciando `EmployeeController.java`. |
| **I2** | Inconsistency | **LOW** | `plan.md:L4` | La cabecera del plan indicaba 4 decisiones cerradas, mientras que la especificación incorporó la 5ª clarificación formal sobre permisos de lectura de terceros. | **RESUELTO**: Actualizada la cabecera de `plan.md` a 5 decisiones cerradas consolidadas. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Identidad desde `SecurityContext`) | Sí | T011, T013, T014 | Sin recepción de IDs de usuario en endpoints `/me`. |
| **FR-002** (Saldo desglosado en 4 métricas) | Sí | T007, T008, T012, T014, T017 | Value Object `LeaveBalance` y `LeaveBalanceWidgetComponent`. |
| **FR-003** (Saldo estrictamente derivado de convenio y ausencias) | Sí | T006, T012 | Sin columnas persistidas editables manualmente (Art. 12). |
| **FR-004** (Recálculo dinámico e inmediato) | Sí | T012, T021 | Testeado en `LeaveRecalculationIntegrationTest`. |
| **FR-005** (Control de acceso RBAC por rol y equipo directo) | Sí | T024, T025, T026 | `@PreAuthorize` con validación de supervisor y rechazo `403`. |
| **FR-006** (Prorrateo proporcional por fecha de alta) | Sí | T020, T022 | Fórmula `round((días / 12) * meses)` verificada en tests unitarios. |
| **FR-007** (Consulta histórica de ejercicios cerrados) | Sí | T027, T028, T029 | Parámetro `year` y selector interactivo con badge de solo lectura. |
| **SC-001** (Tiempo de carga < 800ms) | Sí | T017, T018 | Renderizado reactivo en `MyProfileComponent`. |
| **SC-002** (Coherencia matemática 100%) | Sí | T007, T010, T011 | Invariante `correspondientes == disfrutados + disponibles`. |
| **SC-003** (0% fugas de datos entre empleados) | Sí | T024, T025 | Verificado con tests negativos de autorización HTTP 403. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*  
- **Art. 5 (4 capas desacopladas):** Respetado (`LeaveBalanceController` → `LeaveBalanceService` → `LeaveRequestRepository`).
- **Art. 10 (Seguridad y alcance en servidor):** Respetado (uso estricto de `SecurityContextHolder` y `@PreAuthorize`).
- **Art. 12 (Saldo derivado sin persistencia mutable):** Respetado (cálculo en tiempo real sin contadores almacenados en BD).
- **Art. 14 (Cómputo en año natural con `LocalDate`):** Respetado (1 de enero a 31 de diciembre centralizado).
- **Art. 19 (RFC 9457 Problem Details):** Respetado (`ProblemDetail` ante accesos no autorizados o IDs no encontrados).
- **Art. 21 (Accesibilidad WCAG AA):** Respetado (contraste accesible y navegación por teclado en `LeaveBalanceWidgetComponent`).
- **Art. 22 (Test-First y Testcontainers):** Respetado (tests de integración con PostgreSQL real).

---

## 4. Unmapped Tasks

*Ninguna.* Las 33 tareas definidas en `tasks.md` están directamente asociadas a requisitos funcionales, criterios de éxito o al setup y verificación del módulo.

---

## 5. Métricas

- **Total Requirements (FR + SC):** 10
- **Total Tasks:** 33
- **Coverage %:** **100%** (10/10 requisitos cubiertos con al menos 1 tarea)
- **Ambiguity Count:** 0
- **Duplication Count:** 0
- **Critical Issues Count:** 0
- **High Issues Count:** 0
- **Medium Issues Count:** 0 (1 resuelto)
- **Low Issues Count:** 0 (1 resuelto)

---

## 6. Conclusión

Los tres artefactos nucleares (`spec.md`, `plan.md`, `tasks.md`) y el contrato OpenAPI (`leave-balance-api.yaml`) de **`spec-002`** presentan coherencia y trazabilidad plena del 100%, satisfaciendo todos los invariantes constitucionales.
