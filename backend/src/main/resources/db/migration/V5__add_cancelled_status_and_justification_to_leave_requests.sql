-- V5: Add CANCELLED status and justification to leave_requests

ALTER TABLE leave_requests
ADD COLUMN IF NOT EXISTS justification TEXT;

ALTER TABLE leave_requests
DROP CONSTRAINT IF EXISTS chk_leave_status;

ALTER TABLE leave_requests
ADD CONSTRAINT chk_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));
