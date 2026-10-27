# Feature Specification: spec-006 — Calendario de Ausencias del Equipo

**Feature Branch**: `spec-006`  
**Created**: 2026-10-27  
**Status**: Ready for Planning  
**Historia de Usuario**: HU-06  
**Asignación**: Integrante 3 (Gestión de Equipo y Ausencias)

## Clarifications

### Session 2026-10-27
- Q: ¿Cómo se presentan los fines de semana y festivos en la cuadrícula mensual? → A: Tonalidad atenuada y etiqueta descriptiva del festivo, diferenciando claramente los días laborables de los no computables.
- Q: ¿Cómo se indica visualmente un solapamiento de dos o más ausencias? → A: Indicador visual dual: borde destacado en la casilla e icono de advertencia con texto accesible indicando el número exacto de personas ausentes (cumpliendo WCAG 2.1 AA sin depender solo del color).
- Q: ¿Permite el calendario filtrar por empleado específico dentro del equipo? → A: Sí; además de la visión global del equipo, incluye un filtro por empleado para consultar individualmente la planificación de una persona a su cargo.
- Q: ¿Qué información se muestra al interactuar con una ausencia en el calendario? → A: Al pulsar sobre un bloque de ausencia se despliega un panel lateral o diálogo con el nombre del empleado, tipo de ausencia, rango completo de fechas y estado (Aprobada o Pendiente).

---

## 1. User Scenarios & Testing *(mandatory)*

### User Story Principal (Priority: P1)
Como responsable de equipo, quiero visualizar un calendario mensual interactivo que muestre las ausencias (aprobadas y pendientes) de los miembros a mi cargo, destacando de forma accesible los días en los que coinciden dos o más ausencias simultáneas, para anticipar problemas de cobertura de servicio y planificar con antelación las guardias o entregas.

**Why this priority**: Permite la toma de decisiones informada del responsable antes de aprobar o rechazar nuevas solicitudes (HU-05) y evita que un equipo quede desatendido.

**Independent Test**: Puede probarse introduciendo ausencias solapadas para dos empleados de un mismo equipo en un mes determinado, accediendo a la vista de calendario como su responsable, comprobando que se visualizan los nombres, las ausencias pendientes y aprobadas con estilos e iconos diferenciados, y que los días con 2 o más ausencias se marcan con un indicador de solape/alerta.

### Acceptance Scenarios

1. **Visualización de ausencias del mes**:
   - **Given** un responsable de equipo autenticado,
   - **When** accede a la sección "Calendario de Equipo" para el mes actual,
   - **Then** el sistema presenta una cuadrícula mensual con los eventos de ausencia de todos los integrantes de su equipo, indicando nombre del empleado, tipo de ausencia y periodo.

2. **Diferenciación visual accesible entre aprobadas y pendientes**:
   - **Given** un mes con ausencias en estado `Aprobada` y otras en estado `Pendiente`,
   - **When** se renderiza el calendario,
   - **Then** las ausencias se diferencian tanto por color como por iconos o tramas/etiquetas textuales (Art. 21 de la Constitución), garantizando accesibilidad WCAG 2.1 AA para personas con daltonismo.

3. **Detección y resalte de solapamientos (>= 2 ausencias)**:
   - **Given** dos o más empleados del equipo con ausencia coincidente en el mismo día laboral,
   - **When** el responsable visualiza dicho día en el calendario,
   - **Then** el día aparece resaltado con un distintivo especial de concurrencia ("⚠️ 2 ausencias coincidentes"), calculado por el servidor (Art. 16.3).

4. **Navegación temporal por meses**:
   - **Given** el calendario abierto en el mes actual,
   - **When** el usuario pulsa en el botón "Mes anterior" o "Mes siguiente",
   - **Then** el sistema consulta al backend únicamente el rango de fechas del mes seleccionado y actualiza la vista sin recargar la página.

5. **Filtrado por empleado del equipo**:
   - **Given** la vista del calendario con múltiples integrantes,
   - **When** el responsable selecciona un empleado concreto en el selector de filtro,
   - **Then** el calendario aísla y muestra únicamente los eventos correspondientes a dicho trabajador.

6. **Restricción de visibilidad a miembros del equipo**:
   - **Given** un responsable de equipo,
   - **When** se consultan las ausencias del calendario,
   - **Then** el sistema garantiza que bajo ningún concepto aparezcan ausencias de personas pertenecientes a otros departamentos o equipos.

### Edge Cases
- **Mes sin ninguna ausencia:** El calendario se muestra limpio con las casillas de días hábiles y festivos correctamente posicionadas.
- **Ausencia que abarca cambio de mes:** La ausencia divide visualmente sus bloques cubriendo los días correspondientes a cada mes al navegar entre ellos.

---

## 2. Requirements *(mandatory)*

### Functional Requirements
- **FR-001**: El backend DEBE exponer un endpoint que reciba el rango del mes (`startDate`, `endDate`) y retorne las ausencias de los integrantes del equipo del responsable en sesión (Art. 16).
- **FR-002**: El servidor DEBE calcular explícitamente qué días del rango registran 2 o más ausencias simultáneas en el equipo, entregando dicho indicador en el contrato API.
- **FR-003**: El calendario DEBE mostrar el nombre del empleado, el tipo (`VACATION`, `PERMIT`) y el estado (`PENDING`, `APPROVED`).
- **FR-004**: La interfaz NO DEBE transmitir el estado únicamente mediante el color; debe incorporar patrones, iconos y textos explicativos (Art. 21).
- **FR-005**: La interfaz DEBE permitir la navegación fluida hacia meses anteriores y posteriores.
- **FR-006**: Las ausencias en estado `REJECTED` NO DEBEN mostrarse en el calendario.
- **FR-007**: La interfaz DEBE incluir un filtro opcional por empleado dentro del equipo del responsable.

### Key Entities
- **TeamCalendarEvent (DTO)**:
  - `employeeId`: Identificador del empleado.
  - `employeeName`: Nombre y apellidos.
  - `leaveType`: Tipo de ausencia.
  - `status`: `PENDING` o `APPROVED`.
  - `date`: Día concreto en el que aplica la ausencia.
- **TeamCalendarDaySummary (DTO)**:
  - `date`: Fecha.
  - `overlappingCount`: Número de personas ausentes.
  - `hasConflict`: Booleano `true` si `overlappingCount >= 2`.
  - `events`: Lista de eventos del día.

---

## 3. Success Criteria *(mandatory)*

- **SC-001**: La carga del calendario para un mes completo tarda menos de 800ms en responder.
- **SC-002**: 100% de los días con 2 o más ausencias concurrentes quedan identificados y resaltados con icono y texto.
- **SC-003**: Cumplimiento íntegro de contraste y accesibilidad visual según normativa WCAG 2.1 AA.
- **SC-004**: Cero eventos de empleados externos mostrados en la vista del responsable.

---

## 4. Assumptions
- El calendario muestra ausencias computadas por días completos.
- Los fines de semana y festivos se identifican visualmente con tonalidad diferenciada.
