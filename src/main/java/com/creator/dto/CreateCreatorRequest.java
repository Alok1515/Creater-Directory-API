package com.creator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a new Creator (POST /creators).
 * The creator is automatically linked to the caller's agency upon creation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCreatorRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Niche is required")
    private String niche;

    @NotNull(message = "Follower count is required")
    @Min(value = 0, message = "Follower count must be non-negative")
    private Long followerCount;

    @NotNull(message = "Engagement rate is required")
    @Min(value = 0, message = "Engagement rate must be between 0 and 100")
    @Max(value = 100, message = "Engagement rate must be between 0 and 100")
    private Double engagementRate;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /** Optional notes to attach when linking the creator to the caller's agency. */
    private String notes;
}
