package com.gestionstages.user.service;

import com.gestionstages.user.dto.StudentDto;
import com.gestionstages.user.entity.Student;
import com.gestionstages.user.exception.ApiExceptions;
import com.gestionstages.user.repository.StudentRepository;
import com.gestionstages.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository students;

    /** Cree le profil du porteur du jeton : un etudiant ne peut creer que le sien. */
    @Transactional
    public StudentDto.Response createOwn(AuthenticatedUser me, StudentDto.Request req) {
        if (students.existsByUserId(me.id())) {
            throw new ApiExceptions.ProfileAlreadyExistsException("etudiant");
        }
        Student s = Student.builder()
                .userId(me.id())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email().toLowerCase())
                .phone(blankToNull(req.phone()))
                .cin(blankToNull(req.cin()))
                .classe(req.classe().toUpperCase())
                .departement(req.departement())
                .build();
        return StudentDto.Response.from(students.save(s));
    }

    @Transactional(readOnly = true)
    public StudentDto.Response findOwn(AuthenticatedUser me) {
        return students.findByUserId(me.id())
                .map(StudentDto.Response::from)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Profil etudiant", me.email()));
    }

    @Transactional(readOnly = true)
    public StudentDto.Response findById(Long id) {
        return students.findById(id)
                .map(StudentDto.Response::from)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Etudiant", id));
    }

    @Transactional(readOnly = true)
    public Page<StudentDto.Response> findAll(String departement, String classe, Pageable pageable) {
        Page<Student> page;
        if (departement != null && !departement.isBlank()) {
            page = students.findByDepartementIgnoreCase(departement, pageable);
        } else if (classe != null && !classe.isBlank()) {
            page = students.findByClasseIgnoreCase(classe, pageable);
        } else {
            page = students.findAll(pageable);
        }
        return page.map(StudentDto.Response::from);
    }

    /** Mise a jour du profil du porteur du jeton uniquement. */
    @Transactional
    public StudentDto.Response updateOwn(AuthenticatedUser me, StudentDto.Request req) {
        Student s = students.findByUserId(me.id())
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Profil etudiant", me.email()));

        s.setFirstName(req.firstName());
        s.setLastName(req.lastName());
        s.setEmail(req.email().toLowerCase());
        s.setPhone(blankToNull(req.phone()));
        s.setCin(blankToNull(req.cin()));
        s.setClasse(req.classe().toUpperCase());
        s.setDepartement(req.departement());

        return StudentDto.Response.from(s);
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
