# Smart Campus API — 5-Day Implementation Plan

## Overview

**Coursework**: "Smart Campus" Sensor & Room Management REST API  
**Module**: 5COSC022W Client-Server Architectures  
**Tech Stack**: JAX-RS (Jersey) + Maven + Tomcat Server — **NO Spring Boot, NO Database**  
**Deadline**: April 24, 2026 at 13:00  

### Key Rules
- Must use **JAX-RS only** (Jersey implementation). Spring Boot = ZERO.
- Must use **in-memory data structures** (HashMap/ArrayList). Any database = ZERO.
- Must be hosted on a **public GitHub repo**. ZIP submission = ZERO.
- Video demo (max 10 min) is mandatory — 30% of each task's marks.
- Report answers go in **README.md** on GitHub — 20% of each task's marks.

---

## Marking Breakdown

| Part | Topic | Marks | Coding (50%) | Video (30%) | Report (20%) |
|------|-------|-------|-------------|-------------|--------------|
| 1 | Setup & Discovery | 10 | 5 | 3 | 2 |
| 2 | Room Management | 20 | 10 | 6 | 4 |
| 3 | Sensors & Filtering | 20 | 10 | 6 | 4 |
| 4 | Sub-Resources (Readings) | 20 | 10 | 6 | 4 |
| 5 | Error Handling & Logging | 30 | 15 | 9 | 6 |
| **Total** | | **100** | **50** | **30** | **20** |

---

## Day 1 — Foundation & Project Setup (Part 1: 10 Marks)

### Goal
Get a working JAX-RS project running on Tomcat with the discovery endpoint.

### Tasks

#### 1.1 Maven Project Bootstrap
- Create `pom.xml` with WAR packaging
- Dependencies:
  - `jersey-container-servlet` (Jersey on Tomcat)
  - `jersey-media-json-jackson` (JSON serialization)
  - `jersey-hk2` (dependency injection)
  - `javax.servlet-api` (provided by Tomcat)
- Add `tomcat7-maven-plugin` for embedded Tomcat during development

#### 1.2 POJO Data Models
- Package: `com.smartcampus.model`
- `Room.java` — id, name, capacity, sensorIds (ArrayList)
- `Sensor.java` — id, type, status, currentValue, roomId
- `SensorReading.java` — id, timestamp, value
- All with constructors, getters, setters

#### 1.3 In-Memory Data Store
- `com.smartcampus.repository.DataStore.java`
- Static `ConcurrentHashMap` for rooms, sensors, readings
- Pre-populated with sample data

#### 1.4 Application Configuration
- `SmartCampusApplication.java` extending `ResourceConfig` (subclass of `javax.ws.rs.core.Application`)
- `@ApplicationPath("/api/v1")`
- `web.xml` configuring Jersey servlet on Tomcat

#### 1.5 Discovery Endpoint
- `DiscoveryResource.java` at `GET /api/v1`
- Returns JSON with: API version, admin contact, resource links (HATEOAS)

### Commits
```
docs: add 5-day implementation plan
feat: initialize Maven project with Jersey + Tomcat configuration
feat: add POJO data models and in-memory data store
feat: implement discovery endpoint at GET /api/v1
```

---

## Day 2 — Room Management (Part 2: 20 Marks)

### Goal
Full CRUD for Rooms including the delete-safety business logic.

### Tasks

#### 2.1 Room Resource — Basic CRUD
- `RoomResource.java` at path `/rooms`
- `GET /` — return list of all rooms
- `POST /` — create room, return `201 Created` + `Location` header
- `GET /{roomId}` — return single room or `404`

#### 2.2 Room Update
- `PUT /{roomId}` — update room name/capacity

#### 2.3 Room Deletion with Safety Logic
- `DELETE /{roomId}` — delete room by ID
- **Business rule**: Block deletion if room has sensors → respond with 409 (stub error for now)
- If no sensors → `204 No Content`
- If room not found → `404 Not Found`

#### 2.4 Input Validation
- Validate name not null/empty, capacity > 0 on POST
- Return `400 Bad Request` on invalid input

### Commits
```
feat: implement GET /rooms and GET /rooms/{id}
feat: implement POST /rooms with validation
feat: implement PUT /rooms/{id} for room updates
feat: implement DELETE /rooms/{id} with sensor safety check
```

---

## Day 3 — Sensor Operations & Linking (Part 3: 20 Marks)

### Goal
Sensor CRUD with room-existence validation and filtered search.

### Tasks

#### 3.1 Sensor Resource — CRUD
- `SensorResource.java` at path `/sensors`
- `GET /` — return all sensors
- `GET /{sensorId}` — return single sensor
- `POST /` — register sensor with `@Consumes(APPLICATION_JSON)`
  - Validate roomId exists → 422 if not
  - Add sensor ID to room's sensorIds list → `201 Created`
- `PUT /{sensorId}` — update sensor
- `DELETE /{sensorId}` — remove sensor + unlink from room

#### 3.2 Filtered Retrieval
- `GET /sensors?type=CO2` using `@QueryParam("type")`
- Case-insensitive matching
- Return all if type not provided

#### 3.3 Cross-Link Integrity
- On create: add sensor ID to room's sensorIds
- On delete: remove sensor ID from room's sensorIds

### Commits
```
feat: implement Sensor CRUD with room validation
feat: add filtered sensor retrieval with @QueryParam type
feat: ensure Room-Sensor cross-link integrity
```

---

## Day 4 — Sub-Resources & Error Handling (Parts 4 + 5: 50 Marks)

### Goal
Sensor readings as sub-resources + full exception mapping + logging.

