# Specification Analysis Report: spec-003 — Solicitud de Vacaciones o Permisos

**Feature**: `spec-003` · HU-03  
**Fecha de análisis**: 2026-11-20  
**Estado**: Completado (implementación verificada contra código real)  
**Responsable**: Iván Soler (Integrante 2 — Servicios y derechos del Empleado)

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [data-model.md](data-model.md)
- [contracts/leave-request-api.yaml](contracts/leave-request-api.yaml)
- [.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- Código implementado: `backend/src/main/java/com/iw/rrhh/leave/` y `frontend/src/app/features/leave/`

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **HIGH** | `spec.md` vs Constitución v1.1.0/v1.2.0 | Inicialmente la spec no contemplaba la restricción del rol `DIRECCION_GENERAL` ni el bloqueo para empleados sin resolutor válido activo. | **RESUELTO**: Incorporadas aclaraciones de 2026-11-19, requisitos FR-009, FR-010, FR-011 y endpoint `/eligibility`. |
| **U1** | Underspecification | **MEDIUM** | `spec.md FR-006` vs Formulario | Reglas de longitud y obligatoriedad condicional del campo `justification`. | **RESUELTO**: Obligatorio (máx. 255 caracteres) si `leaveType == PERMIT`, opcional si `VACATION`. Validado con tests unitarios. |
| **I2** | Inconsistency | **MEDIUM** | `contracts/leave-request-api.yaml` | Faltaba la documentación del endpoint `GET /api/v1/leave/requests/eligibility`. | **RESUELTO**: Endpoint documentado en OpenAPI devolviendo `LeaveEligibilityResponse` (`eligible`, `reason`). |
| **C1** | Coverage Gap | **LOW** | `tasks.md` | Validación de días que cruzan cambio de año natural en HU-03. | **DOCUMENTADO COMO LÍMITE CONOCIDO**: HU-03 valida contra el año de inicio; HU-05 revalida por año natural en la aprobación (`LeaveDayAllocator`). |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Formulario tipo VACATION/PERMIT y rango fechas) | Sí | T012, T014, T015 | Formulario reactivo con `mat-select` y selector de rango en Angular. |
| **FR-002** (Cálculo oficial de días laborables en servidor) | Sí | T003, T007, T010, T011, T013 | `HolidayCalendarService` descuenta fines de semana y festivos oficiales; endpoint `/calculate-days`. |
| **FR-003** (Reserva preventiva de saldo: balance disponible) | Sí | T005, T008, T010 | Comprueba que `disponible = correspondientes - disfrutados - pendientes >= solicitados`. |
| **FR-004** (Validación de fechas: fin >= inicio y startDate >= hoy) | Sí | T008, T010, T014 | `minDate = today` en datepicker; validación backend y constraint `CHECK` en BD. |
| **FR-005** (Bloqueo de solapamiento de fechas) | Sí | T005, T006, T008, T010 | `LeaveRequestRepository.hasOverlappingRequests` lanza `LeaveRequestOverlapException` (409). |
| **FR-006** (Justificación condicional por tipo de ausencia) | Sí | T004, T008, T010, T014 | Obligatoria en `PERMIT`, opcional en `VACATION`. |
| **FR-007** (Días laborables completos, entero >= 1) | Sí | T004, T010 | Unidad mínima 1 jornada laborable completa. |
| **FR-008** (Aviso de cortesía si antelación < 7 días) | Sí | T014, T015 | Banner informativo no bloqueante en la interfaz. |
| **FR-009** (Bloqueo si no hay resolutor válido asignado) | Sí | T008, T010, T014 | Error 422 con mensaje descriptivo si no tiene responsable activo. |
| **FR-010** (Bloqueo de solicitud para DIRECCION_GENERAL) | Sí | T008, T010, T016 | Respuesta 422 («Dirección General no solicita ausencias»); enlace oculto en menú. |
| **FR-011** (Consulta de elegibilidad previa) | Sí | T010, T011, T013, T014 | `GET /api/v1/leave/requests/eligibility` consultado antes de renderizar el formulario. |
| **SC-001** (Solicitud en < 1 min con cálculo inmediato) | Sí | T011, T014 | Cálculo interactivo debounced (< 300 ms). |
| **SC-002** (100% solapamientos bloqueados) | Sí | T008, T009 | Verificado en tests unitarios y de integración. |
| **SC-003** (100% reserva preventiva de saldo respetada) | Sí | T008, T009 | Cobertura total en `LeaveRequestServiceTest`. |
| **SC-004** (Cálculo único oficial en servidor) | Sí | T003, T007, T010 | Sin duplicación de lógica de festivos en el cliente (Art. 14). |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*

- **Art. 5 (4 capas desacopladas)**: Respetado (`LeaveRequestController` → `LeaveRequestService` → `LeaveRequestRepository` → PostgreSQL).
- **Art. 10 (Seguridad y autorización en servidor)**: Respetado. El `employeeId` se extrae del `Principal` autenticado, nunca de parámetros del cliente.
- **Art. 12 (Saldo como valor derivado)**: Respetado. Las solicitudes pendientes no mutan tablas de saldo; se agregan dinámicamente en el cálculo preventivo.
- **Art. 14 (Gestión de fechas y días laborables)**: Respetado. `HolidayCalendarService` centraliza en el backend el cálculo en `Europe/Madrid`.
- **Art. 19 (RFC 9457 Problem Details)**: Respetado. `LeaveRequestOverlapException` (409) e `InsufficientLeaveBalanceException` (422) mapeadas en `GlobalExceptionHandler`.
- **Art. 21 (Accesibilidad WCAG 2.1 AA)**: Respetado. Formularios con labels explícitos, mensajes de error accesibles y controles Angular Material.

---

## 4. Unmapped Tasks

*Ninguna.* Todas las 25 tareas (`T001` a `T019` más subtareas) trazan directamente a los requisitos de HU-03.

---

## 5. Métricas

- **Total Requirements (FR + SC)**: 15
- **Requisitos cubiertos con tareas y código**: 15 (100%)
- **Hallazgos identificados**: 4 (todos resueltos o documentados)
- **Estado general**: **Aprobado sin brechas críticas**
