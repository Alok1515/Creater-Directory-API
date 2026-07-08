package com.creator.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for updating a creator (PATCH /creators/:id).
 *
 * Design decision: shared fields (name, niche, followerCount, engagementRate, email)
 * and the caller's private notes are all accepted in a single PATCH request.
 * - Shared fields update the Creator entity (visible to all linked agencies).
 * - The 'notes' field updates only the caller's own AgencyCreatorLink record.
 * - All fields are optional — only non-null fields are applied.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCreatorRequest {

    private String name;

    private String niche;

    @Min(value = 0, message = "Follower count must be non-negative")
    private Long followerCount;

    @Min(value = 0, message = "Engagement rate must be between 0 and 100")
    @Max(value = 100, message = "Engagement rate must be between 0 and 100")
    private Double engagementRate;

    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * Updates the caller's own agency-specific notes about this creator.
     * This ONLY modifies the caller's AgencyCreatorLink.notes — never another
     * agency's notes.
     */
    private String notes;
}
