#!/usr/bin/env bash
#
# simulate_game.sh — drive a full Fantasy Cricket League game through the REST API.
#
# It logs in as superadmin, creates a game, signs up a set of users (each with a
# random XI of up to 11 players), starts the game, records random ball events,
# and prints the winner with their players. The game auto-ends once all overs are
# bowled or 10 wickets fall, whichever comes first.
#
# Requirements: curl, python3, and a running app (e.g. `mvn spring-boot:run`).
#
# Configuration (environment variables):
#   BASE_URL                          host of the running app   (default http://localhost:8080)
#   NUM_USERS                         number of fantasy users   (default 4)
#   OVERS                             overs in the game         (default 5, i.e. 30 balls)
#   FCL_SECURITY_SUPERADMIN_USERNAME  superadmin username       (default fcl-admin)
#   FCL_SECURITY_SUPERADMIN_PASSWORD  superadmin password       (default fcl-admin-password)
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
NUM_USERS="${NUM_USERS:-4}"
OVERS="${OVERS:-5}"
NUM_EVENTS=$((OVERS * 6))
SUPERADMIN_USER="${FCL_SECURITY_SUPERADMIN_USERNAME:-fcl-admin}"
SUPERADMIN_PASS="${FCL_SECURITY_SUPERADMIN_PASSWORD:-fcl-admin-password}"

command -v curl >/dev/null 2>&1 || { echo "error: curl is required" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "error: python3 is required" >&2; exit 1; }

API="$BASE_URL/api"

# Read a value out of a JSON document on stdin, e.g. jval "['accessToken']".
jval() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }

echo "==> Logging in as superadmin ($SUPERADMIN_USER)"
SA_TOKEN=$(curl -fsS -X POST "$API/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"userName\":\"$SUPERADMIN_USER\",\"password\":\"$SUPERADMIN_PASS\"}" \
  | jval "['accessToken']")

echo "==> Creating game ($OVERS overs = $NUM_EVENTS balls)"
GID=$(curl -fsS -X POST "$API/games" \
  -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"team1\":\"Team Alpha\",\"team2\":\"Team Beta\",\"k\":$NUM_USERS,\"overs\":$OVERS}" \
  | jval "['id']")
echo "    game id = $GID"

SUFFIX=$(date +%s)

echo "==> Signing up $NUM_USERS users and creating teams (max 11 players each)"
for i in $(seq 1 "$NUM_USERS"); do
  uname="player${i}_${SUFFIX}"
  utoken=$(curl -fsS -X POST "$API/auth/signup" \
    -H 'Content-Type: application/json' \
    -d "{\"userName\":\"$uname\",\"password\":\"password123\"}" \
    | jval "['accessToken']")
  players=$(python3 -c "import random;print(','.join(map(str,sorted(random.sample(range(1,23),11)))))")
  curl -fsS -X POST "$API/user-teams" \
    -H "Authorization: Bearer $utoken" -H 'Content-Type: application/json' \
    -d "{\"gameId\":$GID,\"userName\":\"$uname\",\"players\":[$players]}" >/dev/null
  printf '    %-18s -> [%s]\n' "$uname" "$players"
done

echo "==> Starting game"
curl -fsS -X POST "$API/games/$GID/start" -H "Authorization: Bearer $SA_TOKEN" >/dev/null

echo "==> Recording up to $NUM_EVENTS ball events ($OVERS overs); game auto-ends after all overs or 10 wickets"
OUTCOMES=(1 2 4 6 -1)
WICKETS=0
for e in $(seq 1 "$NUM_EVENTS"); do
  batsman=$(( (RANDOM % 22) + 1 ))
  bowler=$(( (RANDOM % 22) + 1 ))
  outcome=${OUTCOMES[$((RANDOM % ${#OUTCOMES[@]}))]}
  http=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$API/games/$GID/plays" \
    -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
    -d "{\"batsman\":$batsman,\"bowler\":$bowler,\"outcome\":$outcome}")
  if [ "$http" != "200" ]; then
    echo "    game already ended (HTTP $http); stopping after $((e - 1)) balls"
    break
  fi
  [ "$outcome" = "-1" ] && WICKETS=$((WICKETS + 1))
  printf '    ball %2d: batsman=%2d bowler=%2d outcome=%3s  (wickets=%d)\n' \
    "$e" "$batsman" "$bowler" "$outcome" "$WICKETS"
done

STATUS=$(curl -fsS "$API/games/$GID" -H "Authorization: Bearer $SA_TOKEN" | jval "['status']")
echo "==> Game status: $STATUS"

echo "==> Final leaderboard"
LB=$(curl -fsS "$API/games/$GID/leaderboard" -H "Authorization: Bearer $SA_TOKEN")
echo "$LB" | python3 -c "
import sys, json
rows = json.load(sys.stdin)
if not rows:
    print('    (no entries)')
for i, e in enumerate(rows, 1):
    print(f'    {i}. {e[\"userName\"]:<18} {e[\"points\"]:>7} pts')
"

WINNER=$(echo "$LB" | jval "[0]['userName']")
WPOINTS=$(echo "$LB" | jval "[0]['points']")

TEAMS=$(curl -fsS "$API/user-teams?gameId=$GID&size=1000" -H "Authorization: Bearer $SA_TOKEN")
WPLAYERS=$(echo "$TEAMS" | WINNER="$WINNER" python3 -c "
import sys, os, json
winner = os.environ['WINNER']
for t in json.load(sys.stdin)['content']:
    if t['userName'] == winner:
        print(','.join(map(str, sorted(t['players']))))
        break
")

echo
echo "================================================"
echo "  WINNER: $WINNER  ($WPOINTS points)"
echo "  Players: [$WPLAYERS]"
echo "================================================"
