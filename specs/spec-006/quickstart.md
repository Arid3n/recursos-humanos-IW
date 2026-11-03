# Quickstart: spec-006 — Calendario de Ausencias del Equipo

**Feature**: `spec-006` (HU-06)
**Date**: 2026-11-03
**Author**: Integrante 3

---

## 1. Requisitos Previos

- PostgreSQL activo: `docker compose up -d` (requiere el plugin `docker-compose`)
- Backend: `cd backend && sh ./mvnw spring-boot:run`
- Frontend: `cd frontend && npm start` → `http://localhost:4200`
- Usuarios de desarrollo (contraseña `admin123`): `responsable@empresa.com` (responsable de `empleado@empresa.com`), `empleado@empresa.com`, `rrhh@empresa.com`
- Contrato: [contracts/team-calendar-api.yaml](contracts/team-calendar-api.yaml) · Modelo: [data-model.md](data-model.md)

**Preparación**:
1. Como `rrhh@empresa.com`, dar de alta un segundo empleado con responsable "Carlos Ruiz Navarro" (HU-01) y otro empleado sin ese responsable.
2. Con cada empleado, crear solicitudes (HU-03) en el mismo mes: Laura y el segundo empleado con al menos un día laborable en común; el empleado ajeno en esas mismas fechas.
3. Como `responsable@empresa.com`, aprobar una de las dos solicitudes del equipo (HU-05) y rechazar alguna otra solicitud del equipo de otro día.

---

## 2. Escenarios de Validación

### Escenario 1 — Vista mensual y navegación (HU-06.CA-1)
1. Iniciar sesión como `responsable@empresa.com` → "Calendario del Equipo" (`/team/calendar`).
2. **Esperado**: cuadrícula del mes actual (lunes a domingo) con título "mes año" en español.
3. Pulsar "Mes siguiente" hasta el mes preparado.
4. **Esperado**: cada ausencia aparece en sus días laborables con nombre y tipo; la página no se recarga.
5. Pulsar una ausencia.
6. **Esperado**: diálogo con nombre, tipo, rango completo de fechas y estado.

### Escenario 2 — Aprobadas frente a pendientes (HU-06.CA-2)
1. En el mes preparado, observar ambas ausencias del equipo.
2. **Esperado**: la aprobada muestra icono y texto "Aprobada"; la pendiente icono y texto "Pendiente" con borde discontinuo. La leyenda explica ambos. La rechazada no aparece.
3. Activar escala de grises en el navegador/sistema.
4. **Esperado**: el estado sigue identificándose por icono y texto.

### Escenario 3 — Coincidencias (HU-06.CA-3)
1. Localizar el día laborable en común.
2. **Esperado**: borde destacado, icono de advertencia y texto "2 ausencias coincidentes". Los días con una sola persona no se resaltan; fines de semana y festivos aparecen atenuados (festivos con su nombre) y sin ausencias.

### Escenario 4 — Solo el equipo propio (HU-06.CA-4)
1. Comprobar que el empleado ajeno no aparece ni suma en las coincidencias.
2. Llamada directa con un usuario sin rol de responsable:
   ```bash
   curl -i -u empleado@empresa.com:admin123 "http://localhost:8080/api/v1/leave/team-calendar?month=2026-12"
   ```
   **Esperado**: `403 Forbidden`.
3. Mes inválido:
   ```bash
   curl -i -u responsable@empresa.com:admin123 "http://localhost:8080/api/v1/leave/team-calendar?month=2026-13"
   ```
   **Esperado**: `400 Bad Request` con ProblemDetail.

### Escenario 5 — Filtro por empleado (US2)
1. Seleccionar a Laura en el filtro.
2. **Esperado**: solo se muestran sus ausencias; el resalte de coincidencia del día común se mantiene (cobertura del equipo).
3. Seleccionar "Todo el equipo".
4. **Esperado**: reaparecen todas las ausencias. El filtro no contiene al empleado ajeno.

---

## 3. Tests Automatizados

```bash
cd backend && sh ./mvnw test -Dtest="HolidayCalendarServiceTest,TeamCalendarServiceTest,TeamCalendarIntegrationTest" -DargLine="-Dapi.version=1.44"
cd frontend && npx ng test --watch=false
```

(`-DargLine="-Dapi.version=1.44"` solo es necesario con Docker ≥ 29 y Testcontainers 1.20.x; ver informe de implementación de spec-005.)

| CA | Cobertura esperada |
|---|---|
| HU-06.CA-1 | Días del mes completos y ordenados; eventos solo en días laborables dentro del rango; ausencia que cruza el cambio de mes; navegación pide el mes correcto; detalle con rango completo. |
| HU-06.CA-2 | `status` `PENDING`/`APPROVED` en eventos; `REJECTED` excluida; UI con icono + texto + leyenda. |
| HU-06.CA-3 | `hasConflict` y `absentCount` con ≥ 2 empleados distintos; 1 ausencia no es coincidencia; fines de semana/festivos sin recuento; filtro mantiene el resalte. |
| HU-06.CA-4 | Otro equipo excluido de eventos, recuento y miembros; empleado inactivo excluido; `403` con rol `EMPLEADO`; `400` con mes inválido. |
