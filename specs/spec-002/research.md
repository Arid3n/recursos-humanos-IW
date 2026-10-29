# Research & Technical Decisions: spec-002 (Consulta de Ficha y Saldo de Vacaciones)

**Feature**: `spec-002` · HU-02  
**Date**: 2026-10-29  
**Branch**: `spec-002`  
**Status**: Completed  

---

## Decision 1: Estrategia de Cálculo Dinámico en Memoria del Saldo de Vacaciones (Art. 12)

### Context & Problem
El Artículo 12 de la Constitución del Proyecto establece que el saldo de vacaciones es un dato derivado y prohíbe taxativamente la existencia de contadores de saldo editables manualmente o desacoplados de la fuente de verdad. Además, las variaciones contractuales de convenio (HU-01) o la aprobación de solicitudes (HU-05) deben reflejarse de forma inmediata en el saldo sin requerir procesos por lotes.

### Decision
Se adopta una **estrategia de cálculo dinámico derivado bajo demanda** en la capa de servicio (`LeaveBalanceService`), sin almacenar una columna o tabla de "saldo acumulado" en la base de datos:
1. **Días correspondientes (`entitledDays`)**:
   - Se consulta el contrato vigente en el año fiscal objetivo mediante `ContractRecordRepository.findCurrentContract(employeeId, endOfYear)`.
   - Se obtienen los días base fijados por el convenio colectivo vigente (`CollectiveAgreement.vacationDaysPerYear`, por defecto 22 días laborables).
   - Si el empleado fue contratado en el año consultado (`employee.hireDate.getYear() == year`):
     - Se aplica la regla de prorrateo acordada: `Math.round((convenioDays / 12.0) * remainingMonths)`, donde `remainingMonths = 12 - employee.hireDate.getMonthValue() + 1`.
   - Si fue contratado antes del año consultado: `entitledDays = convenioDays`.
2. **Días disfrutados (`spentDays`)**:
   - Suma agregada de `leave_requests.calculated_days` correspondientes al empleado, al año consultado, con `leave_type = 'VACATION'` y `status = 'APPROVED'`.
   - Implementado mediante una consulta SQL agregada en PostgreSQL (`SUM(coalesce(lr.calculatedDays, 0))`) para evitar cargar entidades en memoria (Art. 8.6).
3. **Días en trámite (`pendingDays`)**:
   - Suma agregada de `leave_requests.calculated_days` con `leave_type = 'VACATION'` y `status = 'PENDING'`.
4. **Días disponibles (`availableDays`)**:
   - `availableDays = Math.max(0, entitledDays - spentDays)`.

### Rationale
- Garantiza 100% de coherencia matemática inmediata (SC-002). Si un cambio de convenio entra en vigor o un responsable aprueba una solicitud, la siguiente lectura de saldo refleja el nuevo valor al instante.
- Elimina cualquier riesgo de inconsistencias por estados intermedios, fallos de sincronización o concurrencia sobre contadores estáticos.
- Complejidad mínima (Art. 23): no requiere triggers en BD, ni colas de eventos, ni tareas batch programadas.

### Alternatives Considered
- **Tabla persistida de saldos anuales (`employee_leave_balances`) con trigger/listener:** Descartada. Viola el espíritu del Art. 12 ("El saldo se calcula a partir de la fuente de verdad... no existe un contador editable a mano"). Además, añade sobrecarga de sincronización transaccional y riesgo de desincronización si cambian convenios de forma retroactiva.
- **Cálculo en el cliente (Frontend):** Descartado categóricamente. Viola el Art. 5 (frontend desacoplado) y el Art. 14 (las reglas de negocio y cómputo residen en el backend).

---

## Decision 2: Identificación del Solicitante y Autorización en Servidor (Art. 10, FR-001, FR-005)

### Context & Problem
El Art. 10 y el FR-001 prohíben confiar en identificadores recibidos del cliente para determinar el alcance de datos de un usuario con rol `EMPLEADO`. Un empleado debe consultar únicamente su propia ficha y saldo, y cualquier intento de consultar datos ajenos debe devolver `403 Forbidden` o `404 Not Found`.

### Decision
1. **Endpoint propio sin parámetros de ID**:
   - Se expone `GET /api/v1/employees/me` para la ficha del empleado autenticado.
   - Se expone `GET /api/v1/leave/balances/me?year={year}` para el saldo del empleado autenticado.
   - El backend recupera el usuario autenticado directamente del `SecurityContextHolder` (`authentication.getName()`), busca el `User` en `UserRepository`, y resuelve el `Employee` asociado vía `EmployeeRepository.findByUserId(user.getId())`.
