# Data Model: spec-008 — Evaluación de Desempeño con Conformidad

**Feature**: `spec-008` · HU-08  
**Date**: 2026-10-27  
**Database**: PostgreSQL 16 (tabla `performance_reviews` creada en `V1__init_shared_schema.sql`)

---

## 1. Entidad del Dominio: `PerformanceReview`

```java
@Entity
@Table(name = "performance_reviews")
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;          // Empleado evaluado

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Employee evaluator;         // Responsable evaluador

    @Column(nullable = false, length = 50)
    private String period;              // Ej: "2026-ANUAL"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objectives;

    @Column(nullable = false)
    private Integer rating;             // 1 a 5 (CHECK en BD)

    @Column(columnDefinition = "TEXT")
    private String comments;            // Comentarios del responsable

    @Column(name = "employee_comments", columnDefinition = "TEXT")
    private String employeeComments;    // Observaciones opcionales del empleado

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PerformanceReviewStatus status; // PENDING_ACKNOWLEDGMENT | FINALIZED

    @Column(name = "employee_acknowledged_at")
    private Instant employeeAcknowledgedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;          // Bloqueo optimista de red de seguridad
}
```

```java
public enum PerformanceReviewStatus {
    PENDING_ACKNOWLEDGMENT,
    FINALIZED
}
```

---

## 2. DDL PostgreSQL

```sql
CREATE TABLE performance_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    evaluator_id UUID NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    period VARCHAR(50) NOT NULL,
    objectives TEXT NOT NULL,
    rating INTEGER NOT NULL,
    comments TEXT,
    employee_comments TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ACKNOWLEDGMENT',
    employee_acknowledged_at TIMESTAMPTZ,
    finalized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_status CHECK (status IN ('PENDING_ACKNOWLEDGMENT', 'FINALIZED'))
);

CREATE INDEX idx_reviews_employee ON performance_reviews (employee_id, period);
CREATE INDEX idx_reviews_evaluator ON performance_reviews (evaluator_id);
```

**Trigger de inmutabilidad** (migración `V4__immutable_finalized_performance_reviews.sql`):
```sql
-- Prohíbe INSERT directo en FINALIZED, UPDATE/DELETE de cualquier fila FINALIZED,
-- y UPDATE a FINALIZED sin las marcas temporales obligatorias.
CREATE TRIGGER trg_performance_reviews_immutable_finalized
    BEFORE INSERT OR UPDATE OR DELETE ON performance_reviews
    FOR EACH ROW EXECUTE FUNCTION fn_performance_reviews_immutable_finalized();
```

---

## 3. DTOs (records, paquete `com.iw.rrhh.performance.dto`)

```java
// Solicitud de creación (responsable)
public record CreatePerformanceReviewRequest(
    @NotBlank String period,
    @NotBlank String objectives,
    @Min(1) @Max(5) Integer rating,
    @NotBlank String comments
) {}

// Solicitud de conformidad (empleado)
public record AcknowledgePerformanceReviewRequest(
    String employeeComments    // opcional
) {}

// Respuesta completa de evaluación
public record PerformanceReviewResponse(
    UUID id,
    UUID employeeId,
    String employeeName,
    UUID evaluatorId,
    String evaluatorName,
    String period,
    String objectives,
    Integer rating,
    String comments,
    String employeeComments,
    PerformanceReviewStatus status,
    Instant employeeAcknowledgedAt,
    Instant finalizedAt,
    Instant createdAt
) {
    public static PerformanceReviewResponse from(PerformanceReview review) { ... }
}

// Miembro del equipo para el selector de creación
public record PerformanceReviewEmployeeResponse(UUID id, String name) {}
```

---

## 4. Consultas de Repositorio

```java
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {

    // Evaluaciones propias del empleado, ordenadas por periodo desc y fecha de creación desc
    @EntityGraph(attributePaths = {"employee", "evaluator"})
    List<PerformanceReview> findByEmployeeIdOrderByPeriodDescCreatedAtDesc(UUID employeeId);

    // Evaluaciones del equipo del responsable (solo miembros con manager_id = managerId)
    @EntityGraph(attributePaths = {"employee", "evaluator"})
    List<PerformanceReview> findByEmployeeManagerIdOrderByCreatedAtDesc(UUID managerId);

    // Bloqueo pesimista para el flujo de conformidad (evita conformidades concurrentes)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"employee", "evaluator"})
    @Query("SELECT review FROM PerformanceReview review WHERE review.id = :id")
    Optional<PerformanceReview> findByIdForUpdate(@Param("id") UUID id);
}
```

---

## 5. Reglas de validación e integridad

| Campo / Restricción | Tipo | Regla de negocio |
|---|---|---|
| `rating` | `CHECK (rating BETWEEN 1 AND 5)` | Escala entera 1–5 obligatoria; también validada con `@Min(1) @Max(5)` en el DTO. |
| `status` | `CHECK (status IN ('PENDING_ACKNOWLEDGMENT', 'FINALIZED'))` | Solo dos estados posibles. |
| `employee_acknowledged_at`, `finalized_at` | `NOT NULL` al formalizar | El trigger exige ambos campos al pasar a `FINALIZED`. |
| Inmutabilidad | Trigger `trg_performance_reviews_immutable_finalized` | Prohíbe cualquier `UPDATE`/`DELETE` sobre filas `FINALIZED` a nivel de base de datos. |
| `evaluator_id` | `ON DELETE RESTRICT` | No se puede eliminar un evaluador con evaluaciones asociadas. |
| `employee_id` | `ON DELETE CASCADE` | Al dar de baja un empleado (baja lógica; no se borra la fila), sus evaluaciones se conservan. |

---

## 6. Flujo de estados

```
[Creación por RESPONSABLE_EQUIPO]
            │
            ▼
  PENDING_ACKNOWLEDGMENT  ──(empleado da conformidad)──►  FINALIZED (inmutable)
```

- **`PENDING_ACKNOWLEDGMENT`**: el empleado puede leer, añadir comentarios y dar conformidad.
- **`FINALIZED`**: lectura solo. El trigger de BD impide cualquier modificación posterior.
