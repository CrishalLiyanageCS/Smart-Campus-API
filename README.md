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
    │   │   ├── SensorReading.java                    # Reading POJO (id, timestamp, value)
    │   │   └── SensorStatus.java                     # Enum — ACTIVE, MAINTENANCE, OFFLINE
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

**Q1: Explain the JAX-RS resource lifecycle and how it influences your data storage design.**

Resource classes in JAX-RS have a default lifecycle of per-request. Whenever an HTTP request is received, the Jersey runtime will instantiate a new object of the resource class (e.g., `RoomResource`), call the specified method and drop the same object after a response is delivered. This implies that instance-level fields within a resource class are not shared between requests - any information put in an instance field would disappear once the request had been served.

Our data storage design was based on this lifecycle. The fact that resource instances are non-persistent meant that we needed a resource that remains in a shared data store without any ties to a particular request. The `DataStore` class has the `ConcurrentHashMap` fields whose values are defined as `static final` and are loaded once, on first load of the class into the JVM, which stay in memory as long as the application is running. We selected a `ConcurrentHashMap` in particular (not a normal `HashMap`) because servers based on the JAX-RS, like Tomcat, make use of a thread pool to serve many requests in parallel. In the absence of thread-safe collections, concurrent PUT and DELETEs of the same resource might end up having a race condition, corrupted data, or `ConcurrentModificationException` exceptions. The `ConcurrentHashMap` offers atomic operations such as `put()`, `get()`, `remove()`, and `computeIfAbsent()` that can be safely invoked with several threads and external synchronization is not required.

---

**Q2: What are the benefits of the HATEOAS approach used in the discovery endpoint?**

HATEOAS (Hypermedia As The Engine Of Application State) is a constraint of REST architecture in which the server contains links to the next available operation, in addition to data, enabling the client to inquire into the available actions dynamically, instead of fixed URL hard-coded.

In our implementation, the `DiscoveryResource` at `GET /api/v1` returns a `resources` map containing links to `/api/v1/rooms` and `/api/v1/sensors`. This provides several benefits:

1. **Loose coupling:** Client applications do not have to create or hard-code API URLs. A client is able to invoke the discovery endpoint once and explore the rest of the API by using the links that it provides, just like a human being explores a Web site by clicking hyperlinks.

2. **Evolvability:** In case the API changes its URI layout in a new version (such as `/api/v2/rooms` or `/api/sensors` renamed to `/api/devices`), only the link map that is used in the discovery endpoint has to be changed. Clients, which dynamically find links, will be able to change without altering the code.

3. **Self-documentation:** The discovery response is a machine-readable catalogue of API. The root endpoint can be inspected by new developers or automated tools to learn what resources can be accessed and how to access them, decreasing the time to integrate.

4. **Single entry point:** Rather than have clients pre-know a set of URLs, all they need is the API root (`/api/v1`) - all other resources are available as an extension of that one.

---

### Part 2: Room Management

**Q1: Why store sensor IDs in a room instead of full Sensor objects?**

The `Room` model maintains a `List<String> sensorIds` rather than a `List<Sensor>` of full objects. This is an intentional data normalization design decision with several important justifications:

1. **Single source of truth:** Data representation of a sensor resides in sensor store which is represented as a canonical entity (`DataStore.getSensors()`). By embedding full `Sensor` objects within `Room`, we would have two identical copies of sensor information. When the sensor changes state to `MAINTENANCE` where it was formerly in `ACTIVE` state, we would have to update the sensor in two states, one in the sensor store and the other in the embedded list within the room. This duplication brings the possibility of some bugs related to the data inconsistency, as in the sensor store and the copy of the room, both display different values.

2. **Avoids circular reference serialization issues:** A `Sensor` has a `roomId` field pointing back to its parent room. If `Room` contained full `Sensor` objects and `Sensor` contained a full `Room` object, Jackson's JSON serializer would enter an infinite recursive loop (`Room → Sensor → Room → Sensor → ...`), resulting in a `StackOverflowError`. While Jackson annotations like `@JsonManagedReference` / `@JsonBackReference` can mitigate this, storing only IDs eliminates the problem entirely and keeps the model simple.

