# Research: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` (HU-05)
**Date**: 2026-11-03 · **Revisado**: 2026-11-16
**Author**: Integrante 3

---

## 1. Decisiones Técnicas

### 1.1 Definición de "equipo" y autorización de alcance (Art. 10)

- **Decisión**: El equipo de un responsable son los empleados cuyo `employees.manager_id` es el `id` del empleado en sesión (obtenido de `SecurityContextHolder` → `UserRepository.findByUsername` → `EmployeeRepository.findByUserId`, igual que en HU-03/HU-04). Nivel de rol: `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`. Nivel de alcance: el servicio compara `leaveRequest.employee.manager.id` con el responsable en sesión y lanza `AccessDeniedException` (→ `403`).
- **Justificación**: `manager_id` ya existe (HU-01) y es la relación usada por `LeaveBalanceServiceImpl.getEmployeeLeaveBalance`. Comprobar el responsable **actual** del empleado cubre el caso "cambio de responsable" de la spec.
- **Alternativas consideradas**:
  - Usar `leave_requests.resolved_by` (que HU-03 rellena con el responsable al crear): descartado, queda obsoleto si cambia el responsable.
- **Auto-resolución (aclaración 2026-11-16)**: el servicio lanza `AccessDeniedException` (`403`, con `WARN` de auditoría) si `request.employee.id == resolver.id`, aunque `manager_id` fuese el propio empleado. La bandeja excluye `employee.id = :managerId`. La solicitud de un responsable la ve y resuelve su superior (`manager_id`), que debe tener rol `RESPONSABLE_EQUIPO`. Un responsable sin superior queda sin resolutor (límite conocido de la spec).
- **Nota para HU-06 y siguientes**: en solicitudes `PENDING`, `resolved_by` contiene el responsable asignado al crear (comportamiento de HU-03, no se modifica). Solo en `APPROVED`/`REJECTED` indica quién resolvió; HU-05 lo sobrescribe al resolver.
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

- **Decisión (revisada 2026-11-16)**: Reutilizar `LeaveBalanceService.getEmployeeLeaveBalance(employeeId, year)` **por cada año natural** entre `startDate` y `endDate`. Los días de cada año se obtienen contando los días laborables del tramo con `HolidayCalendarService.isWorkingDay` (único componente de cómputo, Art. 14.3); si `daysDelAño > availableDays` de ese año se bloquea la aprobación. Solo para `VACATION` (spec-003: los permisos no consumen saldo). **Regla de reparto (aclaración C4)** en `LeaveDayAllocator.allocate(start, end, calculatedDays)`: el total es siempre `calculatedDays`. Para cada año natural del periodo, salvo el último, el reparto es `min(díasLaborablesDelTramoHoy, restante)`; el último año recibe el `restante`, de modo que la suma coincide siempre aunque cambien los festivos. Una solicitud contenida en un solo año devuelve `calculatedDays` sin recalcular (comportamiento actual de HU-02 intacto). Un único componente (Art. 14.3) lo usan la resolución y el saldo.
- **Justificación**: Una única regla de saldo (Art. 12 y 14.3); ese método ya aplica la regla de alcance para responsables.
- **Código HTTP (revisado 2026-11-16)**: La aclaración fija `422` para saldo insuficiente al aprobar (Art. 9.3: regla de negocio violada). `InsufficientLeaveBalanceException` sigue devolviendo `409` en HU-03 (crear solicitud) para no romper su contrato. En HU-05 el servicio traduce el fallo a una nueva `LeaveApprovalNotAllowedException` (`422`, `requestedDays`, `availableDays`, `year`), mapeada en `GlobalExceptionHandler`. Se descarta cambiar el mapeo global: afectaría a HU-03.
- **Alternativas consideradas**: Duplicar la suma en el servicio de resolución: descartado (dos implementaciones que pueden divergir).

### 1.5 Transiciones en la entidad

- **Decisión**: Añadir a `LeaveRequest` los métodos `approve(Employee resolver, String comment, Instant now)` y `reject(Employee resolver, String reason, Instant now)`, que lanzan `LeaveRequestAlreadyResolvedException` si `status != PENDING`. El comentario de aprobación y el motivo de rechazo se guardan en `rejection_reason` (columna existente; ver Assumptions de la spec).
- **Justificación**: La regla de transición vive en un solo sitio y es testeable sin Spring. No requiere columna nueva.
- **Alternativas consideradas**: Nueva columna `resolution_comment`: descartado por simplicidad; la spec asume un único campo.
- **Nota**: `LeaveRequestSummaryResponse` (HU-04) solo expone `rejectionReason` cuando `status = REJECTED`, por lo que el comentario de aprobación no aparece como "motivo de rechazo" en HU-04; el empleado lo recibe en la notificación.

### 1.6 Refuerzo en base de datos (Art. 8.2 y 13.2)

- **Decisión**: Migración `V3__leave_resolution_constraints.sql` (ya en `main`; **no se edita**, Flyway rechazaría un checksum distinto) con:
  - `CHECK chk_leave_rejection_reason`: si `status = 'REJECTED'` entonces `char_length(btrim(rejection_reason)) >= 5`.
  - *Trigger* `trg_leave_requests_immutable_resolution` (`BEFORE UPDATE`) que lanza excepción si `OLD.status <> 'PENDING'` y cambia `status` o `rejection_reason`.
