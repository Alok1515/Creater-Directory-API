package com.creator.controller;

import com.creator.dto.CreateCreatorRequest;
import com.creator.dto.CreatorResponse;
import com.creator.dto.LinkCreatorRequest;
import com.creator.dto.PaginatedResponse;
import com.creator.dto.UpdateCreatorRequest;
import com.creator.service.CreatorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Creator operations.
 *
 * All endpoints are tenant-scoped — the caller's agency is resolved from the
 * X-User-Id header via TenantContextFilter, and CreatorService uses that
 * context to scope every query and mutation.
 *
 * Endpoints:
 *   GET    /creators           — list creators with pagination, filtering, sorting
 *   GET    /creators/:id       — get one creator (404 if not linked to caller's agency)
 *   POST   /creators           — create a new creator, auto-linked to caller's agency
 *   POST   /creators/:id/link  — link an existing creator to caller's agency
 *   PATCH  /creators/:id       — update shared fields and/or caller's own notes
 *   DELETE /creators/:id       — unlink from caller's agency (deletes if orphaned)
 */
@RestController
@RequestMapping("/creators")
public class CreatorController {

    private static final Logger log = LoggerFactory.getLogger(CreatorController.class);

    private final CreatorService creatorService;

    public CreatorController(CreatorService creatorService) {
        this.creatorService = creatorService;
    }

    /**
     * List creators visible to the caller's agency with optional filtering,
     * sorting, and pagination.
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<CreatorResponse>> listCreators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String niche,
            @RequestParam(required = false) Long minFollowers,
            @RequestParam(required = false) Long maxFollowers,
            @RequestParam(defaultValue = "followerCount") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {

        log.debug("GET /creators — page={}, limit={}, niche={}, minFollowers={}, maxFollowers={}, sortBy={}, order={}",
                page, limit, niche, minFollowers, maxFollowers, sortBy, order);

        PaginatedResponse<CreatorResponse> response = creatorService.listCreatorsFiltered(
                page, limit, niche, minFollowers, maxFollowers, sortBy, order);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a single creator by ID.
     * Returns 404 if the creator is not linked to the caller's agency
     * (deliberately hides resource existence from other tenants).
     */
    @GetMapping("/{id}")
    public ResponseEntity<CreatorResponse> getCreator(@PathVariable String id) {
        log.debug("GET /creators/{}", id);
        CreatorResponse response = creatorService.getCreatorById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new creator, automatically linked to the caller's agency.
     * Enforces free-plan limit (402 if exceeded).
     */
    @PostMapping
    public ResponseEntity<CreatorResponse> createCreator(@Valid @RequestBody CreateCreatorRequest request) {
        log.debug("POST /creators — name: {}", request.getName());
        CreatorResponse response = creatorService.createCreator(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Link an existing creator to the caller's agency with optional notes.
     * Enforces free-plan limit (402 if exceeded).
     * Returns 409 if already linked, 404 if creator doesn't exist.
     */
    @PostMapping("/{id}/link")
    public ResponseEntity<CreatorResponse> linkCreator(
            @PathVariable String id,
            @RequestBody(required = false) LinkCreatorRequest request) {
        log.debug("POST /creators/{}/link", id);
        CreatorResponse response = creatorService.linkCreatorToAgency(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update shared fields and/or the caller's own agency-specific notes.
     * Only non-null fields are applied (true PATCH semantics).
     * Returns 404 if the creator is not linked to the caller's agency.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<CreatorResponse> updateCreator(
            @PathVariable String id,
            @Valid @RequestBody UpdateCreatorRequest request) {
        log.debug("PATCH /creators/{}", id);
        CreatorResponse response = creatorService.updateCreator(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Unlink a creator from the caller's agency.
     * Removes only the caller's link — the creator is deleted globally only
     * if no other agency is linked to it.
     * Returns 404 if the creator is not linked to the caller's agency.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreator(@PathVariable String id) {
        log.debug("DELETE /creators/{}", id);
        creatorService.unlinkCreator(id);
        return ResponseEntity.noContent().build();
    }
}
