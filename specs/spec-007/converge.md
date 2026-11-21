# Informe de convergencia: spec-007 (HU-07, informes de absentismo)

**Responsable**: José Ramón Castells · **Referencia**: tag `ref-refinamiento`

> Informe de convergencia de 2026-11-21. Método: comparación de `spec.md`, `plan.md` y `tasks.md` entre el tag `ref-refinamiento` (`afed4c4`) y `HEAD`, y contraste de cada obligación modificada con el código (lectura y `grep`; las pruebas se ejecutaron aparte, ver `docs/verificacion-integrada.md`). Una tarea marcada `[X]` no se toma como prueba de cumplimiento.

## Cambios de documentación desde la referencia
En la referencia solo existían `spec.md` y `quickstart.md`. Después se añadieron `plan.md`, `tasks.md`, `research.md`, `data-model.md`, contrato, `analysis.md` e `implementation-report.md`; la spec pasa a «Implemented» y gana la sesión de clarify del 2026-11-20 (accesibilidad WCAG; sin ambigüedades adicionales).

## Obligaciones verificadas en el código
| Obligación | Evidencia | Resultado |
| :-- | :-- | :-- |
| FR-001 solo `RRHH` | `DepartmentReportController.java:24` | Cumple |
| FR-004/SC-002 paridad pantalla y CSV | `DepartmentReportIntegrationTest` (`vacationReportAndCsvShareDepartmentAndEmployeeTotals`) | Cumple |
| FR-005 UTF-8 con BOM | `DepartmentReportServiceImpl.java:112`; la prueba comprueba el primer byte `0xEF` | Cumple |

## Hallazgos
| ID | Tipo | Detalle | Estado |
| :-- | :-- | :-- | :-- |
| C1 | Contradicción entre documentos | `memoria.md` §4.1 da a Dirección General acceso a los informes, pero FR-001 y el código lo limitan a `RRHH` | Resuelto en la dirección del código: solo `RRHH` accede a informes; pendiente corregir `docs/memoria.md` §4.1 |
| C2 | Sin evidencia | SC-001 (menos de 1,5 s) no tiene medición; sin prueba E2E ni de interfaz | Abierto (sin medición, aceptado) |

Sin brechas de código frente a FR-001 a FR-007 en lo comprobado.
