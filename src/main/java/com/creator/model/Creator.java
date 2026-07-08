package com.creator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a Creator (influencer) — the core shared resource in the system.
 *
 * A Creator's profile fields (name, niche, followerCount, engagementRate, email)
 * are shared across all agencies that have linked to this creator. However, each
 * agency's private relationship data (notes, addedAt) is stored separately in the
 * {@link AgencyCreatorLink} join entity.
 *
 * This separation is a deliberate design choice for tenant isolation:
 * - Shared fields live here on Creator and can be updated by any linked agency.
 * - Per-agency private data lives in AgencyCreatorLink and is always filtered
 *   by the caller's agencyId, making cross-tenant data leakage structurally
 *   impossible at the query level.
 */
@Entity
@Table(name = "creators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String niche;

    @Column(name = "follower_count", nullable = false)
    private Long followerCount;

    @Column(name = "engagement_rate", nullable = false)
    private Double engagementRate;

    @Column(nullable = false)
    private String email;
}
