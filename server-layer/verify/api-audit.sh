#!/usr/bin/env bash
# Per-role companion API reachability + latency audit.
#
# Logs in as each demo persona (MEMBER / TREASURER / ORGANIZER / field-officer) and curls every
# endpoint the app consumes, recording HTTP status + latency. Use it to confirm — after any companion
# change — that every role still reaches all the APIs its screens need, and to catch latency
# regressions on the heavy aggregation endpoints (auth/me, groups/mine, organizer/dashboard).
#
# Usage:
#   COMPANION_BASE_URL=https://mifossave-companion.onrender.com bash api-audit.sh
#   bash api-audit.sh            # defaults to the onrender URL below
#
# Notes:
#  - mifos-bank-2 (Fineract) is chronically flaky and onrender free-tier cold-starts; the script warms
#    the host and retries login. A 502 storm means the BACKEND is down, not the companion (check
#    GET /companion/build → 200 = companion up).
#  - "DENY" on a cross-group savings read is EXPECTED (the ownership guard); it is not a failure.

set -u
CB="${COMPANION_BASE_URL:-https://mifossave-companion.onrender.com}"
CB="${CB%/}"; CB="${CB%/companion}"   # tolerate a trailing /companion

# demo personas: label | phone | password  (passwords differ per persona)
PERSONAS=(
  "MEMBER|+254700000003|DemoMember@2026"
  "TREASURER|+254700000001|DemoExplore@2026"
  "ORGANIZER|+254700000002|DemoChair@2026"
  "FIELDOFF|+254700000010|DemoField@2026"
)

# endpoint | path-template ({g} = persona's first group)
ENDPOINTS=(
  "auth-me|/companion/auth/me"
  "groups-mine|/companion/groups/mine"
  "member-dashboard|/companion/member/dashboard?groupId={g}"
  "organizer-dashboard|/companion/organizer/dashboard"
  "group-detail|/companion/groups/{g}"
  "group-my-role|/companion/groups/{g}/my-role"
  "group-corpus|/companion/groups/{g}/corpus"
  "group-accounts|/companion/groups/{g}/accounts"
  "group-dashboard|/companion/groups/{g}/dashboard"
  "group-savings|/companion/groups/{g}/savings"
  "group-savings-indiv|/companion/groups/{g}/savings/individual"
  "loan-requests|/companion/groups/{g}/loan-requests"
  "dt-group-config|/datatables/dt_group_config/{g}"
  "dt-meeting-record|/datatables/dt_meeting_record/{g}"
  "loanproducts|/loanproducts"
  "group-loans|/groups/{g}/loans"
  "fo-groups|/companion/field-officer/groups"
)

cls(){ case "$1" in 2??) echo "OK";; 401|403) echo "DENY";; 404) echo "404";; 000) echo "TMO";; 5??) echo "5xx";; *) echo "$1";; esac; }
hit(){ curl -s --connect-timeout 15 --max-time 60 -o /dev/null -w '%{http_code}|%{time_total}' -H "Authorization: Bearer $1" "$CB$2"; }
login(){ curl -s --max-time 45 -X POST "$CB/companion/auth/login" -H 'Content-Type: application/json' -d "{\"emailPhone\":\"$1\",\"password\":\"$2\"}"; }

echo ">> warming $CB ..."
for i in 1 2 3; do curl -s -o /dev/null --max-time 60 "$CB/companion/build"; done

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
for p in "${PERSONAS[@]}"; do
  IFS='|' read -r lvl phone pw <<< "$p"
  ( # per-persona, in parallel
    tok=""; g="0"; role="?"
    for a in 1 2 3; do
      R="$(login "$phone" "$pw")"
      tok="$(printf '%s' "$R" | jq -r '.sessionToken // empty' 2>/dev/null)"
      [ -n "$tok" ] && { g="$(printf '%s' "$R" | jq -r '.groupMemberships[0].groupId // "0"')"; role="$(printf '%s' "$R" | jq -r '.groupMemberships[0].role // "?"')"; break; }
      sleep 4
    done
    echo "PERSONA|$lvl|role=$role|group=$g|token=$([ -n "$tok" ] && echo ok || echo FAIL)" > "$TMP/$lvl"
    for e in "${ENDPOINTS[@]}"; do
      IFS='|' read -r label tmpl <<< "$e"
      path="${tmpl//\{g\}/$g}"
      printf '%s|%s\n' "$label" "$(hit "$tok" "$path")" >> "$TMP/$lvl"
    done
  ) &
done
wait

echo ""
echo "=== PERSONAS ==="
for p in "${PERSONAS[@]}"; do IFS='|' read -r lvl _ _ <<< "$p"; grep '^PERSONA' "$TMP/$lvl"; done
echo ""
printf "%-22s %-12s %-12s %-12s %-12s\n" "ENDPOINT" "MEMBER" "TREASURER" "ORGANIZER" "FIELDOFF"
fail=0
for e in "${ENDPOINTS[@]}"; do
  IFS='|' read -r label _ <<< "$e"
  row="$label"
  for lvl in MEMBER TREASURER ORGANIZER FIELDOFF; do
    line="$(grep "^$label|" "$TMP/$lvl" | head -1)"
    st="${line#*|}"; st="${st%%|*}"; tm="${line##*|}"
    verd="$(cls "$st")"
    [ "$verd" = "5xx" ] || [ "$verd" = "TMO" ] && fail=$((fail+1))
    printf -v cell "%s/%.1fs" "$verd" "$tm"
    row="$row|$cell"
  done
  echo "$row" | awk -F'|' '{printf "%-22s %-12s %-12s %-12s %-12s\n",$1,$2,$3,$4,$5}'
done
echo ""
[ "$fail" -eq 0 ] && echo "✅ no 5xx / timeouts" || echo "⚠️  $fail cell(s) 5xx/TMO — re-run (backend may be flapping) or investigate"
