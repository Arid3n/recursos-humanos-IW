# Feature Specification: spec-008 — Evaluación de Desempeño con Conformidad

**Feature Branch**: `spec-008`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-08  
**Asignación**: Integrante 4 (Analítica de RRHH y Desempeño)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como responsable de equipo, quiero registrar para cada persona a mi cargo una evaluación periódica de desempeño (con objetivos fijados, valoración cualitativa/cuantitativa y comentarios), y como empleado quiero poder revisarla y manifestar mi conformidad, para formalizar de manera inmutable el seguimiento profesional con fecha oficial de cierre.

**Why this priority**: Proporciona el marco formal de desarrollo y revisión del talento dentro de la empresa garantizando la bilateralidad (conformidad de ambas partes) y la protección de datos sensibles.

**Independent Test**: Puede probarse creando una evaluación como responsable para un miembro de su equipo (naciendo en `Pendiente de conformidad`), iniciando sesión con dicho empleado, revisando el contenido y pulsando "Dar conformidad", verificando que pasa a estado `Formalizada`, registra la fecha de formalización y queda bloqueada para futuras ediciones.

### Acceptance Scenarios

1. **Creación de evaluación por responsable**:
   - **Given** un responsable de equipo autenticado,
   - **When** selecciona a un empleado de su equipo y completa los campos: periodo evaluado (p. ej. "Anual 2026"), objetivos evaluados, valoración (p. ej. numérica o escala) y comentarios descriptivos,
   - **Then** el sistema crea la evaluación en estado `Pendiente de conformidad` (`PENDING_ACKNOWLEDGMENT`) y queda accesible para el empleado evaluado.

2. **Revisión y conformidad por el empleado**:
   - **Given** un empleado que tiene una evaluación en estado `Pendiente de conformidad`,
   - **When** accede a su sección de evaluaciones y pulsa el botón "Dar conformidad",
   - **Then** el sistema registra la conformidad del empleado, cambia el estado a `Formalizada` (`FINALIZED`) y almacena la fecha y hora exacta de la formalización.

3. **Inmutabilidad tras la formalización**:
   - **Given** una evaluación en estado `Formalizada`,
   - **When** tanto el empleado como el responsable intentan editar los comentarios, notas u objetivos,
   - **Then** el sistema bloquea cualquier edición (las evaluaciones formalizadas son de solo lectura, Art. 18 de la Constitución).

4. **Aislamiento y privacidad de datos sensibles**:
   - **Given** las evaluaciones de desempeño almacenadas en el sistema,
   - **When** un empleado consulta sus evaluaciones,
   - **Then** solo visualiza las suyas propias; y cuando un responsable accede, solo ve las de los integrantes de su propio equipo (nunca de empleados de otros equipos).

### Edge Cases
- **Comentarios extensos:** El campo de comentarios debe admitir texto enriquecido o multilínea sin perder saltos de línea ni sufrir ataques XSS.
- **Auditoría de conformidad:** La acción de otorgar conformidad debe registrar la identidad del usuario y la marca temporal certificada por el servidor (`Clock` inyectado).

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El responsable de equipo DEBE poder crear evaluaciones únicamente para los empleados asignados a su equipo.
- **FR-002**: Toda evaluación DEBE contener: empleado evaluado, responsable evaluador, periodo de evaluación, objetivos, valoración y comentarios.
- **FR-003**: Toda evaluación recién creada nace en estado `PENDING_ACKNOWLEDGMENT`.
- **FR-004**: El empleado evaluado DEBE tener potestad de revisar su evaluación y pulsar la acción de registrar su conformidad (`Acknowledgment`).
- **FR-005**: Al recibir la conformidad, el estado de la evaluación DEBE pasar automáticamente a `FINALIZED` y registrar el campo `finalizedAt`.
- **FR-006**: Una vez en estado `FINALIZED`, los datos de la evaluación NO DEBEN poder modificarse bajo ninguna circunstancia.
- **FR-007**: Los usuarios con rol `EMPLEADO` solo pueden consultar sus propias evaluaciones. Los usuarios con rol `RESPONSABLE_EQUIPO` solo pueden consultar y evaluar a su equipo (Art. 10 y Art. 18).

### Key Entities
- **PerformanceReview (Evaluación de Desempeño)**:
  - `id`: Identificador único.
  - `employeeId`: Empleado evaluado.
  - `evaluatorId`: Responsable evaluador.
  - `period`: Periodo evaluado (p. ej. "2026-Q1", "2026-ANUAL").
  - `objectives`: Objetivos y metas asignadas.
  - `rating`: Valoración otorgada.
  - `comments`: Comentarios cualitativos.
  - `status`: `PENDING_ACKNOWLEDGMENT` o `FINALIZED`.
  - `employeeAcknowledgedAt`: Fecha/hora de conformidad del empleado.
  - `finalizedAt`: Fecha/hora de formalización inmutable.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: Proceso ágil de evaluación: creación y formalización completadas en menos de 3 pasos en interfaz.
- **SC-002**: 100% de inmutabilidad: ninguna evaluación formalizada puede ser editada o borrada desde la aplicación.
- **SC-003**: 0% de violaciones de privacidad: confidencialidad estricta entre empleados y entre diferentes equipos.

---

## 4. Assumptions
- La firma/conformidad es digital dentro de la propia plataforma registrando la sesión autenticada del empleado.
- La escala de valoración estándar acordada es numérica (1 a 5) o cualitativa estándar según la política interna.
