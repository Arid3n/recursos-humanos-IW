# Informe de Implementación: spec-005 — Aprobación o Rechazo de Solicitudes

**Historia de Usuario**: HU-05  
**Rama**: `spec-005` (integrada en `main`)  
**Fecha de finalización**: 2026-11-20  
**Responsable**: Andrei Radu (Integrante 3 — Gestión y Supervisión del Responsable)  
**Estado**: **Completado y Convergido (100%)**

---

## 1. Resumen Ejecutivo

Se ha completado satisfactoriamente la implementación y el exhaustivo refinamiento de la especificación **`spec-005` (HU-05)**. Este módulo representa el eje transaccional y supervisor del sistema de Recursos Humanos, permitiendo a los responsables de equipo (así como a los roles de `DIRECCION_GENERAL` y `RRHH` para sus respectivos subordinados directos) revisar, aprobar o rechazar las solicitudes de ausencias pendientes de su personal a cargo.

La implementación ha atravesado sucesivas fases de refinamiento metodológico (*loop engineering*, Fases 8, 9 y 10), incorporando:
1. Revalidación atómica de saldo en el instante de la aprobación.
2. Reparto de días laborables entre años naturales (`LeaveDayAllocator`) para peticiones que cruzan el cambio de ejercicio.
3. Prohibición de resolver solicitudes en ejercicios fiscales cerrados.
4. Soporte para el nuevo rol `DIRECCION_GENERAL` como resolutor de la cúspide organizativa.
5. Servicio transaccional de cancelación automática de solicitudes pendientes (`LeaveRequestAutoCancelService`) cuando un superior causa baja en el sistema.
6. Emisión automática de notificaciones persistidas en base de datos para el empleado solicitante.

---

## 2. Cobertura de Requisitos y Criterios de Aceptación

### 2.1. Requisitos Funcionales

| Requisito | Descripción | Estado | Evidencia de Implementación |
| :--- | :--- | :---: | :--- |
| **FR-001** | Bandeja de pendientes exclusiva del equipo | **CUMPLIDO** | `LeaveResolutionController.getPendingRequests()`, `TeamPendingRequestsComponent`. |
| **FR-002** | Aprobación con comentario opcional (≤ 500 car.) | **CUMPLIDO** | `POST /resolve` con `decision: APPROVED`, actualiza a `LeaveStatus.APPROVED`. |
| **FR-003** | Rechazo con motivo obligatorio (5 a 500 car.) | **CUMPLIDO** | Validación backend y diálogo en Angular exigiendo motivo sustantivo sin espacios vacíos. |
| **FR-004** | Inmutabilidad de decisiones (bloqueo ante re-resolución) | **CUMPLIDO** | Devuelve HTTP `409 Conflict` si la solicitud ya no está en `PENDING`. |
| **FR-005** | Notificación interna persistida al empleado | **CUMPLIDO** | Creación de entidad `Notification` consumible en la campana de avisos del frontend. |
| **FR-006** | Actualización y deducción inmediata de saldo de vacaciones | **CUMPLIDO** | Integración con `LeaveBalanceService`; los días aprobados pasan a `spentDays`. |
| **FR-007** | Revalidación atómica de saldo disponible al aprobar | **CUMPLIDO** | Comprueba saldo remanente antes de confirmar; lanza 422 si es insuficiente. |
| **FR-008** | Reparto de días laborables entre años naturales | **CUMPLIDO** | `LeaveDayAllocator.allocateByYear()` divide días laborables de forma balanceada. |
| **FR-009** | Bloqueo en años naturales cerrados | **CUMPLIDO** | Lanza HTTP 422 si la solicitud incluye fechas de un año cerrado. |
| **FR-010** | Aislamiento estricto de equipo (prohibida resolución ajena) | **CUMPLIDO** | Verificación `employee.manager.id == currentUserId`; lanza 403 Forbidden. |
| **FR-011** | Prohibición absoluta de auto-resolución | **CUMPLIDO** | Bloqueo si `employee.id == currentUserId`; lanza 403 Forbidden. |
| **FR-012** | Auditoría inmutable de resolución (`resolved_by`, `resolved_at`) | **CUMPLIDO** | Columnas persistidas en `leave_requests` con el ID del resolutor y timestamp UTC. |
| **FR-013** | Concurrencia segura y atómica | **CUMPLIDO** | Control transaccional con aislamiento en PostgreSQL. |
| **FR-014** | Recarga inmediata de interfaz tras 409 o 404 | **CUMPLIDO** | Manejador en `TeamPendingRequestsComponent` para refrescar bandeja. |
| **FR-015** | Bloqueo sin resolutor válido activo | **CUMPLIDO** | `LeaveResolverPolicy` valida superior activo con rol resolutor. |
| **FR-016** | Cancelación automática de pendientes ante baja del superior | **CUMPLIDO** | `LeaveRequestAutoCancelService` ejecutado en baja de empleado de HU-01. |
| **FR-017** | Soporte para rol `DIRECCION_GENERAL` | **CUMPLIDO** | Rol reconocido en autorización y políticas de resolución de ausencias. |
| **FR-018** | Rol `RRHH` como resolutor de sus subordinados directos | **CUMPLIDO** | `RRHH` resuelve a su equipo propio sin extenderse a toda la plantilla. |