2. **Endpoint con ID explícito para supervisores/RRHH**:
   - Se expone `GET /api/v1/leave/balances/employee/{employeeId}?year={year}` y `GET /api/v1/employees/{employeeId}`.
   - Verificación de alcance en capa de servicio:
     - Rol `RRHH`: acceso permitido a cualquier empleado.
     - Rol `RESPONSABLE_EQUIPO`: permitido únicamente si `targetEmployee.manager.id == currentEmployee.id`.
     - Rol `EMPLEADO`: permitido únicamente si `targetEmployee.id == currentEmployee.id`; de lo contrario, lanza `AccessDeniedException` (mapeada a HTTP 403 RFC 9457).
3. **Tests de seguridad negativos**:
   - Se incluirán pruebas unitarias e integración en `LeaveBalanceSecurityTest` y `EmployeeSecurityTest` verificando que un empleado `A` recibe `403 Forbidden` al solicitar datos del empleado `B`.

### Rationale
- Cumplimiento estricto del Art. 10 de la Constitución: "La identidad del solicitante se obtiene del token/sesión, NUNCA de un parámetro enviado por el cliente".
- Prevención total de vulnerabilidades IDOR (Insecure Direct Object Reference) del OWASP Top 10 (Art. 20).

### Alternatives Considered
- **Usar siempre `/api/v1/leave/balances/{employeeId}` pasando el ID desde el frontend:** Descartado; fuerza al backend a validar que el ID coincida con el token en cada llamada y tienta al frontend a manipular la URL. Los endpoints `/me` son el estándar de la industria para datos propios.

---

## Decision 3: Consulta Histórica de Ejercicios Fiscales y Fechas de Ejercicio (FR-007, Aclaración Q4)

### Context & Problem
El empleado debe poder consultar saldos y ausencias de años fiscales pasados en modo solo lectura, manteniendo por defecto el año en curso.

### Decision
- El parámetro de consulta `year` en los endpoints de balance es opcional (`@RequestParam(required = false) Integer year`).
- Si no se especifica, toma `LocalDate.now().getYear()`.
- Si el año consultado es anterior al año actual:
  - El DTO de respuesta incluirá una bandera booleana `isClosedYear = true`.
  - El contrato colectivo considerado para el cálculo será el vigente a 31 de diciembre de dicho año fiscal (`targetDate = LocalDate.of(year, 12, 31)`).
  - Los días correspondientes y disfrutados reflejan el balance cerrado de dicho ejercicio.
- En el frontend, un selector (`mat-select`) en la cabecera del bloque de saldo permite conmutar entre el año actual y hasta 3 años precedentes.

### Rationale
- Cumple con la clarificación funcional Q4 y el requerimiento FR-007.
- Proporciona trazabilidad histórica sin requerir estructuras de datos separadas.

### Alternatives Considered
- **Permitir solo consulta del año en curso:** Rechazada; incumple la especificación funcional acordada.

---

## Decision 4: Diseño de Interfaz de Usuario y Componentes Angular (Art. 6, Art. 21)

### Context & Problem
La ficha del empleado y el saldo de vacaciones deben presentarse de forma accesible (WCAG 2.1 AA), responsive y con componentes nativos de Angular Material.

### Decision
- Se implementa el componente standalone `MyProfileComponent` en `frontend/src/app/features/employees/components/my-profile/` que incorpora:
  - Tarjeta de datos personales del empleado (nombre, DNI, email, fecha de contratación, puesto actual, departamento, responsable directo).
  - Subcomponente embebible `LeaveBalanceWidgetComponent` (`frontend/src/app/features/leave/components/leave-balance-widget/`):
    - Selector de año (`mat-form-field` + `mat-select`).
    - Cuadrícula de 4 tarjetas de métricas (`mat-card`):
      1. **Días correspondientes** (por convenio/prorrateo).
      2. **Días disfrutados** (aprobados en el año).
      3. **Días disponibles** (`correspondientes - disfrutados`), destacado visualmente con tipografía clara y contraste AA.
      4. **Días en trámite (pendientes)** (indicador informativo de solicitudes no resueltas).
    - Indicador de estado de carga (`mat-progress-spinner` o `mat-progress-bar`).
    - Banner informativo si el ejercicio fiscal está cerrado.
- Las llamadas HTTP se realizan a través de `LeaveService` y `EmployeeService` usando Signals reactivos de Angular 21.

### Alternatives Considered
- **Un único componente monolítico gigante:** Descartado; modularizar el widget de saldo en `features/leave` permite reutilizarlo tanto en la vista propia del empleado como en la vista de supervisión del responsable de equipo (HU-06).

