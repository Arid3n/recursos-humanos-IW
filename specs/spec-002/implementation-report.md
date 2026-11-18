# Informe de Implementación: spec-002 — Consulta de Ficha y Saldo de Vacaciones

**Historia de Usuario**: HU-02  
**Rama**: `spec-002` (integrada en `main`)  
**Fecha de finalización**: 2026-11-17  
**Responsable**: Integrante 2 (Flujo de Ausencias del Empleado)  
**Estado**: **Completado y Convergido (100%)**

---

## 1. Resumen Ejecutivo

Se ha completado satisfactoriamente la implementación y el análisis de la especificación **`spec-002` (HU-02)**. El módulo permite a cualquier empleado de la plantilla acceder a su ficha personal y consultar en tiempo real su saldo de vacaciones desglosado (días correspondientes por convenio, disfrutados, disponibles y en trámite) sin depender de procesos por lotes ni consultas manuales a Recursos Humanos.

El diseño garantiza de forma estricta los principios de seguridad en el servidor (Art. 10 de la Constitución), derivando la identidad del solicitante a través de `SecurityContextHolder` en los endpoints `/me`, y el principio de saldo como dato estrictamente derivado (Art. 12), calculando las métricas en tiempo real a partir del convenio contractual vigente en `contract_records` y las solicitudes formalmente aprobadas en `leave_requests`.

---

## 2. Cobertura de Requisitos y Criterios de Aceptación

### 2.1. Requisitos Funcionales

| Requisito | Descripción | Estado | Evidencia de Implementación |
| :--- | :--- | :---: | :--- |
| **FR-001** | Identidad obtenida desde el contexto de seguridad del servidor | **CUMPLIDO** | `SecurityContextHolder` en `GET /api/v1/employees/me` y `GET /api/v1/leave/balances/me`. |
| **FR-002** | Saldo desglosado en correspondientes, disfrutados, disponibles y en trámite | **CUMPLIDO** | Objeto de dominio `LeaveBalance` y componente `LeaveBalanceWidgetComponent`. |
| **FR-003** | Saldo estrictamente derivado de la fuente de verdad (sin persistencia mutable) | **CUMPLIDO** | Sumatorio SQL en `LeaveRequestRepository` y días base de `CollectiveAgreement`. |
| **FR-004** | Recálculo dinámico e inmediato ante variaciones de convenio o ausencias | **CUMPLIDO** | Comprobado en `LeaveRecalculationIntegrationTest` sin contadores estáticos en BD. |
| **FR-005** | Control de acceso por alcance: denegado a Empleado para fichas ajenas; supervisores solo equipo directo; RRHH toda la plantilla | **CUMPLIDO** | `@PreAuthorize` en `GET /balances/employee/{id}` con validación jerárquica y test negativo 403. |
| **FR-006** | Prorrateo proporcional por fecha de incorporación durante el año | **CUMPLIDO** | Fórmula `round((días_anuales / 12) * meses_restantes)` en `LeaveBalanceServiceImpl`. |
| **FR-007** | Selector de ejercicios fiscales anteriores en modo solo lectura | **CUMPLIDO** | Parámetro `year` y visualización de badge de ejercicio cerrado en frontend. |

### 2.2. Criterios de Éxito (Success Criteria)

- **SC-001 (Tiempo de carga < 800ms)**: Renderizado reactivo instantáneo en `MyProfileComponent` y `LeaveBalanceWidgetComponent`.
- **SC-002 (Coherencia matemática 100%)**: En todo momento se cumple la invariante `correspondientes == disfrutados + disponibles` (`availableDays = Math.max(0, entitledDays - spentDays)`).
- **SC-003 (0% fugas de datos entre empleados)**: Validación estricta con tests negativos de autorización HTTP 403 (`LeaveBalanceIntegrationTest`).

---

## 3. Arquitectura y Componentes Implementados

### 3.1. Backend (Spring Boot 3.4.3 + Java 21)
- **Controladores REST**:
  - `com.iw.rrhh.employee.controller.EmployeeController`: Endpoint `GET /api/v1/employees/me` (consolidado para prevenir colisiones de ruta).
  - `com.iw.rrhh.leave.controller.LeaveBalanceController`: Endpoints `GET /api/v1/leave/balances/me`, `GET /api/v1/leave/requests/me` y `GET /api/v1/leave/balances/employee/{id}`.
