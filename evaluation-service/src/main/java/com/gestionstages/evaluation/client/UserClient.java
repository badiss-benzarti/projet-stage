package com.gestionstages.evaluation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/** Resolution du profil metier du porteur du jeton. */
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/students/me")
    Ref myStudentProfile(@RequestHeader("Authorization") String bearer);

    @GetMapping("/supervisors/me")
    Ref mySupervisorProfile(@RequestHeader("Authorization") String bearer);

    record Ref(Long id, Long userId, String firstName, String lastName) {
        public String fullName() { return firstName + " " + lastName; }
    }
}
