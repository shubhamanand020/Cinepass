# Cinepass — Cinema Microservices Backend Platform

An enterprise distributed cinema ticket booking backend platform built on **Java 21**, **Spring Boot 4.0.8**, **Spring Cloud 2025.1.3**, **Spring Security 7.0.7**, and **PostgreSQL 18**.

Cinepass provides an end-to-end digital cinema experience: from movie discovery and multi-theatre showtime scheduling to atomic 5-minute seat locking, automated PDF ticket generation with embedded QR passes, and real-time gate admission scanning.

---

## 1. System Architecture & Topology

Cinepass adheres to a strict **Microservices Architecture (Database-per-Service)**. All external requests from web and mobile clients route through the API Gateway, which discovers downstream services via Eureka Service Registry.

```
                              [ React Frontend / Mobile Clients ]
                                              │
                                              ▼
                             [ API Gateway (Port 8080) ]
                                              │
                     ┌────────────────────────┼────────────────────────┐
                     │ (Service Discovery via Eureka Port 8761)        │
                     ▼                        ▼                        ▼
           [ User Service ]          [ Movie Service ]       [ Show Service ]
              Port 8081                 Port 8082               Port 8084
                  │                         │                       │
                  ▼                         ▼                       ▼
          [ cinepass_user_db ]     [ cinepass_movie_db ]   [ cinepass_show_db ]
                                                                    ▲
                                                                    │ (OpenFeign)
                                                                    ▼
                                                         [ Booking Service ]
                                                            Port 8083
                                                                │
                                                                ▼
                                                      [ cinepass_booking_db ]
```

### Microservice Directory

| Service | Port | Database | Primary Responsibility |
|---|---:|---|---|
| **Eureka Server** | `8761` | *None* | Centralized service registration and dynamic instance discovery |
| **API Gateway** | `8080` | *None* | Single entry point, JWT forwarding, route filtering, and CORS policies |
| **User Service** | `8081` | `cinepass_user_db` | User registration, BCrypt password hashing, JWT issuance, and profile management |
| **Movie Service** | `8082` | `cinepass_movie_db` | Canonical movie catalog CRUD, genres, ratings, languages, and poster media |
| **Show Service** | `8084` | `cinepass_show_db` | Theatre screens, seating layout configurations, and multi-time show schedules |
| **Booking Service** | `8083` | `cinepass_booking_db` | 5-minute atomic seat holds, payment confirmation, ticket QR code/PDF generation, and admission verification |

---

## 2. Core Platform Capabilities

### A. 5-Minute Atomic Seat Hold Engine
To prevent race conditions and double-booking during high-demand blockbusters, the Booking Service implements an atomic seat reservation workflow:
1. When a user selects seats, a `POST /api/bookings/hold` request locks the seats with a `HELD` status.
2. A composite database constraint `(show_id, seat_number)` physically guarantees that no two concurrent transactions can hold or book the same seat.
3. A background scheduled task sweeps the booking database every 60 seconds, releasing any held seats whose `heldUntil` timestamp has expired back to `AVAILABLE`.

### B. Role-Based Access Control (RBAC)
The platform enforces role separation using stateless JWT tokens signed with HMAC-SHA256:
- **`ROLE_USER`**: Allows browsing the movie catalog, viewing schedules, holding seats, confirming bookings, downloading PDF tickets, and viewing personal booking history.
- **`ROLE_ADMIN`**: Grants administrative privileges to create/edit movies, define auditorium screens and layout dimensions, schedule showtimes, review real-time booking audits, and verify tickets at the gate.

### C. Digital Ticketing, QR Codes & Gate Admission
- Upon booking confirmation, a unique ticket token (`UUID`) is generated and stamped into the database.
- A high-resolution QR code is dynamically rendered and embedded directly into a downloadable official PDF ticket using iText and ZXing.
- The Admin Console features a live **Gate Admission & Ticket Scanner**. Scanning or submitting a ticket token invokes `POST /api/bookings/verify-ticket`:
  - **Fresh Valid Admission (`200 OK`)**: Marks the ticket as `checkedIn = true`, records the check-in timestamp, and grants entry.
  - **Duplicate Check-in Alert (`200 OK`)**: Detects already checked-in passes, preserves the original admission timestamp, and warns gate staff.
  - **Invalid Ticket (`404 Not Found`)**: Flags unrecognized or forged tokens immediately.

