# Feature Specification: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature Branch**: `spec-005`

**Created**: 2026-10-27 · **Actualizada**: 2026-11-19

**Status**: Implementada y verificada (tests automatizados + validación manual, 2026-11-19), incluidos FR-015 (bloqueo sin resolutor válido), FR-016 (cancelación automática al darse de baja el superior) y FR-017 (rol `DIRECCION_GENERAL`).

**Historia de Usuario**: HU-05 · **Asignación**: Integrante 3 (Gestión de Equipo y Ausencias)

**Input**: User description: "HU-05 · Aprobación o rechazo de solicitudes. Como responsable de equipo, quiero aprobar o rechazar las solicitudes de las personas a mi cargo, indicando el motivo si lo hay, para decidir sobre las ausencias y que el empleado conozca el resultado de inmediato. Criterios de aceptación: (CA-1) El responsable ve las solicitudes en estado Pendiente de su equipo, y de ningún otro. (CA-2) Al aprobar, la solicitud pasa a Aprobada; al rechazar, pasa a Rechazada, con un campo para el motivo. (CA-3) Una solicitud ya resuelta no puede volver a resolverse. (CA-4) Al registrar la decisión, el empleado recibe automáticamente una notificación con el resultado y el motivo, si existe. (CA-5) Una solicitud aprobada actualiza el saldo del empleado (según HU-02) y aparece en el calendario del equipo."

## Clarifications

### Session 2026-10-27

- Q: ¿El motivo es estrictamente obligatorio al rechazar una solicitud? → A: Sí; rechazar exige indicar un motivo explicativo obligatorio (mínimo 5 caracteres) para garantizar transparencia al empleado, mientras que al aprobar el comentario es opcional.
- Q: ¿Se revalida el saldo de vacaciones en el momento de la aprobación? → A: Sí; en la misma operación atómica de aprobación se comprueba que el empleado conserva saldo suficiente disponible, bloqueando la aprobación si otra solicitud concurrente ya consumió el crédito.
- Q: ¿Cuál es el canal oficial de entrega de las notificaciones al empleado? → A: Notificación interna persistida en la aplicación, visualizable en la cabecera de la interfaz con distintivo de no leída.
- Q: ¿Puede un responsable delegar o resolver solicitudes de otro equipo? → A: No; la resolución se limita estrictamente a los miembros directos del equipo del responsable en sesión (Art. 10 de la Constitución).

### Session 2026-11-16

