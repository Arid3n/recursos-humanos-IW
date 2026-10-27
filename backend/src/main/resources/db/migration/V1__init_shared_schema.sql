-- =============================================================================
-- V1__init_shared_schema.sql
-- Base de Datos Compartida: SI-09 Recursos Humanos y Vacaciones
-- =============================================================================

-- 1. Tabla de Usuarios y Autenticación
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('EMPLEADO', 'RESPONSABLE_EQUIPO', 'RRHH'))
);

-- 2. Tabla de Convenios Colectivos
CREATE TABLE collective_agreements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    vacation_days_per_year INTEGER NOT NULL DEFAULT 22,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vacation_days_positive CHECK (vacation_days_per_year > 0)
);

-- 3. Tabla de Empleados
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(150) NOT NULL,
    national_id VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    manager_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    hire_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. Trayectoria Contractual (Append-Only - Art. 11)
CREATE TABLE contract_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    effective_from DATE NOT NULL,
    position VARCHAR(150) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2) NOT NULL,
    collective_agreement_id BIGINT NOT NULL REFERENCES collective_agreements(id) ON DELETE RESTRICT,
    manager_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_salary_positive CHECK (salary >= 0)
);

CREATE INDEX idx_contract_records_lookup ON contract_records (employee_id, effective_from DESC, created_at DESC);
CREATE INDEX idx_contract_department ON contract_records (department);

-- 5. Solicitudes de Vacaciones y Permisos (Arts. 12, 13 y 14)
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    calculated_days INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    resolved_by UUID REFERENCES employees(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_leave_type CHECK (leave_type IN ('VACATION', 'PERMIT')),
    CONSTRAINT chk_calculated_days_positive CHECK (calculated_days >= 0)
);

CREATE INDEX idx_leave_calendar ON leave_requests (start_date, end_date, status);
CREATE INDEX idx_leave_employee_status ON leave_requests (employee_id, status);

-- 6. Notificaciones Persistidas (Art. 15)
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, is_read, created_at DESC);

-- 7. Evaluaciones de Desempeño (Art. 18)
CREATE TABLE performance_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    evaluator_id UUID NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    period VARCHAR(50) NOT NULL,
    objectives TEXT NOT NULL,
    rating INTEGER NOT NULL,
    comments TEXT,
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
