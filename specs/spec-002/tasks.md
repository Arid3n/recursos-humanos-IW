# Tasks: spec-002 — Consulta de Ficha y Saldo de Vacaciones

**Feature**: `spec-002` · HU-02  
**Branch**: `spec-002`  
**Input**: [specs/spec-002/plan.md](plan.md), [specs/spec-002/spec.md](spec.md), [specs/spec-002/data-model.md](data-model.md), [specs/spec-002/contracts/leave-balance-api.yaml](contracts/leave-balance-api.yaml)  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verificación de estructura de paquetes y configuración base para el módulo de vacaciones y perfil

- [x] T001 Verify and prepare backend package structure for leave domain, repository, service, controller, and dto in backend/src/main/java/com/iw/rrhh/leave/
- [x] T002 [P] Setup frontend leave feature directory structure and export barrels in frontend/src/app/features/leave/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entidades nucleares, enums, repositorios JPA con consultas de agregación y DTOs que bloquean la implementación de las historias de usuario

- [x] T003 [P] Create LeaveType enum with values 'VACATION' and 'PERMIT' with constraint "CHECK (leave_type IN ('VACATION', 'PERMIT'))" in backend/src/main/java/com/iw/rrhh/leave/domain/LeaveType.java
- [x] T004 [P] Create LeaveStatus enum with values 'PENDING', 'APPROVED', and 'REJECTED' with constraint "CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))" in backend/src/main/java/com/iw/rrhh/leave/domain/LeaveStatus.java
- [x] T005 [P] Create JPA Entity LeaveRequest with constraints "PRIMARY KEY DEFAULT gen_random_uuid()", "REFERENCES employees(id) ON DELETE CASCADE", "CHECK (end_date >= start_date)", "CHECK (calculated_days >= 0)", and optimistic locking "version BIGINT NOT NULL DEFAULT 0, @Version" in backend/src/main/java/com/iw/rrhh/leave/domain/LeaveRequest.java
- [x] T006 [P] Create LeaveRequestRepository with SQL aggregation query sumCalculatedDays and findByEmployeeAndYear in backend/src/main/java/com/iw/rrhh/leave/repository/LeaveRequestRepository.java
- [x] T007 [P] Create domain Value Object LeaveBalance record with invariant "availableDays = Math.max(0, entitledDays - spentDays)" in backend/src/main/java/com/iw/rrhh/leave/domain/LeaveBalance.java
- [x] T008 [P] Create DTO records LeaveBalanceResponse and LeaveSummaryItemResponse in backend/src/main/java/com/iw/rrhh/leave/dto/
- [x] T009 [P] Create DTO record EmployeeProfileResponse in backend/src/main/java/com/iw/rrhh/employee/dto/EmployeeProfileResponse.java

---

## Phase 3: User Story 1 - Consulta de Ficha Propia y Saldo de Vacaciones del Año en Curso (Priority: P1) 🎯 MVP

**Goal**: Permitir al empleado autenticado consultar su ficha personal y su saldo de vacaciones desglosado (correspondientes, disfrutados, disponibles y en trámite) mediante endpoints seguros `/me` que obtienen la identidad del `SecurityContextHolder`.

**Independent Test**: Autenticarse como empleado en el sistema y verificar que `GET /api/v1/employees/me` y `GET /api/v1/leave/balances/me` devuelven la información personal correcta y el desglose de días con 100% de coherencia matemática (`correspondientes == disfrutados + disponibles`).

### Tests for User Story 1

- [x] T010 [P] [US1] Unit test for profile retrieval and current year leave balance calculation in backend/src/test/java/com/iw/rrhh/leave/LeaveBalanceServiceTest.java
- [x] T011 [P] [US1] Integration test testMyLeaveBalanceAndProfile_Success with real PostgreSQL via Testcontainers in backend/src/test/java/com/iw/rrhh/leave/LeaveBalanceIntegrationTest.java

### Implementation for User Story 1

- [x] T012 [US1] Implement LeaveBalanceService and LeaveBalanceServiceImpl.getMyLeaveBalance calculating dynamic balance from contract and LeaveRequestRepository in backend/src/main/java/com/iw/rrhh/leave/service/LeaveBalanceServiceImpl.java
- [x] T013 [US1] Implement EmployeeProfileController exposing GET /api/v1/employees/me using SecurityContext authentication in backend/src/main/java/com/iw/rrhh/employee/controller/EmployeeProfileController.java
- [x] T014 [US1] Implement LeaveBalanceController exposing GET /api/v1/leave/balances/me and GET /api/v1/leave/requests/me in backend/src/main/java/com/iw/rrhh/leave/controller/LeaveBalanceController.java
- [x] T015 [P] [US1] Create TypeScript interfaces LeaveBalanceResponse, LeaveSummaryItem, and EmployeeProfile in frontend/src/app/features/leave/models/leave.models.ts
- [x] T016 [US1] Implement LeaveService HTTP client for /balances/me and /requests/me in frontend/src/app/features/leave/services/leave.service.ts
- [x] T017 [P] [US1] Implement LeaveBalanceWidgetComponent rendering 4 metric cards (Correspondientes, Disfrutados, Disponibles, En trámite) with Angular Material in frontend/src/app/features/leave/components/leave-balance-widget/leave-balance-widget.component.ts
- [x] T018 [US1] Implement MyProfileComponent rendering personal details and embedding LeaveBalanceWidgetComponent in frontend/src/app/features/employees/components/my-profile/my-profile.component.ts
- [x] T019 [US1] Register /profile route and navigation item in frontend/src/app/app.routes.ts

---

## Phase 4: User Story 2 - Recálculo Dinámico y Prorrateo por Incorporación a Mitad de Año (Priority: P1)

