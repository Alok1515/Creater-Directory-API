package com.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for linking an existing creator to the caller's agency
 * (POST /creators/:id/link).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LinkCreatorRequest {

    /** Agency-specific notes about this creator. Optional. */
    private String notes;
}
