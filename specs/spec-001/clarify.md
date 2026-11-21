# Registro de clarify: spec-001 (HU-01)

**Responsable**: Joaquín · **Revisor**: Iván · **Referencia**: tag `ref-refinamiento`

> Registro elaborado el 2026-11-21 a partir de la sección «Clarifications» de `spec.md` y del historial de git (commit = primero que introduce la pregunta). La columna «justificación» es el texto de la respuesta tal como quedó en la spec; no se ha añadido justificación nueva.

## Diferencia con la versión de referencia

| Documento | Cambios frente a `ref-refinamiento` | Diff completo |
| :-- | :-- | :-- |
| `spec.md` | 1 file changed, 6 insertions(+), 3 deletions(-) | `docs/evidencias/spec-diffs/spec-001-spec.diff` |
| `plan.md` | 1 file changed, 1 insertion(+), 1 deletion(-) | `docs/evidencias/spec-diffs/spec-001-plan.diff` |
| `tasks.md` | 1 file changed, 5 insertions(+), 5 deletions(-) | `docs/evidencias/spec-diffs/spec-001-tasks.diff` |

## Preguntas y decisiones (6)

| # | Sesión | Pregunta | Respuesta y justificación | Criterios afectados | Commit |
| :-: | :-- | :-- | :-- | :-- | :-- |
| 1 | Session 2026-10-27 | ¿Se permite registrar modificaciones contractuales con fecha de entrada en vigor retroactiva (fechas pasadas) o únicamente fechas presentes y futuras? | Se admiten fechas pasadas (retroactivas) para regularizaciones laborales, integrándose cronológicamente en el historial contractual sin sobrescrituras ni alteración de auditoría. | — | `aa7b9d6 2026-10-27` |
| 2 | Session 2026-10-27 | ¿Cómo se gestiona el acceso inicial y las credenciales de usuario del empleado al tramitar su alta? | Contraseña provisional autogenerada por el sistema en el momento del alta y visible para RRHH en la confirmación, requiriendo cambio en el primer acceso y sin depender de correo SMTP. | — | `aa7b9d6 2026-10-27` |
| 3 | Session 2026-10-27 | ¿Cómo deben gestionarse las modificaciones de datos personales o de contacto que no forman parte de las condiciones laborales? | Actualización directa sobre la entidad Employee reflejando el estado corriente, reservando el histórico inmutable append-only con fecha de efectos para las condiciones contractuales. | — | `aa7b9d6 2026-10-27` |
| 4 | Session 2026-10-27 | ¿Cómo debe tramitarse la baja o cese laboral de un empleado en el sistema? | Inactivación lógica (active = false) del empleado y de su cuenta de usuario, revocando de inmediato el acceso pero manteniendo intacto el histórico contractual para auditorías y cumplimiento legal. | — | `aa7b9d6 2026-10-27` |
| 5 | Session 2026-10-27 | ¿Puede un empleado quedar registrado sin responsable de equipo asignado o es estrictamente obligatorio? | El responsable de equipo es obligatorio en alta y modificaciones para toda la plantilla, excepto para el puesto de Dirección General / CEO que puede quedar sin superior jerárquico. **Revisada 2026-11-19:** la excepción deja de depender del puesto; todo empleado salvo Dirección General tiene responsable y quien no lo tiene es Dirección General (rol `DIRECCION_GENERAL`), indicándolo de forma explícita en el alta. | — | `aa7b9d6 2026-10-27` |
| 6 | Session 2026-11-21 (cierre con el código congelado) | ¿Qué alcance de interfaz tienen las variaciones contractuales, la consulta de la trayectoria y la baja de empleados en la versión final? | Se entregan como API con pruebas de integración (`POST` y `GET /employees/{id}/contracts`, `DELETE /employees/{id}`, `PUT /employees/{id}/personal-data`); los componentes `contract-variation` y `contract-history` existen y tienen pruebas unitarias, pero no están enlazados en la navegación de la aplicación y la baja no tiene acción en la interfaz. Como el código no puede cambiar, FR-002, FR-007, FR-009 y FR-010 se consideran cumplidos a nivel de servidor y se acepta como límite conocido la ausencia de acceso desde la interfaz. FR-011 se refiere solo al alta: las variaciones contractuales no cambian el responsable en la interfaz. Es una reducción del alcance de interfaz que requiere validación del docente. | FR-002, FR-007, FR-009, FR-010, FR-011 | `?` |

## Qué falta (para el apartado 5.1 del enunciado)

- Casos «Dado… Cuando… Entonces…» de los criterios modificados: están en los escenarios de aceptación de `spec.md` (ver diff); no se reescribieron aquí.
- Prueba de las tres situaciones por historia (habitual, inválida o límite, permisos o estado): ver `docs/recorridos-e2e.md` y `docs/evidencias/`.
- Versión de la herramienta Spec Kit: Spec Kit 1.0.6 (`.specify/init-options.json`, integración `agy`).
