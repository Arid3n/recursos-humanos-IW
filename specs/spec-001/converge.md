# Informe de convergencia: spec-001 (HU-01, altas y trayectoria contractual)

**Responsable**: Joaquín Navarro · **Referencia**: tag `ref-refinamiento`

> Informe de convergencia de 2026-11-21. Método: comparación de `spec.md`, `plan.md` y `tasks.md` entre el tag `ref-refinamiento` (`afed4c4`) y `HEAD`, y contraste de cada obligación modificada con el código (lectura y `grep`; las pruebas se ejecutaron aparte, ver `docs/verificacion-integrada.md`). Una tarea marcada `[X]` no se toma como prueba de cumplimiento.

## Cambios de documentación desde la referencia
- `spec.md` (revisada 2026-11-19 desde HU-05): FR-011 revisado (alta sin responsable solo con `withoutManager=true`, rol `DIRECCION_GENERAL`, sin comprobar el puesto), FR-012 nuevo (la baja cancela las `PENDING` de subordinados), FR-013 nuevo (validar el rol del responsable queda como mejora pendiente).
- `data-model.md`, `plan.md` y `tasks.md`: rutas `features/employees` → `employees` y T025 marcada `[X]`. `contracts/employee-api.yaml` ampliado.

## Obligaciones verificadas en el código
| Obligación | Evidencia | Resultado |
| :-- | :-- | :-- |
| FR-011 `withoutManager` (400 si falta o si vienen ambos) | `EmployeeServiceImpl.java:69-74`, `CreateEmployeeRequest.java:48` | Cumple |
| FR-012 cancelar `PENDING` al dar de baja | `EmployeeServiceImpl.java:231` (`cancelPendingOf`) | Cumple |
| FR-009 `PUT /employees/{id}/personal-data` | `EmployeeController.java:64` | Cumple |
| FR-013 validación del rol del responsable | No implementada, declarada pendiente en la spec | Decisión documentada, no es brecha |

## Hallazgos (brechas)
| ID | Tipo | Obligación | Evidencia | Acción propuesta | Estado |
| :-- | :-- | :-- | :-- | :-- | :-- |
| C1 | Cumplimiento parcial | FR-002, FR-007, FR-009 (variación contractual) y US de trayectoria (FR-003/004) | `contract-variation` y `contract-history` existen y T025/T030 están `[X]`, pero ninguna ruta de `app.routes.ts` ni plantilla los usa; solo funciona el endpoint del backend | Tarea nueva: enlazar ambas pantallas (ruta, menú y guard RRHH) con prueba | Aceptado como límite conocido (2026-11-21) |
| C2 | Cumplimiento parcial | FR-010 (baja lógica) | `EmployeeService.deactivateEmployee` existe en el frontend, pero ningún componente lo invoca; el menú solo ofrece «Alta de Empleado» | Tarea nueva: acción de baja en la interfaz de RRHH | Aceptado como límite conocido (2026-11-21) |
| C3 | Contradicción documental | FR-011 dice que las variaciones contractuales «no están soportadas de momento» mientras FR-002/FR-007/FR-009 las exigen | `spec.md` FR-011 frente a FR-002 | Volver a `/speckit-clarify`: decidir si las variaciones están en el alcance. Si lo están, C1 es bloqueante | Aceptado como límite conocido (2026-11-21) |

## Siguiente paso
Resolver C3 con clarify, ejecutar analyze, añadir las tareas de C1 y C2 al final de `tasks.md` e implementarlas con prueba reproducible del fallo previo.


## Decisión de cierre (2026-11-21, código congelado)

El código no puede cambiar, así que C1 y C2 se resuelven en la documentación: `spec.md` (Session 2026-11-21, FR-002, FR-007, FR-009 y FR-010) los declara cumplidos a nivel de API, con pruebas de integración (`EmployeeIntegrationTest`), y la falta de acceso desde la interfaz queda como límite conocido. C3 se resuelve aclarando que FR-011 solo se refiere al alta. **Requiere validación del docente**, porque reduce el alcance de interfaz exigido por la historia. Las notas en `tasks.md` (T025, T030) dejan constancia de que ambos componentes no están enlazados.
