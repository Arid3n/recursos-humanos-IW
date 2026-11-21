# Registro de hallazgos · analyze inicial · spec-005 (HU-05)

**Origen:** informe inicial de `/speckit-analyze` (16/11/2026), guardado sin editar aparte.
**Responsable por defecto:** Andrei (HU-05). **Revisor:** por asignar (compañero del grupo).
**Estados:** `Abierto` · `Pendiente de clarify` · `En corrección` · `Cerrado` · `Falso positivo` · `No aplicable`

> La columna **Comprobación** indica lo que se ha verificado leyendo el proyecto (solo lectura). Las **decisiones** son propuestas: el grupo debe validarlas o cambiarlas y justificarlas. Dejo en blanco lo que no he comprobado.

---

## 1. Registro

| ID | Historia / criterio | Gravedad | Comprobación (evidencia) | Decisión propuesta | Responsable | Revisor | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| C1 | HU-05 · CA-5 · FR-012 | HIGH | **Confirmado.** En `tasks.md` ninguna tarea menciona el calendario de HU-06; solo hay tareas de calendario del frontend previas a HU-05 que no prueban este criterio. | Añadir una tarea de test de integración: tras aprobar, la ausencia aparece en la consulta del calendario del responsable (etiqueta `HU-05.CA-5`). Incluir la prueba en la matriz de trazabilidad. | Andrei | | Abierto |
| C2 | Tareas T010, T013 | HIGH | **Confirmado.** T010 dice «ver T036» y T013 dice «ver T040» (dos veces). Según `tasks.md`: T035 = tests de integración backend (el 422); T036 = tests de frontend; T040 = `HolidayCalendarService`; T041 = `LeaveResolutionServiceImpl`. | Corregir T036→T035 y T040→T041 en las notas «Revisada». Mantener los IDs de las tareas. | Andrei | | Abierto |
| C3 | FR-003 · CA-2 | MEDIUM | **Confirmado en código.** `RejectLeaveRequest` usa `@Size(min=5,max=500)` sin recortar; `LeaveResolutionServiceImpl.reject` normaliza después y lanza `IllegalArgumentException`. Un motivo `" abc "` pasa el DTO y falla en el servicio, sin `invalidFields.reason`. | Validar la longitud sobre el texto recortado (validador propio o `strip` en el DTO antes de `@Size`). Añadir test con espacios y con 500 caracteres + espacios finales, y comprobar `invalidFields.reason`. Es corrección de código: va a tareas (y a `converge`). | Andrei | | Abierto |
| C4 | FR-007 · research.md §1.4 | MEDIUM | No comprobado en código. El texto de `research.md` no define cómo repartir si el reparto por año no suma `calculatedDays`. | **Decisión funcional → volver a `clarify`.** Opciones: (a) prorratear el desfase al último año; (b) validar que la suma coincide y fallar con error claro. Cubrir la decisión en T033. | Andrei | | Pendiente de clarify |
| C5 | FR-007 · quickstart escenario 8 | MEDIUM | **Parcialmente comprobado.** En `LeaveBalanceServiceImpl.calculateBalance`, un año futuro (no cerrado) usa la fecha actual como referencia y, si el alta es anterior, devuelve los días completos del convenio (22 por defecto), no 0. La premisa «saldo 0 en el año siguiente» del escenario 8 parece falsa. Falta confirmarlo con una prueba. | Verificar con una prueba el saldo del año siguiente. Fijar el comportamiento en la spec o en la fixture de T035 y ajustar el escenario 8 del quickstart. Si cambia una regla funcional, pasar por `clarify`. Afecta a HU-02 (Joaquín). | Andrei | Joaquín | Pendiente de clarify |
| C6 | FR-013 · Art. 10.6 · Art. 20.5 | MEDIUM | No comprobado. El informe dice que el plan prevé logs WARN/INFO y que ningún test los comprueba. En el código sí hay un `log.info("Auditoría: …")` en `reject`. | Añadir aserciones de log en T034/T035 (acceso fuera de alcance y auto-resolución, sin datos personales). | Andrei | | Abierto |
| C7 | FR-006 | MEDIUM | **Confirmado en parte.** `LeaveResolutionController` tiene `@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")` a nivel de clase, así que un superior sin ese rol no puede resolver. Falta comprobar si HU-01 valida el rol del superior al asignarlo. | **Decisión funcional → `clarify`.** Propuesta: declarar que el superior debe tener el rol `RESPONSABLE_EQUIPO` y añadirlo como caso límite; si se quiere validar al dar de alta, coordinar con HU-01. Relacionado con el CEO sin superior. | Andrei | Joaquín | Pendiente de clarify |
| C8 | CA-2 · SC-002 · escenarios 3 y 8 | MEDIUM | No comprobado en la spec (solo el informe). | Ajustar los escenarios de aceptación a «5–500 caracteres» y al código 422, coherentes con FR-004b. Tras `clarify` si afecta a decisiones. | Andrei | | Abierto |
| C9 | CA-1 · CA-5 | MEDIUM | No comprobado en la spec (solo el informe). | Añadir 3 escenarios Given/When/Then (auto-resolución, año cerrado, cruce de años) con su etiqueta CA. Depende de C4, C5 y C7. | Andrei | | Abierto |
| C10 | Documental | LOW | No comprobado. | Añadir 2026-11-16 a la fila «Art. 1-3» de `plan.md`. | Andrei | | Abierto |
| C11 | Documental | LOW | No comprobado. | Actualizar fecha y estado de la cabecera de `spec.md`. | Andrei | | Abierto |
| C12 | FR-004b | LOW | No comprobado. | Mantener la numeración: renumerar rompería los IDs de los que depende la trazabilidad, y el enunciado pide conservarlos. Documentar como aceptado. | Andrei | | No aplicable (aceptado) |
| C13 | Tareas Phase 6 · grafo | LOW | **Confirmado.** El grafo tiene `T037 --> T035`; la nota de la fase dice que T035 pasa a verde tras T041. | Aclarar en `tasks.md` que T035 se escribe antes y solo pasa en verde cuando T037 y T041 están hechas. | Andrei | | Abierto |
| C14 | T042 | LOW | **Confirmado.** T042 dice que el DTO ya tiene mensajes y `max = 500` y que no hay cambio de código; `RejectLeaveRequest` lo confirma. | No borrar la tarea (conservar historial): marcarla como cubierta por T043 o fusionarla con T035/T043, dejando la nota. | Andrei | | Abierto |
| C15 | SC-001 | LOW | No comprobado. | Aceptado: es una verificación manual en el quickstart (T047). Anotarlo como tal en la matriz. | Andrei | | No aplicable (aceptado) |

