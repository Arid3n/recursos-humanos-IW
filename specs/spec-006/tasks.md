# Tasks: spec-006 — Calendario de Ausencias del Equipo

**Feature**: `spec-006` (HU-06) · **Revisado**: 2026-11-18 (tras `/speckit-clarify` y `/speckit-plan` de esa fecha)

> **Estado**: T001–T020 corresponden a la implementación previa a las aclaraciones de 2026-11-18 y se conservan marcadas. Las tareas T023–T028 (Phase 6) añaden lo que cambian las aclaraciones: solicitudes retiradas, ausencias propias del responsable, mes inicial en Europe/Madrid, vista de lista en pantallas estrechas (FR-003b), ausencia de datos de motivo y navegación sin límite. T021 y T022 quedan **sustituidas** por T031 y T032 (que cubren los escenarios 1–9 y la spec revisada); T019 y T020 se repiten en T029 y T030 tras los cambios. La bandeja de HU-05 ya excluye las solicitudes propias del responsable (spec-005, T039); T026 aplica la misma exclusión al calendario.

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/team-calendar-api.yaml](contracts/team-calendar-api.yaml), [quickstart.md](quickstart.md)

**Tests**: Incluidos (Art. 22 test-first y Art. 2 trazabilidad): cada test se etiqueta con `@DisplayName` `HU-06.CA-X`. Dentro de cada historia, los tests se escriben antes que la implementación y deben fallar primero.

**Organization**: US1 = calendario mensual con coincidencias (P1, MVP); US2 = filtro por empleado (P2).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Paralelizable (ficheros distintos, sin dependencias pendientes)
- **[Story]**: Historia a la que pertenece (US1, US2)

---

## Phase 1: Setup

**Purpose**: No aplica. Sin migraciones, dependencias ni datos de desarrollo nuevos (se reutilizan los de spec-005).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nombre de los festivos en el componente único de calendario (Art. 14.3), necesario para clasificar los días.

- [X] T001 Add tests for `getHolidayName` to `backend/src/test/java/com/iw/rrhh/common/HolidayCalendarServiceTest.java`: fixed holiday returns its name (e.g. `2026-12-08` → "Inmaculada Concepción"), movable holiday returns "Viernes Santo"/"Jueves Santo", working day returns `Optional.empty()`; existing tests stay unchanged
- [X] T002 Refactor `backend/src/main/java/com/iw/rrhh/common/service/HolidayCalendarService.java`: replace `Set<MonthDay> FIXED_HOLIDAYS` by `Map<MonthDay, String>` with the names now in comments, add `Optional<String> getHolidayName(LocalDate date)` (fixed + "Jueves Santo"/"Viernes Santo"), and implement `isHoliday(date)` as `getHolidayName(date).isPresent()` without behaviour change

**Checkpoint**: `HolidayCalendarServiceTest` en verde (existentes + nuevos).

---

## Phase 3: User Story 1 - Consultar el calendario mensual del equipo (Priority: P1) 🎯 MVP

**Goal**: El responsable ve el mes con las ausencias `APPROVED`/`PENDING` de su equipo, distingue su estado, ve resaltados los días laborables con ≥ 2 personas ausentes y navega entre meses; nunca ve otros equipos.

**Independent Test**: Quickstart escenarios 1-4.

### Tests for User Story 1 ⚠️

