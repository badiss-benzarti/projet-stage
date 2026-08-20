package com.gestionstages.user.repository;

import com.gestionstages.user.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<Student> findByDepartementIgnoreCase(String departement, Pageable pageable);

    Page<Student> findByClasseIgnoreCase(String classe, Pageable pageable);
}
