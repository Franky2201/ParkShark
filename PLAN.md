# ParkShark — Product Backlog, Dependencies & Issue Specifications

## 1. Story Dependency Graph & Sprint Roadmap

### Dependency Overview
* **Technical Foundation:** `TECH-1` → `TECH-2` → `TECH-3` (and `TECH-4` can be plugged in once endpoints exist or alongside `TECH-3`)
* **Division Track:** `DI1` → `DI2`, `DI3`, `DI4`
* **Parking Lot Track:** `PL1` → `PL2`, `PL3` | `DI1` + `PL1` → `DIPL1`
* **Member Track:** `ME1` → `ME2`, `ME3`, `ML1`
* **Allocation Track:** `PL1` + `ME1` + `ML1` → `PSA1` → `PSA2` → `PSA3` → `PSA4`, `PSA5`, `PSA6`
* **Invoice Track:** `PSA2` + `ML1` → `INV1` → `INV2`, `INV3`

### Suggested 2-Sprint Plan (4 Developers)
* **Sprint 1 (Days 1–2) — Core Must-Haves & Foundation (SM: Noah)**
  * **Tech Setup:** `TECH-1`, `TECH-2`, `TECH-3`
  * **Divisions:** `DI1`, `DI2`, `DI4`
  * **Parking Lots:** `PL1`, `PL2`, `DIPL1`
  * **Members:** `ME1`, `ME2`, `ML1`
* **Sprint 2 (Days 3–4) — Allocations, Security & Nice-To-Haves (SM: Pietro)**
  * **Allocations (Must-Have):** `PSA1`, `PSA2`, `PSA3`
  * **Security (Tech Story):** `TECH-4`
  * **Single-Resource Getters (Nice-To-Have):** `DI3`, `PL3`, `ME3`
  * **Allocation Extensions (Nice-To-Have):** `PSA4`, `PSA5`, `PSA6`
  * **Invoicing (Nice-To-Have):** `INV1`, `INV2`, `INV3`

---

## 2. Technical Stories (`chore/`)

