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
| `CreateGroupRequestDto` | `name`, `officeId`, `userId`, `currency`, `meetingDay`, `meetingTime`, `typeConfig` | all required, camelCase | `POST /companion/groups` (COMP-GRP-001) request |
| `CreateGroupTypeConfigDto` | `group_type` (default `UNKNOWN`, reuses `GroupTypeDto`), `pool_model` (default `UNKNOWN`, reuses `SavingsMechanismDto`), `contribution_model` (default `UNKNOWN`), `shareout_formula` (default `UNKNOWN`), `payout_order_method` (default `UNKNOWN`), `share_value`, `contribution_amount`, `social_fund_enabled`, `social_fund_percent`, `cycle_length_months`, `loan_multiplier`, `interest_rate`, `fine_amount`, `max_members` | 5 enum fields default `UNKNOWN`; remaining 9 non-null required. **snake_case** — raw `group_type_config` Fineract datatable columns (Hard Rule 5), NOT the companion camelCase convention | field of `CreateGroupRequestDto.typeConfig`; provisioned as a `group_type_config` datatable row (COMP-GRP-001 step 5) |
| `CreateGroupResponseDto` | `groupId`, `fineractCenterId`, `inviteCode` | all required, camelCase | response of COMP-GRP-001 |
| `OfficeDto` | `id`, `name`, `nameDecorated`, `externalId` (nullable, default `null`) | `externalId` nullable/optional (registry-vs-operation-schema gap, see note below); rest required | `GET /offices` (`orderBy=name` default), cached SWR (`ttl=3600`) |
| `ContributionModelDto` | enum `@SerialName`: `FIXED_AMOUNT`, `SHARE_BASED_VARIABLE`, `FIXED_NEGOTIATED`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback; DISTINCT value-set from `ContributionModeDto` — see reuse note below | field of `CreateGroupTypeConfigDto.contributionModel` |
| `ShareoutFormulaDto` | enum `@SerialName`: `NONE`, `PRORATA_SHARES`, `PRORATA_SAVINGS`, `EQUAL`, `INVESTMENT_PROPORTIONAL`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `CreateGroupTypeConfigDto.shareoutFormula` |
| `PayoutOrderMethodDto` | enum `@SerialName`: `FIXED_ORDER`, `LOTTERY`, `AUCTION`, `NEED_BASED`, `NA`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback; api.yaml declares 5 known values (task prose narrowed to 3 — api.yaml wins per PP-1) | field of `CreateGroupTypeConfigDto.payoutOrderMethod` |
| `GroupDashboardResponseDto` | `group`, `viewerRole`, `corpus`, `accounts` | all required, camelCase | client-side composite (COMP-GRP-001 4-way parallel fan-in); NOT returned by a single endpoint, assembled by `GroupRepository` |
| `GroupDetailDto` | `id`, `fineractCenterId`, `name`, `cycleNumber`, `cycleLengthMonths`, `meetingFrequency`, `memberCount`, `overdueLoansCount`, `status`, `typeConfig` | all required, camelCase | `GET /companion/groups/{groupId}` (`get_group`, COMP-GRP-001 read path); NOT the same shape as `GroupDto` — see field-shape-divergence note below |
| `GroupInstanceConfigDto` | `group_type` (default `UNKNOWN`, reuses `GroupTypeSlugDto`), `pool_model` (default `UNKNOWN`, reuses `SavingsMechanismDto`), `contribution_model` (default `UNKNOWN`), `shareout_formula`, `payout_order_method`, `share_value`, `contribution_amount`, `social_fund_enabled`, `cycle_length_months`, `loan_multiplier`, `interest_rate`, `fine_amount` | 3 enum fields default `UNKNOWN`; remaining 9 non-null required. **snake_case** — raw `group_type_config` Fineract datatable row for THIS group (Hard Rule 5) | field of `GroupDetailDto.typeConfig`; naming-collision with `GroupTypeConfigDto` — see note below |
| `GroupContributionModelDto` | enum `@SerialName`: `FIXED_AMOUNT`, `SHARE_BASED_VARIABLE`, `FIXED_NEGOTIATED`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback; identical value-set to `ContributionModelDto` (group-create) but declared independently — see note below | field of `GroupInstanceConfigDto.contribution_model` |
| `ViewerRoleInfoDto` | `role` (default `UNKNOWN`, reuses `ViewerRoleDto`), `memberId` | `role` defaults `UNKNOWN`; `memberId` required | `GET /companion/groups/{groupId}/my-role` (`get_viewer_role`) |
| `GroupCorpusDto` | `currentBalance`, `openingBalance`, `totalContributionsThisCycle`, `totalLoansOutstanding`, `lastUpdated`, `rotationPosition` (nullable, default `null`), `nextRecipientName` (nullable, default `null`), `nextRecipientPosition` (nullable, default `null`) | 3 nullable ROTATING_PAYOUT-only fields default `null`; rest required | `GET /companion/groups/{groupId}/corpus` (`get_group_corpus`) |
| `ActivityItemDto` | `id`, `type` (default `UNKNOWN`), `description`, `amount` (nullable, default `null`), `date`, `memberName` (nullable, default `null`) | `amount`/`memberName` nullable; `type` defaults `UNKNOWN`; rest required | field of `GroupAccountsDto.recentActivity` (last 10) |
| `ActivityTypeDto` | enum `@SerialName`: `MEETING`, `DEPOSIT`, `LOAN`, `PENALTY`, `SHARE_OUT`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `ActivityItemDto.type` |
| `GroupAccountsDto` | `savingsBalance`, `loansOutstanding`, `activeLoanCount`, `shareOutProjection` (nullable, default `null`), `recentActivity` (default `[]`) | `shareOutProjection` nullable (ACCUMULATING only); `recentActivity` defaults empty; rest required | `GET /companion/groups/{groupId}/accounts` (`get_group_accounts`) |
| `GroupConfigDto` | `shareValue`/`shareMin`/`shareMax`/`contributionAmount`/`loanMultiplier`/`interestRate`/`fineAmount`/`minimumDisbursementThreshold` (all nullable, default `null`), `cycleLengthMonths` (required) | 8 nullable fields; only `cycleLengthMonths` required | NOT returned by any endpoint — "constructed in GroupRepository from the GroupTypeConfig embedded in get_group response" per `api.yaml#dtos.GroupConfig`; kept `@Serializable` for round-trip test coverage |
| `MemberDto` | `id`, `fineractClientId`, `displayName`, `photoUri` (nullable, default `null`), `role` (default `UNKNOWN`), `savingsBalance`, `loanStatus` (default `UNKNOWN`) | `photoUri` nullable; 2 enum fields default `UNKNOWN`; rest required | `GET /groups/{groupId}/clients` — CANONICAL `Member`, reused by member-profile + member-add + member-invite; see registry-divergence note below |
| `MemberPageDto` | `totalFilteredRecords`, `pageItems` (default `[]`) | `pageItems` defaults empty; `totalFilteredRecords` required | offset-paginated envelope (`page_size=20`, stale-while-revalidate `ttl=120`) |
| `MemberRoleDto` | enum `@SerialName`: `CHAIRPERSON`, `TREASURER`, `SECRETARY`, `MEMBER`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback; 4 known values declared by member-list's own `api.yaml#dtos.MemberRole` — NOT identical to `GroupRoleDto` or `ViewerRoleDto`, see note below | field of `MemberDto.role` |
| `LoanStatusDto` | enum `@SerialName`: `ACTIVE`, `NONE`, `OVERDUE`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `MemberDto.loanStatus` |

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`;
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003);
`idea-layer/screens/group-list/{api.yaml,docs.yaml,data-flow.yaml}` (COMP-GRP-001);
`idea-layer/screens/join-with-code/{api.yaml,docs.yaml}` (COMP-DT-004 + COMP-GRP-003);
`idea-layer/screens/personal-dashboard/{api.yaml,docs.yaml}` (companion `GET
/companion/member/dashboard`, status: approved, approved 2026-07-17);
`idea-layer/screens/group-create/api.yaml` (COMP-GRP-001 `POST
/companion/groups` + `GET /offices`; no dedicated `idea-layer/dtos/{Dto}.yaml`
registry entry exists for this feature — `api.yaml` is the sole SoT, per PP-1
"registry, or its equivalent, wins");
`idea-layer/screens/group-dashboard/{api.yaml,ui.yaml,docs.yaml}` (COMP-GRP-001
4-way parallel fan-in: `get_group` + `get_viewer_role` + `get_group_corpus` +
`get_group_accounts`; no dedicated `idea-layer/dtos/{Dto}.yaml` registry entry
exists for this feature — `api.yaml` is the sole SoT);
`idea-layer/screens/member-list/api.yaml` (`GET /groups/{groupId}/clients`,
offset-paginated `limit`/`offset`, `page_size=20`, stale-while-revalidate
`ttl=120` + offline show-cached; `api.yaml#dtos.Member` is the SoT used here
— a DIFFERENT `idea-layer/dtos/MemberDto.yaml` registry entry also exists for
the same list endpoint, see divergence note below).

