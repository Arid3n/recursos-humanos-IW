# Feature Specification: spec-003 — Solicitud de Vacaciones o Permisos

**Feature Branch**: `spec-003`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-03  
**Asignación**: Integrante 2 (Flujo de Ausencias del Empleado)

## Clarifications

### Session 2026-10-27
- Q: ¿El cómputo de días de ausencia se realiza en días naturales o laborables? → A: Días laborables (lunes a viernes), descontando automáticamente fines de semana y festivos oficiales del calendario de la sede en el backend.
- Q: ¿Los permisos consumen saldo de vacaciones ordinarias? → A: No; las ausencias de tipo permiso (`PERMIT`) tienen bolsa y justificación independiente y no descuentan del saldo anual de vacaciones.
- Q: ¿Se permite solicitar periodos que se solapen con otras ausencias del mismo empleado? → A: No; el sistema bloquea cualquier solicitud cuyas fechas coincidan o se solapen con otra solicitud previa en estado Pendiente o Aprobada del mismo trabajador.
- Q: ¿Existe una antelación mínima obligatoria para registrar una solicitud? → A: No se bloquea el envío para permitir imprevistos justificados, pero se muestra un aviso de recomendación si la solicitud se formula con menos de 7 días de antelación.

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como empleado, quiero cumplimentar un formulario para solicitar un periodo de vacaciones o un permiso especificando el tipo y las fechas deseadas, visualizando el cómputo de días laborables antes de confirmar y garantizando que no excedo mi saldo disponible ni solapo ausencias previas, para gestionar mis ausencias de forma ágil y transparente.

**Why this priority**: Es el punto de inicio de todo el circuito operativo de ausencias; sin la creación de solicitudes, no hay flujo de aprobación ni calendario de equipo.

**Independent Test**: Puede probarse seleccionando un rango de fechas con saldo suficiente, comprobando que la previsualización calcula los días laborables correspondientes descontando festivos y fines de semana, registrando la solicitud y verificando que se almacena en base de datos en estado `Pendiente`.

### Acceptance Scenarios

1. **Solicitud de vacaciones válida**:
   - **Given** un empleado autenticado con 10 días disponibles de vacaciones,
   - **When** completa el formulario seleccionando tipo "Vacaciones", fecha de inicio lunes y fecha de fin viernes (5 días laborables) y confirma,
   - **Then** el sistema crea la solicitud con estado `Pendiente`, la asocia al empleado y a su responsable actual, y muestra un mensaje de confirmación.

2. **Previsualización interactiva de días laborables**:
   - **Given** el formulario de solicitud abierto,
   - **When** el usuario selecciona las fechas de inicio y fin,
   - **Then** el sistema consulta al servidor el cómputo oficial de días laborables (descontando sábados, domingos y festivos) y lo muestra en pantalla antes de que el usuario envíe el formulario (Art. 14 de la Constitución).

3. **Rechazo por saldo insuficiente**:
   - **Given** un empleado con 3 días disponibles,
   - **When** intenta solicitar un periodo de vacaciones que supone 5 días laborables,
   - **Then** el sistema bloquea el envío, no crea la solicitud e informa claramente del saldo disponible frente al número de días requeridos.

4. **Validación de rango de fechas (Fin anterior a Inicio)**:
   - **Given** el formulario de solicitud,
   - **When** el usuario introduce una fecha de fin cronológicamente anterior a la de inicio,
   - **Then** el sistema muestra un error de validación en el formulario impidiendo la creación tanto en cliente como en servidor (`CHECK` en BD).

5. **Detección y bloqueo de solapamientos**:
   - **Given** un empleado con una solicitud previa (Pendiente o Aprobada) del 10 al 15 de julio,
   - **When** intenta crear una nueva solicitud del 12 al 18 de julio,
   - **Then** el sistema rechaza la operación informando del conflicto de fechas con una solicitud existente.

6. **Solicitud de permisos (retribuidos/justificados)**:
   - **Given** un empleado que requiere un permiso especial (mudanza, examen, asunto médico),
   - **When** selecciona tipo "Permiso" y las fechas pertinentes,
   - **Then** la solicitud se crea en estado `Pendiente` sin descontar saldo de vacaciones ordinarias.

### Edge Cases
- **Fines de semana y festivos en el rango:** El cómputo del servidor solo contabiliza días laborables de lunes a viernes, descontando festivos oficiales parametrizados.
- **Rango de 1 solo día:** Si fecha de inicio es igual a fecha de fin, el cómputo resulta en 1 día hábil si es laborable o 0 si coincide en fin de semana/festivo (en cuyo caso no permite tramitar la solicitud).

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El formulario DEBE permitir seleccionar el tipo de ausencia (`VACATION` o `PERMIT`) y las fechas de inicio y fin (`LocalDate`).
- **FR-002**: El servidor DEBE proveer un endpoint para calcular los días laborables entre dos fechas según el calendario laboral oficial, impidiendo que el cliente replique esta lógica por su cuenta (Art. 14).
- **FR-003**: Toda solicitud válida DEBE nacer estrictamente con el estado `PENDIENTE` (`PENDING`).
- **FR-004**: Si el número de días solicitados para vacaciones supera el saldo disponible del empleado, la solicitud NO DEBE crearse y el sistema DEBE devolver un error explicativo.
- **FR-005**: Si `fechaFin < fechaInicio`, el sistema DEBE rechazar la petición con error de validación asociado al campo fecha fin.
- **FR-006**: La solicitud creada DEBE quedar asignada automáticamente al responsable del equipo actual del empleado.
- **FR-007**: El sistema DEBE impedir la creación de solicitudes cuyas fechas se solapen total o parcialmente con otra solicitud del empleado en estado `PENDING` o `APPROVED`.
- **FR-008**: Las solicitudes de tipo `PERMIT` NO DEBEN deducir días del saldo de vacaciones del trabajador.

### Key Entities
- **LeaveRequest (Solicitud de Ausencia)**:
  - `id`: Identificador único (UUID).
  - `employeeId`: Empleado solicitante.
  - `managerId`: Responsable asignado para resolver.
  - `leaveType`: `VACATION` | `PERMIT`.
  - `startDate`, `endDate`: Rango temporal.
  - `calculatedDays`: Días laborables computados.
  - `status`: `PENDING`, `APPROVED`, `REJECTED`.
  - `rejectionReason`: Justificación en caso de rechazo.
  - `createdAt`, `updatedAt`, `version`.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: El cálculo interactivo de días laborables en el formulario responde en menos de 300ms.
- **SC-002**: 100% de solicitudes con solapamiento de fechas rechazadas preventivamente.
- **SC-003**: 100% de solicitudes bloqueadas si exceden el saldo disponible de vacaciones.
- **SC-004**: 0 inconsistencias de fechas (`fechaFin >= fechaInicio` garantizado por constraint en BD).

---

## 4. Assumptions
- El calendario laboral se parametriza de lunes a viernes con festivos oficiales.
- Las ausencias de permisos especiales son evaluadas por el responsable de equipo bajo criterios de justificación documental.
