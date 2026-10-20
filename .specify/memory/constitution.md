

Constitution · MD
# Constitución del Proyecto — SI-09 · Gestión de Recursos Humanos y Vacaciones
 
**Versión:** 1.0.0 · **Ratificada:** 2026-10-20 · **Última enmienda:** 2026-10-20
 
**Pila tecnológica:** Angular + Angular Material · Spring Boot (arquitectura por capas) · PostgreSQL
 
---
 
## Preámbulo
 
Este documento recoge los principios **no negociables** del proyecto. Toda especificación (`spec.md`), plan técnico (`plan.md`), lista de tareas (`tasks.md`) e implementación DEBE ser coherente con él. Si una especificación o un plan entra en conflicto con esta constitución, prevalece la constitución, salvo que se apruebe una enmienda según el Título V.
 
**Flujo de trabajo SDD:** `Constitución → Especificación (qué y por qué) → Plan (cómo) → Tareas → Implementación → Verificación contra la especificación`.
 
**Convención de lenguaje normativo:** DEBE / NO DEBE = obligatorio · DEBERÍA / NO DEBERÍA = recomendado salvo justificación escrita · PUEDE = opcional.
 
**Lenguaje del proyecto:**
- Documentación, historias de usuario y textos de interfaz: **español**.
- Identificadores de código (clases, métodos, tablas, columnas, endpoints): **inglés**, siguiendo el glosario del Anexo A.
---
 
## Título I — Principios de desarrollo guiado por especificaciones
 
### Art. 1 · La especificación es la fuente de verdad
1. No se escribe código de producción sin una historia de usuario (HU) con criterios de aceptación (CA) verificables.
2. Si el código y la especificación divergen, se corrige uno de los dos de forma explícita; nunca se deja la divergencia sin resolver.
3. Los cambios de comportamiento empiezan por actualizar la especificación, no por el código.
### Art. 2 · Trazabilidad completa HU → CA → Test
1. Cada CA de cada HU DEBE tener al menos un test automatizado que lo verifique.
2. Los tests DEBEN nombrarse o etiquetarse con su referencia (por ejemplo, `HU-03.CA-3`), de modo que se pueda comprobar la cobertura de CA.
3. Los commits y las Pull Requests DEBEN referenciar la HU que implementan.
### Art. 3 · La ambigüedad se declara, no se improvisa
1. Cuando la especificación no define un comportamiento, se marca como `[NEEDS CLARIFICATION]` y se resuelve **antes** del plan.
2. Quien implementa NO DEBE inventar reglas de negocio para cubrir un hueco. Las preguntas abiertas conocidas se recogen en el Anexo B.
### Art. 4 · Entrega en cortes verticales
1. La unidad de entrega es una HU completa o un corte de ella que atraviese todas las capas (UI → API → BD) y sea demostrable.
2. NO DEBE entregarse una capa «preparada» sin que ninguna HU la consuma.
---
 
## Título II — Arquitectura
 
### Art. 5 · Cuatro capas desacopladas
El sistema se organiza en cuatro capas con dependencias en un solo sentido:
 
```
┌─────────────────────────────────────────────┐
│ 1. PRESENTACIÓN  — SPA Angular + Material   │
└───────────────────────┬─────────────────────┘
                        │ HTTP/JSON (solo vía contrato)
┌───────────────────────▼─────────────────────┐
│ 2. CONTRATO API  — REST + OpenAPI + DTOs    │
└───────────────────────┬─────────────────────┘
                        │
┌───────────────────────▼─────────────────────┐
│ 3. APLICACIÓN/DOMINIO — Spring Boot         │
│    Controlador → Servicio → Repositorio     │
└───────────────────────┬─────────────────────┘
                        │ JPA/JDBC
┌───────────────────────▼─────────────────────┐
│ 4. PERSISTENCIA  — PostgreSQL               │
└─────────────────────────────────────────────┘
```
 
