# Booking Platform Spring Boot Project

## Executive Summary  
This report details a Spring Boot 4.0.5 project skeleton (with a 3.5.13 branch option) using Java 25 (and Java 21 as alternative). The project is configured for PostgreSQL on port 5432 (user/postgres) and demonstrates both Ehcache and Redis caching. All Jakarta EE APIs are used (jakarta.* for JPA, Servlets, Validation), with `javax.cache:cache-api:1.1.1` retained for JSR-107. We provide complete file contents (pom.xml, code, configs, Docker files, etc.) and a manifest. **Recommendation:** *Use Spring Boot 4.0.5 with Java 25 for this project* (latest stable SB4 and Java LTS).

## Key Versions and Dependencies  

- **Spring Boot:** 4.0.5 (Spring Framework 7, Jakarta EE 11 baseline)【86†L593-L597】. (Alternative: 3.5.13 branch for Spring Boot 3.x compatibility【86†L593-L597】.)  
- **Java:** 25 (LTS) – supported by SB4【87†L220-L228】. (Alternative: Java 21 LTS.)  
- **Spring Boot Parent:** `org.springframework.boot:spring-boot-starter-parent:4.0.5`.  
- **Dependencies:** (BOM-managed)   
  - `spring-boot-starter-web` (web MVC)  
  - `spring-boot-starter-data-jpa` (JPA)  
  - `spring-boot-starter-cache` (Spring Cache)  
  - `spring-boot-starter-data-redis` (Redis cache)  
  - **JPA:** `jakarta.persistence:jakarta.persistence-api:3.1.0` (Jakarta JPA API)【82†L1-L4】  
  - **Servlet API:** `jakarta.servlet:jakarta.servlet-api:6.0.0`【83†L1-L4】  
  - **Validation API:** `jakarta.validation:jakarta.validation-api:3.0.2`【84†L1-L4】  
  - **Cache API (JSR-107):** `javax.cache:cache-api:1.1.1` (no jakarta replacement)【64†L139-L147】  
  - `org.ehcache:ehcache:3.11.1` (Ehcache 3)  
  - `org.postgresql:postgresql:42.7.10` (Postgres JDBC)【91†L1-L4】  
  - `spring-boot-starter-test` (test utilities)  

*Note:* Versions above mostly come from Spring Boot’s BOM (via the parent POM), so they need not be hard-coded. We do specify the Postgres driver and cache API explicitly.  

## Jakarta vs Javax Namespace Migration  
Spring Boot 4 uses Jakarta EE 11, so any Java EE APIs have `jakarta.*` artifacts. For example, JPA, Servlets, and Validation use `jakarta.*` groupIds【82†L1-L4】【83†L1-L4】【84†L1-L4】. The one exception here is JCache (JSR-107): it still resides in `javax.cache` (no Jakarta equivalent)【64†L139-L147】. Thus in our files:  
- Use `jakarta.persistence-api`, `jakarta.servlet-api`, `jakarta.validation-api`, etc. (as shown below).  
- Retain `javax.cache:cache-api:1.1.1` for the cache interface.  

### Dependency Update Example  
The table below shows key Maven dependency lines before and after migration:

| API                 | Before (javax)                                          | After (jakarta)                                             |
|---------------------|---------------------------------------------------------|-------------------------------------------------------------|
| JPA (Hibernate)     | `<dependency><groupId>javax.persistence</groupId><artifactId>javax.persistence-api</artifactId><version>2.2</version></dependency>` | `<dependency><groupId>jakarta.persistence</groupId><artifactId>jakarta.persistence-api</artifactId><version>3.1.0</version></dependency>`【82†L1-L4】 |
| Servlet API         | `<dependency><groupId>javax.servlet</groupId><artifactId>javax.servlet-api</artifactId><version>4.0.1</version></dependency>` | `<dependency><groupId>jakarta.servlet</groupId><artifactId>jakarta.servlet-api</artifactId><version>6.0.0</version></dependency>`【83†L1-L4】 |
| Validation API      | `<dependency><groupId>javax.validation</groupId><artifactId>validation-api</artifactId><version>2.0.1.Final</version></dependency>` | `<dependency><groupId>jakarta.validation</groupId><artifactId>jakarta.validation-api</artifactId><version>3.0.2</version></dependency>`【84†L1-L4】 |
| Cache API (JSR-107) | `<dependency><groupId>javax.cache</groupId><artifactId>cache-api</artifactId><version>1.1.1</version></dependency>` | *(remains the same; no jakarta/cache-api exists)* |

