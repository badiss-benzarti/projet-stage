package com.gestionstages.internship.repository;

import com.gestionstages.internship.entity.DocumentRequest;
import com.gestionstages.internship.enums.RequestStatus;
import com.gestionstages.internship.enums.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {

    List<DocumentRequest> findByInternshipId(Long internshipId);

    /** Toutes les demandes d'un etudiant, y compris celles sans stage rattache. */
    List<DocumentRequest> findByStudentIdOrderByIdDesc(Long studentId);

    boolean existsByInternshipIdAndTypeAndStatus(Long internshipId, RequestType type, RequestStatus status);

    /** Garde-fou des demandes sans stage : une seule en attente a la fois. */
    boolean existsByStudentIdAndTypeAndStatus(Long studentId, RequestType type, RequestStatus status);

    Page<DocumentRequest> findByStatus(RequestStatus status, Pageable pageable);

    /** File du service des stages, hors documents delivres par l'entreprise. */
    Page<DocumentRequest> findByStatusAndTypeNot(RequestStatus status, RequestType type,
                                                 Pageable pageable);

    /** File de l'entreprise : ce qu'elle seule peut delivrer. */
    List<DocumentRequest> findByStatusAndType(RequestStatus status, RequestType type);
}
