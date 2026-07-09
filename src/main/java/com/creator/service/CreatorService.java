package com.creator.service;

import com.creator.dto.AgencyLinkDto;
import com.creator.dto.CreateCreatorRequest;
import com.creator.dto.CreatorResponse;
import com.creator.dto.LinkCreatorRequest;
import com.creator.dto.PaginatedResponse;
import com.creator.exception.DuplicateResourceException;
import com.creator.exception.PlanLimitExceededException;
import com.creator.exception.ResourceNotFoundException;
import com.creator.model.Agency;
import com.creator.model.AgencyCreatorLink;
import com.creator.model.Creator;
import com.creator.repository.AgencyCreatorLinkRepository;
import com.creator.repository.AgencyRepository;
import com.creator.repository.CreatorRepository;
import com.creator.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Creator operations.
 *
 * All operations are scoped to the caller's agency via TenantContext.
 * The core isolation pattern:
 * 1. Query AgencyCreatorLinkRepository (filtered by agencyId) to find which
 *    creators the caller's agency is linked to.
 * 2. Only then fetch or modify Creator records for those IDs.
 * 3. Response DTOs include only the caller's own agencyLink — never another
 *    agency's notes.
 */
@Service
public class CreatorService {

    private static final Logger log = LoggerFactory.getLogger(CreatorService.class);

    private static final int FREE_PLAN_CREATOR_LIMIT = 5;

    private final CreatorRepository creatorRepository;
    private final AgencyCreatorLinkRepository linkRepository;
    private final AgencyRepository agencyRepository;

    public CreatorService(CreatorRepository creatorRepository,
                          AgencyCreatorLinkRepository linkRepository,
                          AgencyRepository agencyRepository) {
        this.creatorRepository = creatorRepository;
        this.linkRepository = linkRepository;
        this.agencyRepository = agencyRepository;
    }

    /**
     * List creators visible to the caller's agency (basic, unfiltered version).
     *
     * Tenant isolation:
     * - Step 1: Find all AgencyCreatorLinks for the caller's agencyId.
     * - Step 2: Fetch the Creator records for those linked creatorIds.
     * - Step 3: Build responses with only the caller's own link data.
     */
    @Transactional(readOnly = true)
    public List<CreatorResponse> listCreators() {
        String agencyId = TenantContext.getCurrentAgencyId();
        log.debug("Listing creators for agency: {}", agencyId);

        // Step 1: Find all links for this agency
        List<AgencyCreatorLink> links = linkRepository.findByAgencyId(agencyId);

        if (links.isEmpty()) {
            return Collections.emptyList();
        }

        // Build a map of creatorId -> link for quick lookup
        Map<String, AgencyCreatorLink> linkMap = links.stream()
                .collect(Collectors.toMap(AgencyCreatorLink::getCreatorId, link -> link));

        // Step 2: Fetch the creator records
        List<String> creatorIds = links.stream()
                .map(AgencyCreatorLink::getCreatorId)
                .collect(Collectors.toList());

        List<Creator> creators = creatorRepository.findAllById(creatorIds);

        // Step 3: Build responses with only the caller's own link
        return creators.stream()
                .map(creator -> toResponse(creator, linkMap.get(creator.getId())))
                .collect(Collectors.toList());
    }

