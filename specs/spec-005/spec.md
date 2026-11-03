# Feature Specification: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature Branch**: `spec-005`

**Created**: 2026-10-27 · **Actualizada**: 2026-11-03

**Status**: Draft

**Historia de Usuario**: HU-05 · **Asignación**: Integrante 3 (Gestión de Equipo y Ausencias)

**Input**: User description: "HU-05 · Aprobación o rechazo de solicitudes. Como responsable de equipo, quiero aprobar o rechazar las solicitudes de las personas a mi cargo, indicando el motivo si lo hay, para decidir sobre las ausencias y que el empleado conozca el resultado de inmediato. Criterios de aceptación: (CA-1) El responsable ve las solicitudes en estado Pendiente de su equipo, y de ningún otro. (CA-2) Al aprobar, la solicitud pasa a Aprobada; al rechazar, pasa a Rechazada, con un campo para el motivo. (CA-3) Una solicitud ya resuelta no puede volver a resolverse. (CA-4) Al registrar la decisión, el empleado recibe automáticamente una notificación con el resultado y el motivo, si existe. (CA-5) Una solicitud aprobada actualiza el saldo del empleado (según HU-02) y aparece en el calendario del equipo."

## Clarifications

### Session 2026-10-27

- Q: ¿El motivo es estrictamente obligatorio al rechazar una solicitud? → A: Sí; rechazar exige indicar un motivo explicativo obligatorio (mínimo 5 caracteres) para garantizar transparencia al empleado, mientras que al aprobar el comentario es opcional.
- Q: ¿Se revalida el saldo de vacaciones en el momento de la aprobación? → A: Sí; en la misma operación atómica de aprobación se comprueba que el empleado conserva saldo suficiente disponible, bloqueando la aprobación si otra solicitud concurrente ya consumió el crédito.
- Q: ¿Cuál es el canal oficial de entrega de las notificaciones al empleado? → A: Notificación interna persistida en la aplicación, visualizable en la cabecera de la interfaz con distintivo de no leída.
- Q: ¿Puede un responsable delegar o resolver solicitudes de otro equipo? → A: No; la resolución se limita estrictamente a los miembros directos del equipo del responsable en sesión (Art. 10 de la Constitución).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Resolver solicitudes pendientes del equipo (Priority: P1)

Como responsable de equipo, accedo a una bandeja con las solicitudes pendientes de las personas a mi cargo y apruebo o rechazo cada una. Al rechazar indico obligatoriamente el motivo; al aprobar puedo añadir un comentario opcional. La decisión queda registrada de forma definitiva, junto con quién la tomó y cuándo.

**Why this priority**: Es el núcleo de la historia: sin decisión no hay ausencia confirmada. Conecta la petición del empleado (HU-03) con su saldo (HU-02) y con la planificación del equipo (HU-06).

**Independent Test**: Autenticarse como responsable, comprobar que la bandeja solo contiene solicitudes pendientes de su equipo, aprobar una y rechazar otra con motivo, y verificar que ambas cambian de estado de forma definitiva y que un segundo intento de resolución es rechazado.

**Acceptance Scenarios**:

1. **(CA-1) Given** un responsable autenticado con miembros a su cargo que tienen solicitudes en estados Pendiente, Aprobada y Rechazada, y existen solicitudes pendientes de empleados de otros equipos, **When** accede a la bandeja de solicitudes de su equipo, **Then** ve únicamente las solicitudes en estado Pendiente de sus miembros directos, con el nombre del empleado, tipo de ausencia, fechas y días computados.
2. **(CA-2) Given** una solicitud pendiente de un miembro de su equipo con saldo suficiente, **When** el responsable la aprueba (con o sin comentario), **Then** la solicitud pasa a Aprobada y se registran el responsable que resolvió y el momento de la resolución.
3. **(CA-2) Given** una solicitud pendiente de un miembro de su equipo, **When** el responsable la rechaza indicando un motivo de al menos 5 caracteres, **Then** la solicitud pasa a Rechazada, el motivo queda registrado y el empleado puede verlo en el seguimiento de sus solicitudes (HU-04).
4. **(CA-2) Given** una solicitud pendiente, **When** el responsable intenta rechazarla con el motivo vacío o con menos de 5 caracteres, **Then** el sistema impide la acción e indica junto al campo que el motivo es obligatorio.
5. **(CA-3) Given** una solicitud ya Aprobada o Rechazada, **When** se intenta resolver de nuevo por cualquier vía, **Then** el sistema rechaza la operación informando de que la solicitud ya está resuelta, y la solicitud no sufre ningún cambio.
6. **(CA-1) Given** un responsable del Equipo A, **When** intenta aprobar o rechazar una solicitud de un miembro del Equipo B, **Then** el sistema deniega la acción por falta de permiso y la solicitud no sufre ningún cambio.
7. **(CA-5) Given** una solicitud de vacaciones pendiente, **When** el responsable la aprueba, **Then** los días computados pasan a contar como disfrutados en el saldo del empleado (HU-02) y la ausencia queda disponible para el calendario del equipo (HU-06).
8. **(CA-5) Given** una solicitud de vacaciones pendiente cuyo número de días supera el saldo disponible actual del empleado, **When** el responsable intenta aprobarla, **Then** el sistema bloquea la aprobación informando del saldo insuficiente y la solicitud permanece Pendiente.