- **Justificación**: El Art. 13.2 exige que la imposibilidad de re-resolver se refuerce en BD; un `CHECK` no puede comparar valores anterior/nuevo, por eso se usa un *trigger* mínimo.
- **Alcance deliberadamente limitado** (resultado de `/speckit-analyze`):
  - No se añade `CHECK` que obligue a `resolved_at`/`resolved_by` no nulos en solicitudes resueltas: los tests de integración existentes de HU-03/HU-04 (`LeaveRequestIntegrationTest`, `LeaveBalanceIntegrationTest`, `LeaveRequestTrackingIntegrationTest`) crean solicitudes `APPROVED`/`REJECTED` sin esos campos como *fixtures*. El registro de quién y cuándo (Art. 13.6) lo garantizan los métodos de dominio y los tests de HU-05.
  - El *trigger* no protege `resolved_by`/`resolved_at`: la FK `resolved_by ... ON DELETE SET NULL` (V1) ejecuta un `UPDATE` sobre filas resueltas al borrar un empleado, y el *trigger* lo bloquearía.
- **Límite de 500 caracteres (2026-11-16)**: nueva migración `V5__leave_resolution_reason_max_length.sql` (V4 ya la usa HU-08) con `CHECK chk_leave_rejection_reason_max (rejection_reason IS NULL OR char_length(rejection_reason) <= 500)`. Reversión: `ALTER TABLE leave_requests DROP CONSTRAINT chk_leave_rejection_reason_max`. Si otra rama ocupa V5, renumerar al integrar.

### 1.7 Notificaciones (Art. 15)

- **Decisión**: Nuevo paquete `notification` con entidad `Notification` sobre la tabla existente `notifications`. `NotificationService.createNotification(Employee recipient, String title, String message)` se invoca desde la resolución. Endpoints del empleado en sesión: `GET /api/v1/notifications?page&size` (más recientes primero), `GET /api/v1/notifications/unread-count`, `POST /api/v1/notifications/{id}/read` (`204`; `403` si no es suya; idempotente).
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

### 1.11 Resolución de solicitudes de responsables (2026-11-16)

- **Decisión**: Sin rol ni endpoint nuevos. La bandeja del superior ya incluye a sus subordinados directos, responsables incluidos. Solo se añade la guarda de auto-resolución (§1.1).
- **Alternativas consideradas**: Bandeja de RRHH para responsables sin superior: descartada, la aclaración lo deja fuera de HU-05.

### 1.12 Año cerrado (2026-11-16)

- **Decisión**: Al aprobar, si `startDate.getYear() < Year.now(clock, Europe/Madrid)` (o cualquier año del tramo), se lanza `LeaveApprovalNotAllowedException` (`422`) antes de consultar el saldo. El rechazo no tiene esta restricción. `Clock` ya está inyectado (Art. 22.5).
- **Orden de comprobaciones al aprobar**: existe (`404`) → alcance y auto-resolución (`403`) → estado `PENDING` (`409`) → año cerrado (`422`) → saldo por año (`422`).

### 1.14 Saldo de HU-02 con solicitudes que cruzan años (aclaración C5, 2026-11-16)

- **Hallazgo**: `LeaveRequestRepository.sumCalculatedDays` filtra `startDate >= :startOfYear AND endDate <= :endOfYear`; una solicitud 28/12–08/01 aprobada no cuenta como disfrutada en ningún año (Art. 12.1 incumplido). Además `calculateBalance` da el saldo de un año futuro con el convenio vigente (o 0 si el empleado se incorpora después).
- **Decisión**: Nueva consulta `findOverlappingByYear(employeeId, leaveType, status, startOfYear, endOfYear)` (`startDate <= :endOfYear AND endDate >= :startOfYear`). `calculateBalance` suma `LeaveDayAllocator.daysInYear(request, year)` para `APPROVED` (disfrutados) y `PENDING` (pendientes). Para solicitudes contenidas en un año el resultado es idéntico al actual. El año siguiente no necesita lógica propia: usa su saldo normal.
- **Validación al aprobar**: para cada año `y` del periodo, `daysInYear(request, y) <= balance(y).availableDays` (la solicitud aún es `PENDING`, no está en `spentDays`). Si no, `LeaveApprovalNotAllowedException` (`422`, `year`).
- **Alternativas consideradas**: dejar el cálculo y contar todo en el año de inicio (descartado: decisión C5-A); columna materializada por año (descartada: Art. 12.1 y Art. 23).
- **Riesgo**: `calculateBalance` usa `LocalDate.now()` en vez del `Clock` inyectado; fuera del alcance de HU-05, pero los tests deben fijar el año con *fixtures* relativas al año actual. `LeaveBalanceServiceTest` mockea `sumCalculatedDays` y debe adaptarse.

### 1.15 Resolutor de un responsable sin superior válido (aclaración C7, 2026-11-16)

- **Decisión**: Sin cambios de código en HU-05: resuelve el `manager` directo con rol `RESPONSABLE_EQUIPO` (el `@PreAuthorize` ya lo exige). Sin superior, o con superior sin ese rol, la solicitud queda `PENDING` sin resolutor (límite conocido). HU-01 acepta hoy cualquier empleado como `managerId` sin validar su rol: mejora pendiente fuera de HU-05.
- **Alternativas consideradas**: bandeja de RRHH (descartada en la aclaración); rechazar en HU-03 si no hay resolutor válido (descartado).

### 1.13 Frontend (2026-11-16)

- El diálogo de resolución añade `Validators.maxLength(500)` y contador de caracteres en ambos modos; el rechazo sigue con `required` + `minLength(5)` sobre el texto recortado.
- El servicio Angular, ante `409` o `404`, recarga la bandeja y muestra el mensaje en `MatSnackBar`; ante `422`, muestra el `detail` del ProblemDetail (saldo o año cerrado) y mantiene la fila.

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
