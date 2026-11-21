# Specification Analysis Report: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Feature**: `spec-004` · HU-04  
**Fecha de análisis**: 2026-11-20  
**Estado**: Completado (implementación verificada contra código real)  
**Responsable**: Iván Soler (Integrante 2 — Servicios y derechos del Empleado)

**Artefactos analizados:**
- [spec.md](spec.md)
- [plan.md](plan.md)
- [tasks.md](tasks.md)
- [data-model.md](data-model.md)
- [contracts/leave-tracking-api.yaml](contracts/leave-tracking-api.yaml)
- [.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- Código implementado: `backend/src/main/java/com/iw/rrhh/leave/` y `frontend/src/app/features/leave/`

---

## 1. Tabla de Hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen | Estado / Remediación |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **I1** | Inconsistency | **MEDIUM** | `contracts/leave-tracking-api.yaml` vs Estado | Código de respuesta al intentar cancelar una solicitud ya resuelta (`APPROVED` o `REJECTED`). | **RESUELTO**: Retorna `409 Conflict` (o `404` si no existe); documentado con esquema ProblemDetail en OpenAPI. |
| **U1** | Underspecification | **LOW** | `spec.md FR-003` | Impacto de la cancelación en el saldo retenido. | **RESUELTO**: Al transicionar a `CANCELLED`, los días retenidos preventivamente en HU-03 se liberan de inmediato para el saldo disponible. |
| **C1** | Coverage Gap | **LOW** | `tasks.md` | Filtro por año de consulta en el listado de solicitudes. | **CUBIERTO**: Parámetro opcional `year` en endpoint `GET /api/v1/leave/requests/my-requests` y selector en frontend. |

---

## 2. Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
| :--- | :---: | :--- | :--- |
| **FR-001** (Listado histórico paginado ordenado por `created_at DESC`) | Sí | T004, T008, T010, T011 | Paginación en servidor con `Pageable` de Spring Data (bloques de 10). |
| **FR-002** (Filtrado por estado: PENDING, APPROVED, REJECTED, CANCELLED) | Sí | T005, T008, T010, T012 | Selector accesible en interfaz con actualización dinámica reactiva. |
| **FR-003** (Cancelación de peticiones PENDING con liberación de saldo) | Sí | T006, T008, T009, T013 | Transición atómica a `CANCELLED`; libera el rango de fechas para solapamiento. |
| **FR-004** (Visualización del motivo de rechazo en solicitudes denegadas) | Sí | T007, T010, T012 | Columna y diálogo/tooltip accesible con el motivo registrado por el responsable. |
| **FR-005** (Aislamiento estricto por empleado autenticado) | Sí | T008, T009, T016 | Extracción en servidor vía `SecurityContextHolder`; test negativo de acceso cruzado. |
| **FR-006** (Detalle de justificación del empleado y días laborables) | Sí | T007, T010, T012 | Exposición completa del DTO `LeaveRequestResponse`. |
| **SC-001** (Listado presentado en < 800 ms) | Sí | T005, T010 | Índices en `(employee_id, status)` y `(employee_id, created_at)`. |
| **SC-002** (100% solicitudes canceladas liberan preventivamente el saldo) | Sí | T008, T009 | Verificado en `LeaveRequestTrackingServiceTest`. |
| **SC-003** (0% fugas de solicitudes ajenas entre empleados) | Sí | T009, T016 | Suite de tests de seguridad negativa con MockMvc. |

---

## 3. Constitution Alignment Issues

*No se detectaron violaciones constitucionales.*

- **Art. 5 (4 capas desacopladas)**: Respetado (Controlador REST → Servicio de Tracking → Repositorio JPA → PostgreSQL).
- **Art. 10 (Seguridad en servidor)**: El empleado autenticado no puede consultar ni cancelar solicitudes de otros empleados (HTTP 403 / 404).
- **Art. 19 (RFC 9457 Problem Details)**: Formato uniforme para errores de estado inválido (`409 Conflict`) o recurso no encontrado (`404 Not Found`).
- **Art. 21 (Accesibilidad)**: Tabla con `mat-table`, encabezados semánticos accesibles y paginador estándar de Angular Material.

---

## 4. Unmapped Tasks

*Ninguna.* Todas las 22 tareas de `spec-004` cubren los requerimientos funcionales, de interfaz y de verificación.

---

## 5. Métricas

- **Total Requirements (FR + SC)**: 9
- **Requisitos cubiertos con tareas y código**: 9 (100%)
- **Hallazgos identificados**: 3 (todos resueltos)
- **Estado general**: **Aprobado sin brechas críticas**