### Tasks

#### 4.1 Sub-Resource Locator for Readings (Part 4)
- In SensorResource: `@Path("{sensorId}/readings")` sub-resource locator
- `SensorReadingResource.java` (no `@Path` at class level)
- `GET /` — return all readings for sensor
- `POST /` — add reading (UUID id, auto-timestamp)
  - **Side effect**: update parent sensor's `currentValue`
  - If sensor status is MAINTENANCE → throw `SensorUnavailableException` (403)

#### 4.2 Custom Exceptions (Part 5)
- `RoomNotEmptyException` → 409 Conflict
- `LinkedResourceNotFoundException` → 422 Unprocessable Entity
- `SensorUnavailableException` → 403 Forbidden
- `ResourceNotFoundException` → 404 Not Found

#### 4.3 Exception Mappers (Part 5)
- `RoomNotEmptyExceptionMapper` → 409
- `LinkedResourceNotFoundExceptionMapper` → 422
- `SensorUnavailableExceptionMapper` → 403
- `ResourceNotFoundExceptionMapper` → 404
- `GenericExceptionMapper` (Throwable) → 500 (no stack traces!)

All return consistent JSON:
```json
{
  "error": "CONFLICT",
  "message": "Room LIB-301 cannot be deleted: 2 active sensors assigned.",
  "status": 409
}
```

#### 4.4 Logging Filter (Part 5)
- `LoggingFilter.java` implementing `ContainerRequestFilter` + `ContainerResponseFilter`
- Log HTTP method + URI on request
- Log status code on response
- Use `java.util.logging.Logger`

#### 4.5 Wire Everything
- Replace all stubbed error responses in Day 2/3 code with proper custom exceptions
- Register all mappers and filters

### Commits
```
feat: implement SensorReadingResource as sub-resource
feat: add custom exceptions for business logic violations
feat: implement exception mappers (409, 422, 403, 404, 500)
feat: add request/response logging filter
refactor: wire custom exceptions into Room and Sensor resources
```

---

## Day 5 — README Report, Polish & Final Testing (Documentation: 20%)

### Goal
Write README.md report with coursework answers, test everything, prepare for video.

### Tasks

#### 5.1 README.md Report
1. **API Overview**
2. **Build & Run Instructions** (prerequisites, clone, mvn, deploy to Tomcat)
3. **API Endpoints Table** (all methods, paths, descriptions)
4. **5+ Sample curl Commands**:
   - `GET /api/v1` (discovery)
   - `POST /api/v1/rooms` (create room)
   - `GET /api/v1/sensors?type=Temperature` (filtered)
   - `POST /api/v1/sensors/{id}/readings` (add reading)
   - `DELETE /api/v1/rooms/{id}` (conflict scenario)
5. **Coursework Answers** organized by Part:
   - Part 1: JAX-RS lifecycle, HATEOAS benefits
   - Part 2: Full objects vs IDs, DELETE idempotency
   - Part 3: @Consumes mismatch (415), @QueryParam vs path
   - Part 4: Sub-resource locator pattern benefits
   - Part 5: 422 vs 404, stack trace security risks, filter benefits

#### 5.2 Final Testing
- Test every endpoint with Postman
- Verify all error codes
- Ensure no stack traces leak

#### 5.3 Code Cleanup
- JavaDoc comments, formatting, remove debug code

#### 5.4 Video Preparation
- Plan Postman demo flow, test camera/mic, record ≤ 10 min

### Commits
```
docs: add comprehensive README with API overview and build instructions
docs: add coursework report answers for Parts 1-5
docs: add sample curl commands
chore: final code cleanup and comments
```

---

## Final Project Structure

```
SmartCampusAPI/
├── pom.xml
├── README.md
├── PLAN.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── smartcampus/
        │           ├── SmartCampusApplication.java
        │           ├── model/
        │           │   ├── Room.java
        │           │   ├── Sensor.java
        │           │   └── SensorReading.java
        │           ├── repository/
        │           │   └── DataStore.java
        │           ├── resource/
        │           │   ├── DiscoveryResource.java
        │           │   ├── RoomResource.java
        │           │   ├── SensorResource.java
        │           │   └── SensorReadingResource.java
        │           ├── exception/
        │           │   ├── ResourceNotFoundException.java
        │           │   ├── RoomNotEmptyException.java
        │           │   ├── LinkedResourceNotFoundException.java
        │           │   └── SensorUnavailableException.java
        │           ├── exception/mapper/
        │           │   ├── ResourceNotFoundExceptionMapper.java
        │           │   ├── RoomNotEmptyExceptionMapper.java
        │           │   ├── LinkedResourceNotFoundExceptionMapper.java
        │           │   ├── SensorUnavailableExceptionMapper.java
        │           │   └── GenericExceptionMapper.java
        │           └── filter/
        │               └── LoggingFilter.java
        └── webapp/
            └── WEB-INF/
                └── web.xml
```

---

## Schedule

| Day | Suggested Date | Focus | Marks Covered |
|-----|---------------|-------|---------------|
| 1 | Apr 13 (Sun) | Project setup, models, discovery | Part 1 (10) |
| 2 | Apr 14 (Mon) | Room CRUD + delete safety | Part 2 (20) |
| 3 | Apr 15 (Tue) | Sensor CRUD + filtering | Part 3 (20) |
| 4 | Apr 16 (Wed) | Sub-resources + errors + logging | Parts 4+5 (50) |
| 5 | Apr 17 (Thu) | README report + testing + polish | Documentation |

> This leaves 7 days (Apr 18-24) as buffer for video recording, fixes, and final review.
