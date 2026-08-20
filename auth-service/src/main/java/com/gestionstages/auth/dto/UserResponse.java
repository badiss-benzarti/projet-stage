package com.gestionstages.auth.dto;

import com.gestionstages.auth.entity.User;
import com.gestionstages.auth.enums.Role;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    Role role,
    boolean enabled
) {
    public static UserResponse from(User u) {
        return new UserResponse(
            u.getId(), u.getEmail(), u.getFirstName(),
            u.getLastName(), u.getRole(), u.isEnabled()
        );
    }
}
