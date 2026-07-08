package com.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Response DTO for User entities.
 * Exposes user data without leaking internal entity details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private String id;
    private String agencyId;
    private String email;
    private String role;
    private Instant createdAt;
}
