# Fantasy Cricket League

Fantasy Cricket League is a Spring Boot app for creating games, user teams,
recording ball events, and viewing the top-K fantasy leaderboard. It includes a
modern static UI at `/`.

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
jdbc:h2:file:./data/fcl;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

The schema is created by Liquibase changelogs from
[db/changelog](src/main/resources/db/changelog). Hibernate is configured with
`ddl-auto=validate`, so Java mappings are checked against the migrated SQL DDL.

The H2 console is available at:

```text
http://localhost:8080/h2-console
```

Use these default credentials:

```text
JDBC URL: jdbc:h2:file:./data/fcl;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
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

Only the superadmin can create admin users through `POST /api/users` with
`role=ADMIN`. Normal users sign up through `POST /api/auth/signup`.

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
  -d '{"team1":"IND","team2":"PAK","k":5}'
```

### Users

- `POST /users`
- `GET /users`
- `GET /users/{id}`
- `PUT /users/{id}`
- `DELETE /users/{id}`

The create user endpoint accepts `userName`, `password`, and `role`.
`role=ADMIN` is reserved for the superadmin.

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
