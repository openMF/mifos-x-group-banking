#!/usr/bin/env bash
# Rule-driven, role-wise companion API health runner.
#
# Reads api-registry.json (the role-wise API design SoT), logs in as each demo persona, probes every
# endpoint that persona's role is supposed to reach, and records status + latency vs the endpoint's
# SLO. Emits machine-readable health data (health-data/health-<ts>.json + latest.json) AND a
# human matrix, so you can SEE where to optimize.
#
# SAFETY: writes (POST/PUT/DELETE) are NEVER executed — they are wiring-checked only (GET the path,
# assert the route is registered i.e. non-404). Reads are side-effect-free.
#
# Usage:
#   bash api-health.sh                         # run full health sweep against the default host
#   COMPANION_BASE_URL=http://localhost:8080 bash api-health.sh
#   bash api-health.sh --design                # print the role-wise design table, no network
#
# Backend note: mifos-bank-2 (Fineract) is chronically flaky + onrender free-tier cold-starts. The
# runner warms the host and retries login. A 502 storm = BACKEND down (GET /companion/build -> 200
# means the companion itself is up).

set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
REG="$HERE/api-registry.json"
OUT_DIR="$HERE/health-data"
CB="${COMPANION_BASE_URL:-https://mifossave-companion.onrender.com}"
CB="${CB%/}"; CB="${CB%/companion}"

command -v jq >/dev/null || { echo "jq required"; exit 2; }
[ -f "$REG" ] || { echo "registry not found: $REG"; exit 2; }

# ---- --design: print the role-wise design table and exit (no network) ----
if [ "${1:-}" = "--design" ]; then
  echo "# MifosSave API — role-wise design ($(jq -r '.endpoints|length' "$REG") endpoints)"
  printf "%-22s %-7s %-46s %-20s %-14s %-8s %-6s\n" "ID" "METHOD" "PATH" "SCREEN" "ROLES" "SLO" "PROBE"
  jq -r '.endpoints[] | [.id,.method,.path,.screen,.roles,.slo,.probe] | @tsv' "$REG" \
    | awk -F'\t' '{printf "%-22s %-7s %-46s %-20s %-14s %-8s %-6s\n",$1,$2,$3,$4,$5,$6,$7}'
  exit 0
fi

mkdir -p "$OUT_DIR"
TS="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="$OUT_DIR/health-$TS.json"

# resolve role_groups + slo budgets from the registry
declare -A SLO
SLO[fast]=$(jq -r '.slo_seconds.fast' "$REG")
SLO[medium]=$(jq -r '.slo_seconds.medium' "$REG")
SLO[slow]=$(jq -r '.slo_seconds.slow' "$REG")

persona_keys(){ # $1 = roles field (a group name); expand via role_groups
  jq -r --arg g "$1" '.role_groups[$g] // [$g] | .[]' "$REG"
}

login(){ curl -s --max-time 45 -X POST "$CB/companion/auth/login" -H 'Content-Type: application/json' \
         -d "{\"emailPhone\":\"$1\",\"password\":\"$2\"}"; }

echo ">> warming $CB ..." >&2
for i in 1 2 3; do curl -s -o /dev/null --max-time 60 "$CB/companion/build"; done

# ---- resolve per-persona session + context (groupId, clientId, savingsId, loanId) ----
declare -A TOK GID CID SID LID PNAME
for pk in $(jq -r '.personas|keys[]' "$REG"); do
  phone=$(jq -r --arg k "$pk" '.personas[$k].phone' "$REG")
  pw=$(jq -r --arg k "$pk" '.personas[$k].password' "$REG")
  PNAME[$pk]=$(jq -r --arg k "$pk" '.personas[$k].name' "$REG")
  tok=""; R=""
  for a in 1 2 3 4 5; do
    R="$(login "$phone" "$pw")"
    tok="$(printf '%s' "$R" | jq -r '.sessionToken // empty' 2>/dev/null)"
    [ -n "$tok" ] && break; sleep 5
  done
  TOK[$pk]="$tok"
  GID[$pk]="$(printf '%s' "$R" | jq -r '.groupMemberships[0].groupId // empty' 2>/dev/null)"
  [ -z "$tok" ] && { echo "   ! $pk login FAILED (backend flap?)" >&2; continue; }
  g="${GID[$pk]}"
  # clientId: match persona name in the group's member list, else first member
  members="$(curl -s --max-time 40 -H "Authorization: Bearer $tok" "$CB/companion/groups/$g/members" 2>/dev/null)"
  cid="$(printf '%s' "$members" | jq -r --arg n "${PNAME[$pk]}" 'try ((.pageItems // .) | map(select((.displayName//.name//"")==$n)) | .[0].id) // empty' 2>/dev/null)"
  [ -z "$cid" ] && cid="$(printf '%s' "$members" | jq -r 'try ((.pageItems // .)[0].id) // empty' 2>/dev/null)"
  CID[$pk]="$cid"
  # savingsId: from the caller's member-savings detail (savingsAccountNo, strip leading zeros)
  if [ -n "$cid" ]; then
    acc="$(curl -s --max-time 40 -H "Authorization: Bearer $tok" "$CB/companion/groups/$g/members/$cid/savings" 2>/dev/null | jq -r '.savingsAccountNo // empty' 2>/dev/null)"
    SID[$pk]="$(echo "$acc" | sed 's/^0*//')"
  fi
  # loanId: first loan on the group (optional; many groups have none)
  LID[$pk]="$(curl -s --max-time 40 -H "Authorization: Bearer $tok" "$CB/groups/$g/loans" 2>/dev/null | jq -r 'try (( .pageItems // . )[0].id) // empty' 2>/dev/null)"
  echo "   $pk: group=${GID[$pk]} client=${CID[$pk]} savings=${SID[$pk]:-none} loan=${LID[$pk]:-none}" >&2