- [X] T003 [P] [US1] Unit tests (Mockito, `Clock` not needed) for `TeamCalendarServiceImpl` in `backend/src/test/java/com/iw/rrhh/leave/TeamCalendarServiceTest.java`: returns one `TeamCalendarDayResponse` per day of the month in order (`HU-06.CA-1`); events only on `WORKING` days within each request range, including a request crossing the month boundary, with the full `startDate`/`endDate` (`HU-06.CA-1`); `WEEKEND` and `HOLIDAY` days (with `holidayName`) have no events and `absentCount = 0` (`HU-06.CA-3`); two distinct employees on the same working day → `absentCount = 2`, `hasConflict = true`; one employee with two requests the same day counts once; a single absence → `hasConflict = false` (`HU-06.CA-3`); events keep `status` `PENDING`/`APPROVED` (`HU-06.CA-2`); repository is called with the session manager id, `[PENDING, APPROVED]` and the first/last day of the month (`HU-06.CA-4`)
- [X] T004 [P] [US1] Integration tests (Testcontainers + MockMvc, class NOT `@Transactional`) in `backend/src/test/java/com/iw/rrhh/leave/TeamCalendarIntegrationTest.java`: `GET /leave/team-calendar?month=2026-12` returns 31 days and team members (`HU-06.CA-1`); approved and pending events present, rejected absent (`HU-06.CA-2`); common working day of two team members has `hasConflict = true` and `absentCount = 2` (`HU-06.CA-3`); absences of another manager's employee and of an inactive team member never appear in events, counts or members (`HU-06.CA-4`); role `EMPLEADO` → `403` (`HU-06.CA-4`); `month=2026-13` and missing `month` → `400` ProblemDetail; holiday `2026-12-08` returned as `HOLIDAY` with name "Inmaculada Concepción"; after reassigning a team member to another manager, their absences disappear from the previous manager's calendar and members (edge case change of manager); role denial produces a `403` ProblemDetail; test employees use national IDs from `30000009J` onwards to avoid collisions with other test classes

### Backend Implementation for User Story 1

- [X] T005 [US1] Log every `403` in `handleAccessDenied` of `backend/src/main/java/com/iw/rrhh/common/exception/GlobalExceptionHandler.java` with `log.warn` including only HTTP method and request path (no personal data), so role denials are registered (Art. 10.6, FR-011; also covers HU-05)
- [X] T006 [P] [US1] Create enum `CalendarDayType { WORKING, WEEKEND, HOLIDAY }` in `backend/src/main/java/com/iw/rrhh/leave/domain/CalendarDayType.java`
- [X] T007 [P] [US1] Create DTO records in `backend/src/main/java/com/iw/rrhh/leave/dto/`: `TeamCalendarResponse(YearMonth month, List<TeamMemberResponse> members, List<TeamCalendarDayResponse> days)`, `TeamMemberResponse(UUID id, String name)`, `TeamCalendarDayResponse(LocalDate date, CalendarDayType dayType, String holidayName, int absentCount, boolean hasConflict, List<TeamCalendarEventResponse> events)`, `TeamCalendarEventResponse(UUID requestId, UUID employeeId, String employeeName, LeaveType leaveType, LeaveStatus status, LocalDate startDate, LocalDate endDate)`
- [X] T008 [P] [US1] Add `findTeamCalendarRequests(UUID managerId, List<LeaveStatus> statuses, LocalDate rangeStart, LocalDate rangeEnd)` to `backend/src/main/java/com/iw/rrhh/leave/repository/LeaveRequestRepository.java` with `SELECT lr FROM LeaveRequest lr JOIN FETCH lr.employee e WHERE e.manager.id = :managerId AND e.active = true AND lr.status IN :statuses AND lr.startDate <= :rangeEnd AND lr.endDate >= :rangeStart ORDER BY e.lastName, e.firstName, lr.startDate`
- [X] T009 [US1] Create `TeamCalendarService` and `TeamCalendarServiceImpl` (`@Transactional(readOnly = true)`) with `TeamCalendarResponse getTeamCalendar(YearMonth month)` in `backend/src/main/java/com/iw/rrhh/leave/service/`: manager from `SecurityContextHolder` (Art. 10.5); members from `EmployeeRepository.findByManagerIdAndActiveTrue` sorted by last name, first name; requests from T008; per day: `WEEKEND` (precedence) / `HOLIDAY` / `WORKING`, and `holidayName` = `getHolidayName(date)` whenever present, also on weekends; events only on `WORKING` days; `absentCount` = distinct `employeeId`s; `hasConflict = absentCount >= 2` (Art. 16.3)
- [X] T010 [US1] Create `TeamCalendarController` (`@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`) with `GET /leave/team-calendar` and required `@RequestParam YearMonth month` → `200 TeamCalendarResponse`, in `backend/src/main/java/com/iw/rrhh/leave/controller/TeamCalendarController.java`

### Frontend Implementation for User Story 1

