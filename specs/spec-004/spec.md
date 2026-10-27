# Feature Specification: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Feature Branch**: `spec-004`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-04  
**Asignación**: Integrante 2 (Flujo de Ausencias del Empleado)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como empleado, quiero consultar un listado histórico de todas mis solicitudes de vacaciones y permisos, filtrar por su estado actual (Pendiente, Aprobada, Rechazada) y revisar el motivo en caso de que alguna haya sido denegada, para estar informado de la evolución de mis peticiones y poder planificar alternativas si procede.

**Why this priority**: Cierra el ciclo de visibilidad del empleado respecto a las peticiones iniciadas en HU-03.

**Independent Test**: Puede probarse creando peticiones en distintos estados (`Pendiente`, `Aprobada`, `Rechazada` con motivo de prueba), iniciando sesión con el empleado y comprobando que el listado pagina, filtra correctamente y muestra el motivo de denegación sin exponer solicitudes ajenas.

### Acceptance Scenarios

1. **Listado completo de solicitudes del empleado**:
   - **Given** un empleado autenticado que ha registrado diversas solicitudes a lo largo del tiempo,
   - **When** accede a su bandeja "Mis Solicitudes",
   - **Then** visualiza una tabla o listado con columnas: tipo (Vacaciones / Permiso), fecha de inicio, fecha de fin, total de días solicitados, estado actual y fecha de solicitud.

2. **Detalle de motivo de rechazo**:
   - **Given** una solicitud en estado `Rechazada` que contiene una justificación introducida por el responsable ("Coincidencia crítica con entrega de proyecto"),
   - **When** el empleado examina la fila o detalle de dicha solicitud,
   - **Then** el sistema muestra claramente el motivo explicativo registrado en HU-05.

3. **Filtrado dinámico por estado**:
   - **Given** un historial con solicitudes pendientes, aprobadas y rechazadas,
   - **When** el empleado selecciona el filtro "Pendiente",
   - **Then** la vista actualiza el listado mostrando únicamente aquellas solicitudes en estado `Pendiente`.

4. **Aislamiento de alcance (Privacidad)**:
   - **Given** un empleado autenticado,
   - **When** se cargan las solicitudes,
   - **Then** el servidor filtra en origen por el `employeeId` de la sesión y garantiza que nunca se devuelvan registros de otros compañeros de la organización.

### Edge Cases
- **Empleado sin solicitudes previas:** Debe mostrarse un estado vacío informativo con un botón accesible para crear su primera solicitud ("No tienes solicitudes registradas. Solicitar vacaciones").
- **Solicitud rechazada sin motivo explícito:** Si el motivo fuera opcional o no estuviera informado, se muestra una indicación neutral ("Sin motivo especificado").

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El sistema DEBE exponer un endpoint de listado paginado de solicitudes para el empleado en sesión.
- **FR-002**: Cada elemento del listado DEBE incluir: identificador, tipo, fecha de inicio, fecha de fin, días laborables computados, estado (`PENDING`, `APPROVED`, `REJECTED`), fecha de creación y motivo de rechazo si aplica.
- **FR-003**: La vista DEBE permitir filtrar por estado mediante un selector accesible (`Todos`, `Pendiente`, `Aprobada`, `Rechazada`).
- **FR-004**: Si una solicitud está en estado `REJECTED`, el motivo (`rejectionReason`) DEBE ser visible para el empleado en la interfaz.
- **FR-005**: El backend DEBE rechazar cualquier intento de consultar solicitudes de terceros por parte de un usuario con rol `EMPLEADO`.

### Key Entities
- **LeaveRequestSummary (DTO de Consulta)**:
  - `id`: Identificador.
  - `type`: Tipo de ausencia.
  - `startDate`, `endDate`: Rango temporal.
  - `days`: Días cómputo.
  - `status`: Estado de la petición.
  - `rejectionReason`: Justificación en caso de rechazo.
  - `createdAt`: Fecha de solicitud.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: El listado de solicitudes carga y se renderiza en menos de 800ms.
- **SC-002**: El filtro por estado responde de forma reactiva e instantánea.
- **SC-003**: 100% de precisión en la visualización del motivo de rechazo cuando exista.
- **SC-004**: Cero filtraciones de solicitudes entre empleados (verificado mediante tests de integración con múltiples usuarios).

---

## 4. Assumptions
- El listado soporta paginación por defecto (p. ej. 10 elementos por página) si el histórico es extenso.
