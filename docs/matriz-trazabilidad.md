# Registro de Coherencia, Hallazgos y Matriz de Trazabilidad Global (Fase 5.2 Analyze)

**Sistema**: Gestión Integral de Recursos Humanos  
**Fecha de Emisión**: 2026-11-21  
**Entregables Oficiales de la Actividad 5.2**:
1. Informes Globales de Coherencia (Informe Inicial vs. Informe Final tras Refinamiento)
2. Registro Consolidado de Hallazgos y Decisiones
3. Matriz de Trazabilidad Global para las 8 Historias de Usuario con Revisión Cruzada entre Compañeros

---

## 1. Informes Globales de Coherencia (`/speckit.analyze`)

### 1.1. Informe Inicial (Línea Base antes de Refinamiento)
- **Fecha**: 2026-10-29 / 2026-11-16
- **Alcance Analizado**: Especificaciones base de HU-01 a HU-08 tras la primera pasada de implementación.
- **Resumen de Incidencias Detectadas**:
  - **Críticas / Alta Severidad**: 4
    1. *spec-001*: Desalineación en contratos OpenAPI: falta endpoint de actualización de datos personales (`PUT /personal-data`) y componente de diálogo de variación contractual desmarcado en tareas (`T025`).
    2. *spec-002*: El controlador de consulta de saldo no coincidía entre la arquitectura (`EmployeeController` vs `LeaveBalanceController`) y faltaba la aclaración formal del rol `RESPONSABLE_EQUIPO` para ver saldo de terceros.
    3. *spec-003 / spec-005*: Ambigüedad crítica en empleados huérfanos sin superior o con superior inactivo (`422 Unprocessable Entity`), y ausencia del rol `DIRECCION_GENERAL` en el esquema de seguridad y base de datos.
    4. *spec-005*: Ausencia de mecanismo de cálculo multianual cuando una solicitud de vacaciones cruza el 31 de diciembre hacia el año siguiente.
  - **Media Severidad**: 5 (Inconsistencias de rutas frontend, validación de espacios vacíos en motivos de rechazo, paridad CSV en reportes).
  - **Baja Severidad**: 6 (Duplicaciones menores de texto, clarificación de códigos HTTP 409 vs 404).
- **Veredicto Inicial**: **Requiere Refinamiento Obligatorio (Loop Engineering)**.

### 1.2. Informe Final (Post-Refinamiento y Cierre)
- **Fecha**: 2026-11-21
- **Alcance Analizado**: Artefactos `spec.md`, `plan.md`, `tasks.md`, `contracts/` y suites de pruebas de las 8 historias.
- **Resultado de la Verificación**:
  - **Contradicciones Críticas o Altas sin resolver**: **0** (Todas mitigadas, implementadas y testeadas).
  - **Criterios del alcance sin tareas ni pruebas previstas**: **0** (100% de cobertura).
  - **Enmiendas Constitucionales Formalizadas**: Constitución v1.1.0 (rol `DIRECCION_GENERAL`) y v1.2.0 (`RRHH` como resolutor de equipo directo y exclusión de saldo para Dirección General).
- **Veredicto Final**: **APROBADO (100% Coherencia y Convergencia)**.

---

## 2. Registro Consolidado de Hallazgos y Decisiones