1. Cada capa SOLO conoce a la inmediatamente inferior a través de su interfaz pública.
2. El frontend NO DEBE acceder jamás a la base de datos ni conocer su esquema; solo conoce el contrato de la API.
3. El backend NO DEBE depender de nada del frontend (ni de su estructura ni de sus modelos).
4. Frontend y backend se construyen, prueban y despliegan de forma independiente.
### Art. 6 · Frontend (Angular + Angular Material)
1. SPA con Angular en **TypeScript estricto** (`strict: true`); prohibido `any` salvo justificación puntual comentada.
2. La UI DEBE construirse con **componentes de Angular Material**; los estilos propios se limitan a tematización y maquetación, sin reimplementar componentes que Material ya ofrece.
3. Componentes **standalone**, organizados por funcionalidad (`employees`, `leave`, `team-calendar`, `reports`, `evaluations`, `core`, `shared`) con *lazy loading* de rutas.
4. Separación de responsabilidades:
   - *Componentes de presentación*: sin lógica de negocio ni llamadas HTTP.
   - *Servicios*: acceso a la API y estado de la funcionalidad.
5. Formularios **reactivos** (`ReactiveFormsModule`) con validación en cliente y **errores mostrados junto al campo** (`mat-error`).
6. Los *guards* de ruta y la ocultación de opciones según rol son **solo ayuda de usabilidad**; NUNCA sustituyen a la autorización del servidor (Art. 10).
7. Los DTOs/clientes TypeScript DEBERÍAN generarse desde el OpenAPI del backend para evitar divergencias de contrato.
8. Un interceptor HTTP centraliza la autenticación y el tratamiento de errores de la API.
### Art. 7 · Backend (Spring Boot, arquitectura por capas)
1. **Java LTS vigente y Spring Boot estable**; versiones concretas fijadas en `plan.md`.
2. Organización por **funcionalidad** (`employee`, `leave`, `evaluation`, `report`, `notification`, `security`) y, dentro de cada una, por capa: `controller`, `service`, `repository`, `domain`, `dto`.
3. Responsabilidades **estrictas**:
   | Capa | Hace | NO hace |
   |---|---|---|
   | **Controlador** | Recibir HTTP, validar formato (`@Valid`), delegar, mapear a DTO y código HTTP | Lógica de negocio, acceso a datos, transacciones |
   | **Servicio** | Reglas de negocio, autorización de alcance, transacciones (`@Transactional`), orquestación | Conocer HTTP (`HttpServletRequest`, `ResponseEntity`) |
   | **Repositorio** | Acceso a datos (Spring Data JPA / consultas explícitas) | Reglas de negocio |
