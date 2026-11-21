# Registro de clarify: spec-004 (HU-04)

**Responsable**: Iván · **Revisor**: José · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 16 insertions(+), 8 deletions(-) | `docs/evidencias/spec-diffs/spec-004-spec.diff` |
| `plan.md` | 1 file changed, 8 insertions(+), 6 deletions(-) | `docs/evidencias/spec-diffs/spec-004-plan.diff` |
| `tasks.md` | 1 file changed, 21 insertions(+), 10 deletions(-) | `docs/evidencias/spec-diffs/spec-004-tasks.diff` |

## Preguntas y decisiones (9)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-11-17 | ¿Cómo deben persistirse y representarse en el sistema las solicitudes en estado Pendiente que el empleado decide cancelar? | Estado formal `CANCELLED` (`CANCELADA`): la solicitud transiciona a estado `CANCELLED`, permanece visible en el historial con opción de filtrado específico y libera inmediatamente el rango de fechas para no bloquear nuevas solicitudes. | — | `9e00b69 2026-11-17` |
| 2 | Session 2026-11-17 | ¿Qué impacto tiene la cancelación de una solicitud pendiente sobre el saldo disponible del empleado? | Al cancelar una solicitud de vacaciones en estado `PENDING`, los días retenidos preventivamente se liberan de inmediato, restableciendo el saldo disponible para futuras solicitudes. | — | `9e00b69 2026-11-17` |
| 3 | Session 2026-11-17 | ¿Debe mostrarse la justificación del empleado en el seguimiento de solicitudes de HU-04? | Sí; el DTO y la interfaz de HU-04 incluyen el campo `justification` para que el empleado pueda revisar el motivo aportado tanto en vacaciones como en permisos. | — | `9e00b69 2026-11-17` |
| 4 | Session 2026-11-17 | ¿Cuál es el formato del cómputo de días mostrado en el listado de HU-04? | Números enteros de días laborables (`calculatedDays` como `Integer >= 1`), ya que la unidad mínima de ausencia es la jornada completa. | — | `9e00b69 2026-11-17` |
| 5 | Session 2026-11-17 | ¿Pueden figurar solicitudes con fechas pasadas en el listado de HU-04? | Sí; el histórico de HU-04 presenta todas las solicitudes registradas por el empleado con anterioridad en cualquier estado, garantizando la trazabilidad histórica aunque las nuevas solicitudes requieran `startDate >= hoy`. | — | `9e00b69 2026-11-17` |
| 6 | Session 2026-10-27 | ¿Puede el empleado cancelar una solicitud suya mientras permanezca en estado Pendiente? | Sí; el empleado puede cancelar/retirar directamente cualquier solicitud en estado Pendiente, liberando el periodo sin penalización. | — | `aa7b9d6 2026-10-27` |
| 7 | Session 2026-10-27 | ¿Puede el empleado cancelar una solicitud que ya ha sido Aprobada? | No de forma unilateral; una vez Aprobada, la ausencia consolida saldo y calendario, requiriendo solicitar su revocación o ajuste al responsable de equipo. | — | `aa7b9d6 2026-10-27` |
| 8 | Session 2026-10-27 | ¿Cuál es el criterio de ordenación por defecto del listado? | Ordenación cronológica descendente por fecha de registro (`created_at DESC`), situando en primer lugar las solicitudes más recientes. | — | `aa7b9d6 2026-10-27` |
| 9 | Session 2026-10-27 | ¿Cómo se maneja la paginación del histórico? | Paginación en servidor con bloques estándar de 10 registros y selector de página accesible. | — | `aa7b9d6 2026-10-27` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
