# Registro de clarify: spec-003 (HU-03)

**Responsable**: Iván · **Revisor**: Joaquín · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 31 insertions(+), 9 deletions(-) | `docs/evidencias/spec-diffs/spec-003-spec.diff` |
| `plan.md` | 1 file changed, 9 insertions(+), 8 deletions(-) | `docs/evidencias/spec-diffs/spec-003-plan.diff` |
| `tasks.md` | 1 file changed, 20 insertions(+), 9 deletions(-) | `docs/evidencias/spec-diffs/spec-003-tasks.diff` |

## Preguntas y decisiones (11)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-11-19 (revisión por HU-05, FR-015 y FR-017) | ¿Se puede crear una solicitud sin resolutor válido o desde Dirección General? | No. Sin responsable asignado activo, con rol `RESPONSABLE_EQUIPO` o `DIRECCION_GENERAL` y distinto del empleado, la creación se bloquea con `422` y el mensaje «no tienes un responsable asignado». Un usuario `DIRECCION_GENERAL` tampoco puede crear solicitudes (`422`, «Dirección General no solicita ausencias»). La interfaz consulta `GET /api/v1/leave/requests/eligibility` y muestra el mensaje en lugar del formulario. | — | `02d5d18 2026-11-19` |
| 2 | Session 2026-11-17 | ¿Cómo afecta la cancelación de una solicitud al ciclo de vida y solapamiento de ausencias? | Las solicitudes canceladas por el empleado pasan al estado `CANCELLED`, liberando inmediatamente las fechas de forma que no computan como solapamiento para nuevas solicitudes. | — | `9e00b69 2026-11-17` |
| 3 | Session 2026-11-17 | ¿Cómo deben afectar las solicitudes de vacaciones en estado Pendiente al saldo disponible al registrar una nueva solicitud? | Reserva preventiva: las solicitudes en estado `PENDING` restan preventivamente del saldo disponible para nuevas solicitudes (`disponible = total - aprobados - pendientes`). Si la solicitud se rechaza o cancela (HU-04), el saldo retenido se libera inmediatamente. | — | `9e00b69 2026-11-17` |
| 4 | Session 2026-11-17 | ¿Debe incluirse un campo de justificación del empleado y ser obligatorio para ausencias de tipo Permiso? | Obligatorio en Permisos y opcional en Vacaciones: se incluye el campo `justification` (máx. 255 caracteres), obligatorio al seleccionar `leaveType == PERMIT` y opcional para notas aclaratorias en `VACATION`. | — | `9e00b69 2026-11-17` |
| 5 | Session 2026-11-17 | ¿Se admiten solicitudes por medios días o fracciones de jornada? | Solo días completos: la unidad mínima indivisible de solicitud es 1 día laborable completo (`calculatedDays` como entero positivo `Integer >= 1`). No se contemplan fracciones de jornada ni medios días. | — | `9e00b69 2026-11-17` |
| 6 | Session 2026-11-17 | ¿Pueden registrarse solicitudes con fechas de inicio en el pasado (retroactivas) en HU-03? | Estrictamente presente o futuro (`startDate >= hoy`): el autoservicio bloquea solicitudes con fecha de inicio en el pasado tanto para vacaciones como para permisos; regularizaciones retroactivas corresponden a gestión administrativa de RRHH. | — | `9e00b69 2026-11-17` |
| 7 | Session 2026-10-27 | ¿El cómputo de días de ausencia se realiza en días naturales o laborables? | Días laborables (lunes a viernes), descontando automáticamente fines de semana y festivos oficiales del calendario de la sede en el backend. | — | `aa7b9d6 2026-10-27` |
| 8 | Session 2026-10-27 | ¿Los permisos consumen saldo de vacaciones ordinarias? | No; las ausencias de tipo permiso (`PERMIT`) tienen bolsa y justificación independiente y no descuentan del saldo anual de vacaciones. | — | `aa7b9d6 2026-10-27` |
| 9 | Session 2026-10-27 | ¿Se permite solicitar periodos que se solapen con otras ausencias del mismo empleado? | No; el sistema bloquea cualquier solicitud cuyas fechas coincidan o se solapen con otra solicitud previa en estado Pendiente o Aprobada del mismo trabajador. | — | `aa7b9d6 2026-10-27` |
| 10 | Session 2026-10-27 | ¿Existe una antelación mínima obligatoria para registrar una solicitud? | No se bloquea el envío para permitir imprevistos justificados, pero se muestra un aviso de recomendación si la solicitud se formula con menos de 7 días de antelación. | — | `aa7b9d6 2026-10-27` |
| 11 | Session 2026-11-21 (cierre con el código congelado) | ¿Se valida el saldo de cada año natural cuando una solicitud de vacaciones cruza el 31 de diciembre? | No. HU-03 valida el saldo disponible del año de inicio; HU-05 reparte los días por año natural al aprobar (`LeaveDayAllocator`) y devuelve `422` si falta saldo en el año siguiente. La solicitud puede crearse y fallar en la aprobación. Con el código congelado se acepta como límite conocido (antes pendiente de decisión, hallazgo N1 de spec-005). | — | `?` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
