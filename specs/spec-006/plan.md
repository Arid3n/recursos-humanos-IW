# Implementation Plan: spec-006 — Calendario de Ausencias del Equipo

**Branch**: `spec-006` | **Date**: 2026-11-03 · **Revisado**: 2026-11-18 (tras `/speckit-clarify`, sesión 2026-11-18) | **Spec**: [specs/spec-006/spec.md](spec.md)

**Input**: Feature specification from `/specs/spec-006/spec.md`

---

## Summary

La funcionalidad **HU-06 (`spec-006`)** ofrece al responsable de equipo un calendario mensual de solo consulta con las ausencias `APPROVED` y `PENDING` de las personas a su cargo (empleados activos cuyo `manager_id` es el responsable en sesión). Un único endpoint `GET /api/v1/leave/team-calendar?month=YYYY-MM` devuelve, para cada día del mes: su tipo (laborable, fin de semana o festivo con nombre), las ausencias que lo ocupan, el número de personas ausentes y el indicador de coincidencia (`≥ 2`), calculado en servidor (Art. 16.3). También devuelve la lista de miembros del equipo para el filtro. El frontend pinta la cuadrícula, la navegación por meses, la leyenda, el filtro por empleado (sobre la vista) y el detalle de cada ausencia, sin reimplementar reglas de negocio. Tras las aclaraciones de 2026-11-18: las solicitudes retiradas (`CANCELLED`) no se muestran, nunca se expone la justificación, en pantallas estrechas la vista es una lista de días con ausencias (FR-003b), la navegación de meses no tiene límite y la coincidencia es siempre ≥ 2 personas con independencia del tamaño del equipo.

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
| **Art. 1-3** (Spec como fuente de verdad) | Plan basado en `spec.md` y aclaraciones de 2026-10-27 y 2026-11-18; sin `[NEEDS CLARIFICATION]`. | ✅ |
| **Art. 2** (Trazabilidad) | Tests etiquetados `HU-06.CA-1` … `HU-06.CA-4` (ver `quickstart.md` §3). | ✅ |
| **Art. 4** (Corte vertical) | Calendario UI → API → consulta en BD. | ✅ |
| **Art. 5 / 7** (Capas) | `TeamCalendarController` → `TeamCalendarService` (`@Transactional(readOnly = true)`) → `LeaveRequestRepository`/`EmployeeRepository`; DTOs `record`. | ✅ |
| **Art. 8.5** (Índices) | La consulta por equipo y rango usa `idx_leave_calendar` existente. | ✅ |
| **Art. 9** (API REST) | `GET /api/v1/leave/team-calendar?month=YYYY-MM`; contrato en `contracts/`. No es un listado paginado: el tamaño está acotado por el mes (Art. 16.4). | ✅ |
| **Art. 10** (Autorización 2 niveles) | Rol: `hasRole('RESPONSABLE_EQUIPO')` (ampliado a `RRHH` y `DIRECCION_GENERAL` por HU-05, FR-017/FR-018; mismo alcance por responsable directo). Alcance: el equipo se obtiene del responsable en sesión; el cliente no envía identificadores de empleado. Test negativo con otro equipo y con rol `EMPLEADO`. | ✅ |
| **Art. 14** (Fechas y cómputo) | Días laborables y festivos desde `HolidayCalendarService` (único componente de cómputo); se amplía con el nombre del festivo sin duplicar la regla. | ✅ |
| **Art. 16** (Calendario) | Solo equipo (16.1); aprobada/pendiente con icono y texto (16.2); coincidencias en servidor (16.3); consulta por mes (16.4). | ✅ |
| **Art. 19** (Errores) | Mes con formato inválido → `400` ProblemDetail; `401`/`403` estándar. | ✅ |
| **Art. 20** (Datos personales) | Solo se exponen nombre, tipo, fechas y estado (minimización); ni la respuesta ni la interfaz incluyen justificación, motivos de rechazo ni datos contractuales (FR-002); test que lo comprueba sobre el JSON. | ✅ |
| **Art. 21** (Accesibilidad) | Leyenda, iconos y texto para estado y coincidencias; ausencias como botones navegables por teclado. Responsive (Art. 21.2): cuadrícula en pantallas anchas y lista de días con ausencias en estrechas (FR-003b). | ✅ |
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
├── common/exception/GlobalExceptionHandler.java          # MODIFICADO (registro de 403, Art. 10.6)
└── leave/
    ├── controller/TeamCalendarController.java             # NUEVO
    ├── domain/CalendarDayType.java                        # NUEVO (enum WORKING, WEEKEND, HOLIDAY)
    ├── dto/TeamCalendarResponse.java                      # NUEVO
    ├── dto/TeamCalendarDayResponse.java                   # NUEVO
    ├── dto/TeamCalendarEventResponse.java                 # NUEVO
    ├── dto/TeamMemberResponse.java                        # NUEVO
    ├── repository/LeaveRequestRepository.java             # MODIFICADO (consulta por equipo y rango; 2026-11-18: excluye al propio responsable)
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
│   │   ├── team-calendar/team-calendar.component.{ts,html,scss,spec.ts}    # NUEVO (MODIFICADO 2026-11-18: vista lista en pantallas estrechas, mes inicial en Europe/Madrid)
│   │   └── absence-detail-dialog/absence-detail-dialog.component.ts       # NUEVO
│   ├── models/team-calendar.models.ts                     # NUEVO
│   └── services/team-calendar.service.ts                  # NUEVO
├── app.routes.ts                                          # MODIFICADO (/team/calendar)
└── app.html                                               # MODIFICADO (enlace "Calendario del Equipo")
```

**Structure Decision**: El calendario es una consulta sobre solicitudes de ausencia, por lo que vive en el módulo `leave` del backend junto a la resolución (HU-05). En frontend se ubica en `features/team-calendar/`, asignado a HU-05/HU-06 en `specs/plan.md` §4, con su propio servicio y modelos para no mezclarlo con la bandeja de HU-05.

---

## Decisiones de la sesión de aclaraciones 2026-11-18

| # | Aclaración (spec) | Decisión de diseño | Detalle |
|---|---|---|---|
| 1 | Las solicitudes retiradas (`CANCELLED`, HU-04) no se muestran ni cuentan (FR-005) | Sin código nuevo: `VISIBLE_STATUSES = [PENDING, APPROVED]` ya las excluye; falta el test y la mención en el contrato | `research.md` §1.8 |
| 2 | Solo nombre, tipo, fechas y estado; nunca justificación (FR-002) | Sin código nuevo: los DTO no tienen campo de motivo; test que comprueba que el JSON no contiene `rejectionReason`/`justification` | `research.md` §1.9 |
| 3 | Pantallas estrechas: lista vertical de días con ausencias (FR-003b) | `BreakpointObserver` de `@angular/cdk/layout` (ya instalado con Material, sin dependencia nueva); misma respuesta de la API, solo cambia la presentación | `research.md` §1.10 |
| 4 | Navegación de meses sin límite (FR-003) | Sin código nuevo: cualquier `YearMonth` válido; se añade test con un mes lejano | `research.md` §1.11 |
| 5 | Coincidencia = ≥ 2 personas, sin depender del tamaño del equipo (FR-006) | Sin cambio: constante `CONFLICT_THRESHOLD = 2` en servidor | `research.md` §1.4 |
| — | Supuesto «las ausencias propias del responsable no aparecen» (heredado de HU-05) | La consulta excluye `e.id = :managerId` aunque su `manager_id` apunte a sí mismo, como la bandeja de HU-05 | `research.md` §1.3 |
| — | Mes inicial en Europe/Madrid (Assumptions, Art. 14.2) | El frontend calcula el mes actual con `Intl.DateTimeFormat` en `Europe/Madrid`, no con la zona del navegador | `research.md` §1.7 |

**Impacto en código ya implementado** (la rama contiene la implementación previa de HU-06; estas diferencias deben reflejarse en `/speckit-tasks`):

- `TeamCalendarComponent` no tiene vista de lista para pantallas estrechas (FR-003b): hay que añadirla.
- `TeamCalendarComponent` calcula el mes inicial con `new Date()` del navegador: debe usar `Europe/Madrid`.
- `findTeamCalendarRequests` no excluye al propio responsable cuando su `manager_id` apunta a sí mismo: añadir `AND e.id <> :managerId`.
- Faltan los tests: solicitud `CANCELLED` excluida del evento y del recuento, JSON sin justificación, responsable autorreferenciado, mes lejano, vista de lista estrecha.
- El contrato `contracts/team-calendar-api.yaml` pasa a 1.1.0 (estados excluidos y ausencia de datos de motivo documentados).

---

## Complexity Tracking

No aplica: el diseño no introduce violaciones de la constitución.
