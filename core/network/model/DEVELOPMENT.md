<!-- generated-by: kmp-dto-gen -->
<!-- kmp-dto-gen:BEGIN -->
# core/network/model — DEVELOPMENT.md

> Docs-only anchor per `RULE-FEATURE-DEVELOPMENT-MD-001`. The DTO source files
> physically live in the `core/network` Gradle module, under the
> `core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/model/`
> package (per `core/registries/KMP_MODULE_PLACEMENT.yaml` — `Dto → core/network`,
> package `core/network/model`). This directory carries only the docs surface.

## 1. Module Identity

`core/network/model` is the **wire/DTO layer** — every class here is
`@Serializable` with `@SerialName` on every field, matching the companion
bridge's actual JSON casing exactly (see `## 4. Boundaries`). No business
logic, no domain field names.

## 2. Public API

| DTO | File | Kind |
|---|---|---|
| `SelfRegisterRequestDto` | `LoginSignupDto.kt` | `@Serializable` request (COMP-AUTH-001) |
| `LoginRequestDto` | `LoginSignupDto.kt` | `@Serializable` request (COMP-AUTH-002) |
| `AuthResponseDto` | `LoginSignupDto.kt` | `@Serializable` response (COMP-AUTH-001/002) |
| `UserProfileDto` | `LoginSignupDto.kt` | `@Serializable` response (COMP-AUTH-003) |
| `GroupMembershipDto` | `LoginSignupDto.kt` | `@Serializable` nested response DTO |
| `GroupRoleDto` | `LoginSignupDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `GroupTypeConfigDto` | `GroupTypeConfigDto.kt` | `@Serializable` response row (COMP-DT-003) |
| `GroupTypeSlugDto` | `GroupTypeConfigDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `SavingsMechanismDto` | `GroupTypeConfigDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `ContributionModeDto` | `GroupTypeConfigDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `GroupDto` | `GroupDto.kt` | `@Serializable` response row (COMP-GRP-001) — canonical `Group` |
| `GroupPageDto` | `GroupDto.kt` | `@Serializable` offset-paginated envelope |
| `GroupTypeDto` | `GroupDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `ViewerRoleDto` | `GroupDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `HealthIndicatorDto` | `GroupDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `InvitationRowDto` | `JoinWithCodeDto.kt` | `@Serializable` response row (COMP-DT-004), snake_case wire columns |
| `GroupPreviewDto` | `JoinWithCodeDto.kt` | `@Serializable` response (companion group lookup) |
| `AssociateClientsRequestDto` | `JoinWithCodeDto.kt` | `@Serializable` request (COMP-GRP-003) |
| `AssociateClientsResponseDto` | `JoinWithCodeDto.kt` | `@Serializable` response (COMP-GRP-003) |
| `MarkAcceptedRequestDto` | `JoinWithCodeDto.kt` | `@Serializable` request (COMP-DT-004 `PUT`), snake_case |
| `MarkAcceptedResponseDto` | `JoinWithCodeDto.kt` | `@Serializable` response (COMP-DT-004 `PUT`) |
| `MarkAcceptedChangesDto` | `JoinWithCodeDto.kt` | `@Serializable` nested response DTO, snake_case |
| `MemberDashboardResponseDto` | `MemberDashboardDto.kt` | `@Serializable` response (companion `GET /companion/member/dashboard`) |
| `GroupSummaryDto` | `MemberDashboardDto.kt` | `@Serializable` nested response DTO; reuses `SavingsMechanismDto` |
| `SavingsTransactionDto` | `SavingsTransactionDto.kt` | `@Serializable` CANONICAL compact recent-activity row — see `## 4. Boundaries` naming-collision note |
| `TransactionTypeDto` | `SavingsTransactionDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `CreateGroupRequestDto` | `GroupCreateDto.kt` | `@Serializable` request (COMP-GRP-001 `POST /companion/groups`) |
| `CreateGroupTypeConfigDto` | `GroupCreateDto.kt` | `@Serializable` nested request DTO — raw `group_type_config` datatable payload, snake_case |
| `CreateGroupResponseDto` | `GroupCreateDto.kt` | `@Serializable` response (COMP-GRP-001) |
| `OfficeDto` | `GroupCreateDto.kt` | `@Serializable` response row (`GET /offices`), office dropdown |
| `ContributionModelDto` | `GroupCreateDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) — distinct from `ContributionModeDto` |
| `ShareoutFormulaDto` | `GroupCreateDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `PayoutOrderMethodDto` | `GroupCreateDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `GroupDashboardResponseDto` | `GroupDashboardDto.kt` | `@Serializable` composite (COMP-GRP-001 4-way parallel fan-in), NOT returned by a single endpoint |
| `GroupDetailDto` | `GroupDashboardDto.kt` | `@Serializable` response row (`get_group`) — NOT the same shape as `GroupDto` |
| `GroupInstanceConfigDto` | `GroupDashboardDto.kt` | `@Serializable` nested response DTO, snake_case wire columns — naming-collision with `GroupTypeConfigDto` |
| `GroupContributionModelDto` | `GroupDashboardDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `ViewerRoleInfoDto` | `GroupDashboardDto.kt` | `@Serializable` response (`get_viewer_role`); reuses `ViewerRoleDto` |
| `GroupCorpusDto` | `GroupDashboardDto.kt` | `@Serializable` response (`get_group_corpus`) |
| `ActivityItemDto` | `GroupDashboardDto.kt` | `@Serializable` nested response DTO |
| `ActivityTypeDto` | `GroupDashboardDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `GroupAccountsDto` | `GroupDashboardDto.kt` | `@Serializable` response (`get_group_accounts`) |
| `GroupConfigDto` | `GroupDashboardDto.kt` | `@Serializable` client-constructed shape, not returned by any endpoint |
| `MemberDto` | `MemberDto.kt` | `@Serializable` response row (`GET /groups/{groupId}/clients`) — canonical `Member`; see `## 4. Boundaries` registry-divergence note |
| `MemberPageDto` | `MemberDto.kt` | `@Serializable` offset-paginated envelope |
| `MemberRoleDto` | `MemberDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `LoanStatusDto` | `MemberDto.kt` | `@Serializable` enum, `UNKNOWN` fallback (T7/EC30) |
| `MemberProfileDto` | `MemberProfileDto.kt` | `@Serializable` response row (`get_client`) — NOT the same shape as `MemberDto` |
| `FineractStatusDto` | `MemberProfileDto.kt` | `@Serializable` shared nested `{id, value}` status pair |
| `MemberAccountsDto` | `MemberProfileDto.kt` | `@Serializable` response (`get_client_accounts`) — literal raw arrays, NOT the aggregated domain shape |
| `MemberSavingsAccountDto` | `MemberProfileDto.kt` | `@Serializable` nested response DTO |
| `MemberLoanAccountDto` | `MemberProfileDto.kt` | `@Serializable` nested response DTO |
| `MemberLoanAccountSummaryDto` | `MemberProfileDto.kt` | `@Serializable` nested response DTO |
| `MemberRoleInfoDto` | `MemberProfileDto.kt` | `@Serializable` response row (`get_member_role`); reuses `MemberRoleDto` |
| `UpdateMemberRoleRequestDto` | `MemberProfileDto.kt` | `@Serializable` request (`update_member_role` `PUT`); reuses `MemberRoleDto` |
| `UpdateMemberRoleResponseDto` | `MemberProfileDto.kt` | `@Serializable` response (`update_member_role` `PUT`) |

## 3. Consumers

- Ktor Services (`core/network` — the companion auth service + companion
  datatables service + companion groups service + companion member-dashboard
  service built on these DTOs)
- Repository mappers (`core/network/mapper/LoginSignupMappers.kt` → `core/data`
  `AuthRepositoryImpl`; `core/network/mapper/GroupTypeConfigMappers.kt` →
  `core/data` `GroupTypeConfigRepositoryImpl`; `core/network/mapper/GroupMappers.kt`
  → `core/data` `GroupRepositoryImpl`; `core/network/mapper/JoinWithCodeMappers.kt`
  → `core/data` invitation/join repository; `core/network/mapper/MemberDashboardMappers.kt`
  + `core/network/mapper/SavingsTransactionMappers.kt` → `core/data`
  `MemberDashboardRepository`; `core/network/mapper/GroupCreateMappers.kt` →
  `core/data` group-create repository (offices list + submit);
  `core/network/mapper/GroupDashboardMappers.kt` → `core/data`
  `GroupDashboardRepository` (COMP-GRP-001 4-way parallel fan-in);
  `core/network/mapper/MemberMappers.kt` → `core/data` `MemberRepository`
  (`GET /groups/{groupId}/clients`); `core/network/mapper/MemberProfileMappers.kt`
  → `core/data` `MemberRepository` (`get_client` + `get_client_accounts` +
  `get_member_role`, DTO -> domain; `update_member_role`, domain -> DTO)

## 4. Boundaries

- Every DTO field carries `@SerialName` matching the companion bridge's actual
  JSON key casing (camelCase — e.g. `emailPhone`, `tokenExpiresAt`,
  `groupMemberships` — confirmed against the existing `idea-layer/dtos/LoanDto.yaml`
  registry precedent + `api.yaml#dtos`, both of which declare camelCase field
  names; this is a Fineract/companion-bridge convention, not a raw Postgres
  snake_case table).
