package com.gestionstages.document.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/students/me")
    Ref myStudentProfile(@RequestHeader("Authorization") String bearer);

    record Ref(Long id, Long userId, String firstName, String lastName, String email) {
        public String fullName() { return firstName + " " + lastName; }
    }
}
