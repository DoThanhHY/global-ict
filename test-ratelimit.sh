#!/bin/bash
# Test rate limiting tiers
# Usage: bash test-ratelimit.sh

BASE="http://localhost:8080/api"
USER="admin"
PASS="admin"

echo "=== 1. Auth tier (5 req/min per IP) ==="
AUTH_ENDPOINTS=("login" "register")
for ep in "${AUTH_ENDPOINTS[@]}"; do
  echo "--- POST /api/auth/$ep ---"
  for i in $(seq 1 6); do
    code=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "$BASE/auth/$ep" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
    echo "  Request $i → $code"
  done
done

echo ""
echo "=== 2. Get JWT token for authenticated tests ==="
TOKEN=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ Login failed — check credentials or backend"
  exit 1
fi
echo "Token obtained ✓"

echo ""
echo "=== 3. Read tier (100 req/min per user) ==="
echo "--- GET /api/devices x101 ---"
for i in $(seq 1 101); do
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE/devices" \
    -H "Authorization: Bearer $TOKEN")
  if [ "$code" = "429" ]; then
    echo "  Hit rate limit at request $i → $code"
    break
  elif [ "$i" -eq 101 ]; then
    echo "  All 101 requests passed (no limit hit)"
  fi
done

echo ""
echo "=== 4. Command tier (10 req/min per user) ==="
echo "--- POST /api/devices/{id}/command x11 ---"
for i in $(seq 1 11); do
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE/devices/esp32-001/command" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"action":"toggle"}')
  if [ "$code" = "429" ]; then
    echo "  Hit rate limit at request $i → $code"
    break
  elif [ "$i" -eq 11 ]; then
    echo "  All 11 requests passed (no limit hit)"
  fi
done

echo ""
echo "=== 5. Write tier (30 req/min per user) ==="
echo "--- POST /api/devices x31 ---"
for i in $(seq 1 31); do
  code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE/devices" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"deviceId\":\"test-$i\",\"name\":\"Test $i\",\"type\":\"TEMPERATURE_HUMIDITY\"}")
  if [ "$code" = "429" ]; then
    echo "  Hit rate limit at request $i → $code"
    break
  elif [ "$i" -eq 31 ]; then
    echo "  All 31 requests passed (no limit hit)"
  fi
done

echo ""
echo "=== 6. Verify 429 response body ==="
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" \
  -w "\nHTTP status: %{http_code}\n"

echo ""
echo "=== Done ==="