- Q: ¿Quién resuelve las solicitudes de un responsable de equipo y puede resolver las propias? → A: Nadie resuelve las propias; la solicitud de un responsable la resuelve su responsable superior (`manager_id`). Si no tiene superior, queda fuera de HU-05 (límite conocido).
- Q: ¿Contra qué saldo se valida al aprobar una solicitud de vacaciones que cruza dos años naturales? → A: Los días computados se reparten por año natural y se exige saldo suficiente en cada año; si falla alguno, la aprobación se bloquea.
- Q: ¿Qué respuesta HTTP se devuelve en solicitud ya resuelta, resuelta a la vez, retirada o con saldo insuficiente? → A: Ya resuelta o resuelta a la vez: `409`; retirada por el empleado: `404`; saldo insuficiente: `422`. El frontend recarga la bandeja tras cualquier `409` o `404`.
- Q: ¿Qué límites tienen el motivo de rechazo y el comentario de aprobación? → A: Motivo de rechazo de 5 a 500 caracteres tras quitar espacios extremos; comentario de aprobación opcional, hasta 500 caracteres; texto plano. Si se supera el máximo, `400` con error junto al campo.
- Q: ¿Puede aprobarse una solicitud pendiente con fechas en un año natural ya cerrado? → A: No; las fechas pasadas sólo se aprueban dentro del año natural en curso. Si alguna fecha cae en un año cerrado, la aprobación se bloquea con `422` y la solicitud permanece Pendiente (puede rechazarse).
- Q: Si la suma de días laborables por año no coincide con `calculatedDays` en una solicitud que cruza años, ¿qué regla reparte los días? → A: El total es siempre `calculatedDays`; los años anteriores al último usan los días laborables calculados hoy (limitados a `calculatedDays`) y el último año recibe el resto, de modo que la suma coincide siempre.
- Q: ¿Cómo cuenta el saldo de cada año una solicitud de vacaciones aprobada que cruza dos años, y qué ocurre si no hay días en el año siguiente? → A: Se corrige el cálculo de saldo (HU-02): cada año cuenta los días que le tocan según el reparto anterior, de modo que las solicitudes que cruzan años cuentan en ambos. El año siguiente usa su saldo normal (convenio del contrato vigente, con prorrateo si el empleado se incorpora ese año); si no alcanza, la aprobación se bloquea con `422` indicando el año.
- Q: ¿Quién resuelve la solicitud de un responsable cuando su superior no tiene el rol `RESPONSABLE_EQUIPO`, o no tiene superior? → A: En HU-05 sólo resuelve el superior directo con rol `RESPONSABLE_EQUIPO`; si no lo tiene o no hay superior (p. ej. la dirección), la solicitud queda sin resolutor (límite conocido) y se anota como mejora pendiente de HU-01 (validar el rol al asignar responsable).
- Q: ¿Debe HU-03 validar el saldo por año natural al crear una solicitud de vacaciones que cruza años (hallazgo N1 de `/speckit-analyze`)? → A: No en HU-05; se acepta como límite conocido. HU-03 sigue validando contra el saldo del año de inicio, de modo que una solicitud que cruza años puede crearse y después bloquearse al aprobar con `422` indicando el año. Justificación: HU-03 es una historia ya entregada, fuera del alcance de HU-05, y cambiarla exige actualizar antes su especificación y sus tests (Art. 1.3); el fallo es seguro, porque la aprobación revalida cada año (FR-007), la solicitud permanece Pendiente, puede rechazarse y nunca se produce saldo negativo (SC-006). Mejora pendiente de HU-03: validar por año con el mismo reparto (`LeaveDayAllocator`). Registrado en `plan.md` (Complexity Tracking).

### Session 2026-11-18

