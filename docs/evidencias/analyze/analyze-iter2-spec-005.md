# Specification Analysis Report — spec-005 (HU-05), 2.ª pasada

**Tipo:** informe intermedio de `/speckit-analyze` (iteración 2, tras `clarify` y las primeras correcciones)
**Fecha:** 2026-11-16
**Alcance:** spec-005 · Aprobación o rechazo de solicitudes (HU-05)

> Solo lectura: no se modificó ningún archivo. No se ejecutó `check-prerequisites.ps1` (no hay `pwsh`) y no existe `.specify/extensions.yml`, así que no corrió ningún hook. Se contrastaron `spec.md`, `plan.md`, `tasks.md` y la constitución con `research.md`, `data-model.md`, `quickstart.md` y el código (`LeaveRequestServiceImpl`, `LeaveBalanceServiceImpl`, `LeaveRequestRepository`).

**Resuelto desde la pasada anterior:** C2 (referencias T036/T040 de las notas «Revisada»), C4 (regla de reparto), C5 (saldo de HU-02), C7 (resolutor sin rol o sin superior). Ahora todo es coherente entre spec, plan y tasks: `LeaveDayAllocator`, T040 → T052 → T041, y la nota de T033 de no tocar `HolidayCalendarServiceTest`.

---

## 1. Hallazgos nuevos de esta pasada

| ID | Categoría | Severidad | Ubicación | Resumen |
| :-- | :-- | :-- | :-- | :-- |
| N1 | Inconsistency | **HIGH** | `LeaveRequestServiceImpl` ~L103-108 (HU-03), spec.md FR-007, T052 | HU-03 valida solo con el saldo del año de inicio; HU-05 bloquea luego por el año siguiente. |
| N2 | Coverage | MEDIUM | `LeaveRequestRepository.findByEmployeeAndYear`, `getMyLeaveRequests`, T052 | El listado de HU-02 por año no muestra las solicitudes que cruzan años. |
| N3 | Inconsistency | LOW | research.md §1.4 | Sigue indicando `isWorkingDay` en lugar de `LeaveDayAllocator.daysInYear`. |
| N4 | Inconsistency | LOW | research.md (§1.12, §1.14, §1.15, §1.13) | Secciones desordenadas. |
| N5 | Risk | MEDIUM | T050, T052, `LeaveBalanceServiceImpl.calculateBalance` | El saldo usa `LocalDate.now()` e ignora el `Clock` inyectado. |
| N6 | Ordering | LOW | tasks.md Phase 6 | T050–T052 tras T044, con IDs no correlativos. |

### N1 · Inconsistency · HIGH
- **Ubicación:** `LeaveRequestServiceImpl` ~L103-108 (HU-03), spec.md FR-007, T052.
- **Resumen:** al crear una solicitud, HU-03 compara todos los días laborables con el saldo del año de inicio. Una solicitud 28/12–08/01 se crea con saldo suficiente en el año de inicio, y HU-05 la bloquea luego con 422 por el año siguiente. Además, tras T052 ese saldo de inicio ya descontará el reparto de las solicitudes que cruzan años, así que el resultado de HU-03 cambia. El Art. 12.5 y el Art. 14.3 piden una única regla.
- **Recomendación:** decidir: (a) ampliar el alcance con una tarea que haga que HU-03 valide año a año con `LeaveDayAllocator` y añadir su test; o (b) dejarlo como límite conocido en la spec. Recomienda (a).

### N2 · Coverage · MEDIUM
- **Ubicación:** `LeaveRequestRepository.findByEmployeeAndYear` (mismo filtro de contención `start >= 1 ene AND end <= 31 dic`), `getMyLeaveRequests`, T052.
- **Resumen:** el listado de HU-02 por año tiene el mismo defecto que `sumCalculatedDays`: una solicitud que cruza años no aparece en ninguno de los dos años. Tras T052 el saldo la contaría en ambos años y el listado no la mostraría en ninguno.
- **Recomendación:** incluir en T051/T052 la consulta del listado con solapamiento y un caso en T050.

### N3 · Inconsistency · LOW
- **Ubicación:** research.md §1.4.
- **Resumen:** sigue diciendo que los días de cada año «se obtienen contando los días laborables del tramo con `isWorkingDay`». Es redundante con el reparto de `LeaveDayAllocator.daysInYear` y puede inducir a calcularlo fuera del componente único.
- **Recomendación:** sustituir por «con `LeaveDayAllocator.daysInYear`».

### N4 · Inconsistency · LOW
- **Ubicación:** research.md (orden de §1.12, §1.14, §1.15, §1.13).
- **Resumen:** las secciones no están en orden numérico.
- **Recomendación:** renumerar o reordenar.