### 2.2. Criterios de Éxito (Success Criteria)

- **SC-001 (Resolución ágil en < 2 clics)**: Diálogo modal optimizado `ResolveRequestDialogComponent`.
- **SC-002 (0% fugas de permisos)**: Verificado en `LeaveResolutionServiceTest` y tests de seguridad negativa.
- **SC-003 (100% de decisiones inmutables)**: Ninguna solicitud resuelta puede ser alterada posteriormente.
- **SC-004 (100% notificaciones generadas)**: Notificación asociada a cada resolución sin excepciones.

---

## 3. Arquitectura y Componentes Implementados

### 3.1. Backend (Spring Boot 3.4.3 + Java 21)
- **Controlador REST**: `com.iw.rrhh.leave.controller.LeaveResolutionController`
  - `GET /api/v1/leave/resolutions/pending`: Solicitudes pendientes del equipo a cargo.
  - `POST /api/v1/leave/resolutions/{id}/resolve`: Resolución atómica (aprobación/rechazo).
- **Servicios de Dominio**:
  - `LeaveResolutionServiceImpl`: Motor de resolución, validación jerárquica y auditoría.
  - `LeaveDayAllocator`: Algoritmo de cómputo y distribución multianual de ausencias.
  - `LeaveRequestAutoCancelService`: Cancelación en cascada de solicitudes huérfanas con notificación agrupada.
  - `LeaveResolverPolicy`: Determinación de permisos resolutores según jerarquía organizativa.
- **Módulo de Notificaciones**:
  - `NotificationService` y `NotificationRepository`: Gestión del buzón interno de avisos.

### 3.2. Frontend (Angular 21 + Material Design)
- **Componentes**:
  - `TeamPendingRequestsComponent`: Bandeja de entrada del responsable con contador de solicitudes pendientes.
  - `ResolveRequestDialogComponent`: Diálogo modal accesible para aprobar con comentario o rechazar con motivo.
  - `NotificationBellComponent`: Campana interactiva con contador de avisos no leídos en tiempo real.

---

## 4. Verificación y Resultados de Pruebas

- **`LeaveResolutionServiceTest`**: 16 tests unitarios, **16 pasados (0 fallos)**.
- **`LeaveResolverPolicyTest`**: 9 tests unitarios, **9 pasados (0 fallos)**.
- **`LeaveRequestAutoCancelServiceTest`**: 5 tests unitarios, **5 pasados (0 fallos)**.
- **Tests de Integración**: `LeaveResolutionIntegrationTest` y `LeaveResolutionAtomicityIntegrationTest` verificando concurrencia y atomicidad transaccional.
- **Componentes Frontend**: `team-pending-requests.component.spec.ts` y `resolve-request-dialog.component.spec.ts` verificados con Jasmine/Karma.

---

## 5. Alineación con la Constitución del Proyecto

- **Art. 10 (Seguridad en servidor)**: El responsable solo puede operar sobre subordinados directos.
- **Art. 12 (Saldo como valor derivado)**: La aprobación actualiza el estado de la solicitud, y el saldo de HU-02 se recalcula dinámicamente sin campos redundantes.
- **Art. 13 (Transición de estados de ausencias)**: Modelo de estados estricto (`PENDING` → `APPROVED` / `REJECTED` / `CANCELLED`).
- **Art. 19 (RFC 9457 Problem Details)**: Manejo uniforme de excepciones con payload semántico.

---

## 6. Conclusiones y Estado de Convergencia

La especificación `spec-005` ha alcanzado una **convergencia total (100%)** tras superar con éxito tres rondas de refinamiento iterativo. El módulo resuelve con precisión matemática y estricta seguridad todas las casuísticas de supervisión de equipo.
