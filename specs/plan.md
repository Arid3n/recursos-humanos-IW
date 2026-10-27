# Implementation Plan: Sistema de Gestión de Recursos Humanos y Vacaciones (Base Común)

**Fecha**: 2026-10-27  
**Estado**: Acordado por el equipo (4 integrantes)  
**Alcance**: Arquitectura base y Modelo de Datos compartido entre las 8 especificaciones (`spec-001` a `spec-008`).

---

## 1. Resumen Ejecutivo y Enfoque de Colaboración

El proyecto implementa un sistema web integral para la gestión de Recursos Humanos, ausencias, evaluaciones y calendario de equipos. El desarrollo se organiza en un equipo de **4 personas** que asumen **2 historias de usuario / especificaciones cada una**, compartiendo una **base técnica y un modelo de datos común** integrado en la rama `main`:

* **Integrante 1**: `spec-001` (Alta y contratos) + `spec-002` (Ficha y saldo).
* **Integrante 2**: `spec-003` (Solicitud de ausencia) + `spec-004` (Seguimiento de solicitudes).
* **Integrante 3**: `spec-005` (Aprobación/Rechazo de solicitudes) + `spec-006` (Calendario de ausencias del equipo).
* **Integrante 4**: `spec-007` (Informes y exportación) + `spec-008` (Evaluación de desempeño).

Para evitar colisiones y divergencias en base de datos o contratos de API, la base compartida define el esquema relacional global (migración Flyway), la infraestructura de seguridad/roles y las convenciones de arquitectura antes de bifurcar el trabajo en las ramas `spec-001` a `spec-008`.

---

## 2. Pila Tecnológica del Sistema

| Capa | Tecnología / Herramienta | Versión | Justificación técnica |
| :--- | :--- | :--- | :--- |
| **Backend Framework** | Java LTS + Spring Boot | Java 21 · Spring Boot 3.4.3 | Monolito modular por capas, robusto, tipado estricto y soporte nativo para records y Jakarta Validation. |
| **Persistencia** | PostgreSQL + Spring Data JPA | PostgreSQL 16 | Única fuente de verdad. Integridad reforzada por BD (`CHECK`, `FK`, `UNIQUE`). |
| **Migraciones BD** | Flyway | Integrado en Spring Boot | Control de versiones reproducible de esquema; prohibido `ddl-auto=update`. |
| **Seguridad y Roles** | Spring Security | 6.x | Control de acceso por rol (`EMPLEADO`, `RESPONSABLE_EQUIPO`, `RRHH`) y alcance verificado en servicio. |
| **Contrato de API** | SpringDoc OpenAPI + Swagger UI | OpenAPI 3.0 (v2.8.5) | Contrato vivo sincronizado; errores estandarizados según RFC 9457 (Problem Details). |
| **Frontend Framework** | Angular | 21.2 | SPA con TypeScript en modo estricto (`strict: true`), componentes *standalone* y formularios reactivos. |
| **UI Components** | Angular Material + CDK | 22.2 | Componentes estándar accesibles WCAG 2.1 AA, theming centralizado sin reimplementar componentes básicos. |
| **Testing Backend** | JUnit 5 + Mockito + Testcontainers | PostgreSQL Testcontainers | Test-first; pruebas de integración contra PostgreSQL real, sin bases en memoria (H2 prohibido). |
| **Testing Frontend** | Vitest + Testing Library | Vitest 4 | Pruebas unitarias de componentes y servicios rápidas y deterministas. |
| **Observabilidad** | Spring Boot Actuator | 3.4.3 | Health checks y métricas de rendimiento protegidos. |

---

## 3. Constitution Check (Verificación de Invariantes)

