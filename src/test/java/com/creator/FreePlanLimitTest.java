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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying FREE PLAN LIMIT enforcement.
 *
 * These tests prove:
 * (a) A free-plan agency can link up to 5 creators successfully.
 * (b) The 6th POST /creators attempt is rejected with HTTP 402.
 * (c) The 6th POST /creators/:id/link attempt is also rejected with HTTP 402.
 * (d) A pro-plan agency has no limit and can link more than 5 creators.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FreePlanLimitTest {

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

        // Free-plan agency with an owner
        agencyRepository.save(Agency.builder()
                .id("a1").name("Nova Talent").plan("free").createdAt(Instant.now()).build());
        userRepository.save(User.builder()
                .id("u1").agencyId("a1").email("owner@nova.com").role("owner").createdAt(Instant.now()).build());

        // Pro-plan agency with an owner
        agencyRepository.save(Agency.builder()
                .id("a2").name("Bright Star Agency").plan("pro").createdAt(Instant.now()).build());
        userRepository.save(User.builder()
                .id("u3").agencyId("a2").email("owner@brightstar.com").role("owner").createdAt(Instant.now()).build());

        // Pre-seed 5 creators linked to the free-plan agency (a1) — at the limit
        for (int i = 1; i <= 5; i++) {
            String creatorId = "limit-c" + i;
            creatorRepository.save(Creator.builder()
                    .id(creatorId).name("Creator " + i).niche("test")
                    .followerCount(1000L * i).engagementRate(1.0 + i).email("creator" + i + "@test.com")
                    .build());
            linkRepository.save(AgencyCreatorLink.builder()
                    .agencyId("a1").creatorId(creatorId)
                    .notes("Test note " + i).addedAt(Instant.now()).build());
        }
    }

    // =========================================================================
    // (a) Free-plan: 6th POST /creators is rejected with 402
    // =========================================================================

    @Test
    @DisplayName("Free plan: 6th POST /creators returns 402 Payment Required")
    void freePlan_sixthCreator_returns402() throws Exception {
        mockMvc.perform(post("/creators")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sixth Creator\", \"niche\": \"gaming\", " +
                                "\"followerCount\": 5000, \"engagementRate\": 4.5, " +
                                "\"email\": \"sixth@test.com\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message", containsString("Free plan limit reached")));
    }

    // =========================================================================
    // (b) Free-plan: 6th POST /creators/:id/link is also rejected with 402
    // =========================================================================

    @Test
    @DisplayName("Free plan: 6th POST /creators/:id/link returns 402 Payment Required")
    void freePlan_sixthLink_returns402() throws Exception {
        // Create an unlinked creator for the link attempt
        creatorRepository.save(Creator.builder()
                .id("extra-c").name("Extra Creator").niche("music")
                .followerCount(50000L).engagementRate(5.0).email("extra@test.com")
                .build());

        mockMvc.perform(post("/creators/extra-c/link")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\": \"Trying to exceed limit\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message", containsString("Free plan limit reached")));
    }

    // =========================================================================
    // (c) Pro-plan: no limit — can link more than 5 creators
    // =========================================================================

    @Test
    @DisplayName("Pro plan: can create more than 5 creators without hitting a limit")
    void proPlan_noLimit() throws Exception {
        // Link 5 creators to pro agency first
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/creators")
                            .header("X-User-Id", "u3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Pro Creator " + i + "\", \"niche\": \"tech\", " +
                                    "\"followerCount\": " + (i * 1000) + ", \"engagementRate\": 3.0, " +
                                    "\"email\": \"pro" + i + "@test.com\"}"))
                    .andExpect(status().isCreated());
        }

        // The 6th should also succeed for pro plan
        mockMvc.perform(post("/creators")
                        .header("X-User-Id", "u3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Pro Creator 6\", \"niche\": \"tech\", " +
                                "\"followerCount\": 6000, \"engagementRate\": 3.0, " +
                                "\"email\": \"pro6@test.com\"}"))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // (d) Free-plan: first 5 succeed
    // =========================================================================

    @Test
    @DisplayName("Free plan: creating creators 1-5 all succeed (verifying limit is exactly 5)")
    void freePlan_firstFive_succeed() throws Exception {
        // Clear existing links so we start fresh
        linkRepository.deleteAll();
        creatorRepository.deleteAll();

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/creators")
                            .header("X-User-Id", "u1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Fresh Creator " + i + "\", \"niche\": \"food\", " +
                                    "\"followerCount\": " + (i * 100) + ", \"engagementRate\": 2.0, " +
                                    "\"email\": \"fresh" + i + "@test.com\"}"))
                    .andExpect(status().isCreated());
        }

        // The 6th should fail
        mockMvc.perform(post("/creators")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Fresh Creator 6\", \"niche\": \"food\", " +
                                "\"followerCount\": 600, \"engagementRate\": 2.0, " +
                                "\"email\": \"fresh6@test.com\"}"))
                .andExpect(status().isPaymentRequired());
    }
}
