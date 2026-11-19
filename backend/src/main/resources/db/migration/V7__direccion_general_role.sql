-- V7: rol DIRECCION_GENERAL (HU-05, FR-017)
-- La cúspide de la jerarquía: sin responsable, resuelve las solicitudes y ve el calendario de sus
-- subordinados directos, y no solicita ausencias.

ALTER TABLE users
DROP CONSTRAINT IF EXISTS chk_user_role;

ALTER TABLE users
ADD CONSTRAINT chk_user_role CHECK (role IN ('EMPLEADO', 'RESPONSABLE_EQUIPO', 'RRHH', 'DIRECCION_GENERAL'));
