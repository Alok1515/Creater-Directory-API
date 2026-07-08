package com.creator.repository;

import com.creator.model.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Data access layer for Creator entities.
 *
 * Extends JpaSpecificationExecutor to support dynamic filtering (niche,
 * follower range) and sorting via Spring Data Specifications, which will
 * be used in the GET /creators listing endpoint.
 *
 * IMPORTANT: This repository accesses the shared Creator table directly.
 * All tenant-scoped access must go through AgencyCreatorLinkRepository first
 * to find which creator IDs the calling agency is linked to, and then use
 * this repository only for those IDs. Never query this repository without
 * first filtering through the link table.
 */
@Repository
public interface CreatorRepository extends JpaRepository<Creator, String>, JpaSpecificationExecutor<Creator> {
}
