package com.creator.service;

import com.creator.dto.InviteUserRequest;
import com.creator.dto.UserDto;
import com.creator.exception.DuplicateResourceException;
import com.creator.exception.ForbiddenException;
import com.creator.model.User;
import com.creator.repository.UserRepository;
import com.creator.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for User operations.
 *
 * All operations are scoped to the current tenant (agency) via TenantContext.
 * Role-based access control is enforced here at the service level:
 * - Only 'owner' and 'admin' roles may invite new users.
 * - 'member' role attempting to invite gets a 403.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * List all users belonging to the caller's agency.
     * Tenant isolation: only returns users with the same agencyId as the caller.
     */
    @Transactional(readOnly = true)
    public List<UserDto> listUsersForCurrentAgency() {
        String agencyId = TenantContext.getCurrentAgencyId();
        log.debug("Listing users for agency: {}", agencyId);

        List<User> users = userRepository.findByAgencyId(agencyId);
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Invite a new user to the caller's agency.
     *
     * Authorization rules:
     * - 'owner' and 'admin' can invite users.
     * - 'member' cannot invite — throws ForbiddenException (403).
     * - Duplicate emails are rejected with DuplicateResourceException (409).
     * - New users are always assigned to the caller's own agency (tenant isolation).
     */
    @Transactional
    public UserDto inviteUser(InviteUserRequest request) {
        String callerRole = TenantContext.getCurrentUserRole();
        String agencyId = TenantContext.getCurrentAgencyId();

        // RBAC check: only owner and admin may invite
        if ("member".equals(callerRole)) {
            throw new ForbiddenException("invite users", callerRole);
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists");
        }

        // Create the new user, scoped to the caller's agency
        User newUser = User.builder()
                .id(UUID.randomUUID().toString())
                .agencyId(agencyId)
                .email(request.getEmail())
                .role(request.getRole())
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(newUser);
        log.info("User '{}' invited to agency '{}' with role '{}'",
                saved.getEmail(), agencyId, saved.getRole());

        return toDto(saved);
    }

    /**
     * Maps a User entity to a UserDto for API response.
     */
    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .agencyId(user.getAgencyId())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
