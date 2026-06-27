#!/usr/bin/env python3
"""Drive a full Fantasy Cricket League game through the REST API.

It logs in as superadmin, creates a game, signs up a set of users (each with a
random XI of up to 11 players), starts the game, records random ball events, and
prints the winner with their players. The game auto-ends once all overs are
bowled or 10 wickets fall, whichever comes first.

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
import sys
import time
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
PLAYER_POOL = range(1, 23)  # player ids 1..22


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


def create_game(sa_token):
    print(f"==> Creating game ({OVERS} overs = {NUM_EVENTS} balls)")
    _, game = request(
        "POST",
        "/games",
        token=sa_token,
        body={"team1": "Team Alpha", "team2": "Team Beta", "k": NUM_USERS, "overs": OVERS},
    )
    print(f"    game id = {game['id']}")
    return game["id"]


def register_users_with_teams(game_id):
    suffix = int(time.time())
    print(f"==> Signing up {NUM_USERS} users and creating teams (max 11 players each)")
    for i in range(1, NUM_USERS + 1):
        username = f"player{i}_{suffix}"
        user_token = signup(username, "password123")
        players = sorted(random.sample(PLAYER_POOL, 11))
        request(
            "POST",
            "/user-teams",
            token=user_token,
            body={"gameId": game_id, "userName": username, "players": players},
        )
        print(f"    {username:<18} -> {players}")


def play_ball_events(game_id, sa_token):
    print("==> Starting game")
    request("POST", f"/games/{game_id}/start", token=sa_token)

    print(
        f"==> Recording up to {NUM_EVENTS} ball events ({OVERS} overs); "
        "game auto-ends after all overs or 10 wickets"
    )
    wickets = 0
    for ball in range(1, NUM_EVENTS + 1):
        batsman = random.randint(1, 22)
        bowler = random.randint(1, 22)
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
            f"    ball {ball:2d}: batsman={batsman:2d} bowler={bowler:2d} "
            f"outcome={outcome:3d}  (wickets={wickets})"
        )

    _, game = request("GET", f"/games/{game_id}", token=sa_token)
    print(f"==> Game status: {game['status']}")


def fetch_winning_players(game_id, sa_token, winner_name):
    _, teams = request("GET", f"/user-teams?gameId={game_id}&size=1000", token=sa_token)
    return next(
        (sorted(t["players"]) for t in teams["content"] if t["userName"] == winner_name),
        [],
    )


def print_results(game_id, sa_token):
    print("==> Final leaderboard")
    _, leaderboard = request("GET", f"/games/{game_id}/leaderboard", token=sa_token)
    if not leaderboard:
        print("    (no entries)")
        return
    for rank, entry in enumerate(leaderboard, 1):
        print(f"    {rank}. {entry['userName']:<18} {entry['points']:>7} pts")

    winner = leaderboard[0]
    winning_players = fetch_winning_players(game_id, sa_token, winner["userName"])

    print()
    print("================================================")
    print(f"  WINNER: {winner['userName']}  ({winner['points']} points)")
    print(f"  Players: {winning_players}")
    print("================================================")


def main():
    print(f"==> Logging in as superadmin ({SUPERADMIN_USER})")
    sa_token = login(SUPERADMIN_USER, SUPERADMIN_PASS)

    game_id = create_game(sa_token)
    register_users_with_teams(game_id)
    play_ball_events(game_id, sa_token)
    print_results(game_id, sa_token)


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as err:
        print(f"error: could not reach {BASE_URL} ({err.reason})", file=sys.stderr)
        sys.exit(1)
