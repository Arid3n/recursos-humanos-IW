# Feature Specification: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature Branch**: `spec-005`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-05  
**Asignación**: Integrante 3 (Gestión de Equipo y Ausencias)

## Clarifications

### Session 2026-10-27
- Q: ¿El motivo es estrictamente obligatorio al rechazar una solicitud? → A: Sí; rechazar exige indicar un motivo explicativo obligatorio (mínimo 5 caracteres) para garantizar transparencia al empleado, mientras que al aprobar el comentario es opcional.
- Q: ¿Se revalida el saldo de vacaciones en el momento de la aprobación? → A: Sí; en la misma transacción atómica de aprobación se comprueba que el empleado conserva saldo suficiente disponible, bloqueando la aprobación si otra solicitud concurrente ya consumió el crédito.
- Q: ¿Cuál es el canal oficial de entrega de las notificaciones al empleado? → A: Notificación interna persistida en la aplicación (`notifications`), visualizable en la cabecera de la UI con distintivo de no leída.
- Q: ¿Puede un responsable delegar o resolver solicitudes de otro equipo? → A: No; la autorización de alcance limita la resolución estrictamente a miembros directos del equipo del responsable en sesión (Art. 10 de la Constitución).

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como responsable de equipo, quiero acceder a la bandeja de solicitudes pendientes de los miembros de mi equipo y aprobarlas o rechazarlas (indicando obligatoriamente el motivo al denegar), para que la decisión quede formalizada en una única transacción atómica que actualice el saldo del empleado, envíe una notificación persistente y refleje la ausencia en el calendario del equipo.

**Why this priority**: Es el núcleo de la toma de decisiones sobre las ausencias de los trabajadores; conecta la petición (HU-03) con el saldo definitivo (HU-02) y la planificación de equipo (HU-06).

**Independent Test**: Puede probarse autenticándose como responsable, resolviendo una solicitud en `Pendiente` (aprobando o rechazando), y comprobando que: 1) el estado cambia irrevocablemente, 2) el saldo se descuenta si se aprueba, 3) se crea la notificación para el empleado y 4) un segundo intento de resolución devuelve error HTTP `409 Conflict`.

### Acceptance Scenarios

1. **Visualización de solicitudes pendientes del equipo**:
   - **Given** un responsable de equipo autenticado,
   - **When** accede a su panel de gestión de equipo,
   - **Then** visualiza únicamente las solicitudes en estado `Pendiente` correspondientes a miembros directos de su equipo, sin ver solicitudes de otros departamentos.

2. **Aprobación de solicitud con impacto atómico**:
   - **Given** una solicitud pendiente de un empleado de su equipo con saldo suficiente,
   - **When** el responsable pulsa "Aprobar" (añadiendo comentario opcional),
   - **Then** en una única transacción:
     - El estado de la solicitud pasa a `Aprobada`.
     - Se descuentan los días del saldo disponible del empleado.
     - Se genera una notificación en base de datos para el empleado.
     - La ausencia queda disponible para el calendario del equipo.

3. **Rechazo de solicitud con motivo obligatorio**:
   - **Given** una solicitud pendiente de un empleado del equipo,
   - **When** el responsable pulsa "Rechazar" e introduce la justificación obligatoria,
   - **Then** la solicitud pasa a estado `Rechazada`, el motivo queda registrado de forma inmutable, se genera la notificación al empleado y el saldo de vacaciones no sufre alteración.

4. **Bloqueo de rechazo sin motivo**:
   - **Given** una solicitud pendiente,
   - **When** el responsable intenta rechazar dejando el campo de motivo en blanco,
   - **Then** el sistema impide la acción mostrando un error de validación exigiendo la justificación.

5. **Inmutabilidad de solicitudes ya resueltas**:
   - **Given** una solicitud que ya se encuentra en estado `Aprobada` o `Rechazada`,
   - **When** se intenta resolver nuevamente mediante llamada a la API,
   - **Then** el servidor rechaza la operación con código HTTP `409 Conflict`.

6. **Control de acceso negativo**:
   - **Given** un responsable del Equipo A,
   - **When** intenta aprobar o rechazar una solicitud perteneciente a un miembro del Equipo B,
   - **Then** el sistema deniega la acción con código HTTP `403 Forbidden`.

### Edge Cases
- **Aprobación concurrente sobre saldo ajustado:** Si dos solicitudes pendientes de un mismo empleado se aprueban simultáneamente y la suma excede el saldo, la segunda operación aborta con error de saldo insuficiente gracias al bloqueo transaccional.
- **Cambio de responsable mientras hay solicitudes pendientes:** La solicitud pendiente se resuelve por el responsable que esté formalmente asignado al empleado en el momento de la resolución.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El responsable de equipo SOLO DEBE poder consultar y resolver solicitudes en estado `PENDING` de personas que pertenezcan a su equipo (Art. 10 y Art. 13 de la Constitución).
- **FR-002**: Las transiciones de estado permitidas DEBEN ser únicamente `PENDING → APPROVED` o `PENDING → REJECTED`.
- **FR-003**: Al rechazar una solicitud, el sistema DEBE exigir obligatoriamente un motivo (`rejectionReason`) de al menos 5 caracteres.
- **FR-004**: Al aprobar o rechazar, el sistema DEBE crear de forma automática y transaccional una entidad `Notification` persistida para el empleado solicitante.
- **FR-005**: Al aprobar una solicitud de tipo `VACATION`, el sistema DEBE revalidar y descontar los días del saldo del empleado de forma atómica (`@Transactional`).
- **FR-006**: Una solicitud en estado final (`APPROVED` o `REJECTED`) NO PUEDE volver a modificarse bajo ninguna circunstancia (devolviendo `409 Conflict`).

### Key Entities
- **LeaveRequest (Actualización)**: Registro de `status`, `resolvedAt`, `resolvedBy`, `rejectionReason`.
- **Notification**:
  - `id`: Identificador.
  - `recipientId`: ID del empleado destinatario.
  - `title`: Título de la notificación.
  - `message`: Contenido descriptivo y motivo si existe.
  - `isRead`: Booleano de lectura.
  - `createdAt`: Momento de emisión.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: La resolución completa (cambio de estado + saldo + notificación) se ejecuta en menos de 500ms garantizando atomicidad ACID.
- **SC-002**: 100% de solicitudes rechazadas contienen un motivo explicativo registrado.
- **SC-003**: 0% de solicitudes reabiertas o modificadas una vez resueltas.
- **SC-004**: 0% de aprobaciones cruzadas entre responsables de distintos equipos.

---

## 4. Assumptions
- La asignación de equipo se extrae del contrato vigente del empleado.
- La notificación interna en la aplicación es el canal estándar y autosuficiente del sistema.