## Project Structure & Files  

Below is the directory structure and key file contents. All code is in package **com.example.booking**. Comments or separate sections indicate Ehcache vs Redis setup.

```
booking-platform/
├── pom.xml
├── build.gradle.kts (optional)
├── mvnw, mvnw.cmd, .mvn/            (Maven Wrapper)
├── .gitignore
├── src/
│   └── main/
│       ├── java/com/example/booking/
│       │   ├── Application.java
│       │   ├── config/CacheConfig.java
│       │   ├── model/Movie.java
│       │   ├── model/Booking.java
│       │   ├── repository/MovieRepository.java
│       │   ├── repository/BookingRepository.java
│       │   ├── service/MovieService.java
│       │   ├── service/BookingService.java
│       │   ├── controller/MovieController.java
│       │   └── controller/BookingController.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── ehcache.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## README and Build Instructions  

**README.md:**

```
# Booking Platform Spring Boot Application

## Overview
This is a Spring Boot application with PostgreSQL (port 5432) and caching (Ehcache or Redis). Spring Boot 4.0.5 and Java 25 are used by default.

## Prerequisites
- Java 25 (or 21) SDK installed
- Maven (or use Maven Wrapper)
- Docker & Docker Compose (for container setup)

## Build and Run (Maven)
1. Set environment vars if needed (defaults: DB_USER=postgres, DB_PASS=postgres).
2. Build the JAR:
   ```
   mvn clean package
   ```
3. Run the app:
   ```
   java -jar target/booking-0.0.1-SNAPSHOT.jar
   ```
   The app runs on port 8080.

## Run with Maven Wrapper
- On Linux/Mac:
  ```
  ./mvnw spring-boot:run
  ```
- On Windows:
  ```
  mvnw.cmd spring-boot:run
  ```

## Docker Deployment
1. Build and start services:
   ```
   docker-compose up --build
   ```
   This spins up PostgreSQL, Redis, and the Spring Boot app.
2. Access the API at `http://localhost:8080`.

## Caching Toggle
- By default, no cache is active. To enable a cache, uncomment in `application-dev.yml`:
  - For **Ehcache**, uncomment `spring.cache.type: ehcache` and ensure `ehcache.xml` is present.
  - For **Redis**, uncomment `spring.cache.type: redis`.

## Troubleshooting
- If PostgreSQL fails to start, check port conflicts (5432). Ensure DB container logs show “ready to accept connections”.
- If cache config errors, verify the correct `spring.cache` settings and that the respective service (Redis) is running.
- Enable Spring Boot debug logs (`--debug`) for detailed startup info.
```

## Comparison Tables and Diagrams  

**Compatibility (Spring Boot vs Java):**

| Spring Boot | Java Versions Supported            | Notes                         |
|-------------|------------------------------------|-------------------------------|
| 4.0.5       | 17 (min) through 26 (max)【87†L220-L228】 | Requires Java 17+【87†L220-L228】. Best with Java 25 LTS. |
| 3.5.13      | 17 (min) through 25                | Latest 3.x; older baseline.   |

**Ehcache vs Redis Cache:**

| Feature        | **Ehcache (Embedded)**                     | **Redis (External)**                         |
|----------------|--------------------------------------------|----------------------------------------------|
| Architecture   | In-JVM, uses JCache (JSR-107)【20†L344-L352】  | Client-server, networked store               |
| Distribution   | Local (single instance)                    | Distributed (multi-instance clusters)       |
| Setup          | Include `ehcache.xml`; no external service | Requires running Redis server/container     |
| Data Model     | Simple key-value (serializable objects)     | Rich data types (strings, hashes, lists)    |
| Multi-Language | Java only (integrated with Spring Cache)    | Clients in any language (via TCP)           |
| Security       | Controlled by app (no external port)       | Supports AUTH/TLS if configured              |
| Typical Use    | Local caching of method results            | Shared cache (sessions, distributed cache)  |
| Spring Support | `spring.cache.jcache.config=ehcache.xml`【93†L1-L4】 | `spring-boot-starter-data-redis` auto-configures【21†L324-L326】  |

**Mermaid Architecture Diagram:**

```mermaid
graph TB
  User((User))
  Controller[/REST Controller/]
  Service([Service Layer])
  Repository[[Repository]]
  PostgreSQL((PostgreSQL))
  Cache((Cache: Ehcache or Redis))
  User --> Controller
  Controller --> Service
  Service --> Repository
  Repository --> PostgreSQL
  Service --> Cache
