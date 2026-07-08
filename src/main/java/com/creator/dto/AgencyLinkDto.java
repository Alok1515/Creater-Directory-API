package com.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents the caller's own agency link data for a creator.
 * This DTO is embedded inside CreatorResponse and only ever contains
 * the calling agency's notes — never another agency's.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyLinkDto {

    private String agencyId;
    private String notes;
    private Instant addedAt;
}
