# Backend Challenge — Multi-Tenant Creator Directory API

**Team size:** up to 5 people
**Time budget:** 7 days
**All you need.** Everything you need to complete this challenge is in this document. If anything is ambiguous, write down the assumption you made and move on — we're evaluating your judgment, not your ability to guess what we meant.

---

## The scenario

An agency is building an AI-native CRM for talent/influencer agencies. One of the hardest problems in a product like this is **multi-tenancy**: many separate agencies use the same platform, and one agency must never be able to see or touch another agency's data — even by accident, even under a bug, even under load.

Your team's job: build a backend API for a simplified version of this problem — a **Creator Directory** — and prove, with your own tests, that tenant isolation actually holds.

We care more about **how you guarantee isolation** than about how many features you build. A small, correct, well-tested API beats a large one with a data leak.

---

## What you're building

A REST API (any language/framework you like — Node.js + TypeScript is our own stack, but use whatever your team is strongest in) with the following entities:

### Entities

**Agency** (the tenant)
```json
{
  "id": "uuid",
  "name": "string",
  "plan": "free | pro",
  "createdAt": "ISO date"
}
```

**User** (belongs to exactly one Agency)
```json
{
  "id": "uuid",
  "agencyId": "uuid",
  "email": "string",
  "role": "owner | admin | member",
  "createdAt": "ISO date"
}
```

**Creator** (an influencer record — the core resource)
```json
{
  "id": "uuid",
  "name": "string",
  "niche": "string",
  "followerCount": "number",
  "engagementRate": "number (0-100)",
  "email": "string",
  "agencyLinks": [
    { "agencyId": "uuid", "notes": "string", "addedAt": "ISO date" }
  ]
}
```

Notice `agencyLinks` is an **array**, not a single `agencyId`. This is deliberate: a Creator can be discovered/added independently by more than one Agency (e.g., two agencies both work with the same influencer). Each Agency should only ever see **its own** entry in `agencyLinks` (its own `notes`) — never another agency's notes about the same creator. Design this relationship carefully; it's one of the two things we're scoring most closely.

---

## Functional requirements

1. **Auth (simplified):** you do not need to build real password auth. Assume every request carries a header `X-User-Id: <uuid>` identifying an already-authenticated user. Your API looks up that user, finds their `agencyId`, and every subsequent action must be scoped to that agency. (This lets you focus on authorization/isolation, not on building a login system.)

2. **Creators API:**
   - `GET /creators` — list creators visible to the caller's agency (i.e., where `agencyLinks` contains their `agencyId`), with pagination (`?page=&limit=`), filtering (`?niche=`, `?minFollowers=&maxFollowers=`), and sorting (`?sortBy=followerCount&order=desc`).
   - `GET /creators/:id` — get one creator, but **only** if the caller's agency is linked to it; otherwise return 404 (not 403 — don't reveal that the record exists at all).
   - `POST /creators` — create a new creator, automatically linked to the caller's agency.
   - `POST /creators/:id/link` — link an *existing* creator (found via `GET /creators`, or by knowing its id from your own agency's prior interactions — you decide) to the caller's agency, with agency-specific `notes`.
   - `PATCH /creators/:id` — update the shared fields (name, niche, followerCount, engagementRate, email) **or** update your own agency's `notes` in `agencyLinks` — but never another agency's notes. Decide and document how a caller updates "their" notes vs. the shared fields.
   - `DELETE /creators/:id` — this should remove **only your agency's link** to the creator (unlink), not delete the creator globally, unless your agency was the only one linked (then it's fine to delete the record, or soft-delete — your call, document it).

3. **Users API:**
   - `GET /users` — list users in the caller's own agency only.
   - `POST /users` — invite a new user to the caller's agency. Only `owner` and `admin` roles may do this — `member` role gets a 403.

