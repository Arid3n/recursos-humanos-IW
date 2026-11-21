# Informe de convergencia: spec-008 (HU-08, evaluación de desempeño)

**Responsable**: José Ramón Castells · **Referencia**: tag `ref-refinamiento`

> Informe de convergencia de 2026-11-21. Método: comparación de `spec.md`, `plan.md` y `tasks.md` entre el tag `ref-refinamiento` (`afed4c4`) y `HEAD`, y contraste de cada obligación modificada con el código (lectura y `grep`; las pruebas se ejecutaron aparte, ver `docs/verificacion-integrada.md`). Una tarea marcada `[X]` no se toma como prueba de cumplimiento.

## Cambios de documentación desde la referencia
En la referencia solo existían `spec.md` y `quickstart.md`. Después se añadieron plan, tareas, investigación, modelo de datos, contrato, análisis e informe de implementación; la spec pasa a «Implemented» con la sesión de clarify del 2026-11-20 (control `<select>` para la valoración, bloqueo `PESSIMISTIC_WRITE` más `@Version`; sin otras ambigüedades).

## Obligaciones verificadas en el código
| Obligación | Evidencia | Resultado |
| :-- | :-- | :-- |
| FR-006 / SC-002 inmutabilidad de `FINALIZED` | Disparador en `V4__immutable_finalized_performance_reviews.sql` | Cumple |
| Concurrencia de la conformidad | `PESSIMISTIC_WRITE` en `PerformanceReviewRepository` | Cumple |
| FR-001/FR-007 acceso por rol | `PerformanceReviewController`: `EMPLEADO` y `RESPONSABLE_EQUIPO` | Cumple según el texto de la spec |

## Hallazgos
| ID | Tipo | Detalle | Estado |
| :-- | :-- | :-- | :-- |
| C1 | Incoherencia entre specs | HU-05 FR-017/FR-018 hacen resolutores y superiores directos a `DIRECCION_GENERAL` y `RRHH`, pero HU-08 solo permite evaluar a `RESPONSABLE_EQUIPO`; un RRHH o Dirección General con subordinados no puede evaluarlos. La spec-008 no se revisó tras esos cambios | Aceptado como límite conocido (2026-11-21): solo `RESPONSABLE_EQUIPO` evalúa; ver Session 2026-11-21 de `spec.md` |
| C2 | Sin evidencia | Sin medición de SC-001 (menos de 3 pasos) ni prueba E2E | Abierto (sin medición, aceptado) |


## Decisión de cierre (2026-11-21, código congelado)

Con el código congelado, la decisión es mantener la evaluación restringida a `RESPONSABLE_EQUIPO` (FR-001 y FR-007) y documentar que RRHH y Dirección General con subordinados no pueden evaluarlos. Queda anotado en `spec.md`.
