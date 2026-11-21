# Specification Analysis Report — spec-005 (HU-05)

**Tipo:** informe inicial de `/speckit-analyze` (antes de aplicar correcciones)
**Fecha:** 2026-11-16
**Alcance:** spec-005 · Aprobación o rechazo de solicitudes (HU-05)

> Solo lectura: no se modificó ningún archivo. No se ejecutó `check-prerequisites.ps1` (no hay `pwsh`) y no existe `.specify/extensions.yml`, así que no corrió ningún hook. Se cargaron `spec.md`, `plan.md`, `tasks.md` y la constitución. Para algunos puntos también se consultaron `research.md`, `data-model.md`, `quickstart.md`, `GlobalExceptionHandler` y `LeaveBalanceServiceImpl`.

---

## 1. Resumen de hallazgos

| ID | Categoría | Severidad | Ubicación | Resumen |
| :-- | :-- | :-- | :-- | :-- |
| C1 | Coverage | **HIGH** | spec.md CA-5 (escenario 7), FR-012; tasks.md | Ninguna tarea comprueba que una solicitud aprobada llega al calendario. |
| C2 | Inconsistency | **HIGH** | tasks.md T010, T013 | Referencias cruzadas erróneas (T036/T040). |
| C3 | Underspec | MEDIUM | FR-003, `RejectLeaveRequest`, `LeaveResolutionServiceImpl.reject` | Longitud del motivo validada sin recortar espacios. |
| C4 | Underspec | MEDIUM | research.md §1.4, T033, T041 | Reparto de días por año no definido si no suma `calculatedDays`. |
| C5 | Risk | MEDIUM | quickstart.md escenario 8, `LeaveBalanceServiceImpl` | No comprobado el saldo de un año futuro. |
| C6 | Constitution | MEDIUM | Art. 10.6, Art. 20.5, FR-013; T034/T035 | Ningún test comprueba el registro (log) de acceso denegado / auto-resolución. |
| C7 | Underspec | MEDIUM | spec.md FR-006, `@PreAuthorize` | Superior sin rol `RESPONSABLE_EQUIPO`: nadie puede resolver. |
| C8 | Inconsistency | MEDIUM | spec.md escenarios 3 y 8, SC-002 | Escenarios sin el máximo de 500 caracteres ni el código 422. |
| C9 | Coverage | MEDIUM | spec.md «User Scenarios» | Auto-resolución, año cerrado y cruce de años sin escenario Given/When/Then. |
| C10 | Inconsistency | LOW | plan.md fila «Art. 1-3» | Fecha de aclaraciones desfasada. |
| C11 | Inconsistency | LOW | spec.md cabecera | Cabecera obsoleta. |
| C12 | Terminology | LOW | FR-004b | Numeración irregular en FR-001…014. |
| C13 | Ordering | LOW | tasks.md Phase 6, grafo | Orden test/migración ambiguo. |
| C14 | Unmapped | LOW | T042 | Tarea de verificación sin cambio de código. |
| C15 | Coverage | LOW | SC-001 | Solo se valida a mano. |

---

## 2. Detalle de cada hallazgo

### C1 · Coverage · HIGH
- **Ubicación:** spec.md CA-5 (escenario 7), FR-012; tasks.md (sin ninguna tarea de calendario).
- **Resumen:** CA-5 exige que la solicitud aprobada aparezca en el calendario del equipo. Ninguna tarea comprueba que una solicitud aprobada llega a `TeamCalendarService` (HU-06). El Art. 2.1 pide un test por CA.
- **Recomendación:** añadir un test de integración: tras aprobar, la ausencia sale en la consulta del calendario del responsable (etiqueta `HU-05.CA-5`).