- [X] T011 [P] [US1] Create models `CalendarDayType`, `TeamMember`, `TeamCalendarEvent`, `TeamCalendarDay`, `TeamCalendar` in `frontend/src/app/features/team-calendar/models/team-calendar.models.ts` (reuse `LeaveType` from `features/leave/models/leave.models.ts`; status typed `'PENDING' | 'APPROVED'`)
- [X] T012 [P] [US1] Create `TeamCalendarService.getTeamCalendar(month: string)` (`month` as `YYYY-MM`) against `/api/v1/leave/team-calendar` in `frontend/src/app/features/team-calendar/services/team-calendar.service.ts`
- [X] T013 [P] [US1] Create `AbsenceDetailDialogComponent` (standalone, `MAT_DIALOG_DATA` = `TeamCalendarEvent`; shows employee name, type, full range `dd/MM/yyyy` and status with icon + text; close button) in `frontend/src/app/features/team-calendar/components/absence-detail-dialog/absence-detail-dialog.component.ts`
- [X] T014 [P] [US1] Unit tests (Vitest) for `TeamCalendarComponent` in `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.spec.ts`: loads current month on init; "Mes anterior"/"Mes siguiente" request the right `YYYY-MM` (including year change) (`HU-06.CA-1`); renders events with name and status text "Aprobada"/"Pendiente" and a legend (`HU-06.CA-2`); conflict day shows text "2 ausencias coincidentes" (`HU-06.CA-3`); holiday name shown; empty-month message "No hay ausencias en este mes"; empty-team message "No tienes personas a tu cargo" when `members` is empty; clicking an event opens `AbsenceDetailDialogComponent` with that event (`HU-06.CA-1`)
- [X] T015 [US1] Create `TeamCalendarComponent` in `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.{ts,html,scss}`: month title in Spanish via `Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' })`; prev/next buttons with `aria-label`; semantic `<table>` Lun…Dom with one row per week (leading empty cells only for layout); each event a `button` with name, type icon and status badge (approved: `check_circle` + "Aprobada", solid; pending: `schedule` + "Pendiente", dashed border); conflict cell with highlighted border + `warning` icon + "N ausencias coincidentes"; weekend/holiday cells muted, holiday with its name; legend; loading, error, "No hay ausencias en este mes" and "No tienes personas a tu cargo" (when `members` is empty) states
- [X] T016 [US1] Register lazy route `team/calendar` with `authGuard` in `frontend/src/app/app.routes.ts` and add nav link "Calendario del Equipo" shown only when `authService.isResponsable()` in `frontend/src/app/app.html`

**Checkpoint**: US1 completa y validable de forma independiente.

---

## Phase 4: User Story 2 - Filtrar el calendario por empleado (Priority: P2)

**Goal**: El responsable aísla la planificación de una persona de su equipo y vuelve a la vista completa; las coincidencias siguen reflejando todo el equipo.

**Independent Test**: Quickstart escenario 5.

### Tests for User Story 2 ⚠️

- [X] T017 [US2] Add unit tests (Vitest) to `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.spec.ts`: filter options are "Todo el equipo" + `members`; selecting an employee shows only their events while the conflict text of the day remains; selecting "Todo el equipo" shows all events again; changing month keeps the selected employee and does not send any employee id to the API

### Implementation for User Story 2

- [X] T018 [US2] Add the employee filter (`mat-select` with label "Empleado", option "Todo el equipo" + `members`; signal `selectedEmployeeId`; view-only filtering of `events` with a `computed`) to `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.{ts,html}`

**Checkpoint**: US1 y US2 funcionales.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T019 [P] Run backend tests `cd backend && sh ./mvnw test -DargLine="-Dapi.version=1.44"` (full suite; HU-06 tests and `HolidayCalendarServiceTest` must pass; the 4 pre-existing failures reported in spec-005 are out of scope)
- [X] T020 [P] Run frontend tests `cd frontend && npx ng test --watch=false` and build `npx ng build` (no warnings, initial budget respected)
- [ ] T021 Validate manual scenarios 1-5 of `specs/spec-006/quickstart.md`
- [ ] T022 Update `specs/spec-006/spec.md` if any divergence with the final behaviour was found (Art. 1.2)

---

## Phase 6: Refinamiento tras las aclaraciones de 2026-11-18 (US1)

