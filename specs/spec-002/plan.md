# Implementation Plan: spec-002 — Consulta de Ficha y Saldo de Vacaciones

**Branch**: `spec-002` | **Date**: 2026-10-29 | **Spec**: [specs/spec-002/spec.md](spec.md)  
**Input**: Especificación HU-02 (clarificada con 4 decisiones cerradas)  

---

## 1. Summary

Implementar el módulo completo de consulta de ficha personal y saldo de vacaciones desglosado (correspondientes, disfrutados, disponibles y en trámite) garantizando:
- Consulta de ficha y saldo del usuario autenticado mediante contexto de seguridad del servidor (`SecurityContextHolder`), sin confiar en parámetros enviados por el cliente (Art. 10, FR-001).
- Saldo estrictamente derivado y calculado en tiempo real desde la fuente de verdad (convenio colectivo vigente en `contract_records` y solicitudes en `leave_requests`), sin contadores editables manualmente ni riesgo de desfases (Art. 12, FR-003, FR-004).
- Soporte de prorrateo para incorporaciones a mitad de año y consulta histórica de ejercicios pasados en modo solo lectura (FR-006, FR-007).
- Protección estricta de alcance: un empleado no puede acceder a la ficha ni saldo de otro empleado, devolviendo `403 Forbidden` (Art. 10, FR-005).
- Interfaz reactiva en Angular 21 con componentes Angular Material accesibles (WCAG 2.1 AA, Art. 21).

---

## 2. Technical Context

- **Language/Version**: Java 21 LTS (Backend) · TypeScript 5.9 (Frontend, `strict: true`).
- **Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA, Validation), Angular 21, Angular Material 22.
- **Storage**: PostgreSQL 16 (tablas `employees`, `contract_records`, `collective_agreements`, `leave_requests`, `users` gestionadas por Flyway).
- **Testing**: JUnit 5, Mockito, Spring Boot Test, **Testcontainers** (PostgreSQL real para persistencia, Art. 22), Vitest (Frontend).
- **Target Platform**: Aplicación Web SPA responsive (escritorio y móvil).
- **Project Type**: Monolito modular por capas desacopladas con contrato OpenAPI 3.0.
- **Performance Goals**: Tiempo de respuesta de consulta de perfil y saldo < 500ms; renderizado reactivo en UI < 800ms (SC-001).
- **Constraints**: 0 datos sensibles en logs (Art. 20); cumplimiento WCAG 2.1 AA (Art. 21); errores con RFC 9457 Problem Details (Art. 19); coherencia matemática 100% (SC-002).

---

## 3. Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Invariante Constitucional | Estado | Evidencia en el Diseño |
| :--- | :--- | :--- |
| **Art. 1 (Spec como verdad)** | **PASS** | Todas las funcionalidades derivan de HU-02 y sus 4 clarificaciones registradas. |
| **Art. 5 (4 capas desacopladas)** | **PASS** | Controladores (`LeaveBalanceController`, `EmployeeProfileController`) delegan en servicios, servicios en repositorios, y frontend consume vía contrato OpenAPI. |
| **Art. 8 (PostgreSQL real)** | **PASS** | Uso de tablas existentes migadas en Flyway `V1__init_shared_schema.sql`; consultas agregadas SQL para sumatorios; tests de persistencia con Testcontainers PostgreSQL. |
| **Art. 10 (Autorización en servidor)** | **PASS** | Identidad del solicitante obtenida de `SecurityContextHolder` en endpoints `/me`. Verificación de alcance por rol/equipo en endpoints con ID explícito con rechazo 403. Tests negativos obligatorios. |
| **Art. 12 (Saldo es dato derivado)** | **PASS** | No existe columna de saldo persistido en BD. El saldo se calcula dinámicamente sumando días de convenio y restando días de solicitudes aprobadas. |
| **Art. 14 (Fechas y cómputo de días)** | **PASS** | Uso de `LocalDate` y `DATE` en PostgreSQL; cómputo de año natural (1 de enero a 31 de diciembre) centralizado en `LeaveBalanceService`. |
| **Art. 19 (Errores RFC 9457)** | **PASS** | Los accesos denegados o recursos inexistentes se mapean en `GlobalExceptionHandler` con `ProblemDetail`. |
| **Art. 21 (Accesibilidad WCAG AA)** | **PASS** | Angular Material con etiquetas semánticas, contraste suficiente y no transmisión de estados exclusivamente por color. |
| **Art. 22 (Test-First con Testcontainers)** | **PASS** | Suite de pruebas unitarias y de integración contra PostgreSQL real, incluyendo pruebas de prorrateo y tests negativos de autorización. |
| **Art. 23 (Simplicidad y proporcionalidad)** | **PASS** | Monolito modular estándar, sin colas de mensajes ni caches innecesarias. |

