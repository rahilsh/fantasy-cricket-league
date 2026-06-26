# Fantasy Cricket League

Fantasy Cricket League is a Spring Boot REST API for creating games, user teams,
recording ball events, and viewing the top-K fantasy leaderboard.

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- H2 file database for local persistent storage
- JUnit 6 via Spring Boot test support
- JaCoCo coverage gate at 80%

## Local Database

The default local database is a persistent H2 file database:

```text
jdbc:h2:file:./data/fcl;MODE=PostgreSQL;AUTO_SERVER=TRUE
```

Hibernate is configured with `ddl-auto=update`, so the local schema is created
and evolved automatically while keeping existing data on restart.
`AUTO_SERVER=TRUE` lets the file database accept additional connections (for
example the H2 console) and makes restarts resilient if a previous instance is
still shutting down.

The H2 console is available at:

```text
http://localhost:8080/h2-console
```

Use these default credentials:

```text
JDBC URL: jdbc:h2:file:./data/fcl;MODE=PostgreSQL;AUTO_SERVER=TRUE
User: sa
Password:
```

## Switch Database By Configuration

Set these environment variables in non-local environments:

```bash
export FCL_DB_URL='jdbc:postgresql://localhost:5432/fcl'
export FCL_DB_USERNAME='fcl_user'
export FCL_DB_PASSWORD='secret'
export FCL_DB_DRIVER='org.postgresql.Driver'
```

Add the target database JDBC driver dependency to `pom.xml` when switching away from H2.

## Run Locally

```bash
mvn spring-boot:run
```

Or build and run the jar:

```bash
mvn verify
java -jar target/fcl-1.0-SNAPSHOT.jar
```

The REST API is available at `http://localhost:8080/api`. A static OpenAPI
document is served at `http://localhost:8080/openapi.yaml`.

## Simulate A Game

`scripts/simulate_game.sh` drives a full game through the REST API: it logs in as
superadmin, creates a game with a fixed number of overs, signs up users with
teams, starts the game, and records one random ball event per delivery. The game
**auto-completes once all overs are bowled** (6 balls per over), after which the
script prints the winner with their players.

```bash
# Start the app first (mvn spring-boot:run), then:
./scripts/simulate_game.sh
```

Configure via environment variables: `BASE_URL` (default
`http://localhost:8080`), `NUM_USERS` (default `4`), `OVERS` (default `5`, i.e.
30 balls), and the superadmin credentials `FCL_SECURITY_SUPERADMIN_USERNAME` /
`FCL_SECURITY_SUPERADMIN_PASSWORD`. Requires `curl` and `python3`.

## Test And Coverage

```bash
mvn verify
```

This runs unit/functional tests and enforces the 80% JaCoCo coverage rule.

## REST APIs

Base URL:

```text
http://localhost:8080/api
```

Authentication is JWT Bearer token based.

Auth endpoints:

```text
POST /api/auth/signup
POST /api/auth/login
```

Superadmin credentials for login:

```text
Username: fcl-admin
Password: fcl-admin-password
```

Override with environment variables `FCL_SECURITY_SUPERADMIN_USERNAME` and
`FCL_SECURITY_SUPERADMIN_PASSWORD`.

JWT signing secret is configured by `FCL_SECURITY_JWT_SECRET`.

There are two roles: `USER` and `SUPERADMIN`. Normal users self-sign up through
`POST /api/auth/signup`. The superadmin logs in with the static credentials
above. Superadmins create/start/end games and record ball events; users manage
their own team and read games and leaderboards.

Observability endpoints (auth required):

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
```

List endpoints support pagination and sorting via `page`, `size`, and `sort`
query parameters (for example: `?page=0&size=20&sort=id,desc`).

A static OpenAPI document is available at:

```text
http://localhost:8080/openapi.yaml
```

### Games

- `POST /games`
- `GET /games`
- `GET /games/{id}`
- `PUT /games/{id}`
- `DELETE /games/{id}`
- `POST /games/{id}/start`
- `POST /games/{id}/end`
- `POST /games/{id}/plays`
- `GET /games/{id}/leaderboard`

Create a game:

```bash
curl -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer <admin_token>" \
  -H 'Content-Type: application/json' \
  -d '{"team1":"IND","team2":"PAK","k":5,"overs":20}'
```

`overs` is required and sets the match length: the game **automatically
completes** once `overs * 6` ball events have been recorded, so an explicit
`POST /games/{id}/end` is only needed to stop a game early.

The `Authorization` header above must carry a superadmin token; game writes
(`POST`/`PUT`/`DELETE`, `start`, `end`, `plays`) are superadmin-only, while any
authenticated user can read games and leaderboards (`GET /games`,
`GET /games/{id}/leaderboard`).

### Users

- `POST /users`
- `GET /users`
- `GET /users/{id}`
- `PUT /users/{id}`
- `DELETE /users/{id}`

User-management endpoints are superadmin-only. The create user endpoint accepts
`userName` and `password`; all created users have the `USER` role.

### User Teams

- `POST /user-teams`
- `GET /user-teams`
- `GET /user-teams?gameId={gameId}`
- `GET /user-teams/{id}`
- `PUT /user-teams/{id}`
- `DELETE /user-teams/{id}`

Create a user team:

```bash
curl -X POST http://localhost:8080/api/user-teams \
  -H "Authorization: Bearer <user_token>" \
  -H 'Content-Type: application/json' \
  -d '{"gameId":1,"userName":"user1","players":[1,12,2,13,3,14,7,18,8,19,10]}'
```

`userName` must reference an existing user from `POST /users`.
Users can only access and modify their own teams.
User team players are limited to max 11.
User teams cannot be modified once the game has started.

Use `POST /games/{id}/plays` for game simulation because it records the ball event and
updates fantasy points.

## End-To-End Game Flow

```bash
curl -X POST http://localhost:8080/api/games/1/start

curl -X POST http://localhost:8080/api/games/1/plays \
  -H 'Content-Type: application/json' \
  -d '{"batsman":1,"bowler":22,"outcome":6}'

curl http://localhost:8080/api/games/1/leaderboard

curl -X POST http://localhost:8080/api/games/1/end
```

Supported ball event outcomes:

- `1`: batsman teams +0.5
- `2`: batsman teams +1.0, bowler teams -0.5
- `4`: batsman teams +2.0, bowler teams -1.0
- `6`: batsman teams +3.0, bowler teams -2.0
- `-1`: batsman teams -2.0, bowler teams +4.0

## Functional Tests

[GameFunctionalTest](src/test/java/com/rsh/fcl/GameFunctionalTest.java) drives the REST
API end to end with `MockMvc`, including a full game simulation, CRUD coverage, and
error cases.
