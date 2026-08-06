#!/usr/bin/env bash
# Response-SHAPE audit: does the app actually PARSE each 200 response?
#
# api-health.sh checks HTTP status only. A 200 with a missing/renamed required field still breaks the
# app: kotlinx.serialization throws "Field 'X' is required ... but it was missing" and the screen
# shows its error state (e.g. group-list "Could not load groups" while /companion/groups/mine was 200).
#
# This tool fetches each screen-rendering GET live and checks the response contains every REQUIRED
# field of the app DTO it deserializes into (a Kotlin `val name: Type` with NO `?` and NO `= default`).
# Missing required fields = a guaranteed deserialization failure on that screen.
#
# Usage:
#   COMPANION_BASE_URL=https://mifossave-companion.onrender.com bash api-response-audit.sh
#   APP_SRC=/path/to/app bash api-response-audit.sh      # DTO source root (default: repo of this script)
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
APP_SRC="${APP_SRC:-$(cd "$HERE/../.." && pwd)}"
MODEL_DIR="$APP_SRC/core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/model"
CB="${COMPANION_BASE_URL:-https://mifossave-companion.onrender.com}"; CB="${CB%/}"; CB="${CB%/companion}"
command -v jq >/dev/null || { echo "jq required"; exit 2; }
[ -d "$MODEL_DIR" ] || { echo "model dir not found: $MODEL_DIR"; exit 2; }

# ---- endpoint | path (params: {g}=group {c}=client {s}=savings {l}=loan) | DTO class | jq-path to a sample object ----
MAP=(
  "groups-mine|/companion/groups/mine|GroupDto|.pageItems[0]"
  "group-detail|/companion/groups/{g}|GroupDetailDto|."
  "group-corpus|/companion/groups/{g}/corpus|GroupCorpusDto|."
  "group-accounts|/companion/groups/{g}/accounts|GroupAccountsDto|."
  "my-role|/companion/groups/{g}/my-role|ViewerRoleInfoDto|."
  "member-dashboard|/companion/member/dashboard?groupId={g}|MemberDashboardResponseDto|."
  "organizer-dashboard|/companion/organizer/dashboard|OrganizerDashboardSummaryDto|."
  "group-savings|/companion/groups/{g}/savings|GroupSavingsSummaryDto|."
  "group-savings-indiv|/companion/groups/{g}/savings/individual|IndividualSavingsSummaryDto|."
  "savings-txn|/companion/savings/{s}/transactions|SavingsLedgerEntryDto|.[0]"
  "member-savings|/companion/groups/{g}/members/{c}/savings|MemberSavingsDetailDto|."
  "group-loans-envelope|/groups/{g}/loans|LoanPageDto|."
  "group-loans-row|/groups/{g}/loans|LoanSummaryDto|.pageItems[0]"
  "loan-detail|/loans/{l}|LoanDetailResponseDto|."
  "member-list-envelope|/groups/{g}/clients|MemberPageDto|."
  "member-list-row|/groups/{g}/clients|MemberDto|.pageItems[0]"
)

