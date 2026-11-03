package com.iw.rrhh.leave.dto;

import jakarta.validation.constraints.Size;

public record ApproveLeaveRequest(
    @Size(max = 500, message = "El comentario no puede exceder 500 caracteres")
    String comment
) {}
