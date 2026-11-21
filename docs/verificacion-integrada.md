# Verificación integrada y valoración del refinamiento (apartado 5.4)

**Fecha**: 2026-11-21 · **Versión del código**: `662da39` (rama `memoria-y-entregables`; sin cambios en `backend/` ni `frontend/` durante las comprobaciones) · **Commit de referencia previo al refinamiento**: tag `ref-refinamiento` (`afed4c4`).

Todas las evidencias están en el repositorio y se enlazan desde aquí. No se ha modificado el código de la aplicación para obtenerlas.

## 1. Recorridos completos

Dos recorridos que cruzan historias de los cuatro integrantes, ejecutados contra la API real (backend Spring Boot sobre PostgreSQL 16 limpio, migraciones V1 a V7 y datos semilla), con `docs/evidencias/e2e/run_recorridos.py`. Resultado paso a paso (precondiciones, datos, esperado, observado, estado): `docs/evidencias/e2e/resultado-recorridos.md`. El diseño de los recorridos está en `docs/recorridos-e2e.md`.

| Recorrido | Tipo | Historias | Pasos | Conformes |
| :-- | :-- | :-- | :-: | :-: |
| 1 | Satisfactorio: solicitud, aprobación, saldo, calendario, informe, CSV y evaluación | HU-01 a HU-08 | 17 | 17 |
| 2 | Error, permisos y estado: validaciones, solapamiento, 403 cruzados, rechazo, 409 por doble resolución, cancelación, evaluación inmutable | HU-01 a HU-08 | 32 | 32 |

**Limitaciones declaradas**: se ejecutó por API, no por la interfaz gráfica; los datos son los semilla (Carlos y Laura) más Pedro, creado en el recorrido 2; las fechas son de 2027 para no chocar con la validación de fechas pasadas. La validación en navegador de HU-05 y HU-06 se hizo en sesiones anteriores (`specs/spec-005/converge.md`, `specs/spec-006/converge.md`).

### Diferencias entre el diseño original del recorrido y lo observado

La primera redacción de `docs/recorridos-e2e.md` contenía valores esperados que no coinciden con la aplicación. En todos los casos la aplicación sigue su especificación y contrato, y se corrigió el esperado (no la aplicación):

| Paso | Esperado en el documento original | Observado | Fuente que da la razón |
| :-- | :-- | :-- | :-- |
| Saldo con una solicitud pendiente | 13 disponibles (pendientes restadas) | `pendingDays=10`, `availableDays=22` | HU-02: `disponible = correspondientes − disfrutados`; la reserva de pendientes se aplica al validar nuevas solicitudes (HU-03 FR-004) |
| Saldo insuficiente al solicitar | `422` | `409` | `specs/spec-003/contracts/leave-request-api.yaml`: 409 = saldo insuficiente o solapamiento; 422 queda para «sin resolutor válido» y Dirección General |
| Cancelación de solicitud | `200` con `status: CANCELLED` | `204` sin contenido; el listado muestra `CANCELLED` | `specs/spec-004/contracts/leave-tracking-api.yaml` |
| Rutas y usuarios | `/team/calendar`, `/performance-reviews`, `ana.garcia@`… | `/leave/team-calendar`, `/performance-reviews/team/{id}`, usuarios semilla | Código y `V2__seed_initial_data.sql` |

## 2. Regresión

| Suite | Comando | Resultado | Evidencia |
| :-- | :-- | :-- | :-- |
| Backend (JUnit 5, Testcontainers/PostgreSQL) | `cd backend && JAVA_TOOL_OPTIONS="-Dapi.version=1.44" sh ./mvnw test` | **224 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS** | `docs/evidencias/backend-tests.txt` |
| Frontend (Vitest) | `cd frontend && npx ng test --watch=false` | **86 pruebas en 16 ficheros, todas en verde** | `docs/evidencias/frontend-tests.txt` |

Las historias dependientes de las correcciones (HU-02, HU-03, HU-04 por el cambio de HU-05/FR-015 a FR-018) están cubiertas por estas suites completas, y los dos recorridos las reejecutan de extremo a extremo. Verificación manual que no cubre la suite: pantallas y escalas de grises (no emuladas), tiempos de carga (SC-001 de HU-02, HU-06, HU-07), contratos y trayectoria de HU-01 en la interfaz (no accesibles, ver `specs/spec-001/converge.md`).

