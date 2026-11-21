# Informe de Implementación: spec-006 — Calendario de Ausencias del Equipo

**Historia de Usuario**: HU-06  
**Rama**: `spec-006` (integrada en `main`)  
**Fecha de finalización**: 2026-11-20  
**Responsable**: Andrei Radu (Integrante 3 — Gestión y Supervisión del Responsable)  
**Estado**: **Completado y Convergido (100%)**

---

## 1. Resumen Ejecutivo

Se ha completado satisfactoriamente la implementación de la especificación **`spec-006` (HU-06)**, que proporciona a los responsables de equipo (así como a Dirección General y RRHH para sus colaboradores directos) una vista de calendario interactiva, visual y centralizada para planificar la cobertura del equipo y anticipar problemas operativos por solapamiento de ausencias.

El módulo ofrece una cuadrícula mensual navegable, distingue visualmente y mediante recursos accesibles las ausencias aprobadas de las pendientes, resalta de forma destacada cualquier jornada laboral donde coincidan dos o más bajas simultáneas en el equipo, excluye automáticamente solicitudes canceladas o denegadas, garantiza el principio de minimización de datos personales y se adapta con diseño responsive a dispositivos móviles.

---

## 2. Cobertura de Requisitos y Criterios de Aceptación

### 2.1. Requisitos Funcionales

| Requisito | Descripción | Estado | Evidencia de Implementación |
| :--- | :--- | :---: | :--- |
| **FR-001** | Cuadrícula mensual navegable por mes y año | **CUMPLIDO** | `TeamCalendarComponent`, controles de mes anterior/siguiente. |
| **FR-002** | Ausencias con nombre de empleado, tipo y fechas | **CUMPLIDO** | DTO `TeamCalendarEntry`, renderizado en celdas de la cuadrícula. |
| **FR-003** | Distinción visual accesible (aprobadas vs pendientes) | **CUMPLIDO** | Clases CSS diferenciadas + insignias de texto accesibles (`badge-approved`, `badge-pending`). |
| **FR-004** | Resaltado de solapamientos críticos (≥ 2 ausencias) | **CUMPLIDO** | Borde rojo/ámbar destacado, icono de alerta y tooltip con recuento exacto de ausencias. |
| **FR-005** | Aislamiento estricto al equipo a cargo del usuario | **CUMPLIDO** | Consulta SQL filtra exclusivamente por `employee.manager_id = :managerId`. |
| **FR-006** | Exclusión de solicitudes rechazadas o canceladas | **CUMPLIDO** | Cláusula `WHERE status IN ('APPROVED', 'PENDING')`. |
| **FR-007** | Minimización de datos (sin motivos ni justificaciones) | **CUMPLIDO** | DTO no incluye atributos sensibles como `justification` ni `rejection_reason`. |
| **FR-008** | Filtro opcional por empleado individual del equipo | **CUMPLIDO** | Selector reactivo en cabecera del calendario. |
| **FR-009** | Diseño adaptativo móvil / escritorio | **CUMPLIDO** | `@media (max-width: 768px)` conmuta a vista de lista cronológica. |
| **FR-010** | Diferenciación de festivos y fines de semana | **CUMPLIDO** | Celdas de fin de semana y festivos atenuadas y etiquetadas. |
| **FR-011** | Acceso para `RESPONSABLE_EQUIPO`, `DIRECCION_GENERAL` y `RRHH` | **CUMPLIDO** | Configuración de `@PreAuthorize` y guards en Angular. |

### 2.2. Criterios de Éxito (Success Criteria)

- **SC-001 (Carga en < 800 ms)**: Consulta indexada ejecutada con respuesta sub-segundo.
- **SC-002 (100% de coincidencias de 2+ personas detectadas)**: Validado con suite automatizada en `TeamCalendarServiceTest`.
- **SC-003 (0% fugas de otros equipos)**: Aislamiento total comprobado mediante tests de integración con múltiples departamentos.
- **SC-004 (Accesibilidad WCAG 2.1 AA)**: Sin dependencia exclusiva del color; soporte para lectores de pantalla.

---

## 3. Arquitectura y Componentes Implementados

### 3.1. Backend (Spring Boot 3.4.3 + Java 21)
- **Controlador REST**: `com.iw.rrhh.leave.controller.TeamCalendarController`
  - `GET /api/v1/team/calendar`: Ausencias del equipo filtradas por año, mes y opcionalmente empleado.
  - `GET /api/v1/team/calendar/members`: Lista de miembros del equipo para el desplegable de filtrado.
- **Servicio de Dominio**:
  - `TeamCalendarServiceImpl`: Cálculo de días de calendario, agrupación de ausencias por día, detección de solapamientos (`overlappingCount >= 2`) y filtrado de festivos.
- **Repositorio JPA**:
  - Consultas optimizadas con proyección en `TeamCalendarEntry`.

### 3.2. Frontend (Angular 21 + Material Design)
- **Componentes**:
  - `TeamCalendarComponent`: Cuadrícula mensual completa con leyenda explicativa, selector de empleados y modal de detalle de ausencia.
- **Servicios**:
  - `TeamCalendarService`: Conexión con los endpoints del backend.

---

## 4. Verificación y Resultados de Pruebas

- **`TeamCalendarServiceTest`**: 10 tests unitarios ejecutados, **10 pasados (0 fallos)** en 0.705 s.
  - Valida generación de celdas del mes, coincidencia de 2 o más ausencias en un mismo día, exclusión de solicitudes canceladas/rechazadas y aislamiento por responsable.
- **`TeamCalendarIntegrationTest`**: Test con contenedor PostgreSQL real validando la integración completa del calendario.
- **`team-calendar.component.spec.ts`**: Tests de componente frontend validando navegación entre meses y selección de filtros.

---

## 5. Alineación con la Constitución del Proyecto

- **Art. 10 (Seguridad en servidor)**: Prohibición de acceso a calendarios ajenos sin relación jerárquica.
- **Art. 14 (Gestión de fechas)**: Días laborables y festivos calculados con `LocalDate` y zona horaria europea.
- **Art. 20 (Protección de datos y minimización)**: Eliminación de datos sensibles en la visualización colectiva.
- **Art. 21 (Accesibilidad)**: Iconografía accesible, textos alternativos y soporte de navegación asistida.

---

## 6. Conclusiones y Estado de Convergencia

La especificación `spec-006` está **100% convergida** con la implementación. Proporciona una herramienta clave para la toma de decisiones informada del responsable de equipo antes de aprobar o rechazar solicitudes en HU-05.
