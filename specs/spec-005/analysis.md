# Specification Analysis Report: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` · HU-05  
**Fecha de análisis**: 2026-11-20  
**Estado**: Completado (implementación verificada contra código real)  
**Responsable**: Andrei Radu (Integrante 3 — Gestión y Supervisión del Responsable)

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [data-model.md](data-model.md)
- [contracts/leave-resolution-api.yaml](contracts/leave-resolution-api.yaml)
- [.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- Código implementado: `backend/src/main/java/com/iw/rrhh/leave/` y `frontend/src/app/features/team-calendar/`

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **HIGH** | Constitución vs spec-005 (Fase 9 y 10) | La constitución inicial no contemplaba la figura de `DIRECCION_GENERAL` ni que `RRHH` pudiera actuar como resolutor de su propio equipo sin ser superusuario general. | **RESUELTO**: Enmienda constitucional (versiones 1.1.0 y 1.2.0) formalizada; incorporados requisitos FR-015, FR-016, FR-017 y FR-018. |
| **U1** | Underspecification | **HIGH** | `spec.md Clarifications` | Ausencia de comportamiento ante baja del superior cuando existían solicitudes `PENDING` de subordinados. | **RESUELTO**: Implementado `LeaveRequestAutoCancelService` que cancela automáticamente las solicitudes huérfanas en la misma transacción de baja de HU-01. |
| **I2** | Inconsistency | **MEDIUM** | `spec.md FR-008` vs Balance | Asignación de días en solicitudes que cruzan dos años naturales (ej. 28 dic a 5 ene). | **RESUELTO**: Creado componente `LeaveDayAllocator` que reparte estrictamente los días laborables entre ambos ejercicios fiscales. |
| **U2** | Underspecification | **MEDIUM** | `spec.md FR-003` | Validación de espacios en blanco en el motivo de rechazo. | **RESUELTO**: Recorte estricto (`trim()`) y validación `@Size(min=5, max=500)` garantizando motivos sustantivos. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Bandeja de pendientes del equipo) | Sí | T007, T013, T015, T022 | Filtro por subordinados directos del responsable autenticado. |
| **FR-002** (Aprobación a `APPROVED` con comentario opcional) | Sí | T008, T011, T014, T017 | Actualización de estado y persistencia de comentario (hasta 500 car.). |
| **FR-003** (Rechazo a `REJECTED` con motivo obligatorio) | Sí | T008, T011, T014, T017 | Validación estricta de 5 a 500 caracteres limpios de espacios. |
| **FR-004** (Inmutabilidad de solicitudes resueltas) | Sí | T008, T011, T021 | Rechaza re-resolución con HTTP `409 Conflict`. |
| **FR-005** (Notificación interna automática al empleado) | Sí | T004, T008, T011, T023 | Inserción en tabla `notifications` con tipo y mensaje descriptivo. |
| **FR-006** (Deducción y actualización del saldo HU-02) | Sí | T008, T011, T025 | El saldo dinámico refleja inmediatamente la reducción de disponibles. |
| **FR-007** (Revalidación atómica de saldo en aprobación) | Sí | T008, T011, T020 | Bloquea aprobación con `422` si otra solicitud consumió el crédito. |
| **FR-008** (Reparto de días en solicitudes que cruzan años) | Sí | T035, T038, T040 | `LeaveDayAllocator` divide días por año natural. |
| **FR-009** (Bloqueo en años naturales cerrados) | Sí | T036, T039 | Bloquea aprobación con `422` si incluye fechas en ejercicios cerrados. |
| **FR-010** (Aislamiento de equipo: prohibida resolución ajena) | Sí | T008, T011, T024 | Lanza 403 Forbidden si el solicitante no es subordinado directo. |
| **FR-011** (Prohibición de auto-resolución) | Sí | T008, T011, T024 | Nadie puede resolver sus propias solicitudes (403 Forbidden). |
| **FR-012** (Auditoría: `resolved_by` y `resolved_at`) | Sí | T005, T008, T011 | Campos persistidos inmutables con usuario y timestamp del servidor. |
| **FR-013** (Concurrencia optimista / atómica) | Sí | T011, T021 | Transacciones serializadas con control de versiones en JPA. |
| **FR-014** (Recarga de interfaz tras 409 o 404) | Sí | T018, T022 | Diálogo reactivo recarga la bandeja para refrescar estado en UI. |
| **FR-015** (Bloqueo sin resolutor válido) | Sí | T068, T070, T080 | Valida que el empleado tenga manager activo y con rol adecuado. |
| **FR-016** (Cancelación automática de pendientes al darse de baja superior) | Sí | T071, T073, T078 | `LeaveRequestAutoCancelService` cancela y notifica en la misma transacción. |
| **FR-017** (Soporte de rol `DIRECCION_GENERAL`) | Sí | T057, T074, T079 | Resolutor válido para sus subordinados; sin saldo propio ni solicitudes. |
| **FR-018** (`RRHH` como resolutor de sus subordinados directos) | Sí | T085, T095, T100 | Alcance limitado a su equipo directo sin superusuario de resolución. |
| **SC-001 a SC-006** (Criterios de éxito y rendimiento) | Sí | T020 a T025, T081 | 100% verificado en tests unitarios, de integración y E2E. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales tras las enmiendas v1.1.0 y v1.2.0.*

- **Art. 10 (Seguridad en servidor)**: Validación jerárquica estricta (`manager_id == authenticatedEmployee.id`); prohibición taxativa de auto-aprobación.
- **Art. 12 (Saldo derivado)**: Las aprobaciones consolidan días disfrutados sin tocar contadores mutables.
- **Art. 13 (Ciclo transaccional de ausencias)**: Transición atómica de estados con rastro auditable completo.
- **Art. 19 (Problem Details RFC 9457)**: Códigos de error semánticos: 409 (conflicto/ya resuelta), 422 (saldo insuficiente/año cerrado), 403 (acceso denegado).

---

## 4. Unmapped Tasks

*Ninguna.* Todas las 103 tareas de las sucesivas fases (incluidas las fases 8, 9 y 10 de refinamiento) están trazadas a requisitos.

---

## 5. Métricas

- **Total Requirements (FR + SC)**: 24
- **Requisitos cubiertos con tareas y código**: 24 (100%)
- **Hallazgos identificados**: 4 (todos resueltos e implementados)
- **Estado general**: **Aprobado con refinamiento exhaustivo**
