# Data Model: spec-001 (Alta de Empleados y Trayectoria Contractual)

**Feature**: `spec-001` · HU-01  
**Date**: 2026-10-27  
**Database**: PostgreSQL 16 (Migración `V1__init_shared_schema.sql`)

---

## 1. Entidades del Dominio

### 1.1. `Employee` (Ficha Básica del Empleado)
Representa la identidad principal del trabajador en la organización.
```java
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "national_id", nullable = false, unique = true, length = 50)
    private String nationalId; // DNI / NIE

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager; // Nulo únicamente si es CEO / Dirección General

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
```

### 1.2. `ContractRecord` (Historial Contractual Inmutable — *Append-Only*)
Almacena cada versión de las condiciones laborales pactadas.
```java
@Entity
@Table(name = "contract_records")
public class ContractRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false, length = 150)
    private String position;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collective_agreement_id", nullable = false)
    private CollectiveAgreement collectiveAgreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;
}
```

### 1.3. `CollectiveAgreement` (Convenio Colectivo)
```java
@Entity
@Table(name = "collective_agreements")
public class CollectiveAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "vacation_days_per_year", nullable = false)
    private Integer vacationDaysPerYear;
}
```

---

## 2. Reglas de Validación y Restricciones de Integridad

| Entidad | Campo / Restricción | Tipo | Regla de Negocio |
| :--- | :--- | :--- | :--- |
| `Employee` | `national_id` | `UNIQUE` | DNI/NIE único en la organización. |
| `Employee` | `email` | `UNIQUE` | Correo corporativo único. |
| `Employee` | `manager_id` | `FK` | Obligatorio salvo que el puesto sea "CEO" o "Dirección General". |
| `ContractRecord` | `effective_from` | `NOT NULL` | Toda modificación contractual DEBE tener fecha de efectos. |
| `ContractRecord` | `salary` | `CHECK (salary >= 0)` | Salario positivo con escala 2 decimales (`NUMERIC(12,2)`). |
| `ContractRecord` | Mutabilidad | *Append-Only* | Prohibido UPDATE y DELETE en la capa de persistencia. |

---

## 3. Consultas Clave de Repositorio

### 3.1. Obtener contrato vigente a una fecha dada
```java
@Query("""
    SELECT c FROM ContractRecord c
    WHERE c.employee.id = :employeeId AND c.effectiveFrom <= :targetDate
    ORDER BY c.effectiveFrom DESC, c.createdAt DESC
    LIMIT 1
""")
Optional<ContractRecord> findCurrentContract(
    @Param("employeeId") UUID employeeId, 
    @Param("targetDate") LocalDate targetDate
);
```

### 3.2. Reconstrucción cronológica de la trayectoria
```java
@Query("""
    SELECT c FROM ContractRecord c
    JOIN FETCH c.collectiveAgreement
    LEFT JOIN FETCH c.manager
    WHERE c.employee.id = :employeeId
    ORDER BY c.effectiveFrom ASC, c.createdAt ASC
""")
List<ContractRecord> findFullHistoryChronological(@Param("employeeId") UUID employeeId);
```
