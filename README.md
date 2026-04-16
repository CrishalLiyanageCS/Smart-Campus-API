# Smart Campus API

**Module**: 5COSC022W — Client-Server Architectures  
**Student**: Crishal Liyanage  
**Technology Stack**: JAX-RS (Jersey 2.41) · Maven · Apache Tomcat  
**Constraints**: No Spring Boot · No Database — In-memory data structures only

---

## 1. API Overview

The Smart Campus API is a RESTful web service designed to manage university campus infrastructure — specifically **rooms**, **sensors**, and **sensor readings**. The system models a real-world IoT scenario where physical rooms on campus contain deployed sensor devices (temperature, CO2, occupancy), and those sensors continuously produce time-series measurement readings.

### Architecture Summary

The API follows a **layered architecture** with clear separation of concerns:

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **Resources** | `com.smartcampus.resource` | JAX-RS endpoint classes handling HTTP request/response mapping |
| **Models** | `com.smartcampus.model` | Plain Old Java Objects (POJOs) representing domain entities |
| **Repository** | `com.smartcampus.repository` | Thread-safe in-memory data persistence using `ConcurrentHashMap` |
| **Exceptions** | `com.smartcampus.exception` | Custom business-logic exceptions with dedicated ExceptionMappers |
| **Filters** | `com.smartcampus.filter` | Cross-cutting request/response logging via JAX-RS filters |

### Key Design Decisions

- **In-memory storage**: All data is stored in static `ConcurrentHashMap` collections within `DataStore.java`. This satisfies the coursework constraint of no external database while providing thread-safe concurrent access, since JAX-RS creates a new resource class instance per request.
- **Cross-link integrity**: Rooms maintain an `ArrayList<String>` of sensor IDs (`sensorIds`), while each sensor holds a `roomId` foreign key. The API enforces referential integrity on every create, update, and delete operation.
- **Sub-resource pattern**: Sensor readings are modelled as a sub-resource of sensors (`/sensors/{id}/readings`), reflecting the natural parent-child ownership relationship.
- **Consistent error format**: All error responses across the API follow an identical JSON structure with `error`, `message`, and `status` fields.

### Project Structure

```
SmartCampusAPI/
├── pom.xml                                           # Maven config — Jersey + Tomcat dependencies
├── README.md                                         # This file — API overview, build instructions, report
└── src/main/
    ├── java/com/smartcampus/
    │   ├── SmartCampusApplication.java               # JAX-RS app config (@ApplicationPath("/api/v1"))
    │   ├── model/
    │   │   ├── Room.java                             # Room POJO (id, name, capacity, sensorIds)
    │   │   ├── Sensor.java                           # Sensor POJO (id, type, status, currentValue, roomId)
    │   │   └── SensorReading.java                    # Reading POJO (id, timestamp, value)
    │   ├── repository/
    │   │   └── DataStore.java                        # In-memory ConcurrentHashMap store (no database)
    │   ├── resource/
    │   │   ├── DiscoveryResource.java                # GET /api/v1 — HATEOAS entry point
    │   │   ├── RoomResource.java                     # CRUD for /api/v1/rooms
    │   │   ├── SensorResource.java                   # CRUD for /api/v1/sensors + sub-resource locator
    │   │   └── SensorReadingResource.java            # Sub-resource for /api/v1/sensors/{id}/readings
    │   ├── exception/
    │   │   ├── ResourceNotFoundException.java        # Thrown on missing room/sensor → 404
    │   │   ├── RoomNotEmptyException.java            # Thrown on DELETE room with sensors → 409
    │   │   ├── LinkedResourceNotFoundException.java  # Thrown on invalid roomId reference → 422
    │   │   └── SensorUnavailableException.java       # Thrown on reading to MAINTENANCE sensor → 403
    │   ├── exception/mapper/
    │   │   ├── ResourceNotFoundExceptionMapper.java  # @Provider → HTTP 404
    │   │   ├── RoomNotEmptyExceptionMapper.java      # @Provider → HTTP 409
    │   │   ├── LinkedResourceNotFoundExceptionMapper.java  # @Provider → HTTP 422
    │   │   ├── SensorUnavailableExceptionMapper.java # @Provider → HTTP 403
    │   │   └── GenericExceptionMapper.java           # @Provider → HTTP 500 catch-all (no stack trace)
    │   └── filter/
    │       └── LoggingFilter.java                    # @Provider — logs every request + response
    └── webapp/WEB-INF/
        └── web.xml                                   # Jersey servlet config — maps /api/v1/*
```

