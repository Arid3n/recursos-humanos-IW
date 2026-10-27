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
-- Hash BCrypt para 'admin123': $2a$10$fG6mH8mR36p53rM1tWz70.dK6qA9n8zV6sU5wY2tX1mN3vB4c5d6e (o estándar)
INSERT INTO users (id, username, password_hash, role, active)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'rrhh@empresa.com',
    '$2a$10$1r2/iGv8Jek2a12B5j3bXeQv6p4R7t8y9u0i1o2p3a4s5d6f7g8h9',
    'RRHH',
    true
)
ON CONFLICT (username) DO NOTHING;
