# Feature Specification: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Feature Branch**: `spec-004`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-04  
**Asignación**: Integrante 2 (Flujo de Ausencias del Empleado)

## Clarifications

### Session 2026-11-17
- Q: ¿Cómo deben persistirse y representarse en el sistema las solicitudes en estado Pendiente que el empleado decide cancelar? → A: Estado formal `CANCELLED` (`CANCELADA`): la solicitud transiciona a estado `CANCELLED`, permanece visible en el historial con opción de filtrado específico y libera inmediatamente el rango de fechas para no bloquear nuevas solicitudes.
- Q: ¿Qué impacto tiene la cancelación de una solicitud pendiente sobre el saldo disponible del empleado? → A: Al cancelar una solicitud de vacaciones en estado `PENDING`, los días retenidos preventivamente se liberan de inmediato, restableciendo el saldo disponible para futuras solicitudes.
- Q: ¿Debe mostrarse la justificación del empleado en el seguimiento de solicitudes de HU-04? → A: Sí; el DTO y la interfaz de HU-04 incluyen el campo `justification` para que el empleado pueda revisar el motivo aportado tanto en vacaciones como en permisos.
- Q: ¿Cuál es el formato del cómputo de días mostrado en el listado de HU-04? → A: Números enteros de días laborables (`calculatedDays` como `Integer >= 1`), ya que la unidad mínima de ausencia es la jornada completa.
- Q: ¿Pueden figurar solicitudes con fechas pasadas en el listado de HU-04? → A: Sí; el histórico de HU-04 presenta todas las solicitudes registradas por el empleado con anterioridad en cualquier estado, garantizando la trazabilidad histórica aunque las nuevas solicitudes requieran `startDate >= hoy`.

### Session 2026-10-27
- Q: ¿Puede el empleado cancelar una solicitud suya mientras permanezca en estado Pendiente? → A: Sí; el empleado puede cancelar/retirar directamente cualquier solicitud en estado Pendiente, liberando el periodo sin penalización.
- Q: ¿Puede el empleado cancelar una solicitud que ya ha sido Aprobada? → A: No de forma unilateral; una vez Aprobada, la ausencia consolida saldo y calendario, requiriendo solicitar su revocación o ajuste al responsable de equipo.
- Q: ¿Cuál es el criterio de ordenación por defecto del listado? → A: Ordenación cronológica descendente por fecha de registro (`created_at DESC`), situando en primer lugar las solicitudes más recientes.
- Q: ¿Cómo se maneja la paginación del histórico? → A: Paginación en servidor con bloques estándar de 10 registros y selector de página accesible.

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como empleado, quiero consultar un listado histórico de todas mis solicitudes de vacaciones y permisos, filtrar por su estado actual (Pendiente, Aprobada, Rechazada), revisar el motivo en caso de denegación y poder cancelar solicitudes que aún estén en trámite, para tener pleno control sobre la situación de mis ausencias.

**Why this priority**: Cierra el ciclo de visibilidad del empleado respecto a las peticiones iniciadas en HU-03.

**Independent Test**: Puede probarse creando peticiones en distintos estados (`Pendiente`, `Aprobada`, `Rechazada` con motivo de prueba), iniciando sesión con el empleado y comprobando que el listado pagina, filtra correctamente, cancela solicitudes en trámite y muestra el motivo de denegación sin exponer solicitudes ajenas.

### Acceptance Scenarios

1. **Listado completo de solicitudes del empleado**:
   - **Given** un empleado autenticado que ha registrado diversas solicitudes,
   - **When** accede a su bandeja "Mis Solicitudes",
   - **Then** visualiza una tabla ordenada con columnas: tipo (Vacaciones / Permiso), fecha de inicio, fecha de fin, días laborables, justificación del empleado, estado actual y fecha de solicitud.