    /**
     * List creators with pagination, filtering, and sorting.
     *
     * Tenant isolation flow:
     * 1. Find all creatorIds linked to the caller's agency.
     * 2. Build a JPA Specification that filters Creator records by those IDs,
     *    plus optional niche and follower range filters.
     * 3. Apply sorting and pagination.
     * 4. Build responses with only the caller's own link data.
     *
     * @param page           zero-based page number (default 0)
     * @param limit          page size (default 10)
     * @param niche          optional niche filter (exact match, case-insensitive)
     * @param minFollowers   optional minimum follower count
     * @param maxFollowers   optional maximum follower count
     * @param sortBy         field to sort by (default "followerCount")
     * @param order          sort direction: "asc" or "desc" (default "desc")
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<CreatorResponse> listCreatorsFiltered(
            int page, int limit,
            String niche, Long minFollowers, Long maxFollowers,
            String sortBy, String order) {

        String agencyId = TenantContext.getCurrentAgencyId();
        log.debug("Listing creators (filtered) for agency: {}", agencyId);

        // Step 1: Find all linked creatorIds for this agency
        List<AgencyCreatorLink> links = linkRepository.findByAgencyId(agencyId);

        if (links.isEmpty()) {
            return PaginatedResponse.<CreatorResponse>builder()
                    .data(Collections.emptyList())
                    .page(page)
                    .limit(limit)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        Map<String, AgencyCreatorLink> linkMap = links.stream()
                .collect(Collectors.toMap(AgencyCreatorLink::getCreatorId, link -> link));

        List<String> creatorIds = links.stream()
                .map(AgencyCreatorLink::getCreatorId)
                .collect(Collectors.toList());

        // Step 2: Build dynamic specification for filtering
        Specification<Creator> spec = buildSpecification(creatorIds, niche, minFollowers, maxFollowers);

        // Step 3: Apply sorting and pagination
        Sort sort = buildSort(sortBy, order);
        Pageable pageable = PageRequest.of(page, limit, sort);

        Page<Creator> creatorPage = creatorRepository.findAll(spec, pageable);

        // Step 4: Build responses
        List<CreatorResponse> responses = creatorPage.getContent().stream()
                .map(creator -> toResponse(creator, linkMap.get(creator.getId())))
                .collect(Collectors.toList());

        return PaginatedResponse.<CreatorResponse>builder()
                .data(responses)
                .page(page)
                .limit(limit)
                .totalElements(creatorPage.getTotalElements())
                .totalPages(creatorPage.getTotalPages())
                .build();
    }

    /**
     * Builds a JPA Specification that:
     * - Restricts results to only the given creatorIds (tenant isolation)
     * - Optionally filters by niche (case-insensitive)
     * - Optionally filters by follower count range
     */
    private Specification<Creator> buildSpecification(
            List<String> creatorIds, String niche, Long minFollowers, Long maxFollowers) {

        Specification<Creator> spec = (root, query, cb) -> root.get("id").in(creatorIds);

        if (niche != null && !niche.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("niche")), niche.toLowerCase()));
        }

        if (minFollowers != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("followerCount"), minFollowers));
        }

        if (maxFollowers != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("followerCount"), maxFollowers));
        }

        return spec;
    }

    /**
     * Builds a Sort object from the given field name and direction.
     * Defaults to followerCount DESC if not specified.
     */
    private Sort buildSort(String sortBy, String order) {
        String field = (sortBy != null && !sortBy.isBlank()) ? sortBy : "followerCount";
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    /**
     * Get a single creator by ID — but ONLY if the caller's agency is linked to it.
     *
     * If the creator exists but the caller's agency has no link, we return 404
     * (not 403) to avoid revealing that the record exists to other tenants.
     * This is a deliberate security measure specified in the challenge.
     */
    @Transactional(readOnly = true)
    public CreatorResponse getCreatorById(String creatorId) {
        String agencyId = TenantContext.getCurrentAgencyId();
        log.debug("Getting creator '{}' for agency '{}'", creatorId, agencyId);

        // Check if the caller's agency is linked to this creator
        Optional<AgencyCreatorLink> linkOpt = linkRepository.findByAgencyIdAndCreatorId(agencyId, creatorId);

        if (linkOpt.isEmpty()) {
            // Return 404 — don't reveal whether the creator exists at all
            throw new ResourceNotFoundException("Creator", "id", creatorId);
        }

        // Fetch the creator entity
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator", "id", creatorId));

        return toResponse(creator, linkOpt.get());
    }

    /**
     * Enforces the free-plan creator limit.
     *
     * Free-plan agencies may link to a maximum of 5 creators. If the agency
     * is on the free plan and already has 5 links, this method throws
     * PlanLimitExceededException (HTTP 402).
     *
     * Pro-plan agencies have no limit — this method is a no-op for them.
     */
    public void enforcePlanLimit(String agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", "id", agencyId));

        if ("free".equalsIgnoreCase(agency.getPlan())) {
            long currentLinkCount = linkRepository.countByAgencyId(agencyId);
            if (currentLinkCount >= FREE_PLAN_CREATOR_LIMIT) {
                log.info("Agency '{}' (free plan) has reached the {} creator limit. Current count: {}",
                        agencyId, FREE_PLAN_CREATOR_LIMIT, currentLinkCount);
                throw new PlanLimitExceededException();
            }
        }
    }

    /**
     * Create a new creator and automatically link it to the caller's agency.
     *
     * Steps:
     * 1. Enforce free-plan limit (throws 402 if exceeded).
     * 2. Create and persist the shared Creator entity.
     * 3. Create the AgencyCreatorLink binding the creator to the caller's agency.
     * 4. Return the response with the caller's own link data.
     */
    @Transactional
    public CreatorResponse createCreator(CreateCreatorRequest request) {
        String agencyId = TenantContext.getCurrentAgencyId();
        log.debug("Creating new creator for agency '{}'", agencyId);

        // Step 1: Enforce plan limit
        enforcePlanLimit(agencyId);

        // Step 2: Create the shared creator record
        Creator creator = Creator.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .niche(request.getNiche())
                .followerCount(request.getFollowerCount())
                .engagementRate(request.getEngagementRate())
                .email(request.getEmail())
                .build();
        creator = creatorRepository.save(creator);

        // Step 3: Link to the caller's agency
        AgencyCreatorLink link = AgencyCreatorLink.builder()
                .agencyId(agencyId)
                .creatorId(creator.getId())
                .notes(request.getNotes())
                .addedAt(Instant.now())
                .build();
        link = linkRepository.save(link);

        log.info("Created creator '{}' and linked to agency '{}'", creator.getId(), agencyId);
        return toResponse(creator, link);
    }

    /**
     * Link an existing creator to the caller's agency with optional notes.
     *
     * Steps:
     * 1. Enforce free-plan limit (throws 402 if exceeded).
     * 2. Verify the creator exists (throws 404 if not).
     * 3. Verify no duplicate link exists (throws 409 if already linked).
     * 4. Create the AgencyCreatorLink.
     * 5. Return the response with the caller's own link data.
     */
    @Transactional
    public CreatorResponse linkCreatorToAgency(String creatorId, LinkCreatorRequest request) {
        String agencyId = TenantContext.getCurrentAgencyId();
        log.debug("Linking creator '{}' to agency '{}'", creatorId, agencyId);

        // Step 1: Enforce plan limit
        enforcePlanLimit(agencyId);

        // Step 2: Verify the creator exists
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator", "id", creatorId));

        // Step 3: Check for duplicate link
        if (linkRepository.existsByAgencyIdAndCreatorId(agencyId, creatorId)) {
            throw new DuplicateResourceException(
                    "Creator '" + creatorId + "' is already linked to your agency");
        }

        // Step 4: Create the link
        AgencyCreatorLink link = AgencyCreatorLink.builder()
                .agencyId(agencyId)
                .creatorId(creatorId)
                .notes(request != null ? request.getNotes() : null)
                .addedAt(Instant.now())
                .build();
        link = linkRepository.save(link);

        log.info("Linked existing creator '{}' to agency '{}'", creatorId, agencyId);
        return toResponse(creator, link);
    }

    /**
     * Maps a Creator entity and its AgencyCreatorLink to a response DTO.
     * Only the calling agency's link data is included.
     */
    protected CreatorResponse toResponse(Creator creator, AgencyCreatorLink link) {
        AgencyLinkDto linkDto = null;
        if (link != null) {
            linkDto = AgencyLinkDto.builder()
                    .agencyId(link.getAgencyId())
                    .notes(link.getNotes())
                    .addedAt(link.getAddedAt())
                    .build();
        }

        return CreatorResponse.builder()
                .id(creator.getId())
                .name(creator.getName())
                .niche(creator.getNiche())
                .followerCount(creator.getFollowerCount())
                .engagementRate(creator.getEngagementRate())
                .email(creator.getEmail())
                .agencyLinks(linkDto != null ? List.of(linkDto) : Collections.emptyList())
                .build();
    }
}