**Purpose**: Adaptar la implementación existente a las aclaraciones (`spec.md` sesión 2026-11-18; `plan.md` «Decisiones de la sesión de aclaraciones 2026-11-18»). Orden: tests primero (deben FALLAR con el código actual cuando aplique), después implementación.

**Goal**: (1) `CANCELLED` y `REJECTED` nunca aparecen ni cuentan; (2) ninguna justificación en la API ni en la UI; (3) el propio responsable no aparece en eventos ni en el filtro; (4) mes inicial en Europe/Madrid; (5) lista de días con ausencias en pantallas estrechas; (6) navegación de meses sin límite; (7) coincidencia ≥ 2 sea cual sea el tamaño del equipo.

**Independent Test**: quickstart escenarios 6–9 y los escenarios 2 y 4 revisados.

### Tests (escribir primero)

- [X] T023 [P] [US1] Extend `backend/src/test/java/com/iw/rrhh/leave/TeamCalendarServiceTest.java` (Mockito): the repository is queried with exactly `[PENDING, APPROVED]` so `REJECTED` and `CANCELLED` can never reach the response (`HU-06.CA-2`, FR-005); the session manager is excluded from `members` even when their `manager_id` points to themselves (`HU-06.CA-4`); `TeamCalendarEventResponse` exposes only the components `requestId`, `employeeId`, `employeeName`, `leaveType`, `status`, `startDate`, `endDate` (inspect `getRecordComponents()`, no reason or justification) (`HU-06.CA-1`, FR-002); a team of exactly two members both absent the same working day → `hasConflict = true` (FR-006)
- [X] T024 [P] [US1] Extend `backend/src/test/java/com/iw/rrhh/leave/TeamCalendarIntegrationTest.java` (Testcontainers + MockMvc, fixed `Clock` where dates matter): a `CANCELLED` request of a team member (created through HU-04 `POST /leave/requests/{id}/cancel`) does not appear in `events` nor in `absentCount`/`hasConflict` (`HU-06.CA-2`); a `REJECTED` request with `rejectionReason` and a request with justification exist in the month and the JSON contains no `rejectionReason`, `justification` or the reason text anywhere (`HU-06.CA-1`, FR-002); the manager's own pending request, with `manager_id` pointing to themselves, does not appear in `events` nor in `members` (`HU-06.CA-4`); `month=2031-01` and `month=2020-01` → `200` with every day of the month (FR-003); national IDs for new fixtures continue from the last one used in this class
- [X] T025 [P] [US1] Extend `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.spec.ts` (Vitest; provide a mock `BreakpointObserver` and a fake clock): at `2026-12-31T23:30:00Z` the initial request is for `2027-01` (Europe/Madrid), independent of the test machine zone (FR-003, Art. 14.2); with a narrow viewport the `<table>` is not rendered and a list (viewport width < 600 px) shows only working days that have events and omits weekends and holidays (FR-003b, FR-008), each with name, type, status text, legend and, on conflict days, the icon plus text "N ausencias coincidentes" (`HU-06.CA-1`, `HU-06.CA-3`, FR-003b); the month title, previous/next buttons and the employee filter still work in the narrow view and the filter keeps the conflict text (`US2`); with a wide viewport the table is rendered and the list is not; previous/next buttons are never disabled after navigating several years (FR-003)

### Backend Implementation

- [X] T026 [US1] In `backend/src/main/java/com/iw/rrhh/leave/repository/LeaveRequestRepository.java` add `AND e.id <> :managerId` to `findTeamCalendarRequests`, and in `backend/src/main/java/com/iw/rrhh/leave/service/TeamCalendarServiceImpl.java` filter the session manager out of `members`, so the manager's own absences and name never appear (Assumptions; consistent with the HU-05 inbox). Keep `VISIBLE_STATUSES = [PENDING, APPROVED]` and `CONFLICT_THRESHOLD = 2` unchanged. No DTO, contract or migration change (`contracts/team-calendar-api.yaml` is already v1.1.0)

### Frontend Implementation

