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
 * Represents an Agency — the tenant in this multi-tenant system.
 *
 * Each Agency has a billing plan (free or pro) which governs how many Creators
 * it may link to. The free plan is capped at 5 creators; pro has no limit.
 *
 * All data isolation in this system is anchored to the Agency: a User belongs
 * to exactly one Agency, and every Creator query is scoped through the
 * AgencyCreatorLink join table filtered by the caller's agencyId.
 */
@Entity
@Table(name = "agencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agency {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    /**
     * Billing plan: "free" or "pro".
     * Free plan agencies are limited to 5 creator links.
     * Only an owner-role user may change this field.
     */
    @Column(nullable = false)
    private String plan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
