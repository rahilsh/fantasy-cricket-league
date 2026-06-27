# Fantasy Cricket League

Fantasy Cricket League is a Spring Boot REST API for creating games, user teams,
recording ball events, and viewing the top-K fantasy leaderboard.

## Game Rules

- **Cricketers** are standalone, predefined reference entities. Each cricketer is
  created with a client-supplied **globally unique id** in the
  `<3 letters>_<3 letters>` format (for example `abc_xyz`), a `name`, and a
  `type` of `BATTER`, `BOWLER`, `ALLROUNDER`, or `WICKETKEEPER`. Cricketers are
  not tied to any tournament, team, or game in their own table; the link is
  established when they are onboarded into a team.
- **A tournament** is the parent entity with its own lifecycle status
  (`CREATED` → `IN_PROGRESS` → `COMPLETED`). Teams and games belong to a
  tournament; cricketers do not.
- **A team** belongs to exactly one tournament and has exactly **11 cricketers**.
  Every team squad must satisfy the composition rule:
  - at least **one WICKETKEEPER**;
  - at least **five** cricketers that are `BOWLER` or `ALLROUNDER` combined.
  A cricketer can be part of only **one active (non-completed) tournament** at a
  time. While a tournament is `IN_PROGRESS`, a team's cricketers can be
  added/replaced/removed, but **not while one of the team's matches is in
  progress**.
- **A game** belongs to a tournament and is played between **two teams** of that
  tournament, referenced by their **team ids**. A game therefore has a 22-cricketer
  roster (11 per team) and references cricketers only through its two teams. A team
  can play at most one game at a time.
- **A user team** is a fantasy XI picked from a game's 22-cricketer roster and
  must contain exactly **11 cricketers**, selected subject to these constraints:
  - at least **one WICKETKEEPER**;
  - at least **five** cricketers that are `BOWLER` or `ALLROUNDER` combined.
- **Ball events** record a batsman, a bowler (by cricketer id), and an outcome.
  Run outcomes (`1`, `2`, `4`, `6`) and wickets (`-1`) score fantasy points for
  the user teams that own the involved cricketers.
- **A game auto-ends** once all overs are bowled or 10 wickets fall, whichever
  comes first, after which the top-K **leaderboard** is finalised.
- **Access control**: Tournament, Cricketer (writes), Team, and Game APIs are
  **superadmin-only**. Authenticated users can read cricketers, read games and
  leaderboards, and manage their own user teams.

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- H2 file database for local persistent storage
- JUnit 6 via Spring Boot test support
- JaCoCo coverage gate at 85%

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
java -jar target/fantasy-cricket-league-1.0-SNAPSHOT.jar
```

The REST API is available at `http://localhost:8080/api`. A static OpenAPI
document is served at `http://localhost:8080/openapi.yaml`.

## Simulate A Game

`scripts/simulate_game.py` drives a full game through the REST API: it logs in as
superadmin, creates 22 cricketers, creates a tournament, onboards two teams of 11
cricketers each, starts the tournament, creates a game between the two teams,
signs up users (`player1`, `player2`, ...) with valid XIs, starts the game, and
records one random ball event per delivery. The game **auto-completes once all
overs are bowled (6 balls per over) or 10 wickets fall**, whichever comes first,
after which the script prints the winner with their cricketers.

```bash
# Start the app first (mvn spring-boot:run), then:
./scripts/simulate_game.py
```

Configure via environment variables: `BASE_URL` (default
`http://localhost:8080`), `NUM_USERS` (default `4`), `OVERS` (default `5`, i.e.
30 balls), and the superadmin credentials `FCL_SECURITY_SUPERADMIN_USERNAME` /
`FCL_SECURITY_SUPERADMIN_PASSWORD`. Requires `python3` (standard library only).

## Test And Coverage

```bash
mvn verify
```

This runs unit/functional tests and enforces the 85% JaCoCo coverage rule.

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
above. Superadmins manage tournaments, cricketers, teams and games (create teams,
onboard squads, create/start/end games, record ball events); users read
cricketers, games and leaderboards and manage their own user team.

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

### Cricketers

- `POST /cricketers?globalUniqueId=<abc_xyz>`
- `GET /cricketers`
- `GET /cricketers/{globalUniqueId}`
- `PUT /cricketers/{globalUniqueId}`
- `DELETE /cricketers/{globalUniqueId}`

Create a cricketer (the id is supplied as a query parameter in the
`<3 letters>_<3 letters>` format):

```bash
curl -X POST 'http://localhost:8080/api/cricketers?globalUniqueId=abc_xyz' \
  -H "Authorization: Bearer <admin_token>" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Virat","type":"BATTER"}'
```

