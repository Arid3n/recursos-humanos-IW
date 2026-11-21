# Research & Technical Decisions: spec-008 — Evaluación de Desempeño con Conformidad

**Feature**: `spec-008` · HU-08  
**Date**: 2026-10-27  
**Asignación**: Integrante 4 (Analítica de RRHH y Desempeño)

---

## Decisión 1: Inmutabilidad a dos niveles — aplicación y base de datos

### Context & Problem
La spec exige que una evaluación `FINALIZED` sea completamente inmutable (FR-006, Art. 18). Es necesario garantizarlo tanto si el acceso pasa por la aplicación como si alguien accede directamente a la base de datos.

### Decision
Se implementa inmutabilidad en dos niveles:

1. **Nivel aplicación**: el método `PerformanceReviewServiceImpl.acknowledge()` comprueba que el estado sea `PENDING_ACKNOWLEDGMENT` antes de llamar a `PerformanceReview.acknowledge()`. Si el estado ya es `FINALIZED`, lanza `PerformanceReviewAlreadyFinalizedException` (mapeada a `409 Conflict` por `GlobalExceptionHandler`).

2. **Nivel base de datos** (migración `V4__immutable_finalized_performance_reviews.sql`): un trigger `BEFORE INSERT OR UPDATE OR DELETE` llamado `trg_performance_reviews_immutable_finalized` lanza una excepción PostgreSQL si:
   - Se intenta insertar una fila directamente en estado `FINALIZED`.
   - Se intenta modificar o eliminar una fila cuyo `OLD.status = 'FINALIZED'`.
   - Un `UPDATE` a `FINALIZED` llega sin `employee_acknowledged_at` o `finalized_at`.

### Alternatives Considered
- **Solo validación en la aplicación**: descartado; un acceso directo a la BD o un bug podría mutar evaluaciones formalizadas, violando el Art. 18 irrecuperablemente.
- **Hibernate Envers / tabla de auditoría**: descartado; la spec exige inmutabilidad, no solo auditoría del historial.

---

## Decisión 2: Fecha de formalización generada en servidor con `Clock` inyectado

### Context & Problem
La spec prohíbe que el cliente manipule la marca temporal de conformidad (edge case, Art. 18). Un `Instant.now()` sin inyección impide la prueba unitaria determinista.

### Decision
`PerformanceReviewServiceImpl` recibe un `Clock` mediante inyección de dependencias (configurado como `@Bean Clock.systemUTC()` en la configuración de la aplicación). En `acknowledge()`, la hora de formalización se obtiene con `Instant.now(clock)`. Los tests unitarios sustituyen el `Clock` por uno fijo con `Clock.fixed(...)`.

### Alternatives Considered
- **`Instant.now()` directo**: descartado; imposibilita tests deterministas y deja la hora expuesta a manipulación cliente.
- **Parámetro de fecha en la petición**: descartado; viola el edge case de la spec (la fecha la genera el servidor).

---

## Decisión 3: Aislamiento y control de acceso basado en la sesión

### Context & Problem
Un empleado no debe poder ver ni dar conformidad a evaluaciones ajenas. Un responsable no puede evaluar a empleados de otro equipo (FR-007, Art. 10).

### Decision
Todos los métodos de `PerformanceReviewServiceImpl` obtienen el `Employee` autenticado a través de `SecurityContextHolder` → `UserRepository.findByUsername` → `EmployeeRepository.findByUserId`. No se acepta ningún identificador de usuario desde el cliente.

- **`getMyReviews()`**: filtra por `employee.id = authenticatedEmployee.id`.
- **`getTeamReviews()`**: filtra por `employee.manager.id = authenticatedEmployee.id` (solo equipo actual).
- **`createReview(employeeId, ...)`**: comprueba que `targetEmployee.manager.id == authenticatedEmployee.id` y que el empleado está activo antes de crear la evaluación; en caso contrario lanza `AccessDeniedException` (→ `403 Forbidden`).
- **`acknowledge(reviewId, ...)`**: comprueba que `review.employee.id == authenticatedEmployee.id`.

### Alternatives Considered
- **Pasar el `employeeId` del evaluador en el body de creación**: descartado; el evaluador siempre es el responsable en sesión; aceptarlo desde el cliente abriría un vector de suplantación.

---

## Decisión 4: Bloqueo pesimista en `acknowledge` para evitar conformidades concurrentes

### Context & Problem
Si dos peticiones de conformidad llegan simultáneamente sobre la misma evaluación, sin coordinación podrían pasar ambas la comprobación de estado y generar una evaluación con estado inconsistente.

### Decision
`PerformanceReviewRepository.findByIdForUpdate` usa `@Lock(LockModeType.PESSIMISTIC_WRITE)` junto a `@EntityGraph`. La transacción de `acknowledge` adquiere un bloqueo exclusivo a nivel de fila en PostgreSQL; la segunda petición espera hasta que la primera libera el bloqueo y luego detecta el estado `FINALIZED` y lanza `PerformanceReviewAlreadyFinalizedException`.

### Alternatives Considered
- **Bloqueo optimista (`@Version`)**: la entidad tiene `@Version` como red de seguridad adicional, pero el bloqueo optimista solo falla *después* de confirmar la transacción, exponiendo una ventana de escritura simultánea no detectada.

---

## Decisión 5: Escala de valoración — restricción en cliente, servidor y base de datos

### Context & Problem
El campo `rating` debe estar entre 1 y 5 (FR-008). Un solo punto de validación es insuficiente.

### Decision
La restricción se aplica en tres capas:
1. **Frontend**: el `mat-slider` (o `mat-select`) limita la selección a valores enteros 1–5.
2. **Backend (DTO)**: `@Min(1) @Max(5) Integer rating` en `CreatePerformanceReviewRequest`.
3. **Base de datos**: `CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)` en `V1__init_shared_schema.sql`.

---

## Cobertura de la Constitución

| Artículo | Requisito | Enfoque spec-008 |
|---|---|---|
| Art. 1 | Spec como verdad | Todas las decisiones derivan de HU-08 y sus clarificaciones. |
| Art. 5 / 7 | Capas desacopladas | `PerformanceReviewController` → `PerformanceReviewService` → `PerformanceReviewRepository` → PostgreSQL. |
| Art. 8 | PostgreSQL real | Restricción `CHECK` y trigger validados con Testcontainers. |
| Art. 10 | Autorización en servidor | Acceso derivado siempre de la sesión; sin identificadores de usuario desde el cliente. |
| Art. 18 | Inmutabilidad | Doble garantía: lógica de aplicación + trigger de BD; `finalizedAt` del servidor vía `Clock` inyectado. |
| Art. 22 | Test-First con Testcontainers | `PerformanceReviewIntegrationTest` contra PostgreSQL real. |
| Art. 23 | Simplicidad | Sin librerías externas; entidad de dominio con método `acknowledge()` encapsulado. |
