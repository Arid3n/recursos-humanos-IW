# Modelo de Datos Compartido — Sistema de Gestión de Recursos Humanos y Vacaciones

**Fecha**: 2026-10-27  
**Estado**: Acordado por el equipo (Base Común para `spec-001` a `spec-008`)  
**Motor**: PostgreSQL 16 (Gestionado con migraciones Flyway)

---

## 1. Diagrama Entidad-Relación (ER)

```mermaid
erDiagram
    USERS ||--|| EMPLOYEES : "tiene cuenta de usuario"
    COLLECTIVE_AGREEMENTS ||--o{ CONTRACT_RECORDS : "aplica en contrato"
    EMPLOYEES ||--o{ CONTRACT_RECORDS : "historial contractual (append-only)"
    EMPLOYEES ||--o{ EMPLOYEES : "responsable de equipo (manager_id)"
    EMPLOYEES ||--o{ LEAVE_REQUESTS : "solicita ausencia"
    EMPLOYEES ||--o{ NOTIFICATIONS : "recibe notificación"
    EMPLOYEES ||--o{ PERFORMANCE_REVIEWS : "es evaluado"
    EMPLOYEES ||--o{ PERFORMANCE_REVIEWS : "evalúa como responsable"

    USERS {
        uuid id PK
        varchar username UK
        varchar password_hash
        varchar role "EMPLEADO, RESPONSABLE_EQUIPO, RRHH"
        boolean active
        timestamptz created_at
    }

    COLLECTIVE_AGREEMENTS {
        bigserial id PK
        varchar code UK "Convenio general, etc."
        varchar name
        integer vacation_days_per_year "Días base anuales (p. ej. 22)"
        timestamptz created_at
    }

    EMPLOYEES {
        uuid id PK
        uuid user_id FK, UK
        varchar first_name
        varchar last_name
        varchar national_id UK "DNI/NIE"
        varchar email UK
        uuid manager_id FK "Responsable de equipo actual"
        date hire_date "Fecha de incorporación"
        boolean active
        timestamptz created_at
        bigint version
    }

    CONTRACT_RECORDS {
        bigserial id PK
        uuid employee_id FK
        date effective_from "Fecha de entrada en vigor (inmutable)"
        varchar position "Puesto de trabajo"
        varchar department "Departamento (TI, Ventas, etc.)"
        numeric salary "Salario bruto anual (NUMERIC 12,2)"
        bigint collective_agreement_id FK
        uuid manager_id FK "Responsable en dicho periodo"
        timestamptz created_at
        varchar created_by
    }

    LEAVE_REQUESTS {
        uuid id PK
        uuid employee_id FK
        varchar leave_type "VACATION, PERMIT"
        date start_date
        date end_date
        integer calculated_days "Días laborables computados"
        varchar status "PENDING, APPROVED, REJECTED"
        text rejection_reason "Motivo al rechazar"
        uuid resolved_by FK "Responsable que resuelve"
        timestamptz resolved_at "Momento de resolución"
        timestamptz created_at
        bigint version "Bloqueo optimista"
    }

    NOTIFICATIONS {
        uuid id PK
        uuid recipient_id FK "Empleado destinatario"
        varchar title
        text message
        boolean is_read "Leído / No leído"
        timestamptz created_at
    }

    PERFORMANCE_REVIEWS {
        uuid id PK
        uuid employee_id FK "Empleado evaluado"
        uuid evaluator_id FK "Responsable evaluador"
        varchar period "Periodo (p. ej. 2026-ANUAL)"
        text objectives "Objetivos fijados"
        integer rating "Valoración numérica (1-5)"
        text comments "Comentarios cualitativos"
        varchar status "PENDING_ACKNOWLEDGMENT, FINALIZED"
        timestamptz employee_acknowledged_at "Conformidad empleado"
        timestamptz finalized_at "Cierre inmutable"
        timestamptz created_at
        bigint version
    }
```

---

## 2. Descripción de Entidades y Restricciones Constitucionales

### 2.1. `users` (Seguridad y Autenticación)
* Gestiona credenciales y el rol principal (`EMPLEADO`, `RESPONSABLE_EQUIPO`, `RRHH`).
* Las contraseñas se almacenan mediante hash seguro (BCrypt).

### 2.2. `collective_agreements` (Convenios Colectivos)
* Fuente de verdad para el cálculo derivado de saldo de vacaciones (`vacation_days_per_year`).

### 2.3. `employees` y `contract_records` (Historial Contractual Append-Only - Art. 11)
* **`employees`**: Cabecera del trabajador y datos personales identificativos.
* **`contract_records`**: Cada cambio salarial, de puesto o de departamento genera un nuevo registro con `effective_from`.
* **Invariante**: Prohibido el borrado o actualización de registros existentes en `contract_records` (*append-only*). Las condiciones vigentes a una fecha determinada se resuelven mediante:
  ```sql
  SELECT * FROM contract_records
  WHERE employee_id = :empId AND effective_from <= :targetDate
  ORDER BY effective_from DESC, created_at DESC
  LIMIT 1;
  ```

### 2.4. `leave_requests` (Solicitudes y Saldo Derivado - Arts. 12, 13 y 14)
* **Restricción de Integridad en BD**:
  ```sql
  CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
  CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
  CONSTRAINT chk_leave_type CHECK (leave_type IN ('VACATION', 'PERMIT'))
  ```
* **Saldo Derivado (Art. 12)**: El saldo disponible de un empleado para el año $Y$ se calcula en tiempo de ejecución:
  $$\text{Días Disponibles} = \text{Días Convenio Vigente} - \sum \text{Días de } \text{leave\_requests(status='APPROVED', type='VACATION', year=Y)}$$
* **Resolución Atómica (Art. 13)**: `PENDING → APPROVED` o `PENDING → REJECTED`. Una vez resuelto, `version` y control en servicio bloquean transiciones posteriores devolviendo `409 Conflict`.

### 2.5. `notifications` (Notificaciones Persistidas - Art. 15)
* Cada resolución de solicitud genera un registro persistido en esta tabla para el empleado solicitante.

### 2.6. `performance_reviews` (Evaluación con Conformidad - Art. 18)
* **Restricción de Integridad**:
  ```sql
  CONSTRAINT chk_review_status CHECK (status IN ('PENDING_ACKNOWLEDGMENT', 'FINALIZED'))
  CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
  ```
* **Inmutabilidad**: Al pulsar conformidad el empleado, se actualiza `status = 'FINALIZED'` y `finalized_at = now()`. A partir de ese momento, la entidad queda bloqueada para escritura.

---

## 3. Índices Estratégicos (Art. 8.5)

Para garantizar un rendimiento óptimo en las consultas de las 8 specs, la base común incluye:
1. `idx_contract_records_lookup`: `(employee_id, effective_from DESC)` para resolver rápidamente las condiciones vigentes.
2. `idx_leave_calendar`: `(start_date, end_date, status)` para la cuadrícula mensual del calendario de equipo (HU-06).
3. `idx_leave_employee_status`: `(employee_id, status)` para el listado filtrable de HU-04 y el cálculo de saldo de HU-02.
4. `idx_reviews_employee`: `(employee_id, period)` para la consulta de evaluaciones de HU-08.