### Pre-loaded Sample Data

The `DataStore` is pre-populated with sample data for immediate testing:

| Entity | ID | Details |
|--------|----|---------|
| Room | `LIB-301` | Library Quiet Study (capacity: 50) — has 2 sensors |
| Room | `ENG-102` | Engineering Lab A (capacity: 30) — has 2 sensors |
| Room | `SCI-201` | Science Lecture Hall (capacity: 120) — has 0 sensors (useful for DELETE testing) |
| Sensor | `TEMP-001` | Temperature sensor in LIB-301, status: ACTIVE, value: 22.5 |
| Sensor | `CO2-001` | CO2 sensor in LIB-301, status: ACTIVE, value: 415.0 |
| Sensor | `OCC-001` | Occupancy sensor in ENG-102, status: ACTIVE, value: 15.0 |
| Sensor | `TEMP-002` | Temperature sensor in ENG-102, status: **MAINTENANCE**, value: 0.0 |

---

## 2. Build & Run Instructions

### Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java Development Kit (JDK) | 11 or higher | Compiling and running Java source code |
| Apache Maven | 3.6+ | Build automation and dependency management |
| Apache Tomcat | 7+ (or use embedded plugin) | Servlet container for deploying the WAR |

### Option A — Run with NetBeans (Required for Markers)

> **Note**: The module lecturer has confirmed that markers will use **NetBeans only**. Please follow this option to open and run the project.

**Prerequisites:**
- NetBeans IDE 12 or higher (with Maven support built in)
- Apache Tomcat 9 or 10 registered in NetBeans
- JDK 11 or higher

**Steps:**

1. **Clone the repository**
   ```
   git clone https://github.com/CrishalLiyanageCS/Smart-Campus-API.git
   ```

2. **Open the project in NetBeans**
   - Launch NetBeans
   - Go to **File → Open Project**
   - Navigate to the cloned `Smart-Campus-API` folder
   - NetBeans will recognise it automatically as a Maven project — click **Open Project**

3. **Register Apache Tomcat (first time only)**
   - Go to **Tools → Servers → Add Server**
   - Select **Apache Tomcat** and provide the path to your Tomcat installation
   - Click **Finish**

4. **Run the project**
   - Right-click the project in the Projects panel
   - Select **Run** (or press **F6**)
   - NetBeans will build the WAR and deploy it to Tomcat automatically

5. **Verify the server is running**
   - Open a browser or Postman
   - Navigate to: `http://localhost:8080/SmartCampusAPI/api/v1`
   - You should see the Discovery endpoint JSON response

### Option B — Run with Embedded Tomcat (Command Line)

```bash
# 1. Clone the repository
git clone https://github.com/CrishalLiyanageCS/Smart-Campus-API.git
cd Smart-Campus-API

# 2. Build and run in one command
mvn clean compile tomcat7:run
```

The API will start at: **`http://localhost:8080/api/v1`**

### Option C — Deploy to External Tomcat

```bash
# 1. Build the WAR file
mvn clean package

# 2. Copy the WAR to Tomcat
cp target/smartcampus.war /path/to/tomcat/webapps/

# 3. Start Tomcat
/path/to/tomcat/bin/startup.sh    # Linux/Mac
/path/to/tomcat/bin/startup.bat   # Windows
```

The API will be available at: **`http://localhost:8080/smartcampus/api/v1`**

### Configuration Files

