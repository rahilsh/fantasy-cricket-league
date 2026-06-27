#!/usr/bin/env python3
"""Drive a full Fantasy Cricket League game through the REST API.

The script logs in as superadmin and walks the whole onboarding flow:

  1. Create 22 cricketers (standalone reference data). Each cricketer is created
     with a client-supplied globally unique id in the ``<3 letters>_<3 letters>``
     format (e.g. ``abc_xyz``) plus a name and a type.
  2. Create a tournament and onboard two teams into it, each made of 11 of those
     cricketers (referenced by id). Then start the tournament.
  3. Create a game between the two teams by passing their team ids.
  4. Sign up a set of fantasy users (``player1``, ``player2`` ...), each picking a
     valid random XI from the 22 cricketers in the game.
  5. Start the game, record random ball events, and print the winner.

The game auto-ends once all overs are bowled or 10 wickets fall, whichever comes
first.

Requirements: Python 3 (standard library only) and a running app
(e.g. ``mvn spring-boot:run``).

Configuration (environment variables):
    BASE_URL                          host of the running app  (default http://localhost:8080)
    NUM_USERS                         number of fantasy users  (default 4)
    OVERS                             overs in the game        (default 5, i.e. 30 balls)
    FCL_SECURITY_SUPERADMIN_USERNAME  superadmin username      (default fcl-admin)
    FCL_SECURITY_SUPERADMIN_PASSWORD  superadmin password      (default fcl-admin-password)
"""

import json
import os
import random
import string
import sys
import urllib.error
import urllib.request

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
NUM_USERS = int(os.environ.get("NUM_USERS", "4"))
OVERS = int(os.environ.get("OVERS", "5"))
SUPERADMIN_USER = os.environ.get("FCL_SECURITY_SUPERADMIN_USERNAME", "fcl-admin")
SUPERADMIN_PASS = os.environ.get("FCL_SECURITY_SUPERADMIN_PASSWORD", "fcl-admin-password")

API = f"{BASE_URL}/api"
NUM_EVENTS = OVERS * 6
OUTCOMES = [1, 2, 4, 6, -1]

TEAM_SIZE = 11

# Local offset within a team -> cricketer type. This guarantees every team has one
# wicketkeeper and at least five bowlers/all-rounders, matching the squad rules
# enforced when teams are onboarded, when a game is created, and when users pick
# their XI.
TYPE_BY_OFFSET = (
    "WICKETKEEPER",  # 0
    "BOWLER", "BOWLER", "BOWLER", "BOWLER",  # 1-4
    "ALLROUNDER", "ALLROUNDER", "ALLROUNDER",  # 5-7
    "BATTER", "BATTER", "BATTER",  # 8-10
)

_used_ids = set()


def unique_cricketer_id():
    """Return a unique ``<3 letters>_<3 letters>`` cricketer id."""
    while True:
        candidate = (
            "".join(random.choices(string.ascii_lowercase, k=3))
            + "_"
            + "".join(random.choices(string.ascii_lowercase, k=3))
        )
        if candidate not in _used_ids:
            _used_ids.add(candidate)
            return candidate


def request(method, path, token=None, body=None):
    """Make a JSON request and return (status_code, parsed_body)."""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{API}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            payload = resp.read().decode()
            return resp.status, (json.loads(payload) if payload else None)
    except urllib.error.HTTPError as err:
        payload = err.read().decode()
        return err.code, (json.loads(payload) if payload else None)


def login(username, password):
    _, body = request("POST", "/auth/login", body={"userName": username, "password": password})
    return body["accessToken"]


def signup(username, password):
    _, body = request("POST", "/auth/signup", body={"userName": username, "password": password})
    return body["accessToken"]


def create_cricketer(sa_token, team_name, offset):
    """Create one cricketer with a client-supplied id; return its id."""
    cricketer_id = unique_cricketer_id()
    status, body = request(
        "POST",
        f"/cricketers?globalUniqueId={cricketer_id}",
        token=sa_token,
        body={"name": f"{team_name} P{offset + 1}", "type": TYPE_BY_OFFSET[offset]},
    )
    if status not in (200, 201):
        raise SystemExit(f"error: cricketer creation failed (HTTP {status}): {body}")
    return cricketer_id


def onboard_team(sa_token, tournament_id, team_name):
    """Create 11 cricketers and onboard them as a team; return the team id."""
    cricketer_ids = [create_cricketer(sa_token, team_name, offset) for offset in range(TEAM_SIZE)]
    status, body = request(
        "POST",
        f"/tournaments/{tournament_id}/teams",
        token=sa_token,
        body={"name": team_name, "cricketers": cricketer_ids},
    )
    if status not in (200, 201) or not body or "id" not in body:
        raise SystemExit(f"error: team onboarding failed (HTTP {status}): {body}")
    print(f"    onboarded {team_name:<10} (team id = {body['id']}, cricketers = {cricketer_ids})")
    return body["id"]


