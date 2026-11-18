# Implementation Plan: spec-005 — Aprobación o Rechazo de Solicitudes

**Branch**: `spec-005` | **Date**: 2026-11-03 · **Revisado**: 2026-11-16 (tras `/speckit-clarify`, sesión 2026-11-16, incluidas C4, C5 y C7 de `/speckit-analyze`) | **Spec**: [specs/spec-005/spec.md](spec.md)

**Input**: Feature specification from `/specs/spec-005/spec.md`

---

## Summary

La funcionalidad **HU-05 (`spec-005`)** permite al responsable de equipo consultar las solicitudes en estado `PENDING` de las personas a su cargo (empleados cuyo `manager_id` es el responsable en sesión) y aprobarlas o rechazarlas. El rechazo exige un motivo de 5 a 500 caracteres; la aprobación admite un comentario opcional de hasta 500. Nadie resuelve sus propias solicitudes (las de un responsable las resuelve su superior). Las vacaciones que cruzan años se reparten por año con una regla única (`LeaveDayAllocator`), se validan año a año y cuentan en el saldo derivado (HU-02) de cada año; no se aprueban fechas de años cerrados. La resolución se ejecuta en una única transacción que: bloquea la solicitud y la ficha del empleado (`SELECT ... FOR UPDATE`), revalida el saldo si es `VACATION`, registra `status`, `resolved_by`, `resolved_at` y motivo, y crea la notificación persistida para el empleado. El empleado consulta sus notificaciones desde un distintivo en la cabecera y puede marcarlas como leídas. El saldo sigue siendo derivado (HU-02): una solicitud `APPROVED` pasa a contar como días disfrutados sin contadores adicionales.

---

## Technical Context

**Language/Version**: Java 21 LTS (Backend) · TypeScript 5.x estricto / Angular 21 (Frontend)

**Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA, Validation, Flyway) · Angular Material 21 (table, paginator, dialog, menu, badge, snack-bar)

**Storage**: PostgreSQL 16 — tablas existentes `leave_requests` y `notifications` (V1). Migración `V3__leave_resolution_constraints.sql` (ya integrada en `main`, no se modifica) y nueva `V5__leave_resolution_reason_max_length.sql` con el límite de 500 caracteres (V4 ya está ocupada por HU-08).

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
| **Art. 10** (Autorización 2 niveles) | Rol: `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`. Alcance: el servicio compara `employee.manager.id` con el empleado de la sesión → `403`; además deniega expresamente la auto-resolución (`request.employee.id == resolver.id`) aunque `manager_id` apunte a sí mismo. Tests negativos incluidos. | ✅ |
| **Art. 12 / 14** (Saldo derivado, concurrencia, cómputo único) | Sin contadores; revalidación con `LeaveBalanceService` por cada año natural afectado, tras bloquear la fila del empleado (`PESSIMISTIC_WRITE`). El reparto de días por año lo hace un único componente, `LeaveDayAllocator` (sobre `HolidayCalendarService.isWorkingDay`), usado por la resolución **y** por el saldo de HU-02; no hay dos implementaciones (Art. 14.3). | ✅ |
| **Art. 13** (Ciclo de vida) | Solo `PENDING → APPROVED/REJECTED`; resuelta → `409`; registro de `resolved_by`/`resolved_at`; todo en una transacción. | ✅ |
| **Art. 15** (Notificaciones) | Notificación persistida en la misma transacción; canal único interno. | ✅ |
| **Art. 19** (Errores) | `LeaveRequestAlreadyResolvedException` → `409`; solicitud retirada → `404`; nueva `LeaveApprovalNotAllowedException` → `422` (saldo insuficiente al aprobar, año cerrado); validación de motivo con `jakarta.validation` → `400` con `invalidFields`. Tabla completa en `data-model.md` §5. | ✅ |
| **Art. 20** (Auditoría y datos personales) | Log de auditoría con identificadores (UUID), sin nombres ni motivos; accesos denegados registrados. | ✅ |
| **Art. 21** (Accesibilidad) | Estados y contador con texto + icono; diálogos con foco y `mat-error` junto al campo. | ✅ |
| **Art. 22** (Tests obligatorios) | Autorización negativa, transición inválida, saldo insuficiente y concurrencia sobre el saldo cubiertos. | ✅ |
| **Art. 23** (Simplicidad) | Reutiliza `LeaveBalanceService`, `PageResponse` y el patrón de servicio existente; sin dependencias nuevas. | ✅ |

