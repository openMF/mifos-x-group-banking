<!-- generated-by: kmp-dto-gen -->
<!-- kmp-dto-gen:BEGIN -->
# core/model — DEVELOPMENT.md

## 1. Module Identity

`core/model` holds **pure-Kotlin domain models** — no `@Serializable`, no
`@SerialName`, no wire-format or platform concerns. Every type here is a
business shape consumed by UseCases, Repositories, and ViewModels. Wire
representations of the same concepts live in `core/network/model` (see that
module's `DEVELOPMENT.md`) and are converted at the network boundary via
mappers in `core/network/mapper`.

## 2. Public API

| Model | File | Kind |
|---|---|---|
| `LoginCredentials` | `AuthModels.kt` | data class — login-signup input |
| `SelfRegistration` | `AuthModels.kt` | data class — login-signup input |
| `AuthSession` | `AuthModels.kt` | data class — login-signup auth-result value object |
| `UserProfile` | `AuthModels.kt` | data class — companion_me domain model |
| `GroupMembership` | `AuthModels.kt` | data class |
| `GroupRole` | `AuthModels.kt` | enum (`ORGANIZER`, `MEMBER`, `TREASURER`, `SECRETARY`, `UNKNOWN`) |
| `AuthState` | `user/AuthState.kt` | sealed class (pre-existing template model) |
| `UserData` | `user/UserData.kt` | data class (pre-existing template model) |
| `GroupTypeConfig` | `GroupTypeConfig.kt` | data class — COMP-DT-003 seeded group-type catalogue row (group-type-picker) |
| `GroupTypeSlug` | `GroupTypeConfig.kt` | enum (`VSLA`, `ROSCA`, `ASCA`, `SILC`, `SHG`, `SACCO`, `CBO_VILLAGE_BANK`, `BURIAL_WELFARE`, `JLG`, `UNKNOWN`) |
| `SavingsMechanism` | `GroupTypeConfig.kt` | enum (`ACCUMULATING`, `ROTATING_PAYOUT`, `NONE`, `UNKNOWN`) |
| `ContributionMode` | `GroupTypeConfig.kt` | enum (`SHARE_BASED_VARIABLE`, `FIXED`, `MINIMAL`, `UNKNOWN`) |
| `Group` | `Group.kt` | data class — COMP-GRP-001 canonical group-list row (also used by group-dashboard + member features) |
| `GroupPage` | `Group.kt` | data class — offset-paginated envelope |
| `ViewerRole` | `Group.kt` | enum (`ORGANIZER`, `MEMBER`, `TREASURER`, `CHAIRPERSON`, `SECRETARY`, `UNKNOWN`) |
| `HealthIndicator` | `Group.kt` | enum (`GREEN`, `AMBER`, `RED`, `UNKNOWN`) + `fromOverdueRate(rate: Double)` companion factory |
| `Invitation` | `Invitation.kt` | data class — COMP-DT-004 invitations-datatable row; `isAlreadyUsed` property + `isExpired(now)` function |
| `GroupPreview` | `Invitation.kt` | data class — join-with-code group preview card; reuses `GroupTypeSlug` + `GroupRole` |
| `JoinGroupRequest` | `Invitation.kt` | data class — COMP-GRP-003 associate-clients input |
| `JoinGroupResult` | `Invitation.kt` | data class — COMP-GRP-003 associate-clients result |
| `InvitationAcceptance` | `Invitation.kt` | data class — COMP-DT-004 mark-accepted input |
| `InvitationAcceptanceResult` | `Invitation.kt` | data class — COMP-DT-004 mark-accepted result |
| `MemberDashboard` | `MemberDashboard.kt` | data class — personal-dashboard member home screen (companion `GET /companion/member/dashboard`) |
| `GroupSummary` | `MemberDashboard.kt` | data class — lightweight per-group summary row; reuses `SavingsMechanism` |
| `SavingsTransaction` | `SavingsTransaction.kt` | data class — CANONICAL compact recent-activity row (personal-dashboard); see registry/naming-collision note in `core/network/model/API.md` |
| `TransactionType` | `SavingsTransaction.kt` | enum (`DEPOSIT`, `WITHDRAWAL`, `UNKNOWN`) |
| `CreateGroupRequest` | `GroupCreate.kt` | data class — group-create wizard submission payload (COMP-GRP-001) |
| `CreateGroupTypeConfig` | `GroupCreate.kt` | data class — type-adaptive rule set; reuses `GroupTypeSlug` + `SavingsMechanism` |
| `GroupCreationResult` | `GroupCreate.kt` | data class — group-create success result (groupId + inviteCode) |
| `Office` | `GroupCreate.kt` | data class — office dropdown row; `externalId` nullable |
| `ContributionModel` | `GroupCreate.kt` | enum (`FIXED_AMOUNT`, `SHARE_BASED_VARIABLE`, `FIXED_NEGOTIATED`, `UNKNOWN`) — distinct from `ContributionMode` |
| `ShareoutFormula` | `GroupCreate.kt` | enum (`NONE`, `PRORATA_SHARES`, `PRORATA_SAVINGS`, `EQUAL`, `INVESTMENT_PROPORTIONAL`, `UNKNOWN`) |
| `PayoutOrderMethod` | `GroupCreate.kt` | enum (`FIXED_ORDER`, `LOTTERY`, `AUCTION`, `NEED_BASED`, `NA`, `UNKNOWN`) |
| `GroupDashboard` | `GroupDashboard.kt` | data class — group-dashboard composite (COMP-GRP-001 4-way parallel fan-in) |
| `GroupDetail` | `GroupDashboard.kt` | data class — `get_group` identity/header shape; deliberately NOT `Group` (field-shape divergence, see `core/network/model/API.md`) |
| `GroupInstanceConfig` | `GroupDashboard.kt` | data class — per-group configured instance; reuses `GroupTypeSlug` + `SavingsMechanism`; naming-collision with catalogue `GroupTypeConfig` (see `core/network/model/API.md`) |
| `GroupContributionModel` | `GroupDashboard.kt` | enum (`FIXED_AMOUNT`, `SHARE_BASED_VARIABLE`, `FIXED_NEGOTIATED`, `UNKNOWN`) — distinct declaration from `ContributionMode`/`ContributionModel` |
| `ViewerRoleInfo` | `GroupDashboard.kt` | data class — `get_viewer_role` result; reuses `ViewerRole` |
| `GroupCorpus` | `GroupDashboard.kt` | data class — `get_group_corpus` result |
| `ActivityItem` | `GroupDashboard.kt` | data class — recent-activity row |
| `ActivityType` | `GroupDashboard.kt` | enum (`MEETING`, `DEPOSIT`, `LOAN`, `PENALTY`, `SHARE_OUT`, `UNKNOWN`) |
| `GroupAccounts` | `GroupDashboard.kt` | data class — `get_group_accounts` result |
| `GroupConfig` | `GroupDashboard.kt` | data class — client-side-constructed savings/loan rule set; repository-layer merge, out of DTO/mapper scope |

## 3. Consumers

- UseCases (`core/domain`)
- ViewModels (`feature/*`)
- Repositories (`core/data` — `AuthRepositoryImpl` / `GroupRepositoryImpl` map
  `core/network/model` DTOs through `core/network/mapper` into these domain
  types before returning them; the join-with-code repository maps
  `JoinWithCodeMappers.kt` output the same way; the personal-dashboard
  repository maps `MemberDashboardMappers.kt` / `SavingsTransactionMappers.kt`
  output the same way; the group-create repository maps
  `GroupCreateMappers.kt` output the same way, BOTH directions — DTO -> domain
  for the office list, domain -> DTO for the submitted `CreateGroupRequest`;
  the group-dashboard repository maps `GroupDashboardMappers.kt` output the
  same way)

## 4. Boundaries

- NO `@Serializable` / `@SerialName` — these types never touch the wire directly.
- NO wire-format concerns (JSON keys, snake/camelCase server casing) — that's
  `core/network/model`'s job.
- Timestamps use `kotlinx.datetime.Instant` (parsed from ISO-8601 wire strings
  by the mapper), never a raw `String`.
- `core/model` depends only on `core/common` + `kotlinx.datetime` (exposed as
  `api` since `Instant` appears in public model signatures) — no Ktor, no
  kotlinx.serialization runtime dependency leaking into its public surface.

## 5. Data

| Model | Field | Type | Nullability |
|---|---|---|---|
| `LoginCredentials` | `emailPhone` | `String` | non-null |
| `LoginCredentials` | `password` | `String` | non-null |
| `SelfRegistration` | `name` | `String` | non-null |
| `SelfRegistration` | `emailPhone` | `String` | non-null |
| `SelfRegistration` | `password` | `String` | non-null |
| `AuthSession` | `userId` | `String` | non-null |
| `AuthSession` | `sessionToken` | `String` | non-null |
| `AuthSession` | `tokenExpiresAt` | `Instant` | non-null |
| `AuthSession` | `groupMemberships` | `List<GroupMembership>` | non-null (may be empty) |
| `UserProfile` | `userId` | `String` | non-null |
| `UserProfile` | `name` | `String` | non-null |
| `UserProfile` | `emailPhone` | `String` | non-null |
| `UserProfile` | `groupMemberships` | `List<GroupMembership>` | non-null (may be empty) |
| `GroupMembership` | `groupId` | `String` | non-null |
| `GroupMembership` | `groupName` | `String` | non-null |
| `GroupMembership` | `role` | `GroupRole` | non-null |
| `GroupMembership` | `joinedAt` | `Instant` | non-null |
| `GroupTypeConfig` | `typeSlug` | `GroupTypeSlug` | non-null |
| `GroupTypeConfig` | `displayName` | `String` | non-null |
| `GroupTypeConfig` | `tagline` | `String` | non-null |
| `GroupTypeConfig` | `savingsMechanism` | `SavingsMechanism` | non-null |
| `GroupTypeConfig` | `contributionMode` | `ContributionMode` | non-null |
| `GroupTypeConfig` | `lendingEnabled` | `Boolean` | non-null |
| `GroupTypeConfig` | `hasSocialFund` | `Boolean` | non-null |
| `GroupTypeConfig` | `hasBankLinkage` | `Boolean` | non-null |
| `GroupTypeConfig` | `welfareOnlyMode` | `Boolean` | non-null |
| `GroupTypeConfig` | `formallyRegistered` | `Boolean` | non-null |
| `GroupTypeConfig` | `defaultLoanMultiplier` | `Double` | non-null |
| `GroupTypeConfig` | `defaultInterestRatePct` | `Double` | non-null |
| `GroupTypeConfig` | `defaultCycleLengthMonths` | `Int` | non-null |
| `GroupTypeConfig` | `maxMembers` | `Int` | non-null |
| `GroupTypeConfig` | `minMembers` | `Int` | non-null |
| `Group` | `id` | `String` | non-null |
| `Group` | `name` | `String` | non-null |
| `Group` | `groupType` | `GroupTypeSlug` | non-null |
| `Group` | `viewerRole` | `ViewerRole` | non-null |
| `Group` | `cycleNumber` | `Int` | non-null |
| `Group` | `memberCount` | `Int` | non-null |
| `Group` | `lastMeetingDate` | `LocalDate` | non-null |
| `Group` | `healthIndicator` | `HealthIndicator` | non-null |
| `Group` | `overdueRate` | `Double` | non-null |
| `Group` | `status` | `String` | non-null |
| `Group` | `fineractCenterId` | `Long` | non-null |
| `GroupPage` | `totalFilteredRecords` | `Int` | non-null |
| `GroupPage` | `groups` | `List<Group>` | non-null (may be empty) |
| `Invitation` | `token` | `String` | non-null |
| `Invitation` | `groupId` | `Long` | non-null |
| `Invitation` | `inviterClientId` | `Long` | non-null |
| `Invitation` | `invitedEmailPhone` | `String` | non-null |
| `Invitation` | `roleToAssign` | `GroupRole` | non-null |
| `Invitation` | `expiresAt` | `Instant` | non-null |
| `Invitation` | `acceptedAt` | `Instant?` | nullable (`null` = code unused) |
| `GroupPreview` | `groupId` | `Long` | non-null |
| `GroupPreview` | `groupName` | `String` | non-null |
| `GroupPreview` | `groupType` | `GroupTypeSlug` | non-null |
| `GroupPreview` | `organizerName` | `String` | non-null |
| `GroupPreview` | `memberCount` | `Int` | non-null |
| `GroupPreview` | `officeId` | `Long` | non-null |
| `GroupPreview` | `roleToAssign` | `GroupRole` | non-null |
| `JoinGroupRequest` | `clientIds` | `List<Long>` | non-null |
| `JoinGroupRequest` | `roleToAssign` | `GroupRole` | non-null |
| `JoinGroupResult` | `resourceId` | `Long` | non-null |
| `JoinGroupResult` | `groupId` | `Long` | non-null |
| `JoinGroupResult` | `clientIds` | `List<Long>` | non-null |
| `InvitationAcceptance` | `acceptedAt` | `Instant` | non-null |
| `InvitationAcceptanceResult` | `resourceId` | `Long` | non-null |
| `InvitationAcceptanceResult` | `acceptedAt` | `Instant` | non-null |
| `MemberDashboard` | `memberName` | `String` | non-null |
| `MemberDashboard` | `myGroups` | `List<GroupSummary>` | non-null (may be empty) |
| `MemberDashboard` | `selectedGroup` | `GroupSummary` | non-null |
| `MemberDashboard` | `poolModel` | `SavingsMechanism` | non-null |
| `MemberDashboard` | `groupLinkedSavingsBalance` | `Double` | non-null |
| `MemberDashboard` | `individualSavingsBalance` | `Double` | non-null |
| `MemberDashboard` | `shareOutProjection` | `Double?` | nullable (ACCUMULATING pool models only) |
| `MemberDashboard` | `rotationPosition` | `Int?` | nullable (ROTATING_PAYOUT pool models only) |
| `MemberDashboard` | `nextRecipientEta` | `String?` | nullable (ROTATING_PAYOUT pool models only) |
| `MemberDashboard` | `recentTransactions` | `List<SavingsTransaction>` | non-null (may be empty) |
| `GroupSummary` | `groupId` | `String` | non-null |
| `GroupSummary` | `name` | `String` | non-null |
| `GroupSummary` | `poolModel` | `SavingsMechanism` | non-null |
| `SavingsTransaction` | `id` | `String` | non-null |
| `SavingsTransaction` | `date` | `LocalDate` | non-null |
| `SavingsTransaction` | `type` | `TransactionType` | non-null |
| `SavingsTransaction` | `amount` | `Double` | non-null |
| `CreateGroupRequest` | `name` | `String` | non-null |
| `CreateGroupRequest` | `officeId` | `Long` | non-null |
| `CreateGroupRequest` | `userId` | `Long` | non-null |
| `CreateGroupRequest` | `currency` | `String` | non-null |
| `CreateGroupRequest` | `meetingDay` | `String` | non-null |
| `CreateGroupRequest` | `meetingTime` | `String` | non-null |
| `CreateGroupRequest` | `typeConfig` | `CreateGroupTypeConfig` | non-null |
| `CreateGroupTypeConfig` | `groupType` | `GroupTypeSlug` | non-null |
| `CreateGroupTypeConfig` | `poolModel` | `SavingsMechanism` | non-null |
| `CreateGroupTypeConfig` | `contributionModel` | `ContributionModel` | non-null |
| `CreateGroupTypeConfig` | `shareoutFormula` | `ShareoutFormula` | non-null |
| `CreateGroupTypeConfig` | `payoutOrderMethod` | `PayoutOrderMethod` | non-null |
| `CreateGroupTypeConfig` | `shareValue` | `Double` | non-null |
| `CreateGroupTypeConfig` | `contributionAmount` | `Double` | non-null |
| `CreateGroupTypeConfig` | `socialFundEnabled` | `Boolean` | non-null |
| `CreateGroupTypeConfig` | `socialFundPercent` | `Double` | non-null |
| `CreateGroupTypeConfig` | `cycleLengthMonths` | `Int` | non-null |
| `CreateGroupTypeConfig` | `loanMultiplier` | `Double` | non-null |
| `CreateGroupTypeConfig` | `interestRate` | `Double` | non-null |
| `CreateGroupTypeConfig` | `fineAmount` | `Double` | non-null |
| `CreateGroupTypeConfig` | `maxMembers` | `Int` | non-null |
| `GroupCreationResult` | `groupId` | `String` | non-null |
| `GroupCreationResult` | `fineractCenterId` | `Long` | non-null |
| `GroupCreationResult` | `inviteCode` | `String` | non-null |
| `Office` | `id` | `Long` | non-null |
| `Office` | `name` | `String` | non-null |
| `Office` | `nameDecorated` | `String` | non-null |
| `Office` | `externalId` | `String?` | nullable |
| `GroupDashboard` | `group` | `GroupDetail` | non-null |
| `GroupDashboard` | `viewerRole` | `ViewerRoleInfo` | non-null |
| `GroupDashboard` | `corpus` | `GroupCorpus` | non-null |
| `GroupDashboard` | `accounts` | `GroupAccounts` | non-null |
| `GroupDetail` | `id` | `String` | non-null |
| `GroupDetail` | `fineractCenterId` | `Long` | non-null |
| `GroupDetail` | `name` | `String` | non-null |
| `GroupDetail` | `cycleNumber` | `Int` | non-null |
| `GroupDetail` | `cycleLengthMonths` | `Int` | non-null |
| `GroupDetail` | `meetingFrequency` | `String` | non-null |
| `GroupDetail` | `memberCount` | `Int` | non-null |
| `GroupDetail` | `overdueLoansCount` | `Int` | non-null |
| `GroupDetail` | `status` | `String` | non-null |
| `GroupDetail` | `typeConfig` | `GroupInstanceConfig` | non-null |
| `GroupInstanceConfig` | `groupType` | `GroupTypeSlug` | non-null |
| `GroupInstanceConfig` | `poolModel` | `SavingsMechanism` | non-null |
| `GroupInstanceConfig` | `contributionModel` | `GroupContributionModel` | non-null |
| `GroupInstanceConfig` | `shareoutFormula` | `String` | non-null |
| `GroupInstanceConfig` | `payoutOrderMethod` | `String` | non-null |
| `GroupInstanceConfig` | `shareValue` | `Double` | non-null |
| `GroupInstanceConfig` | `contributionAmount` | `Double` | non-null |
| `GroupInstanceConfig` | `socialFundEnabled` | `Boolean` | non-null |
| `GroupInstanceConfig` | `cycleLengthMonths` | `Int` | non-null |
| `GroupInstanceConfig` | `loanMultiplier` | `Double` | non-null |
| `GroupInstanceConfig` | `interestRate` | `Double` | non-null |
| `GroupInstanceConfig` | `fineAmount` | `Double` | non-null |
| `ViewerRoleInfo` | `role` | `ViewerRole` | non-null |
| `ViewerRoleInfo` | `memberId` | `Long` | non-null |
| `GroupCorpus` | `currentBalance` | `Double` | non-null |
| `GroupCorpus` | `openingBalance` | `Double` | non-null |
| `GroupCorpus` | `totalContributionsThisCycle` | `Double` | non-null |
| `GroupCorpus` | `totalLoansOutstanding` | `Double` | non-null |
| `GroupCorpus` | `lastUpdated` | `String` | non-null |
| `GroupCorpus` | `rotationPosition` | `Int?` | nullable (ROTATING_PAYOUT only) |
| `GroupCorpus` | `nextRecipientName` | `String?` | nullable (ROTATING_PAYOUT only) |
| `GroupCorpus` | `nextRecipientPosition` | `Int?` | nullable (ROTATING_PAYOUT only) |
| `ActivityItem` | `id` | `String` | non-null |
| `ActivityItem` | `type` | `ActivityType` | non-null |
| `ActivityItem` | `description` | `String` | non-null |
| `ActivityItem` | `amount` | `Double?` | nullable |
| `ActivityItem` | `date` | `String` | non-null |
| `ActivityItem` | `memberName` | `String?` | nullable |
| `GroupAccounts` | `savingsBalance` | `Double` | non-null |
| `GroupAccounts` | `loansOutstanding` | `Double` | non-null |
| `GroupAccounts` | `activeLoanCount` | `Int` | non-null |
| `GroupAccounts` | `shareOutProjection` | `Double?` | nullable (ACCUMULATING only) |
| `GroupAccounts` | `recentActivity` | `List<ActivityItem>` | non-null (may be empty) |
| `GroupConfig` | `shareValue` | `Double?` | nullable |
| `GroupConfig` | `shareMin` | `Int?` | nullable (no wire source — confirmed gap) |
| `GroupConfig` | `shareMax` | `Int?` | nullable (no wire source — confirmed gap) |
| `GroupConfig` | `contributionAmount` | `Double?` | nullable |
| `GroupConfig` | `loanMultiplier` | `Double?` | nullable |
| `GroupConfig` | `interestRate` | `Double?` | nullable |
| `GroupConfig` | `cycleLengthMonths` | `Int` | non-null |
| `GroupConfig` | `fineAmount` | `Double?` | nullable |
| `GroupConfig` | `minimumDisbursementThreshold` | `Double?` | nullable (no wire source — confirmed gap) |

## 6. Errors

`core/model` types carry no error/exception state — parse failures (e.g. a
malformed `tokenExpiresAt` ISO-8601 string, or `Group.lastMeetingDate`'s
`LocalDate.parse`) surface as exceptions at the mapper boundary
(`core/network/mapper`), not here.

## 7. Testing

Domain models are pure data classes exercised indirectly via the mapper test
suites (`core/network/src/commonTest/.../mapper/LoginSignupMappersTest.kt`,
`GroupTypeConfigMappersTest.kt`, `GroupMappersTest.kt`,
`JoinWithCodeMappersTest.kt`, `MemberDashboardMappersTest.kt`,
`SavingsTransactionMappersTest.kt`, `GroupCreateMappersTest.kt`,
`GroupDashboardMappersTest.kt`), which
assert every field is mapped and equality holds end to end, in BOTH
directions where a request is submitted (`GroupCreateMappersTest.kt` covers
`CreateGroupRequestDto -> CreateGroupRequest` AND the reverse
`CreateGroupRequest -> CreateGroupRequestDto`). `HealthIndicator.fromOverdueRate`
additionally has direct boundary-value tests in `GroupMappersTest.kt`;
`Invitation.isExpired` / `isAlreadyUsed` have direct boundary-value tests in
`JoinWithCodeMappersTest.kt` (before-expiry / at-exact-expiry / after-expiry,
plus null-vs-non-null `acceptedAt`).

## 8. Observability

Never log `LoginCredentials.password` or `AuthSession.sessionToken` — both
carry sensitive auth material. `Invitation.invitedEmailPhone` carries PII —
avoid logging it. `MemberDashboard` / `GroupSummary` / `SavingsTransaction`
carry no sensitive fields (balances are not PII by this project's threat
model, but avoid logging full transaction history in bulk).
`CreateGroupRequest` / `CreateGroupTypeConfig` / `GroupCreationResult` /
`Office` carry no sensitive fields. `GroupDashboard` / `GroupDetail` /
`GroupInstanceConfig` / `ViewerRoleInfo` / `GroupCorpus` / `ActivityItem` /
`GroupAccounts` / `GroupConfig` carry no sensitive fields (balances + group
config only; no PII) — `ActivityItem.memberName` is a display name, not a
credential, but avoid bulk-logging the full activity feed.

## 9. Evolution

New auth-related domain concepts are added to `AuthModels.kt`; unrelated
feature domains get their own `{Feature}.kt` file in this module (one file
per feature, per `kmp-dto-gen`'s Step 3 convention). Join-with-code's domain
concepts live in `Invitation.kt`. Personal-dashboard's domain concepts live in
`MemberDashboard.kt` (`MemberDashboard` + `GroupSummary`); the CANONICAL
compact `SavingsTransaction` shape lives in its own `SavingsTransaction.kt`
file since it is explicitly intended for reuse across multiple features
(personal-dashboard today; personal-savings/savings-dashboard pending the
naming-collision resolution flagged in `core/network/model/API.md`).
Group-create's domain concepts live in `GroupCreate.kt`; if a future
group-EDIT feature needs the same type-adaptive rule set, reuse
`CreateGroupTypeConfig` (and `ContributionModel`/`ShareoutFormula`/
`PayoutOrderMethod`) rather than introducing a second shape. Group-dashboard's
domain concepts live in `GroupDashboard.kt` (`GroupDashboard`, `GroupDetail`,
`GroupInstanceConfig`, `GroupContributionModel`, `ViewerRoleInfo`,
`GroupCorpus`, `ActivityItem`, `ActivityType`, `GroupAccounts`,
`GroupConfig`); before generating a feature that needs the full merged
`GroupConfig` (catalogue defaults + per-group overrides), resolve the
repository-layer merge documented on `GroupConfig`'s kdoc rather than
duplicating the derivation. Before generating `group-edit` or any feature
that also embeds a per-group `group_type_config` datatable row, resolve the
`GroupInstanceConfig`/`GroupTypeConfig` naming collision flagged in
`core/network/model/API.md`.
<!-- kmp-dto-gen:END -->