- Q: Cuando un empleado no tiene un resolutor válido (sin superior asignado o con un superior sin el rol `RESPONSABLE_EQUIPO`, como hoy `rrhh@empresa.com` y `responsable@empresa.com`), ¿debe el sistema impedirle crear la solicitud o permitirla y dejarla pendiente? → A: Impedirla: la creación se bloquea con `422` y un mensaje claro («no tienes un responsable asignado»), y la interfaz muestra ese mensaje en lugar del formulario. Las solicitudes huérfanas ya existentes permanecen Pendientes y el empleado puede retirarlas (HU-04). Requiere actualizar antes la spec de HU-03, que es quien crea la solicitud (Art. 1.3).
- Q: ¿Cuenta como resolutor válido un superior dado de baja (inactivo)? → A: No; sólo cuenta un superior activo con rol `RESPONSABLE_EQUIPO`. Con un superior inactivo la creación se bloquea con el mismo mensaje.
- Q: Si el superior de un empleado deja de ser válido (hoy solo: se da de baja; las variaciones de contrato no están soportadas) cuando éste ya tiene solicitudes Pendientes, ¿qué ocurre con ellas? → A (revisada 2026-11-18 a petición del propietario; sustituye la respuesta anterior «permanecen Pendientes sin cambios»): Las solicitudes Pendientes se «eliminan» pasando automáticamente a estado `CANCELLED` (como la retirada de HU-04; no se borra ninguna fila, queda rastro de auditoría) y se notifica al empleado el motivo; podrá crear una nueva solicitud cuando tenga asignado un nuevo superior válido.
- Q: ¿Dónde se ejecuta esa cancelación automática? → A: Dentro de las operaciones de HU-01 que la provocan, en la misma transacción: dar de baja a un empleado cancela las Pendientes de sus subordinados directos. Revisado 2026-11-18 (tarde): la única operación que lo provoca hoy es la baja; las variaciones de contrato (cambiar, quitar o asignar superior) no están soportadas y quedan fuera, y la pérdida del rol no se trata porque ninguna operación la permite. Exige actualizar antes la spec y los tests de HU-01 (Art. 1.3) y sustituye, sólo para esta regla, la respuesta anterior «la validación del rol en HU-01 queda como mejora separada».
- Q: Bloquear la creación deja sin poder solicitar ausencias a RRHH y a la dirección (sin superior); ¿se acepta como límite conocido hasta que se defina quién los aprueba? → A: Sí; límite conocido documentado, con decisión pendiente sobre quién aprueba las solicitudes de ese personal (Anexo B, pregunta 11 de la Constitución).
- Q: ¿Debe HU-01 rechazar al asignar un superior inactivo o sin rol `RESPONSABLE_EQUIPO` como parte de este cambio? → A: No; queda como mejora pendiente y separada de HU-01 (cambiarla exige antes su spec y tests, Art. 1.3). Mientras tanto, una asignación errónea la hace visible el bloqueo de FR-015 con su mensaje.
- Q: ¿Cómo se representa a un usuario de Dirección General: con un rol nuevo o como un `RESPONSABLE_EQUIPO` sin superior? → A: Con un rol nuevo `DIRECCION_GENERAL`. No puede crear solicitudes de ausencia (sea cual sea su superior), pero puede resolver las solicitudes de las personas que lo tienen como superior directo y consultar el calendario de ausencias de esas personas, igual que un responsable. Es un resolutor válido a efectos de FR-015. Exige enmendar la Constitución (Art. 10.1, 10.4 y Anexo A, enmienda MINOR, Art. 27) y actualizar antes las specs de HU-01 (alta con ese rol), HU-03 (bloqueo por rol) y HU-06 (acceso al calendario) (Art. 1.3).
- Q: Para un usuario de Dirección General, ¿se ocultan del menú los enlaces «Solicitar Ausencia» y «Mis Solicitudes» o se dejan visibles con un mensaje? → A: Se ocultan ambos enlaces; si accede por URL, ve el mensaje «Dirección General no solicita ausencias».
- Q: ¿Cómo obtiene un usuario el rol `DIRECCION_GENERAL`? → A: Al darlo de alta RRHH, eligiendo en el campo «Responsable de equipo» la opción «Sin responsable (solo Dirección General)» que ya existe en el formulario: esa elección (sin superior) crea el usuario con rol `DIRECCION_GENERAL`. Todo empleado salvo Dirección General tiene responsable, y Dirección General es la cúspide.
- Q: ¿Se enmienda el Art. 13.8 de la Constitución («solo el responsable del equipo del solicitante puede resolver»)? → A: No; sigue valiendo tal cual, porque quien resuelve es siempre el responsable directo de la persona y Dirección General lo es de las suyas.
- Q: ¿Se mantiene en el alta la comprobación del puesto (Director General / CEO) al elegir «Sin responsable»? → A: No. Todos los empleados salvo Dirección General deben tener responsable; quien no lo tiene es Dirección General (rol `DIRECCION_GENERAL`). Se elimina `isTopManagement` del alta y el seed asigna a Dirección General como superior de RRHH.
- Q: ¿Qué pasa con las variaciones de contrato en FR-016? → A: No están soportadas de momento; quedan fuera. Solo la baja del superior dispara la cancelación. Esto cierra también el caso de variaciones con fecha futura (sin *job* que las active).
- Q: ¿Una notificación por solicitud cancelada o por empleado? → A: Una por empleado, agrupando todas sus solicitudes canceladas.
- Q: ¿Se conectan las pantallas de HU-01 de variación e historial de contrato (hoy sin ruta)? → A: No; quedan fuera como hueco conocido de HU-01, junto con las variaciones de contrato.

### Session 2026-11-19