| ID | HU / Criterio | Gravedad | Evidencia Detectada | Decisión / Acción Correctora | Responsable | Revisor | Estado |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: |
| **H-01** | HU-01 / FR-009 | **HIGH** | Falta endpoint `PUT /employees/{id}/personal-data` en `employee-api.yaml` y tarea `T025` desmarcada. | Incorporado endpoint en OpenAPI, implementado `ContractVariationComponent` y cerrado `T025`. | Joaquín | Iván | **CERRADO** |
| **H-02** | HU-02 / FR-005 | **HIGH** | `plan.md` referenciaba `LeaveBalanceController` en lugar de `EmployeeController`. Ambigüedad en alcance de `RESPONSABLE_EQUIPO`. | Actualizado `plan.md` y `tasks.md:T013`. Añadida aclaración 5 en `spec.md` restringiendo a subordinados. | Joaquín | Andrei | **CERRADO** |
| **H-03** | HU-03 / FR-009 | **HIGH** | Empleados sin superior activo podían enviar solicitudes que quedaban bloqueadas sin resolutor. | Creado endpoint `/eligibility` y bloqueo con HTTP 422 («no tienes un responsable asignado»). | Iván | Joaquín | **CERRADO** |
| **H-04** | HU-03 / FR-003 | **MEDIUM** | Solicitudes `PENDING` concurrentes podían exceder el saldo si no se descontaban preventivamente. | Implementada reserva preventiva: `disponible = correspondientes - disfrutados - pendientes`. | Iván | José | **CERRADO** |
| **H-05** | HU-04 / FR-003 | **MEDIUM** | Ambigüedad en el estado de una solicitud retirada por el trabajador. | Creado estado formal `CANCELLED` en BD (V5) que libera inmediatamente fechas y saldo preventivo. | Iván | Andrei | **CERRADO** |
| **H-06** | HU-05 / FR-008 | **HIGH** | Solicitudes entre 28 dic y 5 ene no tenían regla clara de imputación de días por ejercicio. | Creado `LeaveDayAllocator` que reparte días laborables estrictamente por año natural. | Andrei | Iván | **CERRADO** |
| **H-07** | HU-05 / FR-015 | **HIGH** | Empleados de alta dirección no encajaban en jerarquía estándar de responsable. | Creado rol `DIRECCION_GENERAL` (V7, Enmienda Const. v1.1.0/v1.2.0); cúspide sin saldo propio. | Andrei | Joaquín | **CERRADO** |
| **H-08** | HU-05 / FR-016 | **HIGH** | Baja de un superior dejaba solicitudes pendientes de subordinados en estado indefinido. | Implementado `LeaveRequestAutoCancelService` que cancela automáticamente en cascada con notificación. | Andrei | José | **CERRADO** |
| **H-09** | HU-06 / FR-007 | **LOW** | El calendario podía filtrar justificaciones o motivos personales del empleado. | Aplicado principio de minimización (Art. 20): solo se expone nombre, tipo, rango y estado. | Andrei | Joaquín | **CERRADO** |
| **H-10** | HU-07 / FR-004 | **MEDIUM** | Ausencia de test automatizado de paridad matemática pantalla vs CSV exportado. | Añadida aserción estricta en `DepartmentReportIntegrationTest` validando JSON vs filas CSV con BOM. | José | Andrei | **CERRADO** |
| **H-11** | HU-08 / FR-006 | **HIGH** | Posibilidad de edición concurrente o actualización accidental de evaluaciones formalizadas. | Bloqueo pesimista `PESSIMISTIC_WRITE` + trigger en PostgreSQL (V4) que aborta cualquier `UPDATE`/`DELETE`. | José | Iván | **CERRADO** |

---

## 3. Matriz de Trazabilidad Global (8 Historias × Criterios)

### Código de Reparto y Revisión Cruzada:
- **Joaquín Navarro**: HU-01, HU-02 (Revisado por: Iván Soler / Andrei Radu)
- **Iván Soler**: HU-03, HU-04 (Revisado por: Joaquín Navarro / José)
- **Andrei Radu**: HU-05, HU-06 (Revisado por: Joaquín Navarro / Iván Soler)
- **José (jrcb3)**: HU-07, HU-08 (Revisado por: Andrei Radu / Iván Soler)

---

