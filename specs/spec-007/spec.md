# Feature Specification: spec-007 — Informes de Absentismo y Uso de Vacaciones

**Feature Branch**: `spec-007`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-07  
**Asignación**: Integrante 4 (Analítica de RRHH y Desempeño)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como miembro de Recursos Humanos (RRHH), quiero generar informes agregados sobre el índice de absentismo y el porcentaje de uso de vacaciones por departamento y rango temporal, y exportarlos en formato tabular (CSV descargable), para disponer de métricas fiables que respalden decisiones estratégicas de personal y auditorías laborales.

**Why this priority**: Es la herramienta directiva de RRHH para auditar el cumplimiento del descanso reglamentario de la plantilla y prever costes o desviaciones por absentismo a nivel empresarial.

**Independent Test**: Puede probarse seleccionando un departamento y un rango de fechas con ausencias registradas, verificando que los totales agregados en pantalla coinciden exactamente con la suma individual de ausencias aprobadas y que al pulsar "Exportar CSV" el archivo descargado contiene exactamente los mismos registros y totales que la vista web.

### Acceptance Scenarios

1. **Generación de informe agregado por departamento y periodo**:
   - **Given** un usuario autenticado con rol `RRHH` en el panel de informes,
   - **When** selecciona el tipo de informe ("Uso de vacaciones" o "Absentismo") y define un periodo (año o trimestre),
   - **Then** el sistema presenta una tabla agrupada por departamento con: total de empleados, días totales correspondientes, días disfrutados, saldo remanente y porcentaje medio de uso/absentismo.

2. **Exportación tabular fidedigna (CSV)**:
   - **Given** un informe generado en pantalla con resultados agregados,
   - **When** el usuario pulsa en el botón "Exportar CSV",
   - **Then** se descarga un archivo CSV codificado en UTF-8 cuyos valores, columnas y totales coinciden con exactitud matemática con los datos presentados en la interfaz (Art. 17 de la Constitución).

3. **Manejo explícito de periodos sin datos**:
   - **Given** un rango de fechas o departamento en el que no existen ausencias ni registros,
   - **When** se solicita la generación del informe,
   - **Then** el sistema no muestra una tabla vacía ni errores técnicos, sino un mensaje claro y contextual: "No existen registros de ausencias para el departamento y periodo seleccionados".

4. **Control de acceso negativo**:
   - **Given** un usuario con rol `EMPLEADO` o `RESPONSABLE_EQUIPO`,
   - **When** intenta acceder a la URL del módulo de informes o a los endpoints de exportación de la API,
   - **Then** el servidor devuelve `403 Forbidden` bloqueando el acceso.

### Edge Cases
- **Caracteres especiales en nombres de departamentos o personas:** La exportación a CSV debe utilizar codificación UTF-8 con soporte para tildes y caracteres especiales castellanos (`ñ`, vocales acentuadas) y entrecomillado estándar RFC 4180.
- **División por cero en porcentajes:** En departamentos recién creados con 0 ausencias o 0 días teóricos, el porcentaje de uso debe mostrar `0.0%` sin errores aritméticos.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El acceso al módulo de informes y a sus endpoints DEBE estar restringido exclusivamente a usuarios con rol `RRHH` (Art. 10 y Art. 17).
- **FR-002**: El sistema DEBE permitir filtrar por tipo de informe (`ABSENTEEISM` o `VACATION_USAGE`) y por rango temporal de fechas (`startDate`, `endDate`).
- **FR-003**: Los resultados DEBEN agruparse a nivel de departamento y computarse en base de datos mediante agregación SQL eficiente (Art. 8.6).
- **FR-004**: La pantalla y la exportación DEBEN nutrirse de la misma capa de servicio/consulta para imposibilitar discrepancias entre la vista y el fichero generado.
- **FR-005**: El formato de exportación DEBE ser CSV estructurado (con separador delimitador estándar y codificación UTF-8).
- **FR-006**: Si no se encuentran datos para el periodo consultado, el sistema DEBE retornar una respuesta semántica que la UI pinte como aviso informativo comprensible.

### Key Entities
- **DepartmentReportSummary (DTO)**:
  - `departmentId`: Identificador del departamento.
  - `departmentName`: Nombre del departamento.
  - `headcount`: Número total de empleados adscritos.
  - `totalEntitledDays`: Suma de días correspondientes según convenio.
  - `totalTakenDays`: Suma de días aprobados/disfrutados.
  - `usagePercentage`: Porcentaje de uso sobre el total disponible.
  - `periodLabel`: Etiqueta del periodo consultado (p. ej. "2026 - T1").

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: Generación y exportación de informes en menos de 2 segundos para conjuntos de datos de toda la compañía.
- **SC-002**: 100% de paridad y coincidencia entre los datos visualizados en pantalla y las filas del archivo CSV exportado (validado con test automatizado obligatorio de Art. 17.6).
- **SC-003**: 0% de fugas de datos hacia empleados o responsables de equipo sin rol RRHH.

---

## 4. Assumptions
- El informe de "Absentismo" computa tanto permisos especiales no retribuidos como bajas/ausencias justificadas según las tipologías recogidas en el sistema.