**`GroupDetailDto` vs `GroupDto` field-shape divergence (flagged for the
cross-feature repair station):** `get_group`'s response
(`id`/`fineractCenterId`/`name`/`cycleNumber`/`cycleLengthMonths`/
`meetingFrequency`/`memberCount`/`overdueLoansCount`/`status`/`typeConfig`)
genuinely diverges from the group-list `GroupDto` (COMP-GRP-001 `/mine`):
missing `groupType`/`viewerRole`/`lastMeetingDate`/`healthIndicator`/
`overdueRate` (all non-null required on `GroupDto`, no default), and carrying
4 fields `GroupDto` doesn't have. The generation brief instructed reusing
`GroupDto`/`Group` "do NOT duplicate", but doing so here would require
fabricating values with no wire source (Hard Rule 4) — `GroupDetailDto` was
introduced instead. Resolve at Station 3.

**`GroupInstanceConfigDto` naming collision (flagged for the cross-feature
repair station — same class of issue as the `SavingsTransactionDto` collision
below):** `idea-layer/screens/group-dashboard/api.yaml#dtos.GroupTypeConfig`
declares the snake_case per-group-instance shape embedded on
`GroupDetailDto.typeConfig` under the bare name `GroupTypeConfig` — the SAME
name already used by the camelCase COMP-DT-003 catalogue row
(`GroupTypeConfigDto` in `GroupTypeConfigDto.kt`). The two are NOT the same
wire shape (no field overlap beyond the group-type/pool-model axes) and use
DIFFERENT casing conventions (this one is raw-datatable snake_case; the
catalogue row is companion-bridge camelCase) — confirming they are genuinely
different endpoints' payloads, not a copy-paste duplicate. Named
`GroupInstanceConfigDto` here to avoid the Kotlin class-name clash while
flagging the collision for Station 3.

