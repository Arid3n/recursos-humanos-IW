package com.iw.rrhh.security.controller;

import com.iw.rrhh.security.dto.LoginRequest;
import com.iw.rrhh.security.dto.LoginResponse;
import com.iw.rrhh.security.dto.UserSummaryResponse;
import com.iw.rrhh.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticación y sesión de usuario")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicio de sesión de usuario y obtención de token de acceso")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil y rol del usuario actualmente autenticado")
    public ResponseEntity<UserSummaryResponse> getCurrentUser(Principal principal) {
        UserSummaryResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(response);
    }
}
