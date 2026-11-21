# Informe de convergencia: spec-003 (HU-03, solicitud de vacaciones y permisos)

**Responsable**: Iván Soler · **Referencia**: tag `ref-refinamiento`

> Informe de convergencia de 2026-11-21. Método: comparación de `spec.md`, `plan.md` y `tasks.md` entre el tag `ref-refinamiento` (`afed4c4`) y `HEAD`, y contraste de cada obligación modificada con el código (lectura y `grep`; las pruebas se ejecutaron aparte, ver `docs/verificacion-integrada.md`). Una tarea marcada `[X]` no se toma como prueba de cumplimiento.

## Convergencia ya registrada
`tasks.md` tiene la fase «Convergence» (T020-T025, todas `[X]`): campo `justification`, validación en `PERMIT`, rechazo de `startDate` pasada, reserva preventiva de saldo, pruebas y control del formulario.

## Obligaciones verificadas hoy en el código
| Obligación | Evidencia | Resultado |
| :-- | :-- | :-- |
| FR-001 justificación obligatoria en `PERMIT` | `LeaveRequestServiceImpl.java:117` | Cumple |
| FR-005 `endDate >= startDate` y `startDate >= hoy` | `LeaveRequestServiceImpl.java:111-114` (con `Clock`) | Cumple |
| FR-004 saldo con `PENDING` retenidos | `LeaveBalanceServiceImpl.java:165` | Cumple |
| FR-009/FR-010/FR-011 (resolutor, Dirección General, `/eligibility`) | implementados y probados en spec-005 (T062, T063, T071, T072) | Cumple |

## Hallazgos
- N1 (límite conocido, no corregido): el saldo se valida solo para el año de inicio; una solicitud interanual puede crearse y fallar en la aprobación con `422`. Documentado en spec-005; sigue sin tarea en HU-03. **Decisión 2026-11-21**: aceptado como límite conocido (Session 2026-11-21 de `spec.md`).
- T005 cita una consulta de repositorio que no existe (ver spec-002, C1). Corregido el 2026-11-21 (nota en `tasks.md` T005).
