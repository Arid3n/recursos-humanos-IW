# Data Model: spec-006 — Calendario de Ausencias del Equipo

**Feature**: `spec-006` (HU-06)
**Date**: 2026-11-03
**Author**: Integrante 3

---

## 1. Entidades

Sin cambios de esquema ni migraciones. HU-06 es de solo lectura sobre:

| Entidad (tabla) | Campos usados | Regla |
|---|---|---|
| `LeaveRequest` (`leave_requests`) | `id`, `employee`, `leave_type`, `start_date`, `end_date`, `status` | Solo `PENDING` y `APPROVED`; rango que solapa el mes visible. |
| `Employee` (`employees`) | `id`, `first_name`, `last_name`, `manager_id`, `active` | Equipo = `manager_id` = responsable en sesión y `active = true`. |

**Nota**: en solicitudes `PENDING`, `resolved_by` contiene el responsable asignado en HU-03 (ver `specs/spec-005/research.md` §1.1); HU-06 no lo usa.

---

## 2. Enum nuevo

```java
// com.iw.rrhh.leave.domain
public enum CalendarDayType { WORKING, WEEKEND, HOLIDAY }
```

Prioridad al clasificar: `WEEKEND` antes que `HOLIDAY` (un festivo en sábado se muestra como fin de semana con su nombre).

---

## 3. Servicio de calendario existente (ampliación)

`HolidayCalendarService`:

| Método | Cambio |
|---|---|
| `Optional<String> getHolidayName(LocalDate date)` | **Nuevo**. Nombre del festivo nacional o `Optional.empty()`. |
| `boolean isHoliday(LocalDate date)` | Sin cambio de comportamiento; delega en `getHolidayName`. |

Nombres: Año Nuevo, Epifanía del Señor, Fiesta del Trabajo, Asunción de la Virgen, Fiesta Nacional de España, Todos los Santos, Día de la Constitución, Inmaculada Concepción, Natividad del Señor, Jueves Santo, Viernes Santo.

---

## 4. DTOs de salida (records, paquete `com.iw.rrhh.leave.dto`)

```java
public record TeamCalendarResponse(
    YearMonth month,                         // "2026-12"
    List<TeamMemberResponse> members,        // equipo activo, ordenado por apellidos
    List<TeamCalendarDayResponse> days       // un elemento por día del mes, en orden
) {}

public record TeamMemberResponse(UUID id, String name) {}

public record TeamCalendarDayResponse(
    LocalDate date,
    CalendarDayType dayType,
    String holidayName,                      // null si no es festivo
    int absentCount,                         // empleados distintos ausentes (0 si no laborable)
    boolean hasConflict,                     // absentCount >= 2
    List<TeamCalendarEventResponse> events   // vacío si no laborable
) {}

public record TeamCalendarEventResponse(
    UUID requestId,
    UUID employeeId,
    String employeeName,
    LeaveType leaveType,
    LeaveStatus status,                      // PENDING | APPROVED
    LocalDate startDate,                     // rango completo de la solicitud
    LocalDate endDate
) {}
```

---

## 5. Consultas de repositorio

| Repositorio | Método | Uso |
|---|---|---|
| `LeaveRequestRepository` | `List<LeaveRequest> findTeamCalendarRequests(UUID managerId, List<LeaveStatus> statuses, LocalDate rangeStart, LocalDate rangeEnd)` con `JOIN FETCH lr.employee` (ver `research.md` §1.3) | Ausencias del equipo que solapan el mes. Índice `idx_leave_calendar`. |
| `EmployeeRepository` | `findByManagerIdAndActiveTrue(UUID)` (existente) | Miembros del filtro. |

---

## 6. Reglas de negocio

| Regla | Origen | Resultado |
|---|---|---|
| Solo rol `RESPONSABLE_EQUIPO` | Art. 10, FR-011 | `403` en otro caso |
| Solo personas del equipo del responsable en sesión | FR-001, FR-010 | Otros equipos nunca se consultan |
| Solo `PENDING` y `APPROVED` | FR-001, FR-005 | `REJECTED` excluidas en la consulta |
| Consulta acotada al mes | FR-003, Art. 16.4 | Parámetro `month` obligatorio, `400` si inválido |
| Eventos solo en días laborables | FR-008 | Fines de semana y festivos sin eventos ni recuento |
| `hasConflict` si ≥ 2 empleados distintos ausentes | FR-006, Art. 16.3 | Calculado en servidor |