done

# ---- probe one endpoint for one persona; echoes a JSON record ----
probe(){
  local pk="$1" id="$2" method="$3" path="$4" access="$5" screen="$6" slo="$7" ptype="$8" requires="$9"
  local tok="${TOK[$pk]}" g="${GID[$pk]}" c="${CID[$pk]}" s="${SID[$pk]}" l="${LID[$pk]}"
  # substitute params
  local p="$path"
  p="${p//\{groupId\}/$g}"; p="${p//\{clientId\}/$c}"; p="${p//\{savingsId\}/$s}"; p="${p//\{loanId\}/$l}"
  p="${p//\{code\}/x}"; p="${p//\{rowId\}/1}"
  local verdict status latency budget="${SLO[$slo]}"
  # skip if a required, unresolved param remains
  if [ "$requires" = "loanId" ] && [ -z "$l" ]; then
    jsonrec "$pk" "$id" "$method" "$p" "$access" "$screen" "$slo" "SKIP" "" "unresolved loanId"; return
  fi
  if [ "$requires" = "savingsId" ] && [ -z "$s" ]; then
    jsonrec "$pk" "$id" "$method" "$p" "$access" "$screen" "$slo" "SKIP" "" "unresolved savingsId"; return
  fi
  if [ -z "$tok" ]; then jsonrec "$pk" "$id" "$method" "$p" "$access" "$screen" "$slo" "NOAUTH" "" "no session"; return; fi
  # GET-probe everything (reads = real check; writes = wiring check, never executed)
  local res; res="$(curl -s --connect-timeout 15 --max-time 60 -o /dev/null -w '%{http_code}|%{time_total}' -H "Authorization: Bearer $tok" "$CB$p")"
  status="${res%%|*}"; latency="${res##*|}"
  if [ "$ptype" = "write" ]; then
    case "$status" in 000) verdict="FAIL";; 404) verdict="MISSING";; *) verdict="WIRED";; esac
  else
    case "$status" in
      2??) awk "BEGIN{exit !($latency > $budget)}" && verdict="SLOW" || verdict="OK";;
      401|403) verdict="DENY";;
      404) verdict="MISSING";;
      000) verdict="TIMEOUT";;
      5??) verdict="ERROR";;
      *) verdict="HTTP$status";;
    esac
  fi
  jsonrec "$pk" "$id" "$method" "$p" "$access" "$screen" "$slo" "$verdict" "$status" "$latency"
}

jsonrec(){ # pk id method path access screen slo verdict status latency_or_note
  jq -nc --arg pk "$1" --arg id "$2" --arg m "$3" --arg p "$4" --arg a "$5" --arg sc "$6" \
    --arg slo "$7" --arg v "$8" --arg st "$9" --arg lt "${10}" \
    '{persona:$pk,endpoint:$id,method:$m,path:$p,access:$a,screen:$sc,slo:$slo,verdict:$v,status:$st,latency:$lt}'
}

