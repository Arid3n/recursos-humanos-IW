# Data Model: spec-003 (Solicitud de Vacaciones o Permisos)

**Feature**: `spec-003` · HU-03  
**Date**: 2026-11-01  
**Branch**: `spec-003`  

---

## 1. Entities & Relational Schema (PostgreSQL)

La persistencia se apoya en el esquema relacional ya migrado en Flyway `V1__init_shared_schema.sql` y en la entidad JPA `LeaveRequest` del módulo `com.iw.rrhh.leave`.

### Entity: `LeaveRequest` (`leave_requests`)
Representa el registro formal de la solicitud de ausencia creada por el empleado.

| Campo / Columna | Tipo Java | Tipo BD | Restricciones / Reglas | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` | Identificador único de la solicitud |
| `employee` / `employee_id` | `Employee` | `UUID` | `NOT NULL, REFERENCES employees(id) ON DELETE CASCADE` | Empleado solicitante (obtenido de sesión) |
| `leaveType` / `leave_type` | `LeaveType` | `VARCHAR(50)` | `NOT NULL, CHECK (leave_type IN ('VACATION', 'PERMIT'))` | Tipo de ausencia solicitada |
| `startDate` / `start_date` | `LocalDate` | `DATE` | `NOT NULL` | Fecha de inicio del periodo |
| `endDate` / `end_date` | `LocalDate` | `DATE` | `NOT NULL, CHECK (end_date >= start_date)` | Fecha de finalización del periodo |
| `calculatedDays` / `calculated_days` | `Integer` | `INTEGER` | `NOT NULL, CHECK (calculated_days >= 0)` | Días laborables computados por el servidor |
| `status` / `status` | `LeaveStatus` | `VARCHAR(50)` | `NOT NULL, CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))` | Nace estrictamente en `PENDING` |
| `rejectionReason` / `rejection_reason` | `String` | `TEXT` | `NULL` al crearse | Justificación en caso de rechazo posterior (HU-05) |
| `resolvedBy` / `resolved_by` | `Employee` | `UUID` | `REFERENCES employees(id) ON DELETE SET NULL` | Asignado automáticamente al `manager` del empleado |
| `resolvedAt` / `resolved_at` | `Instant` | `TIMESTAMPTZ` | `NULL` al crearse | Momento en que el responsable resolverá (HU-05) |
| `createdAt` / `created_at` | `Instant` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP, updatable = false` | Instante de creación auditado |
| `version` | `Long` | `BIGINT` | `NOT NULL DEFAULT 0, @Version` | Control de concurrencia optimista (Art. 13) |

#### Enums de Dominio (Preexistentes en `com.iw.rrhh.leave.domain`)
```java
public enum LeaveType {
    VACATION,
    PERMIT
}

public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED
}
```

---

## 2. Invariants & Business Constraints

1. **Estado inicial estricto (Art. 13, FR-003)**:
   - Toda solicitud DEBE crearse en estado `PENDING`. No es posible crear solicitudes en estado `APPROVED` o `REJECTED` desde el endpoint de creación.
2. **Validación de fechas (FR-005)**:
   - `endDate >= startDate`. Si `endDate < startDate`, se rechaza con error de validación RFC 9457 asociado al campo `endDate`.
3. **Cómputo en días hábiles (Art. 14, FR-002)**:
   - Solo se contabilizan de lunes a viernes, restando fines de semana y festivos oficiales parametrizados.
   - Si `calculatedDays == 0`, la solicitud es rechazada con error explicativo ("El rango seleccionado no contiene ningún día laborable").
4. **No solapamiento (FR-007, SC-002)**:
   - No puede existir ninguna solicitud previa del mismo empleado en estado `PENDING` o `APPROVED` cuyo intervalo temporal coincida o se solape (`startDate <= :newEndDate AND endDate >= :newStartDate`).
5. **Tope de saldo disponible (Art. 12, FR-004, SC-003)**:
   - Para `leaveType == VACATION`, `calculatedDays <= leaveBalanceService.getMyLeaveBalance(startDate.getYear()).availableDays()`.
   - Para `leaveType == PERMIT`, no se valida ni se descuenta saldo de vacaciones (FR-008).
6. **Asignación de responsable (FR-006)**:
   - El sistema vincula la solicitud al responsable de equipo configurado en `employee.manager`.

---

## 3. Data Transfer Objects (DTOs) & Records

### `CreateLeaveRequest` (Request para registrar ausencia)
```java
package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateLeaveRequest(
    @NotNull(message = "El tipo de ausencia es obligatorio")
    LeaveType leaveType,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate startDate,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDate endDate
) {}
```

### `LeaveRequestResponse` (Respuesta de solicitud creada)
```java
package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestResponse(
    UUID id,
    UUID employeeId,
    String employeeFullName,
    UUID managerId,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    Integer calculatedDays,
    LeaveStatus status,
    Instant createdAt
) {}
```

### `WorkingDaysCalculationResponse` (Respuesta de cómputo previo interactivo)
```java
package com.iw.rrhh.leave.dto;

import java.time.LocalDate;

public record WorkingDaysCalculationResponse(
    LocalDate startDate,
    LocalDate endDate,
    int workingDays,
    int calendarDays,
    int weekendDays,
    int holidayDays
) {}
```

---

## 4. Repositorio JPA: Métodos Requeridos

En `LeaveRequestRepository`:
```java
@Query("""
    SELECT COUNT(lr) > 0
    FROM LeaveRequest lr
    WHERE lr.employee.id = :employeeId
      AND lr.status IN ('PENDING', 'APPROVED')
      AND lr.startDate <= :endDate
      AND lr.endDate >= :startDate
""")
boolean hasOverlappingRequests(
    @Param("employeeId") UUID employeeId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```