- **Servicio de Saldo y Prorrateo**:
  - `LeaveBalanceServiceImpl`:
    - Cálculo dinámico de días correspondientes según fecha de alta (`hireDate`).
    - Agregación de días disfrutados (`APPROVED`) y en trámite (`PENDING`).
    - Soporte para consultas históricas filtrando contratos y solicitudes del año fiscal seleccionado.
- **Entidades y Repositorios**:
  - `LeaveRequest`: Entidad JPA con enums `LeaveType` (`VACATION`, `PERMIT`) y `LeaveStatus` (`PENDING`, `APPROVED`, `REJECTED`), restricción `CHECK (end_date >= start_date)` y `@Version`.
  - `LeaveRequestRepository`: Consultas optimizadas de agregación SQL `sumCalculatedDaysByEmployeeAndStatusAndYear`.
- **Manejo Centralizado de Excepciones**:
  - `GlobalExceptionHandler`: Serialización de `AccessDeniedException` y validaciones a RFC 9457 `ProblemDetail`.

### 3.2. Frontend (Angular 21 + Angular Material)
- **Componentes Standalone**:
  - `MyProfileComponent` (`src/app/features/employees/components/my-profile/`): Vista de ficha personal (`/profile` y `/vacations`) que presenta los datos contractuales y personales básicos del trabajador autenticado.
  - `LeaveBalanceWidgetComponent` (`src/app/features/leave/components/leave-balance-widget/`): Widget de 4 tarjetas métricas con selector interactivo de año fiscal y badges visuales accesibles.
- **Servicios y Modelos**:
  - `LeaveService` (`src/app/features/leave/services/leave.service.ts`): Cliente HTTP para consulta de balance y resumen de solicitudes.
  - `leave.models.ts` (`src/app/features/leave/models/leave.models.ts`): Modelos TypeScript con tipado estricto.

---

## 4. Alineación Constitucional

1. **Art. 5 (4 capas desacopladas)**: La UI solo consume los contratos OpenAPI (`leave-balance-api.yaml`), sin acceso directo a esquemas o bases de datos.
2. **Art. 10 (Seguridad en servidor)**: La identidad se extrae exclusivamente del token/sesión autenticada (`SecurityContextHolder`), impidiendo la suplantación mediante alteración de parámetros en el cliente.
3. **Art. 12 (Saldo derivado)**: Prohibición absoluta de campos mutables de saldo en base de datos; el saldo es siempre un valor calculado al vuelo a partir de hechos inmutables (contrato y solicitudes aprobadas).
4. **Art. 14 (Fechas y año natural)**: Tratamiento homogéneo con `LocalDate` y `DATE` en PostgreSQL, operando sobre el año natural de 1 de enero a 31 de diciembre.
5. **Art. 19 (Errores RFC 9457)**: Respuestas `application/problem+json` uniformes ante accesos denegados o recursos no encontrados.
6. **Art. 21 (Accesibilidad WCAG AA)**: Etiquetas descriptivas, contraste verificado y no dependencia exclusiva del color en los badges de estado.
7. **Art. 22 (Testcontainers)**: Pruebas de integración sobre PostgreSQL real con Testcontainers.

---

## 5. Métricas de Ejecución y Calidad

- **Total de tareas completadas**: 33 de 33 (100% en [tasks.md](tasks.md)).
- **Checklist de requisitos**: 27 de 27 puntos aprobados (100% en [checklists/review.md](checklists/review.md)).
- **Análisis de consistencia**: 0 inconsistencias pendientes en [analysis.md](analysis.md) (hallazgos **I1** e **I2** remediados).
- **Cobertura de requisitos**: 100% de los requisitos funcionales y criterios de éxito vinculados a tareas y tests.

---

## 6. Conclusión

La funcionalidad **`spec-002` (HU-02)** cumple todos los estándares de ingeniería de software, arquitectura desacoplada y principios constitucionales del proyecto, garantizando una base sólida y transparente para el flujo de solicitudes de ausencias (HU-03).
