# Feature Specification: spec-008 — Evaluación de Desempeño con Conformidad

**Feature Branch**: `spec-008`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-08  
**Asignación**: Integrante 4 (Analítica de RRHH y Desempeño)

## Clarifications

### Session 2026-10-27
- Q: ¿Qué escala de valoración cuantitativa se utiliza en la evaluación? → A: Escala entera de 1 a 5 con descriptores cualitativos estándar (1: Insatisfactorio, 2: En desarrollo, 3: Cumple expectativas, 4: Supera expectativas, 5: Sobresaliente).
- Q: ¿Cómo puede el empleado expresar observaciones o alegaciones antes de otorgar su conformidad? → A: El empleado dispone de un campo opcional "Comentarios del empleado" para añadir sus propias notas u observaciones previas a pulsar el botón de conformidad, las cuales se consolidan de forma inmutable en la formalización.
- Q: ¿Qué sucede exactamente una vez que la evaluación pasa a estado Formalizada? → A: Se registra la marca temporal de cierre (`finalized_at = now()`) y la evaluación pasa a solo lectura permanente; ninguna de las dos partes puede modificar objetivos, puntuaciones ni comentarios (Art. 18 de la Constitución).
- Q: ¿Quién tiene acceso de lectura a las evaluaciones pasadas? → A: El empleado evaluado solo puede consultar sus propias evaluaciones históricas; el responsable de equipo solo puede consultar las evaluaciones de las personas que actualmente pertenezcan a su equipo.

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como responsable de equipo, quiero registrar para cada persona a mi cargo una evaluación periódica de desempeño (con objetivos fijados, valoración cuantitativa 1-5 y comentarios), y como empleado quiero poder revisarla, aportar comentarios y manifestar mi conformidad, para formalizar de manera inmutable el seguimiento profesional con fecha oficial de cierre.

**Why this priority**: Proporciona el marco formal de desarrollo y revisión del talento dentro de la empresa garantizando la bilateralidad (conformidad de ambas partes) y la protección de datos sensibles.

**Independent Test**: Puede probarse creando una evaluación como responsable para un miembro de su equipo (naciendo en `Pendiente de conformidad`), iniciando sesión con dicho empleado, revisando el contenido, añadiendo un comentario y pulsando "Dar conformidad", verificando que pasa a estado `Formalizada`, registra la fecha de formalización y queda bloqueada para futuras ediciones.

### Acceptance Scenarios

1. **Creación de evaluación por responsable**:
   - **Given** un responsable de equipo autenticado,
   - **When** selecciona a un empleado de su equipo y completa los campos: periodo evaluado (p. ej. "2026-ANUAL"), objetivos evaluados, valoración (escala 1 a 5) y comentarios descriptivos,
   - **Then** el sistema crea la evaluación en estado `Pendiente de conformidad` (`PENDING_ACKNOWLEDGMENT`) y queda accesible para el empleado evaluado.

2. **Revisión, comentarios y conformidad por el empleado**:
   - **Given** un empleado con una evaluación en estado `Pendiente de conformidad`,
   - **When** accede a su sección de evaluaciones, añade sus observaciones en "Comentarios del empleado" y pulsa "Dar conformidad",
   - **Then** el sistema registra la conformidad, fija el estado en `Formalizada` (`FINALIZED`) y almacena la fecha y hora exacta de la formalización.

3. **Inmutabilidad absoluta tras la formalización**:
   - **Given** una evaluación en estado `Formalizada`,
   - **When** tanto el empleado como el responsable intentan modificar la puntuación o los textos,
   - **Then** el sistema bloquea cualquier edición (las evaluaciones formalizadas son inmutables de solo lectura, Art. 18 de la Constitución).

4. **Aislamiento y confidencialidad estricta**:
   - **Given** las evaluaciones de desempeño en el sistema,
   - **When** un empleado consulta sus evaluaciones,
   - **Then** solo visualiza las suyas propias; y cuando un responsable accede, solo ve las de los integrantes de su propio equipo (nunca de empleados de otros equipos).

### Edge Cases
- **Escala de calificación:** La valoración se restringe estrictamente a números enteros entre 1 y 5 mediante validación en cliente, servidor y restricción `CHECK` en base de datos.
- **Auditoría temporal:** La fecha de formalización se genera en el servidor utilizando el reloj inyectado (`Clock`), impidiendo manipulación de marcas temporales por el cliente.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El responsable de equipo DEBE poder crear evaluaciones únicamente para los empleados asignados a su equipo.
- **FR-002**: Toda evaluación DEBE contener: empleado evaluado, responsable evaluador, periodo de evaluación, objetivos, valoración (escala 1 a 5) y comentarios.
- **FR-003**: Toda evaluación recién creada nace en estado `PENDING_ACKNOWLEDGMENT`.
- **FR-004**: El empleado evaluado DEBE tener potestad de revisar su evaluación, añadir comentarios propios y registrar su conformidad (`Acknowledgment`).
- **FR-005**: Al recibir la conformidad, el estado de la evaluación DEBE pasar automáticamente a `FINALIZED` y registrar el campo `finalizedAt`.
- **FR-006**: Una vez en estado `FINALIZED`, los datos de la evaluación NO DEBEN poder modificarse bajo ninguna circunstancia.
- **FR-007**: Los usuarios con rol `EMPLEADO` solo pueden consultar sus propias evaluaciones. Los usuarios con rol `RESPONSABLE_EQUIPO` solo pueden consultar y evaluar a su equipo (Art. 10 y Art. 18).
- **FR-008**: La valoración numérica DEBE cumplir la restricción `1 <= rating <= 5`.

### Key Entities
- **PerformanceReview (Evaluación de Desempeño)**:
  - `id`: Identificador único (UUID).
  - `employeeId`: Empleado evaluado.
  - `evaluatorId`: Responsable evaluador.
  - `period`: Periodo evaluado (p. ej. "2026-ANUAL").
  - `objectives`: Objetivos y metas asignadas.
  - `rating`: Valoración numérica (1 a 5).
  - `comments`: Comentarios cualitativos del responsable.
  - `employeeComments`: Observaciones introducidas por el empleado al conformar.
  - `status`: `PENDING_ACKNOWLEDGMENT` o `FINALIZED`.
  - `employeeAcknowledgedAt`: Fecha/hora de conformidad del empleado.
  - `finalizedAt`: Fecha/hora de formalización inmutable.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: Circuito completo de evaluación y formalización realizado en un flujo guiado de menos de 3 pasos.
- **SC-002**: 100% de inmutabilidad: cero modificaciones permitidas en evaluaciones formalizadas.
- **SC-003**: 0% de violaciones de privacidad entre empleados y entre distintos equipos.

---

## 4. Assumptions
- La firma/conformidad es digital dentro de la plataforma autenticada.
- Las evaluaciones corresponden a ciclos periódicos fijados por la dirección (trimestral o anual).
