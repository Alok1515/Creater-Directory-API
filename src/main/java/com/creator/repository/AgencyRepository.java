package com.creator.repository;

import com.creator.model.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access layer for Agency (tenant) entities.
 *
 * Used during seed data loading and for plan-related operations
 * (e.g., checking if an agency is on the free or pro plan before
 * allowing new creator links).
 */
@Repository
public interface AgencyRepository extends JpaRepository<Agency, String> {
}
