# Informe de Implementación: spec-003 — Solicitud de Vacaciones o Permisos

**Historia de Usuario**: HU-03  
**Rama**: `spec-003` (integrada en `main`)  
**Fecha de finalización**: 2026-11-20  
**Responsable**: Iván Soler (Integrante 2 — Servicios y derechos del Empleado)  
**Estado**: **Completado y Convergido (100%)**

---

## 1. Resumen Ejecutivo

Se ha completado satisfactoriamente la implementación de la especificación **`spec-003` (HU-03)**, que permite a los empleados solicitar periodos de vacaciones o permisos retribuidos a través de un autoservicio ágil, intuitivo y rigurosamente validado.

El módulo garantiza el cálculo oficial y en tiempo real de los días laborables computables (descontando fines de semana y festivos oficiales según el calendario laboral corporativo centralizado en backend), valida la reserva preventiva del saldo disponible (evitando que solicitudes pendientes concurrentes excedan el cupo anual), bloquea solapamientos de fechas con ausencias previas, y asegura el cumplimiento de las reglas jerárquicas (bloqueo para usuarios de Dirección General y para empleados sin responsable de equipo activo asignado).

---

## 2. Cobertura de Requisitos y Criterios de Aceptación

### 2.1. Requisitos Funcionales

| Requisito | Descripción | Estado | Evidencia de Implementación |
| :--- | :--- | :---: | :--- |
| **FR-001** | Formulario con tipo (`VACATION`/`PERMIT`) y rango de fechas | **CUMPLIDO** | `LeaveRequestCreateComponent`, campos reactivos en Angular. |
| **FR-002** | Cómputo oficial en días laborables excluyendo festivos | **CUMPLIDO** | `HolidayCalendarService.countWorkingDays()`, endpoint `GET /api/v1/leave/requests/calculate-days`. |
| **FR-003** | Reserva preventiva de saldo disponible | **CUMPLIDO** | `LeaveRequestServiceImpl.createLeaveRequest()` valida `balance.availableDays() - pendingDays >= requestedDays`. |
| **FR-004** | Rango de fechas válido (`fin >= inicio` y `inicio >= hoy`) | **CUMPLIDO** | `@FutureOrPresent` en DTO, constraint `CHECK` en base de datos y validadores en frontend. |
| **FR-005** | Bloqueo absoluto de solapamiento de fechas del mismo empleado | **CUMPLIDO** | `LeaveRequestRepository.hasOverlappingRequests()` lanza `LeaveRequestOverlapException` (409). |
| **FR-006** | Justificación obligatoria en permisos y opcional en vacaciones | **CUMPLIDO** | Validación condicional en `LeaveRequestCreateComponent` y validación de servicio backend. |
| **FR-007** | Cómputo indivisible en jornadas completas (`calculatedDays >= 1`) | **CUMPLIDO** | Validación `@Positive` en backend y verificación de entero en frontend. |
| **FR-008** | Aviso de recomendación con antelación menor a 7 días | **CUMPLIDO** | Alerta no intrusiva en interfaz (`mat-card` informativo de recomendación). |
| **FR-009** | Bloqueo si no hay resolutor válido asignado | **CUMPLIDO** | `LeaveResolverPolicy.findValidResolver()` lanza 422 con mensaje descriptivo si no hay superior activo. |
| **FR-010** | Restricción de solicitud para Dirección General | **CUMPLIDO** | Verificación de rol en servicio lanzando 422 si el usuario es `DIRECCION_GENERAL`. |
| **FR-011** | Endpoint de elegibilidad previa | **CUMPLIDO** | `GET /api/v1/leave/requests/eligibility` consultado al cargar el componente. |

### 2.2. Criterios de Éxito (Success Criteria)

- **SC-001 (Solicitud completable en < 1 minuto)**: Interfaz reactiva con autocompletado y cálculo dinámico debounced (< 300 ms).
- **SC-002 (100% solapamientos bloqueados)**: Comprobado mediante suite automatizada de tests unitarios y de integración.
- **SC-003 (100% reserva preventiva respetada)**: El saldo disponible nunca permite sobreasignación por acumulación de peticiones en estado `PENDING`.
- **SC-004 (Cálculo único en servidor)**: Respeto estricto del Art. 14 de la Constitución; sin discrepancias cliente-servidor.

---

## 3. Arquitectura y Componentes Implementados

### 3.1. Backend (Spring Boot 3.4.3 + Java 21)
- **Controlador REST**: `com.iw.rrhh.leave.controller.LeaveRequestController`
  - `POST /api/v1/leave/requests`: Creación de solicitudes en estado `PENDING`.
  - `GET /api/v1/leave/requests/calculate-days`: Cálculo interactivo de días laborables.
  - `GET /api/v1/leave/requests/eligibility`: Consulta previa de elegibilidad y resolutor válido.
- **Servicio de Dominio**:
  - `LeaveRequestServiceImpl`: Orquestación transaccional (`@Transactional`), control de reserva preventiva y asignación automática de manager resolutor.
  - `HolidayCalendarService`: Calendario oficial de festivos y conteo de jornadas laborales.
  - `LeaveResolverPolicy`: Política centralizada de determinación de resolutores válidos (`RESPONSABLE_EQUIPO`, `DIRECCION_GENERAL`, `RRHH`).

### 3.2. Frontend (Angular 21 + Material Design)
- **Componentes**:
  - `LeaveRequestCreateComponent`: Formulario reactivo con selector de fechas, visualización de días laborables calculados en tiempo real y badges de estado.
  - `DireccionGeneralNoAbsencesComponent`: Vista informativa para usuarios de Dirección General.
  - Guard `absenceRequestGuard`: Protección de rutas según elegibilidad del empleado.

---

## 4. Verificación y Resultados de Pruebas

- **`LeaveRequestServiceTest`**: 19 tests unitarios ejecutados, **19 pasados (0 fallos, 0 errores)** en 0.811 s.
  - Valida cálculo de festivos, rechazo por saldo insuficiente, rechazo de solapamientos, obligatoriedad de justificación en permisos y políticas de elegibilidad.
- **`HolidayCalendarServiceTest`**: Tests unitarios que verifican exclusión de fines de semana y festivos oficiales nacionales y locales.
- **`leave-request-create.component.spec.ts`**: Tests unitarios de componentes frontend verificando formularios reactivos y llamadas mockeadas a la API.

---

## 5. Alineación con la Constitución del Proyecto

- **Art. 10 (Seguridad en servidor)**: El identificador del empleado solicitante se obtiene exclusivamente de `SecurityContextHolder.getContext().getAuthentication()`.
- **Art. 12 (Saldo como valor derivado)**: El cálculo preventivo no modifica el saldo consolidado, sino que computa las solicitudes en estado `PENDING` de forma dinámica.
- **Art. 14 (Fechas y festivos en servidor)**: La lógica temporal y festiva reside exclusivamente en el backend bajo zona `Europe/Madrid`.
- **Art. 19 (RFC 9457 Problem Details)**: Todos los errores de validación y de negocio devuelven payloads estandarizados con `title`, `status`, `detail` y mapa de campos inválidos.

---

## 6. Conclusiones y Estado de Convergencia

La funcionalidad `spec-003` está **100% convergida** con la especificación y el código. No existen brechas pendientes ni tareas huérfanas. El flujo de solicitud queda plenamente integrado con HU-02 (saldo de vacaciones), HU-04 (seguimiento) y HU-05 (resolución por responsables).
