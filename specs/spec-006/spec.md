# Feature Specification: spec-006 — Calendario de Ausencias del Equipo

**Feature Branch**: `spec-006`

**Created**: 2026-10-27 · **Actualizada**: 2026-11-03

**Status**: Draft

**Historia de Usuario**: HU-06 · **Asignación**: Integrante 3 (Gestión de Equipo y Ausencias)

**Input**: User description: "HU-06 · Calendario de ausencias del equipo. Como responsable de equipo, quiero ver de un vistazo el calendario de ausencias de las personas a mi cargo, para anticipar coincidencias de bajas y detectar problemas de cobertura antes de que ocurran. Criterios de aceptación: (CA-1) El calendario muestra las ausencias de las personas a cargo del responsable con nombre, tipo y fechas, y permite navegar por meses. (CA-2) Las ausencias aprobadas y las pendientes se distinguen visualmente. (CA-3) Los días en que coinciden dos o más ausencias del equipo se resaltan. (CA-4) No aparecen ausencias de personas ajenas a su equipo."

## Clarifications

### Session 2026-10-27

- Q: ¿Cómo se presentan los fines de semana y festivos en la cuadrícula mensual? → A: Tonalidad atenuada y etiqueta descriptiva del festivo, diferenciando claramente los días laborables de los no computables.
- Q: ¿Cómo se indica visualmente un solapamiento de dos o más ausencias? → A: Indicador visual dual: borde destacado en la casilla e icono de advertencia con texto accesible indicando el número exacto de personas ausentes (sin depender solo del color).
- Q: ¿Permite el calendario filtrar por empleado específico dentro del equipo? → A: Sí; además de la visión global del equipo, incluye un filtro por empleado para consultar individualmente la planificación de una persona a su cargo.
- Q: ¿Qué información se muestra al interactuar con una ausencia en el calendario? → A: Al pulsar sobre una ausencia se despliega un panel o diálogo con el nombre del empleado, tipo de ausencia, rango completo de fechas y estado (Aprobada o Pendiente).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el calendario mensual del equipo (Priority: P1)

Como responsable de equipo, abro el calendario del equipo y veo, en una cuadrícula mensual, qué personas a mi cargo están ausentes cada día, con su nombre, el tipo de ausencia y si está aprobada o pendiente. Los días en los que coinciden dos o más ausencias aparecen resaltados para detectar problemas de cobertura. Puedo moverme al mes anterior o siguiente.

**Why this priority**: Es el núcleo de la historia: sin la vista mensual con coincidencias no hay forma de anticipar problemas de cobertura. Complementa la resolución de solicitudes (HU-05), ya que permite valorar el impacto antes de aprobar.

**Independent Test**: Con dos miembros del equipo con ausencias que coinciden en algún día del mes (una aprobada y otra pendiente) y una persona de otro equipo también ausente, autenticarse como responsable, abrir el calendario y comprobar que aparecen las dos ausencias del equipo diferenciadas, que el día coincidente está resaltado con el número de personas ausentes y que la persona ajena no aparece.

**Acceptance Scenarios**:

1. **(CA-1) Given** un responsable cuyo equipo tiene ausencias en el mes actual, **When** abre el calendario del equipo, **Then** ve la cuadrícula del mes actual con cada ausencia en los días que ocupa, mostrando nombre del empleado y tipo de ausencia.
2. **(CA-1) Given** el calendario abierto, **When** pulsa "Mes anterior" o "Mes siguiente", **Then** se muestra el mes correspondiente con sus ausencias, sin recargar la página, y el título indica el mes y año visibles.
3. **(CA-1) Given** una ausencia en el calendario, **When** el responsable la pulsa, **Then** ve el nombre del empleado, el tipo, el rango completo de fechas (aunque abarque varios meses) y el estado.
4. **(CA-2) Given** un mes con ausencias aprobadas y pendientes, **When** se muestra el calendario, **Then** cada ausencia indica su estado con texto o icono además del color, y existe una leyenda que explica la diferencia.
5. **(CA-2) Given** una solicitud rechazada en el mes, **When** se muestra el calendario, **Then** esa ausencia no aparece.
6. **(CA-3) Given** dos o más personas del equipo ausentes el mismo día laborable (aprobadas o pendientes), **When** se muestra el calendario, **Then** ese día aparece resaltado con un borde destacado, un icono de advertencia y el texto con el número de personas ausentes (p. ej. "2 ausencias coincidentes").
7. **(CA-3) Given** un día laborable con una sola persona ausente, **When** se muestra el calendario, **Then** ese día no se marca como coincidencia.
8. **(CA-4) Given** personas de otros equipos con ausencias en el mes, **When** el responsable consulta su calendario, **Then** no aparece ninguna de esas ausencias ni cuentan para las coincidencias.
9. **(CA-4) Given** un usuario sin rol de responsable de equipo, **When** intenta consultar el calendario del equipo, **Then** el sistema le deniega el acceso.

---

### User Story 2 - Filtrar el calendario por empleado (Priority: P2)

Como responsable de equipo, selecciono a una persona concreta de mi equipo en un filtro para ver solo su planificación en el calendario, y puedo volver a la vista de todo el equipo.

**Why this priority**: Facilita revisar la planificación de una persona concreta, pero el valor principal (coincidencias del equipo) ya lo aporta la historia P1.

**Independent Test**: Con dos miembros con ausencias en el mismo mes, seleccionar a uno en el filtro y comprobar que solo se muestran sus ausencias; volver a "Todo el equipo" y comprobar que reaparecen ambas.

**Acceptance Scenarios**:

