# Data Model: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` (HU-05)
**Date**: 2026-11-03
**Author**: Integrante 3

---

## 1. Entidades

### 1.1 `LeaveRequest` (tabla `leave_requests`, existente)

Sin columnas nuevas. Campos implicados en la resolución:

| Campo | Tipo BD | Tipo Java | Regla en HU-05 |
|---|---|---|---|
| `status` | `VARCHAR(50)` | `LeaveStatus` | `PENDING` → `APPROVED` \| `REJECTED`. Inmutable una vez resuelta. |
| `rejection_reason` | `TEXT` | `String` | Rechazo: obligatorio, ≥ 5 caracteres (sin espacios extremos). Aprobación: comentario opcional. |
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

## 2. Migración `V3__leave_resolution_constraints.sql`

| Objeto | Definición |
|---|---|
| `chk_leave_resolved_fields` | `status = 'PENDING' OR (resolved_at IS NOT NULL AND resolved_by IS NOT NULL)` |
| `chk_leave_rejection_reason` | `status <> 'REJECTED' OR (rejection_reason IS NOT NULL AND char_length(btrim(rejection_reason)) >= 5)` |
| `fn_leave_requests_immutable_resolution()` + `trg_leave_requests_immutable_resolution` | `BEFORE UPDATE`: si `OLD.status <> 'PENDING'` y cambia `status`, `rejection_reason`, `resolved_by` o `resolved_at` → `RAISE EXCEPTION`. |

**Reversión**: `DROP TRIGGER`, `DROP FUNCTION`, `ALTER TABLE leave_requests DROP CONSTRAINT …` (documentado como comentario en la migración).

---

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
| `LeaveRequestRepository` | `@EntityGraph(attributePaths = "employee") Page<LeaveRequest> findByEmployeeManagerIdAndStatusOrderByCreatedAtAsc(UUID managerId, LeaveStatus status, Pageable)` | Bandeja del responsable (CA-1). Índice `idx_leave_employee_status`. |
| `LeaveRequestRepository` | `@Lock(PESSIMISTIC_WRITE) @Query("SELECT lr FROM LeaveRequest lr WHERE lr.id = :id") Optional<LeaveRequest> findByIdForUpdate(UUID id)` | Resolución serializada por solicitud. |
| `EmployeeRepository` | `@Lock(PESSIMISTIC_WRITE) @Query("SELECT e FROM Employee e WHERE e.id = :id") Optional<Employee> findByIdForUpdate(UUID id)` | Serializa aprobaciones de vacaciones del mismo empleado. |
| `NotificationRepository` | `Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID, Pageable)` | Listado del empleado. |
| `NotificationRepository` | `long countByRecipientIdAndReadFalse(UUID)` | Contador de no leídas. |

---

## 5. Reglas de negocio (resumen)

| Regla | Origen | Error |
|---|---|---|
| Solo rol `RESPONSABLE_EQUIPO` accede a bandeja y resolución | Art. 10 | `403` |
| La solicitud debe ser de un miembro directo del responsable en sesión | FR-001, FR-006 | `403` |
| La solicitud debe existir | Edge case cancelación | `404` |
| Solo `PENDING` es resoluble | FR-004 | `409` |
| Motivo de rechazo ≥ 5 caracteres | FR-003 | `400` (`invalidFields.reason`) |
| `VACATION` aprobada no puede superar `availableDays` | FR-007 | `409` (`requestedDays`, `availableDays`) |
| Una notificación por resolución, misma transacción | FR-009, FR-010 | — |
| Notificaciones: solo las propias | FR-011 | `403` / `404` |
