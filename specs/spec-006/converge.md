# Informe de convergencia: spec-006 (HU-06, calendario de ausencias del equipo)

**Historia**: HU-06 · **Responsable**: Andrei Radu · **Rama**: `refinamiento-HU5-HU6`
**Commit de referencia previo al refinamiento**: tag `ref-refinamiento` (`afed4c4`, 2026-11-10)

> Fuentes: sesiones de trabajo del 2026-11-18 y 2026-11-19, `tasks.md` y el historial de git.

## Secuencia seguida (2026-11-18)

clarify (5 preguntas) → plan → tasks → analyze (A1, A2 e I1 corregidos) → converge → implement de la fase 6 → pruebas completas → validación manual. Commits: `7867b7b` (clarify, plan, tasks y fase 6), `366da0d` (T029, T030: suites completas en verde), `f83640f` (T031, T032: escenarios manuales).

## Iteración 1: converge

Resultado de la nota de trabajo: las obligaciones nuevas surgidas de las aclaraciones quedaron como tareas T023-T028 (fase 6) y T029-T032 (fase 7): retiradas (`CANCELLED`) ocultas, ausencias propias del responsable excluidas, mes inicial en `Europe/Madrid`, lista vertical por debajo de 600 px (FR-003b), sin datos de motivo, navegación sin límite. `tasks.md` no tiene una sección «Convergence» propia; esas tareas se registraron por la vía clarify/plan. **No se conservó el listado de hallazgos original de converge**, por lo que no se puede separar con certeza qué tareas se originaron en converge y cuáles en clarify.

## Iteración 2: converge tras implementar

Resultado: **limpio** (nota del 2026-11-18). Sin brechas nuevas.

## Verificación asociada

- Estado tras la fase 6 (2026-11-18): backend 163 pruebas en verde, frontend 62 en verde, `ng build` correcto.
- Manual en Chrome real: escenarios 1, 2, 3, 5, 6, 7 y 9 de `quickstart.md`; por API real: 4 y 8. No se emuló escala de grises.
- Cambios posteriores (2026-11-19, desde spec-005): el endpoint admite también `DIRECCION_GENERAL` (FR-017) y `RRHH` (FR-018), siempre solo para sus subordinados directos. FR-011 y contrato 1.3.0 actualizados; implementado en `spec-005` (T066, T090, T096) y validado en Chrome (`rrhh@` ve solo a `responsable@`). Sin tareas nuevas en spec-006. Al cierre de ese día: backend 224, frontend 86.

## Brechas abiertas (de baja gravedad, no accionables del alcance)

- U1: sin prueba de que la denegación `403` se registra en el log.
- U2: sin medición del objetivo de menos de 1 s (SC-001).
- U3: sin pruebas de contrato ni E2E.
- Las pruebas del mes en Madrid no pueden fallar en una máquina en zona CET.
- No se ejecutó converge tras el cambio de 2026-11-19 (FR-017/018).
