# Data Model: spec-007 — Informes de Absentismo y Uso de Vacaciones

**Feature**: `spec-007` · HU-07  
**Date**: 2026-10-27  
**Database**: PostgreSQL 16

---

## 1. Sin migraciones nuevas

HU-07 es **de solo lectura** sobre las tablas existentes. No introduce nuevas tablas ni columnas.

| Tabla (existente) | Campos usados | Regla de acceso |
|---|---|---|
| `employees` | `id`, `first_name`, `last_name`, `hire_date`, `active` | Solo empleados `active = TRUE` con `hire_date <= endDate`. |
| `contract_records` | `employee_id`, `department`, `collective_agreement_id`, `effective_from`, `created_at` | Se resuelve el contrato vigente a `endDate` con `JOIN LATERAL` ordenado por `effective_from DESC, created_at DESC LIMIT 1`. |
| `collective_agreements` | `id`, `vacation_days_per_year` | Proporciona los días de vacaciones del convenio para calcular el porcentaje de uso. |
| `leave_requests` | `employee_id`, `leave_type`, `start_date`, `end_date`, `status`, `calculated_days` | Solo solicitudes `status = 'APPROVED'`; tipo `VACATION` para uso de vacaciones y `PERMIT` para absentismo. |

---

## 2. DTOs de salida (records, paquete `com.iw.rrhh.report.dto`)

```java
// Respuesta raíz del informe
public record DepartmentReportResponse(
    ReportType type,
    LocalDate startDate,
    LocalDate endDate,
    String periodLabel,         // "2026-01-01 – 2026-12-31"
    boolean hasData,            // false → no hay registros en el periodo
    List<DepartmentReportRow> departments
) {}

// Fila de departamento con desglose de empleados
public record DepartmentReportRow(
    String department,
    int headcount,
    BigDecimal totalEntitledDays,
    BigDecimal totalTakenDays,
    BigDecimal totalRemainingDays,
    BigDecimal metricPercentage,  // escala 2; 0.00 si denominador = 0
    List<EmployeeReportRow> employees
) {}

// Fila individual de empleado
public record EmployeeReportRow(
    UUID employeeId,
    String employeeName,
    BigDecimal totalEntitledDays,
    BigDecimal totalTakenDays,
    BigDecimal totalRemainingDays,
    BigDecimal metricPercentage
) {}

// Enum del tipo de informe
public enum ReportType { ABSENTEEISM, VACATION_USAGE }
```

---

## 3. Proyecciones de repositorio (interfaces)

```java
// Proyección de empleado con datos del contrato vigente
public interface ReportEmployeeProjection {
    UUID getEmployeeId();
    String getEmployeeName();
    String getDepartment();
    LocalDate getHireDate();
    Integer getVacationDaysPerYear();
}

// Proyección de agregado por departamento
public interface ReportDepartmentAggregateProjection {
    String getDepartment();
    Long getHeadcount();
    Long getFullPeriodTakenDays();
}

// Proyección de días por empleado (solicitudes completas en rango)
public interface ReportLeaveDaysProjection {
    UUID getEmployeeId();
    BigDecimal getTotalDays();
}

// Proyección de solicitudes que cruzan el límite del rango
public interface ReportLeavePeriodProjection {
    UUID getEmployeeId();
    LocalDate getStartDate();
    LocalDate getEndDate();
}
```

---

## 4. Consultas clave de repositorio

### 4.1 Empleados activos con contrato vigente a `endDate`
```sql
SELECT e.id AS "employeeId",
       concat(e.first_name, ' ', e.last_name) AS "employeeName",
       current_contract.department AS "department",
       e.hire_date AS "hireDate",
       current_contract.vacation_days_per_year AS "vacationDaysPerYear"
FROM employees e
JOIN LATERAL (
    SELECT cr.department, ca.vacation_days_per_year
    FROM contract_records cr
    JOIN collective_agreements ca ON ca.id = cr.collective_agreement_id
    WHERE cr.employee_id = e.id
      AND cr.effective_from <= :endDate
    ORDER BY cr.effective_from DESC, cr.created_at DESC, cr.id DESC
    LIMIT 1
) current_contract ON TRUE
WHERE e.active = TRUE
  AND e.hire_date <= :endDate
ORDER BY current_contract.department, e.last_name, e.first_name
```

### 4.2 Días tomados dentro del rango (solicitudes íntegramente incluidas)
```jpql
SELECT lr.employee.id AS employeeId, SUM(lr.calculatedDays) AS totalDays
FROM LeaveRequest lr
WHERE lr.status = :status
  AND lr.leaveType = :leaveType
  AND lr.startDate >= :periodStart
  AND lr.endDate <= :periodEnd
GROUP BY lr.employee.id
```

### 4.3 Solicitudes que cruzan el límite del rango (recorte manual con `HolidayCalendarService`)
```jpql
SELECT lr.employee.id AS employeeId,
       lr.startDate AS startDate,
       lr.endDate AS endDate
FROM LeaveRequest lr
WHERE lr.status = :status
  AND lr.leaveType = :leaveType
  AND lr.startDate <= :periodEnd
  AND lr.endDate >= :periodStart
  AND (lr.startDate < :periodStart OR lr.endDate > :periodEnd)
```

---

## 5. Fórmulas de cálculo

| Tipo | Días correspondientes | Días disfrutados | Porcentaje |
|---|---|---|---|
| `VACATION_USAGE` | Suma anual de `vacationDaysPerYear` del convenio (prorrateado por mes de alta en el año de ingreso) | Días de solicitudes `VACATION` aprobadas en el rango | `(disfrutados / correspondientes) * 100`, escala 2 |
| `ABSENTEEISM` | `countWorkingDays(max(hireDate, startDate), endDate)` | Días de solicitudes `PERMIT` aprobadas en el rango | `(disfrutados / correspondientes) * 100`, escala 2 |

**Denominador cero**: si `totalEntitledDays = 0`, el porcentaje devuelve `0.00` sin excepción aritmética.

---

## 6. Reglas de negocio

| Regla | Origen | Resultado |
|---|---|---|
| Solo rol `RRHH` | FR-001, Art. 10 | `403 Forbidden` para cualquier otro rol |
| Solo solicitudes `APPROVED` | Assumptions, FR-002 | Solicitudes pendientes, rechazadas y canceladas excluidas |
| Departamento según contrato vigente a `endDate` | FR-003 | `JOIN LATERAL` resuelve el departamento actual |
| Paridad pantalla ↔ CSV | FR-004, SC-002 | La misma `DepartmentReportResponse` alimenta ambos endpoints |
| Sin datos → `hasData: false` | FR-006 | La exportación responde `204 No Content` |
| BOM UTF-8 en CSV | FR-005 | `\uFEFF` antepuesto antes de codificar en UTF-8 |