Cricketer writes are superadmin-only; any authenticated user can read cricketers.

### Tournaments

- `POST /tournaments`
- `GET /tournaments`
- `GET /tournaments/{id}`
- `PUT /tournaments/{id}`
- `DELETE /tournaments/{id}`
- `POST /tournaments/{id}/start`
- `POST /tournaments/{id}/end`
- `POST /tournaments/{id}/teams`  (onboard a team)

Create a tournament and onboard a team of 11 cricketers (by id):

```bash
curl -X POST http://localhost:8080/api/tournaments \
  -H "Authorization: Bearer <admin_token>" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Champions Trophy"}'

curl -X POST http://localhost:8080/api/tournaments/1/teams \
  -H "Authorization: Bearer <admin_token>" \
  -H 'Content-Type: application/json' \
  -d '{
        "name": "IND",
        "cricketers": ["abc_wki","abc_bo1","abc_bo2","abc_bo3","abc_bo4",
                        "abc_ar1","abc_ar2","abc_ar3","abc_ba1","abc_ba2","abc_ba3"]
      }'

curl -X POST http://localhost:8080/api/tournaments/1/start \
  -H "Authorization: Bearer <admin_token>"
```

Each onboarded team must have exactly **11 cricketers** satisfying the squad
composition rule (≥1 WICKETKEEPER, ≥5 BOWLER+ALLROUNDER). A cricketer can only be
part of one active (non-completed) tournament. Tournament and team APIs are
superadmin-only.

### Teams

- `GET /teams`
- `GET /teams/{id}`
- `POST /teams/{id}/cricketers`                  (add a cricketer)
- `PUT /teams/{id}/cricketers/{cricketerId}`     (replace a cricketer)
- `DELETE /teams/{id}/cricketers/{cricketerId}`  (remove a cricketer)

Teams are created via `POST /tournaments/{id}/teams`. While the tournament is
`IN_PROGRESS`, a team's cricketers can be added/replaced/removed — but **not while
one of the team's matches is in progress**.

```bash
curl -X PUT http://localhost:8080/api/teams/1/cricketers/abc_ba3 \
  -H "Authorization: Bearer <admin_token>" \
  -H 'Content-Type: application/json' \
  -d '{"cricketerId":"abc_ba9"}'
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

Create a game between two teams of a tournament, referenced by their **team ids**:

```bash
curl -X POST http://localhost:8080/api/games \
  -H "Authorization: Bearer <admin_token>" \
  -H 'Content-Type: application/json' \
  -d '{
        "tournamentId": 1,
        "team1Id": 1,
        "team2Id": 2,
        "k": 5,
        "overs": 20
      }'
```

A game references two distinct teams of the same tournament, giving a 22-cricketer
roster (11 per team). The create-game response includes `team1Cricketers` and
`team2Cricketers` arrays; read each cricketer's `globalUniqueId` to build
user-team selections. Both squads must satisfy the composition rule (≥1
WICKETKEEPER and ≥5 BOWLER+ALLROUNDER), and a team can play only one game at a
time.

`overs` is required and sets the match length: the game **automatically
completes** once `overs * 6` ball events have been recorded, or earlier once
**10 wickets** (ball events with outcome `-1`) have fallen — whichever comes
first. An explicit `POST /games/{id}/end` is only needed to stop a game even
earlier.

The `Authorization` header above must carry a superadmin token; all game APIs
are superadmin-only, while any authenticated user can read games and leaderboards
(`GET /games`, `GET /games/{id}`, `GET /games/{id}/leaderboard`).

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
  -d '{"gameId":1,"userName":"user1","cricketers":["abc_wki","abc_bo1","abc_bo2","abc_bo3","abc_bo4","abc_ar1","abc_ar2","abc_ba1","abc_ba2","abc_ba3","def_wki"]}'
```

`userName` must reference an existing user from `POST /users`.
Users can only access and modify their own teams.
A user team must contain exactly **11 cricketers** drawn from the game's
22-cricketer roster (by their `globalUniqueId`), including at least **one
WICKETKEEPER** and at least **five** cricketers that are `BOWLER` or `ALLROUNDER`
combined.
User teams cannot be modified once the game has started.

Use `POST /games/{id}/plays` for game simulation because it records the ball event and
updates fantasy points.

## End-To-End Game Flow

```bash
curl -X POST http://localhost:8080/api/games/1/start

curl -X POST http://localhost:8080/api/games/1/plays \
  -H 'Content-Type: application/json' \
  -d '{"batsman":"abc_ba1","bowler":"def_bo1","outcome":6}'

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
