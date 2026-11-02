# Research & Technical Decisions: spec-003 (Solicitud de Vacaciones o Permisos)

**Feature**: `spec-003` · HU-03  
**Date**: 2026-11-01  
**Branch**: `spec-003`  
**Status**: Completed  

---

## Decision 1: Cómputo Centralizado de Días Laborables y Calendario de Festivos (Art. 14, FR-002)

### Context & Problem
El Artículo 14 de la Constitución del Proyecto prohíbe taxativamente que el frontend calcule días de forma autónoma o replique la lógica de negocio temporal. El cálculo debe ser provisto exclusivamente por el servidor para garantizar que se descuentan sábados, domingos y los festivos oficiales del calendario laboral.

### Decision
Se implementa un servicio dedicado `HolidayCalendarService` (o `WorkingDaysCalculatorService`) en el backend Spring Boot:
1. **Detección de fines de semana**: Todo sábado (`DayOfWeek.SATURDAY`) y domingo (`DayOfWeek.SUNDAY`) es excluido del cómputo.
2. **Calendario oficial de festivos**:
   - Se parametriza el catálogo de festivos oficiales nacionales/autonómicos estándar para la sede (Año Nuevo 1 Ene, Epifanía 6 Ene, Viernes Santo, Fiesta del Trabajo 1 May, Asunción 15 Ago, Fiesta Nacional 12 Oct, Todos los Santos 1 Nov, Constitución 6 Dic, Inmaculada 8 Dic, Navidad 25 Dic).
   - Se provee el método `countWorkingDays(LocalDate startDate, LocalDate endDate)` que itera el rango temporal sumando únicamente los días hábiles.