- Q: Cuando `RRHH` apruebe o rechace solicitudes y vea el calendario, ¿lo hará solo para sus subordinados directos o para todos los empleados? → A: Solo para sus subordinados directos, igual que `RESPONSABLE_EQUIPO` y `DIRECCION_GENERAL`; el calendario muestra a esas mismas personas. `RRHH` pasa a ser resolutor válido; no se amplía su alcance a toda la empresa.
- Q: Para un usuario de `DIRECCION_GENERAL`, ¿qué se oculta de «Mi Ficha y Saldo»? → A: Solo el apartado de saldo de vacaciones; la ficha personal y el enlace «Mi Ficha» se mantienen. El servidor deja de devolver el saldo propio a ese rol (`GET /leave/balances/me` → `403`, con registro de acceso denegado), no basta con ocultarlo en la interfaz (Art. 10).

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
- **Solicitud que cruza años naturales** (el saldo de HU-02 de ambos años refleja el reparto tras aprobar): se valida el saldo de cada año con los días que le corresponden; basta que un año no tenga saldo para bloquear la aprobación (la solicitud permanece Pendiente). **Límite conocido (N1)**: HU-03 sólo valida el saldo del año de inicio al crearla, por lo que puede existir una solicitud que cruza años y no es aprobable; el responsable puede rechazarla con motivo y el empleado crear otra. Mejora pendiente en HU-03.
- **Resolución simultánea de la misma solicitud**: si dos operaciones intentan resolver la misma solicitud a la vez, solo una tiene éxito; la otra recibe el aviso de que ya está resuelta.
- **Solicitud cancelada por el empleado antes de resolverse**: si el empleado retira la solicitud (HU-04) mientras el responsable tiene la bandeja abierta, la solicitud pasa a `CANCELLED` (HU-04) y el intento de resolución la trata como inexistente: devuelve `404` e informa de que la solicitud ya no existe (comprobado tras el alcance: fuera de alcance sigue siendo `403`).
- **Cambio de responsable con solicitudes pendientes**: la solicitud la resuelve el responsable asignado al empleado en el momento de la resolución; el responsable anterior deja de verla y de poder resolverla.
- **Permisos (no vacaciones)**: aprobar un permiso no altera el saldo de vacaciones (según HU-03), pero sí genera notificación y aparece en el calendario.
- **Solicitud propia del responsable**: no aparece en su bandeja y cualquier intento de resolverla se deniega (`403`); aparece en la bandeja de su responsable superior.
- **Empleado sin resolutor válido** (sin superior asignado, con un superior inactivo o con un superior sin rol `RESPONSABLE_EQUIPO`, `RRHH` ni `DIRECCION_GENERAL`): no puede crear solicitudes (FR-015). Las solicitudes huérfanas anteriores a esta regla permanecen Pendientes sin resolutor y pueden retirarse (HU-04). Cuando el superior de un empleado deja de ser válido (hoy, la baja del superior), sus solicitudes Pendientes pasan automáticamente a `CANCELLED` (sin borrar filas) y se le notifica; podrá crear una nueva al asignársele un superior válido. Regla (2026-11-18): todo empleado salvo Dirección General tiene un responsable y quien no lo tiene es Dirección General; los datos de prueba de RRHH se corrigen asignándole a Dirección General como superior. Dirección General no solicita ausencias por diseño (FR-017); decisión pendiente sobre quién aprueba a la propia Dirección General (Anexo B, pregunta 11). Mejora pendiente en HU-01: validar el rol del responsable al asignarlo (FR-016 sólo cubre la cancelación automática, no esa validación).
- **Bandeja vacía**: si no hay solicitudes pendientes en el equipo, se muestra un mensaje explícito indicándolo.
- **Responsable sin equipo**: un responsable sin personas a su cargo ve la bandeja vacía.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al responsable únicamente las solicitudes en estado Pendiente de las personas cuyo responsable asignado es él (Art. 10).
- **FR-002**: El sistema DEBE permitir al responsable aprobar una solicitud pendiente de su equipo, con un comentario opcional.
- **FR-003**: El sistema DEBE permitir al responsable rechazar una solicitud pendiente de su equipo, exigiendo un motivo de 5 a 500 caracteres una vez eliminados los espacios extremos. El comentario opcional de la aprobación admite hasta 500 caracteres. Ambos se tratan como texto plano; si se supera el máximo o el motivo queda por debajo del mínimo, el servidor devuelve `400` con el error asociado al campo.
- **FR-004**: Las únicas transiciones de estado permitidas DEBEN ser Pendiente → Aprobada y Pendiente → Rechazada; una solicitud resuelta NO DEBE poder resolverse ni modificarse de nuevo (Art. 13).
- **FR-004b**: Las respuestas de error de la resolución DEBEN ser: `409` si la solicitud ya está resuelta o se resolvió simultáneamente, `404` si el empleado la retiró, `422` si el saldo es insuficiente, `403` si está fuera de alcance. Tras un `409` o `404` el frontend DEBE recargar la bandeja.
- **FR-005**: La resolución DEBE registrar quién la realizó y cuándo; el motivo o comentario, si existe, queda registrado de forma inmutable.
- **FR-006**: El sistema DEBE denegar la resolución de solicitudes de personas que no pertenezcan al equipo del responsable en sesión, obteniendo la identidad del responsable de la sesión y nunca de datos enviados por el cliente (Art. 10). Un responsable NO DEBE poder resolver sus propias solicitudes (auto-aprobación): el sistema las deniega con `403` y las registra como acceso denegado. Las solicitudes de un responsable las resuelve su responsable superior; si no tiene superior asignado, o su superior no tiene el rol `RESPONSABLE_EQUIPO` ni `DIRECCION_GENERAL`, no puede crearlas (FR-015) y las anteriores a esa regla permanecen Pendientes sin resolutor; si el superior deja de ser válido después, las Pendientes del empleado pasan automáticamente a `CANCELLED` y se le notifica.
- **FR-007**: Al aprobar una solicitud de vacaciones, el sistema DEBE revalidar que el empleado dispone de saldo suficiente y bloquear la aprobación si no es así (Art. 12). Si la solicitud cruza años naturales, los días computados se reparten por año y se exige saldo suficiente en cada uno; si falla alguno, la aprobación se bloquea indicando el año afectado. El total repartido es siempre `calculatedDays`: los años anteriores al último usan los días laborables calculados con el calendario actual (limitados a `calculatedDays`) y el último año recibe el resto, de modo que la suma coincide siempre aunque hayan cambiado los festivos desde la creación.
- **FR-008**: El saldo del empleado NO se almacena como contador: las solicitudes aprobadas pasan a contar automáticamente como días disfrutados en el cálculo derivado de HU-02 (Art. 12). Una solicitud que cruza años cuenta en el saldo de cada año con los días que le corresponden según el reparto de FR-007; el cálculo de HU-02, que hoy sólo cuenta solicitudes contenidas íntegramente en un año, DEBE ajustarse para ello. El saldo de un año futuro se calcula como el de cualquier otro año (convenio del contrato vigente, con prorrateo si el empleado se incorpora ese año).
- **FR-009**: Al aprobar o rechazar, el sistema DEBE generar automáticamente una notificación persistida para el empleado solicitante, con el resultado, el periodo y el motivo o comentario si existe (Art. 15).
- **FR-010**: El cambio de estado, el efecto sobre el saldo y la creación de la notificación DEBEN realizarse en una única operación atómica: o se registra todo, o nada (Art. 13.7).
- **FR-011**: El empleado DEBE poder consultar sus propias notificaciones, ver el número de no leídas en la cabecera y marcarlas como leídas; NO DEBE poder acceder a notificaciones de otras personas.
- **FR-012**: Las solicitudes aprobadas DEBEN quedar disponibles para el calendario del equipo (HU-06) sin pasos adicionales.
- **FR-013**: Los intentos de resolución fuera de alcance DEBEN quedar registrados como acceso denegado, y las resoluciones como operación sensible auditada (Art. 20.5).
- **FR-014**: Los estados en la interfaz NO DEBEN transmitirse solo mediante color (Art. 21).
- **FR-015**: El sistema NO DEBE permitir crear una solicitud de ausencia a un empleado que no tiene un resolutor válido (responsable asignado activo, con rol `RESPONSABLE_EQUIPO`, `RRHH` o `DIRECCION_GENERAL` y distinto de él mismo). La creación se bloquea con `422` y el mensaje «no tienes un responsable asignado»; la interfaz de solicitud muestra ese mensaje en lugar del formulario. La regla se aplica en la creación (HU-03), por lo que exige actualizar antes la spec y los tests de HU-03 (Art. 1.3); HU-05 sólo la documenta y la consume.
- **FR-016**: Cuando el superior de un empleado deja de ser válido, las solicitudes `PENDING` de ese empleado DEBEN pasar automáticamente a `CANCELLED` y DEBE generarse una notificación persistida que indique el motivo («tu responsable ya no puede resolver tus solicitudes»). Se ejecuta dentro de la operación de HU-01 que lo provoca y en su misma transacción (Art. 13.7): al dar de baja a un empleado se cancelan las Pendientes de sus subordinados directos. Las variaciones de contrato (cambio, retirada o asignación de superior) no están soportadas hoy y quedan fuera de este requisito. Se genera una única notificación por empleado afectado, que agrupa todas sus solicitudes canceladas. No se borra ninguna fila. Exige actualizar antes la spec y los tests de HU-01 (Art. 1.3).
- **FR-017**: Existe el rol `DIRECCION_GENERAL`. Un usuario con ese rol NO DEBE poder crear solicitudes de ausencia (se bloquea con `422`; la interfaz oculta los enlaces «Solicitar Ausencia» y «Mis Solicitudes» y, si accede por URL, muestra el mensaje «Dirección General no solicita ausencias»), aunque tenga un superior asignado. SÍ DEBE poder, con el mismo alcance que `RESPONSABLE_EQUIPO` (Art. 10): ver la bandeja de solicitudes pendientes de las personas que lo tienen como superior directo, aprobarlas o rechazarlas (FR-001 a FR-013) y consultar el calendario de ausencias de esas personas (HU-06). NO DEBE poder consultar un saldo de vacaciones propio: la interfaz oculta el apartado de saldo de «Mi Ficha» (la ficha personal sigue visible) y `GET /leave/balances/me` responde `403`. El rol se asigna al dar de alta a la persona eligiendo la opción «Sin responsable (solo Dirección General)» del campo «Responsable de equipo»; no se comprueba el puesto: sin responsable implica `DIRECCION_GENERAL` y, a la inversa, todo empleado salvo Dirección General tiene responsable (HU-01 elimina su comprobación `isTopManagement` en el alta). La elección DEBE ser explícita: el alta envía `withoutManager=true`; un `managerId` ausente sin ese indicador, o ambos a la vez, devuelve `400`, de modo que un campo vacío por error no concede el rol. Nadie con este rol resuelve sus propias solicitudes ni las de personas ajenas a su equipo. Exige enmendar antes la Constitución (Art. 10.1, 10.4 y Anexo A).
- **FR-018** (2026-11-19): El rol `RRHH` DEBE poder, con el alcance de FR-017 (solo las personas que lo tienen como superior directo; Art. 10): ver la bandeja de solicitudes pendientes de esas personas, aprobarlas o rechazarlas (FR-001 a FR-013) y consultar su calendario de ausencias (HU-06). La interfaz muestra a RRHH los enlaces «Solicitudes del Equipo» y «Calendario del Equipo». RRHH sigue sin resolver solicitudes de personas que no dependen de él ni las propias. Exige enmendar antes la Constitución (Art. 10.4, fila `RRHH`, y Anexo A) y actualizar HU-06 (FR-011).

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
- **SC-007**: El 0% de las resoluciones o consultas de calendario de un usuario `RRHH` afectan a personas que no lo tienen como superior directo (FR-018).
- **SC-008**: El 0% de las consultas de saldo propio de un usuario `DIRECCION_GENERAL` devuelven datos: todas responden `403` y quedan registradas (FR-017).

