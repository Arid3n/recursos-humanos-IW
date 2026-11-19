# Quickstart: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` (HU-05)
**Date**: 2026-11-03 · **Revisado**: 2026-11-18
**Author**: Integrante 3

---

## 1. Requisitos Previos

- PostgreSQL activo: `docker compose up -d`
- Backend: `cd backend && ./mvnw spring-boot:run` (aplica `V3` y `V6__leave_resolution_reason_max_length.sql`)
- Frontend: `cd frontend && npm start` → `http://localhost:4200`
- Usuarios de desarrollo (contraseña `admin123`, ver `research.md` §1.9):
  - `responsable@empresa.com` — `RESPONSABLE_EQUIPO`, responsable de `empleado@empresa.com`
  - `empleado@empresa.com` — `EMPLEADO`
  - `rrhh@empresa.com` — `RRHH`, subordinado directo de `direccion@empresa.com` y superior de `responsable@empresa.com`; resolutor válido (FR-018) para sus subordinados directos y puede solicitar ausencias
  - `direccion@empresa.com` — `DIRECCION_GENERAL` (nuevo en el *seed*, 2026-11-18), superior de `rrhh@empresa.com`; no solicita ausencias (FR-017). Jerarquía de pruebas: `direccion@` → `rrhh@` → `responsable@` → `empleado@`
- Contrato de referencia: [contracts/leave-resolution-api.yaml](contracts/leave-resolution-api.yaml) · Modelo: [data-model.md](data-model.md)

**Preparación**: como `empleado@empresa.com`, crear desde "Solicitar Ausencia" (HU-03) dos solicitudes de vacaciones en fechas distintas y un permiso.

---

## 2. Escenarios de Validación

### Escenario 1 — Bandeja del equipo (HU-05.CA-1)
1. Iniciar sesión como `responsable@empresa.com` → "Solicitudes del Equipo" (`/team/requests`).
2. **Esperado**: solo aparecen las solicitudes `PENDING` de Laura Martínez, de la más antigua a la más reciente, con empleado, tipo, fechas, días y fecha de solicitud. Sin solicitudes aprobadas, rechazadas ni de otros equipos.

### Escenario 2 — Aprobación (HU-05.CA-2, CA-5)
1. Pulsar "Aprobar" en una solicitud de vacaciones, añadir un comentario opcional y confirmar.
2. **Esperado**: mensaje de éxito; la solicitud desaparece de la bandeja.
3. Como `empleado@empresa.com`, en "Mi Ficha y Saldo" (HU-02) los días aparecen como disfrutados y "Disponibles" se reduce; en "Mis Solicitudes" (HU-04) aparece como Aprobada.
4. Como `responsable@empresa.com`, en el calendario del equipo (HU-06) la ausencia aprobada aparece en el mes correspondiente (FR-012).

### Escenario 3 — Rechazo con motivo (HU-05.CA-2)
1. Pulsar "Rechazar" y dejar el motivo vacío o con menos de 5 caracteres.
2. **Esperado**: el botón de confirmar no envía y se muestra el error junto al campo. Un motivo de más de 500 caracteres también se rechaza (`400`, `invalidFields.reason`).
3. Escribir un motivo válido y confirmar.
4. **Esperado**: la solicitud desaparece de la bandeja; en "Mis Solicitudes" del empleado figura Rechazada con "Ver motivo"; el saldo no cambia.

### Escenario 4 — Re-resolución bloqueada (HU-05.CA-3)
```bash
curl -i -u responsable@empresa.com:admin123 -X POST \
  http://localhost:8080/api/v1/leave/requests/<id-ya-resuelta>/approve
```
**Esperado**: `409 Conflict` con ProblemDetail; la solicitud no cambia.

### Escenario 5 — Fuera de alcance (HU-05.CA-1, Art. 10)
```bash
curl -i -u rrhh@empresa.com:admin123 -X POST \
  -H 'Content-Type: application/json' -d '{"reason":"Prueba de alcance"}' \
  http://localhost:8080/api/v1/leave/requests/<id-pendiente>/reject
```
**Esperado**: `403 Forbidden`; también `403` en `GET /api/v1/leave/team-requests` con `empleado@empresa.com`.

