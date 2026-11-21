# Registro de hallazgos · spec-005 (HU-05) · versión 2

**Orígenes:**
- Informe inicial de `analyze` (1.ª pasada, hallazgos C1–C15), guardado sin editar.
- Informe intermedio de `analyze` (2.ª pasada, hallazgos N1–N6).

**Responsable por defecto:** Andrei (HU-05). **Revisor:** por asignar.
**Estados:** `Abierto` · `Pendiente de decisión` · `En corrección` · `Cerrado` · `Falso positivo` · `No aplicable`

> **Comprobación:** lo marcado «verificado» se ha comprobado leyendo el proyecto (solo lectura). Lo demás se toma de los informes de `analyze`. Las decisiones sin marcar como tomadas son propuestas del documento y deben validarse en el grupo.

---

## 1. Hallazgos de la 1.ª pasada (C1–C15)

| ID | Historia / criterio | Gravedad | Evidencia / comprobación | Decisión | Responsable | Revisor | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| C1 | HU-05 · CA-5 · FR-012 | HIGH | Sin tarea de calendario para CA-5 (verificado en 1.ª pasada; sigue abierto en la 2.ª). | Añadir test de integración: tras aprobar, la ausencia sale en el calendario del responsable (`HU-05.CA-5`). | Andrei | | Abierto |
| C2 | Tareas T010, T013 | HIGH | Verificado: las notas «Revisada» ahora dicen «ver T035», «ver T041» y «ver T039». | Referencias corregidas. | Andrei | | **Cerrado** |
| C3 | FR-003 · CA-2 | MEDIUM | `@Size` sin recortar y excepción en el servicio (verificado en el código; sigue abierto en la 2.ª). | Validar tras `strip()`; test con espacios y 500 + espacios; comprobar `invalidFields.reason`. | Andrei | | Abierto |
| C4 | FR-007 | MEDIUM | La 2.ª pasada lo da por resuelto. Verificado: `LeaveDayAllocator` aparece en plan, research y tasks. | Regla de reparto definida mediante `LeaveDayAllocator`. | Andrei | | **Cerrado** (revisar contenido de la regla) |
| C5 | FR-007 · quickstart esc. 8 | MEDIUM | La 2.ª pasada lo da por resuelto («saldo de HU-02»); T052 modifica `calculateBalance` de HU-02. | Saldo por año repartido con `LeaveDayAllocator` (T050–T052). Afecta a Joaquín. | Andrei | Joaquín | **Cerrado** (avisar a Joaquín) |
| C6 | FR-013 · Art. 10.6 · 20.5 | MEDIUM | Sigue abierto en la 2.ª pasada. | Aserción de log en T034/T035 (sin datos personales). | Andrei | | Abierto |
| C7 | FR-006 | MEDIUM | Verificado en `spec.md`: solo resuelve el superior directo con rol `RESPONSABLE_EQUIPO`; sin superior o sin rol, la solicitud queda Pendiente sin resolutor (límite conocido, fuera de alcance). Mejora pendiente en HU-01. | Decisión tomada: límite conocido. Es una decisión del grupo que conviene justificar por escrito (el CEO y los responsables sin superior no pueden tener sus solicitudes resueltas en HU-05). | Andrei | Joaquín | **Cerrado** (decisión de límite documentada) |
| C8 | CA-2 · SC-002 · esc. 3 y 8 | MEDIUM | Sigue abierto en la 2.ª pasada. | Ajustar escenarios a «5–500 caracteres» y 422. | Andrei | | Abierto |
| C9 | CA-1 · CA-5 | MEDIUM | Sigue abierto en la 2.ª pasada. | Añadir escenarios Given/When/Then (auto-resolución, año cerrado, cruce de años, C7). | Andrei | | Abierto |
| C10 | Documental (plan.md) | LOW | Sigue abierto. | Añadir 2026-11-16 a la fila «Art. 1-3». | Andrei | | Abierto |
| C11 | Documental (spec.md) | LOW | Sigue abierto. | Actualizar fecha y estado de la cabecera. | Andrei | | Abierto |
| C12 | FR-004b | LOW | Sigue abierto. | No renumerar para no romper IDs de trazabilidad. | Andrei | | No aplicable (aceptado) |
| C13 | tasks.md Phase 6 · grafo | LOW | Sigue abierto. | Aclarar que T035 se escribe antes y pasa en verde tras T037 y T041. | Andrei | | Abierto |
| C14 | T042 | LOW | Sigue abierto. | No borrar: marcar como cubierta por T043. | Andrei | | Abierto |
| C15 | SC-001 | LOW | Sigue abierto. | Aceptado: verificación manual (T047). | Andrei | | No aplicable (aceptado) |

