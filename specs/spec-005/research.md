# Research: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` (HU-05)
**Date**: 2026-11-03
**Author**: Integrante 3

---

## 1. Decisiones Técnicas

### 1.1 Definición de "equipo" y autorización de alcance (Art. 10)

- **Decisión**: El equipo de un responsable son los empleados cuyo `employees.manager_id` es el `id` del empleado en sesión (obtenido de `SecurityContextHolder` → `UserRepository.findByUsername` → `EmployeeRepository.findByUserId`, igual que en HU-03/HU-04). Nivel de rol: `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`. Nivel de alcance: el servicio compara `leaveRequest.employee.manager.id` con el responsable en sesión y lanza `AccessDeniedException` (→ `403`).
- **Justificación**: `manager_id` ya existe (HU-01) y es la relación usada por `LeaveBalanceServiceImpl.getEmployeeLeaveBalance`. Comprobar el responsable **actual** del empleado cubre el caso "cambio de responsable" de la spec.
- **Alternativas consideradas**:
  - Usar `leave_requests.resolved_by` (que HU-03 rellena con el responsable al crear): descartado, queda obsoleto si cambia el responsable.
  - Equipo por `contract_records.department`: descartado, la spec define equipo como personas a cargo directo.

### 1.2 Endpoints de resolución

- **Decisión**: Dos endpoints explícitos: `POST /api/v1/leave/requests/{id}/approve` (cuerpo opcional `{ comment }`) y `POST /api/v1/leave/requests/{id}/reject` (cuerpo `{ reason }` obligatorio). Bandeja: `GET /api/v1/leave/team-requests?page&size`. Respuesta de resolución: `200` con `LeaveResolutionResponse`.
- **Justificación**: Cada acción tiene su propia validación declarativa (`@NotBlank @Size(min=5)` solo en el rechazo) sin lógica condicional en el controlador; sigue el patrón de `.../cancel` de HU-04.
- **Alternativas consideradas**: `PATCH /requests/{id}` con `{ decision, reason }`: descartado por exigir validación condicional del motivo.

### 1.3 Atomicidad y concurrencia (Art. 12.6 y 13.7)

