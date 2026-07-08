package com.creator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for inviting a new user to the caller's agency.
 * Validates that the email is well-formed and the role is one of the
 * allowed values (admin or member — owners cannot be invited, only
 * the initial owner exists from setup).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InviteUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(admin|member)$", message = "Role must be 'admin' or 'member'")
    private String role;
}