---

## 2. Hallazgos de la 2.ª pasada (N1–N6)

| ID | Historia / criterio | Gravedad | Evidencia | Decisión | Responsable | Revisor | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| N1 | FR-007 · HU-03 vs HU-05 · Arts. 12.5, 14.3 | HIGH | Informe 2.ª pasada: HU-03 valida con el saldo del año de inicio y HU-05 bloquea después por el año siguiente. | **Pendiente.** (a) HU-03 valida año a año con `LeaveDayAllocator` + test, o (b) límite conocido en la spec. El informe recomienda (a). Requiere hablar con Iván (HU-03, ya en `main`). | Andrei | Iván | Pendiente de decisión |
| N2 | FR-008 · HU-02 listado | MEDIUM | Informe: `findByEmployeeAndYear` usa contención y no muestra solicitudes que cruzan años. | Incluir consulta con solapamiento en T051/T052 y un caso en T050. Afecta a Joaquín. | Andrei | Joaquín | Abierto |
| N3 | research.md §1.4 | LOW | Informe. | Sustituir `isWorkingDay` por `LeaveDayAllocator.daysInYear`. | Andrei | | Abierto |
| N4 | research.md (orden de secciones) | LOW | Informe. | Reordenar las secciones. | Andrei | | Abierto |
| N5 | T050 · T052 · Art. 22.5 | MEDIUM | Informe: `calculateBalance` usa `LocalDate.now()` en lugar del `Clock` inyectado. | Inyectar `Clock` en `LeaveBalanceServiceImpl` dentro de T052. Afecta a Joaquín. | Andrei | Joaquín | Abierto |
| N6 | tasks.md Phase 6 | LOW | Informe. | Aceptado; mantener la nota de cabecera. | Andrei | | No aplicable (aceptado) |

---

## 3. Dependencias con otros integrantes

| Con quién | Hallazgos | Motivo |
| :-- | :-- | :-- |
| Iván (HU-03) | N1 | Cambiar la validación de creación de solicitudes. |
| Joaquín (HU-02 / HU-01) | C5, C7, N2, N5 | Saldo, listado por año, `Clock` y validación del rol del superior. |

## 4. Resumen de estado (tras la 2.ª pasada)

| Estado | Nº | IDs |
| :-- | :-- | :-- |
| Cerrado | 4 | C2, C4, C5, C7 |
| Abierto | 13 | C1, C3, C6, C8, C9, C10, C11, C13, C14, N2, N3, N4, N5 |
| Pendiente de decisión | 1 | N1 |
| No aplicable (aceptado) | 3 | C12, C15, N6 |
| **Total** | **21** | 15 de la 1.ª pasada + 6 de la 2.ª |

> Ajusta este resumen cuando cierres hallazgos. Para el balance de 5.4, usa los denominadores del informe final.

## 5. Próximos pasos

1. Decidir N1 con Iván.
2. Añadir la tarea y el test de C1.
3. Corregir C3, C6, C8 y C9 y las ediciones mecánicas (N2–N5, C10–C14).
4. Matriz de trazabilidad de HU-05 y HU-06, revisada por un compañero.
5. Tercer `analyze` como informe **final**.