**Goal**: Asegurar el recálculo automático instantáneo ante cambios contractuales o solicitudes aprobadas, y calcular el prorrateo proporcional para incorporaciones durante el año natural.

**Independent Test**: Registrar un empleado incorporado el 1 de julio y verificar que sus días correspondientes se calculan exactamente como `Math.round((convenio / 12) * 6)`, y comprobar que tras aprobar una solicitud de vacaciones el saldo disponible se reduce de forma inmediata sin procesos batch.

### Tests for User Story 2

- [x] T020 [P] [US2] Unit test for mid-year hire prorating formula round((convenioDays / 12.0) * remainingMonths) in backend/src/test/java/com/iw/rrhh/leave/LeaveProratingUnitTest.java
- [x] T021 [P] [US2] Integration test testDynamicRecalculationOnAgreementChangeAndLeaveApproval in backend/src/test/java/com/iw/rrhh/leave/LeaveRecalculationIntegrationTest.java

### Implementation for User Story 2

- [x] T022 [US2] Implement mid-year hire date prorating formula in LeaveBalanceServiceImpl in backend/src/main/java/com/iw/rrhh/leave/service/LeaveBalanceServiceImpl.java
- [x] T023 [US2] Enforce non-negative available days constraint Math.max(0, entitledDays - spentDays) in backend/src/main/java/com/iw/rrhh/leave/service/LeaveBalanceServiceImpl.java

---

## Phase 5: User Story 3 - Control de Acceso por Alcance y Pruebas Negativas de Seguridad (Priority: P2)

**Goal**: Impedir estrictamente que un usuario con rol `EMPLEADO` acceda a la ficha o saldo de otro compañero devolviendo HTTP 403 Forbidden RFC 9457 (Art. 10), permitiendo a supervisores y RRHH consultar a sus subordinados.

**Independent Test**: Ejecutar test de autorización negativa donde el usuario Empleado A intenta consultar el endpoint `GET /api/v1/leave/balances/employee/{id_empleado_B}` y verificar que recibe HTTP 403 Forbidden con formato ProblemDetail.

### Tests for User Story 3

- [x] T024 [P] [US3] Security negative integration test testForbiddenAccessToOtherEmployeeBalance in backend/src/test/java/com/iw/rrhh/leave/LeaveSecurityNegativeIntegrationTest.java

### Implementation for User Story 3

- [x] T025 [US3] Implement GET /api/v1/leave/balances/employee/{employeeId} in LeaveBalanceController with role and supervisor scope checks in backend/src/main/java/com/iw/rrhh/leave/controller/LeaveBalanceController.java
- [x] T026 [US3] Update GlobalExceptionHandler to serialize AccessDeniedException to RFC 9457 ProblemDetail in backend/src/main/java/com/iw/rrhh/common/exception/GlobalExceptionHandler.java

---

## Phase 6: User Story 4 - Consulta de Ejercicios Fiscales Pasados en Modo Solo Lectura (Priority: P2)

**Goal**: Permitir la consulta histórica de balances de vacaciones de ejercicios anteriores en modo solo lectura con selector interactivo de año.

**Independent Test**: Consultar un año anterior en la interfaz y comprobar que la API devuelve `isClosedYear = true` con los días calculados según el convenio vigente a 31 de diciembre de dicho ejercicio y las solicitudes aprobadas en dicho año.

### Tests for User Story 4

- [x] T027 [P] [US4] Unit test testHistoricalBalanceCalculationWithClosedYearFlag in backend/src/test/java/com/iw/rrhh/leave/LeaveBalanceServiceTest.java

### Implementation for User Story 4

- [x] T028 [US4] Add support for optional year query parameter and historical contract resolution in LeaveBalanceServiceImpl in backend/src/main/java/com/iw/rrhh/leave/service/LeaveBalanceServiceImpl.java
- [x] T029 [US4] Add year selector dropdown with mat-select and closed-year read-only badge in frontend/src/app/features/leave/components/leave-balance-widget/leave-balance-widget.component.ts

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verificación de accesibilidad, ejecución completa de suites de pruebas y validación del flujo funcional

- [x] T030 [P] Accessibility test for LeaveBalanceWidgetComponent keyboard navigation and contrast (WCAG 2.1 AA) in frontend/src/app/features/leave/components/leave-balance-widget/leave-balance-widget.component.spec.ts
- [x] T031 Run backend test suite for leave and employee modules (./mvnw test -Dtest=*Leave*Test,*Employee*Test)
- [x] T032 [P] Run frontend test suite with Vitest (npm test)
- [x] T033 Validate manual end-to-end scenarios described in quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — completada.
- **Foundational (Phase 2)**: Depende de Setup — completada.
- **User Stories (Phase 3+)**: Dependen de la finalización de Foundational — completadas.
- **Polish (Phase 7)**: Completada.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Completar **Phase 1: Setup** y **Phase 2: Foundational**.
2. Implementar **Phase 3: User Story 1**.
3. **VALIDAR MVP**: Ejecutar `LeaveBalanceIntegrationTest` y comprobar en el navegador que el empleado puede ver su ficha y saldo actual con desglose exacto.

### Incremental Delivery
1. Añadir **US2**: Incorporar la lógica de prorrateo para altas a mitad de año y verificar la inmediatez ante cambios.
2. Añadir **US3**: Reforzar el control de acceso en el servidor con tests negativos de autorización HTTP 403.
3. Añadir **US4**: Incorporar el selector de año y la consulta de ejercicios históricos cerrados.
4. Ejecutar **Phase 7: Polish** y validación final de accesibilidad y tests end-to-end.
