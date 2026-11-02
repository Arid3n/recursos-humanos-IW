# Implementation Plan: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Branch**: `spec-004` | **Date**: 2026-11-01 | **Spec**: [specs/spec-004/spec.md](spec.md)

**Input**: Feature specification from `/specs/spec-004/spec.md`

---

## Summary

La funcionalidad **HU-04 (`spec-004`)** permite a cada empleado autenticado realizar un seguimiento completo y transparente de sus solicitudes de vacaciones y permisos. Implementa un listado paginado y ordenado cronológicamente (`created_at DESC`), con filtros por estado (`Todas`, `Pendientes`, `Aprobadas`, `Rechazadas`), visualización del motivo en solicitudes denegadas y la capacidad de cancelar solicitudes propias en estado `PENDING` liberando el periodo sin penalización, con estricto aislamiento de datos garantizado en el servidor (Art. 10 de la Constitución).

---

## Technical Context

**Language/Version**: Java 21 LTS (Backend) · TypeScript 5.7+ / Angular 21 (Frontend)  
**Primary Dependencies**: Spring Boot 3.4.0 (Spring Security, Spring Data JPA, Jakarta Validation) · Angular Material 21  
**Storage**: PostgreSQL 16 (`leave_requests` table)  
**Testing**: JUnit 5 + Mockito + MockMvc (Backend) · Vitest + Angular Testing Library (Frontend)  
**Target Platform**: Navegadores modernos (Desktop/Tablet) · API REST en contenedor Linux  
**Project Type**: Aplicación web SPA + Servicio REST desacoplado (Art. 5)  
**Performance Goals**: Listado renderizado en < 500ms (SC-001) · Filtros reactivos instantáneos (SC-002)  
**Constraints**: RFC 9457 ProblemDetail (Art. 19) · WCAG 2.1 AA (Art. 21) · Autorización en servidor por identidad de sesión (Art. 10)  
**Scale/Scope**: Paginación estándar de 10 registros por página, navegación accesible y confirmación modal de cancelación  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Art. 1 (Especificación como fuente de verdad)**: Plan fundamentado exclusivamente en los 5 escenarios y requisitos de `spec.md`.
- [x] **Art. 2 (Trazabilidad HU → CA → Test)**: Cada criterio de aceptación tiene mapeado al menos un test unitario o de integración (`HU-04.CA-1` a `HU-04.CA-5`).
- [x] **Art. 5 (Arquitectura en capas)**: Cuatro capas desacopladas (Presentación Angular → Contrato OpenAPI → Dominio/Servicio Spring Boot → Persistencia PostgreSQL).
- [x] **Art. 9 (API REST y Contratos)**: `GET /api/v1/leave/my-requests` paginado y `POST /api/v1/leave/requests/{id}/cancel` documentados en `contracts/leave-tracking-api.yaml`.
- [x] **Art. 10 (Autorización en el servidor)**: El empleado se obtiene exclusivamente de `Principal` en sesión. No se confía en ningún parámetro externo. Se incluye test negativo para acceso denegado (`403 Forbidden`).
- [x] **Art. 13 (Ciclo de vida de la solicitud)**: Solo solicitudes en `PENDING` son cancelables. Si la solicitud está `APPROVED` o `REJECTED`, se rechaza con `409 Conflict`.
- [x] **Art. 19 (Problem Details RFC 9457)**: Todas las respuestas de error (`403`, `404`, `409`) usan `ProblemDetail`.
- [x] **Art. 21 (Accesibilidad WCAG 2.1 AA)**: Badges de estado con iconografía y texto, diálogos con foco accesible y controles navegables por teclado.

---

## Project Structure

### Documentation (this feature)

```text
specs/spec-004/
├── plan.md              # Este plan de implementación
├── research.md          # Fase 0: Decisiones técnicas y justificaciones
├── data-model.md        # Fase 1: Entidades, DTOs y reglas de negocio
├── quickstart.md        # Fase 1: Guía de validación y escenarios de prueba
├── contracts/           # Fase 1: Contrato OpenAPI (leave-tracking-api.yaml)
└── tasks.md             # Fase 2: Lista de tareas ordenada (generada con speckit-tasks)
```

### Source Code

```text
backend/src/
├── main/java/com/iw/rrhh/
│   ├── common/
│   │   ├── dto/PageResponse.java
│   │   └── exception/LeaveRequestNotCancellableException.java
│   └── leave/
│       ├── controller/LeaveRequestController.java
│       ├── dto/LeaveRequestSummaryResponse.java
│       ├── repository/LeaveRequestRepository.java
│       └── service/
│           ├── LeaveRequestService.java
│           └── LeaveRequestServiceImpl.java
└── test/java/com/iw/rrhh/leave/
    ├── LeaveRequestTrackingTest.java
    └── LeaveTrackingIntegrationTest.java

frontend/src/app/features/leave/
├── components/
│   ├── leave-request-list/
│   │   ├── leave-request-list.component.ts
│   │   ├── leave-request-list.component.html
│   │   ├── leave-request-list.component.scss
│   │   └── leave-request-list.component.spec.ts
│   └── rejection-reason-dialog/
│       └── rejection-reason-dialog.component.ts
├── models/leave.models.ts
└── services/leave.service.ts
```

---

## Planned Phases

1. **Fase 0 & 1**: Planificación y Diseño (Completada: `research.md`, `data-model.md`, `contracts/`, `quickstart.md`).
2. **Fase 2**: Desglose y dependencias en `tasks.md`.
3. **Fase 3**: Backend — DTOs, métodos en `LeaveRequestRepository`, lógica en `LeaveRequestService` (`getMyRequests`, `cancelRequest`), endpoint en `LeaveRequestController` y tests unitarios / integración.
4. **Fase 4**: Frontend — Modelos, métodos en `LeaveService`, componente `LeaveRequestListComponent` con tabla Angular Material, filtros de estado, diálogo de confirmación de cancelación, diálogo de motivo de rechazo y tests unitarios.
5. **Fase 5**: Integración, navegación (`/leave/my-requests`), validación cruzada y merge en `main`.
