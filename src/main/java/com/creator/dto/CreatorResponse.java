package com.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for Creator entities.
 *
 * The agencyLinks list will only ever contain a SINGLE entry — the calling
 * agency's own link data. This is the key tenant isolation guarantee in the
 * API response layer: even though a creator may be linked to multiple agencies,
 * each caller only sees their own notes and addedAt timestamp.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorResponse {

    private String id;
    private String name;
    private String niche;
    private Long followerCount;
    private Double engagementRate;
    private String email;
    private List<AgencyLinkDto> agencyLinks;
}
