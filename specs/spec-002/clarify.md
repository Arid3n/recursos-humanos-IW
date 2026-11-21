# Registro de clarify: spec-002 (HU-02)

**Responsable**: Joaquín · **Revisor**: Andrei · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 16 insertions(+), 5 deletions(-) | `docs/evidencias/spec-diffs/spec-002-spec.diff` |
| `plan.md` | 1 file changed, 8 insertions(+), 2 deletions(-) | `docs/evidencias/spec-diffs/spec-002-plan.diff` |
| `tasks.md` | 1 file changed, 7 insertions(+), 1 deletion(-) | `docs/evidencias/spec-diffs/spec-002-tasks.diff` |

## Preguntas y decisiones (6)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-11-19 (revisión por HU-05, FR-017) | ¿Tiene saldo de vacaciones propio el rol `DIRECCION_GENERAL`, que no solicita ausencias? | No. La ficha personal se mantiene, pero el apartado de saldo se oculta en la interfaz y el servidor responde `403` a `GET /leave/balances/me` y a `GET /leave/balances/employee/{propio id}`, con registro de acceso denegado. `RRHH` conserva su acceso total. | — | `5811c71 2026-11-19` |
| 2 | Session 2026-10-27 | ¿Cómo se calculan los días de vacaciones para empleados incorporados a mitad de año? | Prorrateo proporcional a los meses trabajados en el año natural: `round((días_anuales / 12) * meses_restantes)`, redondeando al entero o medio día más cercano. | — | `aa7b9d6 2026-10-27` |
| 3 | Session 2026-10-27 | ¿Se permite el arrastre de días de vacaciones no disfrutados al año natural siguiente? | No por defecto; las vacaciones corresponden al año natural y vencen el 31 de diciembre (salvo prórroga excepcional hasta el 15 de enero), operando el cálculo sobre el año de ejercicio en curso. | — | `aa7b9d6 2026-10-27` |
| 4 | Session 2026-10-27 | ¿Cómo se reflejan las solicitudes pendientes en el saldo mostrado al empleado? | El desglose oficial normativo mantiene `Correspondientes - Disfrutados = Disponibles`; adicionalmente se muestra un indicador informativo "En trámite (pendientes)" para que el empleado conozca su crédito proyectado. | — | `aa7b9d6 2026-10-27` |
| 5 | Session 2026-10-27 | ¿Puede el empleado consultar el saldo y ausencias de años fiscales pasados? | Sí, la interfaz incluye un selector de año (por defecto el año en curso) que permite consultar el balance histórico consolidado de años anteriores en modo solo lectura. | — | `aa7b9d6 2026-10-27` |
| 6 | Session 2026-11-17 | ¿Qué permisos de consulta sobre el saldo de vacaciones tienen los roles de Responsable de Equipo y Recursos Humanos respecto a otros empleados? | RRHH puede consultar el saldo de cualquier empleado; el Responsable de Equipo únicamente el de las personas que pertenezcan a su equipo directo; y el Empleado solo el suyo propio, denegando con 403 Forbidden cualquier consulta fuera de estos alcances. | — | `1d748dd 2026-11-18` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
