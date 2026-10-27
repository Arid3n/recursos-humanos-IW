# SI-09 · Sistema de Gestión de Recursos Humanos y Vacaciones

Sistema web desarrollado con **Spring Boot (Java 21)**, **PostgreSQL 16** y **Angular (TypeScript)** para la gestión integral de personal, ausencias, calendario de equipos, informes departamentales y evaluaciones de desempeño.

---

## 1. Reparto de Responsabilidades y Ramas (Equipo de 4 Integrantes)

Para coordinar el desarrollo según el flujo de trabajo guiado por especificaciones (SDD) y *GitHub Flow*, la base común se integra en `main` y cada integrante desarrolla sus historias en ramas dedicadas:

| Integrante | Historia de Usuario | Especificación | Rama Git | Responsabilidad Funcional |
| :--- | :--- | :--- | :--- | :--- |
| **Integrante 1** | **HU-01** · Alta de empleados y contratos<br>**HU-02** · Ficha y saldo de vacaciones | [spec-001](specs/spec-001/spec.md)<br>[spec-002](specs/spec-002/spec.md) | `spec-001`<br>`spec-002` | Gestión de empleados, historial contractual *append-only* (Art. 11) y cálculo de saldo derivado (Art. 12). |
| **Integrante 2** | **HU-03** · Solicitud de vacaciones o permisos<br>**HU-04** · Seguimiento de solicitudes | [spec-003](specs/spec-003/spec.md)<br>[spec-004](specs/spec-004/spec.md) | `spec-003`<br>`spec-004` | Flujo de ausencias del empleado: validación de días en servidor, estado `Pendiente` y listado con filtros y motivos. |
| **Integrante 3** | **HU-05** · Aprobación o rechazo<br>**HU-06** · Calendario de ausencias del equipo | [spec-005](specs/spec-005/spec.md)<br>[spec-006](specs/spec-006/spec.md) | `spec-005`<br>`spec-006` | Funciones del responsable de equipo: resolución atómica con notificación persistida y calendario mensual accesible con solapes. |
| **Integrante 4** | **HU-07** · Informes y exportación<br>**HU-08** · Evaluación de desempeño | [spec-007](specs/spec-007/spec.md)<br>[spec-008](specs/spec-008/spec.md) | `spec-007`<br>`spec-008` | Analítica para RRHH (informes agregados y exportación CSV idéntica a pantalla) y ciclo bilateral de evaluación inmutable. |

---

## 2. Documentación del Sistema
* [Constitución del Proyecto](.specify/memory/constitution.md) (Principios no negociables y gobernanza)
* [Plan Técnico Compartido](specs/plan.md) (Pila tecnológica y arquitectura en 4 capas)
* [Modelo de Datos Unificado](specs/data-model.md) (Diagrama ER y esquema relacional PostgreSQL)

---

## 3. Pila Tecnológica
* **Backend:** Java 21 · Spring Boot 3.4.3 (Web, Security, Data JPA, Validation, Actuator, Flyway).
* **Persistencia:** PostgreSQL 16 · Migraciones versionadas en `backend/src/main/resources/db/migration/`.
* **Testing:** JUnit 5, Mockito y Testcontainers (PostgreSQL real).
* **Frontend:** Angular 21 · Angular Material 22 · TypeScript (strict mode).