### C2 · Inconsistency · HIGH
- **Ubicación:** tasks.md T010 («ver T036»), T013 («ver T040»).
- **Resumen:** las referencias cruzadas añadidas están mal. T036 son los tests de frontend (el test de 422 es T035), y T040 es `HolidayCalendarService` (la lógica de saldo y auto-resolución es T041). Quien implemente irá a la tarea equivocada.
- **Recomendación:** corregir a T035 y T041.

### C3 · Underspec · MEDIUM
- **Ubicación:** FR-003, `RejectLeaveRequest` (`@Size(min=5,max=500)`), `LeaveResolutionServiceImpl.reject`.
- **Resumen:** la spec fija 5–500 caracteres tras quitar espacios extremos, pero `@Size` valida el texto sin recortar. Con `" abc "` pasa el DTO y falla luego en el servicio con `IllegalArgumentException`: el 400 sale sin `invalidFields.reason`, y la spec exige el error junto al campo. Además, 500 caracteres + 2 espacios finales se rechazan aunque recortados sean válidos.
- **Recomendación:** validar la longitud sobre el texto recortado (validador propio o `@Size` tras un `strip` en el DTO). Añadir un caso de test con espacios y comprobar `invalidFields.reason`.

### C4 · Underspec · MEDIUM
- **Ubicación:** research.md §1.4 («si no coincide se usa `calculatedDays` como total»), T033, T041.
- **Resumen:** no está definido qué ocurre si el reparto por año no suma `calculatedDays` (p. ej. cambian los festivos desde que se creó la solicitud). «El reparto sólo decide la distribución» no dice cómo repartir.
- **Recomendación:** definir la regla (p. ej. prorratear el desfase al último año) o validar que la suma coincide y fallar con error claro. Cubrirlo en T033.

### C5 · Risk · MEDIUM
- **Ubicación:** quickstart.md escenario 8 (saldo 0 en el año siguiente), `LeaveBalanceServiceImpl.getEmployeeLeaveBalance(employeeId, year)`.
- **Resumen:** no está comprobado que el saldo calcule días correspondientes para un año futuro (convenio, prorrateo de HU-02). Con esa premisa falsa, una solicitud 28/12–08/01 podría bloquearse siempre o nunca.
- **Recomendación:** antes de T041, verificar qué devuelve `calculateBalance` para el año siguiente y fijarlo en la spec o en la fixture de T035.

### C6 · Constitution · MEDIUM
- **Ubicación:** Art. 10.6, Art. 20.5, FR-013; T034/T035.
- **Resumen:** el acceso denegado y la resolución deben quedar registrados. El plan lo cubre con logs WARN/INFO, pero ningún test comprueba que la auto-resolución o el acceso fuera de alcance generan el log (sin datos personales).
- **Recomendación:** añadir una aserción de log (`OutputCaptureExtension` o appender) en T034/T035.

### C7 · Underspec · MEDIUM
- **Ubicación:** spec.md FR-006 («responsable superior»), `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")`.
- **Resumen:** si el `manager_id` de un responsable apunta a alguien sin rol `RESPONSABLE_EQUIPO` (p. ej. RRHH), la solicitud no puede resolverla nadie, aunque la spec diga que la resuelve el superior.
- **Recomendación:** declarar que el superior debe tener ese rol (y quizá validarlo en HU-01), o añadirlo como caso límite.

### C8 · Inconsistency · MEDIUM
- **Ubicación:** spec.md escenarios 3 y 8, SC-002.
- **Resumen:** el escenario 3 y SC-002 dicen «al menos 5 caracteres» sin el máximo de 500. El escenario 8 dice «bloquea… saldo insuficiente» sin código. FR-004b y los demás artefactos ya dicen 422.
- **Recomendación:** ajustar los escenarios de aceptación a 5–500 y 422.

### C9 · Coverage · MEDIUM
- **Ubicación:** spec.md «User Scenarios».
- **Resumen:** auto-resolución, año cerrado y cruce de años solo están en Edge Cases. No tienen escenario Given/When/Then, y los CA verificables salen de los escenarios.
- **Recomendación:** añadir 3 escenarios con su etiqueta CA (CA-1 y CA-5).