- **`pom.xml`** — Maven project descriptor with WAR packaging. Declares dependencies for `jersey-container-servlet`, `jersey-media-json-jackson` (JSON via Jackson), `jersey-hk2` (dependency injection), and `javax.servlet-api` (provided scope, supplied by Tomcat at runtime). Includes the `tomcat7-maven-plugin` for embedded development server.
- **`web.xml`** — Configures Jersey's `ServletContainer` servlet, mapping it to the `/api/v1/*` URL pattern and pointing it to the `SmartCampusApplication` class.
- **`SmartCampusApplication.java`** — Extends Jersey's `ResourceConfig` (a subclass of `javax.ws.rs.core.Application`) with `@ApplicationPath("/api/v1")`. Calls `packages("com.smartcampus")` to enable automatic classpath scanning, which discovers all `@Path`-annotated resource classes, `@Provider`-annotated exception mappers, and filters at startup.

---

## 3. API Endpoints Table

### Discovery

| Method | Path | Description | Response Codes |
|--------|------|-------------|----------------|
| `GET` | `/api/v1` | API discovery endpoint returning version info, admin contact, and HATEOAS links to available resource collections | `200 OK` |

### Rooms (`/api/v1/rooms`)

| Method | Path | Description | Response Codes |
|--------|------|-------------|----------------|
| `GET` | `/api/v1/rooms` | Retrieve all rooms | `200 OK` |
| `GET` | `/api/v1/rooms/{roomId}` | Retrieve a specific room by its ID | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/rooms` | Create a new room (requires `name` and `capacity` > 0) | `201 Created` + `Location` header, `400 Bad Request` |
| `PUT` | `/api/v1/rooms/{roomId}` | Update an existing room's name and/or capacity | `200 OK`, `404 Not Found` |
| `DELETE` | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors are still assigned) | `204 No Content`, `404 Not Found`, `409 Conflict` |

### Sensors (`/api/v1/sensors`)

| Method | Path | Description | Response Codes |
|--------|------|-------------|----------------|
| `GET` | `/api/v1/sensors` | Retrieve all sensors. Supports optional `?type=` query parameter for case-insensitive filtering (e.g., `?type=Temperature`) | `200 OK` |
| `GET` | `/api/v1/sensors/{sensorId}` | Retrieve a specific sensor by its ID | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/sensors` | Register a new sensor. The `roomId` must reference an existing room. The sensor ID is automatically added to the room's `sensorIds` list | `201 Created` + `Location` header, `400 Bad Request`, `422 Unprocessable Entity` |
| `PUT` | `/api/v1/sensors/{sensorId}` | Update a sensor. If `roomId` changes, sensor is re-linked from old room to new room | `200 OK`, `404 Not Found`, `422 Unprocessable Entity` |
| `DELETE` | `/api/v1/sensors/{sensorId}` | Remove a sensor. Automatically unlinks it from its parent room and deletes all associated readings | `204 No Content`, `404 Not Found` |

### Sensor Readings — Sub-Resource (`/api/v1/sensors/{sensorId}/readings`)

| Method | Path | Description | Response Codes |
|--------|------|-------------|----------------|
| `GET` | `/api/v1/sensors/{sensorId}/readings` | Retrieve all readings recorded for the specified sensor | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/sensors/{sensorId}/readings` | Add a new reading. Auto-generates UUID and timestamp. Updates the parent sensor's `currentValue`. Blocked if sensor status is `MAINTENANCE` | `201 Created` + `Location` header, `404 Not Found`, `403 Forbidden` |

---

## 4. Sample curl Commands

### 4.1 Discovery — Explore the API Entry Point
```bash
curl -X GET http://localhost:8080/api/v1 \
  -H "Accept: application/json"
```
Expected response (200 OK):
```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "description": "RESTful API for managing campus rooms, sensors, and sensor readings",
  "contact": {
    "name": "Smart Campus Administrator",
    "email": "admin@smartcampus.westminster.ac.uk"
  },
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

### 4.2 Create a New Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Computer Lab 4", "capacity": 35}'
```
Expected response (201 Created) — note the auto-generated ID and `Location` header:
```json
{
  "id": "ROOM-1713045600000",
  "name": "Computer Lab 4",
  "capacity": 35,
  "sensorIds": []
}
```

### 4.3 Get Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature" \
  -H "Accept: application/json"
```
Expected response (200 OK) — returns only temperature sensors (case-insensitive match):
```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  },
  {
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "MAINTENANCE",
    "currentValue": 0.0,
    "roomId": "ENG-102"
  }
]
```

