package com.creator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents the link between an Agency and a Creator.
 *
 * This is the most critical entity for tenant isolation. Instead of embedding
 * agencyLinks as a nested array inside Creator (as the API response suggests),
 * we model each link as a separate row in a join table. This design provides:
 *
 * 1. STRUCTURAL ISOLATION — Every query that finds "creators for an agency"
 *    joins through this table filtered by agencyId. A future engineer cannot
 *    accidentally bypass this because the Creator entity itself has no agency
 *    information at all; the link is the only path.
 *
 * 2. PRIVATE NOTES — Each agency stores its own notes about a creator here.
 *    Since the filter always scopes to the caller's agencyId, one agency can
 *    never read another agency's notes — even on a shared creator.
 *
 * 3. PLAN ENFORCEMENT — Counting how many creators an agency has linked to
 *    is a simple COUNT on this table filtered by agencyId.
 *
 * The unique constraint on (agencyId, creatorId) prevents duplicate links.
 */
@Entity
@Table(
    name = "agency_creator_links",
    uniqueConstraints = @UniqueConstraint(columnNames = {"agency_id", "creator_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyCreatorLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agency_id", nullable = false)
    private String agencyId;

    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    /**
     * Agency-specific private notes about this creator.
     * Each agency sees only its own notes — never another agency's.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;
}
