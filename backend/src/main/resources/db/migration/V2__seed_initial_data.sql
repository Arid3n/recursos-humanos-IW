-- =============================================================================
-- V2__seed_initial_data.sql
-- Datos iniciales de convenios y usuario administrador de RRHH
-- =============================================================================

-- Convenios Colectivos
INSERT INTO collective_agreements (id, code, name, vacation_days_per_year)
VALUES 
    (1, 'GENERAL', 'Convenio Colectivo General de Empresa', 22),
    (2, 'TIC', 'Convenio Colectivo Estatal de Empresas de Consultoría y TIC', 23)
ON CONFLICT (code) DO NOTHING;

-- Usuario Inicial de RRHH (usuario: rrhh@empresa.com, contraseña: admin123)
-- Hash BCrypt para 'admin123': $2a$10$X4R408fFKTsrqkfCN4NRP.CuP4lIsN1yoACUP1xXGnYhVcmYcu6gq
INSERT INTO users (id, username, password_hash, role, active)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'rrhh@empresa.com',
    '$2a$10$X4R408fFKTsrqkfCN4NRP.CuP4lIsN1yoACUP1xXGnYhVcmYcu6gq',
    'RRHH',
    true
)
ON CONFLICT (username) DO NOTHING;
