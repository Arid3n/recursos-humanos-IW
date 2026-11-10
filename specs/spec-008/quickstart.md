# HU-08 — Evaluación de desempeño con conformidad

1. Inicia sesión como responsable de equipo y abre **Evaluaciones**.
2. Selecciona una persona activa de tu equipo, indica periodo, objetivos, valoración del 1 al 5 y comentarios del responsable. Al enviarla, queda **Pendiente de conformidad**.
3. Inicia sesión con la cuenta del empleado evaluado. En **Evaluaciones**, revisa el contenido, añade comentarios opcionales y pulsa **Dar conformidad y formalizar**. La evaluación queda **Formalizada** con la fecha generada por el servidor y no vuelve a ser editable.

La API también restringe los recursos: los empleados consultan solo sus propias evaluaciones y los responsables solo las de los miembros que actualmente tienen asignados. Las operaciones de gestión requieren autenticación HTTP Basic como en el resto del backend.