4. Las **entidades JPA NO DEBEN exponerse** en la API: se usan DTOs (records) con mapeo explícito (p. ej. MapStruct).
5. Inyección de dependencias **por constructor**; prohibida la inyección por campo.
6. Las transacciones se declaran en la capa de **servicio**, nunca en controladores ni repositorios.
7. Los errores de negocio se modelan con excepciones de dominio tipadas y se traducen en un único `@RestControllerAdvice` (Art. 19).
8. Configuración externalizada por perfiles (`dev`, `test`, `prod`); NINGÚN secreto en el repositorio.
### Art. 8 · Persistencia (PostgreSQL)
1. PostgreSQL es la **única fuente de verdad**. El esquema se gestiona exclusivamente con **migraciones versionadas** (Flyway o Liquibase); prohibido `ddl-auto=update` fuera de pruebas locales descartables.
2. La integridad se garantiza **también en base de datos**, no solo en la aplicación: `PRIMARY KEY`, `FOREIGN KEY`, `NOT NULL`, `UNIQUE` y `CHECK` (p. ej. `end_date >= start_date`, estados válidos).
3. Tipos: `DATE` para fechas de negocio (sin hora), `TIMESTAMPTZ` para instantes de auditoría, `NUMERIC(precision, scale)` para importes salariales (NUNCA `float`/`double`), identificadores `UUID` o `BIGINT`.
4. Toda tabla de negocio DEBE incluir columnas de auditoría: `created_at`, `created_by` y, si es mutable, `updated_at`, `updated_by` y `version` (bloqueo optimista).
5. Los índices se justifican por consultas reales (calendario por equipo y rango de fechas, solicitudes por empleado y estado, informes por departamento y periodo).
6. Las consultas de informes DEBERÍAN resolverse con SQL agregado en base de datos, no cargando entidades en memoria.
7. Los datos de pruebas se crean mediante *fixtures* o *builders*; NUNCA se usan datos reales de empleados fuera de producción.
### Art. 9 · Contrato de la API REST
1. API REST con JSON, recursos en plural y verbos HTTP semánticos; versionada en la ruta (`/api/v1/...`).
2. El contrato se describe en **OpenAPI** y se mantiene sincronizado con la implementación; un cambio incompatible requiere nueva versión.
3. Códigos HTTP coherentes: `200/201/204` éxito · `400` validación · `401` sin autenticar · `403` sin permiso · `404` no existe · `409` conflicto de estado o concurrencia · `422` regla de negocio violada.
4. Los listados DEBEN ser **paginados** y filtrables en servidor (p. ej. filtro por estado en HU-04).
---
 
## Título III — Reglas de dominio inviolables
 
Estas reglas derivan directamente de las historias de usuario. Se consideran **invariantes del sistema**: ninguna tarea, atajo o urgencia justifica romperlas.
 
### Art. 10 · Autorización siempre en el servidor
1. Roles del sistema: **EMPLEADO**, **RESPONSABLE_EQUIPO** y **RRHH**.
2. Todo endpoint, salvo los explícitamente públicos (login y *health check*), exige autenticación.
3. La autorización tiene **dos niveles, ambos obligatorios**:
   - *Por rol* (qué funciones puede usar): Spring Security con `@PreAuthorize`/reglas de ruta.
   - *Por alcance* (a qué datos puede acceder): verificado en la capa de **servicio** con la identidad del contexto de seguridad.
4. Alcance por rol:
   | Rol | Alcance de datos |
   |---|---|
   | EMPLEADO | Únicamente **su propia** ficha, saldo, solicitudes y evaluaciones (HU-02, 04, 08) |
   | RESPONSABLE_EQUIPO | Únicamente las personas **de su equipo**: solicitudes, calendario y evaluaciones (HU-05, 06, 08) |
   | RRHH | Alta y datos contractuales (HU-01) e informes (HU-07) |
