# Implementation Plan: spec-005 — Aprobación o Rechazo de Solicitudes

**Branch**: `spec-005` | **Date**: 2026-11-03 | **Spec**: [specs/spec-005/spec.md](spec.md)

**Input**: Feature specification from `/specs/spec-005/spec.md`

---

## Summary

La funcionalidad **HU-05 (`spec-005`)** permite al responsable de equipo consultar las solicitudes en estado `PENDING` de las personas a su cargo (empleados cuyo `manager_id` es el responsable en sesión) y aprobarlas o rechazarlas. El rechazo exige un motivo de al menos 5 caracteres; la aprobación admite un comentario opcional. La resolución se ejecuta en una única transacción que: bloquea la solicitud y la ficha del empleado (`SELECT ... FOR UPDATE`), revalida el saldo si es `VACATION`, registra `status`, `resolved_by`, `resolved_at` y motivo, y crea la notificación persistida para el empleado. El empleado consulta sus notificaciones desde un distintivo en la cabecera y puede marcarlas como leídas. El saldo sigue siendo derivado (HU-02): una solicitud `APPROVED` pasa a contar como días disfrutados sin contadores adicionales.

---

## Technical Context

**Language/Version**: Java 21 LTS (Backend) · TypeScript 5.x estricto / Angular 21 (Frontend)

**Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA, Validation, Flyway) · Angular Material 21 (table, paginator, dialog, menu, badge, snack-bar)

**Storage**: PostgreSQL 16 — tablas existentes `leave_requests` y `notifications` (V1). Nueva migración `V3__leave_resolution_constraints.sql` con restricciones de resolución.

**Testing**: JUnit 5 + Mockito (unitarios de servicio) · Spring Boot Test + MockMvc + Testcontainers PostgreSQL (integración) · Vitest (componentes y servicios Angular)

**Target Platform**: Navegadores modernos (escritorio y móvil) · API REST en contenedor Linux

**Project Type**: Aplicación web (SPA Angular + API REST Spring Boot)

**Performance Goals**: Resolución completa (estado + saldo + notificación) en < 500 ms; bandeja paginada de 10 elementos en < 500 ms

**Constraints**: RFC 9457 ProblemDetail (Art. 19) · WCAG 2.1 AA (Art. 21) · autorización por rol y alcance en servidor (Art. 10) · atomicidad de la resolución (Art. 13.7) · seguridad ante concurrencia del saldo (Art. 12.6)

**Scale/Scope**: Equipos de decenas de personas; 3 endpoints de resolución + 3 de notificaciones; 2 pantallas/componentes nuevos en frontend

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Artículo | Verificación | Estado |
|---|---|---|
| **Art. 1-3** (Spec como fuente de verdad, ambigüedad) | Plan basado en `spec.md` con aclaraciones de 2026-10-27; sin `[NEEDS CLARIFICATION]` pendientes. | ✅ |
| **Art. 2** (Trazabilidad HU → CA → Test) | Tests etiquetados `HU-05.CA-1` … `HU-05.CA-5` (ver `quickstart.md` §3). | ✅ |
| **Art. 4** (Corte vertical) | Bandeja + resolución + notificación cruzan UI → API → BD. | ✅ |
| **Art. 5 / 7** (Capas) | `controller` → `service` (`@Transactional`) → `repository`; DTOs `record`, sin exponer entidades. Nuevo paquete `notification` previsto en Art. 7.2. | ✅ |
| **Art. 8** (Persistencia) | Migración Flyway versionada; refuerzo en BD con `CHECK` y *trigger* de inmutabilidad. Índice existente `idx_leave_employee_status` cubre la bandeja. | ✅ |
| **Art. 9** (API REST) | `/api/v1/leave/team-requests` paginado; `POST .../approve` y `.../reject`; `/api/v1/notifications` paginado. Contrato en `contracts/`. | ✅ |
| **Art. 10** (Autorización 2 niveles) | Rol: `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`. Alcance: el servicio compara `employee.manager.id` con el empleado de la sesión → `403`. Tests negativos incluidos. | ✅ |
| **Art. 12** (Saldo derivado y concurrencia) | Sin contadores; revalidación con `LeaveBalanceService` tras bloquear la fila del empleado (`PESSIMISTIC_WRITE`). | ✅ |
| **Art. 13** (Ciclo de vida) | Solo `PENDING → APPROVED/REJECTED`; resuelta → `409`; registro de `resolved_by`/`resolved_at`; todo en una transacción. | ✅ |
| **Art. 15** (Notificaciones) | Notificación persistida en la misma transacción; canal único interno. | ✅ |
| **Art. 19** (Errores) | Nueva `LeaveRequestAlreadyResolvedException` → `409` en `GlobalExceptionHandler`; validación de motivo con `jakarta.validation` → `400` con `invalidFields`. | ✅ |
| **Art. 20** (Auditoría y datos personales) | Log de auditoría con identificadores (UUID), sin nombres ni motivos; accesos denegados registrados. | ✅ |
| **Art. 21** (Accesibilidad) | Estados y contador con texto + icono; diálogos con foco y `mat-error` junto al campo. | ✅ |
| **Art. 22** (Tests obligatorios) | Autorización negativa, transición inválida, saldo insuficiente y concurrencia sobre el saldo cubiertos. | ✅ |
| **Art. 23** (Simplicidad) | Reutiliza `LeaveBalanceService`, `PageResponse` y el patrón de servicio existente; sin dependencias nuevas. | ✅ |

