package com.gestionstages.evaluation.repository;

import com.gestionstages.evaluation.entity.Claim;
import com.gestionstages.evaluation.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Page<Claim> findByStudentId(Long studentId, Pageable pageable);

    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    Page<Claim> findByStatusNot(ClaimStatus status, Pageable pageable);

    @Query("select c from Claim c left join fetch c.messages where c.id = :id")
    Optional<Claim> findByIdWithMessages(Long id);
}
