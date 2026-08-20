package com.gestionstages.auth.security;

import com.gestionstages.auth.entity.User;
import com.gestionstages.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "c2VjcmV0LWRlLXRlc3QtdW5pcXVlbWVudC1wb3VyLWxlcy10ZXN0cy11bml0YWlyZXMtMjU2LWJpdHM=";

    private JwtService jwt;
    private User user;

    @BeforeEach
    void setUp() {
        jwt = new JwtService(SECRET, 3_600_000L);
        user = User.builder()
                .id(42L)
                .email("ahmed@esprit.tn")
                .password("peu-importe")
                .firstName("Ahmed")
                .lastName("Ben Salah")
                .role(Role.ETUDIANT)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("le jeton genere contient l'email, l'id et le role")
    void generatedTokenCarriesClaims() {
        var claims = jwt.parse(jwt.generate(user));

        assertThat(claims.getSubject()).isEqualTo("ahmed@esprit.tn");
        assertThat(claims.get("uid", Integer.class)).isEqualTo(42);
        assertThat(claims.get("role", String.class)).isEqualTo("ETUDIANT");
    }

    @Test
    @DisplayName("un jeton fraichement genere est valide")
    void freshTokenIsValid() {
        assertThat(jwt.isValid(jwt.generate(user))).isTrue();
    }

    @Test
    @DisplayName("un jeton malforme est rejete sans lever d'exception")
    void malformedTokenIsRejected() {
        assertThat(jwt.isValid("pas.un.jeton")).isFalse();
        assertThat(jwt.isValid("")).isFalse();
    }

    @Test
    @DisplayName("un jeton signe avec un autre secret est rejete")
    void tokenSignedWithAnotherSecretIsRejected() {
        String autre = "dW4tYXV0cmUtc2VjcmV0LWNvbXBsZXRlbWVudC1kaWZmZXJlbnQtZGUtMjU2LWJpdHMtaWNp";
        String token = new JwtService(autre, 3_600_000L).generate(user);

        assertThat(jwt.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("un jeton expire est rejete")
    void expiredTokenIsRejected() {
        String token = new JwtService(SECRET, -1_000L).generate(user);

        assertThat(jwt.isValid(token)).isFalse();
    }
}
