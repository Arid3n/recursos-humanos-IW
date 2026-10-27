# Feature Specification: spec-002 — Consulta de Ficha y Saldo de Vacaciones

**Feature Branch**: `spec-002`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-02  
**Asignación**: Integrante 1 (Grupo RRHH / Personal)

## Clarifications

### Session 2026-10-27
- Q: ¿Cómo se calculan los días de vacaciones para empleados incorporados a mitad de año? → A: Prorrateo proporcional a los meses trabajados en el año natural: `round((días_anuales / 12) * meses_restantes)`, redondeando al entero o medio día más cercano.
- Q: ¿Se permite el arrastre de días de vacaciones no disfrutados al año natural siguiente? → A: No por defecto; las vacaciones corresponden al año natural y vencen el 31 de diciembre (salvo prórroga excepcional hasta el 15 de enero), operando el cálculo sobre el año de ejercicio en curso.
- Q: ¿Cómo se reflejan las solicitudes pendientes en el saldo mostrado al empleado? → A: El desglose oficial normativo mantiene `Correspondientes - Disfrutados = Disponibles`; adicionalmente se muestra un indicador informativo "En trámite (pendientes)" para que el empleado conozca su crédito proyectado.
- Q: ¿Puede el empleado consultar el saldo y ausencias de años fiscales pasados? → A: Sí, la interfaz incluye un selector de año (por defecto el año en curso) que permite consultar el balance histórico consolidado de años anteriores en modo solo lectura.

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como empleado, quiero acceder a mi ficha personal y consultar mi saldo de vacaciones actualizado de forma desglosada (días correspondientes por convenio, disfrutados, pendientes y disponibles), para conocer en cualquier momento mi crédito disponible sin depender de consultas manuales a RRHH.

**Why this priority**: Es la base informativa para que cualquier trabajador conozca su situación antes de planificar o solicitar ausencias (HU-03).

**Independent Test**: Puede probarse autenticándose con un usuario empleado, accediendo a su vista de perfil/ficha y comprobando que se muestra el desglose exacto de días derivados de su convenio actual y de los días que tenga formalizados en el año en curso.

### Acceptance Scenarios

1. **Consulta exitosa de ficha y saldo propio**:
   - **Given** un empleado autenticado en el sistema,
   - **When** accede al apartado "Mi Ficha" / "Mis Vacaciones",
   - **Then** el sistema presenta sus datos personales básicos y el bloque de saldo con el desglose exacto:
     - Días correspondientes según convenio vigente (prorrateados si ingresó en el año).
     - Días disfrutados (solicitudes formalmente aprobadas en el año).
     - Días disponibles (`correspondientes - disfrutados`).
     - Indicador informativo de días en solicitudes actualmente pendientes.

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
- **Empleado contratado a mitad de año:** El cómputo de días correspondientes aplica la fórmula proporcional a los meses activos restantes del año natural.
- **Saldo agotado (cero):** Si el empleado ha disfrutado todos sus días, el disponible muestra 0 sin admitir saldos negativos.
- **Consulta de años anteriores:** El saldo de años pasados muestra los días cerrados y disfrutados de dicho ejercicio sin admitir nuevas solicitudes sobre ejercicios cerrados.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El sistema DEBE obtener la identidad del empleado a partir del contexto de seguridad de la sesión, no de parámetros manipulables por el cliente (Art. 10 de la Constitución).
- **FR-002**: La ficha personal DEBE presentar el saldo de vacaciones desglosado en: *días correspondientes*, *días disfrutados* y *días disponibles* (Art. 12), con un indicador complementario de *días en trámite*.
- **FR-003**: El saldo DEBE ser un valor estrictamente calculado y derivado de la fuente de verdad (días de convenio menos días aprobados), impidiendo contadores editables manualmente.
- **FR-004**: Los cambios contractuales de convenio o la aprobación de solicitudes DEBEN reflejarse de forma inmediata en el cálculo del saldo.
- **FR-005**: El acceso a la ficha de otro empleado DEBE estar estrictamente denegado para el rol `EMPLEADO`.
- **FR-006**: Para incorporaciones durante el año en curso, el sistema DEBE prorratear los días correspondientes según los meses restantes hasta fin de año.
- **FR-007**: La vista DEBE permitir consultar el saldo correspondiente a ejercicios fiscales anteriores en modo solo lectura.

### Key Entities
- **LeaveBalance (Saldo de Vacaciones - Objeto de Dominio Derivado)**:
  - `employeeId`: Identificador del empleado.
  - `year`: Año de ejercicio fiscal consultado.
  - `entitledDays`: Días que corresponden por convenio y fecha de incorporación.
  - `spentDays`: Días aprobados y disfrutados en el año.
  - `pendingDays`: Días solicitados en solicitudes pendientes.
  - `availableDays`: Días resultantes disponibles (`entitledDays - spentDays`).

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: El empleado visualiza su ficha y saldo actualizado en pantalla en menos de 800ms tras acceder.
- **SC-002**: 100% de coherencia matemática: en todo momento `días correspondientes == días disfrutados + días disponibles`.
- **SC-003**: 0% de fugas de datos entre empleados (100% de tests negativos de autorización superados).

---

## 4. Assumptions
- El cómputo anual de vacaciones se rige por el año natural (1 de enero a 31 de diciembre).
- Las solicitudes en estado `Pendiente` no descuentan saldo oficial disponible hasta su aprobación por el responsable.
