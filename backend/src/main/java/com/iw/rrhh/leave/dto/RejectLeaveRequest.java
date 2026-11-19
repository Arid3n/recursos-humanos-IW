package com.iw.rrhh.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El motivo se recorta al construir el DTO para que los límites de FR-003 apliquen al texto sin espacios extremos. */
public record RejectLeaveRequest(
    @NotBlank(message = "El motivo del rechazo es obligatorio")
    @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
    String reason
) {
    public RejectLeaveRequest {
        reason = reason == null ? null : reason.strip();
    }
}
