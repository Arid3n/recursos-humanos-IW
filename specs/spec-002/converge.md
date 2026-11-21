# Informe de convergencia: spec-002 (HU-02, ficha personal y saldo de vacaciones)

**Responsable**: Joaquín Navarro · **Referencia**: tag `ref-refinamiento`

> Informe de convergencia de 2026-11-21. Método: comparación de `spec.md`, `plan.md` y `tasks.md` entre el tag `ref-refinamiento` (`afed4c4`) y `HEAD`, y contraste de cada obligación modificada con el código (lectura y `grep`; las pruebas se ejecutaron aparte, ver `docs/verificacion-integrada.md`). Una tarea marcada `[X]` no se toma como prueba de cumplimiento.

## Cambios de documentación desde la referencia
`spec.md` revisada el 2026-11-19: FR-005 (alcance por rol: RRHH cualquier saldo, responsable el de su equipo directo, empleado el suyo), FR-008 (`DIRECCION_GENERAL` sin saldo propio, `403`), FR-009 (rótulo «Mi Ficha» / «Mi Ficha y Saldo») y SC-004. `plan.md` y `tasks.md`: el endpoint `/employees/me` pasa a `EmployeeController` (T013). Contrato `leave-balance-api.yaml` actualizado.

## Obligaciones verificadas en el código
| Obligación | Evidencia | Resultado |
| :-- | :-- | :-- |
| FR-008 `403` a Dirección General en `/balances/me` y `/balances/employee/{propio}` | `LeaveBalanceServiceImpl.java:60,68-71,93` | Cumple |
| FR-005 denegación fuera de alcance | `LeaveBalanceServiceImpl.java:103` | Cumple |
| FR-009 rótulo del menú | `app.html:12` | Cumple |
| Saldo derivado con reserva de `PENDING` | `LeaveBalanceServiceImpl.java:165` | Cumple |

## Hallazgos
| ID | Tipo | Detalle | Estado |
| :-- | :-- | :-- | :-- |
| C1 | Documento desfasado | `tasks.md` de spec-003 (T005) cita la consulta `sumPendingDaysByEmployeeIdAndYear`, que ya no existe: el saldo pendiente se calcula en `LeaveBalanceServiceImpl.sumDaysInYear` | Cerrado 2026-11-21 (nota en `specs/spec-003/tasks.md` T005) |

Sin brechas accionables de código. Las pruebas de FR-008/FR-009 viven en spec-005 (T092, T097-T099).
