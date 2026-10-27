package com.iw.rrhh.security.service;

import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.dto.LoginRequest;
import com.iw.rrhh.security.dto.LoginResponse;
import com.iw.rrhh.security.dto.UserSummaryResponse;
import com.iw.rrhh.security.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.NoSuchElementException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!user.isActive()) {
            throw new DisabledException("La cuenta de usuario se encuentra desactivada");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Token para cabecera HTTP Basic: Base64(username:password)
        String rawCredentials = request.username() + ":" + request.password();
        String basicToken = Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                basicToken
        );
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new BadCredentialsException("No hay usuario autenticado");
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + principal.getName()));

        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
