# Verification suite (MifosSave)

Quick, repeatable checks that the app ↔ companion ↔ Fineract stack works end-to-end. Three layers:

- **`api-registry.json`** — the role-wise API **design** (single source of truth): every companion
  endpoint mapped to the screen + roles that consume it, its access model, latency SLO, and probe type.
- **`api-health.sh`** — the rule-driven **health runner** that reads the registry, probes every
  endpoint per role, and generates health data (`health-data/*.json`) + a matrix to optimize against.
- **`maestro/*.yaml`** — on-device behaviour flows.

## 0. API design + health (`api-registry.json` + `api-health.sh`)

The registry is the SoT for "which role reaches which API, and how fast should it be." The runner
turns it into live health data.

```bash
bash server-layer/verify/api-health.sh --design     # print the role-wise design table (no network)
bash server-layer/verify/api-health.sh              # run the health sweep → health-data/latest.json + matrix
COMPANION_BASE_URL=http://localhost:8080 bash server-layer/verify/api-health.sh
```

**Design fields** (per endpoint): `method`, `path`, `screen`, `roles` (`all` | `leadership` |
`fieldoff`, expanded via `role_groups`), `access` (`companion-service` = service-cred facade,
`companion-service-owned` = facade + ownership guard, `fineract-service` = raw Fineract via
service-cred passthrough), `slo` (`fast`/`medium`/`slow` → budget in `slo_seconds`), `probe`
(`safe` = GET read, executed; `write` = POST/PUT/DELETE, **wiring-checked only, never executed**).

**Verdicts** the runner emits per (endpoint, role):

| Verdict | Meaning |
|---------|---------|
| `OK/2.5s` | 2xx within the SLO budget |
| `SLOW/12s` | 2xx but **over** the SLO budget — an optimization target |
| `WIRED` | a write route is registered (non-404) — confirmed without executing it |
| `DENY` | 401/403 — expected only for a cross-owner read (ownership guard); a bug otherwise |
| `MISSING` | 404 — the route isn't wired for that role's path |
| `ERROR` / `TIMEOUT` | 5xx / no response — re-run (backend flaps); if persistent, a real defect |
| `SKIP` | a required path param (loanId/savingsId) couldn't be resolved for that persona |

**Health data** lands in `health-data/health-<timestamp>.json` + `latest.json` — a `summary` count
map + per-record `{persona, endpoint, verdict, status, latency, slo, access, screen}`. Diff two runs
to track regressions; sort records by latency to pick the next optimization.

**CI gate** — `.github/workflows/api-health.yml` runs this sweep daily (+ manual dispatch + on
`server-layer/verify/**` changes), uploads `latest.json` as an artifact, and writes the matrix to the
job summary. Because it probes the live companion + chronically-flaky mifos-bank-2, it hard-fails
ONLY on a **deterministic regression** — a route `MISSING` (wiring gone) or an access `DENY` on a
read a role should reach (`CI_STRICT=1`); transient `ERROR`/`TIMEOUT` are warnings, and the step
retries once to absorb flaps. Run strict locally: `CI_STRICT=1 bash api-health.sh`.

**Backend reports** — some endpoints run Fineract "stretchy" reports that must be registered on the
instance. `server-layer/migrations/register-reports/register-reports.sh` registers them (idempotent
upsert). Today: `FieldOfficerGroupReport` (the field-officer CSV/PDF export) — without it,
`fo-report` 5xx'd. mifos-bank-2 is PostgreSQL, so the report SQL uses double-quoted aliases +
`COALESCE`; the staff filter reuses the stock `loanOfficerIdSelectAll` parameter (`${loanOfficerId}`
← `R_loanOfficerId`), and the companion translates the app's `R_staffId` → `R_loanOfficerId`.

## Layers below are the older focused checks:

## 1. API audit (curl) — `api-audit.sh`

Per-role reachability + latency for every companion endpoint the app consumes. Run after **any
companion change** to confirm every role still reaches all its APIs and to catch latency
regressions on the heavy aggregation endpoints.

```bash
bash server-layer/verify/api-audit.sh
# or against a different host:
COMPANION_BASE_URL=http://localhost:8080 bash server-layer/verify/api-audit.sh
```

Reads as a matrix (status/latency per role). Interpreting results:

| Cell | Meaning |
|------|---------|
| `OK/2.5s` | reachable, latency in seconds |
| `DENY` | 401/403 — **expected** for a cross-group read (the ownership guard), a bug otherwise |
| `5xx` / `TMO` | server error / timeout — re-run (mifos-bank-2 flaps); if persistent, a real regression |

**Architecture invariant this guards:** a self-service login can only reach `/self/*`, never a
back-office API — so every app call goes through a `/companion/*` **service-credential** facade.
Role does NOT gate API access (the companion serves all roles); role only drives which *screens* the
app shows. Every endpoint should therefore be `OK` for every role (bar the intentional ownership
`DENY`).

**Latency guard:** `resolveGroups` reads `clients/{id}?associations=groups` directly instead of
scanning every group on the instance — keep `auth-me` ~2-3s, not the old ~16s. A regression to
double-digit seconds means someone reintroduced an all-groups scan.

## 2. Device flows (Maestro) — moved to `maestro/journeys/`

On-device behaviour checks (RULE-TEST-VERIFY-001 — Maestro is the sanctioned runtime path) now live
in the app's canonical Maestro testing layer at **`maestro/journeys/`** (repo root), alongside the
existing `maestro/screen-state/` flows. Full read journey (login → group list/detail → members →
member profile → loans → loan detail → savings → meetings), each a regression net over its screen's
DTO deserialization. See `maestro/journeys/README.md`.

```bash
export PATH="$PATH:$HOME/.maestro/bin"
maestro test maestro/journeys/                 # whole suite
maestro test maestro/journeys/08-full-read-journey.yaml
```

Demo personas + passwords (per-persona, they differ):

| Persona | Phone | Password | Role |
|---------|-------|----------|------|
| Amina Otieno | +254700000001 | `DemoExplore@2026` | Treasurer (Demo Explore login) |
| Joseph Mwangi | +254700000002 | `DemoChair@2026` | Chairperson → organizer |
| Grace Wanjiru | +254700000003 | `DemoMember@2026` | Member |
| David Ochieng | +254700000010 | `DemoField@2026` | Field officer |

> Maestro `clearState: true` fails on some OEMs (CLEAR_APP_USER_DATA perm); these flows omit it and
> rely on `launchApp`. For a truly pristine state use `adb uninstall` + reinstall (some ColorOS
> builds don't fully wipe EncryptedSharedPreferences on `pm clear`).
