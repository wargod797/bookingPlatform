# Booking Platform — Detailed Design Document

## Table of Contents

1. [Purpose](#purpose)
2. [System Overview](#system-overview)
3. [Architecture Diagram](#architecture-diagram)
4. [Component Architecture](#component-architecture)
5. [Domain Model](#domain-model)
6. [Entity Relationship Diagram](#entity-relationship-diagram)
7. [Layered Architecture](#layered-architecture)
8. [Booking Flow — Sequence Diagram](#booking-flow--sequence-diagram)
9. [Theatre Onboarding Flow](#theatre-onboarding-flow)
10. [Show Creation Flow](#show-creation-flow)
11. [Pricing Engine Design](#pricing-engine-design)
12. [Caching Design](#caching-design)
13. [Exception Handling Design](#exception-handling-design)
14. [Persistence Design](#persistence-design)
15. [Deployment Architecture](#deployment-architecture)
16. [Configuration Reference](#configuration-reference)
17. [REST API Reference & cURL Examples](#rest-api-reference--curl-examples)
18. [Design Strengths](#design-strengths)
19. [Known Gaps and Risks](#known-gaps-and-risks)
20. [Recommended Next Steps](#recommended-next-steps)

---

## Purpose

This document describes the complete technical design of the **Booking Platform** — a Spring Boot movie ticket booking backend. It captures the architectural decisions, domain model, component interactions, data flows, caching strategy, and the full API surface with executable cURL examples.

The system supports:

- theatre partner onboarding (cities, theatres)
- movie catalog management
- show scheduling and seat inventory allocation
- customer show browsing by movie, city, and date
- seat selection and ticket booking with configurable pricing rules
- location directory lookup backed by an external API with caching

---

## System Overview

The application follows a **standard layered Spring Boot architecture**:

- **Controller layer** — HTTP REST endpoints and one Thymeleaf page
- **Service layer** — business logic, orchestration, pricing, and external API integration
- **Repository layer** — Spring Data JPA interfaces for PostgreSQL
- **Entity model** — JPA-mapped relational domain
- **Cache layer** — Ehcache via JCache (JSR-107), with Redis provisioned as a future option
- **Frontend** — Thymeleaf + vanilla JS single-page application served at `GET /`

---

## Architecture Diagram

```mermaid
flowchart TD
    Browser["🖥 Browser / API Client"]

    subgraph SpringApp["Spring Boot Application (port 8080)"]
        direction TB

        subgraph Controllers["Controller Layer"]
            PC["PageController\n(Thymeleaf UI)"]
            MC["MovieController\n/movies"]
            TC["TheatreController\n/partners/theatres"]
            SC["ShowController\n/partners/shows"]
            PSC["PublicShowController\n/shows/{id}/seats"]
            BC["BookingController\n/bookings"]
            BRC["BrowseController\n/browse/shows"]
            LC["LocationController\n/ui/locations"]
        end

        subgraph Services["Service Layer"]
            MS["MovieService"]
            TS["TheatreService"]
            SS["ShowService"]
            BS["BookingService"]
            BRS["BrowseService"]
            PE["PricingEngine"]
            LDS["LocationDirectoryService"]
        end

        subgraph Repos["Repository Layer (Spring Data JPA)"]
            MR["MovieRepository"]
            TR["TheatreRepository"]
            CR["CityRepository"]
            SR["ShowRepository"]
            SeR["SeatRepository"]
            BR["BookingRepository"]
        end

        subgraph Config["Configuration"]
            PP["PricingProperties\n(offer cities/theatres)"]
            CC["CacheConfig\n(Ehcache/Redis)"]
        end
    end

    subgraph Infra["Infrastructure"]
        PG[("PostgreSQL 15\nport 5432")]
        EHCACHE[("Ehcache\n(in-process JVM)")]
        REDIS[("Redis 8\nport 6379\n(inactive — future)")]
        EXTAPI["countriesnow.space\n(external REST API)"]
    end

    Browser --> Controllers
    PC --> MS
    MC --> MS
    TC --> TS
    SC --> SS
    PSC --> SS
    BC --> BS
    BRC --> BRS
    LC --> LDS

    MS --> MR
    TS --> TR
    TS --> CR
    SS --> SR
    SS --> MR
    SS --> TR
    SS --> SeR
    BS --> SR
    BS --> SeR
    BS --> BR
    BS --> PE
    BRS --> SR
    PE --> PP

    Repos --> PG
    MS --> EHCACHE
    LDS --> EHCACHE
    LDS --> EXTAPI
    CC -.->|"configured but inactive"| REDIS
```

---

## Component Architecture

```mermaid
flowchart LR
    subgraph PartnerAPIs["Partner APIs"]
        P1["POST /partners/theatres\nOnboard theatre"]
        P2["GET /partners/theatres\nList theatres"]
        P3["GET /partners/theatres/cities\nList cities"]
        P4["POST /partners/shows\nCreate show"]
        P5["POST /partners/shows/{id}/seats\nAllocate seats"]
    end

    subgraph CustomerAPIs["Customer APIs"]
        C1["GET /browse/shows\nBrowse by movie+city+date"]
        C2["GET /shows/{id}/seats\nSeat availability"]
        C3["POST /bookings\nBook tickets"]
        C4["GET /bookings/{id}\nGet booking"]
    end

    subgraph CatalogAPIs["Catalog APIs"]
        D1["POST /movies\nCreate movie"]
        D2["GET /movies\nList movies"]
        D3["GET /movies/{id}\nGet movie (cached)"]
    end

    subgraph LocationAPIs["Location APIs"]
        L1["GET /ui/locations/countries"]
        L2["GET /ui/locations/cities?country="]
    end

    subgraph UILayer["UI"]
        U1["GET /\nThymeleaf homepage"]
    end

    PartnerAPIs --> TheatreService
    PartnerAPIs --> ShowService
    CustomerAPIs --> BrowseService
    CustomerAPIs --> ShowService
    CustomerAPIs --> BookingService
    CatalogAPIs --> MovieService
    LocationAPIs --> LocationDirectoryService

    BookingService --> PricingEngine
    PricingEngine --> PricingProperties
```

---

## Domain Model

### Entities

| Entity | Table | Key Fields |
|---|---|---|
| `City` | `city` | `id`, `name (unique)` |
| `Theatre` | `theatre` | `id`, `name`, `city (FK)` |
| `Movie` | `movie` | `id`, `title`, `genre`, `language` |
| `Show` | `show` | `id`, `movie (FK)`, `theatre (FK)`, `showDate`, `showTime`, `price` |
| `Seat` | `seat` | `id`, `seatNumber`, `show (FK)`, `isBooked` |
| `Booking` | `booking` | `id`, `show (FK)`, `seats (M2M)`, `totalPrice`, `createdAt` |

### Request / Response Types (non-entity)

| Type | Kind | Purpose |
|---|---|---|
| `BookingRequest` | DTO | `showId` + `List<String> seats` |
| `ShowRequest` | DTO | `movieId`, `theatreId`, `showDate`, `showTime`, `price` |
| `SeatInventoryRequest` | DTO | `List<String> seatNumbers` |
| `TheatreOnboardingRequest` | DTO | `theatreName`, `cityName` |
| `TheatreResponse` | Record | `id`, `theatreName`, `cityName` |
| `SeatAvailabilityResponse` | Record | `id`, `seatNumber`, `booked` |

---

## Entity Relationship Diagram

```mermaid
erDiagram
    CITY {
        bigint id PK
        varchar name UK
    }
    THEATRE {
        bigint id PK
        varchar name
        bigint city_id FK
    }
    MOVIE {
        bigint id PK
        varchar title
        varchar genre
        varchar language
    }
    SHOW {
        bigint id PK
        bigint movie_id FK
        bigint theatre_id FK
        date show_date
        time show_time
        double price
    }
    SEAT {
        bigint id PK
        varchar seat_number
        bigint show_id FK
        boolean is_booked
    }
    BOOKING {
        bigint id PK
        bigint show_id FK
        double total_price
        timestamp created_at
    }
    BOOKING_SEAT {
        bigint booking_id FK
        bigint seat_id FK
    }

    CITY ||--o{ THEATRE : "has many"
    THEATRE ||--o{ SHOW : "hosts"
    MOVIE ||--o{ SHOW : "screened as"
    SHOW ||--o{ SEAT : "has"
    SHOW ||--o{ BOOKING : "booked for"
    BOOKING ||--o{ BOOKING_SEAT : "links"
    SEAT ||--o{ BOOKING_SEAT : "linked by"
```

---

## Layered Architecture

```mermaid
flowchart TB
    subgraph HTTP["HTTP Layer"]
        REQ["Incoming HTTP Request"]
        RES["HTTP Response (JSON / HTML)"]
    end

    subgraph ControllerLayer["Controller Layer — @RestController / @Controller"]
        direction LR
        CTR["Route mapping\nRequest deserialisation\nResponse serialisation\nSwagger annotations"]
    end

    subgraph ServiceLayer["Service Layer — @Service / @Component"]
        direction LR
        SVC["Business logic\nTransaction boundaries (@Transactional)\nInput validation\nOrchestration"]
    end

    subgraph EngineLayer["Engine / Config Layer"]
        direction LR
        ENG["PricingEngine — pricing rules\nPricingProperties — @ConfigurationProperties\nLocationDirectoryService — external API + cache"]
    end

    subgraph RepoLayer["Repository Layer — JpaRepository"]
        direction LR
        REPO["CRUD operations\nJPQL queries\nDerived query methods"]
    end

    subgraph PersistenceLayer["Persistence Layer"]
        direction LR
        JPA["Hibernate ORM\nJPA entity mapping\n@Entity / @Table / @Column"]
    end

    subgraph CacheLayer["Cache Layer — JCache / Ehcache"]
        CACHE["@Cacheable interceptors\nIn-process heap + off-heap\nEhcache.xml TTL config"]
    end

    DB[("PostgreSQL 15")]

    REQ --> ControllerLayer
    ControllerLayer --> ServiceLayer
    ServiceLayer --> EngineLayer
    ServiceLayer --> RepoLayer
    RepoLayer --> PersistenceLayer
    PersistenceLayer --> DB
    ServiceLayer <--> CacheLayer
    ControllerLayer --> RES
```

---

## Booking Flow — Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant BookingController
    participant BookingService
    participant ShowRepository
    participant SeatRepository
    participant PricingEngine
    participant PricingProperties
    participant BookingRepository

    Client->>BookingController: POST /bookings {showId, seats:[]}
    BookingController->>BookingService: bookTickets(showId, seatNumbers)

    BookingService->>BookingService: normalizeSeatNumbers()\nvalidate non-empty, unique, non-blank
    alt invalid seat list
        BookingService-->>Client: 400 InvalidBookingRequestException
    end

    BookingService->>ShowRepository: findById(showId)
    alt show not found
        ShowRepository-->>BookingService: empty Optional
        BookingService-->>Client: 404 ResourceNotFoundException
    end
    ShowRepository-->>BookingService: Show

    BookingService->>SeatRepository: findByShowIdAndSeatNumberIn(showId, seatNumbers)
    SeatRepository-->>BookingService: List<Seat>

    BookingService->>BookingService: validateSeatSelection()\ncheck requested vs found count
    alt unknown seats
        BookingService-->>Client: 400 InvalidBookingRequestException
    end

    loop each Seat
        BookingService->>BookingService: check seat.isBooked()
        alt seat already booked
            BookingService-->>Client: 409 SeatUnavailableException
        end
    end

    BookingService->>BookingService: mark all seats isBooked = true

    BookingService->>PricingEngine: calculatePrice(seats, show)
    PricingEngine->>PricingProperties: isThirdTicketOfferEligible(show)
    PricingProperties-->>PricingEngine: true / false
    PricingEngine->>PricingEngine: apply 3rd-ticket discount (if eligible)\napply afternoon discount (if 12:00–16:00)
    PricingEngine-->>BookingService: totalPrice (double)

    BookingService->>BookingRepository: save(Booking)
    BookingRepository-->>BookingService: Booking (with id)
    BookingService-->>BookingController: Booking
    BookingController-->>Client: 200 OK — Booking JSON
```

---

## Theatre Onboarding Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant TheatreController
    participant TheatreService
    participant TheatreRepository
    participant CityRepository

    Client->>TheatreController: POST /partners/theatres {theatreName, cityName}
    TheatreController->>TheatreService: onboardTheatre(request)

    TheatreService->>TheatreService: validate non-blank theatreName and cityName
    alt missing fields
        TheatreService-->>Client: 400 InvalidBookingRequestException
    end

    TheatreService->>TheatreRepository: existsByNameAndCityNameIgnoreCase(theatreName, cityName)
    alt theatre already exists in that city
        TheatreRepository-->>TheatreService: true
        TheatreService-->>Client: 400 "Theatre is already onboarded for city"
    end

    TheatreService->>CityRepository: findByNameIgnoreCase(cityName)
    alt city not found
        CityRepository-->>TheatreService: empty Optional
        TheatreService->>CityRepository: save(new City(cityName))
        CityRepository-->>TheatreService: City (persisted)
    else city found
        CityRepository-->>TheatreService: City
    end

    TheatreService->>TheatreRepository: save(Theatre{name, city})
    TheatreRepository-->>TheatreService: Theatre (persisted)
    TheatreService-->>TheatreController: Theatre
    TheatreController-->>Client: 201 Created — TheatreResponse JSON
```

---

## Show Creation Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant ShowController
    participant ShowService
    participant MovieRepository
    participant TheatreRepository
    participant ShowRepository
    participant SeatRepository

    Client->>ShowController: POST /partners/shows {movieId, theatreId, showDate, showTime, price}
    ShowController->>ShowService: createShow(request)

    ShowService->>ShowService: validate all required fields and positive price
    alt invalid request
        ShowService-->>Client: 400 InvalidBookingRequestException
    end

    ShowService->>MovieRepository: findById(movieId)
    alt movie not found
        ShowService-->>Client: 404 ResourceNotFoundException
    end

    ShowService->>TheatreRepository: findById(theatreId)
    alt theatre not found
        ShowService-->>Client: 404 ResourceNotFoundException
    end

    ShowService->>ShowRepository: save(Show)
    ShowRepository-->>ShowService: Show (persisted)
    ShowService-->>ShowController: Show
    ShowController-->>Client: 201 Created — Show JSON

    note over Client, SeatRepository: Allocate seat inventory (separate call)
    Client->>ShowController: POST /partners/shows/{showId}/seats {seatNumbers:[]}
    ShowController->>ShowService: allocateSeats(showId, request)
    ShowService->>ShowRepository: findById(showId)
    ShowService->>SeatRepository: findByShowId(showId) — check for duplicates
    ShowService->>SeatRepository: saveAll(List<Seat>)
    ShowController-->>Client: 201 Created — List<Seat> JSON
```

---

## Pricing Engine Design

```mermaid
flowchart TD
    Start(["calculatePrice(seats, show)"])
    Base["base = seats.size() × show.price"]
    CheckQty{"seats.size() >= 3?"}
    CheckOffer{"pricingProperties\n.isThirdTicketOfferEligible(show)?"}
    Apply3rd["base -= show.price × 0.5\n(50% off 3rd ticket)"]
    CheckTime{"12:00 ≤ showTime < 16:00?"}
    ApplyAfternoon["base *= 0.8\n(20% afternoon discount)"]
    Return(["return base"])

    Start --> Base
    Base --> CheckQty
    CheckQty -- No --> CheckTime
    CheckQty -- Yes --> CheckOffer
    CheckOffer -- No --> CheckTime
    CheckOffer -- Yes --> Apply3rd
    Apply3rd --> CheckTime
    CheckTime -- No --> Return
    CheckTime -- Yes --> ApplyAfternoon
    ApplyAfternoon --> Return
```

### Pricing Properties Logic

```mermaid
flowchart TD
    A["isThirdTicketOfferEligible(show)"]
    B{"thirdTicketOfferCities\nlist empty?"}
    C{"thirdTicketOfferTheatres\nlist empty?"}
    D["return false\n(no rules configured)"]
    E["cityMatch = city in configured list?"]
    F["theatreMatch = theatre in configured list?"]
    G{"Both city AND\ntheatre rules set?"}
    H["return cityMatch AND theatreMatch"]
    I{"Only city rules?"}
    J["return cityMatch"]
    K["return theatreMatch"]

    A --> B
    B -- Yes --> C
    C -- Yes --> D
    B -- No --> E
    C -- No --> F
    E --> G
    F --> G
    G -- Yes --> H
    G -- No --> I
    I -- Yes --> J
    I -- No --> K
```

### Pricing Examples

| Scenario | Seats | Price | City / Theatre | Time | Total |
|---|---|---|---|---|---|
| Eligible location + afternoon | 3 | ₹100 | Mumbai / PVR Andheri | 13:00 | **₹200.0** |
| Non-eligible location + afternoon | 3 | ₹100 | Delhi / Downtown Screens | 13:00 | **₹240.0** |
| Eligible location + evening | 3 | ₹200 | Mumbai / PVR Andheri | 18:00 | **₹500.0** |
| Non-eligible + evening, fewer seats | 2 | ₹150 | Delhi / Downtown Screens | 18:00 | **₹300.0** |

---

## Caching Design

```mermaid
flowchart LR
    subgraph Application["Spring Boot JVM"]
        MS["MovieService\n@Cacheable('movies')\nkey = id"]
        LDS1["LocationDirectoryService\n@Cacheable('locationDirectory')\nkey = 'all'"]
        LDS2["LocationDirectoryService\n@Cacheable('locationCountries')\nkey = 'all'"]
        LDS3["LocationDirectoryService\n@Cacheable('locationCities')\nkey = country"]
    end

    subgraph EhcacheHeap["Ehcache (In-process)"]
        C1["Cache: movies\nKey: Long → Movie\nTTL: 60 min\nHeap: 1000 entries\nOff-heap: 10 MB"]
        C2["Cache: locationDirectory\nKey: String → LinkedHashMap\nTTL: 180 min\nHeap: 20 entries\nOff-heap: 5 MB"]
        C3["Cache: locationCountries\nKey: String → ArrayList\nTTL: 180 min\nHeap: 10 entries\nOff-heap: 2 MB"]
        C4["Cache: locationCities\nKey: String → List\nTTL: 180 min\nHeap: 100 entries\nOff-heap: 5 MB"]
    end

    MS --> C1
    LDS1 --> C2
    LDS2 --> C3
    LDS3 --> C4

    ExternalAPI["countriesnow.space\nexternal REST API"]
    LDS1 -->|"cache miss"| ExternalAPI
    ExternalAPI -->|"fallback on error"| FallbackMap["In-memory fallback:\nIndia, UAE, UK, US"]
```

### Cache Summary

| Cache Name | Key | Value Type | TTL | Heap | Off-Heap |
|---|---|---|---|---|---|
| `movies` | `Long` (movie id) | `Movie` | 60 min | 1 000 entries | 10 MB |
| `locationDirectory` | `"all"` | `LinkedHashMap<String, List<String>>` | 180 min | 20 entries | 5 MB |
| `locationCountries` | `"all"` | `ArrayList<String>` | 180 min | 10 entries | 2 MB |
| `locationCities` | `country` (lowercase) | `List<String>` | 180 min | 100 entries | 5 MB |

### Ehcache vs Redis Decision

| Area | Ehcache (active) | Redis (future) |
|---|---|---|
| Deployment model | Embedded in JVM | External standalone server |
| Network hop | None | Yes |
| Setup complexity | Low | Medium |
| Local dev experience | Excellent | Requires external service |
| Horizontal scaling | Limited | Strong |
| Shared cache across replicas | No | Yes |
| Current fit | ✅ Ideal | 🔜 Future path |

---

## Exception Handling Design

```mermaid
flowchart TD
    REQ["Incoming Request"]
    CTR["Controller"]
    SVC["Service"]
    EH["@RestControllerAdvice\nApiExceptionHandler"]

    SVC -- "ResourceNotFoundException" --> EH
    SVC -- "SeatUnavailableException" --> EH
    SVC -- "InvalidBookingRequestException" --> EH
    JPA["Spring / JPA"] -- "DataIntegrityViolationException" --> EH
    ANY["Any other Exception"] --> EH

    EH -- "404 Not Found" --> R1["{ timestamp, status: 404, error, message }"]
    EH -- "409 Conflict" --> R2["{ timestamp, status: 409, error, message }"]
    EH -- "400 Bad Request" --> R3["{ timestamp, status: 400, error, message }"]
    EH -- "500 Internal Server Error" --> R4["{ timestamp, status: 500, error, message }"]

    REQ --> CTR --> SVC
```

### Error Response Shape

```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Show not found: 99"
}
```

---

## Persistence Design

### Schema (createTables.sql)

```sql
CREATE TABLE city (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE theatre (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    city_id INT NOT NULL REFERENCES city(id)
);

CREATE TABLE movie (
    id       SERIAL PRIMARY KEY,
    title    VARCHAR(100) NOT NULL,
    genre    VARCHAR(50)  NOT NULL,
    language VARCHAR(50)  NOT NULL
);

CREATE TABLE show (
    id         SERIAL PRIMARY KEY,
    movie_id   INT    NOT NULL REFERENCES movie(id),
    theatre_id INT    NOT NULL REFERENCES theatre(id),
    show_date  DATE   NOT NULL,
    show_time  TIME   NOT NULL,
    price      DOUBLE PRECISION NOT NULL
);

CREATE TABLE seat (
    id          SERIAL PRIMARY KEY,
    show_id     INT          NOT NULL REFERENCES show(id),
    seat_number VARCHAR(10)  NOT NULL,
    is_booked   BOOLEAN      DEFAULT FALSE,
    UNIQUE (show_id, seat_number)
);

CREATE TABLE booking (
    id          SERIAL PRIMARY KEY,
    show_id     INT    NOT NULL REFERENCES show(id),
    total_price DOUBLE PRECISION NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_seat (
    booking_id INT NOT NULL REFERENCES booking(id),
    seat_id    INT NOT NULL REFERENCES seat(id),
    PRIMARY KEY (booking_id, seat_id)
);
```

### Custom JPQL Queries

```java
// ShowRepository — filter by movie, city (case-insensitive), and date
@Query("""
    select s from Show s
    where s.movie.id = :movieId
      and lower(s.theatre.city.name) = lower(:city)
      and s.showDate = :showDate
    """)
List<Show> findShowsByMovieCityAndDate(Long movieId, String city, LocalDate showDate);

// TheatreRepository — duplicate check (case-insensitive)
@Query("""
    select count(t) > 0 from Theatre t
    where lower(t.name) = lower(:name)
      and lower(t.city.name) = lower(:cityName)
    """)
boolean existsByNameAndCityNameIgnoreCase(String name, String cityName);

// TheatreRepository — optional city filter, ordered
@Query("""
    select t from Theatre t
    where (:cityName is null or lower(t.city.name) = lower(:cityName))
    order by t.city.name asc, t.name asc
    """)
List<Theatre> findByOptionalCityOrdered(String cityName);
```

---

## Deployment Architecture

```mermaid
flowchart TD
    subgraph DockerCompose["docker-compose.yml"]
        APP["app container\nSpring Boot 8080\neclipse-temurin:21-alpine"]
        PG["db container\nPostgreSQL 15\nport 5432"]
        RD["redis container\nRedis 8\nport 6379"]
    end

    APP -- "JDBC jdbc:postgresql://db:5432/booking_db" --> PG
    APP -.->|"provisioned, inactive"| RD

    DEV["Local developer machine"] -- "localhost:8080" --> APP
    DEV -- "localhost:5432" --> PG
    DEV -- "localhost:6379" --> RD
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY target/booking-platform-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## Configuration Reference

### `application.yaml`

```yaml
spring:
  application:
    name: booking-platform
  profiles:
    active: dev
```

### `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
  jpa:
    hibernate:
      ddl-auto: update
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml

booking:
  pricing:
    third-ticket-offer-cities:
      - Mumbai
      - Bengaluru
    third-ticket-offer-theatres:
      - PVR Andheri
      - Forum Mall Screens
```

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USER` | `postgres` | PostgreSQL username |
| `DB_PASS` | `postgres` | PostgreSQL password |

---

## REST API Reference & cURL Examples

> Base URL: `http://localhost:8080`

---

### 1. Movie APIs

#### `POST /movies` — Create a movie

**Request**
```json
{
  "title": "Interstellar",
  "genre": "Sci-Fi",
  "language": "English"
}
```

**cURL**
```bash
curl -s -X POST http://localhost:8080/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Interstellar","genre":"Sci-Fi","language":"English"}' | jq .
```

**Response `200 OK`**
```json
{
  "id": 1,
  "title": "Interstellar",
  "genre": "Sci-Fi",
  "language": "English"
}
```

---

#### `GET /movies` — List all movies (with optional filters)

**cURL — all movies**
```bash
curl -s http://localhost:8080/movies | jq .
```

**cURL — filter by genre**
```bash
curl -s "http://localhost:8080/movies?genre=Sci-Fi" | jq .
```

**cURL — filter by language**
```bash
curl -s "http://localhost:8080/movies?language=English" | jq .
```

**cURL — filter by genre and language**
```bash
curl -s "http://localhost:8080/movies?genre=Sci-Fi&language=English" | jq .
```

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "title": "Interstellar",
    "genre": "Sci-Fi",
    "language": "English"
  }
]
```

---

#### `GET /movies/{id}` — Get movie by ID (cache-enabled)

**cURL**
```bash
curl -s http://localhost:8080/movies/1 | jq .
```

**Response `200 OK`**
```json
{
  "id": 1,
  "title": "Interstellar",
  "genre": "Sci-Fi",
  "language": "English"
}
```

**Response `404 Not Found`**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Movie not found: 99"
}
```

---

### 2. Partner — Theatre APIs

#### `POST /partners/theatres` — Onboard a theatre

**Request**
```json
{
  "theatreName": "PVR Andheri",
  "cityName": "Mumbai"
}
```

**cURL**
```bash
curl -s -X POST http://localhost:8080/partners/theatres \
  -H "Content-Type: application/json" \
  -d '{"theatreName":"PVR Andheri","cityName":"Mumbai"}' | jq .
```

**Response `201 Created`**
```json
{
  "id": 1,
  "theatreName": "PVR Andheri",
  "cityName": "Mumbai"
}
```

**Response `400 Bad Request` (duplicate)**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Theatre is already onboarded for city: Mumbai"
}
```

---

#### `GET /partners/theatres` — List theatres (optional city filter)

**cURL — all theatres**
```bash
curl -s http://localhost:8080/partners/theatres | jq .
```

**cURL — filter by city**
```bash
curl -s "http://localhost:8080/partners/theatres?city=Mumbai" | jq .
```

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "theatreName": "PVR Andheri",
    "cityName": "Mumbai"
  },
  {
    "id": 2,
    "theatreName": "Forum Mall Screens",
    "cityName": "Bengaluru"
  }
]
```

---

#### `GET /partners/theatres/cities` — List all onboarded cities

**cURL**
```bash
curl -s http://localhost:8080/partners/theatres/cities | jq .
```

**Response `200 OK`**
```json
["Bengaluru", "Mumbai"]
```

---

### 3. Partner — Show APIs

#### `POST /partners/shows` — Create a show

**Request**
```json
{
  "movieId": 1,
  "theatreId": 1,
  "showDate": "2026-04-10",
  "showTime": "13:00",
  "price": 100.0
}
```

**cURL**
```bash
curl -s -X POST http://localhost:8080/partners/shows \
  -H "Content-Type: application/json" \
  -d '{
    "movieId": 1,
    "theatreId": 1,
    "showDate": "2026-04-10",
    "showTime": "13:00",
    "price": 100.0
  }' | jq .
```

**Response `201 Created`**
```json
{
  "id": 1,
  "movie": { "id": 1, "title": "Interstellar", "genre": "Sci-Fi", "language": "English" },
  "theatre": { "id": 1, "name": "PVR Andheri", "city": { "id": 1, "name": "Mumbai" } },
  "showDate": "2026-04-10",
  "showTime": "13:00:00",
  "price": 100.0
}
```

**Response `400 Bad Request` (missing field)**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "movieId, theatreId, showDate, showTime, and positive price are required"
}
```

---

#### `POST /partners/shows/{showId}/seats` — Allocate seat inventory

**Request**
```json
{
  "seatNumbers": ["A1", "A2", "A3", "A4", "A5"]
}
```

**cURL**
```bash
curl -s -X POST http://localhost:8080/partners/shows/1/seats \
  -H "Content-Type: application/json" \
  -d '{"seatNumbers":["A1","A2","A3","A4","A5"]}' | jq .
```

**Response `201 Created`**
```json
[
  { "id": 1, "seatNumber": "A1", "show": { "id": 1 }, "booked": false },
  { "id": 2, "seatNumber": "A2", "show": { "id": 1 }, "booked": false },
  { "id": 3, "seatNumber": "A3", "show": { "id": 1 }, "booked": false }
]
```

**Response `400 Bad Request` (duplicate seat)**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Seat(s) already allocated: A1, A2"
}
```

---

### 4. Customer — Browse APIs

#### `GET /browse/shows` — Browse shows by movie, city, and date

**cURL**
```bash
curl -s "http://localhost:8080/browse/shows?movieId=1&city=Mumbai&date=2026-04-10" | jq .
```

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "movie": { "id": 1, "title": "Interstellar", "genre": "Sci-Fi", "language": "English" },
    "theatre": {
      "id": 1,
      "name": "PVR Andheri",
      "city": { "id": 1, "name": "Mumbai" }
    },
    "showDate": "2026-04-10",
    "showTime": "13:00:00",
    "price": 100.0
  }
]
```

---

### 5. Customer — Seat Availability

#### `GET /shows/{showId}/seats` — View seat availability for a show

**cURL**
```bash
curl -s http://localhost:8080/shows/1/seats | jq .
```

**Response `200 OK`**
```json
[
  { "id": 1, "seatNumber": "A1", "booked": false },
  { "id": 2, "seatNumber": "A2", "booked": false },
  { "id": 3, "seatNumber": "A3", "booked": true },
  { "id": 4, "seatNumber": "A4", "booked": false },
  { "id": 5, "seatNumber": "A5", "booked": false }
]
```

---

### 6. Customer — Booking APIs

#### `POST /bookings` — Book tickets

**Request**
```json
{
  "showId": 1,
  "seats": ["A1", "A2", "A3"]
}
```

**cURL**
```bash
curl -s -X POST http://localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -d '{"showId":1,"seats":["A1","A2","A3"]}' | jq .
```

**Response `200 OK`** *(Mumbai / PVR Andheri matinee — both discounts applied)*
```json
{
  "id": 1,
  "show": {
    "id": 1,
    "movie": { "id": 1, "title": "Interstellar", "genre": "Sci-Fi", "language": "English" },
    "theatre": { "id": 1, "name": "PVR Andheri", "city": { "id": 1, "name": "Mumbai" } },
    "showDate": "2026-04-10",
    "showTime": "13:00:00",
    "price": 100.0
  },
  "seats": [
    { "id": 1, "seatNumber": "A1", "booked": true },
    { "id": 2, "seatNumber": "A2", "booked": true },
    { "id": 3, "seatNumber": "A3", "booked": true }
  ],
  "totalPrice": 200.0,
  "createdAt": "2026-04-10T13:05:00"
}
```

**Response `400 Bad Request` — unknown seat**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Unknown seat(s): Z99"
}
```

**Response `409 Conflict` — seat already booked**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Seat already booked: A1"
}
```

**Response `404 Not Found` — show not found**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Show not found: 99"
}
```

---

#### `GET /bookings/{id}` — Get a booking by ID

**cURL**
```bash
curl -s http://localhost:8080/bookings/1 | jq .
```

**Response `200 OK`**
```json
{
  "id": 1,
  "show": { "id": 1, "showDate": "2026-04-10", "showTime": "13:00:00", "price": 100.0 },
  "totalPrice": 200.0,
  "createdAt": "2026-04-10T13:05:00"
}
```

**Response `404 Not Found`**
```json
{
  "timestamp": "2026-04-04T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": null
}
```

---

### 7. Location Directory APIs

#### `GET /ui/locations/countries` — List all countries

**cURL**
```bash
curl -s http://localhost:8080/ui/locations/countries | jq .
```

**Response `200 OK`**
```json
["Afghanistan", "Albania", "Algeria", "...", "India", "...", "Zimbabwe"]
```

---

#### `GET /ui/locations/cities` — List cities for a country

**cURL — default (India)**
```bash
curl -s "http://localhost:8080/ui/locations/cities" | jq .
```

**cURL — specific country**
```bash
curl -s "http://localhost:8080/ui/locations/cities?country=India" | jq .
```

**cURL — another country**
```bash
curl -s "http://localhost:8080/ui/locations/cities?country=United+States" | jq .
```

**Response `200 OK`**
```json
["Bengaluru", "Chennai", "Delhi", "Hyderabad", "Kolkata", "Mumbai", "Pune"]
```

---

### 8. UI Endpoints

#### `GET /` — Thymeleaf Homepage

**cURL**
```bash
curl -s http://localhost:8080/ | head -30
```

Returns the rendered HTML page served by `PageController` using the `index.html` Thymeleaf template.

---

### 9. OpenAPI / Swagger UI

Available after application start at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:

```bash
curl -s http://localhost:8080/v3/api-docs | jq .
```

---

### Full End-to-End cURL Sequence

The following sequence seeds a complete booking scenario from scratch.

```bash
# Step 1: Create a movie
MOVIE_ID=$(curl -s -X POST http://localhost:8080/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Interstellar","genre":"Sci-Fi","language":"English"}' | jq -r '.id')
echo "Movie ID: $MOVIE_ID"

# Step 2: Onboard a theatre in Mumbai
THEATRE_ID=$(curl -s -X POST http://localhost:8080/partners/theatres \
  -H "Content-Type: application/json" \
  -d '{"theatreName":"PVR Andheri","cityName":"Mumbai"}' | jq -r '.id')
echo "Theatre ID: $THEATRE_ID"

# Step 3: Create an afternoon show (eligible for both discounts)
SHOW_ID=$(curl -s -X POST http://localhost:8080/partners/shows \
  -H "Content-Type: application/json" \
  -d "{\"movieId\":$MOVIE_ID,\"theatreId\":$THEATRE_ID,\"showDate\":\"2026-04-10\",\"showTime\":\"13:00\",\"price\":100.0}" | jq -r '.id')
echo "Show ID: $SHOW_ID"

# Step 4: Allocate seats for the show
curl -s -X POST http://localhost:8080/partners/shows/$SHOW_ID/seats \
  -H "Content-Type: application/json" \
  -d '{"seatNumbers":["A1","A2","A3","A4","A5"]}' | jq .

# Step 5: Browse shows
curl -s "http://localhost:8080/browse/shows?movieId=$MOVIE_ID&city=Mumbai&date=2026-04-10" | jq .

# Step 6: Check seat availability
curl -s http://localhost:8080/shows/$SHOW_ID/seats | jq .

# Step 7: Book 3 seats (triggers both discounts → totalPrice = 200.0)
BOOKING_ID=$(curl -s -X POST http://localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -d "{\"showId\":$SHOW_ID,\"seats\":[\"A1\",\"A2\",\"A3\"]}" | jq -r '.id')
echo "Booking ID: $BOOKING_ID"

# Step 8: Fetch the booking
curl -s http://localhost:8080/bookings/$BOOKING_ID | jq .
```

---

## Design Strengths

- **Clean layered separation** — each layer has a single responsibility; business logic never leaks into controllers or repositories
- **Configurable pricing** — offer cities and theatres are YAML-driven, not hard-coded
- **Graceful external API fallback** — `LocationDirectoryService` degrades to an in-memory map when `countriesnow.space` is unavailable
- **Auto-city creation** — `TheatreService` creates a City on first use, so city management is implicit and zero-friction
- **Duplicate-safe seat allocation** — `ShowService` checks for existing seat numbers before persisting
- **Structured error responses** — `ApiExceptionHandler` returns consistent JSON with timestamp, status, error, and message
- **Compact repository layer** — derived query methods and minimal JPQL keep the data layer thin
- **In-process caching** — Ehcache avoids network round-trips for hot read paths
- **Tested pricing scenarios** — `PricingEngineTest` and `BookingServiceTest` cover the core discount logic

---