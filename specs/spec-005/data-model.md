# Data Model: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` (HU-05)
**Date**: 2026-11-03 · **Revisado**: 2026-11-16
**Author**: Integrante 3

---

## 1. Entidades

### 1.1 `LeaveRequest` (tabla `leave_requests`, existente)

Sin columnas nuevas. Campos implicados en la resolución:

| Campo | Tipo BD | Tipo Java | Regla en HU-05 |
|---|---|---|---|
| `status` | `VARCHAR(50)` | `LeaveStatus` | `PENDING` → `APPROVED` \| `REJECTED`. Inmutable una vez resuelta. |
| `rejection_reason` | `TEXT` | `String` | Rechazo: obligatorio, 5–500 caracteres (sin espacios extremos). Aprobación: comentario opcional, ≤ 500. Texto plano. |
| `resolved_by` | `UUID` FK `employees` | `Employee` | Responsable que resolvió (sobrescribe el valor provisional que pone HU-03 al crear). |
| `resolved_at` | `TIMESTAMPTZ` | `Instant` | Momento de la resolución. |
| `version` | `BIGINT` | `Long` | Bloqueo optimista (existente). |

**Métodos de dominio nuevos**:

- `approve(Employee resolver, String comment, Instant now)`
- `reject(Employee resolver, String reason, Instant now)`

Ambos lanzan `LeaveRequestAlreadyResolvedException` si `status != PENDING`.

**Diagrama de estados**:

```text
            approve()             
PENDING ───────────────▶ APPROVED  (final)
   │
   │        reject(reason≥5)
   └───────────────────▶ REJECTED  (final)

APPROVED/REJECTED ──approve()/reject()──▶ 409 Conflict (servicio) / excepción (trigger BD)
```

### 1.2 `Notification` (tabla `notifications`, existente; entidad JPA nueva)

Paquete `com.iw.rrhh.notification.domain`.

| Campo | Tipo BD | Tipo Java | Restricciones |
|---|---|---|---|
| `id` | `UUID` | `UUID` | PK generada. |
| `recipient_id` | `UUID` FK `employees` | `Employee` (`LAZY`) | No nulo. |
| `title` | `VARCHAR(200)` | `String` | No nulo. |
| `message` | `TEXT` | `String` | No nulo. |
| `is_read` | `BOOLEAN` | `boolean` | Por defecto `false`. Método `markAsRead()`. |
| `created_at` | `TIMESTAMPTZ` | `Instant` | `@CreationTimestamp`, no actualizable. |

### 1.3 `Employee` (existente)

Sin cambios de esquema. Se usa `manager` para determinar el equipo. Repositorio: nuevo método `findByIdForUpdate(UUID)` con `@Lock(PESSIMISTIC_WRITE)`.

---

## 2. Migraciones

### 2.1 `V3__leave_resolution_constraints.sql` (ya en `main`, no se modifica)

| Objeto | Definición |
|---|---|
| `chk_leave_rejection_reason` | `status <> 'REJECTED' OR (rejection_reason IS NOT NULL AND char_length(btrim(rejection_reason)) >= 5)` |
| `fn_leave_requests_immutable_resolution()` + `trg_leave_requests_immutable_resolution` | `BEFORE UPDATE`: si `OLD.status <> 'PENDING'` y cambia `status` o `rejection_reason` (`IS DISTINCT FROM`) → `RAISE EXCEPTION`. |

No se restringen `resolved_by`/`resolved_at` en BD (compatibilidad con *fixtures* de HU-03/HU-04 y con `ON DELETE SET NULL`; ver `research.md` §1.6).

**Reversión**: `DROP TRIGGER`, `DROP FUNCTION`, `ALTER TABLE leave_requests DROP CONSTRAINT …` (documentado como comentario en la migración).

### 2.2 `V6__leave_resolution_reason_max_length.sql` (nueva)

| Objeto | Definición |
|---|---|
| `chk_leave_rejection_reason_max` | `rejection_reason IS NULL OR char_length(rejection_reason) <= 500` |

**Reversión**: `ALTER TABLE leave_requests DROP CONSTRAINT chk_leave_rejection_reason_max`. Se numera `V6` porque `V4` la usa HU-08 y `V5` (HU-04: estado `CANCELLED`) ya está en `main`.

---

### 2.3 `V7__direccion_general_role.sql` (nueva, 2026-11-18)

```sql
ALTER TABLE users DROP CONSTRAINT chk_user_role;
ALTER TABLE users ADD CONSTRAINT chk_user_role
    CHECK (role IN ('EMPLEADO', 'RESPONSABLE_EQUIPO', 'RRHH', 'DIRECCION_GENERAL'));
```

Sin cambios de columnas. La cancelación automática reutiliza `leave_requests.status = 'CANCELLED'` (V5) y la tabla `notifications`.

## 3. DTOs (records)

### 3.1 Entrada

```java
// com.iw.rrhh.leave.dto
public record ApproveLeaveRequest(
    @Size(max = 500, message = "El comentario no puede exceder 500 caracteres") String comment
) {}

public record RejectLeaveRequest(
    @NotBlank(message = "El motivo del rechazo es obligatorio")
    @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
    String reason
) {}
```

El motivo se normaliza con `strip()` en el servicio antes de comprobar la longitud mínima.

### 3.2 Salida