---

## 4. Project Structure

### Documentation (`specs/spec-002/`)
```text
specs/spec-002/
├── spec.md              # Especificación funcional clarificada
├── plan.md              # Este plan de implementación técnico (/speckit-plan)
├── research.md          # Fase 0: Decisiones arquitectónicas y alternativas (/speckit-plan)
├── data-model.md        # Fase 1: Mapeo de entidades JPA, DTOs y repositorio (/speckit-plan)
├── quickstart.md        # Fase 1: Guía de ejecución y tests (/speckit-plan)
├── contracts/
│   └── leave-balance-api.yaml # Fase 1: Contrato OpenAPI 3.0 (/speckit-plan)
└── tasks.md             # Fase 2: Lista ordenada de tareas (/speckit-tasks)
```

### Source Code (`backend/` & `frontend/`)
```text
backend/src/main/java/com/iw/rrhh/
├── employee/
│   ├── controller/
│   │   └── EmployeeProfileController.java     # Endpoint GET /api/v1/employees/me
│   └── dto/
│       └── EmployeeProfileResponse.java
├── leave/
│   ├── controller/
│   │   └── LeaveBalanceController.java        # Endpoints /api/v1/leave/balances/* y /requests/me
│   ├── service/
│   │   ├── LeaveBalanceService.java           # Interfaz de cálculo de saldo
│   │   └── LeaveBalanceServiceImpl.java       # Implementación de cálculo y prorrateo
│   ├── domain/
│   │   ├── LeaveRequest.java                  # Entidad JPA
│   │   ├── LeaveType.java                     # Enum VACATION, PERMIT
│   │   ├── LeaveStatus.java                   # Enum PENDING, APPROVED, REJECTED
│   │   └── LeaveBalance.java                  # Value Object inmutable de dominio
│   ├── repository/
│   │   └── LeaveRequestRepository.java        # Consultas de agregación SQL
│   └── dto/
│       ├── LeaveBalanceResponse.java
│       └── LeaveSummaryItemResponse.java
└── common/exception/GlobalExceptionHandler.java

backend/src/test/java/com/iw/rrhh/leave/
├── LeaveBalanceServiceTest.java               # Tests unitarios de cálculo y prorrateo
└── LeaveBalanceIntegrationTest.java           # Tests de integración y seguridad negativa (Testcontainers)

frontend/src/app/
├── features/
│   ├── employees/
│   │   └── components/
│   │       └── my-profile/                    # Ficha personal del empleado
│   └── leave/
│       ├── components/
│       │   └── leave-balance-widget/          # Widget de métricas de saldo y selector de año
│       ├── services/
│       │   └── leave.service.ts               # Cliente HTTP para saldo y solicitudes
│       └── models/
│           └── leave.models.ts                # Interfaces TypeScript del contrato
```

---

## 5. Complexity Tracking

> **Sin violaciones**: El diseño respeta íntegramente los principios del Art. 23 (solución directa y simple mediante cálculo en memoria y consultas agregadas JPA/SQL, sin caches distribuidas ni estructuras de datos redundantes).
