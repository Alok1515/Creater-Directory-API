package com.creator.repository;

import com.creator.model.AgencyCreatorLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for AgencyCreatorLink — the tenant isolation gateway.
 *
 * This is the MOST IMPORTANT repository for multi-tenancy. Every method here
 * requires an agencyId parameter, ensuring that queries are always scoped to
 * a single tenant. There is deliberately no method that returns links across
 * all agencies — this makes it structurally hard to accidentally leak data.
 *
 * Usage pattern in services:
 * 1. Use this repository to find which creators the calling agency is linked to.
 * 2. Only then fetch the shared Creator data for those IDs via CreatorRepository.
 * 3. Never bypass this repository when accessing creator data.
 */
@Repository
public interface AgencyCreatorLinkRepository extends JpaRepository<AgencyCreatorLink, Long> {

    /**
     * Find all links for a specific agency.
     * Used by GET /creators to discover which creators are visible to this tenant.
     */
    List<AgencyCreatorLink> findByAgencyId(String agencyId);

    /**
     * Find a specific link between an agency and a creator.
     * Used by GET /creators/:id to verify the caller has access, and by
     * PATCH/DELETE to locate the caller's own link record.
     */
    Optional<AgencyCreatorLink> findByAgencyIdAndCreatorId(String agencyId, String creatorId);

    /**
     * Check if a link exists between an agency and a creator.
     * Used for quick existence checks before creating duplicate links.
     */
    boolean existsByAgencyIdAndCreatorId(String agencyId, String creatorId);

    /**
     * Count how many creators an agency is linked to.
     * Used for free-plan enforcement (max 5 creators).
     */
    long countByAgencyId(String agencyId);

    /**
     * Delete the link between an agency and a creator.
     * Used by DELETE /creators/:id to unlink the caller's agency only.
     */
    void deleteByAgencyIdAndCreatorId(String agencyId, String creatorId);

    /**
     * Check if any agency is still linked to a given creator.
     * Used after unlinking to decide whether to delete the creator globally.
     */
    boolean existsByCreatorId(String creatorId);
}