```java
// com.iw.rrhh.leave.dto
public record TeamLeaveRequestResponse(
    UUID id,
    UUID employeeId,
    String employeeName,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    Integer calculatedDays,
    Instant createdAt
) {}

public record LeaveResolutionResponse(
    UUID id,
    LeaveStatus status,
    String reason,          // motivo (REJECTED) o comentario (APPROVED), puede ser null
    UUID resolvedById,
    Instant resolvedAt
) {}

// com.iw.rrhh.notification.dto
public record NotificationResponse(
    UUID id,
    String title,
    String message,
    boolean read,
    Instant createdAt
) {}

public record UnreadCountResponse(long count) {}
```

Listados paginados con el `PageResponse<T>` común existente.

---

## 4. Consultas de repositorio

| Repositorio | Método | Uso |
|---|---|---|
| `LeaveRequestRepository` | `@EntityGraph(attributePaths = "employee") Page<LeaveRequest> findByEmployeeManagerIdAndEmployeeIdNotAndStatusOrderByCreatedAtAsc(UUID managerId, UUID excludedEmployeeId, LeaveStatus status, Pageable)` (se invoca con `excludedEmployeeId = managerId`) | Bandeja del responsable (CA-1); excluye las solicitudes propias. Índice `idx_leave_employee_status`. |
| `LeaveRequestRepository` | `@Lock(PESSIMISTIC_WRITE) @Query("SELECT lr FROM LeaveRequest lr WHERE lr.id = :id") Optional<LeaveRequest> findByIdForUpdate(UUID id)` | Resolución serializada por solicitud. |
| `EmployeeRepository` | `@Lock(PESSIMISTIC_WRITE) @Query("SELECT e FROM Employee e WHERE e.id = :id") Optional<Employee> findByIdForUpdate(UUID id)` | Serializa aprobaciones de vacaciones del mismo empleado. |
| `LeaveRequestRepository` | `List<LeaveRequest> findOverlappingByYear(UUID employeeId, LeaveType type, LeaveStatus status, LocalDate startOfYear, LocalDate endOfYear)` (`startDate <= endOfYear AND endDate >= startOfYear`) | Saldo de HU-02 con reparto por año (C5). Sustituye a `sumCalculatedDays` en `LeaveBalanceServiceImpl`. |
| `NotificationRepository` | `Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID, Pageable)` | Listado del empleado. |
| `NotificationRepository` | `long countByRecipientIdAndReadFalse(UUID)` | Contador de no leídas. |

---

## 5. Reglas de negocio (resumen)

| Regla | Origen | Error |
|---|---|---|
| Solo roles `RESPONSABLE_EQUIPO`, `RRHH` y `DIRECCION_GENERAL` acceden a bandeja y resolución, y solo para sus subordinados directos | Art. 10, FR-017, FR-018 | `403` |
| Crear solicitud exige resolutor válido (superior activo, distinto, rol `RESPONSABLE_EQUIPO`/`RRHH`/`DIRECCION_GENERAL`) | FR-015 (2026-11-18, ampliado 2026-11-19) | `422` |
| `DIRECCION_GENERAL` no crea solicitudes | FR-017 | `422` |
| `DIRECCION_GENERAL` no consulta saldo propio (`/balances/me` ni `/balances/employee/{propio id}`) | FR-017 (2026-11-19) | `403` + acceso denegado registrado |
| Superior que deja de ser válido: `PENDING` → `CANCELLED` + notificación, misma transacción de HU-01 | FR-016 | — |
| La solicitud debe ser de un miembro directo del responsable en sesión | FR-001, FR-006 | `403` |
| Nadie resuelve sus propias solicitudes (`request.employee.id == resolver.id`) | FR-006 (2026-11-16) | `403` + `WARN` de auditoría |
| La solicitud debe existir (retirada por el empleado = estado `CANCELLED` en HU-04; se trata como inexistente) | Edge case retirada, FR-004b | `404` |
| Solo `PENDING` es resoluble (también ante resolución simultánea) | FR-004, FR-004b | `409` |
| Motivo de rechazo 5–500 caracteres tras `strip()`; comentario ≤ 500 | FR-003 | `400` (`invalidFields.reason` / `comment`) |
| Aprobar con fechas en un año natural cerrado (< año actual, Europe/Madrid) | Assumptions (2026-11-16) | `422` (la solicitud sigue `PENDING`; se puede rechazar) |
| `VACATION` aprobada: por cada año natural del periodo, días del año ≤ `availableDays` de ese año | FR-007 (2026-11-16) | `422` (`requestedDays`, `availableDays`, `year`) |
| Una notificación por resolución, misma transacción | FR-009, FR-010 | — |
| Notificaciones: solo las propias | FR-011 | `403` / `404` |

**Componente de dominio nuevo** (`com.iw.rrhh.leave.service.LeaveDayAllocator`, sin estado, sobre `HolidayCalendarService.isWorkingDay`):

| Método | Contrato |
|---|---|
| `Map<Integer,Integer> allocate(LocalDate start, LocalDate end, int calculatedDays)` | Reparto por año natural; la suma es siempre `calculatedDays`; años previos al último: `min(laborables de hoy, restante)`; último: restante. Un solo año: `{año: calculatedDays}`. |
| `int daysInYear(LeaveRequest r, int year)` | Días de la solicitud que cuentan en `year` (0 si no lo toca). Lo usan el saldo (HU-02) y la validación de aprobación. |

**Orden de evaluación al aprobar**: `404` (no existe) → `403` (alcance/auto-resolución) → `404` (retirada: `CANCELLED`) → `409` → `422` año cerrado → `422` saldo por año.

**Nuevo tipo**: `LeaveApprovalNotAllowedException` (`com.iw.rrhh.common.exception`) con `detail`, `year`, y opcionalmente `requestedDays`/`availableDays`; mapeada a `422` en `GlobalExceptionHandler`.
