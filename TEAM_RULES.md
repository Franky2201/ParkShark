# Bordeaux Brothers — Team Rules & Technical Guidelines (ParkShark)

## 1. Team Roster & Roles
* **Team Name:** Bordeaux Brothers
* **Scrum Master Rotation:**
  * **Sprint 1 (Days 1–2):** Noah
  * **Sprint 2 (Days 3–4):** Pietro

---

## 2. Scrum Ceremonies & Daily Rhythm
* **Sprints:** 2 Sprints of 2 days each.
* **Sprint Kickoff:** At the start of each 2-day sprint to estimate, prioritize, and pull stories from the Product Backlog into the Sprint Backlog.
* **Daily Stand-up:** Held **first thing every morning** around the Kanban board.
  * Stories are **not assigned by default** in advance; team members pull and assign stories during the Daily Stand-up.
* **Pair Code Reviews (Continuous):**
  * Each member develops their assigned story independently.
  * Once finished, the story moves to `Pair Review` and is reviewed 1-on-1 with a teammate (walking through the code together).
  * **If a developer finishes an issue and is waiting for a teammate to review:**
    1. Start tackling a new unassigned issue from the Sprint Backlog, **OR**
    2. Pair program with a teammate currently working on an active story.
* **Team Review & Merge Sessions (Twice Daily):**
  * **Session 1:** Right before lunch (5–10 minutes)
  * **Session 2:** End of the day (5–10 minutes)
  * **Goal:** Brief demo/walkthrough so the whole team knows what was accomplished, followed by merging reviewed PRs into `main` and verifying the CI pipeline run.

---

## 3. Kanban Board Workflow & Definition of Done (DoD)

### Columns
`Backlog` → `Sprint Backlog` → `In Progress` → `Pair Review` → `Ready for PR` → `Done`

### Transition Rules
1. **Backlog → Sprint Backlog:** Story is groomed, dependencies are clear, and it is selected during Sprint Kickoff.
2. **Sprint Backlog → In Progress:** Claimed by a developer during Daily Stand-up (or after completing a previous story). Branch created from `main`.
3. **In Progress → Pair Review:**
   * All functional acceptance criteria are met.
   * Flyway SQL migration added/updated (if applicable).
   * Full RestAssured + H2 integration tests pass locally.
   * Branch pushed to GitHub and CI build is green.
4. **Pair Review → PR Review:**
   * 1-on-1 code walkthrough completed with a teammate.
   * Code adheres to package structure, DTO separation, logging, and testing conventions.
5. **PR Review → Done:**
   * Presented during the pre-lunch or end-of-day 5–10 min Team Review.
   * PR merged into `main` and `main` CI pipeline passes.

---

## 4. Git & CI Conventions
* **Branch Naming:**
  * Features / Stories: `feat/<story-id>-<short-description>` (e.g., `feat/DI1-create-division`)
  * Technical / Setup: `chore/<short-description>` (e.g., `chore/flyway-setup`)
  * Bugfixes: `fix/<story-id>-<short-description>` (e.g., `fix/PSA1-capacity-check`)
* **Continuous Integration (GitHub Actions):**
  * Runs automatically on **every branch push** and on **pull requests / merges to `main`**.
*  **Branches are deleted once successfuly merged with main**

---

## 5. Architecture & Technical Standards

### Tech Stack
* **Language & Build:** Java + Maven (Single-Module setup)
* **Framework:** Spring Boot
* **Database & Migration:**
  * **Production / Local Dev:** PostgreSQL
  * **Integration Testing:** H2 In-Memory Database
  * **DDL Version Control:** Flyway (`src/main/resources/db/migration/V1__...sql`)
* **Documentation:** OpenAPI 3 / Swagger UI (`springdoc-openapi-starter-webmvc-ui`)
* **Logging:** SLF4J + Logback (log all errors, exceptions, illegal arguments, and unauthorized access attempts)
* **Security:** Spring Security with HTTP Basic Authentication, RBAC (`MANAGER`, `MEMBER`), and `BCryptPasswordEncoder`

### Package Structure (Package-by-Feature + 4 Internal Layers)
Base package: `com.switchfully.parkshark`

Each domain feature (`division`, `parkinglot`, `member`, `allocation`, `invoice`) contains 4 sub-packages (`api`, `service`, `domain`, `exception`), supplemented by a shared `common` package:

```text
com.switchfully.parkshark
├── common
│   ├── domain          # Shared Value Objects: Address, PostalCode, LicensePlate, PhoneNumber
│   ├── exception       # Global @RestControllerAdvice, custom base exceptions
│   └── security        # Spring Security Config, Basic Auth, RBAC, BCrypt setup
├── division
│   ├── api             # DivisionController, CreateDivisionDto, DivisionDto
│   ├── service         # DivisionService, DivisionMapper
│   ├── domain          # Division (Entity), DivisionRepository
│   └── exception       # DivisionNotFoundException, DuplicateDivisionException, etc.
├── parkinglot
│   ├── api
│   ├── service
│   ├── domain
│   └── exception
├── member
│   ├── api
│   ├── service
│   ├── domain
│   └── exception
├── allocation
│   ├── api
│   ├── service
│   ├── domain
│   └── exception
└── invoice
    ├── api
    ├── service
    ├── domain
    └── exception
```

### Layering & DTO Rules
1. **`api` layer:**
   * Contains `@RestController` classes and Request/Response `record` DTOs.
   * **Strict DTO Separation:** Always use distinct Request DTOs (`CreateXDto`) and Response DTOs (`XDto`, `XOverviewDto`). Never expose JPA entities in controllers.
2. **`service` layer:**
   * Contains `@Service` and `@Transactional` business logic and DTO-to-Entity mappers.
   * **Exception Handling:** All business validation and domain checks happen in the `service` (or domain methods invoked by the service). Throw meaningful custom exceptions caught by the global `@RestControllerAdvice` (which logs the error via SLF4J and returns a clean HTTP error response).
3. **`domain` layer:**
   * Contains JPA `@Entity` classes, `@Embeddable` value objects, enums, and Spring Data `JpaRepository` interfaces.
4. **`exception` layer:**
   * Feature-specific runtime exceptions mapped to HTTP status codes (`400 Bad Request`, `403 Forbidden`, `404 Not Found`, `409 Conflict`).

---

## 6. Testing Conventions
* **Integration Tests (Mandatory for every story):**
  * Full end-to-end integration test coverage across the project using `@SpringBootTest(webEnvironment = RANDOM_PORT)`, **RestAssured**, and **H2**.
* **Unit Tests:**
  * Written at the developer's discretion (recommended when using TDD for complex domain rules such as membership reductions, fine calculation, and invoice generation).
* **Test Structure & Annotations:**
  * Use `@DisplayName` on all test classes and test methods for readable test reports.
  * Use `@Nested` classes to group test scenarios logically (e.g., `HappyPath`, `ValidationAndExceptions`, `SecurityAndAuthorization`).
  * Follow the explicit **`// Given` — `// When` — `// Then`** structure inside every test method.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@DisplayName("DI1 - Division Integration Tests")
class DivisionControllerIntegrationTest {

    @Nested
    @DisplayName("POST /divisions - Create a Division")
    class CreateDivision {

        @Test
        @DisplayName("Should create a new division and return 201 Created when valid data is provided")
        void shouldCreateDivision_whenValidInput() {
            // Given
            CreateDivisionDto request = new CreateDivisionDto("Shark West", "WestPark NV", "John Doe");

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/divisions")
            .then()
                .statusCode(HttpStatus.CREATED.value());
        }
    }
}
```
