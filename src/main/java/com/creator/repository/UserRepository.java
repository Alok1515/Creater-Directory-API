package com.creator.repository;

import com.creator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for User entities.
 *
 * This repository is intentionally kept simple — tenant scoping for users is
 * handled by TenantContextFilter (which uses this repo to resolve the caller)
 * and by service-layer checks (e.g., only returning users within the caller's
 * own agency).
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find all users belonging to a specific agency.
     * Used by GET /users to list only the caller's own agency's users.
     */
    List<User> findByAgencyId(String agencyId);

    /**
     * Check if a user with the given email already exists.
     * Used to prevent duplicate invitations.
     */
    boolean existsByEmail(String email);
}