**`GroupContributionModelDto` — third contribution-mode-adjacent enum:**
its value-set (`FIXED_AMOUNT`/`SHARE_BASED_VARIABLE`/`FIXED_NEGOTIATED`) is
IDENTICAL to `ContributionModelDto`'s (group-create, see below), but the two
were declared independently on unrelated features' `api.yaml`s with no shared
source-of-truth cross-reference — NOT unified here; flagged for Station 3 to
evaluate reusing `ContributionModelDto` instead of a 4th sibling enum.

**Enum reuse (group-create, 2 of 5 typeConfig axes reuse existing wire
enums, no duplicates):** `CreateGroupTypeConfigDto.groupType` reuses the
EXISTING short-form `GroupTypeDto` (declared in `GroupDto.kt`, group-list
feature) — `api.yaml#dtos.GroupTypeConfig.group_type`'s declared value-set
(`VSLA`/`ROSCA`/.../`CBO`/`BURIAL`/`JLG`) is the SHORT form, matching
`GroupTypeDto` exactly, NOT the long-form `GroupTypeSlugDto` used by the
group-type-picker catalogue. `CreateGroupTypeConfigDto.poolModel` reuses the
EXISTING `SavingsMechanismDto` (declared in `GroupTypeConfigDto.kt`) — its
value-set (`ACCUMULATING`/`ROTATING_PAYOUT`/`NONE`) is identical to
`typeConfig.pool_model`'s declared values. `contributionModel` (NEW
`ContributionModelDto`), `shareoutFormula` (NEW `ShareoutFormulaDto`), and
`payoutOrderMethod` (NEW `PayoutOrderMethodDto`) were NOT reuse candidates:
`contribution_model`'s declared value-set (`FIXED_AMOUNT`/
`SHARE_BASED_VARIABLE`/`FIXED_NEGOTIATED`) differs from the existing
`ContributionModeDto` (`SHARE_BASED_VARIABLE`/`FIXED`/`MINIMAL`), and no
existing enum covers `shareout_formula` or `payout_order_method` at all.

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
`CreateGroupTypeConfigDto` (group-create) is the newest snake_case member of this family — it is
the raw `group_type_config` datatable payload nested inside `CreateGroupRequestDto`, whose OWN
top-level fields (`name`/`officeId`/.../`typeConfig`) stay camelCase like every other companion
request DTO.

**Registry gap (flagged for the cross-feature repair station):** `mark_invitation_accepted`'s
`rowId` path param is declared sourced from `validate_invite_token_response.id`, but neither
`api.yaml#api[0].response.fields` nor `#dtos.InvitationRow` declare an `id` field —
`InvitationRowDto` therefore does not carry one either (Hard Rule 4 forbids inventing an
undeclared field). The repository/use-case layer that wires `mark_invitation_accepted`'s `rowId`
will need this contract gap resolved upstream in the idea-layer `api.yaml`.

