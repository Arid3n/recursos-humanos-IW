# Specification Analysis Report: spec-001 — Alta de Empleados y Trayectoria Contractual

**Feature**: `spec-001` · HU-01  
**Fecha de análisis**: 2026-11-17  
**Estado**: Completado (Listo para remediación / verificación)  

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [contracts/employee-api.yaml](contracts/employee-api.yaml)
- [.specify/memory/constitution.md](../../.specify/memory/constitution.md)

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación Aplicada |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **HIGH** | `contracts/employee-api.yaml` vs `tasks.md:L94-96` | El endpoint de actualización de datos personales (`PUT /api/v1/employees/{id}/personal-data`) no estaba documentado en OpenAPI. | **RESUELTO**: Endpoint y esquema `UpdatePersonalDataRequest` añadidos a `employee-api.yaml`. |
| **U1** | Underspecification | **MEDIUM** | `tasks.md:L66` (T025) | La tarea `T025` figuraba desmarcada (`- [ ]`) y faltaba el diálogo de variación en frontend. | **RESUELTO**: Implementado `ContractVariationComponent` con integración en `ContractHistoryComponent`, y `T025` marcado como `[x]`. |
| **I2** | Inconsistency | **MEDIUM** | `plan.md:L93-104` vs `tasks.md:L48-50` vs Código | Discrepancia de rutas frontend (`features/employees/` vs `employees/`). | **RESUELTO**: Normalizadas todas las referencias a `frontend/src/app/employees/` en `plan.md` y `tasks.md`. |
| **D1** | Duplication | **LOW** | `spec.md:L74-75, L81-82` | Solapamiento en requisitos: FR-009 reitera la obligatoriedad de fecha de efectos e inmutabilidad. | Mitigado por diseño inmutable append-only. |
| **I3** | Inconsistency | **LOW** | `plan.md:L97` vs `tasks.md:L66` | Variación en `employee-detail/` vs componente `contract-variation/`. | Alinear el desglose arquitectónico del plan con la lista de tareas. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Alta con datos obligatorios por RRHH) | Sí | T010, T012, T014, T015, T016, T017, T018, T019 | Cubierto en backend, frontend y test de integración. |
| **FR-002** (Fecha de efectos obligatoria en variación) | Sí | T011, T021, T022, T023, T025 | T025 (UI) pendiente de verificación. |
| **FR-003** (Historial contractual append-only) | Sí | T011, T013, T022, T024 | Art. 11 Constitución garantizado en BD y servicio. |
| **FR-004** (Vigencia contractual derivada) | Sí | T013, T026, T027, T028, T029, T030 | Filtro por `effective_from <= :date`. |
| **FR-005** (Errores de validación por campo) | Sí | T009, T017, T019 | RFC 9457 ProblemDetail en `GlobalExceptionHandler`. |
| **FR-006** (Restricción rol RRHH 403 Forbidden) | Sí | T016, T023, T033, T034 | `@PreAuthorize` en servidor y tests negativos. |
| **FR-007** (Fechas retroactivas cronológicas) | Sí | T021, T022, T024, T025 | Testeado en `ContractVariationIntegrationTest`. |
| **FR-008** (Usuario automático y clave provisional) | Sí | T004, T005, T008, T014, T015, T020 | Contraseña provisional en modal sin SMTP. |
| **FR-009** (Datos personales vs. contractuales) | Sí | T031, T032, T033 | Endpoint `personal-data` implementado en backend. |
| **FR-010** (Baja lógica `active = false`) | Sí | T032, T033, T034 | Revocación de acceso con persistencia histórica. |
| **FR-011** (Manager obligatorio salvo CEO) | Sí | T010, T015 | Excepción documentada y validada en servicio. |
| **SC-001** (Alta completable en < 2 min) | Sí | T019 | Formulario reactivo asistido con validaciones inmediatas. |
| **SC-002** (100% cambios inmutables sin sobrescritura) | Sí | T024 | Validado en test de integración con Testcontainers. |
| **SC-003** (0% accesos no autorizados) | Sí | T034 | Suite de tests de seguridad negativa implementada. |
| **SC-004** (Trazabilidad auditable `created_by/at`) | Sí | T011, T015, T022 | Columnas de auditoría persistidas en PostgreSQL. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*  
- **Art. 5 (4 capas desacopladas):** Respetado (Controlador → Servicio → Repositorio → PostgreSQL).
- **Art. 8 (Persistencia e integridad en BD):** Respetado (migraciones Flyway y claves foráneas).
- **Art. 10 (Seguridad en servidor):** Respetado (autorización basada estrictamente en `Principal` y `@PreAuthorize`).
- **Art. 11 (Histórico inmutable):** Respetado (modelo *append-only* en `ContractRecord`).
- **Art. 19 (RFC 9457 Problem Details):** Respetado (`ProblemDetail` con `invalidFields`).
- **Art. 22 (Test-First y Testcontainers):** Respetado (tests de integración contra PostgreSQL real).

---

## 4. Unmapped Tasks

*Ninguna.* Todas las tareas de infraestructura, nucleares y de historia de usuario mapean a los requisitos funcionales, criterios de éxito o al setup/pulido del módulo.

---

## 5. Métricas

- **Total Requirements (FR + SC):** 15
- **Total Tasks:** 38
- **Coverage %:** **100%** (15/15 requisitos con al menos 1 tarea asociada)
- **Ambiguity Count:** 0
- **Duplication Count:** 1 (Menor)
- **Critical Issues Count:** 0
- **High Issues Count:** 1 (Falta de endpoint en OpenAPI)

---

## 6. Próximas Acciones Recomendadas

1. **Resolver discrepancia de contrato (I1):** Añadir `/api/v1/employees/{id}/personal-data` en `specs/spec-001/contracts/employee-api.yaml`.
2. **Revisar estado de T025 (U1):** Verificar si el diálogo de variaciones contractuales en frontend requiere completarse o si se absorbió en otra vista.
3. **Alinear rutas de carpetas (I2):** Ajustar las menciones de `features/employees` a `employees` en `plan.md` y `tasks.md`.
