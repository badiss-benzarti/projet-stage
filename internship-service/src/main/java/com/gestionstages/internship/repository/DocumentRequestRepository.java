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

    boolean existsByInternshipIdAndTypeAndStatus(Long internshipId, RequestType type, RequestStatus status);

    Page<DocumentRequest> findByStatus(RequestStatus status, Pageable pageable);
}
