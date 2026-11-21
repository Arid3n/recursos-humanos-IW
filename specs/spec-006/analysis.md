# Specification Analysis Report: spec-006 — Calendario de Ausencias del Equipo

**Feature**: `spec-006` · HU-06  
**Fecha de análisis**: 2026-11-20  
**Estado**: Completado (implementación verificada contra código real)  
**Responsable**: Andrei Radu (Integrante 3 — Gestión y Supervisión del Responsable)

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [data-model.md](data-model.md)
- [contracts/team-calendar-api.yaml](contracts/team-calendar-api.yaml)
- [.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- Código implementado: `backend/src/main/java/com/iw/rrhh/leave/` y `frontend/src/app/features/team-calendar/`

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **HIGH** | `spec.md FR-011` vs Roles | Inicialmente el calendario solo contemplaba acceso para `RESPONSABLE_EQUIPO`, excluyendo a `DIRECCION_GENERAL` y `RRHH`. | **RESUELTO**: Ampliado el alcance en backend y frontend para permitir que `DIRECCION_GENERAL` y `RRHH` consulten el calendario de sus subordinados directos (HU-05 FR-017/018). |
| **U1** | Underspecification | **MEDIUM** | `spec.md Clarifications` | Representación de solicitudes retiradas/canceladas (`CANCELLED`). | **RESUELTO**: Clarificado que las solicitudes en estado `CANCELLED` no se renderizan en el calendario ni computan para las coincidencias del equipo. |
| **U2** | Underspecification | **LOW** | `spec.md FR-007` | Exposición de justificaciones privadas en la vista mensual. | **RESUELTO**: Principio de minimización estricto (Art. 20): el calendario omite campos de justificación o comentarios personales, exponiendo solo nombre, tipo, fechas y estado. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Cuadrícula mensual interactiva con selector de mes) | Sí | T005, T008, T012, T013 | Navegación mes anterior/siguiente sin recarga de página. |
| **FR-002** (Detalle de ausencia: empleado, tipo y fechas) | Sí | T004, T008, T013 | Panel/diálogo informativo al pulsar sobre la ausencia. |
| **FR-003** (Distinción visual accesible entre aprobadas y pendientes) | Sí | T005, T013, T014 | Distinción dual: color + texto/icono accesible (WCAG 2.1 AA). |
| **FR-004** (Alerta de solapamientos: 2 o más ausencias en el mismo día) | Sí | T006, T008, T013 | Borde destacado, icono de advertencia y texto explicativo del número de ausencias. |
| **FR-005** (Aislamiento de alcance exclusivo al equipo a cargo) | Sí | T007, T008, T016 | El servidor filtra estrictamente por los subordinados del usuario en sesión. |
| **FR-006** (Exclusión de ausencias canceladas o rechazadas) | Sí | T006, T008, T010 | Query SQL filtra por `status IN ('APPROVED', 'PENDING')`. |
| **FR-007** (Minimización de datos: sin justificaciones privadas) | Sí | T004, T008 | DTO `TeamCalendarEntry` no expone campos de justificación. |
| **FR-008** (Filtro por empleado individual dentro del equipo) | Sí | T008, T012, T015 | Desplegable reactivo con los miembros del equipo directo. |
| **FR-009** (Diseño responsive adaptativo: móvil y escritorio) | Sí | T013, T014 | Vista de lista alternativa en viewports pequeños (< 768px). |
| **FR-010** (Atenuación de fines de semana y festivos) | Sí | T005, T013 | Marcado visual de jornadas no laborables en la cuadrícula. |
| **FR-011** (Soporte para `RESPONSABLE_EQUIPO`, `DIRECCION_GENERAL` y `RRHH`) | Sí | T008, T016 | Autorización en servicio y rutas acorde a la Constitución. |
| **SC-001** (Visualización del mes en < 800 ms) | Sí | T008, T010 | Consulta optimizada mediante índices en `leave_requests`. |
| **SC-002** (100% de coincidencias de 2+ personas detectadas) | Sí | T006, T009 | Verificado exhaustivamente en `TeamCalendarServiceTest`. |
| **SC-003** (0% fugas de ausencias de otros equipos) | Sí | T008, T016 | Suite de tests de integración con usuarios de distintos equipos. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*

- **Art. 10 (Seguridad en servidor)**: El listado de miembros del equipo se extrae de la base de datos a partir del `employee_id` del usuario autenticado.
- **Art. 14 (Fechas en servidor)**: Rangos temporales calculados con `LocalDate` y zona `Europe/Madrid`.
- **Art. 20 (Minimización y privacidad)**: No se transmiten datos médicos ni justificaciones personales en las respuestas del calendario.
- **Art. 21 (Accesibilidad)**: Solapamientos indicados con contraste adecuado, iconos con etiquetas `aria-label` y navegación por teclado.

---

## 4. Unmapped Tasks

*Ninguna.* Las 32 tareas de la especificación cubren los modelos de datos, servicios, componentes Angular y suites de pruebas.

---

## 5. Métricas

- **Total Requirements (FR + SC)**: 14
- **Requisitos cubiertos con tareas y código**: 14 (100%)
- **Hallazgos identificados**: 3 (todos resueltos)
- **Estado general**: **Aprobado con máxima coherencia**
