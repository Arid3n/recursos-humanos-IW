# Research: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Feature**: `spec-004` (HU-04)  
**Date**: 2026-11-01  
**Author**: Integrante 2  

---

## 1. Decisiones Técnicas y Arquitectura

### 1.1 Endpoint de Consulta y Paginación en Servidor (Art. 9 y 10)
- **Decisión**: Exponer `GET /api/v1/leave/my-requests` aceptando parámetros opcionales `page` (default 0), `size` (default 10) y `status` (opcional: `PENDING`, `APPROVED`, `REJECTED`).
- **Justificación**:
  - Cumple con Art. 9.4 (listados paginados y filtrables en servidor) y Art. 10.5 (la identidad del empleado se obtiene exclusivamente del `Principal` autenticado en la sesión mediante `employeeRepository.findByUserId(user.getId())`, nunca de parámetros enviados por el cliente).
  - Devuelve `PageResponse<LeaveRequestSummaryResponse>` con metadatos de paginación (`totalPages`, `totalElements`, `size`, `number`, `content`).
- **Alternativas consideradas**:
  - `GET /api/v1/employees/{id}/leave-requests`: Descartado por violar Art. 10.5 (depende de un ID en la ruta propenso a manipulación IDOR).

### 1.2 Comportamiento de Cancelación de Solicitudes (Art. 10 y 13)
- **Decisión**: Exponer `POST /api/v1/leave/requests/{id}/cancel` (y alias semántico `DELETE /api/v1/leave/requests/{id}`).
- **Reglas**:
  - Verificar que la solicitud existe (`404 Not Found`).
  - Verificar que la solicitud pertenece estrictamente al empleado en sesión (si pertenece a otro empleado, devolver `403 Forbidden` según Art. 10.6 con test negativo).
  - Verificar que el estado es estrictamente `PENDING`. Si el estado es `APPROVED` o `REJECTED`, rechazar con `409 Conflict` (ProblemDetail) según Art. 13.2 y FR-006.
  - Al cancelar, se elimina la solicitud de la cola de pendientes (`leaveRequestRepository.delete(request)`), liberando inmediatamente el periodo y descartándola para el responsable sin penalización en saldo (puesto que el saldo se descuenta al aprobarse según Art. 12).
- **Alternativas consideradas**:
  - Estado `CANCELLED`: Requeriría alterar la restricción `chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))` en BD y colisionaría con los estados de Art. 13.1. Eliminar la solicitud en trámite cumple estrictamente con "queda descartada y deja de aparecer como pendiente para su responsable".

### 1.3 Filtrado Reactivo en Interfaz (Art. 21)
- **Decisión**: Selector de pestañas / grupo de botones de selección (`mat-button-toggle-group` o `mat-select`) con las opciones: `Todas`, `Pendientes`, `Aprobadas`, `Rechazadas`.
- **Justificación**:
  - Al cambiar de estado, se reinicia la paginación a la página 0 y se solicita la página correspondiente al servidor.
  - Accesible con teclado y etiquetas claras (`aria-label`) conforme a WCAG 2.1 AA (Art. 21).

### 1.4 Visualización del Motivo de Rechazo (FR-004)
- **Decisión**: En las solicitudes con estado `REJECTED`, mostrar un botón/icono con etiqueta descriptiva "Ver motivo" que despliega un diálogo accesible `MatDialog` (o panel expandible) con el texto de `rejectionReason`.
- **Justificación**:
  - Permite leer motivos extensos sin desmaquetar la tabla.
  - Cumple WCAG 2.1 AA al estar asociado a un botón accesible por teclado.

---

## 2. Cobertura de la Constitución

| Artículo | Requisito Constitucional | Enfoque spec-004 |
|---|---|---|
| **Art. 2** | Trazabilidad HU → CA → Test | Tests unitarios y de integración etiquetados `HU-04.CA-X`. |
| **Art. 9** | API REST paginada y contratos OpenAPI | `GET /api/v1/leave/my-requests` paginado y documentado en OpenAPI YAML. |
| **Art. 10** | Autorización por rol y alcance en servidor | `SecurityContext` extrae el empleado; test negativo `403` al intentar cancelar solicitud ajena. |
| **Art. 13** | Ciclo de vida de la solicitud | Cancelación solo si `PENDING`; `409 Conflict` si ya fue `APPROVED`. |
| **Art. 19** | Errores ProblemDetail (RFC 9457) | `404`, `403` y `409` estructurados según ProblemDetail estándar. |
| **Art. 21** | Accesibilidad WCAG 2.1 AA | Badges de estado con texto explicativo e iconos además de color; diálogos accesibles. |