4. **Role-based permissions:** `owner` can do everything including inviting/removing users and changing roles. `admin` can manage creators and invite users but not remove the `owner` or change billing-plan settings (there's no real billing here — just make sure an `admin` can't, e.g., upgrade/downgrade the agency's plan; only `owner` can). `member` can view and edit creators but cannot invite users or change anything agency-level.

5. **Plan limits, enforced server-side (not just documented):** a `free`-plan Agency may link to a maximum of **5 Creators**. The 6th `POST /creators` or `POST /creators/:id/link` attempt must be rejected with a clear error (e.g., HTTP 402) — not silently allowed. A `pro`-plan Agency has no limit.

6. **Tenant isolation must be structural, not just "we remembered to add a filter."** However you implement this (a repository/data-access layer, middleware, database-level row security, whatever your stack supports) — the important thing is that it should be **hard for a future engineer on your team to accidentally write a query that leaks data**, not just that today's queries happen to be correct. Explain your approach in `ARCHITECTURE.md` (see Deliverables).

---

## Seed data

Use this fixture to seed your database on startup (or provide a seed script that loads it):

```json
{
  "agencies": [
    { "id": "a1", "name": "Nova Talent", "plan": "free" },
    { "id": "a2", "name": "Bright Star Agency", "plan": "pro" },
    { "id": "a3", "name": "Solo Creators Co", "plan": "free" }
  ],
  "users": [
    { "id": "u1", "agencyId": "a1", "email": "owner@nova.com", "role": "owner" },
    { "id": "u2", "agencyId": "a1", "email": "admin@nova.com", "role": "admin" },
    { "id": "u3", "agencyId": "a2", "email": "owner@brightstar.com", "role": "owner" },
    { "id": "u4", "agencyId": "a3", "email": "owner@solo.com", "role": "owner" }
  ],
  "creators": [
    {
      "id": "c1", "name": "Priya Sharma", "niche": "beauty", "followerCount": 45000, "engagementRate": 3.8, "email": "priya@example.com",
      "agencyLinks": [
        { "agencyId": "a1", "notes": "Great for skincare campaigns", "addedAt": "2026-01-10T00:00:00Z" },
        { "agencyId": "a2", "notes": "Booked for Q1 shoot", "addedAt": "2026-02-01T00:00:00Z" }
      ]
    },
    {
      "id": "c2", "name": "Rahul Verma", "niche": "fitness", "followerCount": 120000, "engagementRate": 2.1, "email": "rahul@example.com",
      "agencyLinks": [
        { "agencyId": "a2", "notes": "High reach, slower replies", "addedAt": "2026-01-15T00:00:00Z" }
      ]
    },
    {
      "id": "c3", "name": "Ananya Iyer", "niche": "travel", "followerCount": 8000, "engagementRate": 6.4, "email": "ananya@example.com",
      "agencyLinks": [
        { "agencyId": "a1", "notes": "Micro-influencer, very responsive", "addedAt": "2026-01-20T00:00:00Z" }
      ]
    }
  ]
}
```

A correct implementation means: if `u3` (Bright Star) calls `GET /creators/c1`, they see `c1` with **only** their own note ("Booked for Q1 shoot") — never Nova Talent's note. If `u4` (Solo Creators, not linked to `c1` at all) calls `GET /creators/c1`, they get a 404.

---

## Deliverables

1. A runnable API (a `README.md` with exact setup/run instructions — assume the evaluator has Node/Python/Go/whatever installed but nothing else pre-configured; include how to load the seed data).
2. **Your own automated tests** that specifically prove tenant isolation holds — at minimum: (a) agency A cannot read agency B's private notes on a shared creator, (b) agency A cannot see or modify a creator it has no link to, (c) a `member` role cannot invite a user, (d) the free-plan 5-creator limit is enforced and returns a clear error on the 6th attempt. We will run your test suite ourselves.
3. `ARCHITECTURE.md` — a short (half to one page) explanation of: how you structured tenant isolation (and why it's hard to accidentally break), how you modeled the shared-creator relationship, and any assumptions you made about ambiguous parts of this spec.
4. A brief note on what you'd do differently with more time (this is a real signal for us — we don't expect a finished product in a week).

## How to submit

A link to a git repository (public, or private with access granted to whoever sent you this challenge) or a zip file. Include your `README.md`, `ARCHITECTURE.md`, source code, and tests. Commit history should reflect real work from your whole team over the sprint — don't squash everything into one commit at the deadline.

Good luck — we're looking forward to seeing how your team thinks about this.