# ---- run the sweep ----
echo ">> probing $(jq -r '.endpoints|length' "$REG") endpoints x roles ..." >&2
: > "$OUT.tmp"
while read -r row; do
  id=$(jq -r '.id' <<<"$row"); method=$(jq -r '.method' <<<"$row"); path=$(jq -r '.path' <<<"$row")
  access=$(jq -r '.access' <<<"$row"); screen=$(jq -r '.screen' <<<"$row"); slo=$(jq -r '.slo' <<<"$row")
  ptype=$(jq -r '.probe' <<<"$row"); roles=$(jq -r '.roles' <<<"$row"); requires=$(jq -r '.requires // ""' <<<"$row")
  for pk in $(persona_keys "$roles"); do
    probe "$pk" "$id" "$method" "$path" "$access" "$screen" "$slo" "$ptype" "$requires" >> "$OUT.tmp"
  done
done < <(jq -c '.endpoints[]' "$REG")

# ---- assemble health-data JSON ----
jq -s --arg ts "$TS" --arg host "$CB" \
  '{generatedAt:$ts, host:$host,
    summary:( group_by(.verdict) | map({(.[0].verdict): length}) | add ),
    records: .}' "$OUT.tmp" > "$OUT"
cp "$OUT" "$OUT_DIR/latest.json"
rm -f "$OUT.tmp"

# ---- print the matrix ----
echo ""
echo "=== API HEALTH — $TS — $CB ==="
printf "%-22s %-7s %-9s | %-11s %-11s %-11s %-11s\n" "ENDPOINT" "METHOD" "SLO" "member" "treasurer" "organizer" "fieldoff"
while read -r id; do
  method=$(jq -r --arg i "$id" '.records[]|select(.endpoint==$i)|.method' "$OUT" | head -1)
  slo=$(jq -r --arg i "$id" '.records[]|select(.endpoint==$i)|.slo' "$OUT" | head -1)
  row=""
  for pk in member treasurer organizer fieldoff; do
    cell=$(jq -r --arg i "$id" --arg p "$pk" '(.records[]|select(.endpoint==$i and .persona==$p)) as $r | if $r then ($r.verdict + (if ($r.latency|test("^[0-9.]+$")) then "/"+($r.latency|tonumber|.*10|round/10|tostring)+"s" else "" end)) else "-" end' "$OUT" 2>/dev/null | head -1)
    [ -z "$cell" ] && cell="·"
    row="$row|$cell"
  done
  printf "%-22s %-7s %-9s %s\n" "$id" "$method" "$slo" "$row" | awk -F'|' '{printf "%-22s %-7s %-9s | %-11s %-11s %-11s %-11s\n",$1,$2,$3,$4,$5,$6,$7}'
done < <(jq -r '.endpoints[].id' "$REG")

echo ""
echo "=== SUMMARY ==="
jq -r '.summary | to_entries | sort_by(-.value) | .[] | "  \(.key): \(.value)"' "$OUT"
echo ""
echo "verdicts: OK=within SLO · SLOW=2xx over SLO budget · WIRED=write route registered · DENY=401/403 · MISSING=404 · ERROR=5xx · TIMEOUT · SKIP=unresolved param"
echo "health data: $OUT"
echo "            $OUT_DIR/latest.json"

# Classify failures. MISSING = a route-wiring regression (deterministic — a real defect). ERROR/
# TIMEOUT = usually a mifos-bank-2 flap (transient); DENY on a non-owned read = an access regression.
MISSING=$(jq '[.records[]|select(.verdict=="MISSING")]|length' "$OUT")
ERRTMO=$(jq '[.records[]|select(.verdict=="ERROR" or .verdict=="TIMEOUT")]|length' "$OUT")
DENY=$(jq '[.records[]|select(.verdict=="DENY")]|length' "$OUT")
[ "$((MISSING+ERRTMO+DENY))" -eq 0 ] && echo "✅ no hard failures" || echo "⚠️  MISSING=$MISSING ERROR/TIMEOUT=$ERRTMO DENY=$DENY — see records"

# --ci strict mode: fail the build on a DETERMINISTIC regression (route MISSING or an access DENY on a
# read the role should reach). ERROR/TIMEOUT alone do NOT fail (backend flakiness) — the workflow
# retries to absorb those; a persistent ERROR still shows in the artifact + summary.
if [ "${CI_STRICT:-0}" = "1" ] || [ "${1:-}" = "--ci" ]; then
  if [ "$MISSING" -gt 0 ] || [ "$DENY" -gt 0 ]; then
    echo "❌ CI: deterministic regression (MISSING=$MISSING DENY=$DENY)"; exit 1
  fi
  if [ "$ERRTMO" -gt 0 ]; then
    echo "::warning::API health: $ERRTMO ERROR/TIMEOUT record(s) — likely backend flap; not failing the build"
  fi
fi
exit 0
