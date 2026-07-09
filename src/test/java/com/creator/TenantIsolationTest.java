package com.creator;

import com.creator.model.Agency;
import com.creator.model.AgencyCreatorLink;
import com.creator.model.Creator;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests proving TENANT ISOLATION holds.
 *
 * These tests verify the core multi-tenancy guarantees:
 * (a) Agency A cannot read Agency B's private notes on a shared creator.
 * (b) Agency A cannot see or modify a creator it has no link to.
 * (c) Unlinked agencies get 404 (not 403) — resource existence is hidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationTest {

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

        // Agency A (Nova Talent)
        agencyRepository.save(Agency.builder()
                .id("a1").name("Nova Talent").plan("free").createdAt(Instant.now()).build());
        // Agency B (Bright Star)
        agencyRepository.save(Agency.builder()
                .id("a2").name("Bright Star Agency").plan("pro").createdAt(Instant.now()).build());
        // Agency C (Solo Creators — no linked creators)
        agencyRepository.save(Agency.builder()
                .id("a3").name("Solo Creators Co").plan("free").createdAt(Instant.now()).build());

        // Users
        userRepository.save(User.builder()
                .id("u1").agencyId("a1").email("owner@nova.com").role("owner").createdAt(Instant.now()).build());
        userRepository.save(User.builder()
                .id("u3").agencyId("a2").email("owner@brightstar.com").role("owner").createdAt(Instant.now()).build());
        userRepository.save(User.builder()
                .id("u4").agencyId("a3").email("owner@solo.com").role("owner").createdAt(Instant.now()).build());

        // Shared creator c1 — linked to both a1 and a2 with DIFFERENT notes
        creatorRepository.save(Creator.builder()
                .id("c1").name("Priya Sharma").niche("beauty")
                .followerCount(45000L).engagementRate(3.8).email("priya@example.com").build());

        linkRepository.save(AgencyCreatorLink.builder()
                .agencyId("a1").creatorId("c1")
                .notes("Great for skincare campaigns").addedAt(Instant.now()).build());
        linkRepository.save(AgencyCreatorLink.builder()
                .agencyId("a2").creatorId("c1")
                .notes("Booked for Q1 shoot").addedAt(Instant.now()).build());

        // Creator c2 — linked only to a2
        creatorRepository.save(Creator.builder()
                .id("c2").name("Rahul Verma").niche("fitness")
                .followerCount(120000L).engagementRate(2.1).email("rahul@example.com").build());

        linkRepository.save(AgencyCreatorLink.builder()
                .agencyId("a2").creatorId("c2")
                .notes("High reach, slower replies").addedAt(Instant.now()).build());

        // Creator c3 — linked only to a1
        creatorRepository.save(Creator.builder()
                .id("c3").name("Ananya Iyer").niche("travel")
                .followerCount(8000L).engagementRate(6.4).email("ananya@example.com").build());

        linkRepository.save(AgencyCreatorLink.builder()
                .agencyId("a1").creatorId("c3")
                .notes("Micro-influencer, very responsive").addedAt(Instant.now()).build());
    }

    // =========================================================================
    // (a) Agency A cannot read Agency B's private notes on a shared creator
    // =========================================================================

    @Test
    @DisplayName("Agency A sees only its own notes on shared creator c1")
    void agencyA_seesOwnNotes_onSharedCreator() throws Exception {
        mockMvc.perform(get("/creators/c1").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("c1")))
                .andExpect(jsonPath("$.agencyLinks", hasSize(1)))
                .andExpect(jsonPath("$.agencyLinks[0].agencyId", is("a1")))
                .andExpect(jsonPath("$.agencyLinks[0].notes", is("Great for skincare campaigns")));
    }

    @Test
    @DisplayName("Agency B sees only its own notes on shared creator c1")
    void agencyB_seesOwnNotes_onSharedCreator() throws Exception {
        mockMvc.perform(get("/creators/c1").header("X-User-Id", "u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("c1")))
                .andExpect(jsonPath("$.agencyLinks", hasSize(1)))
                .andExpect(jsonPath("$.agencyLinks[0].agencyId", is("a2")))
                .andExpect(jsonPath("$.agencyLinks[0].notes", is("Booked for Q1 shoot")));
    }

    @Test
    @DisplayName("Agency A's notes never appear in Agency B's response for shared creator")
    void agencyB_neverSeesAgencyA_notes() throws Exception {
        mockMvc.perform(get("/creators/c1").header("X-User-Id", "u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyLinks[0].notes", not(is("Great for skincare campaigns"))));
    }

    // =========================================================================
    // (b) Agency cannot see or modify a creator it has no link to
    // =========================================================================

    @Test
    @DisplayName("Agency A gets 404 for creator c2 (linked only to Agency B)")
    void agencyA_gets404_forUnlinkedCreator() throws Exception {
        mockMvc.perform(get("/creators/c2").header("X-User-Id", "u1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Agency B gets 404 for creator c3 (linked only to Agency A)")
    void agencyB_gets404_forUnlinkedCreator() throws Exception {
        mockMvc.perform(get("/creators/c3").header("X-User-Id", "u3"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Agency C (no creators) gets 404 for any creator")
    void agencyC_gets404_forAnyCreator() throws Exception {
        mockMvc.perform(get("/creators/c1").header("X-User-Id", "u4"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/creators/c2").header("X-User-Id", "u4"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Agency A cannot PATCH a creator it has no link to")
    void agencyA_cannotPatch_unlinkedCreator() throws Exception {
        mockMvc.perform(patch("/creators/c2")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\": \"hacked notes\"}"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Listing only shows linked creators
    // =========================================================================

    @Test
    @DisplayName("Agency A listing shows only c1 and c3 (its linked creators)")
    void agencyA_listShowsOnlyLinkedCreators() throws Exception {
        mockMvc.perform(get("/creators").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Agency B listing shows only c1 and c2 (its linked creators)")
    void agencyB_listShowsOnlyLinkedCreators() throws Exception {
        mockMvc.perform(get("/creators").header("X-User-Id", "u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Agency C listing returns empty (no linked creators)")
    void agencyC_listReturnsEmpty() throws Exception {
        mockMvc.perform(get("/creators").header("X-User-Id", "u4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // =========================================================================
    // Missing or invalid X-User-Id header
    // =========================================================================

    @Test
    @DisplayName("Request without X-User-Id header returns 401")
    void missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/creators"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with unknown X-User-Id returns 401")
    void unknownUserId_returns401() throws Exception {
        mockMvc.perform(get("/creators").header("X-User-Id", "nonexistent"))
                .andExpect(status().isUnauthorized());
    }
}