- **Decisión**: Un único método `@Transactional` en `LeaveResolutionServiceImpl` que:
  1. Carga la solicitud con `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) → serializa resoluciones simultáneas de la **misma** solicitud; la segunda ve el estado ya resuelto y recibe `409`.
  2. Si es `VACATION` y se aprueba, bloquea la fila del empleado solicitante con `PESSIMISTIC_WRITE` antes de recalcular el saldo → serializa aprobaciones simultáneas de **distintas** solicitudes del mismo empleado; la segunda recalcula con la primera ya confirmada y recibe saldo insuficiente.
  3. Aplica la transición en la entidad, guarda y crea la notificación vía `NotificationService` (misma transacción, propagación `REQUIRED`).
- **Justificación**: PostgreSQL en `READ COMMITTED` devuelve la versión confirmada tras liberar el bloqueo, por lo que la revalidación es correcta. Es la solución más simple que cumple el Art. 12.6 sin contadores materializados. El campo `version` existente se mantiene como protección adicional.
- **Alternativas consideradas**:
  - Solo bloqueo optimista con `@Version`: no protege dos solicitudes **distintas** que consumen el mismo saldo.
  - `SERIALIZABLE`: reintentos y mayor complejidad sin beneficio en este caso.

### 1.4 Revalidación del saldo

- **Decisión**: Reutilizar `LeaveBalanceService.getEmployeeLeaveBalance(employeeId, year)` con `year = startDate.getYear()` y comparar `calculatedDays > availableDays` → `InsufficientLeaveBalanceException` (ya mapeada a `409` con `requestedDays`/`availableDays`). Solo para `VACATION` (spec-003: los permisos no consumen saldo).
- **Justificación**: Una única regla de saldo (Art. 12 y 14.3); ese método ya aplica la regla de alcance para responsables.
- **Alternativas consideradas**: Duplicar la suma en el servicio de resolución: descartado (dos implementaciones que pueden divergir).

### 1.5 Transiciones en la entidad

- **Decisión**: Añadir a `LeaveRequest` los métodos `approve(Employee resolver, String comment, Instant now)` y `reject(Employee resolver, String reason, Instant now)`, que lanzan `LeaveRequestAlreadyResolvedException` si `status != PENDING`. El comentario de aprobación y el motivo de rechazo se guardan en `rejection_reason` (columna existente; ver Assumptions de la spec).
- **Justificación**: La regla de transición vive en un solo sitio y es testeable sin Spring. No requiere columna nueva.
- **Alternativas consideradas**: Nueva columna `resolution_comment`: descartado por simplicidad; la spec asume un único campo.
- **Nota**: `LeaveRequestSummaryResponse` (HU-04) solo expone `rejectionReason` cuando `status = REJECTED`, por lo que el comentario de aprobación no aparece como "motivo de rechazo" en HU-04; el empleado lo recibe en la notificación.

### 1.6 Refuerzo en base de datos (Art. 8.2 y 13.2)

- **Decisión**: Migración `V3__leave_resolution_constraints.sql` con:
  - `CHECK chk_leave_resolved_fields`: si `status <> 'PENDING'` entonces `resolved_at` y `resolved_by` no nulos.
  - `CHECK chk_leave_rejection_reason`: si `status = 'REJECTED'` entonces `char_length(trim(rejection_reason)) >= 5`.
  - *Trigger* `trg_leave_requests_immutable_resolution` (`BEFORE UPDATE`) que lanza excepción si `OLD.status <> 'PENDING'` y cambia `status`, `rejection_reason`, `resolved_by` o `resolved_at`.
- **Justificación**: El Art. 13.2 exige que la imposibilidad de re-resolver se refuerce en BD; un `CHECK` no puede comparar valores anterior/nuevo, por eso se usa un *trigger* mínimo.
- **Coordinación**: El número de versión `V3` puede colisionar con migraciones de otras ramas (`spec-007`, `spec-008`); se renumerará al integrar si fuera necesario.

### 1.7 Notificaciones (Art. 15)

- **Decisión**: Nuevo paquete `notification` con entidad `Notification` sobre la tabla existente `notifications`. `NotificationService.notify(Employee recipient, String title, String message)` se invoca desde la resolución. Endpoints del empleado en sesión: `GET /api/v1/notifications?page&size` (más recientes primero), `GET /api/v1/notifications/unread-count`, `POST /api/v1/notifications/{id}/read` (`204`; `403` si no es suya; idempotente).
- **Mensaje**: título `"Solicitud aprobada"` / `"Solicitud rechazada"`; mensaje con tipo, periodo (`dd/MM/yyyy`) y `Motivo: …` o `Comentario: …` si existe.
- **Justificación**: Tabla e índice `idx_notifications_recipient (recipient_id, is_read, created_at DESC)` ya existen en V1.
- **Alternativas consideradas**: Eventos de dominio (`ApplicationEventPublisher`) + listener: indirección innecesaria (Art. 23); la llamada directa ya es transaccional.

### 1.8 Auditoría (Art. 20.5)

- **Decisión**: Log `INFO` en la resolución con `requestId`, decisión y `resolverId` (UUID), y `WARN` en accesos fuera de alcance con `requestId` y `resolverId`. Sin nombres, emails ni motivos.
- **Alternativas consideradas**: Tabla de auditoría: fuera del alcance de HU-05; no existe infraestructura común.

### 1.9 Datos de prueba: usuario responsable

- **Decisión**: Ampliar `SecurityDataInitializer` con `responsable@empresa.com` (rol `RESPONSABLE_EQUIPO`, contraseña de desarrollo `admin123`) y asignarlo como `manager` de `empleado@empresa.com` si este no tiene responsable.
- **Justificación**: El alta de HU-01 siempre crea usuarios `EMPLEADO`; sin este usuario no hay forma de validar HU-05 de extremo a extremo (`quickstart.md`).

### 1.10 Frontend

- **Bandeja** (`features/team-calendar/components/team-pending-requests`): `mat-table` + `mat-paginator` (10 por página, más antiguas primero) con columnas Empleado, Tipo, Fechas, Días, Solicitada y Acciones ("Aprobar"/"Rechazar" con icono y texto). Ruta `/team/requests`, enlace "Solicitudes del Equipo" visible solo con `isResponsable()` (ayuda de usabilidad; la autorización real está en servidor).
- **Diálogo** (`resolve-request-dialog`): formulario reactivo con un `textarea`; en rechazo `Validators.required` + `Validators.minLength(5)` y `mat-error` junto al campo; en aprobación campo opcional. Los errores `409`/`403` del servidor se muestran con `MatSnackBar` y recargan la bandeja.
- **Campana** (`core/components/notification-bell`): `mat-icon-button` + `matBadge` con `aria-label="N notificaciones sin leer"`, `mat-menu` con las 10 más recientes; al pulsar una no leída se marca como leída. El contador se refresca al iniciar sesión y en cada `NavigationEnd`.
- **Alternativas consideradas**: *Polling* periódico o WebSockets para el contador: innecesario para el alcance (Art. 23).

---

## 2. Cobertura de la Constitución

| Artículo | Requisito | Enfoque spec-005 |
|---|---|---|
| Art. 2 | Trazabilidad | Tests etiquetados `HU-05.CA-1…5`. |
| Art. 10 | Rol + alcance, tests negativos | `hasRole('RESPONSABLE_EQUIPO')` + comprobación `manager.id`; tests `403` (otro equipo, rol `EMPLEADO`, notificación ajena). |
| Art. 12 | Saldo derivado y concurrente | Reutiliza `LeaveBalanceService`; bloqueo `FOR UPDATE` de la fila del empleado; test de concurrencia. |
| Art. 13 | Transiciones únicas y atómicas | Métodos de dominio + `409` + *trigger*; una transacción para estado, saldo y notificación. |
| Art. 15 | Notificación persistida | `notifications` en la misma transacción. |
| Art. 19 | ProblemDetail | `400` (motivo), `403`, `404`, `409`. |
| Art. 21 | Accesibilidad | Texto + icono en acciones y contador; `mat-error`. |