3. **Reduced payload size:** Embedding a complete sensor object in the response payload would be extremely bulky when listing all rooms via `GET /api/v1/rooms`. Using ID references, room response is kept lightweight, and clients may only choose to retrieve sensor information with `/api/v1/sensors/{id}` when necessary.

4. **Independent resource lifecycles:** There are independent CRUD lifecycles of rooms and sensors. This means that a sensor can be updated (e.g., its `currentValue` will change with each new reading) without having to change the room. Storing IDs only means that one has a dedicated endpoint to manage each resource.

---

**Q2: How does your API ensure DELETE idempotency?**

The DELETE method of HTTP semantics is specified as idempotent, so making repeated calls with the same parameters should have no effect on the eventual state of the server. This is naturally accomplished by our implementation:

- **First** `DELETE /rooms/SCI-201` **call:** The room exists and has no sensors attached, so it is removed from the data store. The server responds with `204 No Content`.
- **Second** `DELETE /rooms/SCI-201` **call:** The room is no more in the data store. The look up returns a `null` and the resource gives a `ResourceNotFoundException` that is mapped to `404 Not Found`.
- **All subsequent calls:** Continue to return `404 Not Found`.

The important point is that server state is unchanged following the first and second calls - the room does not exist. Response code is different (`204` versus `404`) but this is proper HTTP behavior: `404` is the correct way of telling the world that the resource in question has disappeared. The idempotency property is true since making repeated requests does not introduce new side effects - no more data is deleted, no new state is changed beyond the original deletion.

---

### Part 3: Sensors & Filtering

**Q1: What happens if a client sends a non-JSON content type to an endpoint annotated with `@Consumes(APPLICATION_JSON)`?**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on our `SensorResource` class is an example of a content type contract that is purely enforced by the JAX-RS runtime, prior to the resource method being called at all. The processing is as follows:

1. Client makes `POST /api/v1/sensors` request with `Content-Type: application/xml` (or `text/plain`, or any other type but `application/json`).
2. At the request matching stage, Jersey compares the incoming `Content-Type` header to the annotation of the resource method of the matched resource, which is annotated with `@Consumes`.
3. In case of a mismatch, Jersey right away transmits an `HTTP 415 Unsupported Media Type` reply. The Java code of the resource method is not executed.
4. The JAX-RS runtime (or our `GenericExceptionMapper`, as configured) creates the response body, which usually includes an error message telling the unsupported media type.

The annotation-based model has a number of advantages: it isolates content negotiation issues and business logic, consistent enforcement at all endpoints, and defense against malformed or unforeseen payload format at the deserialization layer, where it might lead to parsing errors.

---

**Q2: Explain the difference between `@QueryParam` and `@PathParam` and when to use each.**

Both annotations bind the values of the annotation of the HTTP request URI to parameters of a Java method, but they have entirely different semantic purposes:

**`@PathParam`** takes the values of the segments of the URI path and locates a particular resource within the hierarchy of resources. As an illustration, in `GET /api/v1/sensors/TEMP-001`, the path parameter `TEMP-001` is a unique identifier of a sensor. Path parameters are required - the request cannot conform to the route without the path parameters. They are the ones that model the resource identity and mirror the hierarchical structure of the API (collections → individual items).

**`@QueryParam`** reads values out of the query string (the query string is followed by a question mark). `@QueryParam` changes the way the resource is represented. To illustrate, in `GET /api/v1/sensors?type=CO2`, the query parameter `type` is not a particular sensor; it is a filtering of the collection. Parameters to queries are optional, meaning that when the `type` is not specified, the endpoint comes back with all the sensors, raw. They simulate refinements, like filtering, sorting, pagination or choosing particular fields.

In our implementation, `SensorResource.getAllSensors(@QueryParam("type") String type)` uses `@QueryParam` because the `type` parameter is optional and acts as a filter on the full sensor collection. The filtering is implemented using Java streams with `.equalsIgnoreCase()` for case-insensitive matching, so `?type=co2`, `?type=CO2`, and `?type=Co2` all return the same results.