### Escenario 6 — Notificación al empleado (HU-05.CA-4)
1. Iniciar sesión como `empleado@empresa.com`.
2. **Esperado**: la campana de la cabecera muestra el número de no leídas (texto accesible "N notificaciones sin leer").
3. Abrir la campana.
4. **Esperado**: una notificación "Solicitud aprobada" (con comentario si lo hubo) y otra "Solicitud rechazada" con el motivo. Al pulsar una no leída, el contador disminuye.

### Escenario 7 — Saldo insuficiente al aprobar (HU-05.CA-5)
1. Con dos solicitudes de vacaciones pendientes cuya suma supera el saldo disponible, aprobar la primera y después la segunda.
2. **Esperado**: la segunda devuelve `422` "Saldo insuficiente de vacaciones" y permanece en la bandeja.

### Escenario 8 — Vacaciones que cruzan años (HU-05.CA-5)
1. Solicitud pendiente del 28/12 al 08/01 (año actual → siguiente). Preparar por *fixture* un empleado cuyo saldo disponible en el año siguiente sea menor que los días que le tocan (el año siguiente usa el convenio completo por defecto; hay que consumirlo con otras aprobadas, o usar un empleado incorporado ese año).
2. Aprobar.
3. **Esperado**: `422` con `year` = año con saldo insuficiente; la solicitud sigue `PENDING`.
4. Con saldo suficiente en ambos años: `200`. Después, en "Mi Ficha y Saldo" (HU-02) los días «disfrutados» de **cada** año incluyen la parte que le corresponde (reparto: años previos con días laborables de hoy, el último año con el resto; suma = `calculatedDays`).
5. Si cambian los festivos y la suma por año no coincide con `calculatedDays`, el reparto sigue sumando `calculatedDays` (el último año absorbe la diferencia).

### Escenario 9 — Año cerrado (HU-05.CA-5)
1. Solicitud pendiente con fechas en un año anterior al actual (preparada como *fixture*).
2. Aprobar → `422`, sigue `PENDING`. Rechazarla con motivo → `200`.

### Escenario 10 — Solicitud propia del responsable (HU-05.CA-1, Art. 10)
1. Como `responsable@empresa.com`, crear una solicitud propia (HU-03).
2. **Esperado**: no aparece en su bandeja; `POST .../approve` o `.../reject` sobre ella devuelve `403` y queda un `WARN` de auditoría.
3. El superior del responsable en el *seed* es `rrhh@empresa.com`, con rol `RRHH`, que es resolutor válido (FR-018): el responsable sí puede crear su solicitud (`201`) y RRHH puede resolver las de su equipo, pero esta propia solicitud sigue sin aparecer en la bandeja de `responsable@` y `POST .../approve` sobre ella devuelve `403`.
4. Un responsable sin superior válido no puede crear solicitudes (FR-015, escenario 12).

### Escenario 11 — Solicitud retirada (HU-05.CA-3)
1. Con la bandeja abierta, el empleado cancela la solicitud (HU-04).
2. Aprobar desde la bandeja → `404`; la bandeja se recarga y la fila desaparece.

### Escenario 12 — Sin resolutor válido (FR-015)
1. Con un empleado sin superior preparado por *fixture* (en el seed solo `direccion@empresa.com` carece de él): «Solicitar Ausencia» muestra «no tienes un responsable asignado». `POST /leave/requests` → `422`.
2. Repetir con un empleado cuyo superior está dado de baja o no tiene rol de responsable → `422`.

### Escenario 13 — Superior que deja de ser válido (FR-016)
1. Con una solicitud `PENDING` de `empleado@empresa.com`, RRHH da de baja a `responsable@empresa.com` (HU-01).
2. La solicitud pasa a `CANCELLED` (no se borra), el empleado recibe una única notificación (una por empleado, aunque tuviera varias pendientes) con el motivo y ya no puede crear otra hasta que se le asigne un superior válido.
3. Las variaciones de contrato no están soportadas hoy: no hay otro disparador.

