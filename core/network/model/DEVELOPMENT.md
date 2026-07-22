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
  `core/data` group-create repository (offices list + submit)

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
`MemberDashboardDtoTest.kt`, `SavingsTransactionDtoTest.kt`, and
`GroupCreateDtoTest.kt` — construction, serialization round-trip,
default-value, and equality tests per DTO, plus a T7/EC30 cross-version
fixture proving an old client tolerates a server-added field + a server-added
enum value without crashing.
`JoinWithCodeDtoTest.kt` additionally covers `InvitationRowDto.acceptedAt`
nullability (both the unused-code `null` case and the already-used
non-null case). `MemberDashboardDtoTest.kt` additionally covers the
mutually-exclusive nullable ACCUMULATING (`shareOutProjection`) vs
ROTATING_PAYOUT (`rotationPosition` + `nextRecipientEta`) field groups and
their omitted-from-payload default-null behavior. `GroupCreateDtoTest.kt`
additionally covers `OfficeDto.externalId`'s absent-from-payload default-null
behavior and every `ContributionModelDto`/`ShareoutFormulaDto`/
`PayoutOrderMethodDto` known value + `UNKNOWN` fallback.

## 8. Observability

Never log `SelfRegisterRequestDto.password`, `LoginRequestDto.password`, or
`AuthResponseDto.sessionToken` — all carry sensitive auth material.
`GroupTypeConfigDto` and `GroupDto` carry no sensitive fields.
`InvitationRowDto.invitedEmailPhone` carries PII (email/phone) — avoid logging
it; other join-with-code DTOs carry no sensitive fields.
`MemberDashboardResponseDto` / `GroupSummaryDto` / `SavingsTransactionDto`
carry no sensitive fields (balances only; no PII). `CreateGroupRequestDto` /
`CreateGroupTypeConfigDto` / `CreateGroupResponseDto` / `OfficeDto` carry no
sensitive fields.

## 9. Evolution

Bump the affected DTO's `SCHEMA_VERSION` when its shape changes; add new enum
values above `UNKNOWN` (never remove existing entries) to keep old clients
decoding safely. Before generating `personal-savings` or `savings-dashboard`
DTOs, resolve the `SavingsTransactionDto` naming collision flagged in
`## 4. Boundaries` and `## dtos` (API.md) — either rename the richer
per-account ledger row to `SavingsLedgerEntryDto` or migrate the consuming
feature onto this companion shape. Before generating a group-EDIT feature that
also submits `typeConfig`, reuse `CreateGroupTypeConfigDto` / its mappers
rather than introducing a second edit-time payload shape.
<!-- kmp-dto-gen:END -->
