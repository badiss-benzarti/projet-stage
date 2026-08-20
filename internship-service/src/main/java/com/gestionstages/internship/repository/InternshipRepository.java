package com.gestionstages.internship.repository;

import com.gestionstages.internship.entity.Internship;
import com.gestionstages.internship.enums.InternshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InternshipRepository extends JpaRepository<Internship, Long> {

    Page<Internship> findByStudentId(Long studentId, Pageable pageable);

    Page<Internship> findByCompanyId(Long companyId, Pageable pageable);

    Page<Internship> findBySupervisorId(Long supervisorId, Pageable pageable);

    Page<Internship> findByStatus(InternshipStatus status, Pageable pageable);

    Page<Internship> findByCompanyIdAndStatus(Long companyId, InternshipStatus status, Pageable pageable);

    List<Internship> findByStudentIdAndStatusNotIn(Long studentId, List<InternshipStatus> excluded);

    @Query("select i from Internship i left join fetch i.history where i.id = :id")
    Optional<Internship> findByIdWithHistory(Long id);

    @Query("select i.status, count(i) from Internship i group by i.status")
    List<Object[]> countByStatus();
}
