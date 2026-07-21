<!-- generated-by: kmp-dto-gen -->
<!-- kmp-dto-gen:BEGIN -->
# core/network/model — API.md

## dtos

| DTO | `@SerialName` mapping | Nullability | Wire operation |
|---|---|---|---|
| `SelfRegisterRequestDto` | `name`, `emailPhone`, `password` | all non-null, required | `POST /companion/auth/self-register` (COMP-AUTH-001) |
| `LoginRequestDto` | `emailPhone`, `password` | all non-null, required | `POST /companion/auth/login` (COMP-AUTH-002) |
| `AuthResponseDto` | `userId`, `sessionToken`, `tokenExpiresAt`, `groupMemberships` (default `[]`) | `groupMemberships` defaults empty; others required | response of COMP-AUTH-001 + COMP-AUTH-002 |
| `UserProfileDto` | `userId`, `name`, `emailPhone`, `groupMemberships` (default `[]`) | `groupMemberships` defaults empty; others required | `GET /companion/auth/me` (COMP-AUTH-003) |
| `GroupMembershipDto` | `groupId`, `groupName`, `role` (default `UNKNOWN`), `joinedAt` | `role` defaults `UNKNOWN`; others required | nested in `AuthResponseDto` / `UserProfileDto` |
| `GroupRoleDto` | enum `@SerialName`: `ORGANIZER`, `MEMBER`, `TREASURER`, `SECRETARY`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback for any future server-added role | field of `GroupMembershipDto.role` |
| `GroupTypeConfigDto` | `typeSlug` (default `UNKNOWN`), `displayName`, `tagline`, `savingsMechanism` (default `UNKNOWN`), `contributionMode` (default `UNKNOWN`), `lendingEnabled`, `hasSocialFund`, `hasBankLinkage`, `welfareOnlyMode`, `formallyRegistered`, `defaultLoanMultiplier`, `defaultInterestRatePct`, `defaultCycleLengthMonths`, `maxMembers`, `minMembers` | 3 enum fields default `UNKNOWN`; remaining 12 non-null required | `GET /companion/datatables/group_type_config/{entityId}` (COMP-DT-003, entityId=0=seed catalogue) |
| `GroupTypeSlugDto` | enum `@SerialName`: `VSLA`, `ROSCA`, `ASCA`, `SILC`, `SHG`, `SACCO`, `CBO_VILLAGE_BANK`, `BURIAL_WELFARE`, `JLG`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback for any future server-added group type | field of `GroupTypeConfigDto.typeSlug` |
| `SavingsMechanismDto` | enum `@SerialName`: `ACCUMULATING`, `ROTATING_PAYOUT`, `NONE`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `GroupTypeConfigDto.savingsMechanism` |
| `ContributionModeDto` | enum `@SerialName`: `SHARE_BASED_VARIABLE`, `FIXED`, `MINIMAL`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `GroupTypeConfigDto.contributionMode` |
| `GroupDto` | `id`, `name`, `groupType` (default `UNKNOWN`), `viewerRole` (default `UNKNOWN`), `cycleNumber`, `memberCount`, `lastMeetingDate`, `healthIndicator` (default `UNKNOWN`), `overdueRate`, `status`, `fineractCenterId` | 3 enum fields default `UNKNOWN`; remaining 8 non-null required | `GET /companion/groups/mine` (COMP-GRP-001) — CANONICAL `Group`, reused by group-dashboard + member features |
| `GroupPageDto` | `totalFilteredRecords`, `pageItems` (default `[]`) | `pageItems` defaults empty; `totalFilteredRecords` required | offset-paginated envelope of COMP-GRP-001 (`page_size=20`) |
| `GroupTypeDto` | enum `@SerialName`: `VSLA`, `ROSCA`, `ASCA`, `SILC`, `SHG`, `SACCO`, `CBO`, `BURIAL`, `JLG`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback; NOTE short-form `CBO`/`BURIAL` (distinct wire values from `GroupTypeSlugDto`'s `CBO_VILLAGE_BANK`/`BURIAL_WELFARE` — see `## 4. Boundaries`) | field of `GroupDto.groupType` |
| `ViewerRoleDto` | enum `@SerialName`: `ORGANIZER`, `MEMBER`, `TREASURER`, `CHAIRPERSON`, `SECRETARY`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `GroupDto.viewerRole` |
| `HealthIndicatorDto` | enum `@SerialName`: `GREEN`, `AMBER`, `RED`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback; server-computed from `overdueRate`, mapped 1:1 (domain also independently re-derives via `HealthIndicator.fromOverdueRate`) | field of `GroupDto.healthIndicator` |

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`;
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003);
`idea-layer/screens/group-list/{api.yaml,docs.yaml,data-flow.yaml}` (COMP-GRP-001).

Field-name casing precedent: `idea-layer/dtos/LoanDto.yaml` (camelCase wire
fields, e.g. `memberId`, `disbursedOn`) — the companion bridge returns
camelCase JSON, not raw Postgres snake_case, so every `@SerialName` here
matches `api.yaml#dtos` field names verbatim (no case translation).

Domain counterparts + field mapping: see `core/model/API.md`. DTO↔domain
mappers: `core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/mapper/LoginSignupMappers.kt`,
`GroupTypeConfigMappers.kt`, `GroupMappers.kt`.

**Registry divergence note (PP-1, flagged for the cross-feature repair
station):** `idea-layer/dtos/GroupDto.yaml` (registry v2.0.0) declares a
DIFFERENT `Group` shape (`id: Long`, `groupTypeSlug`, `poolModel`,
`contributionModel`, `corpusBalance`, `officeId`, `nextMeetingDate`,
`staffId`) sourced from `GET /centers/{centerId}` / `GET
/centers?staffId={staffId}` and claims `used_by: group-list`. That reference
is STALE: `idea-layer/screens/group-list/docs.yaml` states COMP-GRP-001 (`GET
/companion/groups/mine`) explicitly REPLACES the staff-only `/centers?staffId=`
path, and the screen's own approved `api.yaml#dtos.Group` (the shape emitted
here — `id: String`, `viewerRole`, `healthIndicator`, `overdueRate`,
`fineractCenterId`, etc.) is the current contract (`status: approved`,
approved 2026-07-17). `GroupDto` in this file was generated from the
group-list screen's own `api.yaml`/`docs.yaml`/`data-flow.yaml`, NOT from the
stale registry entry. The registry's `list_endpoint` + field set should be
reconciled against COMP-GRP-001 (or re-scoped to the admin/staff
`group-management` endpoints it still accurately describes) at Station 3.
<!-- kmp-dto-gen:END -->