**Re-evaluación post-diseño**: ✅ Sin violaciones propias de HU-05. Un límite heredado de HU-03 (N1) se registra en *Complexity Tracking*.

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
│   ├── common/config/ClockConfig.java                     # NUEVO (bean Clock, Art. 22.5)
│   ├── common/exception/
│   │   ├── LeaveRequestAlreadyResolvedException.java      # NUEVO (409)
│   │   ├── LeaveApprovalNotAllowedException.java          # NUEVO (422: saldo/año cerrado al aprobar)
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
│   │       ├── LeaveDayAllocator.java                     # NUEVO (reparto de días por año natural, regla C4)
│   │       ├── LeaveBalanceServiceImpl.java               # MODIFICADO (saldo con reparto por año, C5)
│   │       ├── LeaveResolutionService.java                # NUEVO (también: reparto de días por año, auto-resolución, año cerrado)
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
    ├── V3__leave_resolution_constraints.sql               # EXISTENTE en main (no se modifica)
    └── V5__leave_resolution_reason_max_length.sql         # NUEVO (CHECK ≤ 500)

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

## Decisiones de la sesión de aclaraciones 2026-11-16

| # | Aclaración (spec) | Decisión de diseño | Detalle |
|---|---|---|---|
| 1 | Sin auto-resolución; la solicitud de un responsable la resuelve su superior | Comprobación explícita `request.employee.id != resolver.id` y exclusión en la bandeja; sin superior, nadie resuelve | `research.md` §1.1, §1.11 |
| 2 | Saldo por año natural en solicitudes que cruzan años | Reparto de días por año con `HolidayCalendarService`; se valida cada año | `research.md` §1.4 |
| 3 | `409` resuelta/simultánea · `404` retirada · `422` saldo insuficiente | `LeaveApprovalNotAllowedException` → `422` solo en la aprobación de HU-05; HU-03 mantiene `409` | `research.md` §1.4, `data-model.md` §5 |
| 4 | Motivo 5–500, comentario ≤ 500 | DTO ya conforme; nuevo `CHECK` en V5 | `research.md` §1.6 |
| 5 | No se aprueban fechas de años cerrados | Comprobación con `Clock` (año < año actual en Europe/Madrid) → `422` | `research.md` §1.12 |
| 6 (C4) | Total = `calculatedDays`; años previos al último con días laborables de hoy (tope `calculatedDays`); el último año recibe el resto | `LeaveDayAllocator.allocate(start, end, calculatedDays)` | `research.md` §1.4 |
| 7 (C5) | El saldo de HU-02 cuenta la solicitud que cruza años en ambos; año futuro con saldo normal; sin saldo → `422` con el año | Nueva consulta de solapamiento + suma con el reparto en `LeaveBalanceServiceImpl`; `sumCalculatedDays` deja de usarse | `research.md` §1.14 |
| 8 (C7) | Sólo resuelve el superior directo con rol `RESPONSABLE_EQUIPO`; sin superior o sin rol → sin resolutor | Sin código nuevo en HU-05; mejora pendiente en HU-01 | `research.md` §1.15 |

**Impacto en código ya implementado** (la rama contiene la implementación previa de HU-05; estas diferencias deben reflejarse en `/speckit-tasks`):

- `LeaveResolutionServiceImpl.ensureSufficientBalance` valida sólo `startDate.getYear()` y lanza `InsufficientLeaveBalanceException` (`409`): pasa a validar todos los años y a lanzar `LeaveApprovalNotAllowedException` (`422`).
- `loadInScope` no bloquea la auto-resolución con `manager_id` autorreferenciado.
- `LeaveBalanceServiceImpl.calculateBalance` suma con `sumCalculatedDays` (`startDate >= 1 ene` y `endDate <= 31 dic`): una solicitud que cruza años no cuenta en ningún año. Se sustituye por una consulta de solapamiento y el reparto de `LeaveDayAllocator`; `LeaveBalanceServiceTest` (mockea `sumCalculatedDays`) debe adaptarse y los tests de HU-02 deben seguir en verde.
- No hay comprobación de año cerrado.
- Faltan los tests: auto-resolución `403`, año cerrado `422`, cruce de años, `422` por saldo.

---

## Complexity Tracking

El diseño de HU-05 no introduce violaciones de la constitución. Se registra un límite heredado de HU-03 que HU-05 no corrige (Art. 26):

| Límite | Qué se incumple | Por qué es necesario | Alternativa descartada |
|---|---|---|---|
| **N1** · HU-03 valida el saldo sólo en el año de inicio (`LeaveRequestServiceImpl`, y `sumCalculatedDays` para las pendientes) | Art. 12.5 de forma parcial: una solicitud que cruza años puede crearse sin saldo suficiente en el año siguiente. HU-05 lo contiene: la aprobación revalida cada año y bloquea con `422` (Art. 12.6, SC-006). | HU-03 está entregada y es otra historia: cambiar su comportamiento exige actualizar antes su spec y tests (Art. 1.3) y arriesga regresiones fuera del alcance de HU-05. El fallo es seguro (la solicitud queda Pendiente y puede rechazarse). | Que HU-03 valide por año con `LeaveDayAllocator`: viable, queda como mejora pendiente de HU-03. |
