package com.gestionstages.evaluation.repository;

import com.gestionstages.evaluation.entity.Evaluation;
import com.gestionstages.evaluation.enums.EvaluationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByInternshipId(Long internshipId);

    boolean existsByInternshipId(Long internshipId);

    Page<Evaluation> findByStatus(EvaluationStatus status, Pageable pageable);

    List<Evaluation> findByStatusOrderByStudentNameAsc(EvaluationStatus status);

    List<Evaluation> findByStudentId(Long studentId);
}
