# Feature Specification: spec-001 — Alta de Empleados y Trayectoria Contractual

**Feature Branch**: `spec-001`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-01  
**Asignación**: Integrante 1 (Grupo RRHH / Personal)

## Clarifications

### Session 2026-10-27
- Q: ¿Se permite registrar modificaciones contractuales con fecha de entrada en vigor retroactiva (fechas pasadas) o únicamente fechas presentes y futuras? → A: Se admiten fechas pasadas (retroactivas) para regularizaciones laborales, integrándose cronológicamente en el historial contractual sin sobrescrituras ni alteración de auditoría.
- Q: ¿Cómo se gestiona el acceso inicial y las credenciales de usuario del empleado al tramitar su alta? → A: Contraseña provisional autogenerada por el sistema en el momento del alta y visible para RRHH en la confirmación, requiriendo cambio en el primer acceso y sin depender de correo SMTP.
- Q: ¿Cómo deben gestionarse las modificaciones de datos personales o de contacto que no forman parte de las condiciones laborales? → A: Actualización directa sobre la entidad Employee reflejando el estado corriente, reservando el histórico inmutable append-only con fecha de efectos para las condiciones contractuales.
- Q: ¿Cómo debe tramitarse la baja o cese laboral de un empleado en el sistema? → A: Inactivación lógica (active = false) del empleado y de su cuenta de usuario, revocando de inmediato el acceso pero manteniendo intacto el histórico contractual para auditorías y cumplimiento legal.
- Q: ¿Puede un empleado quedar registrado sin responsable de equipo asignado o es estrictamente obligatorio? → A: El responsable de equipo es obligatorio en alta y modificaciones para toda la plantilla, excepto para el puesto de Dirección General / CEO que puede quedar sin superior jerárquico.

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
   - **Then** el sistema persiste el empleado, genera el primer registro histórico contractual marcado con la fecha de alta/efectos, crea automáticamente su cuenta de usuario con una contraseña provisional y la muestra en pantalla a RRHH para su entrega.

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
- **Modificaciones retroactivas (fechas pasadas):** Se admite registrar cambios con fecha de efectos anterior a la actual (p. ej. revisiones salariales aplicables desde inicio de año); el registro se incorpora al histórico *append-only* con su fecha de efectos pasada y su fecha de auditoría actual (`created_at = now()`), ordenándose cronológicamente en la reconstrucción histórica.
- **Fecha de entrada en vigor futura:** El cambio queda registrado en el historial, pero la condición vigente actual sigue siendo la anterior hasta alcanzar dicha fecha.
- **Valores monetarios con decimales:** El salario debe registrarse con precisión decimal exacta (`NUMERIC`), impidiendo números negativos o valores vacíos.
- **Empleado inactivo (baja lógica):** Un empleado con `active = false` tiene revocado el acceso de login y no puede seleccionarse como responsable de equipo en nuevas contrataciones.
- **Puesto de Dirección General / CEO:** Es la única excepción permitida donde `manager_id` puede ser nulo tanto en la ficha como en el contrato.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El sistema DEBE permitir a usuarios con rol `RRHH` dar de alta empleados con datos identificativos y laborales obligatorios.
- **FR-002**: Toda modificación de puesto, departamento, salario, convenio o responsable DEBE exigir una fecha de entrada en vigor.
- **FR-003**: El historial contractual DEBE ser *append-only*; los registros previos no se sobrescriben ni se eliminan (Art. 11 de la Constitución).
- **FR-004**: Los datos contractuales vigentes DEBEN derivarse a partir del registro más reciente cuya fecha de entrada en vigor sea menor o igual a la fecha de consulta.
- **FR-005**: La interfaz DEBE mostrar los errores de validación de formulario asociados a cada campo específico.
- **FR-006**: Cualquier usuario que no tenga el rol `RRHH` DEBE tener restringido el acceso a la creación o modificación de empleados y sus contratos.
- **FR-007**: El sistema DEBE admitir fechas de entrada en vigor retroactivas (fechas pasadas) en las modificaciones contractuales, integrándolas en el orden cronológico de la trayectoria sin alterar registros previos.
- **FR-008**: Al guardar un alta de empleado con éxito, el sistema DEBE crear de forma automática su cuenta de acceso (`users`) con una clave provisional generada de forma segura y presentarla a RRHH en el mensaje de éxito.
- **FR-009**: La modificación de datos personales o de contacto no contractuales DEBE actualizar directamente el registro del empleado (`Employee`), mientras que cualquier cambio en puesto, departamento, salario, convenio o responsable DEBE exigir fecha de efectos y generar un nuevo registro histórico inmutable (`contract_records`).
- **FR-010**: El sistema DEBE permitir a usuarios con rol `RRHH` tramitar la baja de un empleado mediante inactivación lógica (`active = false`), deshabilitando su cuenta de usuario y manteniendo intacta su trayectoria histórica para auditoría.
- **FR-011**: El campo de responsable de equipo (`manager_id`) DEBE ser obligatorio tanto en el alta como en cada variación contractual, admitiéndose valor nulo exclusivamente si el puesto indicado es Dirección General / CEO.

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