**Re-evaluación post-diseño**: ✅ Sin violaciones. No aplica *Complexity Tracking*.

---

## Project Structure

### Documentation (this feature)

```text
specs/spec-005/
├── spec.md              # Especificación (/speckit-specify)
├── plan.md              # Este plan (/speckit-plan)
├── research.md          # Fase 0: decisiones técnicas
├── data-model.md        # Fase 1: entidades, DTOs, transiciones
├── quickstart.md        # Fase 1: guía de validación
├── contracts/
│   └── leave-resolution-api.yaml
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks, NO creado aquí)
```

### Source Code (repository root)

```text
backend/src/main/
├── java/com/iw/rrhh/
│   ├── common/exception/
│   │   ├── LeaveRequestAlreadyResolvedException.java      # NUEVO (409)
│   │   └── GlobalExceptionHandler.java                    # MODIFICADO
│   ├── employee/repository/EmployeeRepository.java        # MODIFICADO (findByIdForUpdate)
│   ├── leave/
│   │   ├── controller/LeaveResolutionController.java      # NUEVO
│   │   ├── domain/LeaveRequest.java                       # MODIFICADO (approve/reject)
│   │   ├── dto/TeamLeaveRequestResponse.java              # NUEVO
│   │   ├── dto/ApproveLeaveRequest.java                   # NUEVO
│   │   ├── dto/RejectLeaveRequest.java                    # NUEVO
│   │   ├── dto/LeaveResolutionResponse.java               # NUEVO
│   │   ├── repository/LeaveRequestRepository.java         # MODIFICADO (bandeja + lock)
│   │   └── service/
│   │       ├── LeaveResolutionService.java                # NUEVO
│   │       └── LeaveResolutionServiceImpl.java            # NUEVO
│   ├── notification/
│   │   ├── controller/NotificationController.java         # NUEVO
│   │   ├── domain/Notification.java                       # NUEVO
│   │   ├── dto/NotificationResponse.java                  # NUEVO
│   │   ├── dto/UnreadCountResponse.java                   # NUEVO
│   │   ├── repository/NotificationRepository.java         # NUEVO
│   │   └── service/
│   │       ├── NotificationService.java                   # NUEVO
│   │       └── NotificationServiceImpl.java               # NUEVO
│   └── security/config/SecurityDataInitializer.java       # MODIFICADO (usuario responsable de pruebas)
└── resources/db/migration/
    └── V3__leave_resolution_constraints.sql               # NUEVO

backend/src/test/java/com/iw/rrhh/
├── leave/LeaveResolutionServiceTest.java                  # NUEVO (unitario)
├── leave/LeaveResolutionIntegrationTest.java              # NUEVO (Testcontainers + MockMvc)
└── notification/NotificationIntegrationTest.java          # NUEVO

frontend/src/app/
├── core/
│   ├── components/notification-bell/notification-bell.component.{ts,html,scss,spec.ts}  # NUEVO
│   ├── models/notification.models.ts                      # NUEVO
│   └── services/notification.service.ts                   # NUEVO
├── features/team-calendar/                                # Módulo HU-05/HU-06 (specs/plan.md §4)
│   ├── components/
│   │   ├── team-pending-requests/team-pending-requests.component.{ts,html,scss,spec.ts}  # NUEVO
│   │   └── resolve-request-dialog/resolve-request-dialog.component.ts                    # NUEVO
│   ├── models/team-leave.models.ts                        # NUEVO
│   └── services/team-leave.service.ts                     # NUEVO
├── app.routes.ts                                          # MODIFICADO (/team/requests)
├── app.html                                               # MODIFICADO (nav + campana)
└── app.ts                                                 # MODIFICADO (import campana)
```

**Structure Decision**: Aplicación web con `backend/` y `frontend/` existentes. La resolución vive en el módulo `leave` (es parte del ciclo de vida de la solicitud); las notificaciones en el paquete `notification` previsto por el Art. 7.2 y `specs/plan.md`. En frontend, la bandeja va en `features/team-calendar/` (asignado a HU-05/HU-06 en `specs/plan.md` §4) y la campana de notificaciones en `core/` por ser transversal a todas las páginas.

---

## Complexity Tracking

No aplica: el diseño no introduce violaciones de la constitución.