- [X] T027 [US1] In `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.ts` compute the initial visible month from the current date in `Europe/Madrid` (e.g. `Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Madrid', year: 'numeric', month: '2-digit' })`) instead of `new Date()` local fields; one named zone constant (Art. 14.2); month navigation itself stays unbounded
- [X] T028 [US1] Add the narrow-screen view to `frontend/src/app/features/team-calendar/components/team-calendar/team-calendar.component.{ts,html,scss}`: inject `BreakpointObserver` from `@angular/cdk/layout` (already a dependency, no new package), signal `isNarrow` from `Breakpoints.XSmall`; when narrow render a `<ul>` of the working days of the visible month that have events (weekends and holidays omitted; no label for them in the list) (derived from the same `days`, honouring the employee filter), each item with the date, event buttons (name, type icon, status icon + text, opens the same `AbsenceDetailDialogComponent`) and the conflict marker with `warning` icon + "N ausencias coincidentes"; keep title, navigation, legend, filter and the empty-month message; when wide keep the `<table>`; texts externalised (Art. 21.3); no business rule in the client (Art. 14.4)

**Checkpoint**: T023–T025 pasan; quickstart escenarios 6–9 validados; US1 y US2 siguen en verde.

---

## Phase 7: Polish de la revisión 2026-11-18

- [X] T029 [P] Run backend tests `cd backend && JAVA_TOOL_OPTIONS="-Dapi.version=1.44" sh ./mvnw test` (full suite; HU-01…HU-05 and HU-08 must stay green with the repository change of T026)
- [X] T030 [P] Run frontend tests `cd frontend && npx ng test --watch=false` and build `npx ng build` (no warnings, initial budget respected)
- [ ] T031 Validate manual scenarios 1–9 of `specs/spec-006/quickstart.md` (scenarios 2 and 4 revised, 6–9 new); supersedes T021
- [ ] T032 Check Art. 24.3 Definition of Done for HU-06: every CA covered by a `HU-06.CA-X` test, `contracts/team-calendar-api.yaml` v1.1.0 matches the responses (`200`/`400`/`401`/`403`), no personal data in logs; update `specs/spec-006/spec.md` status and, if any divergence is found, spec first (Art. 1.3); supersedes T022

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: bloquea US1 (clasificación de festivos).
- **US1 (Phase 3)**: depende de Phase 2.
- **US2 (Phase 4)**: depende de T015 (mismo componente); no necesita cambios de backend.
- **Polish (Phase 5)**: depende de US1 y US2.
- **Refinamiento (Phase 6)**: depende de Phases 2–4 (código previo). Dentro: T023–T025 antes de T026–T028; T026 antes de poner en verde T023 y T024; T027 y T028 antes de poner en verde T025 (T028 después de T027 por tocar el mismo fichero).
- **Polish 2 (Phase 7)**: depende de Phase 6. T021 y T022 quedan sustituidas por T031 y T032.

```mermaid
graph TD
    T001 --> T002 --> T009
    T006 --> T007 --> T009
    T008 --> T009 --> T010
    T003 --> T009
    T004 --> T010
    T004 --> T005
    T011 --> T012 --> T015
    T013 --> T015
    T014 --> T015 --> T016
    T015 --> T017 --> T018
    T023 --> T026
    T024 --> T026
    T025 --> T027 --> T028
    T026 --> T029
    T028 --> T030
```

### Parallel Opportunities

- US1: T003 ‖ T004 ‖ T006 ‖ T007 ‖ T008 ‖ T011 ‖ T012 ‖ T013 ‖ T014.
- Backend y frontend de US1 avanzan en paralelo una vez fijado el contrato.
- Phase 6: T023 ‖ T024 ‖ T025 (tests); T026 (backend) ‖ T027 → T028 (frontend).

### Parallel Example: User Story 1

```text
Task: "T003 Unit tests TeamCalendarServiceTest"
Task: "T004 Integration tests TeamCalendarIntegrationTest"
Task: "T008 Repository query findTeamCalendarRequests"
Task: "T013 AbsenceDetailDialogComponent"
Task: "T014 TeamCalendarComponent spec"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 2 → Phase 3 (US1) → validar quickstart escenarios 1-4.

### Incremental Delivery

1. US1 → calendario con estados y coincidencias (MVP).
2. US2 → filtro por empleado.
3. Polish → suites completas y validación manual.
4. Refinamiento 2026-11-18 (Phase 6) → reglas aclaradas; entregable independiente sobre la implementación ya hecha, validable con quickstart 6–9.