# required_fields CLASS -> prints the @SerialName (or val name) of each REQUIRED top-level field.
# Required = `val name: Type` inside the data-class ctor, Type has no trailing `?`, line has no `= default`.
required_fields() {
  local cls="$1" file
  file="$(grep -rl "data class $cls(" "$MODEL_DIR" 2>/dev/null | head -1)"
  [ -z "$file" ] && { echo "__NOFILE__"; return; }
  awk -v cls="$cls" '
    $0 ~ ("data class " cls "\\(") { inblk=1 }
    inblk {
      line=$0
      # capture a pending @SerialName on its own line
      if (match(line, /@SerialName\("[^"]+"\)/)) {
        sn=substr(line, RSTART+13, RLENGTH-15)
      }
      if (match(line, /val[ \t]+[A-Za-z0-9_]+[ \t]*:/)) {
        # field name
        f=line; sub(/.*val[ \t]+/, "", f); sub(/[ \t]*:.*/, "", f)
        # type segment (between : and , or = or ) )
        t=line; sub(/.*val[ \t]+[A-Za-z0-9_]+[ \t]*:[ \t]*/, "", t)
        hasdefault=(index(line,"=")>0)
        # nullable if the type token ends with ? (before , = ) )
        ttok=t; sub(/[,=)].*/, "", ttok); gsub(/[ \t]/,"",ttok)
        nullable=(substr(ttok,length(ttok),1)=="?")
        key=(sn!=""?sn:f)
        if (!hasdefault && !nullable) print key
        sn=""
      }
      if (index(line,")")>0 && inblk && NR>1 && line !~ /val/ && line !~ /data class/) { inblk=0 }
    }
  ' "$file"
}

# ---- resolve params as Amina (treasurer, group 42) ----
TOK=""; for a in 1 2 3 4 5; do
  TOK="$(curl -s --max-time 45 -X POST "$CB/companion/auth/login" -H 'Content-Type: application/json' -d '{"emailPhone":"+254700000001","password":"DemoExplore@2026"}' | jq -r '.sessionToken // empty' 2>/dev/null)"
  [ -n "$TOK" ] && break; sleep 5
done
[ -z "$TOK" ] && { echo "login failed (backend flap) — retry later"; exit 3; }
G="$(curl -s --max-time 40 -H "Authorization: Bearer $TOK" "$CB/companion/groups/mine" | jq -r '.pageItems[0].fineractGroupId // .pageItems[0].id // "42"' 2>/dev/null)"
[ -z "$G" ] && G=42
C="$(curl -s --max-time 40 -H "Authorization: Bearer $TOK" "$CB/companion/groups/$G/members" | jq -r 'try ((.pageItems//.)[0].id) // empty' 2>/dev/null)"
S="$(curl -s --max-time 40 -H "Authorization: Bearer $TOK" "$CB/companion/groups/$G/members/$C/savings" | jq -r '.savingsAccountNo // empty' 2>/dev/null | sed 's/^0*//')"
L="$(curl -s --max-time 40 -H "Authorization: Bearer $TOK" "$CB/groups/$G/loans" | jq -r 'try (.pageItems[0].id) // empty' 2>/dev/null)"
echo ">> host=$CB  group=$G client=$C savings=${S:-none} loan=${L:-none}"
echo ""

# ---- audit each endpoint ----
FAILS=0
printf "%-22s %-8s %s\n" "ENDPOINT" "HTTP" "SHAPE"
for row in "${MAP[@]}"; do
  IFS='|' read -r id path cls jqpath <<< "$row"
  p="$path"; p="${p//\{g\}/$G}"; p="${p//\{c\}/$C}"; p="${p//\{s\}/$S}"; p="${p//\{l\}/$L}"
  if { [ "$id" = "savings-txn" ] && [ -z "$S" ]; } || { [ "$id" = "loan-detail" ] && [ -z "$L" ]; }; then
    printf "%-22s %-8s %s\n" "$id" "-" "SKIP (unresolved param)"; continue
  fi
  code="$(curl -s -o /tmp/ra.json -w '%{http_code}' --connect-timeout 15 --max-time 60 -H "Authorization: Bearer $TOK" "$CB$p")"
  if [ "$code" != "200" ]; then printf "%-22s %-8s %s\n" "$id" "$code" "(non-200)"; continue; fi
  sample="$(jq -c "$jqpath // {}" /tmp/ra.json 2>/dev/null)"
  reqs="$(required_fields "$cls")"
  if [ "$reqs" = "__NOFILE__" ]; then printf "%-22s %-8s %s\n" "$id" "$code" "DTO $cls not found"; continue; fi
  missing=""; nulls=""
  while read -r f; do
    [ -z "$f" ] && continue
    # The extracted fields are all NON-nullable + no-default (required), so both an ABSENT key and a
    # present-but-null value fail kotlinx deserialization. Check both (null caught mobileNo/imagePresent).
    present="$(echo "$sample" | jq --arg k "$f" 'has($k)' 2>/dev/null)"
    if [ "$present" != "true" ]; then
      missing="$missing $f"
    else
      isnull="$(echo "$sample" | jq --arg k "$f" '.[$k] == null' 2>/dev/null)"
      [ "$isnull" = "true" ] && nulls="$nulls $f"
    fi
  done <<< "$reqs"
  missing="$missing$([ -n "$nulls" ] && echo " [null:$nulls]")"
  # DATE-FORMAT heuristic: several app fields (lastMeetingDate, joinedAt, dates) are parsed with
  # LocalDate.parse (date-only). A full ISO instant (…-…-…T…) in the response makes that throw a
  # DateTimeParseException at index 10 — a 200 the app still can't map. Flag any full-instant string
  # value as a risk (warning, not a hard fail; joinedAt/tokenExpiresAt legitimately ARE instants).
  instants="$(echo "$sample" | jq -r 'to_entries[]? | select((.value|type)=="string" and (.value|test("^[0-9]{4}-[0-9]{2}-[0-9]{2}T"))) | .key' 2>/dev/null | tr '\n' ' ')"
  if [ -z "$missing" ]; then
    note=""; [ -n "$(echo "$instants"|tr -d ' ')" ] && note="  ⚠ instant-valued:$instants (LocalDate.parse risk)"
    printf "%-22s %-8s ✅ all %s required fields present%s\n" "$id" "$code" "$(echo "$reqs"|grep -c .)" "$note"
  else
    printf "%-22s %-8s ❌ MISSING:%s  (DTO %s)\n" "$id" "$code" "$missing" "$cls"
    FAILS=$((FAILS+1))
  fi
done
echo ""
echo "note: ⚠ instant-valued flags a full ISO instant where the app may want date-only (LocalDate.parse)."
[ "$FAILS" -eq 0 ] && echo "✅ every checked response satisfies its DTO's required fields" || echo "❌ $FAILS endpoint(s) return a shape the app cannot deserialize — see MISSING above"
exit 0