2. **Detalle de motivo de rechazo**:
   - **Given** una solicitud en estado `Rechazada` con una justificación ("Coincidencia de personal crítico"),
   - **When** el empleado examina la fila o detalle de dicha solicitud,
   - **Then** el sistema muestra claramente el motivo explicativo registrado en HU-05.

3. **Cancelación de solicitud en estado Pendiente**:
   - **Given** una solicitud registrada que aún se encuentra en estado `Pendiente`,
   - **When** el empleado pulsa en el botón "Cancelar solicitud" y confirma,
   - **Then** la solicitud transiciona al estado `CANCELADA` (`CANCELLED`), permanece visible en su historial, libera preventivamente los días de vacaciones retenidos para el saldo disponible, libera las fechas para nuevas solicitudes y deja de aparecer como pendiente para su responsable.

4. **Filtrado dinámico por estado**:
   - **Given** un historial con solicitudes pendientes, aprobadas, rechazadas y canceladas,
   - **When** el empleado selecciona el filtro "Pendiente",
   - **Then** la vista actualiza el listado mostrando únicamente aquellas solicitudes en dicho estado.

5. **Aislamiento de alcance (Privacidad)**:
   - **Given** un empleado autenticado,
   - **When** se cargan las solicitudes,
   - **Then** el servidor filtra estrictamente en origen por el `employeeId` de la sesión y garantiza que nunca se devuelvan registros de otros trabajadores.

### Edge Cases
- **Empleado sin solicitudes registradas:** Se muestra un estado vacío comprensible con acceso directo a solicitar vacaciones.
- **Intento de cancelar una solicitud ya aprobada:** El botón de cancelación no está disponible y la API rechaza cualquier intento directo con código `409 Conflict`.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El sistema DEBE exponer un endpoint de listado paginado de solicitudes para el empleado en sesión, ordenado por `created_at DESC`.
- **FR-002**: Cada elemento del listado DEBE incluir: id, tipo, fechas de inicio y fin, días laborables, justificación del empleado (`justification`), estado (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`), fecha de creación y motivo de rechazo si aplica.
- **FR-003**: La vista DEBE permitir filtrar por estado mediante un selector accesible (`Todas`, `Pendiente`, `Aprobada`, `Rechazada`, `Cancelada`).
- **FR-004**: Si una solicitud está en estado `REJECTED`, el motivo (`rejectionReason`) DEBE ser visible para el empleado en la interfaz.
- **FR-005**: El backend DEBE rechazar cualquier intento de consultar o cancelar solicitudes ajenas por parte de un usuario con rol `EMPLEADO`.
- **FR-006**: El empleado DEBE poder cancelar sus propias solicitudes siempre y cuando su estado sea estrictamente `PENDING`, transicionando la entidad al estado `CANCELLED` y liberando el saldo preventivo retenido y las fechas reservadas.

### Key Entities
- **LeaveRequestSummary (DTO de Consulta)**:
  - `id`: Identificador.
  - `leaveType`: Tipo de ausencia.
  - `startDate`, `endDate`: Rango temporal.
  - `calculatedDays`: Días cómputo (entero `Integer >= 1`).
  - `justification`: Motivo o justificación aportada por el empleado.
  - `status`: Estado de la petición (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`).
  - `rejectionReason`: Justificación en caso de rechazo.
  - `createdAt`: Fecha de solicitud.
  - `canCancel`: Booleano `true` si `status == 'PENDING'`.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: El listado de solicitudes carga y se renderiza en menos de 500ms.
- **SC-002**: El filtro por estado responde de forma reactiva e instantánea en el cliente.
- **SC-003**: 100% de éxito en la cancelación de solicitudes pendientes y 0% de cancelaciones indebidas en solicitudes ya aprobadas.
- **SC-004**: Cero filtraciones de solicitudes entre empleados (100% tests negativos superados).

---

## 4. Assumptions
- La paginación maneja 10 registros por defecto con navegación accesible.
- La anulación de solicitudes aprobadas se canaliza como incidencia administrativa fuera del autoservicio del empleado.
