# Data Model: spec-002 (Consulta de Ficha y Saldo de Vacaciones)

**Feature**: `spec-002` · HU-02  
**Date**: 2026-10-29  
**Branch**: `spec-002`  

---

## 1. Entities & Relational Schema (PostgreSQL)

El modelo de datos aprovecha el esquema ya migrado en Flyway `V1__init_shared_schema.sql` y define las entidades JPA correspondientes en el módulo `com.iw.rrhh.leave`.

### Entity: `LeaveRequest` (`leave_requests`)
Representa una solicitud de ausencia (vacaciones o permiso) solicitada por un empleado.

| Campo / Columna | Tipo Java | Tipo BD | Restricciones / Reglas | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` | Identificador único de la solicitud |
| `employee` / `employee_id` | `Employee` | `UUID` | `NOT NULL, REFERENCES employees(id) ON DELETE CASCADE` | Empleado titular de la solicitud |
| `leaveType` / `leave_type` | `LeaveType` | `VARCHAR(50)` | `NOT NULL, CHECK (leave_type IN ('VACATION', 'PERMIT'))` | Tipo de ausencia |
| `startDate` / `start_date` | `LocalDate` | `DATE` | `NOT NULL` | Fecha de inicio |
| `endDate` / `end_date` | `LocalDate` | `DATE` | `NOT NULL, CHECK (end_date >= start_date)` | Fecha de finalización |
| `calculatedDays` / `calculated_days` | `Integer` | `INTEGER` | `NOT NULL, CHECK (calculated_days >= 0)` | Número de días computables |
| `status` / `status` | `LeaveStatus` | `VARCHAR(50)` | `NOT NULL, CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))` | Estado de la solicitud |
| `rejectionReason` / `rejection_reason` | `String` | `TEXT` | Opcional | Motivo registrado si se rechaza |
| `resolvedBy` / `resolved_by` | `Employee` | `UUID` | `REFERENCES employees(id) ON DELETE SET NULL` | Responsable que resolvió la solicitud |
| `resolvedAt` / `resolved_at` | `Instant` | `TIMESTAMPTZ` | Opcional | Momento de resolución |
| `createdAt` / `created_at` | `Instant` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP, updatable = false` | Instante de auditoría de creación |
| `version` | `Long` | `BIGINT` | `NOT NULL DEFAULT 0, @Version` | Control de concurrencia optimista (Art. 12.6) |

#### Enums de Dominio
```java
package com.iw.rrhh.leave.domain;

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

## 2. Derived Value Object: `LeaveBalance` (Objeto de Dominio Calculado - Art. 12)

El saldo **no** es una tabla de base de datos; es un objeto de dominio inmutable derivado dinámicamente en tiempo de ejecución:

```java
package com.iw.rrhh.leave.domain;

import java.util.UUID;

public record LeaveBalance(
    UUID employeeId,
    int year,
    int entitledDays,
    int spentDays,
    int pendingDays,
    int availableDays,
    boolean isClosedYear,
    String agreementCode,
    String agreementName
) {
    public LeaveBalance {
        if (availableDays < 0) {
            availableDays = 0;
        }
    }
}
```

### Reglas de Validación y Cálculo Matemático (Invariante SC-002)
1. **Días correspondientes (`entitledDays`)**:
   - `agreement = contractRecord.getCollectiveAgreement()`
   - Si `employee.getHireDate().getYear() == year`:
     - `int monthsRemaining = 12 - employee.getHireDate().getMonthValue() + 1;`
     - `entitledDays = (int) Math.round((agreement.getVacationDaysPerYear() / 12.0) * monthsRemaining);`
   - Si `employee.getHireDate().getYear() < year`:
     - `entitledDays = agreement.getVacationDaysPerYear();`
   - Si `employee.getHireDate().getYear() > year`:
     - `entitledDays = 0;` (el empleado no pertenecía a la organización).
2. **Días disfrutados (`spentDays`)**:
   - Total de días de solicitudes con `leave_type = 'VACATION'` y `status = 'APPROVED'` cuyas fechas caen en el año `year`.
3. **Días en trámite (`pendingDays`)**:
   - Total de días con `leave_type = 'VACATION'` y `status = 'PENDING'` en el año `year`.
4. **Días disponibles (`availableDays`)**:
   - `availableDays = Math.max(0, entitledDays - spentDays)`.

---

## 3. Data Transfer Objects (DTOs)

### Package: `com.iw.rrhh.leave.dto`

#### `LeaveBalanceResponse`
```java
package com.iw.rrhh.leave.dto;

import java.util.UUID;

public record LeaveBalanceResponse(
    UUID employeeId,
    int year,
    int entitledDays,
    int spentDays,
    int pendingDays,
    int availableDays,
    boolean isClosedYear,
    String agreementName
) {}
```

#### `LeaveSummaryItemResponse` (Para listar el desglose de solicitudes del año)
```java
package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveSummaryItemResponse(
    UUID id,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    int calculatedDays,
    LeaveStatus status,
    String rejectionReason
) {}
```

### Package: `com.iw.rrhh.employee.dto`

#### `EmployeeProfileResponse`
```java
package com.iw.rrhh.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeProfileResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String nationalId,
    LocalDate hireDate,
    String currentPosition,
    String currentDepartment,
    String currentAgreementName,
    String managerFullName
) {}
```

---

## 4. Repositories & Aggregation Queries

### `LeaveRequestRepository`
```java
package com.iw.rrhh.leave.repository;

import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    @Query("""
        SELECT COALESCE(SUM(lr.calculatedDays), 0)
        FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.leaveType = :leaveType
          AND lr.status = :status
          AND lr.startDate >= :startOfYear
          AND lr.endDate <= :endOfYear
    """)
    int sumCalculatedDays(
        @Param("employeeId") UUID employeeId,
        @Param("leaveType") LeaveType leaveType,
        @Param("status") LeaveStatus status,
        @Param("startOfYear") LocalDate startOfYear,
        @Param("endOfYear") LocalDate endOfYear
    );

    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.startDate >= :startOfYear
          AND lr.endDate <= :endOfYear
        ORDER BY lr.startDate DESC
    """)
    List<LeaveRequest> findByEmployeeAndYear(
        @Param("employeeId") UUID employeeId,
        @Param("startOfYear") LocalDate startOfYear,
        @Param("endOfYear") LocalDate endOfYear
    );
}
```