## Assumptions

- El equipo de un responsable lo forman las personas que lo tienen asignado como responsable en su ficha de empleado (asignación gestionada en HU-01).
- El comentario opcional al aprobar se registra en el mismo campo de motivo de la solicitud y se incluye en la notificación.
- La notificación interna en la aplicación es el único canal; no se envía correo electrónico.
- `DIRECCION_GENERAL` no accede al listado de empleados ni a las evaluaciones de desempeño (HU-08); solo a la bandeja de resolución y al calendario de su equipo. Si hace falta, spec propia.
- Las solicitudes pendientes cuyas fechas ya han pasado pueden aprobarse (regularizaciones) sólo si todas sus fechas caen en el año natural en curso; si alguna cae en un año cerrado (HU-02), la aprobación se bloquea con `422` y la solicitud permanece Pendiente, aunque siempre puede rechazarse.
- La bandeja muestra las solicitudes ordenadas por antigüedad (las más antiguas primero) para resolver primero las que más esperan, con paginación en bloques de 10 como en HU-04.
- El calendario del equipo (HU-06) se desarrolla en `spec-006`; esta historia solo garantiza que la solicitud aprobada quede disponible para él.
- El cálculo de saldo (HU-02), salvo el ajuste para solicitudes que cruzan años (FR-008), el cómputo de días (HU-03) y el seguimiento de solicitudes por el empleado (HU-04) ya existen y se reutilizan.
