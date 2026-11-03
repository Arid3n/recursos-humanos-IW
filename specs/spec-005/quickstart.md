# Quickstart: spec-005 — Aprobación o Rechazo de Solicitudes

**Feature**: `spec-005` (HU-05)
**Date**: 2026-11-03
**Author**: Integrante 3

---

## 1. Requisitos Previos

- PostgreSQL activo: `docker compose up -d`
- Backend: `cd backend && ./mvnw spring-boot:run` (aplica `V3__leave_resolution_constraints.sql`)
- Frontend: `cd frontend && npm start` → `http://localhost:4200`
- Usuarios de desarrollo (contraseña `admin123`, ver `research.md` §1.9):
  - `responsable@empresa.com` — `RESPONSABLE_EQUIPO`, responsable de `empleado@empresa.com`
  - `empleado@empresa.com` — `EMPLEADO`
  - `rrhh@empresa.com` — `RRHH` (no pertenece al equipo; útil para pruebas negativas)
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

### Escenario 3 — Rechazo con motivo (HU-05.CA-2)
1. Pulsar "Rechazar" y dejar el motivo vacío o con menos de 5 caracteres.
2. **Esperado**: el botón de confirmar no envía y se muestra el error junto al campo.
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
2. **Esperado**: la segunda devuelve `409` "Saldo insuficiente de vacaciones" y permanece en la bandeja.

---

## 3. Tests Automatizados

```bash
cd backend && ./mvnw test -Dtest="LeaveResolutionServiceTest,LeaveResolutionIntegrationTest,NotificationIntegrationTest"
cd frontend && npx ng test --watch=false
```

| CA | Cobertura esperada |
|---|---|
| HU-05.CA-1 | Bandeja solo con `PENDING` del equipo; `403` para otro equipo y para rol `EMPLEADO`. |
| HU-05.CA-2 | Aprobación con/sin comentario; rechazo con motivo; `400` sin motivo o < 5 caracteres. |
| HU-05.CA-3 | `409` al resolver una solicitud `APPROVED`/`REJECTED`; *trigger* de BD impide modificarla. |
| HU-05.CA-4 | Notificación creada en la resolución; listado, contador y marcado propios; `403` sobre notificación ajena; *rollback* conjunto si falla la notificación. |
| HU-05.CA-5 | Saldo derivado refleja la aprobación; saldo insuficiente → `409`; aprobaciones concurrentes no dejan saldo negativo. |