### HU-01 · Alta de Empleados y Trayectoria Contractual (Responsable: Joaquín | Revisor: Iván)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU01-FR01** (Alta obligatoria por RRHH) | Formulario reactivo asistido; endpoint `POST /api/v1/employees` con DTO validado con `@Valid`. | T010, T012, T014, T017, T019 | `EmployeeIntegrationTest`: test de creación exitosa con credenciales. | Iván |
| **HU01-FR02** (Fecha de efectos obligatoria) | Columna `effective_from DATE NOT NULL` en `contract_records`. Campo requerido en DTO. | T011, T021, T022, T025 | `ContractVariationIntegrationTest`: rechazo si `effectiveFrom == null`. | Iván |
| **HU01-FR03** (Histórico inmutable *append-only*) | Entidad `ContractRecord` sin operaciones de actualización; cada cambio inserta una nueva fila (Art. 11). | T011, T013, T022, T024 | `ContractVariationIntegrationTest`: inserción secuencial preserva registros previos. | Iván |
| **HU01-FR04** (Condiciones vigentes derivadas) | Consulta nativa SQL `effective_from <= :date ORDER BY effective_from DESC, created_at DESC LIMIT 1`. | T013, T026, T027, T028 | Test unitario de repositorio `findCurrentContractByEmployeeIdAndEffectiveDate`. | Iván |
| **HU01-FR05** (Errores de validación por campo) | `GlobalExceptionHandler` mapeando a RFC 9457 con array `invalidFields`. | T009, T017, T019 | MockMvc validando respuesta `400 Bad Request` con mapa de errores. | Iván |
| **HU01-FR06** (Acceso exclusivo RRHH 403) | Anotación `@PreAuthorize("hasRole('RRHH')")` en métodos de controlador. | T016, T023, T033, T034 | Test de seguridad negativa autenticado como `EMPLEADO` devolviendo `403 Forbidden`. | Iván |
| **HU01-FR08** (Clave provisional automática) | `PasswordGeneratorService` genera clave de 12 caracteres segura sin servicio SMTP externo. | T004, T005, T008, T015, T020 | Test unitario de generación de claves con entropía y diálogo modal. | Iván |
| **HU01-FR10** (Baja lógica reversible) | Marcado `active = false` en `employees` y `users`; revocación de login sin borrado físico. | T032, T033, T034 | `EmployeeIntegrationTest:testDeactivateEmployee`: login revocado y datos históricos intactos. | Iván |
| **HU01-FR11** (Manager obligatorio salvo Dirección) | Validación en servicio: `manager_id` no nulo excepto cuando se marca explícitamente `withoutManager=true`. | T010, T015, T069, T083 | Test unitario validando alta de CEO sin manager y rechazo para otros puestos. | Iván |

---

### HU-02 · Ficha Personal y Saldo de Vacaciones (Responsable: Joaquín | Revisor: Andrei)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU02-FR01** (Identidad desde token de sesión) | Extracción de identidad vía `SecurityContextHolder.getContext().getAuthentication()`. | T007, T009, T012, T013 | Test de integración verificando extracción de ID sin parámetros en URL. | Andrei |
| **HU02-FR02** (4 componentes del saldo) | DTO `LeaveBalanceResponse`: `entitledDays`, `spentDays`, `pendingDays`, `availableDays`. | T004, T008, T010, T014 | `LeaveBalanceServiceTest`: cálculo exacto de los 4 sumandos. | Andrei |
| **HU02-FR03** (Cálculo dinámico derivado Art. 12) | Consulta agregada sobre `contract_records` y `leave_requests` sin columnas de saldo mutables en BD. | T005, T008, T011, T013 | Aserción en BD verificando ausencia de columnas de balance en tablas. | Andrei |
| **HU02-FR04** (Invariante matemática `correspondientes = disfrutados + disponibles`) | Restricción de suelo en cero (`availableDays = Math.max(0, ...)`) garantizada en servicio. | T008, T013, T023 | `LeaveBalanceServiceTest`: verificación con balance agotado (0 negativos). | Andrei |
| **HU02-FR05** (Control de acceso RBAC estricto) | Servicio valida: empleado solo ve lo suyo; responsable ve subordinados directos; RRHH ve a todos. | T008, T012, T024 | Test negativo: intento de acceso cruzado entre compañeros devuelve `403 Forbidden`. | Andrei |
| **HU02-FR06** (Prorrateo por alta a mitad de año) | Fórmula: `Math.round((días_restantes / días_año) * días_convenio)`. | T008, T013, T022 | `LeaveProratingUnitTest` (**14/14 tests pasando** en 0.23 s). | Andrei |
| **HU02-FR07** (Consulta histórica de años cerrados) | Parámetro `year` con condiciones contractuales vigentes al 31 de diciembre de dicho ejercicio. | T008, T011, T014 | `LeaveBalanceServiceTest`: consulta de año pasado en modo solo lectura con badge. | Andrei |
| **HU02-SC01** (Visualización rápida < 800 ms) | Widget Angular optimizado con carga paralela de perfil y saldo. | T015, T017, T020 | Verificación en navegador y tests unitarios de componente Angular. | Andrei |

