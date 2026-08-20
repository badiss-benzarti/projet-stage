package com.gestionstages.auth.controller;

import com.gestionstages.auth.dto.*;
import com.gestionstages.auth.security.JwtService;
import com.gestionstages.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints d'authentification.
 *
 * Le chemin complet /api/auth/** est conserve tel quel : le gateway route
 * sans StripPrefix, donc l'URL vue par le frontend est celle du controleur.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    /** Profil du porteur du jeton. Necessite un jeton valide. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal String email) {
        return auth.currentUser(email);
    }

    /**
     * Verification d'un jeton, utile pour deboguer le gateway et le frontend.
     * Public : ne revele rien qu'un porteur du jeton ne sache deja.
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "");

        if (!jwt.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false));
        }

        var claims = jwt.parse(token);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "email", claims.getSubject(),
                "role", claims.get("role", String.class),
                "expiresAt", claims.getExpiration().toInstant().toString()));
    }
}
