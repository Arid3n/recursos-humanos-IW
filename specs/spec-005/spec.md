# Feature Specification: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature Branch**: `spec-005`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-05  
**Asignación**: Integrante 3 (Gestión de Equipo y Ausencias)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como responsable de equipo, quiero acceder a la bandeja de solicitudes pendientes de los miembros de mi equipo y aprobarlas o rechazarlas (indicando el motivo si procede), para que la decisión quede formalizada en una única transacción atómica que actualice el saldo del empleado, envíe una notificación persistente y refleje la ausencia en el calendario del equipo.

**Why this priority**: Es el núcleo de la toma de decisiones sobre las ausencias de los trabajadores; conecta la petición (HU-03) con el saldo definitivo (HU-02) y la planificación de equipo (HU-06).

**Independent Test**: Puede probarse autenticándose como responsable, resolviendo una solicitud en `Pendiente` (aprobando o rechazando), y comprobando que: 1) el estado cambia irrevocablemente, 2) el saldo se descuenta si se aprueba, 3) se crea la notificación para el empleado y 4) un segundo intento de resolución devuelve error HTTP `409 Conflict`.

### Acceptance Scenarios

1. **Visualización de solicitudes pendientes del equipo**:
   - **Given** un responsable de equipo autenticado,
   - **When** accede a su panel de gestión de equipo,
   - **Then** visualiza únicamente las solicitudes en estado `Pendiente` correspondientes a miembros directos de su equipo, sin ver solicitudes de otros departamentos.

2. **Aprobación de solicitud con impacto atómico**:
   - **Given** una solicitud pendiente de un empleado de su equipo,
   - **When** el responsable pulsa "Aprobar" (añadiendo comentario opcional),
   - **Then** en una única transacción:
     - El estado de la solicitud pasa a `Aprobada`.
     - Se actualiza el saldo del empleado reflejando los días disfrutados.
     - Se genera una notificación en base de datos para el empleado con el resultado.
     - La ausencia queda disponible para el calendario del equipo.

3. **Rechazo de solicitud con motivo**:
   - **Given** una solicitud pendiente de un empleado del equipo,
   - **When** el responsable pulsa "Rechazar" e introduce el motivo en el cuadro de texto habilitado,
   - **Then** la solicitud pasa a estado `Rechazada`, el motivo queda registrado de forma inmutable, se genera la notificación al empleado y el saldo de vacaciones no sufre alteración.

4. **Inmutabilidad de solicitudes ya resueltas**:
   - **Given** una solicitud que ya se encuentra en estado `Aprobada` o `Rechazada`,
   - **When** se intenta resolver nuevamente mediante llamada a la API,
   - **Then** el servidor rechaza la operación con código HTTP `409 Conflict`, impidiendo cambios de estado posteriores.

5. **Control de acceso negativo**:
   - **Given** un responsable del Equipo A,
   - **When** intenta aprobar o rechazar una solicitud perteneciente a un miembro del Equipo B,
   - **Then** el sistema deniega la acción con código HTTP `403 Forbidden`.

### Edge Cases
- **Aprobación concurrente sobre saldo ajustado:** Si dos solicitudes pendientes de un mismo empleado agotan el saldo si se aprobaran ambas a la vez, el sistema valida el saldo remanente en el momento exacto de la aprobación bajo bloqueo optimista/pesimista para impedir saldos negativos.
- **Fallo en notificación:** Si fallase el envío por un canal secundario (p. ej. email simulado), la transacción de aprobación en base de datos no debe abortarse si la notificación interna ya está persistida.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El responsable de equipo SOLO DEBE poder consultar y resolver solicitudes en estado `PENDING` de personas que pertenezcan a su equipo (Art. 10 y Art. 13 de la Constitución).
- **FR-002**: Las transiciones de estado permitidas DEBEN ser únicamente `PENDING → APPROVED` o `PENDING → REJECTED`.
- **FR-003**: Al rechazar una solicitud, el sistema DEBE registrar el motivo proporcionado por el responsable (`rejectionReason`).
- **FR-004**: Al aprobar o rechazar, el sistema DEBE crear de forma automática y transaccional una entidad `Notification` persistida para el empleado.
- **FR-005**: Al aprobar una solicitud de tipo `VACATION`, el sistema DEBE consolidar los días en el saldo del empleado de forma atómica (`@Transactional`).
- **FR-006**: Una solicitud en estado final (`APPROVED` o `REJECTED`) NO PUEDE volver a modificarse bajo ninguna circunstancia (devolviendo `409 Conflict`).

### Key Entities
- **LeaveRequest (Actualización)**: Registro de `status`, `resolvedAt`, `resolvedBy`, `rejectionReason`.
- **Notification**:
  - `id`: Identificador.
  - `recipientId`: ID del empleado destinatario.
  - `title`: Título de la notificación ("Solicitud de vacaciones aprobada/rechazada").
  - `message`: Contenido descriptivo y motivo si existe.
  - `read`: Booleano de lectura.
  - `createdAt`: Momento de emisión.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: La resolución completa (cambio de estado + saldo + notificación) se ejecuta en menos de 500ms garantizando atomicidad ACID.
- **SC-002**: 100% de solicitudes resueltas generan su notificación correspondiente en base de datos.
- **SC-003**: 0% de solicitudes reabiertas o modificadas una vez resueltas.
- **SC-004**: 0% de aprobaciones cruzadas entre responsables de distintos equipos.

---

## 4. Assumptions
- La asignación de empleado a responsable de equipo se extrae del registro contractual vigente del empleado en la fecha de la solicitud.
