# Implementation Plan: spec-003 — Solicitud de Vacaciones o Permisos

**Branch**: `spec-003` | **Date**: 2026-11-01 | **Spec**: [specs/spec-003/spec.md](spec.md)  
**Input**: Especificación HU-03 (clarificada con 4 decisiones cerradas)  

---

## 1. Summary

Implementar el módulo completo para la solicitud de ausencias (vacaciones o permisos) por parte del empleado, garantizando:
- Previsualización interactiva del cómputo oficial de días laborables calculados estrictamente por el servidor (`HolidayCalendarService`), descontando fines de semana y festivos oficiales (Art. 14, FR-002).
- Creación segura de la solicitud (`POST /api/v1/leave/requests`) naciendo exclusivamente con estado `PENDING` y asignada automáticamente al responsable del empleado (Art. 13, FR-003, FR-006).
- Validación estricta contra el saldo disponible de vacaciones en ausencias de tipo `VACATION`, reutilizando el motor dinámico de HU-02 (`LeaveBalanceService`), e independencia de bolsa para ausencias de tipo `PERMIT` (Art. 12, FR-004, FR-008).
- Bloqueo preventivo y automático ante solapamiento de fechas con cualquier otra solicitud previa del empleado en estado `PENDING` o `APPROVED` (FR-007, SC-002).
- Formulario reactivo en Angular 21 con Angular Material, datepicker de rango, aviso informativo de antelación (< 7 días) y accesibilidad WCAG 2.1 AA (Art. 21, SC-001).

---

## 2. Technical Context

- **Language/Version**: Java 21 LTS (Backend) · TypeScript 5.9 (Frontend, `strict: true`).
- **Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA, Validation), Angular 21, Angular Material 22.
- **Storage**: PostgreSQL 16 (tabla `leave_requests`, `employees`, `users` gestionadas por Flyway `V1__init_shared_schema.sql`).
- **Testing**: JUnit 5, Mockito, Spring Boot Test, **Testcontainers** (PostgreSQL real para persistencia, Art. 8, Art. 22), Vitest (Frontend).
- **Target Platform**: Aplicación Web SPA responsive (escritorio y móvil).
- **Project Type**: Monolito modular por capas desacopladas con contrato OpenAPI 3.0.
- **Performance Goals**: Tiempo de respuesta de previsualización de días laborables < 300ms (SC-001); creación de solicitud < 500ms; renderizado en UI < 500ms.
- **Constraints**: 0 cómputos temporales en cliente (Art. 14); 100% de solapamientos bloqueados (SC-002); errores con RFC 9457 Problem Details (Art. 19); cumplimiento WCAG 2.1 AA (Art. 21).

---

## 3. Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Invariante Constitucional | Estado | Evidencia en el Diseño |
| :--- | :--- | :--- |
| **Art. 1 (Spec como verdad)** | **PASS** | Todas las funcionalidades y validaciones derivan estrictamente de HU-03 y sus clarificaciones cerradas. |
| **Art. 5 (4 capas desacopladas)** | **PASS** | `LeaveRequestController` delega en `LeaveRequestService`, este en repositorios JPA, y el frontend interactúa únicamente a través del contrato OpenAPI. |
| **Art. 8 (PostgreSQL real)** | **PASS** | Se apoya en la tabla `leave_requests` de Flyway con `CHECK (end_date >= start_date)`. Tests de integración con Testcontainers PostgreSQL. |
| **Art. 10 (Autorización en servidor)** | **PASS** | La identidad del empleado solicitante se obtiene siempre de `SecurityContextHolder` en la sesión, sin confiar en identificadores recibidos del cliente. |
| **Art. 12 (Saldo es dato derivado)** | **PASS** | Se invoca `LeaveBalanceService.getMyLeaveBalance` para verificar si hay saldo suficiente sin crear contadores desacoplados. |
| **Art. 13 (Ciclo de vida y atomicidad)** | **PASS** | Toda solicitud nace estrictamente en estado `PENDING`, con versión para concurrencia optimista y vinculación automática al responsable. |
| **Art. 14 (Fechas y cómputo de días)** | **PASS** | El cálculo de días hábiles reside exclusivamente en el backend (`HolidayCalendarService`) y es consumido por el frontend vía API. |
| **Art. 19 (Errores RFC 9457)** | **PASS** | Errores de validación, fechas inválidas, saldo insuficiente o solapamientos devuelven `ProblemDetail` estructurado. |
| **Art. 21 (Accesibilidad WCAG AA)** | **PASS** | Formulario con Angular Material, soporte para teclado, contraste adecuado y mensajes de error asociados mediante `mat-error`. |
| **Art. 22 (Test-First con Testcontainers)** | **PASS** | Pruebas de integración sobre PostgreSQL real con verificación de solapes, saldo insuficiente y concurrencia. |
| **Art. 23 (Simplicidad y proporcionalidad)**| **PASS** | Sin servicios externos complejos; cálculo en memoria de días y consulta SQL optimizada por índices. |

---

## 4. Project Structure

### Documentation (`specs/spec-003/`)
```text
specs/spec-003/
├── spec.md              # Especificación funcional clarificada
├── plan.md              # Este plan de implementación técnico (/speckit-plan)
├── research.md          # Fase 0: Decisiones arquitectónicas y alternativas (/speckit-plan)
├── data-model.md        # Fase 1: Mapeo de entidades JPA, DTOs y repositorio (/speckit-plan)
├── quickstart.md        # Fase 1: Guía de ejecución y validación (/speckit-plan)
├── contracts/
│   └── leave-request-api.yaml # Fase 1: Contrato OpenAPI 3.0 (/speckit-plan)
└── tasks.md             # Fase 2: Lista de tareas (/speckit-tasks)
```

### Source Code (`backend/` & `frontend/`)
```text
backend/src/main/java/com/iw/rrhh/
├── common/
│   └── service/
│       └── HolidayCalendarService.java       # Cómputo de días hábiles y festivos
└── leave/
    ├── controller/
    │   └── LeaveRequestController.java       # POST /requests y GET /requests/calculate-days
    ├── service/
    │   ├── LeaveRequestService.java
    │   └── LeaveRequestServiceImpl.java      # Validaciones, solapes, saldo y creación
    ├── repository/
    │   └── LeaveRequestRepository.java       # Añadir hasOverlappingRequests
    └── dto/
        ├── CreateLeaveRequest.java
        ├── LeaveRequestResponse.java
        └── WorkingDaysCalculationResponse.java

frontend/src/app/features/leave/
├── components/
│   └── leave-request-create/
│       ├── leave-request-create.component.ts
│       ├── leave-request-create.component.html
│       ├── leave-request-create.component.scss
│       └── leave-request-create.component.spec.ts
├── models/
│   └── leave.models.ts                       # DTOs en TypeScript
└── services/
    └── leave.service.ts                      # Métodos HTTP calculateWorkingDays y createLeaveRequest
```

---

## 5. Execution Plan (Next Steps)

1. **Fase 0 y Fase 1 completadas** mediante `/speckit-plan`:
   - [x] `research.md` (Decisiones técnicas y alternativas evaluadas).
   - [x] `data-model.md` (Entidades, DTOs y contratos de persistencia).
   - [x] `contracts/leave-request-api.yaml` (Contrato OpenAPI 3.0 con RFC 9457).
   - [x] `quickstart.md` (Escenarios de validación automatizada y pruebas manuales).
2. **Siguiente paso**: Ejecutar `/speckit-tasks` para generar `tasks.md` con las tareas atómicas y dependencias de implementación.
