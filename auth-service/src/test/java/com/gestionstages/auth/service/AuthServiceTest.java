package com.gestionstages.auth.service;

import com.gestionstages.auth.dto.RegisterRequest;
import com.gestionstages.auth.entity.User;
import com.gestionstages.auth.enums.Role;
import com.gestionstages.auth.exception.ApiExceptions;
import com.gestionstages.auth.repository.UserRepository;
import com.gestionstages.auth.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock  UserRepository users;
    @Mock  PasswordEncoder encoder;
    @Mock  JwtService jwt;
    @Mock  AuthenticationManager authManager;

    @InjectMocks AuthService auth;

    private static final RegisterRequest DEMANDE = new RegisterRequest(
            "Ahmed@Esprit.TN", "MotDePasse@2026", "Ahmed", "Ben Salah", Role.ETUDIANT);

    @Test
    @DisplayName("l'inscription refuse un email deja utilise")
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> auth.register(DEMANDE))
                .isInstanceOf(ApiExceptions.EmailAlreadyUsedException.class);

        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("l'inscription hache le mot de passe et ne le stocke jamais en clair")
    void registerHashesPassword() {
        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(encoder.encode("MotDePasse@2026")).thenReturn("$2a$10$hash");
        when(jwt.generate(any())).thenReturn("jeton");

        auth.register(DEMANDE);

        ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
        verify(users).save(capture.capture());

        assertThat(capture.getValue().getPassword())
                .isEqualTo("$2a$10$hash")
                .isNotEqualTo("MotDePasse@2026");
    }

    @Test
    @DisplayName("l'email est normalise en minuscules pour eviter les doublons de casse")
    void registerLowercasesEmail() {
        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hash");
        when(jwt.generate(any())).thenReturn("jeton");

        auth.register(DEMANDE);

        ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
        verify(users).save(capture.capture());

        assertThat(capture.getValue().getEmail()).isEqualTo("ahmed@esprit.tn");
    }
}
