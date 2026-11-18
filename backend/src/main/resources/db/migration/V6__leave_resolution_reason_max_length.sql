-- V6: límite de 500 caracteres para el motivo de rechazo / comentario de aprobación (HU-05, FR-003, Art. 8.2)
-- V3 (restricciones de resolución), V4 (HU-08) y V5 (HU-04, estado CANCELLED) ya están en main: no se editan.

ALTER TABLE leave_requests
    ADD CONSTRAINT chk_leave_rejection_reason_max
    CHECK (rejection_reason IS NULL OR char_length(rejection_reason) <= 500);

-- Reversión:
--   ALTER TABLE leave_requests DROP CONSTRAINT chk_leave_rejection_reason_max;