5. La identidad del solicitante **se obtiene del token/sesión**, NUNCA de un parámetro enviado por el cliente (se prohíbe confiar en un `employeeId` del cuerpo o de la URL para decidir alcance).
6. Un acceso fuera de alcance DEBE devolver `403` (o `404` si se quiere no revelar existencia), y DEBE quedar registrado.
7. Cada regla de «solo ve lo suyo / solo su equipo» DEBE tener un **test negativo** que intente violarla.
### Art. 11 · Trayectoria contractual inmutable (HU-01)
1. Los cambios de puesto, departamento o condiciones salariales se guardan como **registros históricos con fecha de entrada en vigor**; **nunca** se sobrescribe un registro anterior.
2. Modificar uno de estos datos SIN fecha de efectos DEBE ser rechazado.
3. Los registros históricos son **solo-inserción** (*append-only*): no se actualizan ni se borran desde la aplicación. DEBERÍA reforzarse a nivel de base de datos (permisos o *triggers*).
4. El estado «vigente» a una fecha se **deriva** del historial; no se mantiene como dato duplicado editable.
5. El historial se presenta en orden cronológico con valor anterior, valor nuevo y fecha de efectos.
6. Los campos obligatorios del alta se validan en cliente **y** servidor; el error se asocia al campo concreto (Art. 19).
### Art. 12 · El saldo de vacaciones es un dato derivado (HU-02, 03, 05)
1. El saldo se **calcula** a partir de la fuente de verdad: *días que corresponden por convenio* − *días disfrutados / aprobados del año*. NO existe un contador editable a mano.
2. El saldo se recalcula **automáticamente** cuando cambia el convenio o se registran días disfrutados; no requiere intervención manual.
3. Si se materializa o cachea el saldo por rendimiento, DEBE actualizarse en la **misma transacción** que el hecho que lo modifica, y existir un procedimiento verificable para reconstruirlo desde la fuente.
4. El desglose mostrado DEBE ser siempre: **correspondientes · disfrutados · disponibles**.
5. Una solicitud que supere el saldo disponible NO se crea y se informa del saldo (HU-03).
6. Las operaciones que consultan y modifican el saldo DEBEN ser **seguras ante concurrencia** (bloqueo optimista con `version` o bloqueo pesimista puntual), de modo que dos operaciones simultáneas no puedan dejar el saldo en negativo.
### Art. 13 · Ciclo de vida de la solicitud (HU-03, 04, 05)
1. Estados permitidos: `PENDIENTE`, `APROBADA`, `RECHAZADA`. Transiciones válidas **únicas**:
   - `PENDIENTE → APROBADA`
   - `PENDIENTE → RECHAZADA`
2. Una solicitud ya resuelta **NO puede volver a resolverse**: el servicio lo impide y devuelve `409`, y se refuerza con restricción en BD.
3. Toda solicitud válida nace en `PENDIENTE`.
4. Si la fecha de fin es anterior a la de inicio, se rechaza con error de validación (cliente, servidor y `CHECK` en BD).
5. El **motivo** es opcional al aprobar y se registra al rechazar según HU-05; el empleado DEBE poder verlo en HU-04.
6. La resolución registra quién y cuándo la realizó.
7. La transición de estado, el cambio en el saldo (si aprueba) y la creación de la notificación se realizan en **una única transacción**: o ocurre todo, o nada.
8. Solo el responsable del equipo del solicitante PUEDE resolver (Art. 10).
### Art. 14 · Fechas y cómputo de días
1. Las fechas de negocio se representan con `LocalDate` (Java) y `DATE` (PostgreSQL); los instantes con `Instant`/`TIMESTAMPTZ`.
2. La zona horaria de negocio es **Europe/Madrid**; el servidor opera en UTC y convierte en los bordes.
3. La **regla de cómputo de días** (naturales o laborables, tratamiento de festivos y fines de semana, medios días) DEBE estar definida en la especificación (Anexo B), implementada **en un único componente de dominio** y reutilizada por la previsualización del número de días (HU-03), la validación de saldo, el saldo (HU-02) y los informes (HU-07).
4. El cliente NO DEBE reimplementar la regla de cómputo: la previsualización de días en el formulario se obtiene de la API.
### Art. 15 · Notificaciones (HU-05)
1. Al registrar una decisión, se genera automáticamente una notificación al empleado con el resultado y el motivo si existe.
2. La notificación se **persiste** (no depende solo de un canal externo) para que el empleado pueda consultarla.
3. El fallo de un canal secundario (p. ej. correo electrónico, si se añadiera) NO DEBE revertir la decisión ya registrada.
4. El canal concreto se decide en la especificación (Anexo B).
### Art. 16 · Calendario del equipo (HU-06)
1. El calendario solo incluye ausencias de las personas del equipo del responsable.
2. Las ausencias **aprobadas** y **pendientes** DEBEN distinguirse visualmente, sin depender únicamente del color (Art. 21).
3. La detección de días con **dos o más ausencias coincidentes** se calcula en el **servidor**, de forma que el frontend solo la represente.
4. La consulta es por rango de fechas (mes visible), nunca carga todo el histórico.
### Art. 17 · Informes y exportación (HU-07)
1. Los informes (absentismo y uso de vacaciones) se agrupan por **departamento y periodo**.
2. Pantalla y exportación DEBEN obtenerse de **la misma consulta/servicio**; está prohibido tener dos implementaciones que puedan divergir.
3. El formato de exportación es tabular y analizable externamente (CSV con codificación UTF-8 y separador documentado; otros formatos, si se añaden, siguen la misma fuente).
4. Un periodo sin datos muestra un mensaje explícito, no un informe vacío.
5. Los informes agregan datos de personas: DEBEN respetar el Art. 20 (minimización) y limitarse al rol RRHH.
6. Se DEBE incluir un test que compruebe que los datos exportados coinciden con los mostrados.
### Art. 18 · Evaluaciones de desempeño (HU-08)
1. Estados: `PENDIENTE_CONFORMIDAD` → `FORMALIZADA`. La evaluación se asocia siempre a un **periodo**.
2. Pasa a `FORMALIZADA` únicamente cuando se cumplen las conformidades requeridas (según HU-08) y se registra la **fecha de formalización**.
3. Una evaluación formalizada es **inmutable**; cualquier corrección posterior se tramita como nueva evaluación o mecanismo definido en la especificación.
4. Visibilidad: el empleado solo ve las suyas; el responsable solo las de su equipo (Art. 10).
5. Las evaluaciones contienen datos especialmente sensibles: se aplica el Art. 20 con rigor.
---
 
