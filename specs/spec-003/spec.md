# Feature Specification: spec-003 — Solicitud de Vacaciones o Permisos

**Feature Branch**: `spec-003`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-03  
**Asignación**: Integrante 2 (Flujo de Ausencias del Empleado)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como empleado, quiero cumplimentar un formulario para solicitar un periodo de vacaciones o un permiso especificando el tipo y las fechas deseadas, visualizando el cómputo de días antes de confirmar y garantizando que no excedo mi saldo disponible, para gestionar mis ausencias de forma ágil y transparente.

**Why this priority**: Es el punto de inicio de todo el circuito operativo de ausencias; sin la creación de solicitudes, no hay flujo de aprobación ni calendario de equipo.

**Independent Test**: Puede probarse seleccionando un rango de fechas con saldo suficiente, comprobando que la previsualización calcula los días laborables correspondientes, registrando la solicitud y verificando que se almacena en base de datos en estado `Pendiente`.

### Acceptance Scenarios

1. **Solicitud de vacaciones válida**:
   - **Given** un empleado autenticado con 10 días disponibles de vacaciones,
   - **When** completa el formulario seleccionando tipo "Vacaciones", fecha de inicio lunes y fecha de fin viernes (5 días laborables) y confirma,
   - **Then** el sistema crea la solicitud con estado `Pendiente`, la asocia al empleado y a su responsable actual, y muestra un mensaje de confirmación.

2. **Previsualización interactiva de días**:
   - **Given** el formulario de solicitud abierto,
   - **When** el usuario selecciona las fechas de inicio y fin,
   - **Then** el sistema consulta al servidor el cómputo oficial de días según el calendario laboral y lo muestra en pantalla antes de que el usuario envíe el formulario (Art. 14 de la Constitución).

3. **Rechazo por saldo insuficiente**:
   - **Given** un empleado con 3 días disponibles,
   - **When** intenta solicitar un periodo de vacaciones que supone 5 días laborables,
   - **Then** el sistema bloquea el envío, no crea la solicitud e informa claramente del saldo disponible frente al número de días requeridos.

4. **Validación de rango de fechas (Fin anterior a Inicio)**:
   - **Given** el formulario de solicitud,
   - **When** el usuario introduce una fecha de fin cronológicamente anterior a la de inicio,
   - **Then** el sistema muestra un error de validación en el formulario impidiendo la creación tanto en cliente como en servidor (`CHECK` en BD).

5. **Solicitud de permisos (retribuidos/justificados)**:
   - **Given** un empleado que requiere un permiso especial (p. ej. mudanza, examen),
   - **When** selecciona tipo "Permiso" y las fechas pertinentes,
   - **Then** la solicitud se crea en estado `Pendiente` sin descontar saldo de vacaciones ordinarias.

### Edge Cases
- **Fines de semana y festivos en el rango:** El cómputo del servidor solo contabiliza días laborables (salvo que la política de convenio indique naturales), sin contar sábados, domingos ni festivos oficiales.
- **Rango de 1 solo día:** Si fecha de inicio es igual a fecha de fin, el cómputo debe resultar en 1 día hábil si es laborable o 0 si coincide en festivo.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El formulario DEBE permitir seleccionar el tipo de ausencia (`VACATION` o `PERMIT`) y las fechas de inicio y fin (`LocalDate`).
- **FR-002**: El servidor DEBE proveer un endpoint para calcular los días laborables entre dos fechas según el calendario oficial, impidiendo que el cliente replique esta lógica por su cuenta (Art. 14).
- **FR-003**: Toda solicitud válida DEBE nacer estrictamente con el estado `PENDIENTE` (`PENDING`).
- **FR-004**: Si el número de días solicitados para vacaciones supera el saldo disponible del empleado, la solicitud NO DEBE crearse y el sistema DEBE devolver un error explicativo (`422 Unprocessable Entity` o `400 Bad Request`).
- **FR-005**: Si `fechaFin < fechaInicio`, el sistema DEBE rechazar la petición con error de validación asociado al campo fecha fin.
- **FR-006**: La solicitud creada DEBE quedar asignada automáticamente al responsable del equipo actual del empleado.

### Key Entities
- **LeaveRequest (Solicitud de Ausencia)**:
  - `id`: Identificador único (UUID o Long).
  - `employeeId`: Empleado solicitante.
  - `managerId`: Responsable asignado para resolver.
  - `type`: `VACATION` | `PERMIT`.
  - `startDate`: Fecha de inicio.
  - `endDate`: Fecha de fin.
  - `calculatedDays`: Días laborables computados.
  - `status`: `PENDING`, `APPROVED`, `REJECTED`.
  - `rejectionReason`: Motivo de rechazo (opcional al crear, nulo en pendiente).
  - `createdAt`, `updatedAt`, `version`.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: El tiempo de cumplimentación y envío de una solicitud es inferior a 1 minuto.
- **SC-002**: 100% de solicitudes bloqueadas si intentan pedir más días de los disponibles.
- **SC-003**: 0 inconsistencias de fechas (imposibilidad total de crear rangos con fecha fin < inicio en base de datos y API).

---

## 4. Assumptions
- El calendario oficial de festivos está parametrizado en el sistema o sigue el calendario laboral nacional/local aplicable.
- Los permisos especiales se gestionan mediante aprobación de responsable pero no minotan la bolsa de vacaciones ordinaria.
