# Registro de clarify: spec-008 (HU-08)

**Responsable**: José · **Revisor**: Iván · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 6 insertions(+), 1 deletion(-) | `docs/evidencias/spec-diffs/spec-008-spec.diff` |
| `plan.md` | 1 file changed, 105 insertions(+) | `docs/evidencias/spec-diffs/spec-008-plan.diff` |
| `tasks.md` | 1 file changed, 80 insertions(+) | `docs/evidencias/spec-diffs/spec-008-tasks.diff` |

## Preguntas y decisiones (8)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-11-20 | ¿Cómo se implementa el control de valoración (1–5) en la UI del responsable? | `<select>` nativo con 5 opciones textuales descriptivas (1–Insatisfactorio, 2–En desarrollo, 3–Cumple expectativas, 4–Supera expectativas, 5–Sobresaliente) ligado con `ngModel`; más accesible que un slider y consistente con Art. 21 (no transmite información solo visualmente). | — | `0c19a3f 2026-11-20` |
| 2 | Session 2026-11-20 | ¿Qué mecanismo de concurrencia protege el flujo de conformidad? | Bloqueo pesimista `PESSIMISTIC_WRITE` en `findByIdForUpdate` del repositorio; como red de seguridad adicional, `@Version` en la entidad para bloqueo optimista. La segunda petición simultánea recibe `409 Conflict`. | — | `0c19a3f 2026-11-20` |
| 3 | Session 2026-11-20 | ¿Existen ambigüedades adicionales en la spec tras revisar el código implementado? | No. Todos los requisitos (FR-001 a FR-008, SC-001 a SC-003) están completamente especificados y el comportamiento implementado es fiel a la spec. | FR-001, FR-008, SC-001, SC-003 | `0c19a3f 2026-11-20` |
| 4 | Session 2026-10-27 | ¿Qué escala de valoración cuantitativa se utiliza en la evaluación? | Escala entera de 1 a 5 con descriptores cualitativos estándar (1: Insatisfactorio, 2: En desarrollo, 3: Cumple expectativas, 4: Supera expectativas, 5: Sobresaliente). | — | `aa7b9d6 2026-10-27` |
| 5 | Session 2026-10-27 | ¿Cómo puede el empleado expresar observaciones o alegaciones antes de otorgar su conformidad? | El empleado dispone de un campo opcional "Comentarios del empleado" para añadir sus propias notas u observaciones previas a pulsar el botón de conformidad, las cuales se consolidan de forma inmutable en la formalización. | — | `aa7b9d6 2026-10-27` |
| 6 | Session 2026-10-27 | ¿Qué sucede exactamente una vez que la evaluación pasa a estado Formalizada? | Se registra la marca temporal de cierre (`finalized_at = now()`) y la evaluación pasa a solo lectura permanente; ninguna de las dos partes puede modificar objetivos, puntuaciones ni comentarios (Art. 18 de la Constitución). | — | `aa7b9d6 2026-10-27` |
| 7 | Session 2026-10-27 | ¿Quién tiene acceso de lectura a las evaluaciones pasadas? | El empleado evaluado solo puede consultar sus propias evaluaciones históricas; el responsable de equipo solo puede consultar las evaluaciones de las personas que actualmente pertenezcan a su equipo. | — | `aa7b9d6 2026-10-27` |
| 8 | Session 2026-11-21 (cierre con el código congelado) | ¿Pueden crear evaluaciones RRHH y Dirección General respecto de sus subordinados directos, que HU-05 FR-017 y FR-018 reconoce como resolutores? | No. La evaluación de desempeño sigue restringida al rol `RESPONSABLE_EQUIPO` (FR-001 y FR-007) y la conformidad al empleado evaluado. Un RRHH o Dirección General con subordinados directos no puede evaluarlos en la versión final. Con el código congelado se acepta como límite conocido y se deja constancia de que la decisión de HU-05 no se propagó a esta historia. | FR-001, FR-007 | `?` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
