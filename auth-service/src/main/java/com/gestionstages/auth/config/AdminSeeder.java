package com.gestionstages.auth.config;

import com.gestionstages.auth.entity.User;
import com.gestionstages.auth.enums.Role;
import com.gestionstages.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cree un compte ADMIN au premier demarrage.
 *
 * Sans lui, la plateforme demarre vide : impossible de tester un endpoint
 * protege sans d'abord appeler /register, ce qui fausse la demonstration.
 * Desactivable par app.seed.admin.enabled=false.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Bean
    ApplicationRunner seedAdmin(
            @Value("${app.seed.admin.enabled:true}") boolean enabled,
            @Value("${app.seed.admin.email:admin@esprit.tn}") String email,
            @Value("${app.seed.admin.password:Admin@2026}") String password) {

        return args -> {
            if (!enabled || users.existsByEmailIgnoreCase(email)) {
                return;
            }
            users.save(User.builder()
                    .email(email.toLowerCase())
                    .password(encoder.encode(password))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());

            log.warn("Compte ADMIN cree : {} — changez ce mot de passe avant la soutenance", email);
        };
    }
}
