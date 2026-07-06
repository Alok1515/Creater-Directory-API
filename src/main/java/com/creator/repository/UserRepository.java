package com.creator.repository;

import com.creator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