---

### User Story 2 - Notificación del resultado al empleado (Priority: P2)

Como empleado, recibo automáticamente una notificación dentro de la aplicación cuando mi responsable resuelve una de mis solicitudes, con el resultado y el motivo si existe. La cabecera de la aplicación me indica cuántas notificaciones tengo sin leer y puedo consultarlas y marcarlas como leídas.

**Why this priority**: Cumple la parte de la historia "que el empleado conozca el resultado de inmediato". Depende de que exista la resolución (P1), pero aporta valor de forma independiente al empleado.

**Independent Test**: Resolver una solicitud como responsable, autenticarse como el empleado solicitante y comprobar que aparece una notificación no leída con el resultado (y el motivo si es un rechazo), que puede marcarse como leída y que otros empleados no la ven.

**Acceptance Scenarios**:

1. **(CA-4) Given** una solicitud pendiente, **When** el responsable la aprueba o la rechaza, **Then** se genera automáticamente para el empleado solicitante una notificación con el resultado, el periodo de la ausencia y el motivo o comentario si existe, en la misma operación que registra la decisión.
2. **(CA-4) Given** un empleado con notificaciones sin leer, **When** accede a la aplicación, **Then** la cabecera muestra un distintivo con el número de notificaciones no leídas, indicado con texto además de color.
3. **(CA-4) Given** un empleado que consulta sus notificaciones, **When** marca una como leída, **Then** deja de contar como no leída.
4. **(CA-4) Given** un empleado autenticado, **When** consulta las notificaciones, **Then** solo ve las suyas y no puede consultar ni marcar como leídas las de otra persona.
5. **(CA-4) Given** un fallo al registrar la notificación, **When** el responsable resuelve la solicitud, **Then** no se registra ni la decisión ni la notificación (todo o nada).

---

### Edge Cases