### Escenario 14 — Dirección General (FR-017)
1. Como `direccion@empresa.com` (su único subordinado directo es `rrhh@empresa.com`): el menú oculta «Solicitar Ausencia» y «Mis Solicitudes»; por URL, «Dirección General no solicita ausencias»; `POST /leave/requests` → `422`.
2. Ve en la bandeja y el calendario sólo las solicitudes de `rrhh@empresa.com` (su subordinado directo; para tener solicitudes que ver, `rrhh@` debe crear una); puede aprobarlas o rechazarlas. Una solicitud ajena → `403`.
3. Aprobar una solicitud de `VACATION` funciona (la revalidación de saldo no da `403`).
4. «Mi Ficha» (sin «y Saldo» en el menú) muestra la ficha sin el apartado de saldo; `GET /leave/balances/me` y `GET /leave/balances/employee/{su id}` → `403` (FR-017, 2026-11-19).

### Escenario 15 — RRHH como resolutor (FR-018)
1. `rrhh@empresa.com` tiene a `responsable@empresa.com` como subordinado directo: el menú le muestra «Solicitudes del Equipo» y «Calendario del Equipo», además de «Alta de Empleado» e «Informes».
2. `responsable@empresa.com` ya puede crear una solicitud (su superior es resolutor válido; `eligibility` → `canRequest: true`).
3. Como `rrhh@empresa.com`, la solicitud aparece en la bandeja; aprobarla y rechazarla con motivo funciona y notifica a `responsable@`. El calendario muestra solo a `responsable@`.
4. Una solicitud de `empleado@empresa.com` (equipo de `responsable@`) no aparece a RRHH y `POST .../approve` → `403` con registro de acceso denegado.
5. Dar de baja a `rrhh@empresa.com` cancela las pendientes de `responsable@` (FR-016) con una notificación.

---

## 3. Tests Automatizados

```bash
cd backend && ./mvnw test -Dtest="LeaveResolutionServiceTest,LeaveResolutionIntegrationTest,NotificationIntegrationTest"
cd frontend && npx ng test --watch=false
```

| CA | Cobertura esperada |
|---|---|
| HU-05.CA-1 | Bandeja solo con `PENDING` del equipo; `403` para otro equipo y para rol `EMPLEADO`; `403` al resolver una solicitud propia (también con `manager_id` autorreferenciado) y ausente de su bandeja; tras cambio de responsable, solo resuelve el nuevo. |
| HU-05.CA-2 | Aprobación con/sin comentario; rechazo con motivo; `400` sin motivo, < 5 o > 500 caracteres; comentario > 500 → `400`; `CHECK` de BD rechaza > 500. |
| HU-05.CA-3 | `409` al resolver una solicitud `APPROVED`/`REJECTED` y en resolución simultánea de la misma solicitud; `404` si el empleado la retiró; *trigger* de BD impide modificar su estado o motivo. |
| Regresión | Suites de HU-03/HU-04 siguen en verde con `V3` aplicada. |
| HU-05.CA-4 | Notificación creada en la resolución; listado, contador y marcado propios; `403` sobre notificación ajena; *rollback* conjunto si falla la notificación. |
| HU-05.CA-5 | Saldo derivado refleja la aprobación (también el de **ambos** años en una solicitud que cruza años, con la regla de reparto); saldo insuficiente → `422`; cruce de años validado año a año; año cerrado → `422`; aprobaciones concurrentes no dejan saldo negativo. |
| FR-018 / FR-017 (2026-11-19) | `LeaveResolverPolicy` acepta `RRHH`; `RRHH` lista, aprueba, rechaza y ve el calendario solo de sus subordinados directos, `403` fuera de su equipo y en solicitudes propias (`HU-05.CA-1`); creación con superior `RRHH` → `201` y `canRequest=true`; baja de un superior `RRHH` cancela pendientes (FR-016); `DIRECCION_GENERAL` → `403` en `/balances/me` y `/balances/employee/{propio id}`, `RRHH` conserva acceso total; frontend: `canResolveRequests`, menú «Mi Ficha» y sin widget de saldo para Dirección General. |
| FR-015 / FR-016 / FR-017 (2026-11-18) | `LeaveResolverPolicy` (casos: sin superior, inactivo, sin rol, autorreferencia, `DIRECCION_GENERAL`); `422` en creación; cancelación automática y una notificación por empleado al dar de baja al superior; `DIRECCION_GENERAL` resuelve (con saldo) y ve el calendario, `403` fuera de su equipo; `V7` aplicada; frontend: menú oculto, guard y mensaje de elegibilidad. |