---

## 2. Orden de trabajo sugerido

1. **Mecánico, sin decisiones:** C2, C1, C13, C14, C10, C11 (y C12/C15 como aceptados).
2. **`clarify` para las decisiones:** C4, C5, C7.
3. **Con las decisiones tomadas:** C3, C6, C8, C9 (spec, plan y tareas).
4. Repetir `analyze` y guardar el informe final.

## 3. Dependencias con otros integrantes

- **C5:** depende de cómo calcula el saldo HU-02 (Joaquín).
- **C7:** depende de la jerarquía y los roles de HU-01 (Joaquín).
- **C1:** el test de calendario toca HU-06 (también tuya) y `TeamCalendarService`.

## 4. Notas de la comprobación

- Al revisar el proyecto he visto cambios **sin commitear** en `specs/spec-005/` (`plan.md`, `tasks.md`, `research.md`, `data-model.md`, `quickstart.md`, `contracts/leave-resolution-api.yaml`). Parecen ser tus cambios de clarify/analyze.
- Hay una carpeta **sin seguimiento** `Claude outputs/` en la raíz del proyecto, con `analyze-inicial-spec-005.md`. Si no quieres incluirla en el repositorio, muévela fuera o no la añadas a git.
- Esta comprobación es de solo lectura: no se ha ejecutado ningún test ni la aplicación.