La cifra de 93 pruebas de `docs/memoria.md` §7.2 corresponde solo a ocho clases unitarias del módulo de ausencias; el total real del backend es 224.

## 3. Balance de la iteración (versión inicial `ref-refinamiento` frente a la final)

| Indicador | Inicial | Final | Evidencia |
| :-- | :-: | :-: | :-- |
| Preguntas de clarify respondidas | 29 | **83 de 83** | `specs/spec-00N/clarify.md` |
| Requisitos funcionales (FR) | 72 | 83 | `docs/evidencias/spec-diffs/` |
| Tareas en `tasks.md` | 160 | 290 (290 marcadas `[X]`, 0 abiertas) | `specs/spec-00N/tasks.md` |
| Hallazgos documentales de analyze (8 specs) | — | 27 registrados; 25 con remediación marcada como resuelta, cubierta, mitigada o límite conocido; 2 sin marca explícita | `docs/registro-hallazgos.md` |
| Hallazgos de spec-005 (1.ª y 2.ª pasada) | 21 | Al 2026-11-21: 12 cerrados, 4 no aplicables o aceptados (C12, C15, N1, N6) y 5 abiertos de carácter documental menor (C8, C9, C13, N3, N4) | `docs/evidencias/analyze/estado-spec-005-2026-11-21.md` |
| Brechas de implementación halladas por converge | — | cerradas 3 en HU-05 (G1 a G3, commit `49fad56`) y 6 tareas en HU-03 (T020 a T025); **0 abiertas accionables**; 4 aceptadas como límite conocido por código congelado (HU-01 C1 y C2, HU-08 C1, HU-03 N1), que requieren validación del docente por afectar al alcance | `specs/spec-00N/converge.md` |
| Criterios de la matriz verificados | — | **65 de 66** (ver abajo) | `docs/matriz-trazabilidad.md` y este documento |

Los FR y tareas iniciales se contaron con `git show ref-refinamiento:…`; las specs 007 y 008 no tenían plan ni tareas en la referencia (0 tareas).

### Criterios verificados sobre el total revisado

Un criterio cuenta como verificado si la matriz nombra una clase de prueba que existe y pasó en la regresión, o si lo ejercita un paso conforme de los recorridos.

| Historia | Criterios | Con prueba nombrada en verde | Solo por recorrido E2E | Verificados | Sin evidencia directa |
| :-- | :-: | :-: | :-: | :-: | :-: |
| HU-01 | 9 | 7 | 2 | 9 | 0 |
| HU-02 | 8 | 4 | 3 | 7 | 1 |
| HU-03 | 8 | 7 | 1 | 8 | 0 |
| HU-04 | 6 | 4 | 2 | 6 | 0 |
| HU-05 | 12 | 11 | 1 | 12 | 0 |
| HU-06 | 8 | 7 | 1 | 8 | 0 |
| HU-07 | 7 | 6 | 1 | 7 | 0 |
| HU-08 | 8 | 4 | 4 | 8 | 0 |
| **Total** | **66** | **50** | **15** | **65** | **1** |

Sin evidencia: **HU02-SC01** (carga en menos de 800 ms): no se ha medido y queda como excepción documentada. HU01-FR04 cuenta con la prueba de orden cronológico del historial, pero la derivación de condiciones vigentes en la ficha no tiene prueba específica.

**Corrección de la matriz (2026-11-21)**: HU01-FR02 y FR03 citaban `ContractVariationIntegrationTest`, que no existe. Se sustituyó por la prueba real (`EmployeeIntegrationTest`, variación append-only retroactiva) y se nombraron las pruebas existentes de otros 9 criterios que figuraban sin enlace (`docs/matriz-trazabilidad.md`).

## 4. Registro mínimo

