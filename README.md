# Smart Campus API

**Module**: 5COSC022W — Client-Server Architectures  
**Student**: Crishal Liyanage  
**Student ID**: w2121314git
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
| Apache Tomcat | 9 or 10 | Servlet container for deploying the WAR |

---

### Option A — Run with NetBeans (Recommended)

1. **Open the project**
   - Launch NetBeans → **File → Open Project** → select the `Smart-Campus-API` folder
   - NetBeans recognises it automatically as a Maven project

2. **Configure Run settings**
   - Right-click the project → **Properties → Run**
   - Confirm the following settings match the screenshot below:
     - **Server**: Apache Tomcat or TomEE
     - **Java EE Version**: Jakarta EE 8 Web
     - **Context Path**: `/SmartCampusAPI`
     - **Relative URL**: `/api/v1`

   > *Project Properties → Run: Context Path `/SmartCampusAPI`, Relative URL `/api/v1`*

3. **Run the project**
   - Right-click the project → **Run** (or press **F6**)
   - NetBeans builds the WAR and deploys to Tomcat automatically

4. **Verify**
   - Navigate to: `http://localhost:8080/SmartCampusAPI/api/v1`
   - You should see the Discovery endpoint JSON response

---

### Option B — Run with Embedded Tomcat (Command Line)

```bash
mvn clean compile tomcat7:run
```

The API will start at: **`http://localhost:8080/SmartCampusAPI/api/v1`**

---

### Option C — Deploy to External Tomcat

```bash
# 1. Build the WAR file
mvn clean package

# 2. Copy the WAR to Tomcat webapps folder
cp target/smartcampus.war /path/to/tomcat/webapps/

# 3. Start Tomcat
/path/to/tomcat/bin/startup.sh    # Linux/Mac
/path/to/tomcat/bin/startup.bat   # Windows
```

The API will be available at: **`http://localhost:8080/smartcampus/api/v1`**

---

## 3. API Endpoints Summary

| Method | Endpoint | Description | Response Codes |
|--------|----------|-------------|----------------|
| `GET` | `/api/v1` | Discovery — API metadata and HATEOAS links | 200 |
| `GET` | `/api/v1/rooms` | List all rooms | 200 |
| `POST` | `/api/v1/rooms` | Create a new room | 201, 400 |
| `GET` | `/api/v1/rooms/{roomId}` | Get a specific room | 200, 404 |
| `PUT` | `/api/v1/rooms/{roomId}` | Update a room | 200, 404 |
| `DELETE` | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors assigned) | 204, 404, 409 |
| `GET` | `/api/v1/sensors` | List all sensors — supports `?type=` filter | 200 |
| `POST` | `/api/v1/sensors` | Register a sensor (roomId must exist) | 201, 400, 422 |
| `GET` | `/api/v1/sensors/{sensorId}` | Get a specific sensor | 200, 404 |
| `PUT` | `/api/v1/sensors/{sensorId}` | Update a sensor | 200, 404, 422 |
| `DELETE` | `/api/v1/sensors/{sensorId}` | Remove a sensor and unlink from room | 204, 404 |
| `GET` | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor | 200, 404 |
| `POST` | `/api/v1/sensors/{sensorId}/readings` | Add a reading (blocked if MAINTENANCE) | 201, 403, 404 |

---

## 4. Sample curl Commands

### 4.1 Discovery — Explore the API Entry Point
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1 \
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
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Computer Lab 4", "capacity": 35}'
```
Expected response (201 Created):
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
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=Temperature" \
  -H "Accept: application/json"
```
Expected response (200 OK):
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
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.5}'
```
Expected response (201 Created) — UUID and timestamp auto-generated, parent sensor `currentValue` updated to 24.5:
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "timestamp": 1713045600000,
  "value": 24.5
}
```