**Registry gap (`OfficeDto.externalId`, flagged for the cross-feature repair station):**
`idea-layer/screens/group-create/api.yaml#api[0] (get_offices)`'s response `items` schema
declares 4 fields (`id`, `name`, `nameDecorated`, `externalId`), but the abbreviated
`#dtos.Office` registry block only declares 3 (omits `externalId`). Modeled here as
`externalId: String? = null` (nullable/optional) rather than invented as non-null-required — the
two declarations should be reconciled upstream in `api.yaml`.

**`PayoutOrderMethodDto` value-set widened from task prose (flagged, resolved per PP-1):** the
generation brief's prose narrowed this enum to `FIXED_ORDER`/`LOTTERY`/`AUCTION`/`UNKNOWN` (4
entries), but `idea-layer/screens/group-create/api.yaml#dtos.GroupTypeConfig.payout_order_method`
(the actual SoT — no dedicated `idea-layer/dtos/{Dto}.yaml` registry exists for this feature)
declares 5 known values: `FIXED_ORDER | LOTTERY | AUCTION | NEED_BASED | NA`. Per PP-1 ("when the
registry [or its SoT-equivalent] and prose disagree, the registry wins"), all 5 were implemented
(6 total with `UNKNOWN`), not the narrower 3+1 from the prose summary.

Field-name casing precedent: `idea-layer/dtos/LoanDto.yaml` (camelCase wire
fields, e.g. `memberId`, `disbursedOn`) — the companion bridge returns
camelCase JSON, not raw Postgres snake_case, so every `@SerialName` here
matches `api.yaml#dtos` field names verbatim (no case translation).

**`MemberDto` vs `idea-layer/dtos/MemberDto.yaml` registry divergence
(flagged for the cross-feature repair station — same class of issue as the
`GroupDto.yaml` and `SavingsTransactionDto.yaml` divergences above):** the
registry entry (v2.0.0, `source.list_endpoint: GET /groups/{groupId}/clients`
— the SAME list endpoint declared here) declares a DIFFERENT `MemberDto`
shape: `id: Long` (not `String`), `roleInGroup: String` with LOWERCASE values
`organizer`/`treasurer`/`chairperson`/`secretary`/`member` (a raw
per-(member,group) role sourced from `dt_member_role`, includes `organizer`
which THIS DTO's `MemberRoleDto` does not), `status: String` (Fineract client
status: `active`/`inactive`/`pending`/`closed` — an entirely different concept
from `loanStatus`), `imageId: Long?` (a Fineract document ID, not a URL),
`groupId: Long?`, `savingsAccountId: Long?`, `joinedDate: String?` — and
carries NO `loanStatus`/`savingsBalance`/role-badge concept at all. Its
`used_by` list is `member-onboarding` (`search_clients`, `get_client`) +
`meeting-lifecycle` (`list_attendance`) — it does NOT list `member-list`.
`MemberDto` in this file was generated from member-list's OWN approved
`api.yaml#dtos.Member` instead — the feature's own contract explicitly
declares `role: MemberRole` + `loanStatus: LoanStatus`, exactly the
role-badge + loan-status shape this generation brief requested, and the
registry entry's absent `used_by` reference plus non-overlapping field set
indicate it describes a DIFFERENT consumer (the raw Fineract client
resource), not this companion list contract. Reconcile the two `MemberDto`
declarations at Station 3 — options include renaming the registry's richer
per-client resource to `ClientDto`/`ClientProfileDto`, or migrating
member-onboarding / meeting-lifecycle onto this companion shape if their
consumers turn out to be the same wire contract.

**`MemberRoleDto` — near-miss with two existing role enums, neither reused
(flagged for Station 3, same "near-miss, don't force it" precedent as
`ViewerRoleDto` vs `GroupRoleDto` above):** `GroupRoleDto`
(`ORGANIZER`/`MEMBER`/`TREASURER`/`SECRETARY`) is missing `CHAIRPERSON`;
`ViewerRoleDto` (`ORGANIZER`/`MEMBER`/`TREASURER`/`CHAIRPERSON`/`SECRETARY`)
is a strict superset that additionally carries `ORGANIZER`, which
member-list's own `api.yaml#dtos.MemberRole` does not declare. Neither is an
exact value-set match, so a new `MemberRoleDto` was introduced rather than
force-reusing either.

Domain counterparts + field mapping: see `core/model/API.md`. DTO↔domain
mappers: `core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/mapper/LoginSignupMappers.kt`,
`GroupTypeConfigMappers.kt`, `GroupMappers.kt`, `JoinWithCodeMappers.kt`,
`MemberDashboardMappers.kt`, `SavingsTransactionMappers.kt`,
`GroupCreateMappers.kt`, `GroupDashboardMappers.kt`, `MemberMappers.kt`.

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