---

## 3. Database Architecture

Each microservice manages its own isolated PostgreSQL database. Cross-database queries and foreign keys are strictly prohibited:

- **`cinepass_user_db`**: Stores users, hashed passwords, roles (`ROLE_USER`, `ROLE_ADMIN`), contact information, and account preferences.
- **`cinepass_movie_db`**: Stores movie titles, descriptions, release years, duration, genres, languages, trailer URLs, and poster images.
- **`cinepass_show_db`**: Stores screen auditoriums (VIP, IMAX, Standard layouts) and scheduled showtimes referencing `movieId` and `screenId`.
- **`cinepass_booking_db`**: Stores bookings, held/confirmed seat records, ticket verification tokens, check-in status, and payment audits.

---

## 4. API Gateway Endpoints

All external client traffic routes through the API Gateway at `http://localhost:8080`:

### Authentication & Users (`user-service`)
- `POST /api/users` — Register a new cinema account
- `POST /api/auth/login` — Authenticate and receive a JWT Bearer token
- `GET /api/users/profile` — Fetch currently authenticated user profile
- `PUT /api/users/profile` — Update user details and preferences

### Movie Catalog (`movie-service`)
- `GET /api/movies` — List all active movies
- `GET /api/movies/{id}` — Get detailed movie information
- `POST /api/movies` — Create a movie (*Admin only*)
- `PUT /api/movies/{id}` — Update movie details (*Admin only*)
- `DELETE /api/movies/{id}` — Delete movie from catalog (*Admin only*)

### Screens & Showtimes (`show-service`)
- `GET /api/screens` — List all screens and seating layouts
- `POST /api/screens` — Register a new theatre auditorium (*Admin only*)
- `GET /api/shows?movieId={id}` — List scheduled showtimes for a movie
- `GET /api/shows/{id}` — Retrieve show details and pricing
- `POST /api/shows` — Schedule a new showtime (*Admin only*)
- `DELETE /api/shows/{id}` — Cancel/delete a scheduled show (*Admin only*)

### Bookings & Admission (`booking-service`)
- `GET /api/shows/{showId}/availability` — Real-time seat layout availability
- `POST /api/bookings/hold` — Atomically hold seats for 5 minutes
- `POST /api/bookings/{id}/confirm` — Finalize booking and issue ticket
- `POST /api/bookings/{id}/cancel` — Release held or booked seats
- `GET /api/bookings` — Fetch customer's booking history
- `GET /api/bookings/{id}` — Retrieve booking details and QR pass
- `GET /api/bookings/{id}/ticket` — Download official printable PDF ticket
- `POST /api/bookings/verify-ticket` — Gate admission validation (*Admin only*)

---

## 5. Development & Running in Spring Tool Suite (STS4)

### Prerequisites
- **JDK 21** installed and configured in your environment.
- **PostgreSQL 18** running locally with the four databases created:
  ```sql
  CREATE DATABASE cinepass_user_db;
  CREATE DATABASE cinepass_movie_db;
  CREATE DATABASE cinepass_show_db;
  CREATE DATABASE cinepass_booking_db;
  ```

### Startup Order
In Spring Tool Suite (STS4) or via terminal, start the services in the following order:
1. **`eureka_server`** (Wait until `http://localhost:8761` is accessible)
2. **`api-gateway`**
3. **`user-service`**
4. **`movie-service`**
5. **`show-service`**
6. **`booking-service`**

### Building via Maven Wrapper
To build all microservices from the command line:
```powershell
cd Backend
.\mvnw.cmd clean package -DskipTests
```