Todas las especificaciones y componentes se someten a los controles de la [Constitución](file:///c:/Users/jotan/Desktop/IW/recursos-humanos-IW/.specify/memory/constitution.md):

| Artículo | Invariante Constitucional | Cumplimiento en el Plan Compartido |
| :--- | :--- | :--- |
| **Art. 5** | 4 capas desacopladas | Frontend solo conoce HTTP/JSON mediante contratos OpenAPI. Backend estructurado en `controller`, `service`, `repository`, `domain`, `dto`. |
| **Art. 8** | PostgreSQL como fuente de verdad | Migraciones Flyway versionadas (`V1__...`). Columnas de auditoría (`created_at`, `created_by`, `version`). |
| **Art. 10** | Autorización en 2 niveles (Rol y Alcance) | `@PreAuthorize` en endpoints + verificación de pertenencia a equipo/solicitante en la capa de servicio obtenida de la sesión (no de URLs). |
| **Art. 11** | Trayectoria contractual inmutable | Tabla `contract_records` con esquema *append-only* y `effective_from`. No se sobrescriben registros anteriores. |
| **Art. 12** | Saldo de vacaciones derivado | El saldo es un dato calculado en tiempo de ejecución (`días convenio - días aprobados`), sin contadores editables a mano. |
| **Art. 13** | Ciclo de vida y atomicidad | Transición única (`PENDIENTE → APROBADA/RECHAZADA`). Resolución en `@Transactional` único con actualización de saldo y notificación. |
| **Art. 16** | Calendario accesible y solapes | Detección de solapes ($\ge 2$ ausencias) calculada en servidor. Visualización con diferenciación por iconos y texto además de color. |
| **Art. 17** | Paridad pantalla y exportación de informes | Misma capa de consulta agregada SQL para la vista y el fichero CSV exportado (UTF-8). |
| **Art. 18** | Evaluaciones inmutables y bilaterales | Estados `PENDING_ACKNOWLEDGMENT → FINALIZED` con fecha de formalización inmutable. |
| **Art. 19** | RFC 9457 Problem Details | Gestión centralizada de errores con `@RestControllerAdvice` y validación con `jakarta.validation`. |

---

## 4. Estructura del Proyecto

```text
recursos-humanos-IW/
├── .specify/                      # Memoria del proyecto, templates y constitución
│   ├── memory/constitution.md
│   └── templates/
├── specs/                         # Especificaciones acordadas
│   ├── plan.md                    # Plan técnico global compartido (este archivo)
│   ├── data-model.md              # Modelo de datos unificado
│   ├── spec-001/spec.md           # HU-01 (Integrante 1)
│   ├── spec-002/spec.md           # HU-02 (Integrante 1)
│   ├── spec-003/spec.md           # HU-03 (Integrante 2)
│   ├── spec-004/spec.md           # HU-04 (Integrante 2)
│   ├── spec-005/spec.md           # HU-05 (Integrante 3)
│   ├── spec-006/spec.md           # HU-06 (Integrante 3)
│   ├── spec-007/spec.md           # HU-07 (Integrante 4)
│   └── spec-008/spec.md           # HU-08 (Integrante 4)
├── backend/                       # Spring Boot 3.4.3 (Java 21)
│   └── src/main/java/com/iw/rrhh/
│       ├── common/                # Excepciones, RFC 9457 Handler, Utilidades de fecha
│       ├── employee/              # HU-01 y HU-02 (controller, service, repository, domain, dto)
│       ├── leave/                 # HU-03, HU-04, HU-05, HU-06
│       ├── notification/          # Notificaciones internas persistidas (HU-05)
│       ├── report/                # HU-07 (Informes agregados y CSV)
│       ├── evaluation/            # HU-08 (Evaluaciones y conformidad)
│       └── security/              # Configuración Spring Security, JWT/Session y roles
│   └── src/main/resources/
│       └── db/migration/          # Migraciones Flyway comunes (V1__init_shared_schema.sql)
└── frontend/                      # SPA Angular 21 + Material
    └── src/app/
        ├── core/                  # Interceptores, guards, servicios base de auth
        ├── shared/                # Componentes comunes, dialogs, pipes
        └── features/              # Módulos lazy-loaded alineados con las specs
            ├── employees/         # HU-01 y HU-02
            ├── leave/             # HU-03 y HU-04
            ├── team-calendar/     # HU-05 y HU-06
            ├── reports/           # HU-07
            └── evaluations/       # HU-08
```

---

## 5. Acuerdos de Integración y Trabajo en Grupo (GitHub Flow)

1. **Rama `main` protegida**: Aloja el código base consensuado, compilable, con el esquema de base de datos Flyway inicial y tests verdes.
2. **8 Ramas de trabajo**:
   - `spec-001` (Integrante 1)
   - `spec-002` (Integrante 1)
   - `spec-003` (Integrante 2)
   - `spec-004` (Integrante 2)
   - `spec-005` (Integrante 3)
   - `spec-006` (Integrante 3)
   - `spec-007` (Integrante 4)
   - `spec-008` (Integrante 4)
3. **Pull Requests y Revisión por Pares**: Cada integrante abre su PR hacia `main`. Ningún PR se fusiona sin:
   - Cobertura de tests unitarios y de integración con Testcontainers.
   - Verificación de no regresión en los invariantes de la constitución.
   - Aprobación por pares de al menos otro integrante del equipo.