| Historia y criterio | Hallazgo o decisión | Comando | Documentos o código afectados | Tarea y commit | Prueba y resultado | Responsable | Revisor | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-: | :-: | :-: |
| HU-05 CA-2, FR-003 | El tamaño del motivo se validaba sin recortar espacios (`"  abc  "` pasaba el DTO) | `/speckit-converge` | `RejectLeaveRequest`, `ApproveLeaveRequest`, `tasks.md` | T054 · `49fad56` | `LeaveResolutionIntegrationTest` (37 en la suite, 0 fallos) | Andrei | Joaquín | Cerrado |
| HU-05 CA-2, FR-005 | Faltaba la prueba del flujo HU-05 a HU-04 (el empleado ve el motivo) | `/speckit-converge` | `LeaveResolutionIntegrationTest` | T055 · `49fad56` | Recorrido 2, pasos 18 y 19: REJECTED con motivo visible, OK | Andrei | Joaquín | Cerrado |
| HU-05 CA-1, FR-013 | Registro de auditoría sin prueba | `/speckit-converge` | `LeaveResolutionServiceTest` | T056 · `49fad56` | 16 pruebas del servicio, 0 fallos | Andrei | Joaquín | Cerrado |
| HU-03 FR-001, FR-004, FR-005 | Justificación en permisos, fechas pasadas y reserva de saldo no implementadas | `/speckit-converge` | `LeaveRequestServiceImpl`, formulario Angular, `tasks.md` | T020 a T025 · `9e00b69` | Recorrido 2, pasos 5, 6, 7 y 8: 400, 400, 409 y 201, OK | Iván | Joaquín | Cerrado |
| HU-05 FR-015 a FR-018 | Resolutor válido, cancelación automática, `DIRECCION_GENERAL` y RRHH resolutor | clarify, plan, tasks, analyze, implement | Constitución 1.2.0, specs 001/002/003/005/006, V7 | T057 a T103 · `caa1590` a `cc06571` | Backend 224/224 y frontend 86/86; recorrido 2, paso 26 (DG recibe 422) | Andrei | Joaquín | Cerrado |
| HU-06 FR-003b, FR-011 | Lista vertical en pantallas estrechas, retiradas ocultas y acceso de DG y RRHH | clarify, plan, tasks, analyze | `specs/spec-006`, `TeamCalendarService` | T023 a T032 · `7867b7b`, `f83640f` | `TeamCalendarIntegrationTest` (15) y `TeamCalendarServiceTest` (10), 0 fallos; recorrido 2, paso 20 | Andrei | Iván | Cerrado |
| HU-02 FR-008, FR-009 | Dirección General sin saldo propio (403) y rótulo «Mi Ficha» | clarify, plan | `LeaveBalanceServiceImpl`, `app.html` | T092, T097 a T099 · `85503b2` | `LeaveBalanceIntegrationTest` (10), 0 fallos | Joaquín | Andrei | Cerrado |
| HU-01 FR-002, FR-007, FR-009, FR-010 | Pantallas de variación contractual, trayectoria y baja sin ruta en la aplicación | `/speckit-converge` (revisión manual) | `app.routes.ts`, `contract-variation`, `contract-history` | Notas en `tasks.md` (T025, T030) y Session 2026-11-21 de `spec.md` | API con pruebas en `EmployeeIntegrationTest`; sin acceso desde la interfaz | Joaquín | Iván | Aceptado como límite conocido (validación docente) |
| HU-01 FR-011 frente a FR-002 | Contradicción: FR-011 dice que las variaciones no están soportadas | clarify (2026-11-21) | `specs/spec-001/spec.md` | Session 2026-11-21: FR-011 solo afecta al alta | — | Joaquín | Iván | Cerrado |
| HU-08 FR-001, FR-007 | RRHH y Dirección General con subordinados no pueden evaluar (decisión de HU-05 no propagada a HU-08) | clarify (2026-11-21) | `specs/spec-008/spec.md`, `PerformanceReviewController` | Session 2026-11-21 | `PerformanceReviewIntegrationTest` (restricción por rol) | José | Iván | Aceptado como límite conocido (validación docente) |
| HU-03 N1 | El saldo se valida solo para el año de inicio (solicitudes interanuales) | analyze | `LeaveRequestServiceImpl`, `plan.md` de spec-005 | Session 2026-11-21 de spec-003 | Documentado como límite conocido | Iván | Joaquín | Aceptado como límite conocido |
| HU-07 FR-004, FR-005, SC-002 | Paridad pantalla y CSV, UTF-8 con BOM | analyze | `DepartmentReportService` | `0c19a3f` | `DepartmentReportIntegrationTest` (5), 0 fallos; recorrido 1, pasos 14 y 15 | José | Andrei | Cerrado |
| HU-08 FR-005, FR-006, SC-002 | Conformidad con bloqueo pesimista e inmutabilidad (trigger V4) | clarify, analyze | `PerformanceReviewRepository`, `V4__…sql` | `0c19a3f` | `PerformanceReviewIntegrationTest` (4); recorrido 2, pasos 31 y 32: 409 | José | Iván | Cerrado |

Las salidas completas de comandos están en `docs/evidencias/`.
