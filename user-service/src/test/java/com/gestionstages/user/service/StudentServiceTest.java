package com.gestionstages.user.service;

import com.gestionstages.user.dto.StudentDto;
import com.gestionstages.user.entity.Student;
import com.gestionstages.user.exception.ApiExceptions;
import com.gestionstages.user.repository.StudentRepository;
import com.gestionstages.user.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock StudentRepository students;
    @InjectMocks StudentService service;

    private static final AuthenticatedUser MOI =
            new AuthenticatedUser(7L, "ahmed@esprit.tn", "ETUDIANT", "Ahmed", "Ben Salah");

    private static final StudentDto.Request DEMANDE = new StudentDto.Request(
            "Ahmed", "Ben Salah", "Ahmed@Esprit.TN", "20123456", "12345678",
            "4sae3", "Genie Logiciel");

    @Test
    @DisplayName("un etudiant ne peut pas creer deux profils")
    void refusesSecondProfile() {
        when(students.existsByUserId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.createOwn(MOI, DEMANDE))
                .isInstanceOf(ApiExceptions.ProfileAlreadyExistsException.class);

        verify(students, never()).save(any());
    }

    @Test
    @DisplayName("le profil est rattache au porteur du jeton, pas a un id fourni par le client")
    void bindsProfileToTokenOwner() {
        when(students.existsByUserId(anyLong())).thenReturn(false);
        when(students.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createOwn(MOI, DEMANDE);

        ArgumentCaptor<Student> capture = ArgumentCaptor.forClass(Student.class);
        verify(students).save(capture.capture());

        assertThat(capture.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("email en minuscules et classe en majuscules, contre les doublons de casse")
    void normalisesCase() {
        when(students.existsByUserId(anyLong())).thenReturn(false);
        when(students.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createOwn(MOI, DEMANDE);

        ArgumentCaptor<Student> capture = ArgumentCaptor.forClass(Student.class);
        verify(students).save(capture.capture());

        assertThat(capture.getValue().getEmail()).isEqualTo("ahmed@esprit.tn");
        assertThat(capture.getValue().getClasse()).isEqualTo("4SAE3");
    }

    @Test
    @DisplayName("un profil absent remonte une 404 metier, pas un Optional vide")
    void missingProfileRaisesNotFound() {
        when(students.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOwn(MOI))
                .isInstanceOf(ApiExceptions.NotFoundException.class);
    }
}
