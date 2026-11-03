# Implementation Plan: spec-006 — Calendario de Ausencias del Equipo

**Branch**: `spec-006` | **Date**: 2026-11-03 | **Spec**: [specs/spec-006/spec.md](spec.md)

**Input**: Feature specification from `/specs/spec-006/spec.md`

---

## Summary

La funcionalidad **HU-06 (`spec-006`)** ofrece al responsable de equipo un calendario mensual de solo consulta con las ausencias `APPROVED` y `PENDING` de las personas a su cargo (empleados activos cuyo `manager_id` es el responsable en sesión). Un único endpoint `GET /api/v1/leave/team-calendar?month=YYYY-MM` devuelve, para cada día del mes: su tipo (laborable, fin de semana o festivo con nombre), las ausencias que lo ocupan, el número de personas ausentes y el indicador de coincidencia (`≥ 2`), calculado en servidor (Art. 16.3). También devuelve la lista de miembros del equipo para el filtro. El frontend pinta la cuadrícula, la navegación por meses, la leyenda, el filtro por empleado (sobre la vista) y el detalle de cada ausencia, sin reimplementar reglas de negocio.

---

## Technical Context

**Language/Version**: Java 21 LTS (Backend) · TypeScript 5.x estricto / Angular 21 (Frontend)

**Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA) · Angular Material 21 (button, icon, select, dialog, progress-spinner)

**Storage**: PostgreSQL 16 — tabla existente `leave_requests` (índice existente `idx_leave_calendar (start_date, end_date, status)`). Sin migraciones nuevas.

**Testing**: JUnit 5 + Mockito (unitarios) · Spring Boot Test + MockMvc + Testcontainers PostgreSQL (integración) · Vitest (frontend)

**Target Platform**: Navegadores modernos (escritorio y móvil) · API REST en contenedor Linux

**Project Type**: Aplicación web (SPA Angular + API REST Spring Boot)

**Performance Goals**: Respuesta del mes completo < 800 ms; renderizado del mes < 1 s (SC-001)

**Constraints**: Consulta limitada al mes visible (Art. 16.4) · coincidencias calculadas en servidor (Art. 16.3) · estado no solo por color (Art. 16.2 y 21) · autorización por rol y alcance desde la sesión (Art. 10)

**Scale/Scope**: Equipos de decenas de personas; 1 endpoint de solo lectura; 1 pantalla + 1 diálogo en frontend

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Artículo | Verificación | Estado |
|---|---|---|
| **Art. 1-3** (Spec como fuente de verdad) | Plan basado en `spec.md` y aclaraciones de 2026-10-27; sin `[NEEDS CLARIFICATION]`. | ✅ |
| **Art. 2** (Trazabilidad) | Tests etiquetados `HU-06.CA-1` … `HU-06.CA-4` (ver `quickstart.md` §3). | ✅ |
| **Art. 4** (Corte vertical) | Calendario UI → API → consulta en BD. | ✅ |
| **Art. 5 / 7** (Capas) | `TeamCalendarController` → `TeamCalendarService` (`@Transactional(readOnly = true)`) → `LeaveRequestRepository`/`EmployeeRepository`; DTOs `record`. | ✅ |
| **Art. 8.5** (Índices) | La consulta por equipo y rango usa `idx_leave_calendar` existente. | ✅ |
| **Art. 9** (API REST) | `GET /api/v1/leave/team-calendar?month=YYYY-MM`; contrato en `contracts/`. No es un listado paginado: el tamaño está acotado por el mes (Art. 16.4). | ✅ |
| **Art. 10** (Autorización 2 niveles) | Rol: `hasRole('RESPONSABLE_EQUIPO')`. Alcance: el equipo se obtiene del responsable en sesión; el cliente no envía identificadores de empleado. Test negativo con otro equipo y con rol `EMPLEADO`. | ✅ |
| **Art. 14** (Fechas y cómputo) | Días laborables y festivos desde `HolidayCalendarService` (único componente de cómputo); se amplía con el nombre del festivo sin duplicar la regla. | ✅ |
| **Art. 16** (Calendario) | Solo equipo (16.1); aprobada/pendiente con icono y texto (16.2); coincidencias en servidor (16.3); consulta por mes (16.4). | ✅ |
| **Art. 19** (Errores) | Mes con formato inválido → `400` ProblemDetail; `401`/`403` estándar. | ✅ |
| **Art. 20** (Datos personales) | Solo se exponen nombre, tipo, fechas y estado (minimización); sin motivos de rechazo ni datos contractuales. | ✅ |
| **Art. 21** (Accesibilidad) | Leyenda, iconos y texto para estado y coincidencias; ausencias como botones navegables por teclado. | ✅ |
| **Art. 23** (Simplicidad) | Un endpoint, sin dependencias nuevas ni librerías de calendario. | ✅ |

**Re-evaluación post-diseño**: ✅ Sin violaciones. No aplica *Complexity Tracking*.

---

## Project Structure

### Documentation (this feature)

```text
specs/spec-006/
├── spec.md
├── plan.md              # Este plan
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/
│   └── team-calendar-api.yaml
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/com/iw/rrhh/
├── common/service/HolidayCalendarService.java             # MODIFICADO (nombre del festivo)
└── leave/
    ├── controller/TeamCalendarController.java             # NUEVO
    ├── domain/CalendarDayType.java                        # NUEVO (enum WORKING, WEEKEND, HOLIDAY)
    ├── dto/TeamCalendarResponse.java                      # NUEVO
    ├── dto/TeamCalendarDayResponse.java                   # NUEVO
    ├── dto/TeamCalendarEventResponse.java                 # NUEVO
    ├── dto/TeamMemberResponse.java                        # NUEVO
    ├── repository/LeaveRequestRepository.java             # MODIFICADO (consulta por equipo y rango)
    └── service/
        ├── TeamCalendarService.java                       # NUEVO
        └── TeamCalendarServiceImpl.java                   # NUEVO

backend/src/test/java/com/iw/rrhh/
├── common/HolidayCalendarServiceTest.java                 # MODIFICADO (nombres de festivos)
├── leave/TeamCalendarServiceTest.java                     # NUEVO (unitario)
└── leave/TeamCalendarIntegrationTest.java                 # NUEVO (Testcontainers + MockMvc)

frontend/src/app/
├── features/team-calendar/
│   ├── components/
│   │   ├── team-calendar/team-calendar.component.{ts,html,scss,spec.ts}    # NUEVO
│   │   └── absence-detail-dialog/absence-detail-dialog.component.ts       # NUEVO
│   ├── models/team-calendar.models.ts                     # NUEVO
│   └── services/team-calendar.service.ts                  # NUEVO
├── app.routes.ts                                          # MODIFICADO (/team/calendar)
└── app.html                                               # MODIFICADO (enlace "Calendario del Equipo")
```

**Structure Decision**: El calendario es una consulta sobre solicitudes de ausencia, por lo que vive en el módulo `leave` del backend junto a la resolución (HU-05). En frontend se ubica en `features/team-calendar/`, asignado a HU-05/HU-06 en `specs/plan.md` §4, con su propio servicio y modelos para no mezclarlo con la bandeja de HU-05.

---

## Complexity Tracking

No aplica: el diseño no introduce violaciones de la constitución.
