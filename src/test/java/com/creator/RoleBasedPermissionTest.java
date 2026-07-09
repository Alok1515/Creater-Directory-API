package com.creator;

import com.creator.model.Agency;
import com.creator.model.User;
import com.creator.repository.AgencyCreatorLinkRepository;
import com.creator.repository.AgencyRepository;
import com.creator.repository.CreatorRepository;
import com.creator.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying ROLE-BASED PERMISSIONS.
 *
 * These tests prove:
 * (a) A 'member' role cannot invite users (gets 403).
 * (b) An 'owner' can invite users successfully.
 * (c) An 'admin' can invite users successfully.
 * (d) Any role can list users in their own agency.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleBasedPermissionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CreatorRepository creatorRepository;
    @Autowired private AgencyCreatorLinkRepository linkRepository;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        creatorRepository.deleteAll();
        userRepository.deleteAll();
        agencyRepository.deleteAll();

        // Agency A with owner, admin, and member
        agencyRepository.save(Agency.builder()
                .id("a1").name("Nova Talent").plan("free").createdAt(Instant.now()).build());

        userRepository.save(User.builder()
                .id("u1").agencyId("a1").email("owner@nova.com").role("owner").createdAt(Instant.now()).build());
        userRepository.save(User.builder()
                .id("u2").agencyId("a1").email("admin@nova.com").role("admin").createdAt(Instant.now()).build());
        userRepository.save(User.builder()
                .id("u5").agencyId("a1").email("member@nova.com").role("member").createdAt(Instant.now()).build());
    }

    // =========================================================================
    // (a) Member CANNOT invite users — 403 Forbidden
    // =========================================================================

    @Test
    @DisplayName("Member role gets 403 when trying to invite a user")
    void member_cannotInviteUser() throws Exception {
        mockMvc.perform(post("/users")
                        .header("X-User-Id", "u5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"newuser@nova.com\", \"role\": \"member\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // (b) Owner CAN invite users — 201 Created
    // =========================================================================

    @Test
    @DisplayName("Owner role can invite a new user successfully")
    void owner_canInviteUser() throws Exception {
        mockMvc.perform(post("/users")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"newuser1@nova.com\", \"role\": \"member\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser1@nova.com"))
                .andExpect(jsonPath("$.role").value("member"))
                .andExpect(jsonPath("$.agencyId").value("a1"));
    }

    // =========================================================================
    // (c) Admin CAN invite users — 201 Created
    // =========================================================================

    @Test
    @DisplayName("Admin role can invite a new user successfully")
    void admin_canInviteUser() throws Exception {
        mockMvc.perform(post("/users")
                        .header("X-User-Id", "u2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"newuser2@nova.com\", \"role\": \"admin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser2@nova.com"))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.agencyId").value("a1"));
    }

    // =========================================================================
    // (d) Any role can list users
    // =========================================================================

    @Test
    @DisplayName("Member role can list users in their agency")
    void member_canListUsers() throws Exception {
        mockMvc.perform(get("/users").header("X-User-Id", "u5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Owner role can list users in their agency")
    void owner_canListUsers() throws Exception {
        mockMvc.perform(get("/users").header("X-User-Id", "u1"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Validation: invalid role in invite request
    // =========================================================================

    @Test
    @DisplayName("Invite with invalid role returns 400")
    void invite_withInvalidRole_returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"bad@nova.com\", \"role\": \"superadmin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Invite with duplicate email returns 409")
    void invite_withDuplicateEmail_returns409() throws Exception {
        mockMvc.perform(post("/users")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"owner@nova.com\", \"role\": \"member\"}"))
                .andExpect(status().isConflict());
    }
}
