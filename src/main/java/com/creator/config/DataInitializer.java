package com.creator.config;

import com.creator.model.Agency;
import com.creator.model.AgencyCreatorLink;
import com.creator.model.Creator;
import com.creator.model.User;
import com.creator.repository.AgencyCreatorLinkRepository;
import com.creator.repository.AgencyRepository;
import com.creator.repository.CreatorRepository;
import com.creator.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;

/**
 * Loads seed data from seed-data.json into the H2 database on application startup.
 *
 * This component runs once when the application boots, parsing the JSON fixture
 * provided in the challenge spec and persisting it into the normalized relational
 * schema (agencies, users, creators, agency_creator_links).
 *
 * The seed data demonstrates the multi-tenant scenario:
 * - Creator c1 (Priya Sharma) is linked to both agency a1 and a2, each with
 *   their own private notes.
 * - Creator c2 is linked only to a2, and c3 only to a1.
 * - Agency a3 has no creators linked at all.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final CreatorRepository creatorRepository;
    private final AgencyCreatorLinkRepository linkRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(AgencyRepository agencyRepository,
                           UserRepository userRepository,
                           CreatorRepository creatorRepository,
                           AgencyCreatorLinkRepository linkRepository,
                           ObjectMapper objectMapper) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.creatorRepository = creatorRepository;
        this.linkRepository = linkRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the database is empty (avoids duplicates on restart)
        if (agencyRepository.count() > 0) {
            log.info("Database already seeded — skipping initialization.");
            return;
        }

        log.info("Seeding database from seed-data.json...");

        InputStream inputStream = new ClassPathResource("seed-data.json").getInputStream();
        JsonNode root = objectMapper.readTree(inputStream);

        // --- Seed Agencies ---
        JsonNode agenciesNode = root.get("agencies");
        for (JsonNode node : agenciesNode) {
            Agency agency = Agency.builder()
                    .id(node.get("id").asText())
                    .name(node.get("name").asText())
                    .plan(node.get("plan").asText())
                    .createdAt(Instant.now())
                    .build();
            agencyRepository.save(agency);
            log.debug("Seeded agency: {} ({})", agency.getName(), agency.getId());
        }

        // --- Seed Users ---
        JsonNode usersNode = root.get("users");
        for (JsonNode node : usersNode) {
            User user = User.builder()
                    .id(node.get("id").asText())
                    .agencyId(node.get("agencyId").asText())
                    .email(node.get("email").asText())
                    .role(node.get("role").asText())
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(user);
            log.debug("Seeded user: {} ({})", user.getEmail(), user.getId());
        }

        // --- Seed Creators and their AgencyCreatorLinks ---
        JsonNode creatorsNode = root.get("creators");
        for (JsonNode node : creatorsNode) {
            Creator creator = Creator.builder()
                    .id(node.get("id").asText())
                    .name(node.get("name").asText())
                    .niche(node.get("niche").asText())
                    .followerCount(node.get("followerCount").asLong())
                    .engagementRate(node.get("engagementRate").asDouble())
                    .email(node.get("email").asText())
                    .build();
            creatorRepository.save(creator);
            log.debug("Seeded creator: {} ({})", creator.getName(), creator.getId());

            // Parse and persist each agency link for this creator
            JsonNode linksNode = node.get("agencyLinks");
            if (linksNode != null && linksNode.isArray()) {
                for (JsonNode linkNode : linksNode) {
                    AgencyCreatorLink link = AgencyCreatorLink.builder()
                            .agencyId(linkNode.get("agencyId").asText())
                            .creatorId(creator.getId())
                            .notes(linkNode.get("notes").asText())
                            .addedAt(Instant.parse(linkNode.get("addedAt").asText()))
                            .build();
                    linkRepository.save(link);
                    log.debug("  Linked creator {} to agency {} with notes: '{}'",
                            creator.getId(), link.getAgencyId(), link.getNotes());
                }
            }
        }

        log.info("Database seeding complete — {} agencies, {} users, {} creators, {} links.",
                agencyRepository.count(), userRepository.count(),
                creatorRepository.count(), linkRepository.count());
    }
}
