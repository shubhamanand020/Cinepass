# Cinepass Review 1

## Problem
Movie-ticket booking needs separate ownership for users, movies, scheduled shows, and seat reservations. A single application makes service ownership and concurrent seat allocation difficult to demonstrate.

## Proposed solution
Cinepass is a Spring Boot microservice system. Users authenticate once, discover movies and shows, select seats, hold them briefly, confirm the booking, or cancel it. Each business service owns its own PostgreSQL database and communicates through APIs rather than cross-database foreign keys.

## Architecture
- `user-service` (`8081`): registration, BCrypt password hashing, login, JWT issuance, and user data.
- `movie-service` (`8082`): canonical movie catalog CRUD.
- `show-service` (`8084`): screens, show schedules, `movieId` references, and generated seat maps.
- `booking-service` (`8083`): booking lifecycle, seat holds, availability, confirmation, cancellation, and hold expiry. It discovers show-service with Feign and Eureka.
- `eureka_server` (`8761`): service registry.
- `api-gateway` (`8080`): Eureka-backed external routing.

## Data ownership
Databases are `cinepass_user_db`, `cinepass_movie_db`, `cinepass_show_db`, and `cinepass_booking_db`. The show database stores IDs for movies and screens; it does not duplicate movie CRUD. The booking database stores `userId` and `showId` values and validates show/seat data through show-service.

## Authentication
`POST /api/users` registers a user. `POST /api/auth/login` verifies email and password and returns a one-hour HMAC JWT. User, movie write, and booking APIs use stateless Spring Security. Booking derives the user ID from the JWT subject and never accepts a client-supplied owner ID.

## Booking flow
1. Login and keep the returned bearer token.
2. Browse `GET /api/movies` and `GET /api/shows?movieId={movieId}`.
3. Read `GET /api/shows/{showId}/seats` or `GET /api/shows/{showId}/availability`.
4. Hold seats with `POST /api/bookings/hold` and a show ID plus seat numbers.
5. Confirm with `POST /api/bookings/{id}/confirm`, or cancel with `POST /api/bookings/{id}/cancel`.
6. Retrieve one booking with `GET /api/bookings/{id}` or the authenticated user's history with `GET /api/bookings`.

A unique `(show_id, seat_number)` constraint prevents duplicate allocation. Held seats expire after the configured duration and are released by the scheduled sweep.

## Gateway routes
The gateway routes users/auth to user-service, movies to movie-service, screens and shows to show-service, bookings to booking-service, and the exact availability path to booking-service before the general show route.

## Technology
Java 21, Spring Boot 4.0.8, Spring Cloud, Spring Web MVC, Spring Data JPA, PostgreSQL 18, Eureka, OpenFeign, Spring Security JWT resource-server support, BCrypt, Maven, and React as the planned frontend.

## Review 1 scope and status
Implemented for Review 1: service boundaries, unique ports, Eureka configuration, gateway routes, PostgreSQL configuration, JWT login/validation, canonical movie ownership, show scheduling, seat map generation, authenticated booking ownership, hold/confirm/cancel flow, and documentation.

Runtime integration verification remains dependent on local PostgreSQL databases and a running Eureka server. The repository build verifies compilation; live registration, database CRUD, login requests, and gateway routing require those external processes to be started.
