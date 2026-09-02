package com.gestionstages.evaluation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/** Resolution du profil metier du porteur du jeton. */
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/students/me")
    Ref myStudentProfile(@RequestHeader("Authorization") String bearer);

    @GetMapping("/supervisors/me")
    Ref mySupervisorProfile(@RequestHeader("Authorization") String bearer);

    /**
     * Profils detailles, pour remplir le journal de stage officiel.
     * Interdits au role ETUDIANT : l'appel est donc au mieux, et le
     * document se contente des champs qu'il a pu obtenir.
     */
    @GetMapping("/students/{id}")
    Ref studentById(@PathVariable("id") Long id, @RequestHeader("Authorization") String bearer);

    @GetMapping("/supervisors/{id}")
    Ref supervisorById(@PathVariable("id") Long id, @RequestHeader("Authorization") String bearer);

    /**
     * Les champs au-dela du nom ne sont presents que sur les profils
     * detailles : nuls ailleurs, ce qui est sans consequence puisque le
     * formulaire laisse ces cases vides quand l'information manque.
     */
    record Ref(Long id, Long userId, String firstName, String lastName,
               String email, String cin, String classe, String position) {
        public String fullName() { return firstName + " " + lastName; }
    }
}