### C10 · Inconsistency · LOW
- **Ubicación:** plan.md, fila «Art. 1-3».
- **Resumen:** sigue diciendo «aclaraciones de 2026-10-27».
- **Recomendación:** añadir 2026-11-16.

### C11 · Inconsistency · LOW
- **Ubicación:** spec.md, cabecera («Actualizada 2026-11-03», Draft).
- **Resumen:** cabecera obsoleta.
- **Recomendación:** actualizar fecha y estado.

### C12 · Terminology · LOW
- **Ubicación:** FR-004b.
- **Resumen:** numeración irregular dentro de FR-001…014.
- **Recomendación:** renumerar o aceptar como está.

### C13 · Ordering · LOW
- **Ubicación:** tasks.md Phase 6, grafo (T037 → T035).
- **Resumen:** los tests deben fallar primero, pero el grafo pone la migración antes del test de integración.
- **Recomendación:** aclarar que T035 se escribe antes y solo pasa en verde tras T037.

### C14 · Unmapped · LOW
- **Ubicación:** T042.
- **Resumen:** tarea de verificación sin cambio de código. Puede cerrarse al revisar T043.
- **Recomendación:** fusionar con T035/T043.

### C15 · Coverage · LOW
- **Ubicación:** SC-001 (30 s, ≤ 2 interacciones).
- **Resumen:** solo se valida a mano en el quickstart.
- **Recomendación:** aceptable; anotarlo como verificación manual.

---

## 3. Cobertura de requisitos

| Requisito | ¿Tarea? | Tareas | Nota |
| :-- | :-- | :-- | :-- |
| FR-001 | Sí | T010, T012, T039, T035 | |
| FR-002, FR-003 | Sí | T009–T014, T034, T035, T043 | C3 pendiente |
| FR-004, FR-004b | Sí | T003–T004, T010, T035, T044 | |
| FR-005 | Sí | T004, T009 | |
| FR-006 | Sí | T013, T034, T035, T041 | Ver C7 |
| FR-007 | Sí | T013, T034, T035, T040, T041 | Ver C4, C5 |
| FR-008 | Sí | T010 | |
| FR-009, FR-010 | Sí | T008, T010, T013 (atomicidad en T010) | |
| FR-011 | Sí | T021–T028 | |
| FR-012 | **No** | — | Ver C1 |
| FR-013 | Parcial | T013, T041 | Ver C6 |
| FR-014 | Sí | T019, T027 | |
| SC-001 | Manual | T047 | |
| SC-002…SC-006 | Sí | T010, T035 | |

**Problemas con la constitución:** ninguno CRITICAL. C6 roza los Arts. 10.6 y 20.5. C1 roza el Art. 2.1, porque un CA no tiene test.

**Tareas sin requisito claro:** T042 (C14), T045–T049 (verificación general).

---

## 4. Métricas

| Métrica | Valor |
| :-- | :-- |
| Requisitos (FR + SC) | 20 |
| Tareas | 49 (32 hechas, 17 pendientes) |
| Cobertura con al menos una tarea | 19 de 20 (95 %) |
| Ambigüedades | 4 (C3, C4, C7, C8) |
| Duplicaciones | 0 |
| CRITICAL | 0 |
| HIGH | 2 (C1, C2) |

---

## 5. Siguientes pasos (según el informe)

- Sin CRITICAL: se puede ir a `/speckit-implement`, pero conviene corregir C1 y C2 antes.
- C1: añadir una tarea de test de calendario en `tasks.md`.
- C2: corregir a mano T036→T035 y T040→T041 en las notas «Revisada».
- C3, C4, C5, C7, C8 y C9: ejecutar `/speckit-clarify` (en especial C4, C5 y C7) y después editar `spec.md`.
- C10–C15 son retoques menores.
