package com.iw.rrhh.security.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El identificador de usuario o email es obligatorio")
    String username,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {
}
