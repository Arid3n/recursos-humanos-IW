# Informe de Implementación: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Historia de Usuario**: HU-04  
**Rama**: `spec-004` (integrada en `main`)  
**Fecha de finalización**: 2026-11-20  
**Responsable**: Iván Soler (Integrante 2 — Servicios y derechos del Empleado)  
**Estado**: **Completado y Convergido (100%)**

---

## 1. Resumen Ejecutivo

Se ha completado satisfactoriamente la implementación de la especificación **`spec-004` (HU-04)**, que proporciona al empleado una vista consolidada, transparente y filtrable de todas sus solicitudes de ausencias (vacaciones y permisos).

El módulo permite consultar el historial completo ordenado cronológicamente de forma descendente, filtrar por estado (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`), revisar el motivo de rechazo en caso de denegación, y cancelar de manera unilateral aquellas solicitudes que aún se encuentren en trámite (`PENDING`), liberando de inmediato las fechas y los días retenidos preventivamente en el saldo de vacaciones.

---

## 2. Cobertura de Requisitos y Criterios de Aceptación

### 2.1. Requisitos Funcionales

| Requisito | Descripción | Estado | Evidencia de Implementación |
| :--- | :--- | :---: | :--- |
| **FR-001** | Bandeja histórica paginada (`created_at DESC`) | **CUMPLIDO** | `LeaveRequestTrackingController.getMyRequests()`, `LeaveRequestListComponent`. |
| **FR-002** | Filtros dinámicos por estado de solicitud | **CUMPLIDO** | Parámetro `status` en query Spring Data y control de selección en UI. |
| **FR-003** | Cancelación unilateral de solicitudes `PENDING` | **CUMPLIDO** | `POST /api/v1/leave/requests/{id}/cancel` actualiza estado a `CANCELLED`. |
| **FR-004** | Visualización de motivo explicativo de denegación | **CUMPLIDO** | Columna y diálogo accesible con campo `rejection_reason`. |
| **FR-005** | Aislamiento estricto de solicitudes por empleado autenticado | **CUMPLIDO** | Filtro obligatorio por `employee_id` obtenido del token de sesión. |
| **FR-006** | Exposición detallada de justificación y días laborables | **CUMPLIDO** | DTO `LeaveRequestResponse` con `justification` y `calculatedDays`. |

### 2.2. Criterios de Éxito (Success Criteria)

- **SC-001 (Tiempo de respuesta < 800 ms)**: Paginación en base de datos con índices compuestos en PostgreSQL.
- **SC-002 (Liberación inmediata del saldo retenido al cancelar)**: Verificado en `LeaveRequestTrackingServiceTest` comprobando que las solicitudes `CANCELLED` no restan en el saldo disponible de HU-02 ni bloquean fechas en HU-03.
- **SC-003 (0% fugas de privacidad entre empleados)**: Validación estricta con tests de seguridad negativa que confirman que un empleado recibe 404/403 al intentar acceder o cancelar solicitudes ajenas.

---

## 3. Arquitectura y Componentes Implementados

### 3.1. Backend (Spring Boot 3.4.3 + Java 21)
- **Controlador REST**: `com.iw.rrhh.leave.controller.LeaveRequestTrackingController`
  - `GET /api/v1/leave/requests/my-requests`: Consulta paginada con filtros por estado y año.
  - `POST /api/v1/leave/requests/{id}/cancel`: Cancelación unilateral de peticiones en estado pendiente.
- **Servicio de Dominio**:
  - `LeaveRequestTrackingServiceImpl`: Validación de propiedad de la solicitud, transiciones de estado a `CANCELLED` y control de concurrencia.
- **Repositorio JPA**:
  - Métodos `findByEmployeeIdOrderByCreatedAtDesc` y variantes con especificación de estado.

### 3.2. Frontend (Angular 21 + Material Design)
- **Componentes**:
  - `LeaveRequestListComponent`: Tabla accesible con `mat-table`, paginador `mat-paginator`, chips de colores por estado, botón de cancelación con diálogo de confirmación y visualizador de motivos de rechazo.
- **Servicios**:
  - `LeaveTrackingService`: Métodos HTTP `getMyRequests` y `cancelLeaveRequest`.

---

## 4. Verificación y Resultados de Pruebas

- **`LeaveRequestTrackingServiceTest`**: 6 tests unitarios ejecutados, **6 pasados (0 fallos, 0 errores)** en 0.797 s.
  - Cubre listado propio, filtrado por estados, cancelación exitosa de solicitudes pendientes, rechazo de cancelación sobre solicitudes aprobadas/rechazadas y rechazo de cancelación de solicitudes ajenas.
- **`leave-request-list.component.spec.ts`**: Verificación de renderizado de tabla, paginación y eventos de filtrado y cancelación en cliente.

---

## 5. Alineación con la Constitución del Proyecto

- **Art. 10 (Seguridad en servidor)**: Garantía de que un empleado solo accede y opera sobre sus propias ausencias.
- **Art. 11 (Trazabilidad y no borrado)**: Las cancelaciones transicionan a `CANCELLED` manteniendo la fila para auditoría (`updated_at`, `status`).
- **Art. 19 (RFC 9457 Problem Details)**: Formato de error estándar ante intentos de modificar solicitudes resueltas.
- **Art. 21 (Accesibilidad)**: Cumplimiento de WCAG 2.1 AA con contrastes adecuados y feedback ante acciones.

---

## 6. Conclusiones y Estado de Convergencia

La funcionalidad `spec-004` se encuentra **100% convergida** con la especificación y el código. Cierra el ciclo de autoservicio del empleado y se sincroniza armoniosamente con los módulos de resolución (HU-05) y analítica (HU-07).