### 4.5 Attempt to Delete a Room with Active Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301 -v
```
Expected response (409 Conflict):
```json
{
  "error": "CONFLICT",
  "message": "Room LIB-301 cannot be deleted: 2 active sensor(s) assigned.",
  "status": 409
}
```

### 4.6 Post Reading to a Sensor in MAINTENANCE (403 Forbidden)
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-002/readings \
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
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/rooms/INVALID-ID
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

**Q1: Explain the JAX-RS resource lifecycle and how it influences your data storage design.**

Resource classes in JAX-RS have a default lifecycle of per-request. Whenever an HTTP request is received, the Jersey runtime will instantiate a new object of the resource class (e.g., `RoomResource`), call the specified method and drop the same object after a response is delivered. This implies that instance-level fields within a resource class are not shared between requests — any information put in an instance field would disappear once the request had been served.

Our data storage design was based on this lifecycle. The fact that resource instances are non-persistent meant that we needed a resource that remains in a shared data store without any ties to a particular request. The `DataStore` class has the `ConcurrentHashMap` fields whose values are defined as `static final` and are loaded once, on first load of the class into the JVM, which stay in memory as long as the application is running. We selected a `ConcurrentHashMap` in particular (not a normal `HashMap`) because servers based on JAX-RS, like Tomcat, make use of a thread pool to serve many requests in parallel. In the absence of thread-safe collections, concurrent PUT and DELETEs of the same resource might end up having a race condition, corrupted data, or `ConcurrentModificationException` exceptions. The `ConcurrentHashMap` offers atomic operations such as `put()`, `get()`, `remove()`, and `computeIfAbsent()` that can be safely invoked with several threads and external synchronization is not required.

---

**Q2: What are the benefits of the HATEOAS approach used in the discovery endpoint?**

HATEOAS (Hypermedia As The Engine Of Application State) is a constraint of REST architecture in which the server contains links to the next available operation, in addition to data, enabling the client to inquire into the available actions dynamically, instead of fixed URL hard-coded.

In our implementation, the `DiscoveryResource` at `GET /api/v1` returns a `resources` map containing links to `/api/v1/rooms` and `/api/v1/sensors`. This provides several benefits:

1. **Loose coupling:** Client applications do not have to hard-code API URLs. A client can invoke the discovery endpoint once and navigate the rest of the API using the returned links, just as a user navigates a website by clicking hyperlinks.

2. **Evolvability:** If the API changes its URI structure in a new version (e.g., `/api/v2/rooms` or renaming `/api/sensors` to `/api/devices`), only the discovery endpoint link map needs updating. Clients that navigate dynamically adapt without code changes.

3. **Self-documentation:** The discovery response is a machine-readable catalogue of the API. New developers or automated tools can inspect the root endpoint to discover available resources, reducing integration time.

4. **Single entry point:** Rather than requiring clients to know a set of URLs in advance, all they need is the API root (`/api/v1`) — all other resources are reachable from that one endpoint.

---

### Part 2: Room Management

**Q1: Why store sensor IDs in a room instead of full Sensor objects?**

The `GET /api/v1/rooms` endpoint returns a list of complete `Room` objects, where each object contains a `List<String> sensorIds` instead of embedded `Sensor` objects. This is an intentional design choice with direct network bandwidth and client-side processing implications.

Bandwidth wise, full nested objects returned are large in size in terms of payload. When every Room uploaded all the Sensor objects, the size of the response to a GET /api/v1/rooms query would increase directly with the number of sensors in all rooms. In our system, room `LIB-301` alone has two sensors (`TEMP-001`, `CO2-001`), each carrying `type`, `status`, `currentValue`, and `roomId` fields. At campus scale with thousands of rooms and sensors, embedding full objects would transmit large volumes of data that many clients may never use, wasting network resources and increasing latency.

Client-side processing wise, returning ID references makes each response light and provide control to the clients. A client which only requires room names and capacities like a building map dashboard can work on the room list immediately without having to decode deep-nest sensor data. A client requiring sensor details can request a specific follow-up by sending GET /api/v1/sensors/{id} request. This on-demand model minimizes unnecessary parsing overhead on the client and lets each resource be used separately and on-demand, as required.

---

**Q2: How does your API ensure DELETE idempotency?**

The HTTP DELETE method is specified as idempotent — repeated calls with the same parameters should have no effect on the eventual state of the server. This is naturally accomplished by our implementation:

- **First** `DELETE /rooms/SCI-201` **call:** The room exists and has no sensors attached, so it is removed from the data store. The server responds with `204 No Content`.
- **Second** `DELETE /rooms/SCI-201` **call:** The room no longer exists in the data store. The lookup returns `null` and `ResourceNotFoundException` is thrown, mapped to `404 Not Found`.
- **All subsequent calls:** Continue to return `404 Not Found`.

The important point is that server state is unchanged following the first deletion — the room does not exist. The response code differs (`204` versus `404`) but this is correct HTTP behaviour: `404` accurately communicates that the resource has been removed. The idempotency property holds because repeated requests introduce no new side effects — no additional data is deleted and no new state is changed beyond the original deletion.

---

### Part 3: Sensors & Filtering

**Q1: What happens if a client sends a non-JSON content type to an endpoint annotated with `@Consumes(APPLICATION_JSON)`?**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on our `SensorResource` class is a content type contract enforced entirely by the JAX-RS runtime, before the resource method is invoked at all. The processing is as follows:

1. The client sends a `POST /api/v1/sensors` request with `Content-Type: application/xml` (or `text/plain`, or any type other than `application/json`).
2. At the request matching stage, Jersey compares the incoming `Content-Type` header against the `@Consumes` annotation on the matched resource method.
3. On a mismatch, Jersey immediately returns an `HTTP 415 Unsupported Media Type` response. The Java resource method is never executed.
4. The JAX-RS runtime (or our `GenericExceptionMapper`, as configured) constructs the response body with an appropriate error message.

This annotation-based model isolates content negotiation from business logic, enforces consistent media type handling across all endpoints, and protects against malformed or unexpected payload formats at the deserialization layer.

---

**Q2: Explain the difference between `@QueryParam` and `@PathParam` and when to use each.**

In our implementation, GET /api/v1/sensors?type=CO2 makes use of the filtering of the sensor collection through the use of @QueryParam. The second design would use the filter value as part of the path, e.g., /api/v1/sensors/type/CO2, with the help of the element of the path parameter.

The query parameter approach is superior for filtering for two reasons. First, **semantic correctness**: a path segment identifies a specific resource by its identity. In `/api/v1/sensors/TEMP-001`, the value `TEMP-001` uniquely names a sensor that exists as a resource. The value `CO2` in the alternative design is not a resource — it is a search criterion applied to a collection. Embedding it in the path misrepresents it as a resource identifier, which violates REST principles. Second, **flexibility and combinability**: query parameters are optional and composable by nature. Our `getAllSensors(@QueryParam("type") String type)` method returns all sensors when `type` is omitted and filters when it is supplied, all within one method. A path-based approach would require a separate route for every filter combination, making it impossible to support multiple simultaneous filters (e.g., by both `type` and `status`) without an explosion of nested path patterns.

---

### Part 4: Sub-Resources

**Q: What are the benefits of the sub-resource locator pattern used for sensor readings?**

In our implementation, instead of directly defining all reading-related endpoints within `SensorResource`, we delegate to a separate `SensorReadingResource` class by way of a sub-resource locator method. This locator method is annotated with `@Path("/{sensorId}/readings")` within `SensorResource`, resolving to the full path `/api/v1/sensors/{sensorId}/readings` at runtime. It intentionally carries no HTTP method annotation, which signals to Jersey that it is a locator rather than a handler. Jersey makes an invocation and is fed the `SensorReadingResource` instance that is scoped to that particular sensor and the incoming HTTP method — `GET` or `POST` — against the methods defined in that sub-resource class. The `sensorId` is passed once through the constructor, giving the sub-resource its complete parent context without needing to re-extract or re-validate it in every method.

The core architectural benefit is complexity management through delegation. If every reading-related endpoint were defined inside `SensorResource` alongside all sensor CRUD operations, that single class would grow to simultaneously manage sensor creation, retrieval, updating, deletion, and reading history. In large APIs this produces a monolithic controller that is difficult to navigate, test, and extend independently. By delegating to `SensorReadingResource`, each class carries a single narrow responsibility — `SensorResource` owns sensor CRUD at `/api/v1/sensors`, while `SensorReadingResource` owns reading operations at `/api/v1/sensors/{sensorId}/readings`. This separation directly reflects the natural parent-child ownership in the domain model and keeps both classes independently manageable as the API grows.

---

### Part 5: Error Handling & Logging

**Q1: Explain the semantic difference between HTTP 422 and HTTP 404 and when each is appropriate.**

These two status codes address fundamentally different error scenarios.

`HTTP 404 Not Found` means the target URI has no match to an existing resource — the client is attempting to access something that does not exist in the system. For example:
- `GET /api/v1/rooms/INVALID-ID` — there is no room with ID `"INVALID-ID"`.
- `DELETE /api/v1/sensors/NONEXISTENT` — the sensor cannot be deleted because it does not exist.

In our system this is handled by throwing `ResourceNotFoundException`, mapped by `ResourceNotFoundExceptionMapper` to a `404` response.

`HTTP 422 Unprocessable Entity` is returned when the request body is syntactically correct (well-formed JSON that can be decoded) but semantically invalid — the data refers to a resource that does not exist. For example:
- `POST /api/v1/sensors` with `{"type": "Temperature", "roomId": "FAKE-ROOM"}` — the JSON is valid, but the referenced room does not exist.
- `PUT /api/v1/sensors/TEMP-001` with `{"roomId": "NONEXISTENT"}` — the sensor exists, but the new room it references does not.

In our system this is handled by throwing `LinkedResourceNotFoundException`, mapped by `LinkedResourceNotFoundExceptionMapper` to a `422` response.

The critical distinction is: `404` means the target of the URL does not exist, whereas `422` means the target referenced inside the request payload does not exist. Using the appropriate status code allows clients to programmatically differentiate between these cases — a `422` signals to the client to check the `roomId` field and retry.

---

**Q2: Why is it a security risk to expose stack traces in API error responses?**

Many web frameworks include the complete Java stack trace in the HTTP response body when an unhandled exception is thrown. This is a production security hazard for the following reasons:

1. **Technology disclosure:** Stack traces reveal the programming language (Java), framework (Jersey/JAX-RS), and library versions. Attackers can use this to identify known CVEs specific to those versions.

2. **Internal path exposure:** Stack traces include fully qualified class names and file references such as `com.smartcampus.repository.DataStore.getRoom(DataStore.java:71)`, exposing the package structure, class naming conventions, and deployment directory layout — effectively providing attackers a map of the codebase.

3. **Logic disclosure:** A stack trace reveals the internal execution flow — which methods were called, in what order, and by which classes — potentially exposing business logic, security check sequences, or data access patterns that can be exploited.

4. **Data leakage:** Exception messages may inadvertently contain sensitive information such as connection strings, internal IP addresses, or user data that was being processed at the time of the error.

Our `GenericExceptionMapper` prevents this by catching all unhandled `Throwable` exceptions, logging the full stack trace server-side for debugging, and returning only a safe generic message to the client — `"An unexpected error occurred. Please try again later"` with a `500` status code. Internal details are never transmitted over the network.

---

**Q3: What are the benefits of using a JAX-RS filter for logging instead of adding logging code to each resource method?**

Our `LoggingFilter` implements both `ContainerRequestFilter` and `ContainerResponseFilter`, logging the HTTP method and URI on every incoming request and the status code on every outgoing response. Using a filter for this rather than inserting `Logger.info()` calls into each resource method offers two core advantages.

First, **guaranteed and consistent coverage**. A filter intercepts every request that passes through the JAX-RS pipeline, including requests that match no resource (resulting in a `404`) and requests that trigger unhandled exceptions (resulting in a `500`). Manual logging inside resource methods would miss both of these cases entirely, leaving gaps in the audit trail precisely where observability matters most. Additionally, the filter enforces a uniform log format — `>>> REQUEST: GET http://...` and `<<< RESPONSE: 200 GET http://...` — across all endpoints, whereas manual logging scattered across `RoomResource`, `SensorResource`, `SensorReadingResource`, and `DiscoveryResource` would rely on each developer remembering to add and format logging consistently.

Second, **separation of concerns**. Logging is a cross-cutting concern that has no relationship to business logic. Embedding `Logger.info()` calls inside methods like `createRoom()` or `addReading()` mixes infrastructure concerns into domain logic, making the code harder to read and maintain. The `@Provider` annotation on `LoggingFilter` allows Jersey's package scanning to discover and register it automatically at startup, with zero modification to any existing resource class.
