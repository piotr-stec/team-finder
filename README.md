# Team Finder

A REST API for organizing spontaneous sports matches. Users post events with a location and a player limit, others sign up.

## Features

- **Auth** — register and log in with JWT. Access tokens expire in 15 minutes, refresh tokens in 7 days.
- **Events** — create an event with sport type, location, date, max players, and description. Browse and filter by sport or date. Edit or cancel your own events.
- **Participants** — join or leave an event. Spots are counted automatically — the event flips to `FULL` when the limit is reached and back to `OPEN` when someone leaves.
- **Admin** — manage users (enable/disable, delete) and remove events that break the rules.

## Status values

| Status | Meaning |
|---|---|
| `OPEN` | spots available |
| `FULL` | no spots left |
| `CANCELLED` | cancelled by the organizer |

## Stack

- Java 21, Spring Boot 3.3
- Spring Security + JWT (jjwt 0.12)
- Spring Data JPA + Hibernate + PostgreSQL
- Swagger UI (springdoc-openapi 2.5)
- Docker + Docker Compose
- GitHub Actions CI

## Running locally

**With Docker Compose:**
```bash
docker-compose up --build
```

**Dev mode (H2 in-memory):**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API: `http://localhost:8080`
Swagger: `http://localhost:8080/swagger-ui.html`

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/teamfinder` | database URL |
| `DB_USERNAME` | `postgres` | database user |
| `DB_PASSWORD` | `postgres` | database password |
| `JWT_SECRET` | dev default | Base64-encoded secret, min 256 bits — **change in production** |

Generate a secret:
```bash
openssl rand -base64 64
```

## API overview

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout

GET    /api/events              # public, supports ?sport=&from=&to=&page=&size=
POST   /api/events
GET    /api/events/{id}
PUT    /api/events/{id}
PATCH  /api/events/{id}/cancel
DELETE /api/events/{id}

POST   /api/events/{id}/join
DELETE /api/events/{id}/leave
GET    /api/events/{id}/participants

GET    /api/users/me
PUT    /api/users/me

GET    /api/admin/users
PATCH  /api/admin/users/{id}/toggle
DELETE /api/admin/users/{id}
DELETE /api/admin/events/{id}
```

