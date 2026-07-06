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
 * Represents a user belonging to exactly one Agency.
 *
 * The User entity is central to tenant resolution: when a request arrives with
 * an X-User-Id header, the system looks up this entity to discover which agency
 * the caller belongs to and what role they hold. That information then flows
 * into TenantContext, scoping every downstream operation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "agency_id", nullable = false)
    private String agencyId;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * One of: owner, admin, member.
     * Drives role-based access control enforced throughout the API.
     */
    @Column(nullable = false)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
