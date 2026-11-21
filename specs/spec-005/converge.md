# Informe de convergencia: spec-005 (HU-05, aprobación o rechazo de solicitudes)

**Historia**: HU-05 · **Responsable**: Andrei Radu · **Rama**: `refinamiento-HU5-HU6`
**Commit de referencia previo al refinamiento**: tag `ref-refinamiento` (`afed4c4`, 2026-11-10)

> Fuentes: sesiones de trabajo del 2026-11-18 y 2026-11-19, `tasks.md` y el historial de git.

## Iteración 1 (2026-11-18): converge sobre la implementación de las fases 6 y 7

**Estado de partida**: implementación de aprobación y rechazo hecha (commit `e862e5c`). Backend 153 pruebas en verde, frontend 53 (sesión del 2026-11-18). Funcionalidad activa: `specs/spec-005`.

**Hallazgos** (los tres son cumplimiento parcial; no hubo trabajo ausente ni comportamiento contradictorio):

| ID | Tipo | Criterio / obligación | Hallazgo | Tarea | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- |
| G1 | Cumplimiento parcial | `HU-05.CA-2`, FR-003 | `@Size` se evaluaba sobre el texto sin recortar: `"  abc  "` pasaba el DTO y devolvía `400` sin `invalidFields.reason`; un motivo de 500 caracteres con espacios laterales se rechazaba. | T054 | Cerrado |
| G2 | Cumplimiento parcial | `HU-05.CA-2`, US1/AC3, FR-005 | Solo existía una prueba de HU-04 con fixture manual; faltaba el flujo HU-05 → HU-04 (el empleado ve estado `REJECTED` y el mismo motivo). | T055 | Cerrado |
| G3 | Cumplimiento parcial | `HU-05.CA-1`, FR-013, Art. 20.4-20.5 | Registro de auditoría implementado pero sin prueba (INFO con UUIDs, WARN «Acceso denegado», sin datos personales). | T056 | Cerrado |

**Corrección** (implement): commit `49fad56` (2026-11-18, «HU-05: converge», 5 ficheros: `RejectLeaveRequest`, `ApproveLeaveRequest`, `LeaveResolutionIntegrationTest`, `LeaveResolutionServiceTest`, `tasks.md`). Las pruebas de T054-T056 se escribieron antes del cambio de código.
**Resultado posterior**: backend 156 pruebas en verde (nota del 2026-11-18).
**Prueba reproducible del fallo previo**: entrada `"  abc  "` como motivo de rechazo; esperado `400` con `invalidFields.reason`; observado antes de T054 `400` sin ese campo (descrito en `tasks.md`). No se conservó la salida de la ejecución en rojo.

## Iteración 2 (2026-11-18): converge tras las correcciones

Resultado: **limpio**, sin nuevas brechas. Todas las tareas T001-T056 en `[X]`.

## Iteraciones posteriores (2026-11-18 y 2026-11-19): fases 9 y 10

Las fases 9 (FR-015/016/017, rol `DIRECCION_GENERAL`) y 10 (FR-018, RRHH resolutor; Dirección General sin saldo) se tramitaron con clarify, plan, tasks, analyze, tests en rojo, implement y validación en Chrome, no con un nuevo converge. **No consta un converge ejecutado tras la fase 10.** Verificación al cierre (2026-11-19): backend 224 pruebas, frontend 86, `ng build` correcto; commits `caa1590` a `cc06571`.

## Límites conocidos (no son brechas accionables del alcance)

- N1: HU-03 valida el saldo solo del año de inicio; una solicitud interanual puede crearse y fallar en la aprobación con `422` (documentado en `spec.md` y `plan.md`, Complexity Tracking).
- Las pantallas de variación y trayectoria contractual de HU-01 no están enlazadas en la aplicación.
- Sin pruebas de contrato ni E2E automatizadas (solo pruebas unitarias, de integración y validación manual).

## Pendiente de reforzar la evidencia

- Ejecutar de nuevo `/speckit-converge` tras la fase 10 y guardar la salida íntegra aquí.