### N5 · Risk · MEDIUM
- **Ubicación:** T050, T052, `LeaveBalanceServiceImpl.calculateBalance` usa `LocalDate.now()`.
- **Resumen:** el saldo ignora el `Clock` inyectado. Los tests de cruce de años y año cerrado (T035, T050) dependen de la fecha real. Lo señaló antes, pero ninguna tarea lo trata.
- **Recomendación:** añadir `Clock` al constructor de `LeaveBalanceServiceImpl` dentro de T052, o fijar fixtures relativas al año actual y anotarlo en T050. El Art. 22.5 pide el reloj inyectado.

### N6 · Ordering · LOW
- **Ubicación:** tasks.md Phase 6.
- **Resumen:** T050–T052 aparecen detrás de T044, con IDs no correlativos con el orden de lectura.
- **Recomendación:** aceptable; solo mantener la nota de cabecera.

---

## 2. Hallazgos de la 1.ª pasada que siguen abiertos

(No estaban en el alcance de las últimas ediciones.)

| ID | Severidad | Resumen | Acción |
| :-- | :-- | :-- | :-- |
| C1 | HIGH | Ninguna tarea comprueba que una solicitud aprobada llega al calendario del equipo (CA-5, FR-012, Art. 2.1). | Añadir un test de integración contra `TeamCalendarService`. |
| C3 | MEDIUM | `@Size(min=5,max=500)` se evalúa sin recortar. `"  abc  "` pasa el DTO y falla en el servicio con 400 sin `invalidFields.reason`. | Validar longitud tras `strip()` y cubrirlo en T035. |
| C6 | MEDIUM | Ningún test comprueba que los accesos denegados y las resoluciones generan el log de auditoría sin datos personales (Art. 10.6, 20.5, FR-013). | Aserción de log en T034/T035. |
| C8 | MEDIUM | Los escenarios 3 y 8 de spec.md y SC-002 siguen sin el máximo de 500 ni el 422. | Ajustar los textos. |
| C9 | MEDIUM | Auto-resolución, año cerrado, cruce de años y C7 solo están en Edge Cases, sin escenario Given/When/Then. | Añadir escenarios con su etiqueta CA. |
| C10–C15 | LOW | Cabecera de spec.md y fila «Art. 1-3» de plan.md desactualizadas, numeración de FR-004b, T042 sin código, SC-001 solo manual. | Retoques menores. |

---

## 3. Cobertura de requisitos

| Requisito | ¿Tarea? | Tareas | Nota |
| :-- | :-- | :-- | :-- |
| FR-001 | Sí | T010, T012, T039, T035 | |
| FR-002, FR-003 | Sí | T009–T014, T034, T035, T043 | C3 abierto |
| FR-004, FR-004b | Sí | T003–T004, T010, T035, T044 | |
| FR-005 | Sí | T004, T009 | |
| FR-006 | Sí | T013, T034, T035, T041 | C7 recogido en T035 |
| FR-007 | Sí | T033, T034, T035, T040, T041 | N1 |
| FR-008 | Sí | T010, T050–T052 | N2, N5 |
| FR-009, FR-010 | Sí | T008, T010, T013 | |
| FR-011 | Sí | T021–T028 | |
| FR-012 | **No** | — | C1 |
| FR-013 | Parcial | T013, T041 | C6 |
| FR-014 | Sí | T019, T027 | |
| SC-001 | Manual | T047 | |
| SC-002…SC-006 | Sí | T010, T035 | |

**Constitución:** ninguna violación CRITICAL. N1 roza los Arts. 12.5 y 14.3, C1 el Art. 2.1, C6 los Arts. 10.6 y 20.5 y N5 el Art. 22.5.

---

## 4. Métricas

| Métrica | Valor |
| :-- | :-- |
| Requisitos (FR + SC) | 20 |
| Tareas | 52 (32 hechas, 20 pendientes) |
| Cobertura con al menos una tarea | 19 de 20 (95 %) |
| Ambigüedades abiertas | 3 (C3, C8, C9) |
| Duplicaciones | 0 |
| CRITICAL | 0 |
| HIGH | 2 (N1, C1) |

---

## 5. Siguientes pasos (según el informe)

- Sin CRITICAL se puede ir a `/speckit-implement`. Antes conviene decidir N1 y añadir la tarea de C1.
- N1 es una decisión de alcance: afecta a HU-03, que ya está integrada en `main`.
- Si se opta por (a), se pueden añadir tareas y un test a `tasks.md`, y una línea en la spec. Las ediciones de N2, N3, N4 y N5 son mecánicas.