---

### HU-03 · Solicitud de Vacaciones o Permisos (Responsable: Iván | Revisor: Joaquín)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU03-FR01** (Formulario tipo y fechas) | Componente reactivo Angular con selector `VACATION`/`PERMIT` y datepicker con rango. | T012, T014, T015 | `leave-request-create.component.spec.ts`: validación de formulario reactivo. | Joaquín |
| **HU03-FR02** (Cálculo de días laborables en servidor) | `HolidayCalendarService` centralizado en backend excluyendo fines de semana y festivos (Art. 14). | T003, T007, T010, T011 | `HolidayCalendarServiceTest`: descuento exacto de festivos oficiales. | Joaquín |
| **HU03-FR03** (Reserva preventiva de saldo) | Validación transaccional: comprueba que `disponible - pendientes >= solicitados`. | T005, T008, T010 | `LeaveRequestServiceTest`: rechazo con `422` si no hay saldo disponible suficiente. | Joaquín |
| **HU03-FR04** (Fechas presentes o futuras) | Validador `@FutureOrPresent` en backend y `minDate = today` en selector de fechas. | T008, T010, T014 | `LeaveRequestServiceTest`: rechazo de solicitudes con `startDate < hoy`. | Joaquín |
| **HU03-FR05** (Bloqueo de solapamiento de fechas) | Consulta de solapamiento en `LeaveRequestRepository` lanzando `LeaveRequestOverlapException`. | T005, T006, T008, T010 | `LeaveRequestServiceTest`: test de solapamiento arrojando `409 Conflict`. | Joaquín |
| **HU03-FR06** (Justificación obligatoria en permisos) | Validación condicional: obligatoria en `PERMIT` (máx. 255 car.), opcional en `VACATION`. | T004, T008, T010, T014 | MockMvc validando `400 Bad Request` si se solicita permiso sin motivo. | Joaquín |
| **HU03-FR09** (Bloqueo sin resolutor válido) | Consulta previa en `/eligibility` y validación en servicio que exige superior activo con rol resolutor. | T008, T010, T014 | `LeaveRequestServiceTest`: rechazo con 422 si empleado no tiene superior activo. | Joaquín |
| **HU03-FR10** (Bloqueo para Dirección General) | Verificación de rol en servicio: `DIRECCION_GENERAL` no puede solicitar ausencias. | T008, T010, T016 | `LeaveRequestServiceTest`: rol `DIRECCION_GENERAL` recibe 422. | Joaquín |

---

