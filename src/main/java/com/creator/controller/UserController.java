package com.creator.controller;

import com.creator.dto.InviteUserRequest;
import com.creator.dto.UserDto;
import com.creator.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for User operations.
 *
 * Endpoints:
 *   GET  /users — list all users in the caller's own agency.
 *   POST /users — invite a new user to the caller's agency (owner/admin only).
 *
 * Tenant isolation is handled by UserService, which reads the caller's
 * agencyId from TenantContext. Role-based access is enforced in the service layer.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * List all users in the caller's agency.
     * Any role (owner, admin, member) can view the user list.
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        log.debug("GET /users");
        List<UserDto> users = userService.listUsersForCurrentAgency();
        return ResponseEntity.ok(users);
    }

    /**
     * Invite a new user to the caller's agency.
     * Only 'owner' and 'admin' roles may invoke this — 'member' gets 403.
     */
    @PostMapping
    public ResponseEntity<UserDto> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        log.debug("POST /users — email: {}, role: {}", request.getEmail(), request.getRole());
        UserDto created = userService.inviteUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
