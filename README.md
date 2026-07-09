# Creator Directory API — Multi-Tenant REST API

A multi-tenant REST API for managing an influencer/creator directory, built with **Spring Boot 3.4**, **JPA/Hibernate**, and **H2 in-memory database**.

The core challenge: multiple agencies share the same platform, and one agency must never see or modify another agency's data — even on creators that both agencies work with.

---

## Tech Stack

| Layer         | Technology                       |
|---------------|----------------------------------|
| Language      | Java 21                         |
| Framework     | Spring Boot 3.4.1               |
| Database      | H2 (in-memory)                  |
| ORM           | Spring Data JPA / Hibernate     |
| Validation    | Jakarta Bean Validation         |
| Build Tool    | Maven                           |
| Testing       | JUnit 5 + Spring MockMvc        |

---

## Prerequisites

- **Java 21+** installed (`java -version` to verify)
- **Maven 3.8+** installed (`mvn -version` to verify)
- No database setup required — H2 runs in-memory automatically

---

## Setup & Run

### 1. Clone the repository

```bash
git clone <repository-url>
cd Creater-Directory-API
```

### 2. Build the project

```bash
mvn clean install -DskipTests
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

### 4. Load seed data

Seed data is loaded **automatically on startup** from `src/main/resources/seed-data.json`. No manual steps required.

The seed data includes:
- **3 agencies**: Nova Talent (free), Bright Star Agency (pro), Solo Creators Co (free)
- **4 users**: u1 (owner@nova), u2 (admin@nova), u3 (owner@brightstar), u4 (owner@solo)
- **3 creators**: Priya (linked to a1 & a2), Rahul (linked to a2), Ananya (linked to a1)

### 5. Run the test suite

```bash
mvn test
```

This runs **23 integration tests** covering tenant isolation, role-based permissions, and plan limit enforcement.

---

## Authentication

This API uses **simplified header-based auth**. Every request must include:

```
X-User-Id: <user-id>
```

The API resolves the user, finds their `agencyId` and `role`, and scopes all operations to that agency.

**Test users for manual testing:**

| User ID | Agency             | Role   |
|---------|--------------------|--------|
| `u1`    | Nova Talent (free) | owner  |
| `u2`    | Nova Talent (free) | admin  |
| `u3`    | Bright Star (pro)  | owner  |
| `u4`    | Solo Creators (free)| owner  |

---

## API Endpoints

### Creators API

| Method   | Endpoint                | Description                                           |
|----------|-------------------------|-------------------------------------------------------|
| `GET`    | `/creators`             | List creators linked to your agency (paginated)       |
| `GET`    | `/creators/:id`         | Get one creator (404 if not linked to your agency)    |
| `POST`   | `/creators`             | Create a new creator, auto-linked to your agency      |
| `POST`   | `/creators/:id/link`    | Link an existing creator to your agency               |
| `PATCH`  | `/creators/:id`         | Update shared fields and/or your agency's notes       |
| `DELETE` | `/creators/:id`         | Unlink from your agency (deletes if orphaned)         |

#### Query Parameters for `GET /creators`

| Parameter      | Description                                  | Example                          |
|----------------|----------------------------------------------|----------------------------------|
| `page`         | Page number (0-based, default: 0)            | `?page=0`                        |
| `limit`        | Page size (default: 10)                      | `?limit=5`                       |
| `niche`        | Filter by niche (case-insensitive)           | `?niche=beauty`                  |
| `minFollowers` | Minimum follower count                       | `?minFollowers=10000`            |
| `maxFollowers` | Maximum follower count                       | `?maxFollowers=100000`           |
| `sortBy`       | Sort field (default: followerCount)          | `?sortBy=engagementRate`         |
| `order`        | Sort direction: asc or desc (default: desc)  | `?order=asc`                     |

#### PATCH /creators/:id — Design Decision

A single PATCH request handles both shared and private data:
- **Shared fields** (`name`, `niche`, `followerCount`, `engagementRate`, `email`) → updates the Creator entity visible to all linked agencies.
- **`notes`** → updates only your agency's private notes in the AgencyCreatorLink. Never touches another agency's notes.
- Only non-null fields are applied (true PATCH semantics).

### Users API

| Method | Endpoint  | Description                                      |
|--------|-----------|--------------------------------------------------|
| `GET`  | `/users`  | List users in your agency                        |
| `POST` | `/users`  | Invite a new user (owner/admin only, member → 403)|

---

## Example Requests (curl)

```bash
# List creators for Nova Talent (user u1)
curl -H "X-User-Id: u1" http://localhost:8080/creators

# Get creator c1 as Nova Talent — sees only Nova's notes
curl -H "X-User-Id: u1" http://localhost:8080/creators/c1

# Get creator c1 as Bright Star — sees only Bright Star's notes
curl -H "X-User-Id: u3" http://localhost:8080/creators/c1

# Solo Creators trying to access c1 — gets 404 (not linked)
curl -H "X-User-Id: u4" http://localhost:8080/creators/c1

# Create a new creator
curl -X POST -H "X-User-Id: u1" -H "Content-Type: application/json" \
  -d '{"name":"New Creator","niche":"tech","followerCount":5000,"engagementRate":4.2,"email":"new@test.com","notes":"Initial note"}' \
  http://localhost:8080/creators

# Invite a user (as owner)
curl -X POST -H "X-User-Id: u1" -H "Content-Type: application/json" \
  -d '{"email":"newmember@nova.com","role":"member"}' \
  http://localhost:8080/users
```

---

## Error Responses

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-07-09T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Creator not found with id: 'c1'"
}
```

| HTTP Status | Meaning                                              |
|-------------|------------------------------------------------------|
| `401`       | Missing or invalid `X-User-Id` header                |
| `402`       | Free plan limit exceeded (max 5 creators)            |
| `403`       | Role insufficient (e.g., member trying to invite)    |
| `404`       | Resource not found or not linked to your agency      |
| `409`       | Duplicate (e.g., email already exists, already linked)|
| `400`       | Validation error                                     |

---

## H2 Console (Development)

Access the database console at: **http://localhost:8080/h2-console**

- JDBC URL: `jdbc:h2:mem:creatordb`
- Username: `sa`
- Password: *(empty)*
