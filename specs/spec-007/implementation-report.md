# Informe de Implementación: spec-007 — Informes de Absentismo y Uso de Vacaciones

**Historia de Usuario**: HU-07  
**Rama**: `spec-007` (integrada en `main`)  
**Fecha de finalización**: 2026-11-20  
**Responsable**: José / jrcb3 (Integrante 4 — Analítica de RRHH y Desempeño)  
**Estado**: **Completado y Convergido (100%)**

---

## 1. Resumen Ejecutivo

Se ha completado satisfactoriamente la implementación de la especificación **`spec-007` (HU-07)**, que dota al departamento de Recursos Humanos de una potente herramienta analítica para monitorizar el absentismo y el porcentaje de disfrute de vacaciones de toda la organización.

El módulo procesa datos consolidados de empleados, departamentos y ausencias aprobadas, calcula indicadores clave de rendimiento (KPIs) agrupados por departamento y desglosados a nivel de empleado individual, y ofrece exportación tabular en formato CSV estándar RFC 4180 codificado en UTF-8 con BOM. La solución garantiza paridad matemática absoluta entre los datos mostrados en pantalla y los volcados en el archivo exportable (Art. 17 de la Constitución).

---

## 2. Cobertura de Requisitos y Criterios de Aceptación

### 2.1. Requisitos Funcionales

| Requisito | Descripción | Estado | Evidencia de Implementación |
| :--- | :--- | :---: | :--- |
| **FR-001** | Acceso exclusivo restringido a usuarios con rol `RRHH` | **CUMPLIDO** | `@PreAuthorize("hasRole('RRHH')")` en `DepartmentReportController`. |
| **FR-002** | Filtrado por tipo de informe (`VACATION_USAGE`, `ABSENTEEISM`) y periodo | **CUMPLIDO** | Parámetros `type`, `startDate`, `endDate` validados en backend. |
| **FR-003** | Agrupación departamental mediante agregación SQL eficiente | **CUMPLIDO** | `JOIN LATERAL` nativo para resolver contratos vigentes y sumar ausencias. |
| **FR-004** | Paridad fidedigna del 100% entre pantalla y archivo exportado | **CUMPLIDO** | Ambos canales consumen el mismo método de servicio `generateReport()`. |
| **FR-005** | Exportación CSV RFC 4180 en UTF-8 con BOM | **CUMPLIDO** | Anteposición de `\uFEFF`, entrecomillado estándar y cabecera HTTP `text/csv`. |
| **FR-006** | Respuesta semántica ante periodos o departamentos sin datos | **CUMPLIDO** | Flag `hasData: false` en JSON y respuesta HTTP `204 No Content` en exportación CSV. |
| **FR-007** | Desglose individual de empleados por departamento | **CUMPLIDO** | Colección `EmployeeReportRow` expandible en `DepartmentReportsComponent`. |

### 2.2. Criterios de Éxito (Success Criteria)

- **SC-001 (Generación en < 1.5 s para toda la plantilla)**: Consultas nativas de agregación con índices existentes en PostgreSQL.
- **SC-002 (100% de paridad pantalla ↔ CSV)**: Test automatizado en `DepartmentReportIntegrationTest` validando coincidencia fila a fila y total a total.
- **SC-003 (0% fugas de datos)**: Tests negativos que confirman que roles sin privilegios (`EMPLEADO`, `RESPONSABLE_EQUIPO`) reciben HTTP 403 Forbidden.

---

## 3. Arquitectura y Componentes Implementados

### 3.1. Backend (Spring Boot 3.4.3 + Java 21)
- **Controlador REST**: `com.iw.rrhh.report.controller.DepartmentReportController`
  - `GET /api/v1/reports/department`: Retorna reporte JSON con KPIs y desglose departamental.
  - `GET /api/v1/reports/department/export`: Genera y descarga el archivo CSV.
- **Servicio de Dominio**:
  - `DepartmentReportServiceImpl`: Cálculo matemático de tasas de absentismo y uso de vacaciones, generación de filas y serialización RFC 4180.
- **Configuración de Datos de Demostración**:
  - `DepartmentReportDemoDataInitializer`: Generador determinista de datos de prueba representativos para validación de analítica.

### 3.2. Frontend (Angular 21 + Material Design)
- **Componentes**:
  - `DepartmentReportsComponent`: Panel de control con selector de fechas, botones de exportación, tabla interactiva con filas de departamento colapsables/expandibles y badges de porcentajes.
- **Servicios**:
  - `DepartmentReportService`: Cliente HTTP para consulta JSON y descarga de blob CSV.

---

## 4. Verificación y Resultados de Pruebas

- **`DepartmentReportIntegrationTest`**: Suite de integración ejecutada con Spring Boot Test y MockMvc:
  - Valida generación de informe de vacaciones y absentismo.
  - Valida coincidencia exacta de valores numéricos entre JSON y CSV.
  - Valida bloqueo por autorización con HTTP 403.
  - Valida respuesta 204 ante periodos sin actividad.
- **`department-reports.component.spec.ts`**: Tests unitarios de componente frontend verificando interacción con filtros y expansión de acordeones.

---

## 5. Alineación con la Constitución del Proyecto

- **Art. 10 (Seguridad en servidor)**: `@PreAuthorize` aplicado a nivel de clase controladora; la interfaz solo refleja lo autorizado por el backend.
- **Art. 14 (Fechas en servidor)**: Conteo de días laborables apoyado en `HolidayCalendarService`.
- **Art. 17 (Integridad en exportaciones)**: Coincidencia matemática absoluta e incondicional entre UI y CSV.
- **Art. 21 (Accesibilidad)**: Tablas accesibles con `scope="col"`, `aria-expanded` y alertas semánticas.

---

## 6. Conclusiones y Estado de Convergencia

La funcionalidad `spec-007` se encuentra **100% convergida** con la especificación y el código. Proporciona a la dirección de RRHH visibilidad integral y métricas objetivas para la gestión del talento.