1. **Given** el calendario con ausencias de varias personas, **When** el responsable selecciona a un empleado en el filtro, **Then** solo se muestran las ausencias de esa persona.
2. **Given** un filtro aplicado, **When** el responsable selecciona "Todo el equipo", **Then** se vuelven a mostrar las ausencias de todas las personas a su cargo.
3. **Given** el filtro por empleado, **When** se despliega, **Then** solo contiene personas del equipo del responsable.

---

### Edge Cases

- **Mes sin ausencias**: la cuadrícula se muestra completa, con fines de semana y festivos diferenciados, y un mensaje indicando que no hay ausencias en el mes.
- **Ausencia que abarca un cambio de mes**: se muestra en los días que caen dentro del mes visible; al navegar al otro mes aparece en sus días restantes. El detalle siempre muestra el rango completo.
- **Fines de semana y festivos**: se muestran atenuados y, en el caso de los festivos, con una etiqueta que lo indica. No se consideran días de ausencia ni cuentan para las coincidencias, ya que no son días laborables.
- **Filtro por empleado y coincidencias**: las coincidencias reflejan siempre la cobertura del equipo completo, también cuando hay un filtro aplicado, porque el riesgo de cobertura es del equipo.
- **Responsable sin personas a cargo**: el calendario se muestra vacío con un mensaje que lo indica.
- **Cambio de responsable**: el calendario muestra las ausencias de las personas que en ese momento tienen asignado al responsable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar al responsable un calendario mensual con las ausencias aprobadas y pendientes de las personas cuyo responsable asignado es él (Art. 10 y 16.1).
- **FR-002**: Cada ausencia DEBE mostrar el nombre del empleado y el tipo de ausencia; su detalle DEBE mostrar además el rango completo de fechas y el estado.
- **FR-003**: El calendario DEBE permitir navegar al mes anterior y al siguiente; cada consulta se limita al mes visible y nunca carga el histórico completo (Art. 16.4).
- **FR-004**: Las ausencias aprobadas y pendientes DEBEN distinguirse mediante color y, además, mediante icono o texto, con una leyenda visible (Art. 16.2 y 21).
- **FR-005**: Las solicitudes rechazadas NO DEBEN mostrarse.
- **FR-006**: El sistema DEBE identificar los días laborables en los que dos o más personas del equipo están ausentes; este cálculo lo realiza el sistema central, no la interfaz (Art. 16.3).
- **FR-007**: Los días con coincidencia DEBEN resaltarse con borde destacado, icono de advertencia y texto con el número de personas ausentes, sin depender solo del color.
- **FR-008**: Fines de semana y festivos DEBEN mostrarse atenuados y los festivos con una etiqueta descriptiva; no cuentan como días de ausencia ni para las coincidencias.
- **FR-009**: El calendario DEBE incluir un filtro opcional por empleado que solo ofrezca personas del equipo del responsable.
- **FR-010**: NO DEBE mostrarse ninguna ausencia de personas ajenas al equipo del responsable en sesión, ni siquiera manipulando la petición; la identidad del responsable se obtiene de la sesión (Art. 10.5).
- **FR-011**: Solo los usuarios con rol de responsable de equipo PUEDEN consultar el calendario del equipo; los intentos fuera de alcance quedan registrados (Art. 10.6).

### Key Entities *(include if feature involves data)*

- **Ausencia del calendario**: vista de una solicitud de ausencia aprobada o pendiente de un miembro del equipo: empleado (identificador y nombre), tipo, fechas de inicio y fin y estado. Se obtiene de las solicitudes existentes (HU-03/HU-05); no se crea ningún dato nuevo.
- **Día del calendario**: fecha del mes visible con su condición (laborable, fin de semana o festivo con su nombre), las ausencias que lo ocupan, el número de personas ausentes y si constituye una coincidencia (dos o más).
- **Miembro del equipo**: persona cuyo responsable asignado es el responsable en sesión; se usa para el filtro.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El responsable visualiza el calendario de un mes completo en menos de 1 segundo tras abrirlo o cambiar de mes.
- **SC-002**: El 100% de los días laborables con dos o más ausencias del equipo aparecen resaltados con icono y texto.
- **SC-003**: El 0% de las ausencias mostradas pertenecen a personas ajenas al equipo del responsable.
- **SC-004**: El estado (aprobada/pendiente) de cada ausencia y las coincidencias se pueden identificar sin percibir colores (verificable en escala de grises).
- **SC-005**: El responsable identifica los días con problemas de cobertura de un mes en menos de 10 segundos.

## Assumptions

- El equipo de un responsable lo forman las personas **activas** que lo tienen asignado como responsable en su ficha; las personas dadas de baja (HU-01) no afectan a la cobertura y no aparecen. A diferencia de la bandeja de HU-05, que sigue mostrando solicitudes pendientes de cualquier persona a cargo para poder resolverlas. Las ausencias propias del responsable no aparecen en su calendario de equipo.
- Las ausencias pendientes cuentan para las coincidencias, porque el objetivo es anticipar problemas antes de aprobar.
- Una ausencia ocupa los días laborables entre su fecha de inicio y de fin, usando el mismo calendario de festivos que el cómputo de días de HU-03.
- La semana del calendario empieza en lunes y el mes inicial es el mes actual (zona horaria Europe/Madrid).
- El filtro por empleado actúa sobre la vista; las coincidencias siempre se calculan sobre todo el equipo.
- Las solicitudes aprobadas por HU-05 y las pendientes de HU-03 son la única fuente de datos; esta historia es de solo consulta.
