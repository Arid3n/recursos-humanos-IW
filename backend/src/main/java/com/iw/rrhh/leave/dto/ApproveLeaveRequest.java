package com.iw.rrhh.leave.dto;

import jakarta.validation.constraints.Size;

/** El comentario se recorta al construir el DTO para que el límite de FR-003 aplique al texto sin espacios extremos. */
public record ApproveLeaveRequest(
    @Size(max = 500, message = "El comentario no puede exceder 500 caracteres")
    String comment
) {
    public ApproveLeaveRequest {
        comment = comment == null ? null : comment.strip();
    }
}
