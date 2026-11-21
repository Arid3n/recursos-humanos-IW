# Specification Analysis Report: spec-007 — Informes de Absentismo y Uso de Vacaciones

**Feature**: `spec-007` · HU-07  
**Fecha de análisis**: 2026-11-20  
**Estado**: Completado (implementación verificada contra código real)

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [data-model.md](data-model.md)
- [contracts/reports-api.yaml](contracts/reports-api.yaml)
- Código implementado: `backend/src/main/java/com/iw/rrhh/report/`

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **LOW** | `spec.md FR-003` vs implementación | La spec menciona agrupación «mediante agregación SQL eficiente (Art. 8.6)». La implementación usa `JOIN LATERAL` nativo + cálculo en JVM para solicitudes que cruzan el límite del rango; es eficiente pero no es agregación pura en BD. | Comportamiento correcto. La nota se mantiene como intención; no afecta al resultado. |
| **U1** | Underspecification | **LOW** | `spec.md FR-006` | La spec dice «retornar una respuesta semántica» pero no especificaba si el endpoint de exportación devuelve `204` o un CSV vacío. | **RESUELTO en implementación**: exportación devuelve `204 No Content` cuando `hasData: false`; documentado en contrato. |
| **C1** | Coverage Gap | **MEDIUM** | `tasks.md` | No había tarea explícita para el test de paridad pantalla ↔ CSV (SC-002 y FR-004). | **CUBIERTO**: `DepartmentReportIntegrationTest` llama al mismo endpoint con MockMvc y compara JSON vs filas CSV. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Acceso exclusivo RRHH) | Sí | T008, T010 | `@PreAuthorize("hasRole('RRHH')")` en clase controladora; test negativo en `DepartmentReportIntegrationTest`. |
| **FR-002** (Filtro por tipo y rango) | Sí | T006, T007, T008 | Parámetros `type`, `startDate`, `endDate` en ambos endpoints. |
| **FR-003** (Agrupación por departamento con agregación eficiente) | Sí | T006, T007 | `JOIN LATERAL` nativo resuelve contrato vigente; agregación SQL en `summarizeDepartments`. |
| **FR-004** (Paridad pantalla ↔ CSV) | Sí | T008, T010 | Ambos endpoints llaman a `generateReport()`; `exportCsv` recibe el mismo `DepartmentReportResponse`. |
| **FR-005** (CSV RFC 4180 UTF-8 BOM) | Sí | T007, T010 | BOM `\uFEFF` antepuesto; entrecomillado RF 4180; `Content-Type: text/csv; charset=UTF-8`. |
| **FR-006** (Sin datos → respuesta semántica) | Sí | T007, T010 | `hasData: false` en JSON; `204` en exportación. |
| **FR-007** (Desglose individual por empleado) | Sí | T007, T012 | `EmployeeReportRow` en cada `DepartmentReportRow`; frontend expande departamentos. |
| **SC-001** (< 1.5 s para toda la plantilla) | Sí | T006 | Consultas nativas con índices existentes; datos de demo de tamaño representativo en test. |
| **SC-002** (100% paridad pantalla ↔ CSV) | Sí | T010 | Test automatizado valida que totales del JSON coinciden con filas CSV. |
| **SC-003** (0% fugas de datos) | Sí | T008, T010 | Tests negativos con roles `EMPLEADO` y `RESPONSABLE_EQUIPO` → 403. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*

- **Art. 5 (Capas desacopladas)**: Respetado — Controller → Service → Repository → PostgreSQL.
- **Art. 10 (Seguridad en servidor)**: Respetado — `@PreAuthorize` a nivel de clase; sin filtrado en cliente.
- **Art. 14 (Regla de días única)**: Respetado — `HolidayCalendarService.countWorkingDays` sin duplicación.
- **Art. 17 (Exportación)**: Respetado — RFC 4180 con BOM UTF-8; paridad garantizada por diseño.
- **Art. 22 (Test-First y Testcontainers)**: Respetado — integración contra PostgreSQL real.
- **Art. 23 (Simplicidad)**: Respetado — sin dependencias CSV externas.

---

## 4. Unmapped Tasks

*Ninguna.* Todas las tareas mapean a requisitos funcionales, criterios de éxito o setup/pulido del módulo.

---

## 5. Métricas

- **Total Requirements (FR + SC):** 10
- **Total Tasks:** 18
- **Coverage %:** **100%** (10/10 requisitos con al menos 1 tarea asociada)
- **Ambiguity Count:** 0
- **Critical Issues Count:** 0
- **High Issues Count:** 0

---

## 6. Próximas Acciones Recomendadas

1. *(Opcional)* Añadir mención explícita de `204 No Content` en `spec.md FR-006` para alinearlo con el contrato.
2. *(Opcional)* Documentar en `research.md` el tiempo de respuesta medido en test de integración para contrastar con SC-001.
