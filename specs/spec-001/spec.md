# Feature Specification: spec-001 — Alta de Empleados y Trayectoria Contractual

**Feature Branch**: `spec-001`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-01  
**Asignación**: Integrante 1 (Grupo RRHH / Personal)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como miembro del departamento de Recursos Humanos (RRHH), quiero dar de alta a nuevos empleados y registrar cualquier modificación en sus condiciones laborales (puesto, departamento, salario, convenio o responsable), asociando siempre una fecha de entrada en vigor inmutable, para reconstruir la trayectoria contractual completa sin pérdida de información histórica.

**Why this priority**: Es la base del sistema; sin empleados ni condiciones contractuales vigentes, ninguna otra funcionalidad (vacaciones, ausencias, evaluaciones) puede operar.

**Independent Test**: Puede probarse de forma aislada realizando el alta de un empleado con sus datos iniciales, modificando posteriormente su departamento con fecha de efectos futura o presente, y verificando que el historial muestra ambos registros en orden cronológico sin sobrescrituras.

### Acceptance Scenarios

1. **Alta exitosa**:
   - **Given** un usuario autenticado con rol `RRHH` en el formulario de alta,
   - **When** introduce nombre, apellidos, NIF/DNI, email corporativo, puesto, departamento, salario bruto anual, convenio colectivo aplicable y responsable de equipo asignado, y pulsa "Guardar",
   - **Then** el sistema persiste el empleado, genera el primer registro histórico contractual marcado con la fecha de alta/efectos y confirma la operación con éxito.

2. **Validación de campos obligatorios en alta**:
   - **Given** un usuario autenticado con rol `RRHH`,
   - **When** intenta guardar un alta omitiendo campos obligatorios (p. ej. salario o responsable),
   - **Then** el sistema rechaza el guardado tanto en cliente como en servidor y muestra los mensajes de error específicos junto a cada campo no cumplimentado.

3. **Modificación contractual con fecha de entrada en vigor (Append-Only)**:
   - **Given** un empleado existente con una trayectoria contractual previa,
   - **When** el usuario de RRHH registra un cambio de puesto y salario indicando una fecha de efectos obligatoria,
   - **Then** se crea un nuevo registro en el historial contractual, manteniéndose intacto el registro histórico anterior.

4. **Rechazo de modificación sin fecha de efectos**:
   - **Given** la ficha de un empleado existente,
   - **When** se intenta registrar una variación contractual sin especificar la fecha de entrada en vigor,
   - **Then** el sistema bloquea la acción indicando que la fecha de efectos es preceptiva.

5. **Consulta de historial cronológico**:
   - **Given** un empleado con múltiples variaciones contractuales a lo largo del tiempo,
   - **When** RRHH consulta la trayectoria contractual del empleado,
   - **Then** se visualiza la lista en orden cronológico detallando valor anterior, valor nuevo, concepto modificado y fecha de efectos.

6. **Control de acceso negativo**:
   - **Given** un usuario con rol `EMPLEADO` o `RESPONSABLE_EQUIPO`,
   - **When** intenta acceder a la funcionalidad de alta o a los endpoints de modificación contractual,
   - **Then** el sistema deniega el acceso con error HTTP `403 Forbidden`.

### Edge Cases
- **Fecha de entrada en vigor futura:** El cambio queda registrado en el historial, pero la condición vigente actual sigue siendo la anterior hasta alcanzar dicha fecha.
- **Valores monetarios con decimales:** El salario debe registrarse con precisión decimal exacta (`NUMERIC`), impidiendo números negativos o valores vacíos.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El sistema DEBE permitir a usuarios con rol `RRHH` dar de alta empleados con datos identificativos y laborales obligatorios.
- **FR-002**: Toda modificación de puesto, departamento, salario, convenio o responsable DEBE exigir una fecha de entrada en vigor.
- **FR-003**: El historial contractual DEBE ser *append-only*; los registros previos no se sobrescriben ni se eliminan (Art. 11 de la Constitución).
- **FR-004**: Los datos contractuales vigentes DEBEN derivarse a partir del registro más reciente cuya fecha de entrada en vigor sea menor o igual a la fecha de consulta.
- **FR-005**: La interfaz DEBE mostrar los errores de validación de formulario asociados a cada campo específico.
- **FR-006**: Cualquier usuario que no tenga el rol `RRHH` DEBE tener restringido el acceso a la creación o modificación de empleados y sus contratos.

### Key Entities
- **Employee**: Identificador único, datos personales (nombre, apellidos, email, documento de identidad), estado activo/inactivo.
- **ContractRecord (Historial Contractual)**: Relación con Employee, fecha de efectos (`effective_from`), puesto (`position`), departamento (`department`), salario (`salary`), convenio aplicable (`collective_agreement_id`), responsable asignado (`manager_id`), marca de auditoría (`created_at`, `created_by`).
- **CollectiveAgreement**: Convenio colectivo que define, entre otros parámetros, los días base de vacaciones anuales.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: Los usuarios de RRHH pueden completar el alta de un nuevo empleado en menos de 2 minutos.
- **SC-002**: El 100% de los cambios contractuales quedan registrados con fecha de efectos sin ninguna sobrescritura destructiva de datos.
- **SC-003**: 0% de accesos no autorizados permitidos a usuarios sin rol RRHH (cobertura con tests de autorización negativa).
- **SC-004**: Trazabilidad completa auditable de quién realizó cada modificación contractual y en qué fecha.

---

## 4. Assumptions
- La autenticación ya provee la identidad y rol del usuario logueado en la sesión/token.
- Cada empleado tiene asignado un único responsable de equipo en un momento determinado.
