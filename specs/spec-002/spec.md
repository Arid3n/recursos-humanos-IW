# Feature Specification: spec-002 — Consulta de Ficha y Saldo de Vacaciones

**Feature Branch**: `spec-002`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-02  
**Asignación**: Integrante 1 (Grupo RRHH / Personal)

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como empleado, quiero acceder a mi ficha personal y consultar mi saldo de vacaciones actualizado de forma desglosada (días correspondientes por convenio, disfrutados y disponibles), para conocer en cualquier momento mi crédito disponible sin depender de consultas manuales a RRHH.

**Why this priority**: Es la base informativa para que cualquier trabajador conozca su situación antes de planificar o solicitar ausencias (HU-03).

**Independent Test**: Puede probarse autenticándose con un usuario empleado, accediendo a su vista de perfil/ficha y comprobando que se muestra el desglose exacto de días derivados de su convenio actual y de los días que tenga formalizados en el año en curso.

### Acceptance Scenarios

1. **Consulta exitosa de ficha y saldo propio**:
   - **Given** un empleado autenticado en el sistema,
   - **When** accede al apartado "Mi Ficha" / "Mis Vacaciones",
   - **Then** el sistema presenta sus datos personales básicos y el bloque de saldo con el desglose exacto:
     - Días correspondientes según convenio vigente.
     - Días disfrutados (o con solicitud aprobada) en el año en curso.
     - Días disponibles (`correspondientes - disfrutados`).

2. **Recálculo automático por variación de convenio**:
   - **Given** un empleado con un saldo determinado basado en un convenio con 22 días laborables,
   - **When** entra en vigor un cambio de convenio contractual a 24 días anuales,
   - **Then** el saldo disponible se actualiza automáticamente a 24 menos los disfrutados, sin requerir intervención manual ni procesos por lotes diferidos.

3. **Recálculo automático tras disfrute/aprobación de vacaciones**:
   - **Given** un empleado con saldo disponible de 15 días,
   - **When** una de sus solicitudes de 5 días es aprobada (HU-05),
   - **Then** al volver a consultar su ficha el contador de disfrutados incrementa en 5 y el disponible refleja inmediatamente 10.

4. **Intento de acceso a ficha ajena (Alcance negativo)**:
   - **Given** un empleado autenticado con ID `A`,
   - **When** intenta consultar la ficha o saldo de otro empleado con ID `B` (ya sea por URL directa o manipulando el parámetro de API),
   - **Then** el sistema rechaza la petición con código HTTP `403 Forbidden` (o `404 Not Found`), garantizando la privacidad de los datos personales.

### Edge Cases
- **Empleado contratado a mitad de año:** El cómputo de días correspondientes debe prorratearse de forma proporcional a los meses o días restantes del año en curso según convenio.
- **Saldo cero:** Si el empleado ha agotado la totalidad de sus días correspondientes, el disponible muestra 0 sin generar números negativos.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El sistema DEBE obtener la identidad del empleado a partir del contexto de seguridad de la sesión, no de parámetros manipulables por el cliente (Art. 10 de la Constitución).
- **FR-002**: La ficha personal DEBE presentar el saldo de vacaciones siempre desglosado en: *días correspondientes*, *días disfrutados* y *días disponibles* (Art. 12).
- **FR-003**: El saldo DEBE ser un valor estrictamente calculado y derivado de la fuente de verdad (días de convenio menos días aprobados), impidiendo contadores editables manualmente.
- **FR-004**: Los cambios contractuales de convenio o la aprobación de solicitudes DEBEN reflejarse de forma inmediata en el cálculo del saldo.
- **FR-005**: El acceso a la ficha de otro empleado DEBE estar estrictamente denegado para el rol `EMPLEADO`.

### Key Entities
- **LeaveBalance (Saldo de Vacaciones - Objeto de Dominio Derivado)**:
  - `employeeId`: Identificador del empleado.
  - `year`: Año de ejercicio fiscal en curso.
  - `entitledDays`: Días que corresponden por convenio y fecha de incorporación.
  - `spentDays`: Días aprobados y disfrutados en el año.
  - `availableDays`: Días resultantes disponibles (`entitledDays - spentDays`).

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: El empleado puede visualizar su saldo actualizado en pantalla en menos de 1 segundo tras iniciar sesión.
- **SC-002**: 100% de coherencia matemática: en todo momento `días correspondientes == días disfrutados + días disponibles`.
- **SC-003**: 0% de fugas de datos entre empleados (100% de tests negativos de autorización superados).

---

## 4. Assumptions
- El cómputo anual de vacaciones se liquida por año natural salvo indicación expresa en convenio.
- Las solicitudes en estado `Pendiente` no descuentan saldo hasta ser formalmente aprobadas (HU-05).