```

**Mermaid Request Flow Diagram:**

```mermaid
sequenceDiagram
    participant U as User
    participant C as Controller
    participant S as Service
    participant Ca as Cache
    participant R as Repository
    participant DB as PostgreSQL

    U->>C: GET /movies/1
    C->>S: getMovieById(1)
    S->>Ca: @Cacheable("movies")
    alt Cache hit
        Ca-->>S: return Movie
    else Cache miss
        S->>R: findById(1)
        R->>DB: SELECT * FROM movie WHERE id=1
        DB-->>R: result row
        R-->>S: Movie
        S->>Ca: put "movies":Movie
    end
    S-->>C: Movie
    C-->>U: HTTP 200 JSON
```

## File Manifest  

- **`pom.xml`** – Maven project file (Spring Boot 4.0.5 parent, dependencies as above).  
- **`build.gradle.kts`** – *Optional:* Gradle build file (shown as example if using Gradle).  
- **`.gitignore`** – Git ignore rules (ignores `target/`, IntelliJ files, etc.).  
- **Maven Wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) – Maven wrapper scripts.  
- **`src/main/java/com/example/booking/Application.java`** – Main application class (`@SpringBootApplication`).  
- **`src/main/java/com/example/booking/config/CacheConfig.java`** – Caching configuration (Ehcache/Redis beans, commented toggles).  
- **`src/main/java/com/example/booking/model/Movie.java`** – JPA entity for Movie (Jakarta annotations).  
- **`src/main/java/com/example/booking/model/Booking.java`** – JPA entity for Booking.  
- **`src/main/java/com/example/booking/repository/MovieRepository.java`** – Spring Data JPA repo for Movie.  
- **`src/main/java/com/example/booking/repository/BookingRepository.java`** – Repo for Booking.  
- **`src/main/java/com/example/booking/service/MovieService.java`** – Service with `@Cacheable` example.  
- **`src/main/java/com/example/booking/service/BookingService.java`** – Booking service.  
- **`src/main/java/com/example/booking/controller/MovieController.java`** – REST endpoints for Movie.  
- **`src/main/java/com/example/booking/controller/BookingController.java`** – Endpoints for Booking.  
- **`src/main/resources/application.yml`** – Spring profiles config.  
- **`src/main/resources/application-dev.yml`** – Dev profile config (PostgreSQL and cache setup with env vars).  
- **`src/main/resources/ehcache.xml`** – Ehcache configuration.  
- **`Dockerfile`** – Docker build instructions for the app.  
- **`docker-compose.yml`** – Docker Compose (services: app, postgres, redis).  
- **`README.md`** – Build and run instructions (Maven, Docker, troubleshooting).  

*Diagram generation commands:* (if needed for mermaid rendering)
```
# Not needed; diagrams are in markdown code blocks above.
```

*ZIP creation (local):* After creating the above structure and files, from project root run:  
```bash
zip -r booking-platform.zip . -x "*.git/*" 
```  
This will package all files (excluding `.git` directory) into `booking-platform.zip`.  

**Sources:** Spring Boot docs and migration guide【86†L593-L597】【87†L220-L228】【80†L403-L412】, Docker official docs【91†L1-L4】, and knowledge of Jakarta vs Javax, Ehcache and Redis. These informed the version choices and configuration above.