### [TECH-1] Project Skeleton, Git Ignore, CI Pipeline, Logging & OpenAPI
* **Priority:** Must-Have
* **Branch:** `chore/TECH1-project-setup`
* **Dependencies:** None
* **Description:** Initialize the GitHub repository with a proper `.gitignore` before the first commit. Set up the single-module Maven Spring Boot LTS project, GitHub Actions CI workflow, SLF4J/Logback configuration, and Swagger UI (`springdoc-openapi`).
* **Subtasks:**
  - [ ] Create `.gitignore` (ignore `target/`, `.idea/`, `*.iml`, `.DS_Store`, `.env`, local logs).
  - [ ] Generate `pom.xml` with `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, `springdoc-openapi-starter-webmvc-ui`, `h2` (test), and `rest-assured` (test).
  - [ ] Configure `.github/workflows/ci.yml` to run `mvn clean verify` on every push to `feat/**`, `chore/**`, `fix/**`, and `main`.
  - [ ] Verify Swagger UI is reachable at `/swagger-ui.html`.

---

### [TECH-2] Database Configuration (PostgreSQL, H2 Test Profile & Flyway DDL)
* **Priority:** Must-Have
* **Branch:** `chore/TECH2-database-and-flyway`
* **Dependencies:** `TECH-1`
* **Description:** Configure PostgreSQL for main execution and an in-memory H2 database for `@SpringBootTest` integration tests. Set up Flyway so all DDL scripts (`src/main/resources/db/migration/V1__init_schema.sql`) are version-controlled.
* **Subtasks:**
  - [ ] Configure `src/main/resources/application.properties` for PostgreSQL and Flyway.
  - [ ] Configure `src/test/resources/application.properties` (or `@AutoConfigureTestDatabase`) for H2 in PostgreSQL compatibility mode (`jdbc:h2:mem:parkshark;MODE=PostgreSQL`).
  - [ ] Create initial `V1__init_schema.sql` migration file (or modular migrations per feature).
  - [ ] Add a smoke integration test verifying the Spring context and Flyway migrations load against H2.

---

### [TECH-3] Global Exception Handling (`@RestControllerAdvice`) & Shared Common Value Objects
* **Priority:** Must-Have
* **Branch:** `chore/TECH3-common-exceptions-and-value-objects`
* **Dependencies:** `TECH-1`
* **Description:** Create the `common` package containing the global exception handler (with SLF4J error logging) and reusable `@Embeddable` value objects (`Address`, `PostalCode`, `LicensePlateDTO`).
* **Subtasks:**
  - [ ] Implement `ControllerExceptionHandler` (`@RestControllerAdvice`) logging all exceptions (`logger.error(...)`) and returning structured error JSON (`timestamp`, `status`, `error`, `message`, `path`).
  - [ ] Create base exceptions: `ResourceNotFoundException` (404), `BusinessValidationException` / `IllegalArgumentException` (400), `DuplicateResourceException` (409), `ForbiddenOperationException` (403).
  - [ ] Implement `@Embeddable` `PostalCode` (`code`, `label`) and `Address` (`streetName`, `streetNumber`, `PostalCode`).
  - [ ] Implement `@Embeddable` `LicensePlateDTO` (`plateNumber`, `issuingCountry`).

---

### [TECH-4] Security Setup (Basic Auth, RBAC & BCrypt)
* **Priority:** Must-Have (Separate Technical Story)
* **Branch:** `chore/TECH4-security-basic-auth-rbac`
* **Dependencies:** `TECH-3`, `ME1`
* **Description:** Secure the REST API using Spring Security HTTP Basic Authentication with `BCryptPasswordEncoder` and Role-Based Access Control (`MANAGER` and `MEMBER`). Log all unauthorized (`401`) and forbidden (`403`) access attempts.
* **Subtasks:**
  - [ ] Configure `SecurityFilterChain` with HTTP Basic Auth and stateless session management.
  - [ ] Configure `BCryptPasswordEncoder` bean.
  - [ ] Seed or configure a default `MANAGER` account and link registered Members (`MEMBER` role) to authentication credentials.
  - [ ] Restrict manager endpoints (`/divisions/**`, `POST /parking-lots`, `GET /members`, etc.) to `ROLE_MANAGER` and member endpoints (`POST /allocations`, `PUT /allocations/{id}/stop`, etc.) to `ROLE_MEMBER` (while allowing public `POST /members` registration).
  - [ ] Configure `AuthenticationEntryPoint` and `AccessDeniedHandler` to log unauthorized/forbidden attempts via SLF4J.
  - [ ] Write RestAssured integration tests verifying `401 Unauthorized` and `403 Forbidden` scenarios.

---

## 3. Functional Stories — Divisions (`division`)

### [DI1] Create a Division
* **Priority:** Must-Have
* **Branch:** `feat/DI1-create-division`
* **Dependencies:** `TECH-2`, `TECH-3`
* **User Story:** As a Manager I want to create a division so that companies bought by ParkShark are registered in the system.
* **REST Endpoint:**
  * `POST /divisions` → `201 Created` (Role: `MANAGER`)
* **Request DTO (`CreateDivisionDto`):**
```json
{
  "name": "ParkShark West",
  "originalName": "WestParking NV",
  "director": "Al Capone"
}
```
* **Response DTO (`DivisionDto`):**
```json
{
  "id": 1,
  "name": "ParkShark West",
  "originalName": "WestParking NV",
  "director": "Al Capone"
}
```
* **Domain & Service Validation Rules:**
  * `name`, `originalName`, and `director` are required and cannot be blank (`400 Bad Request`).
  * Division `name` must be unique (`400 Bad Request` or `409 Conflict`).
* **Subtasks:**
  - [ ] Add `division` table DDL in Flyway (`id`, `name`, `original_name`, `director`).
  - [ ] Create `Division` entity and `DivisionRepository`.
  - [ ] Create `DivisionService` with validation and `DivisionMapper`.
  - [ ] Create `DivisionController` (`POST /divisions`) and DTOs.
  - [ ] Write `@SpringBootTest` + RestAssured integration tests (`@Nested`, `@DisplayName`, `Given/When/Then`).

---

### [DI2] Get All Divisions
* **Priority:** Must-Have
* **Branch:** `feat/DI2-get-all-divisions`
* **Dependencies:** `DI1`
* **User Story:** As a Manager I want to get an overview of all divisions.
* **REST Endpoint:**
  * `GET /divisions` → `200 OK` (Role: `MANAGER`)
* **Response DTO (`List<DivisionDto>`):**
```json
[
  {
    "id": 1,
    "name": "ParkShark West",
    "originalName": "WestParking NV",
    "director": "Al Capone"
  }
]
```
* **Subtasks:**
  - [ ] Add `getAllDivisions()` in `DivisionService`.
  - [ ] Add `GET /divisions` endpoint in `DivisionController`.
  - [ ] Write RestAssured integration tests (empty list and populated list).

---

### [DI3] Get a Single Division
* **Priority:** Nice-To-Have
* **Branch:** `feat/DI3-get-single-division`
* **Dependencies:** `DI1`
* **User Story:** As a Manager I want to get a single, specific division by its ID.
* **REST Endpoint:**
  * `GET /divisions/{id}` → `200 OK` (Role: `MANAGER`)
* **Domain & Service Validation Rules:**
  * Throw `DivisionNotFoundException` (`404 Not Found`) if `id` does not exist.
* **Subtasks:**
  - [ ] Add `getDivisionById(Long id)` in `DivisionService`.
  - [ ] Add `GET /divisions/{id}` in `DivisionController`.
  - [ ] Write RestAssured integration tests (200 OK and 404 Not Found).

---

### [DI4] Create a Subdivision
* **Priority:** Must-Have
* **Branch:** `feat/DI4-create-subdivision`
* **Dependencies:** `DI1`
* **User Story:** As a Manager I want to create a division that is a subdivision of an already existing division.
* **REST Endpoint:**
  * Extend `POST /divisions` (with optional `parentDivisionId`) or `POST /divisions/{parentId}/subdivisions` → `201 Created` (Role: `MANAGER`)
* **Request DTO (`CreateDivisionDto`):**
```json
{
  "name": "ParkShark Bruges",
  "originalName": "BruggePark BV",
  "director": "Lucky Luciano",
  "parentDivisionId": 1
}
```
* **Response DTO (`DivisionDto`):**
```json
{
  "id": 2,
  "name": "ParkShark Bruges",
  "originalName": "BruggePark BV",
  "director": "Lucky Luciano",
  "parentDivisionId": 1
}
```
* **Domain & Service Validation Rules:**
  * If `parentDivisionId` is provided, verify the parent division exists; otherwise throw `DivisionNotFoundException` (`404 Not Found` / `400 Bad Request`).
* **Subtasks:**
  - [ ] Add nullable `subdivision_id` foreign key column to `division` table in Flyway.
  - [ ] Update `Division` entity with self-referencing `@ManyToOne` `parentDivision`.
  - [ ] Update `DivisionService`, `CreateDivisionDto`, and `DivisionDto`.
  - [ ] Write RestAssured integration tests for valid parent and non-existent parent ID.

---

## 4. Functional Stories — Parking Lots (`parkingLot`)

### [PL1] Create a Parking Lot
* **Priority:** Must-Have
* **Branch:** `feat/PL1-create-parking-lot`
* **Dependencies:** `TECH-2`, `TECH-3`
* **User Story:** As a Manager I want to create a parking lot with a category, capacity, price per hour, address, and contact person.
* **REST Endpoint:**
  * `POST /parking-lots` → `201 Created` (Role: `MANAGER`)
* **Request DTO (`CreateParkingLotDto`):**
```json
{
  "name": "Central Station Underground",
  "category": "UNDERGROUND_BUILDING",
  "maxCapacity": 250,
  "pricePerHour": 3.50,
  "address": {
    "streetName": "Stationstraat",
    "streetNumber": "10",
    "postalCode": {
      "code": "3000",
      "label": "Leuven"
    }
  },
  "contactPerson": {
    "firstName": "Tony",
    "lastName": "Soprano",
    "mobilePhoneNumber": "+32470123456",
    "telephoneNumber": null,
    "email": "tony@parkshark.com",
    "address": {
      "streetName": "Meir",
      "streetNumber": "1",
      "postalCode": {
        "code": "2000",
        "label": "Antwerpen"
      }
    }
  }
}
```
* **Domain & Service Validation Rules:**
  * `name`, `category` (`UNDERGROUND_BUILDING`, `ABOVE_GROUND_BUILDING`), `maxCapacity` (greater than 0), `pricePerHour` (greater than or equal to 0), `address`, and `contactPerson` are required.
  * `contactPerson.email` must be present and follow a valid email format.
  * `contactPerson` must have **at least one** phone number (`mobilePhoneNumber` OR `telephoneNumber`; both allowed). Throw `400 Bad Request` if both are null/blank.
  * Postal code must exist or be persisted (`code` + `label`).
* **Subtasks:**
  - [ ] Add Flyway DDL for `postal_code`, `contact_person`, and `parking_lot`.
  - [ ] Create `ParkingLotCategory` enum, `ContactPerson`, and `ParkingLot` entities.
  - [ ] Implement `ParkingLotService` with contact person phone/email validation.
  - [ ] Create `ParkingLotController` (`POST /parking-lots`) and DTOs.
  - [ ] Write RestAssured integration tests covering valid creation, missing both phone numbers, invalid email, and negative capacity.

---

### [PL2] Get All Parking Lots (Overview)
* **Priority:** Must-Have
* **Branch:** `feat/PL2-get-all-parking-lots`
* **Dependencies:** `PL1`
* **User Story:** As a Manager I want to get an overview of all parking lots returning only `id`, `name`, `capacity`, and the contact person's `email` and phone number(s).
* **REST Endpoint:**
  * `GET /parking-lots` → `200 OK` (Role: `MANAGER`)
* **Response DTO (`List<ParkingLotOverviewDto>`):**
```json
[
  {
    "id": 1,
    "name": "Central Station Underground",
    "maxCapacity": 250,
    "contactPersonEmail": "tony@parkshark.com",
    "contactPersonMobilePhone": "+32470123456",
    "contactPersonTelephone": null
  }
]
```
* **Subtasks:**
  - [ ] Create `ParkingLotOverviewDto` containing strictly the required summary fields.
  - [ ] Add `getAllParkingLots()` in `ParkingLotService` and `GET /parking-lots` in `ParkingLotController`.
  - [ ] Write RestAssured integration tests verifying the exact fields returned.

---

### [PL3] Get a Single Parking Lot
* **Priority:** Nice-To-Have
* **Branch:** `feat/PL3-get-single-parking-lot`
* **Dependencies:** `PL1`
* **User Story:** As a Manager I want to get all data for a single, specified parking lot.
* **REST Endpoint:**
  * `GET /parking-lots/{id}` → `200 OK` (Role: `MANAGER`)
* **Domain & Service Validation Rules:**
  * Throw `ParkingLotNotFoundException` (`404 Not Found`) if the parking lot ID does not exist.
* **Subtasks:**
  - [ ] Create full `ParkingLotDetailDto` with all fields (including division info once `DIPL1` is merged).
  - [ ] Add `getParkingLotById(Long id)` in `ParkingLotService` and `GET /parking-lots/{id}` in `ParkingLotController`.
  - [ ] Write RestAssured integration tests (200 OK and 404 Not Found).

---

### [DIPL1] Parking Lot Belongs to a Division
* **Priority:** Must-Have
* **Branch:** `feat/DIPL1-parking-lot-division`
* **Dependencies:** `DI1`, `PL1` (and updates `PL3` if implemented)
* **User Story:** As a Parking Lot I want to have a Division so that I know to what division I belong.
* **Impact on Endpoints:**
  * `POST /parking-lots` (`CreateParkingLotDto`) now requires `"divisionId": 1`.
  * `GET /parking-lots/{id}` (`ParkingLotDetailDto`) includes `"division": { ... }` (does **not** impact `PL2` overview).
* **Domain & Service Validation Rules:**
  * `divisionId` is required when creating a parking lot.
  * Throw `DivisionNotFoundException` (`404 Not Found` / `400 Bad Request`) if `divisionId` does not match an existing division.
* **Subtasks:**
  - [ ] Add `division_id` foreign key column to `parking_lot` table in Flyway.
  - [ ] Add `@ManyToOne` `Division` relationship to `ParkingLot` entity.
  - [ ] Update `CreateParkingLotDto`, `ParkingLotDetailDto`, and `ParkingLotService`.
  - [ ] Update existing `PL1` / `PL3` integration tests and add test for non-existent `divisionId`.

---

## 5. Functional Stories — Members & Membership Levels (`member`)

### [ME1] Register as a Member
* **Priority:** Must-Have
* **Branch:** `feat/ME1-register-member`
* **Dependencies:** `TECH-2`, `TECH-3`
* **User Story:** As a Person I want to register myself as a Member so that I can park my car in a ParkShark parking lot.
* **REST Endpoint:**
  * `POST /members` → `201 Created` (Public / Unauthenticated)
* **Request DTO (`CreateMemberDto`):**
```json
{
  "firstName": "Michael",
  "lastName": "Corleone",
  "address": {
    "streetName": "Rue Neuve",
    "streetNumber": "42",
    "postalCode": {
      "code": "1000",
      "label": "Brussels"
    }
  },
  "telephoneNumber": "+32488112233",
  "email": "michael@corleone.org",
  "password": "secretPassword123",
  "licensePlate": {
    "plateNumber": "1-ABC-123",
    "issuingCountry": "BE"
  }
}
```
* **Response DTO (`MemberDto`):**
  * Includes generated `id`, all member details, and `registrationDate` (`LocalDate.now()`).
* **Domain & Service Validation Rules:**
  * All fields (`firstName`, `lastName`, `address`, `telephoneNumber`, `email`, `licensePlate`) are mandatory.
  * `email` must be valid and unique (`400 Bad Request` / `409 Conflict`).
  * `registrationDate` is set server-side using `java.time.LocalDate`.
* **Subtasks:**
  - [ ] Add `member` table DDL in Flyway.
  - [ ] Create `Member` entity with embedded `Address` and `LicensePlateDTO`, and `LocalDate registrationDate`.
  - [ ] Create `MemberRepository`, `MemberService`, and `MemberController` (`POST /members`).
  - [ ] Write RestAssured integration tests for valid registration and invalid/missing fields.

---

### [ME2] Get All Members (Overview)
* **Priority:** Must-Have
* **Branch:** `feat/ME2-get-all-members`
* **Dependencies:** `ME1`
* **User Story:** As a Manager I want to get an overview of all Members showing only `id`, `name`, `licensePlateNumber` (just the number), `telephoneNumber`, `email`, and `registrationDate`.
* **REST Endpoint:**
  * `GET /members` → `200 OK` (Role: `MANAGER`)
* **Response DTO (`List<MemberOverviewDto>`):**
```json
[
  {
    "id": 1,
    "firstName": "Michael",
    "lastName": "Corleone",
    "licensePlateNumber": "1-ABC-123",
    "telephoneNumber": "+32488112233",
    "email": "michael@corleone.org",
    "registrationDate": "2026-10-28"
  }
]
```
* **Subtasks:**
  - [ ] Create `MemberOverviewDto` omitting address and license plate issuing country.
  - [ ] Add `getAllMembers()` in `MemberService` and `GET /members` in `MemberController`.
  - [ ] Write RestAssured integration tests verifying the returned overview fields.

---

### [ME3] Get a Single Member
* **Priority:** Nice-To-Have
* **Branch:** `feat/ME3-get-single-member`
* **Dependencies:** `ME1`
* **User Story:** As a Manager I want to get all data for a single, identified Member.
* **REST Endpoint:**
  * `GET /members/{id}` → `200 OK` (Role: `MANAGER`)
* **Domain & Service Validation Rules:**
  * Throw `MemberNotFoundException` (`404 Not Found`) if `id` does not exist.
* **Subtasks:**
  - [ ] Add `getMemberById(Long id)` in `MemberService` and `GET /members/{id}` in `MemberController`.
  - [ ] Write RestAssured integration tests (200 OK and 404 Not Found).

---

### [ML1] Select a Membership Level
* **Priority:** Must-Have
* **Branch:** `feat/ML1-membership-level`
* **Dependencies:** `ME1`
* **User Story:** As a Member I want to select a Membership level (`BRONZE`, `SILVER`, `GOLD`) upon registration (defaulting to `BRONZE` if omitted).
* **Membership Level Rules (Enum):**
  * `BRONZE`: Monthly cost `€0.00`, Hourly reduction `0%`, Max allocation time `4 hours`
  * `SILVER`: Monthly cost `€10.00`, Hourly reduction `20%`, Max allocation time `6 hours`
  * `GOLD`: Monthly cost `€40.00`, Hourly reduction `30%`, Max allocation time `24 hours`
* **Impact on Endpoints:**
  * `POST /members` (`CreateMemberDto`) accepts optional `"membershipLevel": "SILVER"`. If `null`, defaults to `BRONZE`.
  * `MemberDto` (and `ME3`) includes `membershipLevel` details.
* **Subtasks:**
  - [ ] Add `membership_level` column to `member` table in Flyway (default `'BRONZE'`).
  - [ ] Create `MembershipLevel` enum encapsulating `monthlyCost`, `reductionPercentage`, and `maxAllocationHours`.
  - [ ] Update `Member` entity, `CreateMemberDto`, `MemberDto`, and `MemberService` (defaulting null to `BRONZE`).
  - [ ] Write RestAssured integration tests for explicit `SILVER`/`GOLD` selection and default `BRONZE` fallback.

---

## 6. Functional Stories — Parking Spot Allocations (`allocation`)

### [PSA1] Start Allocating a Parking Spot
* **Priority:** Must-Have
* **Branch:** `feat/PSA1-start-parking-allocation`
* **Dependencies:** `PL1`, `ME1`, `ML1`
* **User Story:** As a Member I want to start allocating a parking spot so that I can enter the parking lot and park my car.
* **REST Endpoint:**
  * `POST /allocations` → `201 Created` (Role: `MEMBER`)
* **Request DTO (`StartAllocationDto`):**
```json
{
  "memberId": 1,
  "parkingLotId": 1,
  "licensePlateNumber": "1-ABC-123"
}
```
* **Response DTO (`AllocationDto`):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "memberId": 1,
  "parkingLotId": 1,
  "licensePlateNumber": "1-ABC-123",
  "startTime": "2026-10-28T14:30:00",
  "stopTime": null,
  "status": "ACTIVE"
}
```
* **Domain & Service Validation Rules:**
  * Member must exist (`404 Not Found` / `400 Bad Request`).
  * Parking lot must exist (`404 Not Found` / `400 Bad Request`).
  * Provided `licensePlateNumber` must match the Member's registered license plate number, **UNLESS** the Member has `MembershipLevel.GOLD` (`400 Bad Request` / `403 Forbidden`).
  * Parking lot must have available capacity (`activeAllocationsCount < parkingLot.maxCapacity`), otherwise throw `ParkingLotFullException` (`400 Bad Request`).
  * Allocation is created with `status = ACTIVE` and `startTime = LocalDateTime.now()`.
* **Subtasks:**
  - [ ] Add `parking_allocation` table DDL in Flyway (`id`, `member_id`, `parking_lot_id`, `license_plate_number`, `start_time`, `stop_time`, `status`).
  - [ ] Create `AllocationStatus` enum (`ACTIVE`, `STOPPED`, `INVOICED`), `ParkingAllocation` entity, and `ParkingAllocationRepository`.
  - [ ] Implement `ParkingAllocationService.startAllocation(...)` enforcing all 4 validation rules.
  - [ ] Create `ParkingAllocationController` (`POST /allocations`) and DTOs.
  - [ ] Write RestAssured integration tests (happy path, Gold member with different plate, Bronze/Silver member with different plate rejected, full parking lot rejected).

---

### [PSA2] Stop Allocating a Parking Spot
* **Priority:** Must-Have
* **Branch:** `feat/PSA2-stop-parking-allocation`
* **Dependencies:** `PSA1`
* **User Story:** As a Member I want to stop allocating a parking spot so that I can leave the parking lot.
* **REST Endpoint:**
  * `PUT /allocations/{allocationId}/stop` (or `PATCH /allocations/{allocationId}/stop`) → `200 OK` (Role: `MEMBER`)
* **Request DTO (`StopAllocationDto`):**
```json
{
  "memberId": 1
}
```
*(Note: Once `TECH-4` is active, `memberId` can also be verified against the authenticated principal).*
* **Domain & Service Validation Rules:**
  * Allocation must exist (`404 Not Found`).
  * Allocation status must be `ACTIVE`; if already `STOPPED` or `INVOICED`, throw `AllocationAlreadyStoppedException` (`400 Bad Request`).
  * Provided `memberId` must be the owner of the allocation; otherwise throw `UnauthorizedAllocationAccessException` (`403 Forbidden` / `400 Bad Request`).
  * Sets `stopTime = LocalDateTime.now()` and `status = STOPPED`.
* **Subtasks:**
  - [ ] Implement `stopAllocation(id, stopAllocationDto)` in `ParkingAllocationService`.
  - [ ] Add `PUT /allocations/{allocationId}/stop` in `ParkingAllocationController`.
  - [ ] Write RestAssured integration tests (happy path, stopping an already stopped allocation, wrong member trying to stop).

---

### [PSA3] Get All Parking Allocations (Filtered & Ordered)
* **Priority:** Must-Have
* **Branch:** `feat/PSA3-get-all-allocations`
* **Dependencies:** `PSA1`, `PSA2`
* **User Story:** As a Manager I want to get an overview of all parking allocations with optional limit, status filter, and sort order.
* **REST Endpoint:**
  * `GET /allocations?status=ACTIVE&order=ASC&limit=100` → `200 OK` (Role: `MANAGER`)
* **Query Parameters (All Optional):**
  * `status`: `ACTIVE` | `STOPPED` (if omitted, return all allocations)
  * `order`: `ASC` (default, by `startTime`) | `DESC`
  * `limit`: Positive integer limiting the number of returned records
* **Subtasks:**
  - [ ] Implement filtered and sorted query in `ParkingAllocationRepository` / `ParkingAllocationService`.
  - [ ] Add `GET /allocations` with `@RequestParam` filters in `ParkingAllocationController`.
  - [ ] Write RestAssured integration tests for default sorting (`ASC`), `DESC` sorting, status filtering (`ACTIVE`/`STOPPED`), and `limit`.

---

### [PSA4] Get All Parking Allocations for a Member
* **Priority:** Nice-To-Have
* **Branch:** `feat/PSA4-get-allocations-by-member`
* **Dependencies:** `PSA3`
* **User Story:** As a Manager I want to get an overview of all parking allocations for a given Member, filtered by status.
* **REST Endpoint:**
  * `GET /members/{memberId}/allocations?status=ACTIVE` (or `GET /allocations?memberId={id}&status=ACTIVE`) → `200 OK` (Role: `MANAGER`)
* **Subtasks:**
  - [ ] Verify member exists (`404 Not Found` if not) and filter allocations by `memberId` and optional `status`.
  - [ ] Write RestAssured integration tests.

---

### [PSA5] Get All Parking Allocations for a Parking Lot
* **Priority:** Nice-To-Have
* **Branch:** `feat/PSA5-get-allocations-by-parking-lot`
* **Dependencies:** `PSA3`
* **User Story:** As a Manager I want to get an overview of all allocations for a given Parking Lot, filtered by status.
* **REST Endpoint:**
  * `GET /parking-lots/{parkingLotId}/allocations?status=STOPPED` → `200 OK` (Role: `MANAGER`)
* **Subtasks:**
  - [ ] Verify parking lot exists (`404 Not Found` if not) and filter allocations by `parkingLotId` and optional `status`.
  - [ ] Write RestAssured integration tests.

---

### [PSA6] Get All Parking Allocations (Including Duration)
* **Priority:** Nice-To-Have
* **Branch:** `feat/PSA6-allocation-duration`
* **Dependencies:** `PSA3`
* **User Story:** As a Manager I want each returned parking allocation to include its duration formatted as `hh:mm:ss`.
* **Calculation Rule:**
  * `duration = Duration.between(startTime, stopTime != null ? stopTime : LocalDateTime.now())` formatted as `HH:mm:ss`.
* **Subtasks:**
  - [ ] Add `duration` field (`HH:mm:ss`) to `AllocationDto` mapper.
  - [ ] Write unit and RestAssured integration tests for both active and stopped allocations.

---

## 7. Functional Stories — Invoicing (`invoice`)

### [INV1] Get Monthly Invoice for a Member
* **Priority:** Nice-To-Have
* **Branch:** `feat/INV1-generate-member-invoice`
* **Dependencies:** `PSA2`, `ML1`
* **User Story:** As a Member I want to generate and get my monthly invoice collecting all my `STOPPED` parking allocations.
* **REST Endpoint:**
  * `POST /members/{memberId}/invoices` (or `GET /members/{memberId}/invoices/current`) → `200 OK` / `201 Created` (Role: `MEMBER` / `MANAGER`)
* **Domain & Pricing Calculation Rules:**
  * Collect all allocations for `memberId` with status `STOPPED` and transition their status to `INVOICED`.
  * For each allocation, compute **whole hours parked (rounded up)**:
    * E.g., `2h 20m` (or even `0h 05m`) rounds up to `3 hours` (or `1 hour`): `Math.ceil(seconds / 3600.0)`.
  * **Base Parking Cost:** `hoursRoundedUp * parkingLot.pricePerHour * (1 - membershipLevel.reductionPercentage)`.
  * **Overstay Fine:** If `hoursRoundedUp > membershipLevel.maxAllocationHours`, add `(hoursRoundedUp - maxAllocationHours) * €2.50`.
  * **Invoice Total Price:** `Sum(InvoiceItem prices) + membershipLevel.monthlyCost`.
  * **Invoice Metadata:** Unique reference number, `creationDate` (`LocalDate.now()`), `expirationDate` (`creationDate.plusDays(30)`), `status = OPEN`, `paymentDate = null`.
* **Subtasks:**
  - [ ] Add `invoice` and `invoice_item` tables in Flyway.
  - [ ] Create `InvoiceStatus` (`OPEN`, `CLOSED`), `Invoice`, and `InvoiceItem` entities.
  - [ ] Write unit tests (TDD) for price rounding, membership discount, and €2.50/hr fine logic.
  - [ ] Implement `InvoiceService` and `InvoiceController` + RestAssured integration tests.

---

### [INV2] Get All Invoices
* **Priority:** Nice-To-Have
* **Branch:** `feat/INV2-get-all-invoices`
* **Dependencies:** `INV1`
* **User Story:** As a Manager I want to get an overview of all Invoices.
* **REST Endpoint:**
  * `GET /invoices` → `200 OK` (Role: `MANAGER`)
* **Subtasks:**
  - [ ] Add `getAllInvoices()` in `InvoiceService` and `GET /invoices` in `InvoiceController`.
  - [ ] Write RestAssured integration tests.

---

### [INV3] Mark an Invoice as Closed
* **Priority:** Nice-To-Have
* **Branch:** `feat/INV3-close-invoice`
* **Dependencies:** `INV1`
* **User Story:** As a Manager I want to mark an Invoice as closed using its unique reference number.
* **REST Endpoint:**
  * `PUT /invoices/{referenceNumber}/close` → `200 OK` (Role: `MANAGER`)
* **Domain & Service Validation Rules:**
  * Find invoice by `referenceNumber` (`404 Not Found` if missing).
  * Set `status = CLOSED` and `paymentDate = LocalDate.now()`.
* **Subtasks:**
  - [ ] Add `closeInvoice(String referenceNumber)` in `InvoiceService` and endpoint in `InvoiceController`.
  - [ ] Write RestAssured integration tests verifying status change to `CLOSED` and populated `paymentDate`.