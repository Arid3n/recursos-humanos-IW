# Registro de clarify: spec-007 (HU-07)

**Responsable**: José · **Revisor**: Andrei · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 5 insertions(+), 1 deletion(-) | `docs/evidencias/spec-diffs/spec-007-spec.diff` |
| `plan.md` | 1 file changed, 106 insertions(+) | `docs/evidencias/spec-diffs/spec-007-plan.diff` |
| `tasks.md` | 1 file changed, 78 insertions(+) | `docs/evidencias/spec-diffs/spec-007-tasks.diff` |

## Preguntas y decisiones (6)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-11-20 | ¿Las tablas del informe en pantalla deben cumplir accesibilidad WCAG 2.1 AA? | Sí. El componente implementa `scope="col"` en cabeceras, `aria-expanded` en botones de expansión de departamento, `role="alert"` en errores y `role="status"` en carga; consistente con el Art. 21 de la Constitución que aplica globalmente a toda la UI. | — | `0c19a3f 2026-11-20` |
| 2 | Session 2026-11-20 | ¿Existen ambigüedades adicionales en la spec tras revisar el código implementado? | No. Todos los requisitos (FR-001 a FR-007, SC-001 a SC-003) están completamente especificados y el comportamiento implementado es fiel a la spec. | FR-001, FR-007, SC-001, SC-003 | `0c19a3f 2026-11-20` |
| 3 | Session 2026-10-27 | ¿Cuál es la fórmula exacta para el cálculo del uso de vacaciones y del índice de absentismo? |  | — | `aa7b9d6 2026-10-27` |
| 4 | Session 2026-10-27 | ¿Qué especificación técnica rige la exportación a CSV? | Archivo CSV estándar RFC 4180 con codificación UTF-8 con BOM (para apertura correcta de tildes y caracteres en Excel), delimitador de comas y cabeceras exactas coincidentes con la vista en pantalla (Art. 17). | — | `aa7b9d6 2026-10-27` |
| 5 | Session 2026-10-27 | ¿Qué opciones de periodo de consulta se ofrecen a RRHH? | Selector con opciones rápidas (Año en curso, Año anterior, Trimestre actual) y selector de rango de fechas personalizado (`startDate` y `endDate`). | — | `aa7b9d6 2026-10-27` |
| 6 | Session 2026-10-27 | ¿Permite el informe ver el desglose individual además del agregado departamental? | Sí; la tabla presenta los totales por departamento y permite desplegar el listado de empleados que componen los totales del departamento. | — | `aa7b9d6 2026-10-27` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
