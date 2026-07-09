# Architecture — Multi-Tenant Creator Directory API

## How We Structured Tenant Isolation (and Why It's Hard to Accidentally Break)

### The Core Problem

A Creator can be linked to multiple Agencies. Each Agency has its own private notes about that Creator. Agency A must never see Agency B's notes — even on a shared Creator. And if a future engineer writes a new query, they shouldn't be able to accidentally leak data across tenants.

### Our Approach: Structural Isolation via the Link Table

We use a **separate `AgencyCreatorLink` join table** as the single gateway for all tenant-scoped data access. Here's why this design is robust:

```
┌──────────┐       ┌───────────────────────┐       ┌──────────┐
│  Agency  │──1:N──│  AgencyCreatorLink    │──N:1──│  Creator │
│          │       │  - agencyId           │       │          │
│  id      │       │  - creatorId          │       │  id      │
│  name    │       │  - notes (PRIVATE)    │       │  name    │
│  plan    │       │  - addedAt            │       │  niche   │
└──────────┘       └───────────────────────┘       │  ...     │
                                                    └──────────┘
```

**Key design decisions:**

1. **The Creator entity has NO agency information.** There is no `agencyId` on Creator. The only way to discover which creators belong to an agency is through the link table — which is always filtered by `agencyId`. This means a future engineer cannot write `creatorRepository.findAll()` and accidentally return all creators across all tenants; they'd only get shared profile data with no agency context.

2. **All service methods start with the link table.** Every operation in `CreatorService` follows the same pattern:
   - Step 1: Query `AgencyCreatorLinkRepository` filtered by the caller's `agencyId`.
   - Step 2: Only then fetch/modify Creator records for those linked IDs.
   - Step 3: Build responses with only the caller's own link data.

3. **The `TenantContext` is set by a servlet filter and cleared after every request.** The `TenantContextFilter` resolves the `X-User-Id` header into a `(userId, agencyId, role)` tuple stored in a ThreadLocal. All downstream code reads from this context — there's no way to "forget" which agency is calling.

4. **Private notes are physically separated.** Because `notes` lives on `AgencyCreatorLink` (not on `Creator`), the only way to access them is through a query that requires `agencyId`. Even if someone joins the Creator table directly, notes don't exist there.

5. **Unlinked creators return 404, not 403.** When an agency tries to access a creator it's not linked to, we return 404 instead of 403. This prevents information leakage — the caller can't even discover that a creator exists in another agency's directory.

### Why This Is Hard to Break

| Risk                                      | Mitigation                                                                 |
|-------------------------------------------|----------------------------------------------------------------------------|
| Engineer writes a query without agencyId  | Creator has no agency info; link table is the only path to tenant data     |
| Engineer reads all notes accidentally     | Notes only exist on AgencyCreatorLink, which requires agencyId to query    |
| Thread-local leaks between requests       | TenantContext.clear() is called in a finally block in the filter           |
| 403 reveals resource existence            | We always return 404 for unlinked resources                                |

---

## How We Modeled the Shared-Creator Relationship

The challenge says `agencyLinks` is an array — a Creator can be independently discovered by multiple Agencies. We model this as:

- **Creator** stores the shared profile (name, niche, followerCount, etc.) — one row per creator.
- **AgencyCreatorLink** stores the per-agency relationship (agencyId, creatorId, notes, addedAt) — one row per agency-creator pair, with a unique constraint on `(agencyId, creatorId)`.

In the API response, the `agencyLinks` array always contains **exactly one entry** — the calling agency's own link. This is assembled at the service layer by mapping the link table row for the caller's agencyId.

---

## Role-Based Access Control

| Role     | Creators (CRUD)           | Invite Users | Change Agency Plan |
|----------|---------------------------|--------------|--------------------|
| `owner`  | Full access               | ✅ Yes       | ✅ Yes (only role)  |
| `admin`  | Full access               | ✅ Yes       | ❌ No              |
| `member` | View & edit creators only | ❌ No (403)  | ❌ No              |

RBAC is enforced in the **service layer** (not just the controller), so it cannot be bypassed by calling internal methods.

---

## Plan Limits

- **Free plan**: Maximum 5 creator links per agency. The 6th `POST /creators` or `POST /creators/:id/link` returns HTTP 402 with a clear error message.
- **Pro plan**: Unlimited creator links.

Enforcement is server-side via `CreatorService.enforcePlanLimit()`, which counts existing links in the `AgencyCreatorLink` table before allowing new ones.

---

## Assumptions Made

1. **Simplified auth**: We trust the `X-User-Id` header as-is (no password/JWT). The challenge explicitly says to do this.
2. **In-memory database**: H2 is used for zero-setup evaluation. In production, this would be PostgreSQL with row-level security.
3. **Owner role is pre-seeded**: The `POST /users` endpoint only allows inviting `admin` or `member` roles. There's exactly one `owner` per agency, created during setup.
4. **Shared field updates are visible to all**: When any linked agency PATCHes a creator's name/niche/followerCount, all linked agencies see the change. This matches the "shared profile" model described in the spec.
5. **Soft-delete not implemented**: When unlinking the last agency from a creator, we hard-delete the creator record. In production, we'd likely soft-delete with a `deletedAt` timestamp.

---

## What I'd Do Differently With More Time

1. **PostgreSQL + Row-Level Security (RLS)**: Replace H2 with PostgreSQL and implement database-level RLS policies. This would add a second layer of isolation — even if the application code has a bug, the database itself would prevent cross-tenant queries.

2. **Audit logging**: Add an `audit_log` table tracking who changed what and when. Critical for a multi-tenant system where data integrity disputes can arise between agencies.

3. **API versioning**: Add `/v1/` prefix to all endpoints and implement proper versioning strategy for backward compatibility.

4. **Caching**: Add Redis caching for frequently accessed creator profiles, with cache invalidation scoped to the agency level.

5. **Rate limiting**: Implement per-agency rate limiting to prevent one tenant from degrading service for others.

6. **Pagination cursor**: Replace offset-based pagination with cursor-based pagination for better performance on large datasets.

7. **Docker + docker-compose**: Containerize the app with a proper database for one-command deployment.

8. **CI/CD pipeline**: Add GitHub Actions with automated test runs on every PR.

9. **OpenAPI/Swagger documentation**: Auto-generate interactive API docs from the controller annotations.

10. **Integration with a real auth provider**: Replace the `X-User-Id` header with JWT tokens issued by an identity provider (e.g., Auth0 or Keycloak).
