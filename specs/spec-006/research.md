# Research: spec-006 — Calendario de Ausencias del Equipo

**Feature**: `spec-006` (HU-06)
**Date**: 2026-11-03
**Author**: Integrante 3

---

## 1. Decisiones Técnicas

### 1.1 Forma del endpoint

- **Decisión**: `GET /api/v1/leave/team-calendar?month=YYYY-MM` (parámetro obligatorio, enlazado como `java.time.YearMonth`). Responde `TeamCalendarResponse` con el mes, los miembros del equipo y un elemento por cada día del mes.
- **Justificación**:
  - El mes es la unidad de consulta que define el Art. 16.4; recibir un `YearMonth` evita rangos arbitrarios (que permitirían cargar el histórico) y la validación `startDate <= endDate`.
  - Spring convierte `YearMonth` en formato ISO; un valor inválido produce `MethodArgumentTypeMismatchException`, que `ResponseEntityExceptionHandler` traduce a `400` ProblemDetail.
  - Devolver un elemento por día (y no solo eventos) permite que el servidor entregue tipo de día, festivo, recuento y coincidencia ya calculados (Art. 16.3); el frontend solo pinta.
- **Alternativas consideradas**:
  - `?startDate&endDate` (como sugería la versión inicial de la spec): descartado por permitir rangos ilimitados.
  - Solo lista de ausencias + cálculo de solapes en cliente: descartado, viola el Art. 16.3.

### 1.2 Equipo y alcance (Art. 10)

- **Decisión**: Igual que HU-05: el responsable se obtiene de la sesión y el equipo son los empleados **activos** con `manager_id` = responsable. Rol: `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`. El cliente no envía ningún identificador de empleado, así que no existe forma de pedir datos de otro equipo.
- **Justificación**: Reutiliza `EmployeeRepository.findByManagerIdAndActiveTrue` (existente). Filtrar también por `active` en la consulta de ausencias mantiene coherentes calendario y filtro.

### 1.3 Consulta de ausencias

- **Decisión**: Nuevo método en `LeaveRequestRepository`:
  ```text
  SELECT lr FROM LeaveRequest lr JOIN FETCH lr.employee e
  WHERE e.manager.id = :managerId AND e.active = true
    AND lr.status IN :statuses
    AND lr.startDate <= :rangeEnd AND lr.endDate >= :rangeStart
  ORDER BY e.lastName, e.firstName, lr.startDate
  ```
  con `statuses = [PENDING, APPROVED]` y el rango = primer/último día del mes.
- **Justificación**: Condición estándar de solape de intervalos; recoge ausencias que cruzan el cambio de mes. Usa `idx_leave_calendar`. `JOIN FETCH` evita N+1 al leer el nombre.

### 1.4 Cálculo por día y coincidencias (Art. 16.3)

- **Decisión**: En `TeamCalendarServiceImpl`, para cada fecha del mes:
  - `dayType`: `WEEKEND` si `isWeekend`, `HOLIDAY` si `isHoliday` (con `holidayName`), si no `WORKING`.
  - Solo en días `WORKING`: `events` = ausencias cuyo rango contiene la fecha.
  - `absentCount` = número de **empleados distintos** con evento ese día; `hasConflict = absentCount >= 2`.
  - Días no laborables: `events` vacío, `absentCount = 0`, `hasConflict = false`.
- **Justificación**: Una ausencia solo consume días laborables (mismo criterio que el cómputo de HU-03); contar empleados distintos evita doble conteo si alguien tuviera dos solicitudes el mismo día. Las pendientes cuentan (spec, Assumptions). Coste O(días × ausencias), despreciable para un mes y un equipo.

### 1.5 Nombre de los festivos

- **Decisión**: Ampliar `HolidayCalendarService` con `Optional<String> getHolidayName(LocalDate)`. Los festivos fijos pasan de `Set<MonthDay>` a `Map<MonthDay, String>` con los nombres que hoy están como comentarios, y `Jueves Santo`/`Viernes Santo` para los móviles. `isHoliday(date)` se implementa como `getHolidayName(date).isPresent()`.
- **Justificación**: El Art. 14.3 exige una única regla de calendario; se reutiliza el mismo componente que HU-03 sin duplicar la lista de festivos. El comportamiento de `isHoliday` no cambia (los tests existentes de `HolidayCalendarServiceTest` lo verifican).
- **Alternativas consideradas**: Lista de festivos propia del calendario: descartado (dos fuentes que pueden divergir).

### 1.6 Filtro por empleado

- **Decisión**: El filtro se aplica en el frontend sobre la respuesta del mes (los miembros llegan en `members`). Las coincidencias (`absentCount`, `hasConflict`) se muestran siempre tal como las calcula el servidor para todo el equipo.
- **Justificación**: Coherente con la spec (la cobertura es del equipo) y evita una segunda petición por cada cambio de filtro. No es lógica de negocio: solo oculta eventos ya autorizados.
- **Alternativas consideradas**: Parámetro `employeeId` en el endpoint: añadiría una comprobación de alcance extra y una ruta para enumerar identificadores, sin beneficio.

### 1.7 Frontend

- **Componente** `TeamCalendarComponent` (ruta `/team/calendar`, enlace "Calendario del Equipo" visible con `isResponsable()`):
  - Mes inicial: mes actual del navegador; botones "Mes anterior"/"Mes siguiente" con `aria-label`; título con mes y año en español.
  - Cuadrícula como `<table>` semántica (cabecera Lun…Dom, una fila por semana); las celdas vacías previas al día 1 se calculan solo para maquetar.
  - Ausencia = `button` con nombre, icono de tipo y distintivo de estado: aprobada → icono `check_circle` + texto "Aprobada" con fondo sólido; pendiente → icono `schedule` + texto "Pendiente" con borde discontinuo. Leyenda visible con ambos.
  - Coincidencia: clase con borde destacado + icono `warning` + texto "N ausencias coincidentes".
  - Fin de semana/festivo: celda atenuada; festivo con su nombre.
  - Filtro `mat-select` con "Todo el equipo" + `members`.
  - Detalle: `AbsenceDetailDialogComponent` (`MatDialog`) con nombre, tipo, rango completo y estado.
  - Estados de carga, error y "No hay ausencias en este mes".
- **Locale**: La aplicación no registra `LOCALE_ID` (el `DatePipe` usa `en-US`; las pantallas existentes solo usan formatos numéricos). El título del mes se formatea con `Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' })`, sin cambiar la configuración global de la aplicación (que afectaría a HU-01…HU-05).
- **Alternativas consideradas**: Librerías de calendario (FullCalendar, angular-calendar): descartadas por el Art. 23.4 (dependencia nueva sin necesidad).

---

## 2. Cobertura de la Constitución

| Artículo | Requisito | Enfoque spec-006 |
|---|---|---|
| Art. 2 | Trazabilidad | Tests `HU-06.CA-1…4`. |
| Art. 10 | Rol + alcance, tests negativos | `hasRole('RESPONSABLE_EQUIPO')`; equipo derivado de la sesión; tests con otro equipo y rol `EMPLEADO`. |
| Art. 14 | Regla de días única | `HolidayCalendarService` ampliado, no duplicado. |
| Art. 16 | Calendario | Solo equipo, estados distinguibles, solapes en servidor, consulta mensual. |
| Art. 21 | Accesibilidad | Icono + texto + leyenda; botones con teclado. |
| Art. 23 | Simplicidad | Sin dependencias nuevas; un endpoint. |
