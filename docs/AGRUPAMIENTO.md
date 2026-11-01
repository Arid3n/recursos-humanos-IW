# Propuesta de reparto: 4 personas × 2 historias

| Persona | Semana | Historias | Tema |
| :--- | :--- | :--- | :--- |
| **Joaquín** | Semana 1 | HU-01 + HU-02 | **Base:** Alta de empleados, histórico contractual y saldo de vacaciones |
| **Iván** | Semana 2 | HU-03 + HU-04 | **Servicios y derechos del Empleado:** Solicitud de vacaciones/permisos y seguimiento de estados |
| **Andrei** | Semana 3 | HU-05 + HU-06 | **Gestión y Supervisión:** Aprobación/rechazo de solicitudes y calendario de equipo |
| **José** | Semana 4 | HU-07 + HU-08 | **Analítica y Talento:** Informes de absentismo/vacaciones y evaluación de desempeño |

---

## Por qué así

* **Joaquín (HU-01 + HU-02):** Constituyen el *core* de datos de la aplicación. Comparten los modelos de `Empleado`, `Convenio`, `Departamento` y el histórico contractual. Quien gestiona los datos del empleado y su convenio es la persona idónea para codificar la lógica que calcula el saldo de días disponibles.
* **Iván (HU-03 + HU-04):** Conforman la experiencia integral del empleado como solicitante. La HU-03 crea la entidad `Solicitud` en estado `Pendiente` (validando contra el saldo), y la HU-04 consulta y lista esas mismas solicitudes con sus posibles filtros.
* **Andrei (HU-05 + HU-06):** Centralizan el rol del **Responsable de equipo**. HU-05 procesa y resuelve el flujo transaccional (aprobar/rechazar solicitudes pendientes), mientras que HU-06 proyecta visualmente ese resultado en el calendario mensual para controlar coberturas y solapamientos.
* **José (HU-07 + HU-08):** Funcionalidades de reporting y ciclo corporativo. HU-07 explota a nivel analítico y de exportación los datos consolidados de ausencias por departamento, mientras que HU-08 implementa el circuito cerrado de evaluación de desempeño (responsable y empleado).

---

## Dependencias entre personas

* **Joaquín es el cimiento obligatorio:**
  * Define la entidad `Empleado`, roles (`RR. HH.`, `Responsable`, `Empleado`) y jerarquías (`responsable_id`, `departamento_id`).
  * **Iván** necesita la HU-02 de Joaquín para validar si el empleado dispone de saldo suficiente al solicitar ausencias.
  * **Andrei** necesita la jerarquía de Joaquín para filtrar qué solicitudes y miembros corresponden al equipo del responsable.
  * **José** necesita la jerarquía y departamentos de Joaquín tanto para agrupar los informes (HU-07) como para asignar las evaluaciones de desempeño (HU-08).

* **Iván alimenta a Andrei:**
  * Andrei (HU-05, HU-06) no puede listar ni resolver solicitudes si Iván (HU-03) no las crea previamente en estado `Pendiente`.

* **Andrei retroalimenta a Joaquín y desbloquea a José:**
  * Cuando Andrei aprueba una solicitud (HU-05), debe invocar el recálculo o deducción del saldo de vacaciones gestionado por Joaquín (HU-02).
  * José (HU-07) necesita ausencias aprobadas y consolidadas (Andrei) para generar métricas reales de absentismo. Para HU-08, José solo depende de los usuarios y jerarquías de Joaquín.

---

## Acuerdos a cerrar entre las 4 personas antes de empezar

1. **Esquema de autenticación y roles:**
   * Estandarizar los identificadores de roles en sesión/token (`RRHH`, `RESPONSABLE`, `EMPLEADO`).
2. **Ciclo de vida y estados de la Solicitud:**
   * Estados canónicos: `Pendiente`, `Aprobada`, `Rechazada`.
   * Contrato del motivo de rechazo (`motivo_rechazo` nullable en base de datos).
3. **Mecanismo de deducción de saldo:**
   * Definir si el saldo de HU-02 se recalcula dinámicamente consultando las solicitudes aprobadas en la BD o si se actualiza mediante un evento/servicio transaccional cuando Andrei ejecute la aprobación en HU-05.
4. **Formato y granularidad de fechas:**
   * Fechas en formato estándar ISO 8601 (`YYYY-MM-DD`). Cómputo de días laborales vs. naturales según convenio para el cálculo del número de días en HU-03.
5. **Estructura jerárquica en base de datos:**
   * Asegurar que la tabla de empleados contenga `departamento_id` y `responsable_id` (auto-referencia a la misma tabla de empleados).

---

## Orden de ejecución (Plan de 4 semanas)

1. **Semana 1 · Joaquín (HU-01 + HU-02):**
   * Despliegue de modelos de datos: empleados, departamentos, roles, convenios e histórico contractual (HU-01).
   * Lógica de cálculo y visualización del saldo de vacaciones según convenio (HU-02).
   * *Hito:* Base de datos lista y perfiles operativos para el resto del equipo.

2. **Semana 2 · Iván (HU-03 + HU-04):**
   * Formulario de solicitud de ausencias y validación de tope contra el saldo de Joaquín (HU-03).
   * Listado, historial y filtrado de solicitudes para el empleado (HU-04).
   * *Hito:* Los empleados ya pueden solicitar ausencias y ver su estado `Pendiente`.

3. **Semana 3 · Andrei (HU-05 + HU-06):**
   * Panel del responsable para aprobar o rechazar solicitudes pendientes, con notificación y descuento automático de saldo (HU-05).
   * Calendario interactivo del equipo con visualización de ausencias y alerta de solapamientos (HU-06).
   * *Hito:* Ciclo transaccional de ausencias completado de principio a fin.

4. **Semana 4 · José (HU-07 + HU-08):**
   * Informes agregados de absentismo y uso de vacaciones por departamento y periodo, con exportación tabular sobre datos consolidados (HU-07).
   * Módulo completo de evaluación de desempeño con ciclo de objetivos, valoración y doble conformidad responsable-empleado (HU-08).
   * *Hito:* Cierre analítico del sistema y entrega final del proyecto.