# Feature Specification: spec-007 — Informes de Absentismo y Uso de Vacaciones

**Feature Branch**: `spec-007`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-07  
**Asignación**: Integrante 4 (Analítica de RRHH y Desempeño)

## Clarifications

### Session 2026-10-27
- Q: ¿Cuál es la fórmula exacta para el cálculo del uso de vacaciones y del índice de absentismo? → A:
  - *Uso de vacaciones:* `(Total días vacaciones disfrutados / Total días correspondientes) * 100`.
  - *Índice de absentismo:* `(Total días de permisos o ausencias no vacacionales / Días laborables teóricos del periodo de la plantilla) * 100`.
- Q: ¿Qué especificación técnica rige la exportación a CSV? → A: Archivo CSV estándar RFC 4180 con codificación UTF-8 con BOM (para apertura correcta de tildes y caracteres en Excel), delimitador de comas y cabeceras exactas coincidentes con la vista en pantalla (Art. 17).
- Q: ¿Qué opciones de periodo de consulta se ofrecen a RRHH? → A: Selector con opciones rápidas (Año en curso, Año anterior, Trimestre actual) y selector de rango de fechas personalizado (`startDate` y `endDate`).
- Q: ¿Permite el informe ver el desglose individual además del agregado departamental? → A: Sí; la tabla presenta los totales por departamento y permite desplegar el listado de empleados que componen los totales del departamento.

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como miembro de Recursos Humanos (RRHH), quiero generar informes agregados sobre el índice de absentismo y el porcentaje de uso de vacaciones por departamento y rango temporal, y exportarlos en formato tabular (CSV descargable), para disponer de métricas fiables que respalden decisiones organizativas basadas en datos y auditorías laborales.

**Why this priority**: Es la herramienta directiva de RRHH para auditar el cumplimiento del descanso reglamentario de la plantilla y prever costes o desviaciones por absentismo a nivel empresarial.

**Independent Test**: Puede probarse seleccionando un departamento y un rango de fechas con ausencias registradas, verificando que los totales agregados en pantalla coinciden exactamente con la suma individual de ausencias aprobadas y que al pulsar "Exportar CSV" el archivo descargado contiene exactamente los mismos registros y totales que la vista web.

### Acceptance Scenarios

1. **Generación de informe agregado por departamento y periodo**:
   - **Given** un usuario autenticado con rol `RRHH` en el panel de informes,
   - **When** selecciona el tipo de informe ("Uso de vacaciones" o "Absentismo") y define un periodo,
   - **Then** el sistema presenta una tabla agrupada por departamento con: plantilla total, días totales correspondientes, días disfrutados, saldo remanente y porcentaje medio de uso o absentismo.

2. **Despliegue del desglose por empleado**:
   - **Given** el informe departamental en pantalla,
   - **When** RRHH pulsa en el botón desplegable de un departamento,
   - **Then** se visualizan las filas correspondientes a cada trabajador adscrito con su aportación a la métrica.

3. **Exportación tabular fidedigna (CSV)**:
   - **Given** un informe generado en pantalla con resultados agregados,
   - **When** el usuario pulsa en el botón "Exportar CSV",
   - **Then** se descarga un archivo CSV codificado en UTF-8 con BOM cuyos valores, columnas y totales coinciden con exactitud matemática con los datos presentados en la interfaz (Art. 17 de la Constitución).

4. **Manejo explícito de periodos sin datos**:
   - **Given** un rango de fechas o departamento en el que no existen ausencias ni registros,
   - **When** se solicita la generación del informe,
   - **Then** el sistema no muestra una tabla vacía ni errores técnicos, sino un mensaje claro y contextual: "No existen registros de ausencias para el departamento y periodo seleccionados".

5. **Control de acceso negativo**:
   - **Given** un usuario con rol `EMPLEADO` o `RESPONSABLE_EQUIPO`,
   - **When** intenta acceder a la URL del módulo de informes o a los endpoints de exportación de la API,
   - **Then** el servidor devuelve `403 Forbidden` bloqueando el acceso.

### Edge Cases
- **Caracteres especiales en nombres de departamentos:** La exportación a CSV soporta caracteres UTF-8 (`ñ`, tildes, comillas) respetando la norma RFC 4180.
- **División por cero en porcentajes:** En departamentos con 0 días teóricos o sin trabajadores, el porcentaje muestra `0.0%` sin errores aritméticos.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El acceso al módulo de informes y a sus endpoints DEBE estar restringido exclusivamente a usuarios con rol `RRHH` (Art. 10 y Art. 17).
- **FR-002**: El sistema DEBE permitir filtrar por tipo de informe (`ABSENTEEISM` o `VACATION_USAGE`) y por rango temporal (`startDate`, `endDate`).
- **FR-003**: Los resultados DEBEN agruparse a nivel de departamento y computarse en base de datos mediante agregación SQL eficiente (Art. 8.6).
- **FR-004**: La pantalla y la exportación DEBEN nutrirse de la misma capa de servicio/consulta para imposibilitar discrepancias entre la vista y el fichero generado.
- **FR-005**: El formato de exportación DEBE ser CSV estructurado (RFC 4180, codificación UTF-8 con BOM y separador de comas).
- **FR-006**: Si no se encuentran datos para el periodo consultado, el sistema DEBE retornar una respuesta semántica que la UI pinte como aviso informativo comprensible.
- **FR-007**: La interfaz DEBE permitir desplegar el desglose individual de los empleados de cada departamento.

### Key Entities
- **DepartmentReportSummary (DTO)**:
  - `department`: Nombre del departamento.
  - `headcount`: Número total de empleados adscritos.
  - `totalEntitledDays`: Suma de días correspondientes.
  - `totalTakenDays`: Suma de días disfrutados/ausencias.
  - `metricPercentage`: Porcentaje de uso o absentismo según tipo.
  - `periodLabel`: Etiqueta del periodo consultado.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: Generación y exportación de informes en menos de 1.5 segundos para datos de toda la plantilla.
- **SC-002**: 100% de paridad y coincidencia entre los datos visualizados en pantalla y las filas del archivo CSV exportado (validado con test automatizado obligatorio de Art. 17.6).
- **SC-003**: 0% de fugas de datos hacia empleados o responsables de equipo sin rol RRHH.

---

## 4. Assumptions
- El informe de absentismo contempla ausencias justificadas y permisos retribuidos o no retribuidos.
- Los cálculos se basan en registros de solicitudes en estado `Aprobada`.
