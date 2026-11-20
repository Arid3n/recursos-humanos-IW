# Research & Technical Decisions: spec-007 — Informes de Absentismo y Uso de Vacaciones

**Feature**: `spec-007` · HU-07  
**Date**: 2026-10-27  
**Asignación**: Integrante 4 (Analítica de RRHH y Desempeño)

---

## Decisión 1: Repositorio dedicado con consultas nativas para la capa de informes

### Context & Problem
El módulo de informes necesita calcular métricas de absentismo y uso de vacaciones agregadas por departamento para rangos de fechas arbitrarios. Usar `LeaveRequestRepository` existente habría mezclado responsabilidades del módulo de solicitudes con la lógica de agregación analítica.

### Decision
Se crea un repositorio dedicado `DepartmentReportRepository extends Repository<Employee, UUID>` (sin `JpaRepository` para no exponer mutación) con cuatro métodos de consulta:

1. **`findEmployeesForPeriod(endDate)`** — consulta nativa con `JOIN LATERAL` para resolver el contrato vigente a `endDate` y obtener departamento y días de vacaciones del convenio colectivo.
2. **`summarizeDepartments(leaveType, periodStart, periodEnd, endDate)`** — agrega `leave_requests` aprobadas completamente dentro del rango por departamento.
3. **`sumApprovedDaysWithinPeriod(status, leaveType, periodStart, periodEnd)`** — suma los `calculated_days` de solicitudes que caen íntegramente en el rango.
4. **`findApprovedRequestsCrossingPeriodBoundary(status, leaveType, periodStart, periodEnd)`** — recupera solicitudes que cruzan los límites del rango para recortar los días con `HolidayCalendarService.countWorkingDays`.

### Alternatives Considered
- **Reutilizar `LeaveRequestRepository`**: descartado; contamina el contrato del módulo de solicitudes con lógica de agregación de RRHH.
- **Spring Data Specifications / Criteria API**: descartado; la complejidad del `JOIN LATERAL` nativo y las agregaciones SQL no se expresan limpiamente con la API de criterios (Art. 23 Simplicidad).

---

## Decisión 2: Cálculo de días en servicio — separación por tipo de informe

### Context & Problem
El denominador del porcentaje es diferente para absentismo (días laborables teóricos) y para uso de vacaciones (asignación anual prorrateada por año de alta).

### Decision
En `DepartmentReportServiceImpl`, el método interno `EmployeeTotals.toRow()` ramifica el cálculo del campo `entitledDays`:

- **`ABSENTEEISM`**: `countWorkingDays(max(hireDate, startDate), endDate)` usando `HolidayCalendarService`. Empleados dados de alta después del inicio del periodo se prorratean automáticamente.
- **`VACATION_USAGE`**: suma anual ponderada. Para cada año del rango, si el empleado fue dado de alta ese año se aplica la regla mensual `vacationDaysPerYear * remainingMonths / 12`; en años completos se toma `vacationDaysPerYear` del convenio colectivo. Se itera año a año desde `startYear` hasta `endYear`.

**División por cero**: si el denominador es 0, el porcentaje devuelve `BigDecimal.ZERO.setScale(2)` sin excepción aritmética (edge case de la spec).

### Alternatives Considered
- **Calcular en SQL**: descartado; el prorrateado mensual requiere iterar años en la JVM con la lógica de convenios ya disponible.

---

## Decisión 3: Paridad exacta pantalla ↔ CSV (FR-004)

### Context & Problem
La spec exige que los datos en pantalla y el archivo CSV exportado sean matemáticamente idénticos (FR-004, SC-002).

### Decision
El controlador `DepartmentReportController` llama a `reportService.generateReport(type, startDate, endDate)` **en ambos endpoints** (`GET /reports` y `GET /reports/export`), obteniendo la misma `DepartmentReportResponse`. El endpoint de exportación pasa ese objeto a `reportService.exportCsv(report)` para serializar en CSV. La capa de servicio es una sola fuente de verdad; no hay ruta alternativa de datos para la exportación.

### Alternatives Considered
- **Consulta directa a la BD para el CSV**: descartado; introduciría el riesgo de divergencia y viola FR-004.

---

## Decisión 4: Formato CSV — RFC 4180 con BOM UTF-8

### Context & Problem
Los caracteres españoles (tildes, ñ) deben abrirse correctamente en Microsoft Excel sin configuración manual.

### Decision
El método `exportCsv` construye un `StringBuilder` con:
- BOM `\uFEFF` al inicio del `byte[]` resultante (codificado en UTF-8).
- Cabecera con columnas: `Tipo`, `Periodo`, `Departamento`, `Nivel`, `Nombre`, `Empleados`, `Días Correspondientes`, `Días Disfrutados`, `Días Restantes`, `Porcentaje`.
- Filas departamentales con `Nivel = "Departamento"` y filas individuales con `Nivel = "Empleado"`.
- Entrecomillado RFC 4180: cada campo entre comillas dobles; las comillas internas se duplican.
- Separador de comas; terminador `\r\n`.
- `Content-Type: text/csv; charset=UTF-8` y cabecera `Content-Disposition: attachment; filename=...`.

### Alternatives Considered
- **OpenCSV / Apache Commons CSV**: descartado por Art. 23.4 (dependencia nueva sin necesidad real dado el formato sencillo).

---

## Decisión 5: Autorización y control de acceso

### Decision
`@PreAuthorize("hasRole('RRHH')")` a nivel de clase en `DepartmentReportController`, cubriendo tanto el endpoint de consulta como el de exportación (FR-001, SC-003). Cualquier intento de acceso con rol diferente retorna `403 Forbidden`.

---

## Cobertura de la Constitución

| Artículo | Requisito | Enfoque spec-007 |
|---|---|---|
| Art. 1 | Spec como verdad | Todas las decisiones derivan de HU-07 y sus clarificaciones. |
| Art. 5 / 7 | Capas desacopladas | `DepartmentReportController` → `DepartmentReportService` → `DepartmentReportRepository` → PostgreSQL. |
| Art. 8 | PostgreSQL real | Consultas nativas validadas contra PostgreSQL con Testcontainers. |
| Art. 10 | Autorización en servidor | `@PreAuthorize("hasRole('RRHH')")` en la clase controladora. |
| Art. 14 | Regla de días única | `HolidayCalendarService.countWorkingDays` sin duplicar lógica de festivos. |
| Art. 17 | Exportación | CSV RFC 4180 con BOM UTF-8; paridad pantalla ↔ archivo. |
| Art. 22 | Test-First con Testcontainers | `DepartmentReportIntegrationTest` contra PostgreSQL real. |
| Art. 23 | Simplicidad | Sin dependencias CSV externas; un servicio, dos endpoints. |