## Título IV — Calidad transversal
 
### Art. 19 · Validación y manejo de errores
1. **Validación en profundidad**: cliente (usabilidad) + servidor (`jakarta.validation` y reglas de servicio) + base de datos (restricciones). El servidor NUNCA confía en el cliente.
2. Todos los errores de la API usan un formato único: **Problem Details (RFC 9457)**, con `type`, `title`, `status`, `detail` y, para validación, la lista de errores por **campo**.
3. Los mensajes de error asociados a campos DEBEN poder pintarse junto al campo correspondiente en la UI.
4. NUNCA se exponen trazas de pila, SQL ni detalles internos al cliente.
### Art. 20 · Seguridad y protección de datos personales
1. Este sistema trata datos personales y económicos de empleados: se rige por el **RGPD / LOPDGDD**. Se aplican *privacidad desde el diseño* y **minimización de datos**.
2. Todo tráfico por **HTTPS**; las contraseñas, si existieran, se almacenan con hash adaptativo (p. ej. BCrypt/Argon2); mecanismo de autenticación (sesión, JWT u OIDC) fijado en `plan.md`.
3. Se cubren como mínimo las amenazas del **OWASP Top 10**: control de acceso, inyección (consultas parametrizadas siempre), XSS (sin `innerHTML` sin sanear), CSRF según mecanismo, configuración CORS restrictiva.
4. Los **datos personales y salariales NO DEBEN aparecer en logs**, mensajes de error ni URLs.
5. Las operaciones sensibles (alta, cambio contractual, resolución de solicitud, formalización, exportación de informes, accesos denegados) generan **registro de auditoría**: quién, qué, cuándo.
6. Los secretos se gestionan fuera del código; las dependencias se escanean periódicamente por vulnerabilidades.
### Art. 21 · Accesibilidad, usabilidad e internacionalización
1. La UI DEBE cumplir **WCAG 2.1 nivel AA** como mínimo: navegación por teclado, etiquetas en formularios, contraste suficiente y no transmitir información solo por color (calendario, estados).
2. Diseño **responsive** (escritorio y móvil).
3. Todos los textos de interfaz se externalizan (i18n de Angular o equivalente); idioma por defecto **español**; fechas y números con formato local.
4. Toda acción asíncrona muestra estado de carga y de error comprensible.
### Art. 22 · Estrategia de pruebas (test-first)
1. Desarrollo **orientado a pruebas**: se escribe el test que expresa un CA antes (o junto con) el código que lo satisface.
2. Pirámide de pruebas:
   | Nivel | Alcance | Herramientas orientativas |
   |---|---|---|
   | Unitarias | Reglas de dominio y servicios (saldo, estados, cómputo de días) | JUnit 5, Mockito · Jasmine/Karma o Jest |
   | Integración | Repositorios y API contra **PostgreSQL real** | Spring Boot Test, **Testcontainers** |
   | Contrato | Que la API cumple el OpenAPI | Validación del contrato en CI |
   | E2E | Flujos completos por HU | Playwright o Cypress |