- No domain field names, no business logic, no navigation/state concerns.
- Every DTO carries `SCHEMA_VERSION` (companion `const val`); every enum
  carries an `UNKNOWN` `@SerialName` fallback entry — both per T7/EC30
  cross-version safety (a server-added field/enum value must never crash a
  staggered old client).
- **Naming-collision note (`SavingsTransactionDto`, flagged for the
  cross-feature repair station):** `idea-layer/dtos/SavingsTransactionDto.yaml`
  (registry) AND `idea-layer/screens/personal-savings/api.yaml#dtos` each
  declare a DIFFERENT, richer Fineract-raw shape under the SAME DTO name than
  the compact companion shape emitted here from `personal-dashboard`'s own
  approved `api.yaml`. See the full note in `SavingsTransactionDto.kt` kdoc and
  `## dtos` below — this MUST be resolved before `personal-savings` DTOs are
  generated (class-name collision, not just a field mismatch).
- **`CreateGroupTypeConfigDto` is snake_case** (raw `group_type_config`
  datatable columns, matching `InvitationRowDto`'s precedent) — every OTHER
  DTO in `GroupCreateDto.kt` (`CreateGroupRequestDto`'s top-level fields,
  `CreateGroupResponseDto`, `OfficeDto`) is companion-bridge camelCase as
  usual. See `## dtos` (API.md) for the enum reuse-vs-new-enum rationale
  (`groupType`/`poolModel` reuse existing wire enums; `contributionModel`/
  `shareoutFormula`/`payoutOrderMethod` are new, feature-local enums).
- **`OfficeDto.externalId` nullability gap** (flagged for the cross-feature
  repair station): `GET /offices`' operation response schema declares
  `externalId`, but the abbreviated `idea-layer/screens/group-create/api.yaml#dtos.Office`
  registry block omits it. Modeled here as nullable/optional rather than
  invented as non-null-required (Hard Rule 4).
- **`GroupDetailDto` is NOT `GroupDto`** (flagged for the cross-feature repair
  station): `get_group`'s response genuinely diverges from the group-list
  `GroupDto` wire shape — see the full note in `## dtos` (API.md).
- **`GroupInstanceConfigDto` is snake_case** (raw per-group `group_type_config`
  datatable row, matching `InvitationRowDto`/`CreateGroupTypeConfigDto`'s
  precedent) and shares its bare `GroupTypeConfig` source name with the
  UNRELATED camelCase catalogue `GroupTypeConfigDto` — flagged for Station 3,
  full note in `## dtos` (API.md). Every OTHER DTO in `GroupDashboardDto.kt`
  is companion-bridge camelCase as usual.
- **`MemberDto` vs `idea-layer/dtos/MemberDto.yaml` registry divergence**
  (flagged for the cross-feature repair station): the registry (v2.0.0)
  declares a DIFFERENT shape for the SAME `GET /groups/{groupId}/clients`
  list endpoint and does not list `member-list` as a consumer. `MemberDto`
  here was generated from member-list's own approved `api.yaml#dtos.Member`
  instead — full note in `## dtos` (API.md).
- **`MemberProfileDto` is NOT `MemberDto`** (flagged for the cross-feature
  repair station): `get_client`'s response genuinely diverges from the
  member-list `MemberDto` wire shape (no `role`/`savingsBalance`/`loanStatus`;
  carries `firstname`/`lastname`/`mobileNo`/`imagePresent`/`status`/
  `activationDate`/`officeId` instead) — see the full note in `## dtos`
  (API.md). `firstname`/`lastname` (lowercase `n`) is genuine Fineract API
  casing, matched verbatim per Hard Rule 5.
- **`MemberAccountsDto` is the LITERAL raw response, not the aggregated
  domain shape** (flagged for the cross-feature repair station): `api.yaml`
  declares an aggregated `dtos.MemberAccounts` (`savingsBalance`/
  `savingsHistory`/`activeLoan`), but `get_client_accounts` actually returns
  raw `savingsAccounts[]`/`loanAccounts[]` arrays. `MemberAccountsDto` mirrors
  the literal operation response; the aggregation happens in
  `MemberProfileMappers.kt` on the way to the domain `MemberAccounts` — see
  `## dtos` (API.md).

## 5. Data

| DTO | Field | `@SerialName` | Type | Default |
|---|---|---|---|---|
| `SelfRegisterRequestDto` | `name` | `name` | `String` | — |
| `SelfRegisterRequestDto` | `emailPhone` | `emailPhone` | `String` | — |
| `SelfRegisterRequestDto` | `password` | `password` | `String` | — |
| `LoginRequestDto` | `emailPhone` | `emailPhone` | `String` | — |
| `LoginRequestDto` | `password` | `password` | `String` | — |
| `AuthResponseDto` | `userId` | `userId` | `String` | — |
| `AuthResponseDto` | `sessionToken` | `sessionToken` | `String` | — |
| `AuthResponseDto` | `tokenExpiresAt` | `tokenExpiresAt` | `String` (ISO-8601) | — |
| `AuthResponseDto` | `groupMemberships` | `groupMemberships` | `List<GroupMembershipDto>` | `emptyList()` |
| `UserProfileDto` | `userId` | `userId` | `String` | — |
| `UserProfileDto` | `name` | `name` | `String` | — |
| `UserProfileDto` | `emailPhone` | `emailPhone` | `String` | — |
| `UserProfileDto` | `groupMemberships` | `groupMemberships` | `List<GroupMembershipDto>` | `emptyList()` |
| `GroupMembershipDto` | `groupId` | `groupId` | `String` | — |
| `GroupMembershipDto` | `groupName` | `groupName` | `String` | — |
| `GroupMembershipDto` | `role` | `role` | `GroupRoleDto` | `GroupRoleDto.UNKNOWN` |
| `GroupMembershipDto` | `joinedAt` | `joinedAt` | `String` (ISO-8601) | — |
| `GroupTypeConfigDto` | `typeSlug` | `typeSlug` | `GroupTypeSlugDto` | `GroupTypeSlugDto.UNKNOWN` |
| `GroupTypeConfigDto` | `displayName` | `displayName` | `String` | — |
| `GroupTypeConfigDto` | `tagline` | `tagline` | `String` | — |
| `GroupTypeConfigDto` | `savingsMechanism` | `savingsMechanism` | `SavingsMechanismDto` | `SavingsMechanismDto.UNKNOWN` |
| `GroupTypeConfigDto` | `contributionMode` | `contributionMode` | `ContributionModeDto` | `ContributionModeDto.UNKNOWN` |
| `GroupTypeConfigDto` | `lendingEnabled` | `lendingEnabled` | `Boolean` | — |
| `GroupTypeConfigDto` | `hasSocialFund` | `hasSocialFund` | `Boolean` | — |
| `GroupTypeConfigDto` | `hasBankLinkage` | `hasBankLinkage` | `Boolean` | — |
| `GroupTypeConfigDto` | `welfareOnlyMode` | `welfareOnlyMode` | `Boolean` | — |
| `GroupTypeConfigDto` | `formallyRegistered` | `formallyRegistered` | `Boolean` | — |
| `GroupTypeConfigDto` | `defaultLoanMultiplier` | `defaultLoanMultiplier` | `Double` | — |
| `GroupTypeConfigDto` | `defaultInterestRatePct` | `defaultInterestRatePct` | `Double` | — |
| `GroupTypeConfigDto` | `defaultCycleLengthMonths` | `defaultCycleLengthMonths` | `Int` | — |
| `GroupTypeConfigDto` | `maxMembers` | `maxMembers` | `Int` | — |
| `GroupTypeConfigDto` | `minMembers` | `minMembers` | `Int` | — |
| `GroupDto` | `id` | `id` | `String` | — |
| `GroupDto` | `name` | `name` | `String` | — |
| `GroupDto` | `groupType` | `groupType` | `GroupTypeDto` | `GroupTypeDto.UNKNOWN` |
| `GroupDto` | `viewerRole` | `viewerRole` | `ViewerRoleDto` | `ViewerRoleDto.UNKNOWN` |
| `GroupDto` | `cycleNumber` | `cycleNumber` | `Int` | — |
| `GroupDto` | `memberCount` | `memberCount` | `Int` | — |
| `GroupDto` | `lastMeetingDate` | `lastMeetingDate` | `String` (ISO date) | — |
| `GroupDto` | `healthIndicator` | `healthIndicator` | `HealthIndicatorDto` | `HealthIndicatorDto.UNKNOWN` |
| `GroupDto` | `overdueRate` | `overdueRate` | `Double` | — |
| `GroupDto` | `status` | `status` | `String` | — |
| `GroupDto` | `fineractCenterId` | `fineractCenterId` | `Long` | — |
| `GroupPageDto` | `totalFilteredRecords` | `totalFilteredRecords` | `Int` | — |
| `GroupPageDto` | `pageItems` | `pageItems` | `List<GroupDto>` | `emptyList()` |
| `InvitationRowDto` | `token` | `token` | `String` | — |
| `InvitationRowDto` | `groupId` | `group_id` | `Long` | — |
| `InvitationRowDto` | `inviterClientId` | `inviter_client_id` | `Long` | — |
| `InvitationRowDto` | `invitedEmailPhone` | `invited_email_phone` | `String` | — |
| `InvitationRowDto` | `roleToAssign` | `role_to_assign` | `GroupRoleDto` | `GroupRoleDto.UNKNOWN` |
| `InvitationRowDto` | `expiresAt` | `expires_at` | `String` (ISO-8601) | — |
| `InvitationRowDto` | `acceptedAt` | `accepted_at` | `String?` (ISO-8601) | `null` |
| `GroupPreviewDto` | `groupId` | `groupId` | `Long` | — |
| `GroupPreviewDto` | `groupName` | `groupName` | `String` | — |
| `GroupPreviewDto` | `groupType` | `groupType` | `GroupTypeSlugDto` | `GroupTypeSlugDto.UNKNOWN` |
| `GroupPreviewDto` | `organizerName` | `organizerName` | `String` | — |
| `GroupPreviewDto` | `memberCount` | `memberCount` | `Int` | — |
| `GroupPreviewDto` | `officeId` | `officeId` | `Long` | — |
| `GroupPreviewDto` | `roleToAssign` | `roleToAssign` | `GroupRoleDto` | `GroupRoleDto.UNKNOWN` |
| `AssociateClientsRequestDto` | `clientIds` | `clientIds` | `List<Long>` | — |
| `AssociateClientsRequestDto` | `roleToAssign` | `roleToAssign` | `GroupRoleDto` | `GroupRoleDto.UNKNOWN` |
| `AssociateClientsResponseDto` | `resourceId` | `resourceId` | `Long` | — |
| `AssociateClientsResponseDto` | `groupId` | `groupId` | `Long` | — |
| `AssociateClientsResponseDto` | `clientIds` | `clientIds` | `List<Long>` | — |
| `MarkAcceptedRequestDto` | `acceptedAt` | `accepted_at` | `String` (ISO-8601) | — |
| `MarkAcceptedResponseDto` | `resourceId` | `resourceId` | `Long` | — |
| `MarkAcceptedResponseDto` | `changes` | `changes` | `MarkAcceptedChangesDto` | — |
| `MarkAcceptedChangesDto` | `acceptedAt` | `accepted_at` | `String` (ISO-8601) | — |
| `MemberDashboardResponseDto` | `memberName` | `memberName` | `String` | — |
| `MemberDashboardResponseDto` | `myGroups` | `myGroups` | `List<GroupSummaryDto>` | `emptyList()` |
| `MemberDashboardResponseDto` | `selectedGroup` | `selectedGroup` | `GroupSummaryDto` | — |
| `MemberDashboardResponseDto` | `poolModel` | `poolModel` | `SavingsMechanismDto` | `SavingsMechanismDto.UNKNOWN` |
| `MemberDashboardResponseDto` | `groupLinkedSavingsBalance` | `groupLinkedSavingsBalance` | `Double` | — |
| `MemberDashboardResponseDto` | `individualSavingsBalance` | `individualSavingsBalance` | `Double` | — |
| `MemberDashboardResponseDto` | `shareOutProjection` | `shareOutProjection` | `Double?` | `null` |
| `MemberDashboardResponseDto` | `rotationPosition` | `rotationPosition` | `Int?` | `null` |
| `MemberDashboardResponseDto` | `nextRecipientEta` | `nextRecipientEta` | `String?` | `null` |
| `MemberDashboardResponseDto` | `recentTransactions` | `recentTransactions` | `List<SavingsTransactionDto>` | `emptyList()` |
| `GroupSummaryDto` | `groupId` | `groupId` | `String` | — |
| `GroupSummaryDto` | `name` | `name` | `String` | — |
| `GroupSummaryDto` | `poolModel` | `poolModel` | `SavingsMechanismDto` | `SavingsMechanismDto.UNKNOWN` |
| `SavingsTransactionDto` | `id` | `id` | `String` | — |
| `SavingsTransactionDto` | `date` | `date` | `String` (ISO date) | — |
| `SavingsTransactionDto` | `type` | `type` | `TransactionTypeDto` | `TransactionTypeDto.UNKNOWN` |
| `SavingsTransactionDto` | `amount` | `amount` | `Double` | — |
| `CreateGroupRequestDto` | `name` | `name` | `String` | — |
| `CreateGroupRequestDto` | `officeId` | `officeId` | `Long` | — |
| `CreateGroupRequestDto` | `userId` | `userId` | `Long` | — |
| `CreateGroupRequestDto` | `currency` | `currency` | `String` | — |
| `CreateGroupRequestDto` | `meetingDay` | `meetingDay` | `String` | — |
| `CreateGroupRequestDto` | `meetingTime` | `meetingTime` | `String` | — |
| `CreateGroupRequestDto` | `typeConfig` | `typeConfig` | `CreateGroupTypeConfigDto` | — |
| `CreateGroupTypeConfigDto` | `groupType` | `group_type` | `GroupTypeDto` | `GroupTypeDto.UNKNOWN` |
| `CreateGroupTypeConfigDto` | `poolModel` | `pool_model` | `SavingsMechanismDto` | `SavingsMechanismDto.UNKNOWN` |
| `CreateGroupTypeConfigDto` | `contributionModel` | `contribution_model` | `ContributionModelDto` | `ContributionModelDto.UNKNOWN` |
| `CreateGroupTypeConfigDto` | `shareoutFormula` | `shareout_formula` | `ShareoutFormulaDto` | `ShareoutFormulaDto.UNKNOWN` |
| `CreateGroupTypeConfigDto` | `payoutOrderMethod` | `payout_order_method` | `PayoutOrderMethodDto` | `PayoutOrderMethodDto.UNKNOWN` |
| `CreateGroupTypeConfigDto` | `shareValue` | `share_value` | `Double` | — |
| `CreateGroupTypeConfigDto` | `contributionAmount` | `contribution_amount` | `Double` | — |
| `CreateGroupTypeConfigDto` | `socialFundEnabled` | `social_fund_enabled` | `Boolean` | — |
| `CreateGroupTypeConfigDto` | `socialFundPercent` | `social_fund_percent` | `Double` | — |
| `CreateGroupTypeConfigDto` | `cycleLengthMonths` | `cycle_length_months` | `Int` | — |
| `CreateGroupTypeConfigDto` | `loanMultiplier` | `loan_multiplier` | `Double` | — |
| `CreateGroupTypeConfigDto` | `interestRate` | `interest_rate` | `Double` | — |
| `CreateGroupTypeConfigDto` | `fineAmount` | `fine_amount` | `Double` | — |
| `CreateGroupTypeConfigDto` | `maxMembers` | `max_members` | `Int` | — |
| `CreateGroupResponseDto` | `groupId` | `groupId` | `String` | — |
| `CreateGroupResponseDto` | `fineractCenterId` | `fineractCenterId` | `Long` | — |
| `CreateGroupResponseDto` | `inviteCode` | `inviteCode` | `String` | — |
| `OfficeDto` | `id` | `id` | `Long` | — |
| `OfficeDto` | `name` | `name` | `String` | — |
| `OfficeDto` | `nameDecorated` | `nameDecorated` | `String` | — |
| `OfficeDto` | `externalId` | `externalId` | `String?` | `null` |
| `GroupDashboardResponseDto` | `group` | `group` | `GroupDetailDto` | — |
| `GroupDashboardResponseDto` | `viewerRole` | `viewerRole` | `ViewerRoleInfoDto` | — |
| `GroupDashboardResponseDto` | `corpus` | `corpus` | `GroupCorpusDto` | — |
| `GroupDashboardResponseDto` | `accounts` | `accounts` | `GroupAccountsDto` | — |
| `GroupDetailDto` | `id` | `id` | `String` | — |
| `GroupDetailDto` | `fineractCenterId` | `fineractCenterId` | `Long` | — |
| `GroupDetailDto` | `name` | `name` | `String` | — |
| `GroupDetailDto` | `cycleNumber` | `cycleNumber` | `Int` | — |
| `GroupDetailDto` | `cycleLengthMonths` | `cycleLengthMonths` | `Int` | — |
| `GroupDetailDto` | `meetingFrequency` | `meetingFrequency` | `String` | — |
| `GroupDetailDto` | `memberCount` | `memberCount` | `Int` | — |
| `GroupDetailDto` | `overdueLoansCount` | `overdueLoansCount` | `Int` | — |
| `GroupDetailDto` | `status` | `status` | `String` | — |
| `GroupDetailDto` | `typeConfig` | `typeConfig` | `GroupInstanceConfigDto` | — |
| `GroupInstanceConfigDto` | `groupType` | `group_type` | `GroupTypeSlugDto` | `GroupTypeSlugDto.UNKNOWN` |
| `GroupInstanceConfigDto` | `poolModel` | `pool_model` | `SavingsMechanismDto` | `SavingsMechanismDto.UNKNOWN` |
| `GroupInstanceConfigDto` | `contributionModel` | `contribution_model` | `GroupContributionModelDto` | `GroupContributionModelDto.UNKNOWN` |
| `GroupInstanceConfigDto` | `shareoutFormula` | `shareout_formula` | `String` | — |
| `GroupInstanceConfigDto` | `payoutOrderMethod` | `payout_order_method` | `String` | — |
| `GroupInstanceConfigDto` | `shareValue` | `share_value` | `Double` | — |
| `GroupInstanceConfigDto` | `contributionAmount` | `contribution_amount` | `Double` | — |
| `GroupInstanceConfigDto` | `socialFundEnabled` | `social_fund_enabled` | `Boolean` | — |
| `GroupInstanceConfigDto` | `cycleLengthMonths` | `cycle_length_months` | `Int` | — |
| `GroupInstanceConfigDto` | `loanMultiplier` | `loan_multiplier` | `Double` | — |
| `GroupInstanceConfigDto` | `interestRate` | `interest_rate` | `Double` | — |
| `GroupInstanceConfigDto` | `fineAmount` | `fine_amount` | `Double` | — |
| `ViewerRoleInfoDto` | `role` | `role` | `ViewerRoleDto` | `ViewerRoleDto.UNKNOWN` |
| `ViewerRoleInfoDto` | `memberId` | `memberId` | `Long` | — |
| `GroupCorpusDto` | `currentBalance` | `currentBalance` | `Double` | — |
| `GroupCorpusDto` | `openingBalance` | `openingBalance` | `Double` | — |
| `GroupCorpusDto` | `totalContributionsThisCycle` | `totalContributionsThisCycle` | `Double` | — |
| `GroupCorpusDto` | `totalLoansOutstanding` | `totalLoansOutstanding` | `Double` | — |
| `GroupCorpusDto` | `lastUpdated` | `lastUpdated` | `String` | — |
| `GroupCorpusDto` | `rotationPosition` | `rotationPosition` | `Int?` | `null` |
| `GroupCorpusDto` | `nextRecipientName` | `nextRecipientName` | `String?` | `null` |
| `GroupCorpusDto` | `nextRecipientPosition` | `nextRecipientPosition` | `Int?` | `null` |
| `ActivityItemDto` | `id` | `id` | `String` | — |
| `ActivityItemDto` | `type` | `type` | `ActivityTypeDto` | `ActivityTypeDto.UNKNOWN` |
| `ActivityItemDto` | `description` | `description` | `String` | — |
| `ActivityItemDto` | `amount` | `amount` | `Double?` | `null` |
| `ActivityItemDto` | `date` | `date` | `String` | — |
| `ActivityItemDto` | `memberName` | `memberName` | `String?` | `null` |
| `GroupAccountsDto` | `savingsBalance` | `savingsBalance` | `Double` | — |
| `GroupAccountsDto` | `loansOutstanding` | `loansOutstanding` | `Double` | — |
| `GroupAccountsDto` | `activeLoanCount` | `activeLoanCount` | `Int` | — |
| `GroupAccountsDto` | `shareOutProjection` | `shareOutProjection` | `Double?` | `null` |
| `GroupAccountsDto` | `recentActivity` | `recentActivity` | `List<ActivityItemDto>` | `emptyList()` |
| `GroupConfigDto` | `shareValue` | `shareValue` | `Double?` | `null` |
| `GroupConfigDto` | `shareMin` | `shareMin` | `Int?` | `null` |
| `GroupConfigDto` | `shareMax` | `shareMax` | `Int?` | `null` |
| `GroupConfigDto` | `contributionAmount` | `contributionAmount` | `Double?` | `null` |
| `GroupConfigDto` | `loanMultiplier` | `loanMultiplier` | `Double?` | `null` |
| `GroupConfigDto` | `interestRate` | `interestRate` | `Double?` | `null` |
| `GroupConfigDto` | `cycleLengthMonths` | `cycleLengthMonths` | `Int` | — |
| `GroupConfigDto` | `fineAmount` | `fineAmount` | `Double?` | `null` |
| `GroupConfigDto` | `minimumDisbursementThreshold` | `minimumDisbursementThreshold` | `Double?` | `null` |
| `MemberDto` | `id` | `id` | `String` | — |
| `MemberDto` | `fineractClientId` | `fineractClientId` | `Long` | — |
| `MemberDto` | `displayName` | `displayName` | `String` | — |
| `MemberDto` | `photoUri` | `photoUri` | `String?` | `null` |
| `MemberDto` | `role` | `role` | `MemberRoleDto` | `MemberRoleDto.UNKNOWN` |
| `MemberDto` | `savingsBalance` | `savingsBalance` | `Double` | — |
| `MemberDto` | `loanStatus` | `loanStatus` | `LoanStatusDto` | `LoanStatusDto.UNKNOWN` |
| `MemberPageDto` | `totalFilteredRecords` | `totalFilteredRecords` | `Int` | — |
| `MemberPageDto` | `pageItems` | `pageItems` | `List<MemberDto>` | `emptyList()` |
| `MemberProfileDto` | `id` | `id` | `Long` | — |
| `MemberProfileDto` | `displayName` | `displayName` | `String` | — |
| `MemberProfileDto` | `firstName` | `firstname` | `String` | — |
| `MemberProfileDto` | `lastName` | `lastname` | `String` | — |
| `MemberProfileDto` | `mobileNo` | `mobileNo` | `String` | — |
| `MemberProfileDto` | `imagePresent` | `imagePresent` | `Boolean` | — |
| `MemberProfileDto` | `status` | `status` | `FineractStatusDto` | — |
| `MemberProfileDto` | `activationDate` | `activationDate` | `String` | — |
| `MemberProfileDto` | `officeId` | `officeId` | `Long` | — |
| `FineractStatusDto` | `id` | `id` | `Int` | — |
| `FineractStatusDto` | `value` | `value` | `String` | — |
| `MemberAccountsDto` | `savingsAccounts` | `savingsAccounts` | `List<MemberSavingsAccountDto>` | `emptyList()` |
| `MemberAccountsDto` | `loanAccounts` | `loanAccounts` | `List<MemberLoanAccountDto>` | `emptyList()` |
| `MemberSavingsAccountDto` | `id` | `id` | `Long` | — |
| `MemberSavingsAccountDto` | `productName` | `productName` | `String` | — |
| `MemberSavingsAccountDto` | `accountNo` | `accountNo` | `String` | — |
| `MemberSavingsAccountDto` | `balance` | `balance` | `Double` | — |
| `MemberSavingsAccountDto` | `status` | `status` | `FineractStatusDto` | — |
| `MemberLoanAccountDto` | `id` | `id` | `Long` | — |
| `MemberLoanAccountDto` | `productName` | `productName` | `String` | — |
| `MemberLoanAccountDto` | `accountNo` | `accountNo` | `String` | — |
| `MemberLoanAccountDto` | `status` | `status` | `FineractStatusDto` | — |
| `MemberLoanAccountDto` | `summary` | `summary` | `MemberLoanAccountSummaryDto` | — |
| `MemberLoanAccountSummaryDto` | `principalDisbursed` | `principalDisbursed` | `Double` | — |
| `MemberLoanAccountSummaryDto` | `principalOutstanding` | `principalOutstanding` | `Double` | — |
| `MemberLoanAccountSummaryDto` | `totalOverdue` | `totalOverdue` | `Double` | — |
| `MemberRoleInfoDto` | `role` | `role` | `MemberRoleDto` | `MemberRoleDto.UNKNOWN` |
| `MemberRoleInfoDto` | `groupId` | `groupId` | `Long` | — |
| `MemberRoleInfoDto` | `assignedDate` | `assignedDate` | `String` | — |
| `UpdateMemberRoleRequestDto` | `role` | `role` | `MemberRoleDto` | `MemberRoleDto.UNKNOWN` |
| `UpdateMemberRoleRequestDto` | `groupId` | `groupId` | `Long` | — |
| `UpdateMemberRoleRequestDto` | `assignedDate` | `assignedDate` | `String` | — |
| `UpdateMemberRoleResponseDto` | `resourceId` | `resourceId` | `Long` | — |

## 6. Errors

Malformed JSON or an unrecognized required field raises
`kotlinx.serialization.SerializationException`, caught by the shared Ktor
error-mapper (`core-base/network`, consumed not modified). Unknown *optional*
fields and unknown enum values are tolerated (never thrown) via the shared
`Json { ignoreUnknownKeys = true; coerceInputValues = true }` config wired in
`NetworkModule` (emitted by `kmp-client-gen`).

## 7. Testing

`core/network/src/commonTest/.../model/LoginSignupDtoTest.kt`,
`GroupTypeConfigDtoTest.kt`, `GroupDtoTest.kt`, `JoinWithCodeDtoTest.kt`,
`MemberDashboardDtoTest.kt`, `SavingsTransactionDtoTest.kt`,
`GroupCreateDtoTest.kt`, and `GroupDashboardDtoTest.kt` — construction,
serialization round-trip, default-value, and equality tests per DTO, plus a
T7/EC30 cross-version fixture proving an old client tolerates a server-added
field + a server-added enum value without crashing.
`JoinWithCodeDtoTest.kt` additionally covers `InvitationRowDto.acceptedAt`
nullability (both the unused-code `null` case and the already-used
non-null case). `MemberDashboardDtoTest.kt` additionally covers the
mutually-exclusive nullable ACCUMULATING (`shareOutProjection`) vs
ROTATING_PAYOUT (`rotationPosition` + `nextRecipientEta`) field groups and
their omitted-from-payload default-null behavior. `GroupCreateDtoTest.kt`
additionally covers `OfficeDto.externalId`'s absent-from-payload default-null
behavior and every `ContributionModelDto`/`ShareoutFormulaDto`/
`PayoutOrderMethodDto` known value + `UNKNOWN` fallback. `GroupDashboardDtoTest.kt`
additionally covers `GroupInstanceConfigDto`'s snake_case `@SerialName` wire
casing, the mutually-exclusive nullable ACCUMULATING vs ROTATING_PAYOUT
`GroupCorpusDto` field groups, and the full composite's cross-version
tolerance (server-added top-level field, decoded without crashing).
`core/network/src/commonTest/.../model/MemberDtoTest.kt` covers `MemberDto` /
`MemberPageDto` construction, `photoUri`/`role`/`loanStatus` default-value
behavior, serialization round-trip, every `MemberRoleDto`/`LoanStatusDto`
known value + `UNKNOWN` fallback, and the T7/EC30 cross-version fixture
(server-added field + unknown role value, decoded without crashing).
`core/network/src/commonTest/.../model/MemberProfileDtoTest.kt` covers
`MemberProfileDto`'s literal Fineract `firstname`/`lastname` wire casing
(distinct from the idiomatic `firstName`/`lastName` Kotlin property names),
the nested `FineractStatusDto` shape, `MemberAccountsDto`'s raw
`savingsAccounts[]`/`loanAccounts[]` default-empty behavior,
`MemberRoleInfoDto`'s array-response decode + `UNKNOWN` role fallback, both
`UpdateMemberRoleRequestDto`/`ResponseDto`, every DTO's `SCHEMA_VERSION`, and
a T7/EC30 cross-version fixture (server-added field, decoded without
crashing).

## 8. Observability

Never log `SelfRegisterRequestDto.password`, `LoginRequestDto.password`, or
`AuthResponseDto.sessionToken` — all carry sensitive auth material.
`GroupTypeConfigDto` and `GroupDto` carry no sensitive fields.
`InvitationRowDto.invitedEmailPhone` carries PII (email/phone) — avoid logging
it; other join-with-code DTOs carry no sensitive fields.
`MemberDashboardResponseDto` / `GroupSummaryDto` / `SavingsTransactionDto`
carry no sensitive fields (balances only; no PII). `CreateGroupRequestDto` /
`CreateGroupTypeConfigDto` / `CreateGroupResponseDto` / `OfficeDto` carry no
sensitive fields. `GroupDashboardResponseDto` / `GroupDetailDto` /
`GroupInstanceConfigDto` / `ViewerRoleInfoDto` / `GroupCorpusDto` /
`ActivityItemDto` / `GroupAccountsDto` / `GroupConfigDto` carry no sensitive
fields (balances + group config only; no PII). `MemberDto` carries
`displayName` (display-name PII, same threat model as `ActivityItemDto.memberName`)
and `photoUri` (a CDN URL, not raw image bytes) — avoid bulk-logging the full
member-list page; `savingsBalance`/`role`/`loanStatus` are not sensitive.
`MemberProfileDto` carries `displayName`/`firstName`/`lastName` (PII display
names) and `mobileNo` (PII, a phone number) — avoid bulk-logging
member-profile identity payloads. `MemberAccountsDto` / `FineractStatusDto` /
`MemberSavingsAccountDto` / `MemberLoanAccountDto` /
`MemberLoanAccountSummaryDto` / `MemberRoleInfoDto` /
`UpdateMemberRoleRequestDto` / `UpdateMemberRoleResponseDto` carry no
sensitive fields (balances + role only; no PII).

## 9. Evolution

Bump the affected DTO's `SCHEMA_VERSION` when its shape changes; add new enum
values above `UNKNOWN` (never remove existing entries) to keep old clients
decoding safely. Before generating `personal-savings` or `savings-dashboard`
DTOs, resolve the `SavingsTransactionDto` naming collision flagged in
`## 4. Boundaries` and `## dtos` (API.md) — either rename the richer
per-account ledger row to `SavingsLedgerEntryDto` or migrate the consuming
feature onto this companion shape. Before generating a group-EDIT feature that
also submits `typeConfig`, reuse `CreateGroupTypeConfigDto` / its mappers
rather than introducing a second edit-time payload shape. Before generating
`group-edit` or any feature that also embeds a per-group `group_type_config`
datatable row, resolve the `GroupInstanceConfigDto`/`GroupTypeConfigDto`
naming collision flagged in `## 4. Boundaries` and `## dtos` (API.md).
Group-dashboard's DTOs live in `GroupDashboardDto.kt`; before generating a
feature that needs the fully-merged `GroupConfigDto` (catalogue defaults +
per-group overrides), resolve the repository-layer merge documented on
`GroupConfigDto`'s kdoc rather than duplicating the derivation logic in a new
DTO. Member-list's DTOs live in `MemberDto.kt` (`MemberDto`, `MemberPageDto`,
`MemberRoleDto`, `LoanStatusDto`); reuse `MemberRoleDto` outright for
role-badge concepts (member-profile's `MemberRoleInfoDto` /
`UpdateMemberRoleRequestDto` do exactly this). Before extending `MemberDto`
itself, resolve the `idea-layer/dtos/MemberDto.yaml` registry-divergence
flagged in `## 4. Boundaries` and `## dtos` (API.md). **Member-profile's own
DTOs live in `MemberProfileDto.kt`** (`MemberProfileDto`, `FineractStatusDto`,
`MemberAccountsDto`, `MemberSavingsAccountDto`, `MemberLoanAccountDto`,
`MemberLoanAccountSummaryDto`, `MemberRoleInfoDto`,
`UpdateMemberRoleRequestDto`, `UpdateMemberRoleResponseDto`) —
`MemberProfileDto` was introduced rather than reusing `MemberDto` (the
generation brief's original instruction) because `get_client`'s response
genuinely diverges from `MemberDto`'s wire shape; see the field-shape-
divergence note in `## 4. Boundaries` and `## dtos` (API.md). Before
generating member-add / member-invite, check whether their identity needs
match `MemberProfileDto` (this feature) or `MemberDto` (member-list) rather
than introducing a third identity DTO shape.
<!-- kmp-dto-gen:END -->