def create_game(sa_token):
    print(f"==> Creating tournament and onboarding two teams")
    status, tournament = request(
        "POST", "/tournaments", token=sa_token, body={"name": "Simulation Cup"}
    )
    if status not in (200, 201) or not tournament or "id" not in tournament:
        raise SystemExit(f"error: tournament creation failed (HTTP {status}): {tournament}")
    tournament_id = tournament["id"]

    team1_id = onboard_team(sa_token, tournament_id, "Team Alpha")
    team2_id = onboard_team(sa_token, tournament_id, "Team Beta")

    request("POST", f"/tournaments/{tournament_id}/start", token=sa_token)

    print(f"==> Creating game ({OVERS} overs = {NUM_EVENTS} balls)")
    status, game = request(
        "POST",
        "/games",
        token=sa_token,
        body={
            "tournamentId": tournament_id,
            "team1Id": team1_id,
            "team2Id": team2_id,
            "k": NUM_USERS,
            "overs": OVERS,
        },
    )
    if status not in (200, 201) or not game or "id" not in game:
        raise SystemExit(f"error: game creation failed (HTTP {status}): {game}")
    roster = game["team1Cricketers"] + game["team2Cricketers"]
    print(f"    game id = {game['id']} ({len(roster)} cricketers in roster)")
    print(f"    roster ids = {[c['globalUniqueId'] for c in roster]}")
    return game["id"], roster


def pick_valid_xi(roster):
    """Pick 11 cricketers obeying: >=1 wicketkeeper, >=5 bowler/all-rounder."""
    by_type = {}
    for c in roster:
        by_type.setdefault(c["type"], []).append(c["globalUniqueId"])

    chosen = set()
    chosen.add(random.choice(by_type["WICKETKEEPER"]))
    pace = by_type.get("BOWLER", []) + by_type.get("ALLROUNDER", [])
    chosen.update(random.sample(pace, 5))

    remaining = [c["globalUniqueId"] for c in roster if c["globalUniqueId"] not in chosen]
    chosen.update(random.sample(remaining, TEAM_SIZE - len(chosen)))
    return sorted(chosen)


def register_users_with_teams(game_id, roster):
    print(f"==> Signing up {NUM_USERS} users and creating teams (11 cricketers each)")
    for i in range(1, NUM_USERS + 1):
        username = f"player{i}"
        user_token = signup(username, "password123")
        cricketers = pick_valid_xi(roster)
        request(
            "POST",
            "/user-teams",
            token=user_token,
            body={"gameId": game_id, "userName": username, "cricketers": cricketers},
        )
        print(f"    {username:<10} -> {cricketers}")


def play_ball_events(game_id, sa_token, roster):
    print("==> Starting game")
    request("POST", f"/games/{game_id}/start", token=sa_token)

    cricketer_ids = [c["globalUniqueId"] for c in roster]
    print(
        f"==> Recording up to {NUM_EVENTS} ball events ({OVERS} overs); "
        "game auto-ends after all overs or 10 wickets"
    )
    wickets = 0
    for ball in range(1, NUM_EVENTS + 1):
        batsman = random.choice(cricketer_ids)
        bowler = random.choice(cricketer_ids)
        outcome = random.choice(OUTCOMES)
        status, _ = request(
            "POST",
            f"/games/{game_id}/plays",
            token=sa_token,
            body={"batsman": batsman, "bowler": bowler, "outcome": outcome},
        )
        if status != 200:
            print(f"    game already ended (HTTP {status}); stopping after {ball - 1} balls")
            break
        if outcome == -1:
            wickets += 1
        print(
            f"    ball {ball:2d}: batsman={batsman:<8} bowler={bowler:<8} "
            f"outcome={outcome:3d}  (wickets={wickets})"
        )

    _, game = request("GET", f"/games/{game_id}", token=sa_token)
    print(f"==> Game status: {game['status']}")


def fetch_winning_cricketers(game_id, sa_token, winner_name):
    _, teams = request("GET", f"/user-teams?gameId={game_id}&size=1000", token=sa_token)
    return next(
        (sorted(t["cricketers"]) for t in teams["content"] if t["userName"] == winner_name),
        [],
    )


def print_results(game_id, sa_token):
    print("==> Final leaderboard")
    _, leaderboard = request("GET", f"/games/{game_id}/leaderboard", token=sa_token)
    if not leaderboard:
        print("    (no entries)")
        return
    for rank, entry in enumerate(leaderboard, 1):
        print(f"    {rank}. {entry['userName']:<10} {entry['points']:>7} pts")

    winner = leaderboard[0]
    winning_cricketers = fetch_winning_cricketers(game_id, sa_token, winner["userName"])

    print()
    print("================================================")
    print(f"  WINNER: {winner['userName']}  ({winner['points']} points)")
    print(f"  Cricketers: {winning_cricketers}")
    print("================================================")


def main():
    print(f"==> Logging in as superadmin ({SUPERADMIN_USER})")
    sa_token = login(SUPERADMIN_USER, SUPERADMIN_PASS)

    game_id, roster = create_game(sa_token)
    register_users_with_teams(game_id, roster)
    play_ball_events(game_id, sa_token, roster)
    print_results(game_id, sa_token)


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as err:
        print(f"error: could not reach {BASE_URL} ({err.reason})", file=sys.stderr)
        sys.exit(1)
