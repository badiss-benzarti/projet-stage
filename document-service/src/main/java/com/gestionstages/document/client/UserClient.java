package com.gestionstages.document.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/students/me")
    Ref myStudentProfile(@RequestHeader("Authorization") String bearer);

    /**
     * Profil detaille du stagiaire, pour nommer l'attestation : CIN et
     * filiere y figurent, comme sur l'imprime que remettent les
     * entreprises. Interdit au role ETUDIANT, donc appel au mieux.
     */
    @GetMapping("/students/{id}")
    Ref studentById(@PathVariable("id") Long id, @RequestHeader("Authorization") String bearer);

    record Ref(Long id, Long userId, String firstName, String lastName, String email,
               String cin, String classe, String institutionName) {
        public String fullName() { return firstName + " " + lastName; }
    }
}
