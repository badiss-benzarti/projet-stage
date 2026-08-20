package com.gestionstages.evaluation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDate;

/** Lecture du dossier de stage, source de verite du internship-service. */
@FeignClient(name = "internship-service", path = "/api/internships")
public interface InternshipClient {

    @GetMapping("/{id}")
    InternshipRef byId(@PathVariable Long id, @RequestHeader("Authorization") String bearer);

    record InternshipRef(
            Long id,
            Long studentId, String studentName, String studentEmail,
            String type, String title, String status,
            Long companyId, String companyName,
            Long supervisorId, String supervisorName,
            LocalDate startDate, LocalDate endDate
    ) {}
}