3. Las pruebas de persistencia **NO DEBEN usar bases de datos en memoria distintas de PostgreSQL** (H2, etc.): el comportamiento real de PostgreSQL es parte del requisito.
4. Son **obligatorios** los tests de: autorización negativa (Art. 10), transición de estados inválida (Art. 13), saldo insuficiente (Art. 12), concurrencia sobre el saldo, inmutabilidad del historial (Art. 11) y coincidencia pantalla-exportación (Art. 17).
5. Los tests DEBEN ser deterministas e independientes; el reloj se inyecta (`Clock`) para no depender de la fecha real.
### Art. 23 · Simplicidad y proporcionalidad
1. Se elige la solución **más simple que satisfaga la especificación**. Es un monolito backend modular, no microservicios.
2. NO se introducen sin justificación escrita en `plan.md`: CQRS, *event sourcing*, colas de mensajes, caches distribuidas, bases de datos adicionales ni abstracciones «por si acaso».
3. Se usan los frameworks de la pila **directamente** (Spring, Angular, Material), sin envolverlos en capas propias innecesarias.
4. Toda dependencia nueva requiere justificar su necesidad, mantenimiento y licencia.
5. Cualquier desviación de este artículo se documenta como **complejidad justificada** (Art. 26).
### Art. 24 · Calidad de código y Definition of Done
1. Análisis estático y formato automático obligatorios en CI (p. ej. Checkstyle/Spotless y ESLint/Prettier); la compilación NO DEBE emitir *warnings* ignorados.
2. Revisión por pares obligatoria en cada Pull Request; ramas cortas, integración frecuente (*trunk-based* o *GitHub Flow*), *commits* convencionales.
3. **Una HU o tarea está *Hecha* cuando:**
   - [ ] Todos sus CA están cubiertos por tests automatizados y pasan en CI.
   - [ ] Se cumplen los invariantes de los Arts. 10–18 aplicables, incluidos los tests negativos.
   - [ ] La migración de BD es versionada y reversible o con plan de reversión.
   - [ ] El contrato OpenAPI está actualizado.
   - [ ] La UI cumple accesibilidad e i18n (Art. 21).
   - [ ] No hay datos personales en logs ni secretos en el repositorio.
   - [ ] La especificación refleja el comportamiento final.
   - [ ] Revisión aprobada.
### Art. 25 · Observabilidad y operación
1. Logging estructurado con identificador de correlación por petición.
2. *Health checks* y métricas básicas expuestos mediante Spring Boot Actuator, protegidos adecuadamente.
3. El despliegue es reproducible (contenedores) y los entornos se diferencian solo por configuración.
---
 
## Título V — Gobernanza
 
### Art. 26 · Complejidad justificada
Toda desviación de un principio DEBE registrarse en `plan.md` con: *qué se incumple*, *por qué es necesario*, *qué alternativa más simple se descartó y por qué*. Sin esta justificación, la desviación es un defecto.
 
### Art. 27 · Enmiendas
1. Esta constitución se modifica mediante Pull Request que explique el motivo y el impacto.
2. Se versiona con **SemVer**: *MAJOR* (elimina o redefine un principio) · *MINOR* (añade un artículo o amplía uno) · *PATCH* (aclaraciones sin cambio de significado).
3. Una enmienda MAJOR o MINOR requiere revisar `spec.md`, `plan.md` y `tasks.md` afectados y registrar el resultado.
4. Las reglas de dominio del Título III solo cambian si cambian primero las HU correspondientes (Art. 1).
### Art. 28 · Cumplimiento
Las revisiones de código y los *quality gates* de CI comprueban el cumplimiento de esta constitución. Un incumplimiento no justificado bloquea la integración.
 
