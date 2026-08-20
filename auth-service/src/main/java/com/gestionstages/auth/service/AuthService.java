package com.gestionstages.auth.service;

import com.gestionstages.auth.dto.*;
import com.gestionstages.auth.entity.User;
import com.gestionstages.auth.exception.ApiExceptions;
import com.gestionstages.auth.repository.UserRepository;
import com.gestionstages.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {

        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ApiExceptions.EmailAlreadyUsedException(req.email());
        }

        User user = User.builder()
                .email(req.email().toLowerCase())
                .password(encoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .role(req.role())
                .enabled(true)
                .build();

        users.save(user);
        log.info("Compte cree : {} ({})", user.getEmail(), user.getRole());

        return AuthResponse.of(jwt.generate(user), jwt.expirationMs(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {

        // Delegue la verification du mot de passe a Spring Security :
        // leve BadCredentialsException ou DisabledException, tracees par
        // le GlobalExceptionHandler.
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ApiExceptions.UserNotFoundException(req.email()));

        log.info("Connexion : {} ({})", user.getEmail(), user.getRole());

        return AuthResponse.of(jwt.generate(user), jwt.expirationMs(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiExceptions.UserNotFoundException(email));
    }
}
