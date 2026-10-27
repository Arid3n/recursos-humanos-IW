# Implementation Plan: spec-001 — Alta de Empleados y Trayectoria Contractual

**Branch**: `spec-001` | **Date**: 2026-10-27 | **Spec**: [specs/spec-001/spec.md](spec.md)  
**Input**: Especificación HU-01 (clarificada con 5 decisiones cerradas)  

---

## 1. Summary

Implementar el módulo completo de alta de empleados y gestión de trayectoria contractual inmutable (*append-only* con fecha de efectos obligatoria), garantizando:
- Formulario de alta con validación exhaustiva (cliente y servidor) con reporte de errores por campo según RFC 9457.
- Generación segura de cuenta de usuario (`users`) y credencial provisional entregable al empleado sin servidor SMTP.
- Gestión de variaciones contractuales (puesto, departamento, salario, convenio y responsable) como registros históricos inmutables con fecha de efectos (presente, futura o retroactiva).
- Consulta cronológica de la trayectoria y baja lógica reversible (`active = false`).
- Autorización estricta reservada en exclusiva al rol `RRHH` (`@PreAuthorize("hasRole('RRHH')")`).

---

## 2. Technical Context

- **Language/Version**: Java 21 LTS (Backend) · TypeScript 5.9 (Frontend, `strict: true`).
- **Primary Dependencies**: Spring Boot 3.4.3 (Web, Security, Data JPA, Validation), Angular 21, Angular Material 22.
- **Storage**: PostgreSQL 16 (tabla `employees`, `contract_records`, `collective_agreements`, `users` gestionadas por Flyway).
- **Testing**: JUnit 5, Mockito, Spring Boot Test, **Testcontainers** (PostgreSQL real para persistencia, Art. 22), Vitest (Frontend).
- **Target Platform**: Aplicación Web SPA responsive (escritorio y móvil).
- **Project Type**: Monolito modular por capas desacopladas con contrato OpenAPI 3.0.
- **Performance Goals**: Tiempo de respuesta de endpoints de alta y consulta < 500ms; renderizado reactivo en UI < 800ms.
- **Constraints**: 0 datos sensibles en logs; cumplimiento WCAG 2.1 AA; errores estandarizados con RFC 9457 Problem Details.

---

## 3. Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Invariante Constitucional | Estado | Evidencia en el Diseño |
| :--- | :--- | :--- |
| **Art. 1 (Spec como verdad)** | **PASS** | Todas las funcionalidades derivan de HU-01 y sus 5 clarificaciones registradas. |
| **Art. 5 (4 capas desacopladas)** | **PASS** | Controladores delegan en servicios, servicios en repositorios, y frontend consume solo vía contrato OpenAPI. |
| **Art. 8 (PostgreSQL real)** | **PASS** | Tablas creadas en migración Flyway `V1__init_shared_schema.sql`; tests con Testcontainers PostgreSQL. |
| **Art. 10 (Autorización en servidor)**| **PASS** | `@PreAuthorize("hasRole('RRHH')")` en todos los endpoints de mutación contractual y de alta. |
| **Art. 11 (Historial inmutable)** | **PASS** | `ContractRecord` es *append-only*. Las consultas de vigencia filtran por `effective_from <= :date`. |
| **Art. 19 (Errores RFC 9457)** | **PASS** | `GlobalExceptionHandler` devuelve `ProblemDetail` con `invalidFields` mapeado a cada campo de formulario. |
| **Art. 21 (Accesibilidad WCAG AA)** | **PASS** | Uso de Angular Material con contraste accesible y mensajes de error asociados a inputs con `mat-error`. |
| **Art. 22 (Test-First con Testcontainers)** | **PASS** | Test de integración contra PostgreSQL real con tests negativos de rol incluidos. |

---

## 4. Project Structure

### Documentation (`specs/spec-001/`)
```text
specs/spec-001/
├── spec.md              # Especificación funcional clarificada
├── plan.md              # Este plan de implementación técnico
├── research.md          # Fase 0: Decisiones arquitectónicas y alternativas
├── data-model.md        # Fase 1: Mapeo de entidades JPA y repositorio
├── quickstart.md        # Fase 1: Guía de ejecución y tests
└── contracts/
    └── employee-api.yaml # Fase 1: Contrato OpenAPI 3.0
```

### Source Code (`backend/` & `frontend/`)
```text
backend/src/main/java/com/iw/rrhh/
├── employee/
│   ├── controller/
│   │   └── EmployeeController.java
│   ├── service/
│   │   ├── EmployeeService.java
│   │   ├── EmployeeServiceImpl.java
│   │   └── PasswordGeneratorService.java
│   ├── domain/
│   │   ├── Employee.java
│   │   ├── ContractRecord.java
│   │   └── CollectiveAgreement.java
│   ├── repository/
│   │   ├── EmployeeRepository.java
│   │   ├── ContractRecordRepository.java
│   │   └── CollectiveAgreementRepository.java
│   └── dto/
│       ├── CreateEmployeeRequest.java
│       ├── CreateEmployeeResponse.java
│       ├── CreateContractVariationRequest.java
│       ├── ContractHistoryItemResponse.java
│       └── EmployeeDetailResponse.java
└── common/exception/GlobalExceptionHandler.java

backend/src/test/java/com/iw/rrhh/employee/
├── EmployeeIntegrationTest.java      # Testcontainers PostgreSQL
└── EmployeeServiceTest.java           # Unit test de reglas de negocio

frontend/src/app/
├── features/employees/
│   ├── components/
│   │   ├── employee-create/           # Formulario reactivo de alta
│   │   ├── employee-detail/           # Detalle y variación contractual
│   │   ├── contract-history/          # Histórico cronológico
│   │   └── password-dialog/           # Modal de credencial provisional
│   ├── services/
│   │   └── employee.service.ts
│   └── models/
│       └── employee.models.ts
```

---

## 5. Complexity Tracking

> **Sin violaciones**: El diseño se ciñe estrictamente a los principios de simplicidad del Art. 23 (monolito modular, Spring Data JPA estándar, sin dependencias innecesarias de colas o caches).
