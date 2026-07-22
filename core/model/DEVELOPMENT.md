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
| `Member` | `Member.kt` | data class — canonical member-list row (`GET /groups/{groupId}/clients`); `MemberRole` (not `Member` itself) reused by member-profile — see `core/network/model/API.md` for the `idea-layer/dtos/MemberDto.yaml` registry-divergence note AND the `MemberProfile` vs `Member` field-shape-divergence note |
| `MemberPage` | `Member.kt` | data class — offset-paginated envelope |
| `MemberRole` | `Member.kt` | enum (`CHAIRPERSON`, `TREASURER`, `SECRETARY`, `MEMBER`, `UNKNOWN`) — deliberately NOT unified with `GroupRole` or `ViewerRole` (near-miss value-sets); see `MemberDto.kt` kdoc |
| `LoanStatus` | `Member.kt` | enum (`ACTIVE`, `NONE`, `OVERDUE`, `UNKNOWN`) |
| `MemberProfile` | `MemberProfile.kt` | data class — `get_client` identity/join-date/phone header; deliberately NOT `Member` (field-shape divergence, see `core/network/model/API.md`) |
| `MemberStatus` | `MemberProfile.kt` | data class — shared nested `{id, value}` Fineract status pair |
| `MemberAccounts` | `MemberProfile.kt` | data class — member-profile accounts card; client-side aggregation of `get_client_accounts`' raw arrays |
| `SavingsDataPoint` | `MemberProfile.kt` | data class — weekly sparkline point; no wire source (confirmed gap) |
| `ActiveLoanSummary` | `MemberProfile.kt` | data class — derived from the first `loanAccounts[]` row |
| `MemberRoleInfo` | `MemberProfile.kt` | data class — `get_member_role` datatable row; reuses `MemberRole` |
| `UpdateMemberRoleRequest` | `MemberProfile.kt` | data class — `update_member_role` PUT body; reuses `MemberRole` |
| `UpdateMemberRoleResult` | `MemberProfile.kt` | data class — `update_member_role` result |
| `LoanSummary` | `LoanSummary.kt` | data class — CANONICAL loan-list row (`GET /groups/{groupId}/loans`); reused by loan-detail + loan dialogs + personal-loans |
| `LoanPage` | `LoanSummary.kt` | data class — offset-paginated envelope |
| `LoanAccountStatus` | `LoanSummary.kt` | enum (`ACTIVE`, `OVERDUE`, `CLOSED`, `PENDING`, `REJECTED`, `UNKNOWN`) — deliberately NOT unified with `LoanStatus` (mismatched value-set, symbol-name collision); see `LoanSummary.kt` kdoc |
| `LoanStatusFilter` | `LoanSummary.kt` | enum (`ALL`, `ACTIVE`, `OVERDUE`, `CLOSED`) — pure client-side filter-chip state, no wire counterpart |

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
  same way; the member-profile repository maps `MemberProfileMappers.kt`
  output the same way, BOTH directions — DTO -> domain for `get_client` /
  `get_client_accounts` / `get_member_role`, domain -> DTO for the submitted
  `UpdateMemberRoleRequest`; the loan-list repository maps
  `LoanSummaryMappers.kt` output the same way — `LoanRepository`
  (`GET /groups/{groupId}/loans`))

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
| `Member` | `id` | `String` | non-null |
| `Member` | `fineractClientId` | `Long` | non-null |
| `Member` | `displayName` | `String` | non-null |
| `Member` | `photoUri` | `String?` | nullable |
| `Member` | `role` | `MemberRole` | non-null |
| `Member` | `savingsBalance` | `Double` | non-null |
| `Member` | `loanStatus` | `LoanStatus` | non-null |
| `MemberPage` | `totalFilteredRecords` | `Int` | non-null |
| `MemberPage` | `members` | `List<Member>` | non-null (may be empty) |
| `MemberProfile` | `id` | `Long` | non-null |
| `MemberProfile` | `displayName` | `String` | non-null |
| `MemberProfile` | `firstName` | `String` | non-null |
| `MemberProfile` | `lastName` | `String` | non-null |
| `MemberProfile` | `phone` | `String` | non-null |
| `MemberProfile` | `hasPhoto` | `Boolean` | non-null |
| `MemberProfile` | `status` | `MemberStatus` | non-null |
| `MemberProfile` | `joinDate` | `String` | non-null |
| `MemberProfile` | `officeId` | `Long` | non-null |
| `MemberStatus` | `id` | `Int` | non-null |
| `MemberStatus` | `value` | `String` | non-null |
| `MemberAccounts` | `savingsBalance` | `Double` | non-null |
| `MemberAccounts` | `savingsHistory` | `List<SavingsDataPoint>` | non-null (always empty — no wire source, confirmed gap) |
| `MemberAccounts` | `activeLoan` | `ActiveLoanSummary?` | nullable (`null` when `loanAccounts[]` is empty) |
| `SavingsDataPoint` | `date` | `String` | non-null |
| `SavingsDataPoint` | `balance` | `Double` | non-null |
| `ActiveLoanSummary` | `id` | `Long` | non-null |
| `ActiveLoanSummary` | `productName` | `String` | non-null |
| `ActiveLoanSummary` | `outstandingBalance` | `Double` | non-null |
| `ActiveLoanSummary` | `inArrears` | `Boolean` | non-null |
| `ActiveLoanSummary` | `dueDate` | `String?` | nullable (always `null` — no wire source, confirmed gap) |
| `MemberRoleInfo` | `role` | `MemberRole` | non-null |
| `MemberRoleInfo` | `groupId` | `Long` | non-null |
| `MemberRoleInfo` | `assignedDate` | `String` | non-null |
| `UpdateMemberRoleRequest` | `role` | `MemberRole` | non-null |
| `UpdateMemberRoleRequest` | `groupId` | `Long` | non-null |
| `UpdateMemberRoleRequest` | `assignedDate` | `String` | non-null |
| `UpdateMemberRoleResult` | `resourceId` | `Long` | non-null |
| `LoanSummary` | `id` | `Long` | non-null |
| `LoanSummary` | `memberId` | `Long` | non-null |
| `LoanSummary` | `memberName` | `String` | non-null |
| `LoanSummary` | `memberPhotoUrl` | `String?` | nullable |
| `LoanSummary` | `loanProductName` | `String` | non-null |
| `LoanSummary` | `principalAmount` | `Double` | non-null |
| `LoanSummary` | `outstandingBalance` | `Double` | non-null |
| `LoanSummary` | `overdueAmount` | `Double` | non-null |
| `LoanSummary` | `status` | `LoanAccountStatus` | non-null |
| `LoanSummary` | `nextRepaymentDate` | `String?` | nullable |
| `LoanSummary` | `isOverdue` | `Boolean` | non-null |
| `LoanSummary` | `fineractLoanId` | `Long` | non-null |
| `LoanPage` | `totalFilteredRecords` | `Int` | non-null |
| `LoanPage` | `loans` | `List<LoanSummary>` | non-null (may be empty) |

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
`GroupDashboardMappersTest.kt`, `MemberMappersTest.kt`,
`MemberProfileMappersTest.kt`), which
assert every field is mapped and equality holds end to end, in BOTH
directions where a request is submitted (`GroupCreateMappersTest.kt` covers
`CreateGroupRequestDto -> CreateGroupRequest` AND the reverse
`CreateGroupRequest -> CreateGroupRequestDto`; `MemberProfileMappersTest.kt`
covers the same both-directions pattern for `UpdateMemberRoleResponseDto ->
UpdateMemberRoleResult` AND the reverse `UpdateMemberRoleRequest ->
UpdateMemberRoleRequestDto`). `HealthIndicator.fromOverdueRate`
additionally has direct boundary-value tests in `GroupMappersTest.kt`;
`Invitation.isExpired` / `isAlreadyUsed` have direct boundary-value tests in
`JoinWithCodeMappersTest.kt` (before-expiry / at-exact-expiry / after-expiry,
plus null-vs-non-null `acceptedAt`). `MemberAccounts`' aggregation logic
(`savingsBalance` = sum of balances, `activeLoan` = first loan account,
`savingsHistory` = always empty) has direct boundary-value tests in
`MemberProfileMappersTest.kt` (multi-account sum, zero-accounts, in-arrears
vs not-in-arrears, empty-loan-list). `LoanSummaryMappersTest.kt` covers
`LoanSummaryDto -> LoanSummary` (every field), the batch converter, the page
converter, and every `LoanAccountStatusDto -> LoanAccountStatus` value.

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
credential, but avoid bulk-logging the full activity feed. `Member` carries
`displayName` (PII-adjacent display name, same threat model as
`ActivityItem.memberName`) and `photoUri` (a CDN URL, not raw image bytes) —
avoid bulk-logging the full member-list page; balances/role/loanStatus are
not sensitive. `MemberProfile` carries `displayName`/`firstName`/`lastName`
(PII display names) and `phone` (PII, a mobile number) — avoid bulk-logging
member-profile identity data; `MemberAccounts` / `ActiveLoanSummary` /
`MemberRoleInfo` / `UpdateMemberRoleRequest` / `UpdateMemberRoleResult` carry
no sensitive fields (balances + role only; no PII). `LoanSummary` carries
`memberName` (display-name PII, same threat model as `Member.displayName`)
and `memberPhotoUrl` (a CDN URL, not raw image bytes) — avoid bulk-logging
the full loan-list page; amounts/status/`isOverdue` are not sensitive.

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
`core/network/model/API.md`. Member-list's domain concepts live in `Member.kt`
(`Member`, `MemberPage`, `MemberRole`, `LoanStatus`) — reuse `MemberRole`
outright for role-badge concepts (member-profile's `MemberRoleInfo` /
`UpdateMemberRoleRequest` do exactly this); before extending `Member` itself,
resolve the `idea-layer/dtos/MemberDto.yaml` registry-divergence flagged in
`core/network/model/API.md`. **Member-profile's own domain concepts live in
`MemberProfile.kt`** (`MemberProfile`, `MemberStatus`, `MemberAccounts`,
`SavingsDataPoint`, `ActiveLoanSummary`, `MemberRoleInfo`,
`UpdateMemberRoleRequest`, `UpdateMemberRoleResult`) — `MemberProfile` was
introduced rather than reusing `Member` (the generation brief's original
instruction) because `get_client`'s response genuinely diverges from
`Member`'s wire shape; see the field-shape-divergence note in this file's
`## models` section (API.md). Before generating member-add / member-invite,
check whether their identity needs match `MemberProfile` (this feature) or
`Member` (member-list) rather than introducing a third identity shape.
**Loan-list's own domain concepts live in `LoanSummary.kt`** (`LoanSummary`,
`LoanPage`, `LoanAccountStatus`, `LoanStatusFilter`) — `LoanSummary` is
CANONICAL, intended for reuse by loan-detail, loan dialogs, and
personal-loans; reuse it outright unless a consumer's operation response
genuinely diverges (same test as `MemberProfile`/`GroupDetail`). Before
extending `LoanSummary` itself, resolve the `idea-layer/dtos/LoanDto.yaml`
registry-divergence flagged in `core/network/model/API.md`. Before
introducing any further loan-status-adjacent enum, resolve the
`LoanAccountStatus` vs `LoanStatus` naming-collision note in this file's
`## models` section (API.md).
<!-- kmp-dto-gen:END -->
