# Informe de convergencia: spec-004 (HU-04, seguimiento y cancelación)

**Responsable**: Iván Soler · **Referencia**: tag `ref-refinamiento`

> Informe de convergencia de 2026-11-21. Método: comparación de `spec.md`, `plan.md` y `tasks.md` entre el tag `ref-refinamiento` (`afed4c4`) y `HEAD`, y contraste de cada obligación modificada con el código (lectura y `grep`; las pruebas se ejecutaron aparte, ver `docs/verificacion-integrada.md`). Una tarea marcada `[X]` no se toma como prueba de cumplimiento.

## Cambios de documentación desde la referencia
`spec.md` (sesión 2026-11-17): estado `CANCELLED`, liberación de saldo y fechas al cancelar, `justification` visible, `calculatedDays` entero, histórico con fechas pasadas. `tasks.md` (+31 líneas) incorpora esas obligaciones.

## Obligaciones verificadas en el código
| Obligación | Evidencia | Resultado |
| :-- | :-- | :-- |
| FR-002 `justification` en el listado | `LeaveRequestResponse.java:19` | Cumple |
| FR-003 filtro «Cancelada» | `leave-request-list.component.ts:64`, `.html:138` | Cumple |
| FR-006 cancelación solo de `PENDING` → `CANCELLED` | pruebas del servicio de seguimiento (6 en la memoria) | Verificado por pruebas, no re-ejecutado hoy |
| Visibilidad del motivo de rechazo | flujo HU-05 → HU-04 (spec-005, T055) | Cumple |

Sin brechas accionables detectadas por comparación. Falta guardar la salida de la ejecución de las pruebas como evidencia.
