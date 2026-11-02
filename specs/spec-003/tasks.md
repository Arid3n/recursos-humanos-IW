# Tasks: spec-003 — Solicitud de Vacaciones o Permisos

**Feature**: `spec-003` · HU-03  
**Branch**: `spec-003`  
**Input**: [specs/spec-003/plan.md](plan.md), [specs/spec-003/spec.md](spec.md), [specs/spec-003/data-model.md](data-model.md), [specs/spec-003/contracts/leave-request-api.yaml](contracts/leave-request-api.yaml)  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificación de estructura de paquetes y configuración base para el flujo de solicitud de ausencias

- [x] T001 Verify backend common service package and leave controller structure in backend/src/main/java/com/iw/rrhh/common/
- [x] T002 [P] Verify frontend leave feature module structure and barrel exports in frontend/src/app/features/leave/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entidades, DTOs, servicios de cómputo y métodos de repositorio que bloquean la implementación de la historia de usuario

- [x] T003 [P] Implement HolidayCalendarService excluding weekends and official holidays in backend/src/main/java/com/iw/rrhh/common/service/HolidayCalendarService.java
- [x] T004 [P] Create DTO records CreateLeaveRequest, LeaveRequestResponse, and WorkingDaysCalculationResponse in backend/src/main/java/com/iw/rrhh/leave/dto/
- [x] T005 [P] Add hasOverlappingRequests query to LeaveRequestRepository in backend/src/main/java/com/iw/rrhh/leave/repository/LeaveRequestRepository.java
- [x] T006 [P] Add business exceptions LeaveRequestOverlapException and InsufficientLeaveBalanceException mapped to RFC 9457 ProblemDetail in backend/src/main/java/com/iw/rrhh/common/exception/GlobalExceptionHandler.java

---

## Phase 3: User Story 1 - Solicitud de Vacaciones o Permisos (Priority: P1) 🎯 MVP

**Goal**: Permitir al empleado autenticado cumplimentar un formulario para solicitar un periodo de vacaciones o un permiso especificando el tipo y las fechas, visualizando el cómputo oficial de días laborables antes de confirmar, validando que no excede el saldo de vacaciones ni solapa con ausencias previas, y registrando la solicitud en estado PENDING vinculada a su responsable.

**Independent Test**: Ejecutar `LeaveRequestIntegrationTest` contra PostgreSQL real vía Testcontainers seleccionando un rango de fechas con saldo suficiente, comprobando que el servidor calcula los días laborables descontando festivos y fines de semana, persistiendo la solicitud en estado `PENDING` y verificando los rechazos por saldo insuficiente o solapamiento.

### Tests for User Story 1 (TDD - Tests First) 🧪

- [x] T007 [P] [US1] Unit test for HolidayCalendarService verifying weekends and official holidays exclusion in backend/src/test/java/com/iw/rrhh/common/HolidayCalendarServiceTest.java
- [x] T008 [P] [US1] Unit test for LeaveRequestService verifying balance check, overlap rejection, and manager assignment in backend/src/test/java/com/iw/rrhh/leave/LeaveRequestServiceTest.java
- [x] T009 [P] [US1] Integration test testCreateLeaveRequest_Success_And_Validations with real PostgreSQL via Testcontainers in backend/src/test/java/com/iw/rrhh/leave/LeaveRequestIntegrationTest.java

### Implementation for User Story 1 (Backend)

- [x] T010 [US1] Implement LeaveRequestService and LeaveRequestServiceImpl with working days calculation, overlap detection, vacation balance validation, and PENDING persistence in backend/src/main/java/com/iw/rrhh/leave/service/LeaveRequestServiceImpl.java
- [x] T011 [US1] Implement LeaveRequestController exposing GET /api/v1/leave/requests/calculate-days and POST /api/v1/leave/requests in backend/src/main/java/com/iw/rrhh/leave/controller/LeaveRequestController.java

### Implementation for User Story 1 (Frontend)

- [x] T012 [P] [US1] Define TypeScript interfaces CreateLeaveRequest, LeaveRequestResponse, and WorkingDaysCalculationResponse in frontend/src/app/features/leave/models/leave.models.ts
- [x] T013 [US1] Add calculateWorkingDays and createLeaveRequest methods in frontend/src/app/features/leave/services/leave.service.ts
- [x] T014 [US1] Implement LeaveRequestCreateComponent reactive form with mat-select, mat-date-range-input, debounced days calculation, advance notice recommendation warning (< 7 days), and balance check in frontend/src/app/features/leave/components/leave-request-create/leave-request-create.component.ts
- [x] T015 [P] [US1] Implement accessible template and styling for LeaveRequestCreateComponent with WCAG 2.1 AA in frontend/src/app/features/leave/components/leave-request-create/leave-request-create.component.html
- [x] T016 [US1] Register /leave/request route in frontend/src/app/app.routes.ts and add navigation item in frontend/src/app/app.html
- [x] T017 [P] [US1] Unit test LeaveRequestCreateComponent verifying form validation and API interaction in frontend/src/app/features/leave/components/leave-request-create/leave-request-create.component.spec.ts

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Verificación integral, contratos y validación con quickstart

- [x] T018 [P] Synchronize OpenAPI documentation with springdoc in specs/spec-003/contracts/leave-request-api.yaml
- [x] T019 Execute full quickstart validation running backend test suite and frontend tests per specs/spec-003/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias previas — verificación inmediata.
- **Foundational (Phase 2)**: Depende de Setup — BLOQUEA la implementación de la historia de usuario.
- **User Story 1 (Phase 3)**: Depende de Foundational (Phase 2).
  - TDD primero: Tests unitarios e integración (T007-T009) se escriben y fallan antes del código de producción.
  - Backend: Servicio (T010) → Controlador (T011).
  - Frontend: Modelos (T012) → Servicio HTTP (T013) → Componente reactivo (T014-T016) → Tests (T017).
- **Polish (Phase 4)**: Depende de que User Story 1 esté completada y funcional.

### Parallel Opportunities

- T001 y T002 pueden ejecutarse en paralelo (Backend y Frontend).
- T003, T004, T005 y T006 en la fase Foundational modifican ficheros independientes y pueden implementarse en paralelo.
- Los tests T007, T008 y T009 pueden redactarse en paralelo.
- En el frontend, T012 y T015 pueden realizarse en paralelo con la lógica del componente.

---

## Parallel Example: User Story 1

```bash
# Redacción en paralelo de tests TDD:
Task: "Unit test for HolidayCalendarServiceTest in backend/src/test/java/com/iw/rrhh/common/HolidayCalendarServiceTest.java"
Task: "Unit test for LeaveRequestServiceTest in backend/src/test/java/com/iw/rrhh/leave/LeaveRequestServiceTest.java"
Task: "Integration test in backend/src/test/java/com/iw/rrhh/leave/LeaveRequestIntegrationTest.java"

# Implementación de modelos frontend y DTOs backend:
Task: "Create DTO records in backend/src/main/java/com/iw/rrhh/leave/dto/"
Task: "Define TypeScript interfaces in frontend/src/app/features/leave/models/leave.models.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational (servicio de festivos, consulta de solapamientos en repositorio y DTOs).
3. Completar Phase 3: User Story 1 (Backend + Frontend de creación de solicitud).
4. **Validación Independiente**: Probar el flujo completo creando una solicitud válida, verificando que se rechaza si solapa o excede saldo y comprobando el cálculo de días laborables.
5. Ejecutar Phase 4: Polish & Quickstart.