- **Aprobación concurrente sobre saldo ajustado**: si dos solicitudes de vacaciones pendientes del mismo empleado se aprueban a la vez y entre ambas superan el saldo disponible, solo una se aprueba; la otra se bloquea por saldo insuficiente y permanece Pendiente.
- **Resolución simultánea de la misma solicitud**: si dos operaciones intentan resolver la misma solicitud a la vez, solo una tiene éxito; la otra recibe el aviso de que ya está resuelta.
- **Solicitud cancelada por el empleado antes de resolverse**: si el empleado retira la solicitud (HU-04) mientras el responsable tiene la bandeja abierta, el intento de resolución informa de que la solicitud ya no existe.
- **Cambio de responsable con solicitudes pendientes**: la solicitud la resuelve el responsable asignado al empleado en el momento de la resolución; el responsable anterior deja de verla y de poder resolverla.
- **Permisos (no vacaciones)**: aprobar un permiso no altera el saldo de vacaciones (según HU-03), pero sí genera notificación y aparece en el calendario.
- **Bandeja vacía**: si no hay solicitudes pendientes en el equipo, se muestra un mensaje explícito indicándolo.
- **Responsable sin equipo**: un responsable sin personas a su cargo ve la bandeja vacía.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al responsable únicamente las solicitudes en estado Pendiente de las personas cuyo responsable asignado es él (Art. 10).
- **FR-002**: El sistema DEBE permitir al responsable aprobar una solicitud pendiente de su equipo, con un comentario opcional.
- **FR-003**: El sistema DEBE permitir al responsable rechazar una solicitud pendiente de su equipo, exigiendo un motivo de al menos 5 caracteres.
- **FR-004**: Las únicas transiciones de estado permitidas DEBEN ser Pendiente → Aprobada y Pendiente → Rechazada; una solicitud resuelta NO DEBE poder resolverse ni modificarse de nuevo (Art. 13).
- **FR-005**: La resolución DEBE registrar quién la realizó y cuándo; el motivo o comentario, si existe, queda registrado de forma inmutable.
- **FR-006**: El sistema DEBE denegar la resolución de solicitudes de personas que no pertenezcan al equipo del responsable en sesión, obteniendo la identidad del responsable de la sesión y nunca de datos enviados por el cliente (Art. 10).
- **FR-007**: Al aprobar una solicitud de vacaciones, el sistema DEBE revalidar que el empleado dispone de saldo suficiente y bloquear la aprobación si no es así (Art. 12).
- **FR-008**: El saldo del empleado NO se almacena como contador: las solicitudes aprobadas pasan a contar automáticamente como días disfrutados en el cálculo derivado de HU-02 (Art. 12).
- **FR-009**: Al aprobar o rechazar, el sistema DEBE generar automáticamente una notificación persistida para el empleado solicitante, con el resultado, el periodo y el motivo o comentario si existe (Art. 15).
- **FR-010**: El cambio de estado, el efecto sobre el saldo y la creación de la notificación DEBEN realizarse en una única operación atómica: o se registra todo, o nada (Art. 13.7).
- **FR-011**: El empleado DEBE poder consultar sus propias notificaciones, ver el número de no leídas en la cabecera y marcarlas como leídas; NO DEBE poder acceder a notificaciones de otras personas.
- **FR-012**: Las solicitudes aprobadas DEBEN quedar disponibles para el calendario del equipo (HU-06) sin pasos adicionales.
- **FR-013**: Los intentos de resolución fuera de alcance DEBEN quedar registrados como acceso denegado, y las resoluciones como operación sensible auditada (Art. 20.5).
- **FR-014**: Los estados en la interfaz NO DEBEN transmitirse solo mediante color (Art. 21).

### Key Entities *(include if feature involves data)*

- **Solicitud de ausencia (existente, se amplía su uso)**: petición de un empleado (tipo, fechas, días computados, estado). En esta historia se registran su resolución: nuevo estado, responsable que resolvió, momento de la resolución y motivo o comentario.
- **Notificación**: aviso persistido dirigido a un empleado. Contiene título, mensaje descriptivo (resultado, periodo y motivo si existe), indicador de leída y momento de emisión. Pertenece a un único destinatario.
- **Equipo**: no es una entidad propia; lo forman las personas cuyo responsable asignado es el responsable en sesión.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El responsable puede resolver una solicitud pendiente desde la bandeja en menos de 30 segundos y con un máximo de 2 interacciones (más el motivo si rechaza).
- **SC-002**: El 100% de las solicitudes rechazadas tienen un motivo registrado de al menos 5 caracteres.
- **SC-003**: El 0% de las solicitudes resueltas se reabren o modifican tras su resolución.
- **SC-004**: El 0% de las resoluciones se realizan sobre solicitudes de personas de otro equipo.
- **SC-005**: El 100% de las decisiones registradas tienen su notificación asociada, visible para el empleado en cuanto accede a la aplicación.
- **SC-006**: Ningún empleado queda con saldo de vacaciones negativo por aprobaciones simultáneas.

## Assumptions

- El equipo de un responsable lo forman las personas que lo tienen asignado como responsable en su ficha de empleado (asignación gestionada en HU-01).
- El comentario opcional al aprobar se registra en el mismo campo de motivo de la solicitud y se incluye en la notificación.
- La notificación interna en la aplicación es el único canal; no se envía correo electrónico.
- Las solicitudes pendientes cuyas fechas ya han pasado pueden resolverse igualmente (regularizaciones).
- La bandeja muestra las solicitudes ordenadas por antigüedad (las más antiguas primero) para resolver primero las que más esperan, con paginación en bloques de 10 como en HU-04.
- El calendario del equipo (HU-06) se desarrolla en `spec-006`; esta historia solo garantiza que la solicitud aprobada quede disponible para él.
- El cálculo de saldo (HU-02), el cómputo de días (HU-03) y el seguimiento de solicitudes por el empleado (HU-04) ya existen y se reutilizan.