### 4.4 Add a Sensor Reading (Sub-Resource POST)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.5}'
```
Expected response (201 Created) — ID and timestamp are auto-generated, parent sensor's `currentValue` is updated to 24.5:
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": 1713045600000,
  "value": 24.5
}
```

### 4.5 Attempt to Delete a Room with Active Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 -v
```
Expected response (409 Conflict) — deletion is blocked because LIB-301 has TEMP-001 and CO2-001 assigned:
```json
{
  "error": "CONFLICT",
  "message": "Room LIB-301 cannot be deleted: 2 active sensor(s) assigned.",
  "status": 409
}
```

### 4.6 Post Reading to a Sensor in MAINTENANCE (403 Forbidden)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 19.0}'
```
Expected response (403 Forbidden):
```json
{
  "error": "FORBIDDEN",
  "message": "Sensor 'TEMP-002' is currently under MAINTENANCE and cannot accept readings.",
  "status": 403
}
```

### 4.7 Retrieve a Non-Existent Resource (404 Not Found)
```bash
curl -X GET http://localhost:8080/api/v1/rooms/INVALID-ID
```
Expected response (404 Not Found):
```json
{
  "error": "NOT_FOUND",
  "message": "Room with ID 'INVALID-ID' was not found.",
  "status": 404
}
```

---

## 5. Coursework Answers

### Part 1: Setup & Discovery

**Q: Explain the JAX-RS resource lifecycle and how it influences your data storage design.**

In JAX-RS, resource classes follow a **per-request lifecycle** by default. Each time an HTTP request arrives, the Jersey runtime creates a **new instance** of the resource class (e.g., `RoomResource`), invokes the matched method, and then discards that instance after the response is sent. This means instance-level fields on a resource class are **not shared** across requests — any data stored in an instance variable would be lost after the request completes.

This lifecycle directly influenced our data storage design. Since resource instances are transient, we needed a **static, shared data store** that persists independently of any single request. The `DataStore` class uses `static final ConcurrentHashMap` fields that are initialized once when the class is first loaded by the JVM and remain in memory for the lifetime of the application. We chose `ConcurrentHashMap` specifically (rather than a regular `HashMap`) because JAX-RS servers such as Tomcat use a **thread pool** to process multiple requests concurrently. Without thread-safe collections, simultaneous PUT and DELETE operations on the same resource could cause race conditions, corrupted data, or `ConcurrentModificationException` errors. The `ConcurrentHashMap` provides atomic operations like `put()`, `get()`, `remove()`, and `computeIfAbsent()` that are safe to call from multiple threads without external synchronization.

**Q: What are the benefits of the HATEOAS approach used in the discovery endpoint?**

HATEOAS (Hypermedia As The Engine Of Application State) is a REST architectural constraint where the server provides **navigational links** alongside data, allowing clients to discover available actions dynamically rather than having URLs hard-coded.

In our implementation, the `DiscoveryResource` at `GET /api/v1` returns a `resources` map containing links to `/api/v1/rooms` and `/api/v1/sensors`. This provides several benefits:

1. **Loose coupling**: Client applications do not need to construct or hard-code API URLs. A client can call the discovery endpoint once and navigate the entire API by following the provided links, similar to how a human navigates a website by clicking hyperlinks.
2. **Evolvability**: If the API's URI structure changes in a future version (e.g., `/api/v2/rooms` or renaming `/sensors` to `/devices`), only the discovery endpoint's link map needs updating. Clients that discover links dynamically will adapt automatically without code changes.
3. **Self-documentation**: The discovery response serves as a machine-readable API catalogue. New developers or automated tools can inspect the root endpoint to understand what resources are available and how to reach them, reducing the time to integrate.
4. **Single entry point**: Instead of requiring clients to know multiple URLs upfront, they only need the API root (`/api/v1`) — every other resource is reachable from there.

---

### Part 2: Room Management

**Q: Why store sensor IDs in a room instead of full Sensor objects?**

The `Room` model maintains a `List<String> sensorIds` rather than a `List<Sensor>` of full objects. This is an intentional **data normalisation** design decision with several important justifications:

1. **Single source of truth**: The canonical representation of a sensor lives in the sensor store (`DataStore.getSensors()`). If we embedded full `Sensor` objects inside `Room`, we would have **duplicate copies** of the same sensor data. When a sensor's status changes from `ACTIVE` to `MAINTENANCE`, we would need to update it in two places — once in the sensor store and once inside the room's embedded list. This duplication introduces the risk of **data inconsistency** bugs where the sensor store and the room's copy show different values.

2. **Avoids circular reference serialization issues**: A `Sensor` has a `roomId` field pointing back to its parent room. If `Room` contained full `Sensor` objects and `Sensor` contained a full `Room` object, Jackson's JSON serializer would enter an **infinite recursive loop** (Room → Sensor → Room → Sensor → ...), resulting in a `StackOverflowError`. While Jackson annotations like `@JsonManagedReference` / `@JsonBackReference` can mitigate this, storing only IDs eliminates the problem entirely and keeps the model simple.

3. **Reduced payload size**: When listing all rooms via `GET /api/v1/rooms`, embedding full sensor objects would significantly inflate the response payload. With ID references, the room response remains lightweight, and clients can selectively fetch sensor details via `/api/v1/sensors/{id}` only when needed.

4. **Independent resource lifecycles**: Rooms and sensors have independent CRUD lifecycles. A sensor can be updated (e.g., its `currentValue` changes with every new reading) without needing to modify the room. Storing only IDs ensures that each resource is managed through its own dedicated endpoint.

**Q: How does your API ensure DELETE idempotency?**

In HTTP semantics, the `DELETE` method is defined as **idempotent** — calling it multiple times with the same parameters should leave the server in the same final state. Our implementation achieves this naturally:

- **First `DELETE /rooms/SCI-201` call**: The room exists and has no sensors attached, so it is removed from the data store. The server responds with `204 No Content`.
- **Second `DELETE /rooms/SCI-201` call**: The room no longer exists in the data store. The lookup returns `null`, and the resource throws a `ResourceNotFoundException`, which is mapped to `404 Not Found`.
- **All subsequent calls**: Continue to return `404 Not Found`.

The key insight is that the **server state** is the same after the first and second calls — the room does not exist. The response code differs (204 vs 404), but this is correct HTTP behavior: `404` accurately communicates that the target resource is already gone. The idempotency property holds because repeating the request does not cause additional side effects — no further data is deleted, no state changes occur beyond the initial removal.

---

### Part 3: Sensors & Filtering

**Q: What happens if a client sends a non-JSON content type to an endpoint annotated with `@Consumes(APPLICATION_JSON)`?**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on our `SensorResource` class acts as a **content type contract** enforced entirely by the JAX-RS runtime, before the resource method is even invoked. The processing works as follows:

1. The client sends a `POST /api/v1/sensors` request with `Content-Type: application/xml` (or `text/plain`, or any type other than `application/json`).
2. During the **request matching phase**, Jersey checks the incoming `Content-Type` header against the `@Consumes` annotation of the matched resource method.
3. If there is a **mismatch**, Jersey immediately returns an HTTP `415 Unsupported Media Type` response. The resource method's Java code is **never executed**.
4. The response body is generated by the JAX-RS runtime (or our `GenericExceptionMapper` if configured) and typically contains an error message indicating the unsupported media type.

This annotation-driven approach provides several benefits: it separates content negotiation concerns from business logic, ensures consistent enforcement across all endpoints, and protects against malformed or unexpected payload formats reaching the deserialization layer where they could cause parsing errors.

**Q: Explain the difference between `@QueryParam` and `@PathParam` and when to use each.**

Both annotations bind values from the HTTP request URI to Java method parameters, but they serve fundamentally different semantic purposes:

**`@PathParam`** extracts values from the **URI path segments** and identifies a **specific resource** in the collection hierarchy. For example, in `GET /api/v1/sensors/TEMP-001`, the path parameter `TEMP-001` uniquely identifies one sensor. Path parameters are **mandatory** — the request cannot match the route without them. They model the **resource identity** and reflect the hierarchical structure of the API (collections → individual items).

