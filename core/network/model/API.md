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
| `InvitationRowDto` | `token`, `group_id`, `inviter_client_id`, `invited_email_phone`, `role_to_assign` (default `UNKNOWN`, reuses `GroupRoleDto`), `expires_at`, `accepted_at` (nullable, default `null`) | `roleToAssign` defaults `UNKNOWN`; `acceptedAt` nullable (`null` = code unused); rest required. Raw Fineract datatable columns — snake_case `@SerialName`s, NOT the companion-bridge camelCase convention used elsewhere in this file | `GET /companion/datatables/invitations/{entityId}` (COMP-DT-004) |
| `GroupPreviewDto` | `groupId`, `groupName`, `groupType` (default `UNKNOWN`, reuses `GroupTypeSlugDto`), `organizerName`, `memberCount`, `officeId`, `roleToAssign` (default `UNKNOWN`, reuses `GroupRoleDto`) | 2 enum fields default `UNKNOWN`; rest required. Companion-bridge camelCase | `GET /companion/groups/{groupId}` |
| `AssociateClientsRequestDto` | `clientIds`, `roleToAssign` (default `UNKNOWN`, reuses `GroupRoleDto`) | `clientIds` required, non-empty expected; `roleToAssign` defaults `UNKNOWN` | `POST /companion/groups/{groupId}/associate-clients` (COMP-GRP-003) request |
| `AssociateClientsResponseDto` | `resourceId`, `groupId`, `clientIds` | all required | response of COMP-GRP-003 |
| `MarkAcceptedRequestDto` | `accepted_at` | required | `PUT /companion/datatables/invitations/{entityId}/{rowId}` (COMP-DT-004) request; snake_case (raw datatable column) |
| `MarkAcceptedResponseDto` | `resourceId`, `changes` (nested `MarkAcceptedChangesDto`) | all required | response of the mark-accepted `PUT` |
| `MarkAcceptedChangesDto` | `accepted_at` | required | nested in `MarkAcceptedResponseDto.changes`; snake_case, echoes the updated datatable column |
| `MemberDashboardResponseDto` | `memberName`, `myGroups` (default `[]`), `selectedGroup`, `poolModel` (default `UNKNOWN`, reuses `SavingsMechanismDto`), `groupLinkedSavingsBalance`, `individualSavingsBalance`, `shareOutProjection` (nullable, default `null`), `rotationPosition` (nullable, default `null`), `nextRecipientEta` (nullable, default `null`), `recentTransactions` (default `[]`) | 3 nullable fields default `null`; `myGroups`/`recentTransactions` default empty; `poolModel` defaults `UNKNOWN`; rest required | `GET /companion/member/dashboard` — unified-identity companion API, no `clientId`/`selfServiceToken` |
| `GroupSummaryDto` | `groupId`, `name`, `poolModel` (default `UNKNOWN`, reuses `SavingsMechanismDto`) | `poolModel` defaults `UNKNOWN`; rest required | field of `MemberDashboardResponseDto.myGroups` / `.selectedGroup` |
| `SavingsTransactionDto` | `id`, `date`, `type` (default `UNKNOWN`), `amount` | `type` defaults `UNKNOWN`; rest required | field of `MemberDashboardResponseDto.recentTransactions` — CANONICAL compact shape, see naming-collision note below |
| `TransactionTypeDto` | enum `@SerialName`: `DEPOSIT`, `WITHDRAWAL`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback (also absorbs richer wire values like `INTEREST_POSTING`/`FEE_DEDUCTION` that this compact contract does not model) | field of `SavingsTransactionDto.type` |

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`;
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003);
`idea-layer/screens/group-list/{api.yaml,docs.yaml,data-flow.yaml}` (COMP-GRP-001);
`idea-layer/screens/join-with-code/{api.yaml,docs.yaml}` (COMP-DT-004 + COMP-GRP-003);
`idea-layer/screens/personal-dashboard/{api.yaml,docs.yaml}` (companion `GET
/companion/member/dashboard`, status: approved, approved 2026-07-17).

**Enum reuse (join-with-code, no new enums introduced):** `InvitationRowDto.roleToAssign`,
`GroupPreviewDto.roleToAssign`, and `AssociateClientsRequestDto.roleToAssign` all reuse the
existing `GroupRoleDto` (declared in `LoginSignupDto.kt`) — its value-set
(`ORGANIZER`/`MEMBER`/`TREASURER`/`SECRETARY`/`UNKNOWN`) exactly covers every role an invite can
assign; `ViewerRoleDto`'s extra `CHAIRPERSON` value is never assignable via invite so it was not
the fit. `GroupPreviewDto.groupType` reuses the existing `GroupTypeSlugDto` (declared in
`GroupTypeConfigDto.kt`, long-form slugs) rather than the short-form `GroupTypeDto` used by
`GroupDto` — this is a companion-bridge group lookup (like COMP-DT-003), not the group-list row
contract.

**Enum reuse (personal-dashboard, no new pool-model enum introduced):**
`MemberDashboardResponseDto.poolModel` and `GroupSummaryDto.poolModel` both
reuse the existing `SavingsMechanismDto` (declared in `GroupTypeConfigDto.kt`)
rather than introducing a new pool-model wire enum — its value-set
(`ACCUMULATING`/`ROTATING_PAYOUT`/`NONE`/`UNKNOWN`) exactly matches
`idea-layer/screens/personal-dashboard/api.yaml#dtos.GroupSummary.poolModel`'s
declared `ACCUMULATING | ROTATING_PAYOUT | NONE`.

