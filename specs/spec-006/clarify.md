# Registro de clarify: spec-006 (HU-06)

**Responsable**: Andrei · **Revisor**: Iván · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 23 insertions(+), 9 deletions(-) | `docs/evidencias/spec-diffs/spec-006-spec.diff` |
| `plan.md` | 1 file changed, 30 insertions(+), 8 deletions(-) | `docs/evidencias/spec-diffs/spec-006-plan.diff` |
| `tasks.md` | 1 file changed, 56 insertions(+), 3 deletions(-) | `docs/evidencias/spec-diffs/spec-006-tasks.diff` |

## Preguntas y decisiones (11)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-10-27 | ¿Cómo se presentan los fines de semana y festivos en la cuadrícula mensual? | Tonalidad atenuada y etiqueta descriptiva del festivo, diferenciando claramente los días laborables de los no computables. | — | `aa7b9d6 2026-10-27` |
| 2 | Session 2026-10-27 | ¿Cómo se indica visualmente un solapamiento de dos o más ausencias? | Indicador visual dual: borde destacado en la casilla e icono de advertencia con texto accesible indicando el número exacto de personas ausentes (sin depender solo del color). | — | `aa7b9d6 2026-10-27` |
| 3 | Session 2026-10-27 | ¿Permite el calendario filtrar por empleado específico dentro del equipo? | Sí; además de la visión global del equipo, incluye un filtro por empleado para consultar individualmente la planificación de una persona a su cargo. | — | `aa7b9d6 2026-10-27` |
| 4 | Session 2026-10-27 | ¿Qué información se muestra al interactuar con una ausencia en el calendario? | Al pulsar sobre una ausencia se despliega un panel o diálogo con el nombre del empleado, tipo de ausencia, rango completo de fechas y estado (Aprobada o Pendiente). | — | `aa7b9d6 2026-10-27` |
| 5 | Session 2026-11-19 (revisión por HU-05, FR-017 y FR-018) | ¿Puede consultar el calendario el rol `DIRECCION_GENERAL`? | Sí, con el mismo alcance que un responsable: solo las personas que lo tienen como responsable directo. | — | `02d5d18 2026-11-19` |
| 6 | Session 2026-11-19 (revisión por HU-05, FR-017 y FR-018) | ¿Puede consultar el calendario el rol `RRHH`? | Sí, con el mismo alcance que un responsable: solo las personas que lo tienen como responsable directo, no toda la empresa (HU-05, FR-018). | FR-018 | `5811c71 2026-11-19` |
| 7 | Session 2026-11-18 | ¿Debe ocultarse del calendario y excluirse del recuento de coincidencias una solicitud retirada por el empleado (estado `CANCELLED`, HU-04), igual que las rechazadas? | Sí; una solicitud retirada no es una ausencia, no se muestra y no cuenta para las coincidencias. | — | `7867b7b 2026-11-18` |
| 8 | Session 2026-11-18 | ¿Qué datos de una ausencia ve el responsable en el calendario y en su detalle: solo nombre, tipo, fechas y estado, o también el motivo o justificación del empleado? | Solo nombre, tipo, rango completo de fechas y estado; nunca la justificación ni ningún texto de motivo (minimización, Art. 20). | — | `7867b7b 2026-11-18` |
| 9 | Session 2026-11-18 | ¿Cómo se muestra el calendario mensual en una pantalla de móvil: cuadrícula completa o lista de días con ausencias? | Cuadrícula mensual en pantallas anchas; en pantallas estrechas, lista vertical de los días laborables con ausencias, con la coincidencia marcada en cada día (Art. 21.2). | — | `7867b7b 2026-11-18` |
| 10 | Session 2026-11-18 | ¿Hay un límite de meses hacia atrás o hacia delante en la navegación del calendario? | No; cualquier mes puede consultarse y los meses sin ausencias muestran el mensaje de «sin ausencias». | — | `7867b7b 2026-11-18` |
| 11 | Session 2026-11-18 | ¿Se marca como coincidencia todo día con dos o más personas ausentes, sea cual sea el tamaño del equipo, o la regla depende de dicho tamaño? | Siempre dos o más personas ausentes, independientemente del tamaño del equipo. | — | `7867b7b 2026-11-18` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