**`@QueryParam`** extracts values from the **query string** (after the `?`) and modifies **how** the resource representation is retrieved. For example, in `GET /api/v1/sensors?type=CO2`, the query parameter `type` does not identify a specific sensor; it **filters the collection**. Query parameters are inherently **optional** — if `type` is omitted, the endpoint returns all sensors unfiltered. They model **refinements** such as filtering, sorting, pagination, or selecting specific fields.

In our implementation, `SensorResource.getAllSensors(@QueryParam("type") String type)` uses `@QueryParam` because the `type` parameter is optional and acts as a filter on the full sensor collection. The filtering is implemented using Java streams with `.equalsIgnoreCase()` for case-insensitive matching, so `?type=co2`, `?type=CO2`, and `?type=Co2` all return the same results.

---

### Part 4: Sub-Resources

**Q: What are the benefits of the sub-resource locator pattern used for sensor readings?**

The JAX-RS sub-resource locator pattern is used to model the **hierarchical ownership** relationship between sensors and their readings. Instead of defining all reading-related endpoints within `SensorResource`, we delegate to a separate `SensorReadingResource` class via a locator method:

```java
// In SensorResource.java — this method has @Path but NO HTTP method annotation
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

The `SensorReadingResource` class has **no `@Path` annotation at the class level** because its URL context is entirely determined by the parent's locator method. Jersey calls the locator method, receives the sub-resource instance, and then matches the incoming HTTP method (`GET` or `POST`) against the sub-resource's annotated methods.

**Benefits of this pattern:**

1. **Separation of concerns**: `SensorResource` handles sensor CRUD (GET/POST/PUT/DELETE on `/sensors`), while `SensorReadingResource` exclusively handles reading operations (`GET`/`POST` on `/sensors/{id}/readings`). Each class has a single, focused responsibility, adhering to the **Single Responsibility Principle**.

2. **Encapsulated context**: The sub-resource constructor receives the `sensorId` as a parameter, establishing the parent context. All methods within `SensorReadingResource` operate within the scope of that specific sensor, eliminating the need to repeatedly extract and validate the sensor ID in every method.

3. **Clean URI hierarchy**: The URL structure `/sensors/{sensorId}/readings` naturally expresses that readings **belong to** a sensor. This follows REST best practices where the URI hierarchy mirrors the domain model's ownership relationship. Clients intuitively understand that a reading does not exist independently — it is always in the context of a specific sensor.

4. **Modularity and maintainability**: As the API grows (e.g., adding `GET /sensors/{id}/readings/{readingId}`, aggregation endpoints, or pagination), the new logic is contained within `SensorReadingResource` without increasing the complexity of `SensorResource`. Each class remains manageable, testable, and independently modifiable.

5. **Business logic isolation**: The sub-resource enforces its own business rules. For example, `SensorReadingResource.addReading()` checks whether the parent sensor is in `MAINTENANCE` status and throws a `SensorUnavailableException` (403 Forbidden) if so. This rule is specific to readings and appropriately encapsulated within the readings sub-resource rather than cluttering the main sensor resource.

---

### Part 5: Error Handling & Logging

**Q: Explain the semantic difference between HTTP 422 and HTTP 404 and when each is appropriate.**

These two status codes address fundamentally different error scenarios:

**HTTP 404 Not Found** indicates that the **target URI** does not correspond to any existing resource. The client is trying to access a specific entity that does not exist in the system. For example:
- `GET /api/v1/rooms/INVALID-ID` — there is no room with ID "INVALID-ID".
- `DELETE /api/v1/sensors/NONEXISTENT` — the sensor cannot be deleted because it does not exist.

In our system, this is handled by throwing `ResourceNotFoundException`, which the `ResourceNotFoundExceptionMapper` converts to a 404 response.

**HTTP 422 Unprocessable Entity** indicates that the request body is **syntactically valid** (it is well-formed JSON that can be parsed), but it is **semantically invalid** — the data violates a business rule or references a non-existent related entity. For example:
- `POST /api/v1/sensors` with `{"type": "Temperature", "roomId": "FAKE-ROOM"}` — the JSON is valid, but the referenced room does not exist.
- `PUT /api/v1/sensors/TEMP-001` with `{"roomId": "NONEXISTENT"}` — the sensor exists, but the new room it is being moved to does not.

In our system, this is handled by throwing `LinkedResourceNotFoundException`, which the `LinkedResourceNotFoundExceptionMapper` converts to a 422 response.

The critical distinction: **404 means the URL target is missing** (the thing you are trying to access does not exist), while **422 means the request payload references something that is missing** (the thing your data points to does not exist). Using the correct status code enables clients to programmatically distinguish between these scenarios and respond appropriately — for example, a 422 might prompt the client to verify the `roomId` before retrying.

**Q: Why is it a security risk to expose stack traces in API error responses?**

When an unhandled exception occurs, the default behavior of many web frameworks is to return the full Java stack trace in the HTTP response body. This practice is **dangerous in production** for several reasons:

1. **Technology disclosure**: Stack traces reveal the programming language (Java), framework (Jersey/JAX-RS), and library versions. Attackers can use this information to search for **known vulnerabilities (CVEs)** specific to those versions.

2. **Internal path exposure**: Stack traces contain fully qualified class names and file paths (e.g., `com.smartcampus.repository.DataStore.getRoom(DataStore.java:71)`), revealing the internal package structure, class naming conventions, and potentially deployment directory paths. This gives attackers a **map of the codebase**.

3. **Logic disclosure**: The sequence of method calls in a stack trace reveals the **internal execution flow**, showing which methods are called, in what order, and from which classes. This can expose business logic, security check sequences, or database query patterns that an attacker could exploit.

4. **Data leakage**: Exception messages may inadvertently contain sensitive data such as SQL queries, connection strings, internal IP addresses, or user data that was being processed when the error occurred.

Our implementation prevents this through the `GenericExceptionMapper`, which catches all unhandled `Throwable` exceptions. It logs the **full exception with stack trace server-side** using `java.util.logging` (for developer debugging) but returns only a **safe, generic message** to the client: `"An unexpected error occurred. Please try again later."` with a 500 status code. No internal details are ever transmitted over the network.

**Q: What are the benefits of using a JAX-RS filter for logging instead of adding logging code to each resource method?**

Our `LoggingFilter` implements both `ContainerRequestFilter` and `ContainerResponseFilter`, which are JAX-RS interfaces that allow code to execute **before** and **after** every resource method invocation respectively. The filter is registered globally via the `@Provider` annotation and Jersey's package scanning.

The filter-based approach provides the following benefits over manual logging in each resource:

1. **Cross-cutting concern separation**: Logging is a **cross-cutting concern** — it applies uniformly across all endpoints regardless of their business logic. Placing it in a filter means the logging logic is written **once** in a single class (`LoggingFilter.java`), rather than being duplicated in every method of `RoomResource`, `SensorResource`, `SensorReadingResource`, and `DiscoveryResource`. This adheres to the **DRY (Don't Repeat Yourself)** principle.

2. **Guaranteed coverage**: The filter intercepts **every request** that passes through the JAX-RS pipeline, including requests that match no resource (which would produce a 404) and requests that cause unhandled exceptions (which would produce a 500). Manual logging in resource methods would miss these cases entirely, creating gaps in the audit trail.

3. **Consistent format**: The filter enforces a uniform log format (`>>> REQUEST: GET http://...` and `<<< RESPONSE: 200 GET http://...`) across all endpoints. With manual logging, different developers might use inconsistent message formats, log levels, or forget to log certain methods altogether.

4. **Zero modification to existing code**: Adding the filter required **no changes** to any existing resource class. The filter was added as a new class, annotated with `@Provider`, and Jersey automatically discovered and registered it through package scanning. This demonstrates the **Open/Closed Principle** — the system is open for extension (adding new filters) but closed for modification (existing resource classes remain untouched).

5. **Performance monitoring capability**: By logging both the request entry and response exit, the filter creates natural bookends that can be extended to measure **response times** by recording timestamps. This would be impractical to implement consistently with manual logging scattered across dozens of methods.

---