---
 
## Anexo A — Glosario (lenguaje ubicuo)
 
| Término de negocio | Identificador en código | Notas |
|---|---|---|
| Empleado | `Employee` | Incluye datos personales y contractuales |
| Trayectoria / historial contractual | `ContractRecord` / `ContractHistory` | Append-only, con `effectiveFrom` |
| Convenio | `CollectiveAgreement` | Determina los días de vacaciones que corresponden |
| Responsable de equipo | `TeamManager` | Rol `RESPONSABLE_EQUIPO` |
| Departamento | `Department` | Eje de agrupación de informes |
| Saldo de vacaciones | `LeaveBalance` | Derivado (Art. 12) |
| Solicitud (vacaciones / permiso) | `LeaveRequest` (`LeaveType`: `VACATION`, `PERMIT`) | Estados `PENDING`, `APPROVED`, `REJECTED` |
| Notificación | `Notification` | Persistida (Art. 15) |
| Evaluación de desempeño | `PerformanceReview` | Estados `PENDING_ACKNOWLEDGMENT`, `FINALIZED` |
| Conformidad | `Acknowledgment` | Registro de aceptación con fecha |
 
## Anexo B — Preguntas abiertas a resolver en la especificación (`[NEEDS CLARIFICATION]`)
 
Los textos de las HU no definen los siguientes puntos. Deben resolverse **antes** del plan técnico:
 
1. **Cómputo de días (HU-03):** ¿días naturales o laborables? ¿Se descuentan fines de semana y festivos (nacionales, autonómicos, locales)? ¿Se admiten medios días?
2. **Permisos vs. vacaciones (HU-03):** ¿los permisos consumen saldo de vacaciones o tienen reglas/cupos propios? ¿Existen tipos de permiso (retribuido, médico…)?
3. **Solapamiento (HU-03):** ¿se permite que un mismo empleado tenga solicitudes solapadas en fechas?
4. **Revalidación del saldo (HU-05):** las solicitudes pendientes no descuentan saldo; ¿se revalida el saldo en el momento de aprobar?
5. **Rechazo sin motivo (HU-05):** ¿el motivo es obligatorio al rechazar o opcional?
6. **Cancelación (HU-03/04):** ¿puede el empleado cancelar una solicitud pendiente o aprobada? (hoy el modelo solo tiene tres estados).
7. **Canal de notificación (HU-05):** ¿solo dentro de la aplicación o también correo electrónico?
8. **Año de vacaciones (HU-02):** ¿año natural o ciclo propio? ¿Arrastre de días no disfrutados al año siguiente?
9. **Conformidad (HU-08):** ¿qué ocurre si el empleado no está de acuerdo? ¿Existe un estado «No conforme» o se reabre la evaluación? ¿Cuál es exactamente la «conformidad de ambas partes» (el responsable la da al crear)?
10. **Cambios retroactivos (HU-01):** ¿se admiten fechas de efectos pasadas? ¿Cómo se gestiona un cambio intercalado entre dos registros existentes?
11. **Jerarquía y casos límite:** ¿quién aprueba las solicitudes de un responsable de equipo? ¿Puede una persona tener varios roles? ¿Qué pasa con las solicitudes pendientes cuando cambia el responsable de un empleado?
12. **Autenticación (transversal):** mecanismo (SSO corporativo/OIDC, usuario y contraseña propios) y gestión del ciclo de vida de los usuarios.
13. **Informes (HU-07):** definición exacta de «absentismo» y de «uso de vacaciones» (métricas, porcentajes, unidades) y formato de exportación requerido (CSV, XLSX).
