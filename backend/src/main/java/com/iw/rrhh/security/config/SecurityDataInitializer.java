package com.iw.rrhh.security.config;

import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String defaultUsername = "rrhh@empresa.com";
        String defaultPassword = "admin123";

        userRepository.findByUsername(defaultUsername).ifPresentOrElse(
                user -> {
                    if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
                        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
                        user.setActive(true);
                        user.setRole(UserRole.RRHH);
                        userRepository.save(user);
                        log.info("Actualizada contraseña de usuario inicial RRHH: {}", defaultUsername);
                    }
                },
                () -> {
                    User rrhh = new User(
                            defaultUsername,
                            passwordEncoder.encode(defaultPassword),
                            UserRole.RRHH
                    );
                    userRepository.save(rrhh);
                    log.info("Creado usuario inicial RRHH: {} / {}", defaultUsername, defaultPassword);
                }
        );
    }
}