By contrast, an alternative design such as `/api/v1/sensors/type/CO2` 
would embed the filter value directly into the URL path. This approach 
is semantically incorrect because CO2 is not a resource — it is a search 
criterion applied to a collection. Embedding it in the path treats it as 
a resource identifier, which violates REST principles. It also pollutes 
the URI namespace, risks conflicting with actual sensor IDs, and makes it 
impossible to combine multiple filters (e.g., filtering by both type and 
status simultaneously) without creating an explosion of nested path 
combinations. Query parameters are the correct mechanism for optional, 
non-identifying, combinable filters on collections.

---

### Part 4: Sub-Resources

**Q: What are the benefits of the sub-resource locator pattern used for sensor readings?**

JAX-RS sub-resource locator pattern provides the hierarchical ownership between sensors and sensor readings. Rather than defining all the endpoints related to reading in `SensorResource`, we delegate to a different `SensorReadingResource` class through a locator method:

```java
// In SensorResource.java — this method has @Path but NO HTTP method annotation
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

The class level annotation of the `SensorReadingResource` with `@Path` is omitted since the URL context is fully specified by the locator method of the parent. Jersey invokes the locator strategy, accepts the sub-resource instance and subsequently compares the incoming HTTP method (`GET` or `POST`) with the annotated methods of the sub-resource.

**Benefits of this pattern:**

1. **Separation of concerns:** `SensorResource` manages sensor CRUD (`GET/POST/PUT/DELETE` at `/sensors`), whereas `SensorReadingResource` only manages reading operations (`GET/POST` at `/sensors/{id}/readings`). Every class has one, narrow task, which is in line with the Single Responsibility Principle.

2. **Encapsulated Context:** The parameter of `sensorId` is passed to the sub-resource constructor, which creates the parent context. Each and every technique in `SensorReadingResource` is confined to that particular sensor so that there is no need to repeatedly extract sensor ID and validate sensor ID in each and every technique.

3. **Clean URI hierarchy:** The URL path `/sensors/{sensorId}/readings` will make it intuitive that readings are associated with a sensor. It is based on the REST best practices in which the hierarchy of the URIs reflects the ownership relationship in the domain model. Clients instinctively comprehend that there is no such thing as a reading in isolation - it is always relative to a particular sensor.

4. **Modularity and maintainability:** Since the API expands (e.g. by adding `GET /sensors/{id}/readings/{readingId}`), the new functionality goes into `SensorReadingResource` without adding complexity to `SensorResource`. All classes are manageable, testable and can be modified independently.

5. **Business logic and isolation:** The sub-resource implements its own business rules. For example, `SensorReadingResource.addReading()` verifies that the parent sensor is in the `MAINTENANCE` state and raises a `SensorUnavailableException` (`403 Forbidden`) in this case. This is a rule of readings, and is best represented in the readings sub-resource, not in the main sensor resource.

---

### Part 5: Error Handling & Logging

**Q1: Explain the semantic difference between HTTP 422 and HTTP 404 and when each is appropriate.**

These two status codes address fundamentally different error scenarios:

**HTTP 404 Not Found** means that the target URI has no match to an existing resource. The customer is attempting to log into a particular object that is not present in the system. For example:
- `GET /api/v1/rooms/INVALID-ID` — there is no room with ID `"INVALID-ID"`.
- `DELETE /api/v1/sensors/NONEXISTENT` — the sensor cannot be deleted because it does not exist.

This is done in our system by throwing `ResourceNotFoundException`, which is mapped by the `ResourceNotFoundExceptionMapper` to a `404` response.

**HTTP 422 Unprocessable Entity** is sent when the request body is syntactically correct (it is well-formed JSON that can be decoded) but is semantically incorrect — the data either does not meet a business constraint, or refers to an object that does not exist. For example:
- `POST /api/v1/sensors` with `{"type": "Temperature", "roomId": "FAKE-ROOM"}` — the JSON is valid, but the referenced room does not exist.
- `PUT /api/v1/sensors/TEMP-001` with `{"roomId": "NONEXISTENT"}` — the sensor exists, but the new room it is being moved to does not.

In our system, this is dealt with by throwing `LinkedResourceNotFoundException` which is transformed by `LinkedResourceNotFoundExceptionMapper` into a `422` response.

The important difference: **404** means the target of the URL is not there (the thing you are requesting is not there), whereas **422** means the target of the request payload is not there (the thing your data points to is not there). It is important to use the appropriate status code to allow the clients to programmatically differentiate between these two cases and respond properly — e.g. a `422` could encourage the client to check the `roomId` and then re-try.

---

**Q2: Why is it a security risk to expose stack traces in API error responses?**

Many web frameworks have a default configuration that includes the complete Java stack trace in the body of the HTTP response when an exception is thrown without being handled. This is a production hazard due to a number of reasons:

1. **Technology disclosure:** Stack traces show versions of the programming language (Java) and framework (Jersey/JAX-RS) as well as library versions. This information can be used by attackers to find known vulnerabilities (CVEs) that are specific to those versions.

2. **Internal path exposure:** Stack traces include fully qualified names of classes and file names (e.g., `com.smartcampus.repository.DataStore.getRoom(DataStore.java:71)`) that expose the package structure, naming conventions of classes, and even the deployment directory locations. This provides a map of the codebase to attackers.

3. **Logic disclosure:** A stack trace shows the flow of the internal execution of a program displaying which methods are called, in what sequence, and by what classes. This may reveal business logic, sequence of security checks or database query patterns which may be exploited by an attacker.

4. **Data leakage:** Exception messages can end up accidentally holding sensitive information like SQL queries, connection strings, internal IP addresses or user data that was under processing at the time of the error.

We avoid this by our implementation using the `GenericExceptionMapper` which traps all the unhandled `Throwable` exceptions. It records the entire exception including stack trace on the server side (so that the developer can debug the exception) but responds with a safe, generic message to the client: `"An unexpected error occurred. Please try again later"` with a `500` status code. Internal details are never sent out on the network.

---

**Q3: What are the benefits of using a JAX-RS filter for logging instead of adding logging code to each resource method?**

Our `LoggingFilter` is a `ContainerRequestFilter` and `ContainerResponseFilter` which are JAX-RS interfaces to enable code to be run prior to and following each resource method invocation respectively. The filter is registered with the world through annotation-based filtering, using `@Provider` and the package scanning in Jersey.

The filter method offers the following advantages over hand-logging in any resource:

1. **Cross-cutting concern separation:** Logging is a cross-cutting issue - it cuts across all endpoints irrespective of their business logic. By putting it in a filter, the logging logic is controlled by a single class (`LoggingFilter.java`), and does not have to be duplicated in each and every method of `RoomResource`, `SensorResource`, `SensorReadingResource`, and `DiscoveryResource`. This is in line with the DRY (Don't Repeat Yourself) principle.

2. **Guaranteed coverage:** The filter catches all requests that go through the JAX-RS pipeline, including requests that match no resource (and would result in a `404`) and requests that raise unhandled exceptions (and would result in a `500`). The use of manual resource logging mechanisms would not capture these instances at all, thus there would be gaps in the audit trail.

3. **Consistent format:** The filter imposes a standardized format of logs (`>>> REQUEST: GET http://...` and `<<< RESPONSE: 200 GET http://...`) on all endpoints. In the case of manual logging, various developers may adopt different message formats, log levels or even fail to log some of their methods altogether.

4. **Zero modification to existing code:** There were no modifications to any existing resource class to add the filter. A new class with an annotation of `@Provider` was added as the filter and Jersey automatically scans the package to discover and register it. This illustrates the Open/Closed Principle - the system can be extended (by adding new filters) but not changed (the resource classes already present can never be changed).

5. **Performance monitoring capability:** The filter uses the natural bookends formed by recording timestamps by logging both request entry and response exit to measure response times. This would prove to be an inconvenience to work with on a regular basis with manual logging spread across dozens of methods.