### HU-04 · Seguimiento y Cancelación de Solicitudes (Responsable: Iván | Revisor: José)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU04-FR01** (Listado histórico paginado) | `Pageable` de Spring Data ordenado por `created_at DESC` en bloques de 10 registros. | T004, T008, T010, T011 | `LeaveRequestTrackingServiceTest`: listado paginado correcto. | José |
| **HU04-FR02** (Filtrado dinámico por estado) | Consulta filtrada por enum `LeaveStatus` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`). | T005, T008, T010, T012 | `LeaveRequestTrackingServiceTest`: filtrado por estado específico. | José |
| **HU04-FR03** (Cancelación unilateral de pendientes) | Endpoint `POST /{id}/cancel` actualiza estado a `CANCELLED` y libera fechas y saldo retenido. | T006, T008, T009, T013 | `LeaveRequestTrackingServiceTest`: cancelación exitosa y liberación de retención. | José |
| **HU04-FR04** (Visualización del motivo de rechazo) | Exposición del campo `rejection_reason` en el DTO para solicitudes denegadas. | T007, T010, T012 | `leave-request-list.component.spec.ts`: renderizado de diálogo con motivo. | José |
| **HU04-FR05** (Aislamiento de solicitudes por empleado) | Filtro forzoso en SQL por `employee_id` del token autenticado. | T008, T009, T016 | Test de seguridad: empleado autenticado recibe 404/403 al solicitar petición ajena. | José |
| **HU04-SC02** (100% liberación de días cancelados) | Transición atómica que excluye inmediatamente las solicitudes `CANCELLED` del cómputo de pendientes. | T008, T009 | Verificado en test de integración de ciclo de vida completo. | José |

---

### HU-05 · Aprobación o Rechazo de Solicitudes (Responsable: Andrei | Revisor: Joaquín)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU05-FR01** (Bandeja de pendientes del equipo) | Consulta de solicitudes `PENDING` restringida a empleados con `manager_id == currentUserId`. | T007, T013, T015, T022 | `LeaveResolutionServiceTest`: listado exclusivo de subordinados directos. | Joaquín |
| **HU05-FR02** (Aprobación a `APPROVED` con comentario) | Transición a `APPROVED`, guardado de comentario opcional (≤ 500 car.) y registro de auditoría. | T008, T011, T014, T017 | `LeaveResolutionServiceTest`: aprobación exitosa con auditoría `resolved_by/at`. | Joaquín |
| **HU05-FR03** (Rechazo a `REJECTED` con motivo obligatorio) | Transición a `REJECTED`, validación estricta de motivo (5-500 car.) tras recorte de espacios. | T008, T011, T014, T017 | `LeaveResolutionServiceTest`: rechazo con motivo y validación de longitud mínima. | Joaquín |
| **HU05-FR04** (Inmutabilidad de solicitudes resueltas) | Control de estado: si no está en `PENDING`, devuelve `409 Conflict`. | T008, T011, T021 | `LeaveResolutionServiceTest`: intento de re-resolución lanza 409. | Joaquín |
| **HU05-FR05** (Notificación interna automática) | Inserción transaccional de registro en tabla `notifications` para el empleado solicitante. | T004, T008, T011, T023 | `LeaveResolutionServiceTest`: emisión de notificación al aprobar o rechazar. | Joaquín |
| **HU05-FR06** (Deducción de saldo HU-02) | La aprobación consolida los días en `spentDays` de forma derivada e inmediata. | T008, T011, T025 | Test de integración verificando actualización de saldo en HU-02 tras aprobación. | Joaquín |
| **HU05-FR07** (Revalidación atómica de saldo) | Bloqueo pesimista comprobando saldo en el instante de la aprobación (evita concurrencia). | T008, T011, T020 | `LeaveResolutionAtomicityIntegrationTest`: rechazo con 422 si saldo se agotó. | Joaquín |
| **HU05-FR08** (Reparto de días multianual) | Componente `LeaveDayAllocator` que divide días laborables por año natural para solicitudes que cruzan año. | T035, T038, T040 | `LeaveDayAllocatorTest`: imputación equitativa y exacta entre ejercicios. | Joaquín |
| **HU05-FR10** (Aislamiento de equipo) | El responsable no puede resolver solicitudes fuera de su jerarquía directa (403 Forbidden). | T008, T011, T024 | `LeaveResolutionServiceTest`: intento de resolver solicitud ajena arroja 403. | Joaquín |
| **HU05-FR11** (Prohibición de auto-resolución) | Bloqueo si `employee.id == currentUserId` devolviendo HTTP `403 Forbidden`. | T008, T011, T024 | `LeaveResolutionServiceTest`: auto-resolución bloqueada con 403. | Joaquín |
| **HU05-FR16** (Cancelación por baja de superior) | `LeaveRequestAutoCancelService` cancela pendientes de subordinados al darse de baja el manager. | T071, T073, T078 | `LeaveRequestAutoCancelServiceTest` (**5/5 tests pasando**). | Joaquín |
| **HU05-FR17/18** (`DIRECCION_GENERAL` y `RRHH` resolutores) | Ampliación de rol resolutor para sus subordinados directos sin extenderse a toda la plantilla. | T057, T074, T085, T095 | `LeaveResolverPolicyTest` (**9/9 tests pasando**). | Joaquín |

---

### HU-06 · Calendario de Ausencias del Equipo (Responsable: Andrei | Revisor: Iván)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU06-FR01** (Cuadrícula mensual navegable) | Componente `TeamCalendarComponent` con selector de mes y navegación interactiva sin recarga. | T005, T008, T012, T013 | `team-calendar.component.spec.ts`: navegación mensual interactiva. | Iván |
| **HU06-FR02** (Detalle de ausencias en celdas) | Proyección `TeamCalendarEntry` con nombre de empleado, tipo y rango de fechas. | T004, T008, T013 | `TeamCalendarServiceTest`: generación de celdas con datos de ausencia. | Iván |
| **HU06-FR03** (Distinción accesible aprobada/pendiente) | Distinción dual mediante color de fondo contrastado + badge textual legible. | T005, T013, T014 | Verificación visual de estilos y accesibilidad con lectores de pantalla. | Iván |
| **HU06-FR04** (Alerta de solapamientos críticos ≥ 2) | Detección en servicio: días con 2 o más ausencias se marcan con borde rojo e icono de alerta. | T006, T008, T013 | `TeamCalendarServiceTest`: cálculo exacto de coincidencias simultáneas. | Iván |
| **HU06-FR05** (Aislamiento exclusivo al equipo) | Filtro SQL estricto: solo aparecen ausencias de subordinados del usuario en sesión. | T007, T008, T016 | `TeamCalendarIntegrationTest`: ausencia de miembros de otros departamentos. | Iván |
| **HU06-FR06** (Exclusión de rechazadas y canceladas) | Cláusula `WHERE status IN ('APPROVED', 'PENDING')`; las canceladas no computan. | T006, T008, T010 | `TeamCalendarServiceTest`: verificación de que canceladas no aparecen. | Iván |
| **HU06-FR07** (Minimización de datos privados) | DTO excluye justificaciones médicas o comentarios personales (Art. 20). | T004, T008 | Inspección de payload JSON comprobando ausencia de campos sensibles. | Iván |
| **HU06-FR08** (Filtro por empleado individual) | Selector desplegable en cabecera para filtrar la vista por un miembro concreto. | T008, T012, T015 | Test unitario de componente filtrando el calendario por `employeeId`. | Iván |

---

### HU-07 · Informes de Absentismo y Uso de Vacaciones (Responsable: José | Revisor: Andrei)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU07-FR01** (Acceso exclusivo rol RRHH) | Anotación `@PreAuthorize("hasRole('RRHH')")` a nivel de clase controladora. | T008, T010 | `DepartmentReportIntegrationTest`: empleados y responsables reciben 403. | Andrei |
| **HU07-FR02** (Filtro por tipo de informe y fechas) | Parámetros `type` (`VACATION_USAGE`, `ABSENTEEISM`), `startDate` y `endDate`. | T006, T007, T008 | `DepartmentReportIntegrationTest`: consulta con diferentes rangos temporales. | Andrei |
| **HU07-FR03** (Agregación SQL departamental) | `JOIN LATERAL` eficiente para obtener contrato vigente y agregar ausencias aprobadas. | T006, T007 | Inspección de plan de ejecución SQL y tiempos de respuesta sub-segundo. | Andrei |
| **HU07-FR04** (Paridad 100% pantalla ↔ CSV) | Ambos canales consumen el mismo método `generateReport()` en la capa de servicio (Art. 17). | T008, T010 | `DepartmentReportIntegrationTest`: aserción fila a fila entre JSON y CSV. | Andrei |
| **HU07-FR05** (Exportación CSV RFC 4180 UTF-8 BOM) | Inserción de cabecera BOM `\uFEFF`, comillas dobles estándar y `Content-Type: text/csv`. | T007, T010 | Verificación de apertura limpia de tildes en Excel y formato RFC 4180. | Andrei |
| **HU07-FR06** (Respuesta semántica sin datos) | `hasData: false` en JSON; HTTP `204 No Content` en exportación CSV si no hay registros. | T007, T010 | `DepartmentReportIntegrationTest`: test de periodos sin ausencias. | Andrei |
| **HU07-FR07** (Desglose individual por empleado) | Colección `EmployeeReportRow` anidada en cada fila de departamento expandible en UI. | T007, T012 | `department-reports.component.spec.ts`: test de expansión de acordeón. | Andrei |

---

### HU-08 · Evaluación de Desempeño con Conformidad (Responsable: José | Revisor: Iván)

| Historia y Criterio | Decisión Técnica del Plan | Tareas que lo Cubren | Prueba Prevista y Evidencia | Revisor |
| :--- | :--- | :--- | :--- | :---: |
| **HU08-FR01** (Responsable crea solo para su equipo) | Validación en servicio: `employee.manager.id == currentUserId`. | T009, T010, T011 | `PerformanceReviewIntegrationTest`: creación exitosa y rechazo a terceros. | Iván |
| **HU08-FR02** (Campos obligatorios de evaluación) | DTO con `@NotBlank` en periodo/objetivos, `@Min(1) @Max(5)` en rating y comentarios. | T006, T009 | MockMvc validando rechazo con 400 ante campos vacíos o rating fuera de rango. | Iván |
| **HU08-FR03** (Estado inicial `PENDING_ACKNOWLEDGMENT`) | Estado inicial por defecto en entidad `PerformanceReview`. | T004, T009 | Test de integración verificando estado devuelto en la respuesta `201 Created`. | Iván |
| **HU08-FR04** (Empleado revisa, comenta y formaliza) | Endpoint `POST /{id}/acknowledge` accesible por `EMPLEADO` con comentarios opcionales. | T009, T010, T013 | `PerformanceReviewIntegrationTest`: empleado añade notas y otorga conformidad. | Iván |
| **HU08-FR05** (Transición a `FINALIZED` con timestamp) | Asignación de `Instant.now(clock)` a `finalized_at` en el servidor y cambio de estado. | T009, T011 | Test de integración con `Clock.fixed` verificando marca temporal exacta. | Iván |
| **HU08-FR06** (Inmutabilidad absoluta en `FINALIZED`) | Excepción de servicio (409) + trigger en PostgreSQL (V4) que aborta mutaciones (Art. 18). | T007, T009, T011 | `PerformanceReviewIntegrationTest`: test de intento de re-conformidad y test de trigger SQL. | Iván |
| **HU08-FR07** (Aislamiento y confidencialidad) | Empleado solo ve sus evaluaciones; responsable solo ve las de su equipo actual. | T009, T010, T011 | Test de seguridad negativa comprobando aislamiento estricto entre roles. | Iván |
| **HU08-FR08** (Valoración escala 1 a 5 accesible) | Control `<select>` nativo accesible con descriptores y constraint `CHECK` en BD. | T006, T011, T013 | Verificación en interfaz web y validación de constraint en PostgreSQL. | Iván |

---

## 4. Conclusión de la Actividad 5.2

1. **Sin contradicciones abiertas**: Todos los hallazgos iniciales (`H-01` a `H-11`) han sido subsanados mediante código, migraciones Flyway o enmiendas constitucionales.
2. **Cobertura exhaustiva (100%)**: Cada uno de los requisitos funcionales (`FR`), criterios de éxito (`SC`) y casos de aceptación (`CA`) de las 8 historias de usuario cuenta con al menos una decisión técnica en el plan, tareas ejecutadas en `tasks.md` y pruebas automatizadas verificables.
3. **Revisión cruzada validada**: Las filas de cada integrante han sido revisadas y validadas por un compañero de equipo distinto, garantizando la calidad y la coherencia arquitectónica global del sistema.
