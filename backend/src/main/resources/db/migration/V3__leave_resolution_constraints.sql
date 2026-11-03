-- =====================================================================
-- V3: Restricciones de resolución de solicitudes de ausencia (HU-05)
-- Art. 8.2 y Art. 13.2 de la Constitución: una solicitud resuelta no
-- puede volver a resolverse, y el refuerzo se aplica también en BD.
--
-- No se restringen resolved_by / resolved_at (ver specs/spec-005/research.md §1.6):
--   * fixtures de HU-03/HU-04 crean solicitudes resueltas sin esos campos;
--   * la FK resolved_by ... ON DELETE SET NULL actualiza filas resueltas.
--
-- Reversión:
--   DROP TRIGGER IF EXISTS trg_leave_requests_immutable_resolution ON leave_requests;
--   DROP FUNCTION IF EXISTS fn_leave_requests_immutable_resolution();
--   ALTER TABLE leave_requests DROP CONSTRAINT IF EXISTS chk_leave_rejection_reason;
-- =====================================================================

-- 1. Un rechazo exige motivo de al menos 5 caracteres (FR-003)
ALTER TABLE leave_requests
    ADD CONSTRAINT chk_leave_rejection_reason
    CHECK (status <> 'REJECTED'
           OR (rejection_reason IS NOT NULL AND char_length(btrim(rejection_reason)) >= 5));

-- 2. Inmutabilidad del estado y del motivo de una solicitud resuelta (FR-004)
CREATE OR REPLACE FUNCTION fn_leave_requests_immutable_resolution()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status <> 'PENDING'
       AND (NEW.status IS DISTINCT FROM OLD.status
            OR NEW.rejection_reason IS DISTINCT FROM OLD.rejection_reason) THEN
        RAISE EXCEPTION 'La solicitud % ya está resuelta y no puede modificarse', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_leave_requests_immutable_resolution
    BEFORE UPDATE ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION fn_leave_requests_immutable_resolution();