3. **Endpoint interactivo de previsualización**:
   - `GET /api/v1/leave/requests/calculate-days?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
   - Responde con `WorkingDaysCalculationResponse(startDate, endDate, workingDays, totalDays, holidayCount)` en menos de 300ms (SC-001).
   - Si `calculatedDays == 0` (por ejemplo, el rango seleccionado solo abarca un fin de semana o un festivo), el sistema devuelve 0 días y el formulario bloquea el botón de envío informando de la situación.

### Rationale
- Cumple rigurosamente el Art. 14 de la Constitución.
- Evita inconsistencias entre la estimación vista en pantalla y los días finalmente registrados en base de datos.
- Desacopla el cómputo de días del ciclo transaccional de guardado: el formulario consulta el endpoint al cambiar las fechas y previsualiza el resultado antes de confirmar.

### Alternatives Considered
- **Cálculo en TypeScript en el cliente:** Descartado categóricamente. Viola el Art. 14 ("El frontend no debe replicar cálculos de días laborales").
- **Tabla de BD para festivos:** Descartada para esta fase por sobreingeniería (Art. 23). Un servicio en memoria con fechas oficiales satisface los requisitos funcionales con máxima velocidad de respuesta (< 5ms en servidor, asegurando SC-001).

---

## Decision 2: Validación Transaccional contra el Saldo Disponible de Vacaciones (Art. 12, FR-004, FR-008)

### Context & Problem
Cuando un empleado solicita una ausencia de tipo `VACATION`, el sistema debe garantizar que el número de días laborables calculados no excede su saldo disponible en el año del inicio de la ausencia. Además, para ausencias de tipo `PERMIT` (permisos retribuidos/justificados), no debe tocarse el saldo anual.

### Decision
1. **Comprobación delegada en `LeaveBalanceService` (HU-02)**:
   - Al recibir `POST /api/v1/leave/requests`, si `leaveType == VACATION`:
     - Se invoca `leaveBalanceService.getMyLeaveBalance(request.startDate().getYear())`.
     - Si `calculatedDays > balance.availableDays()`, la transacción se aborta arrojando una excepción de negocio `InsufficientLeaveBalanceException` (mapeada a HTTP 400/409 ProblemDetail según RFC 9457).
     - La respuesta de error detalla el saldo disponible actual y los días requeridos.
2. **Tratamiento de `PERMIT`**:
   - Si `leaveType == PERMIT`, la validación de saldo anual de vacaciones se omite por completo. La solicitud se almacena como permiso pendiente de justificación y validación por su responsable en HU-05.

### Rationale
- Reutiliza la lógica de saldo derivado construida y probada en HU-02, manteniendo una única fuente de verdad (Art. 12).
- Protege la integridad del sistema impidiendo que nazcan solicitudes pendientes que de antemano superen el saldo disponible.

### Alternatives Considered
- **Permitir registrar solicitudes con saldo negativo y que RRHH decida:** Descartado. La especificación FR-004 indica explícitamente: "Si el número de días solicitados para vacaciones supera el saldo disponible del empleado, la solicitud NO DEBE crearse".

---

## Decision 3: Prevención y Bloqueo de Solapamientos de Ausencias (FR-007, SC-002)

### Context & Problem
Un empleado no puede solicitar ausencias cuyas fechas coincidan o se solapen parcial o totalmente con otra solicitud previa del mismo empleado que esté en estado `PENDING` o `APPROVED`.

### Decision
1. **Consulta de colisión en `LeaveRequestRepository`**:
   - Condición matemática de solapamiento de intervalos `[A, B]` y `[C, D]`: `A <= D AND B >= C`.
   - Consulta JPA en `LeaveRequestRepository`:
     ```sql
     SELECT COUNT(lr) > 0 FROM LeaveRequest lr
     WHERE lr.employee.id = :employeeId
       AND lr.status IN ('PENDING', 'APPROVED')
       AND lr.startDate <= :newEndDate
       AND lr.endDate >= :newStartDate
     ```
2. **Validación previa**:
   - Si la consulta retorna `true`, se lanza `LeaveRequestOverlapException`, devolviendo HTTP 409 Conflict con mensaje explícito indicando que ya existe una solicitud activa o en trámite en el intervalo seleccionado.

### Rationale
- Garantiza el 100% de solapamientos bloqueados preventivamente (SC-002).
- La consulta SQL por índice (`idx_leave_calendar` y `idx_leave_employee_status`) es inmediata y previene condiciones de carrera.

---

## Decision 4: Asignación Automática del Responsable y Estado Inicial PENDING (FR-003, FR-006, Art. 13)

### Context & Problem
La solicitud debe nacer asignada automáticamente al responsable del empleado solicitante, con estado estrictamente `PENDING`. El empleado no debe elegir manualmente a su evaluador ni manipular el estado inicial.

### Decision
1. **Resolución del responsable en servidor**:
   - Se obtiene el `Employee` del usuario autenticado en sesión (`SecurityContextHolder`).
   - El empleado contiene su `manager` (relación autoreferenciada `manager_id` en la tabla `employees`).
   - Si el empleado tiene `manager` asignado, la entidad `LeaveRequest` se vincula a dicho responsable.
   - Si el empleado no tiene manager asignado (por ejemplo, directivo o empleado raíz), se deja en null o se asigna a RRHH según política de la empresa.
2. **Estado canónico**:
   - Toda solicitud se instancia con `status = LeaveStatus.PENDING`.
   - `resolvedBy` y `resolvedAt` nacen en `null`.
   - Inserción con auditoría: `createdAt` gestionado por Spring Data JPA y `version = 0L` para optimistic locking (Art. 13).

---

## Decision 5: Formulario Reactivo en Angular con Notificación de Antelación (Art. 21, FR-001, SC-001)

### Context & Problem
La interfaz debe ser accesible (WCAG 2.1 AA), fluida e informar al usuario en tiempo real sin recargar la página.

### Decision
1. **Componente `LeaveRequestCreateComponent`**:
   - `ReactiveFormsModule` con `FormGroup`: campos `leaveType`, `startDate`, `endDate`.
   - `mat-date-range-input` y `mat-datepicker` con filtro para deshabilitar fechas pasadas si aplica, y validadores síncronos de formulario (`endDate >= startDate`).
   - `debounceTime(250)` en los cambios de fecha para invocar al endpoint `calculate-days` del backend.
   - Si la fecha de inicio es inferior a 7 días respecto a la fecha actual (`LocalDate.now()`), se muestra una advertencia informativa (`mat-icon` con aviso visual accesible) recomendando 7 días de antelación, sin bloquear el botón de confirmación (acorde a las clarificaciones de HU-03).
2. **Indicador de saldo**:
   - El formulario muestra el saldo disponible de vacaciones actual (obtenido de `LeaveBalanceService.getMyLeaveBalance`) y compara en vivo si los días calculados exceden el saldo, deshabilitando el botón de envío y mostrando `mat-error` accesible.