**Naming collision (`SavingsTransactionDto`, flagged for the cross-feature
repair station — NOT resolved here, out of this generation's scope):** three
sources declare a type named `SavingsTransactionDto` with THREE incompatible
shapes:

1. **This file** (from `personal-dashboard`'s own approved `api.yaml`, status
   `approved`, approved 2026-07-17): compact companion shape — `id: String`,
   `date: String`, `type: TransactionTypeDto` (`DEPOSIT`/`WITHDRAWAL`/`UNKNOWN`),
   `amount: Double`.
2. `idea-layer/dtos/SavingsTransactionDto.yaml` (registry v1.0.0): richer
   Fineract-raw shape — `id: Long`, `memberId: Long`, `savingsAccountId: Long`,
   `transactionType: String` (4 values incl. `interest_posting`/`fee_deduction`),
   `date: String`, `currency: String`, `runningBalance: Double?`, `note: String?`
   — sourced from `GET /savingsaccounts/{accountId}/transactions` and lists
   `end-user-dashboard` / api_id `list_self_savings` as a consumer, but that
   api_id does not exist on `personal-dashboard`'s current `api.yaml`
   (`get_member_dashboard` is the only operation); `docs.yaml` states the
   single companion call REPLACES the old per-account SelfService path this
   registry entry describes — the registry entry is stale for this consumer.
3. `idea-layer/screens/personal-savings/api.yaml#dtos.SavingsTransactionDto`:
   still-richer raw-Fineract-SelfService component shape — `id: Long`,
   `transactionType: {value: Int, code: String, description: String}`,
   `date: List<Int>`, `amount: Double`, `runningBalance: Double`,
   `currency: {code: String, displaySymbol: String}`.

This generation emitted shape (1), the compact companion contract, per Hard
Rule 5 (`@SerialName` must match the DECLARING feature's own approved
contract) — `personal-dashboard`'s `api.yaml` is the SoT for THIS feature's
DTO. Before `personal-savings` (or `savings-dashboard`, though its own
`api.yaml` does not currently declare a `SavingsTransactionDto` at all) is
generated via `kmp-dto-gen`, this class-name collision MUST be resolved at
Station 3 — options include renaming `personal-savings`' raw ledger row to
`SavingsLedgerEntryDto`, or migrating `personal-savings` onto the companion
API and reusing THIS `SavingsTransactionDto` outright (matching the pattern
already established for `GroupDto` below).

**Wire-casing note:** `InvitationRowDto` and `MarkAcceptedRequestDto`/`MarkAcceptedChangesDto`
use snake_case `@SerialName`s (raw Fineract datatable columns, matching `idea-layer/screens/join-with-code/api.yaml#dtos.InvitationRow` /
`#dtos.MarkAcceptedRequest` verbatim) — distinct from every other DTO in this file, which uses the
companion bridge's normalized camelCase. `GroupPreviewDto` / `AssociateClientsRequestDto` /
`AssociateClientsResponseDto` (companion-bridge, non-datatable endpoints) use camelCase as usual.

**Registry gap (flagged for the cross-feature repair station):** `mark_invitation_accepted`'s
`rowId` path param is declared sourced from `validate_invite_token_response.id`, but neither
`api.yaml#api[0].response.fields` nor `#dtos.InvitationRow` declare an `id` field —
`InvitationRowDto` therefore does not carry one either (Hard Rule 4 forbids inventing an
undeclared field). The repository/use-case layer that wires `mark_invitation_accepted`'s `rowId`
will need this contract gap resolved upstream in the idea-layer `api.yaml`.

Field-name casing precedent: `idea-layer/dtos/LoanDto.yaml` (camelCase wire
fields, e.g. `memberId`, `disbursedOn`) — the companion bridge returns
camelCase JSON, not raw Postgres snake_case, so every `@SerialName` here
matches `api.yaml#dtos` field names verbatim (no case translation).

Domain counterparts + field mapping: see `core/model/API.md`. DTO↔domain
mappers: `core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/mapper/LoginSignupMappers.kt`,
`GroupTypeConfigMappers.kt`, `GroupMappers.kt`, `JoinWithCodeMappers.kt`,
`MemberDashboardMappers.kt`, `SavingsTransactionMappers.kt`.

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
