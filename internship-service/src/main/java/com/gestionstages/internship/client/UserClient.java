package com.gestionstages.internship.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * Appels vers user-service, resolus par Eureka (aucune URL en dur).
 *
 * Sert a traduire l'identite du porteur du jeton en identifiant metier :
 * un compte ENTREPRISE (uid 3) correspond a l'entreprise 1. Cette
 * correspondance vit dans user-service, pas ici.
 *
 * Le jeton est retransmis tel quel : user-service applique ses propres
 * regles de role, ce service ne court-circuite aucune securite.
 */
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/students/me")
    StudentRef myStudentProfile(@RequestHeader("Authorization") String bearer);

    @GetMapping("/companies/me")
    CompanyRef myCompanyProfile(@RequestHeader("Authorization") String bearer);

    @GetMapping("/supervisors/me")
    SupervisorRef mySupervisorProfile(@RequestHeader("Authorization") String bearer);

    /**
     * Encadrants declares par une entreprise, projection allegee.
     *
     * Sert a verifier que l'encadrant propose par l'etudiant appartient
     * bien a l'entreprise choisie : le client envoie un identifiant, pas
     * une preuve d'appartenance.
     */
    @GetMapping("/companies/{companyId}/supervisors/options")
    List<SupervisorOption> supervisorOptions(@PathVariable("companyId") Long companyId,
                                             @RequestHeader("Authorization") String bearer);

    record SupervisorOption(Long id, String fullName, String position, Long companyId) {}

    /** Projection minimale : on ne deserialise que ce dont on a besoin. */
    record StudentRef(Long id, Long userId, String firstName, String lastName, String email) {
        public String fullName() { return firstName + " " + lastName; }
    }

    record CompanyRef(Long id, Long userId, String name) {}

    record SupervisorRef(Long id, Long userId, String firstName, String lastName, Long companyId) {
        public String fullName() { return firstName + " " + lastName; }
    }
}
