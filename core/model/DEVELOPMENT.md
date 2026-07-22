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
| `LoanDetail` | `LoanDetail.kt` | data class — loan-detail header (`GET /loans/{loanId}`); reuses `LoanAccountStatus` |
| `RepaymentScheduleRow` | `LoanDetail.kt` | data class — single installment period |
| `RepaymentRowStatus` | `LoanDetail.kt` | enum (`PAID`, `PARTIAL`, `UPCOMING`, `OVERDUE`, `UNKNOWN`) |
| `RepaymentTransaction` | `LoanDetail.kt` | data class — single posted repayment-history row; `type` raw `String` |
| `LoanDetailResponse` | `LoanDetail.kt` | data class — composite Store5 read result (`loan` + `repaymentSchedule` + `repaymentHistory`) |
| `LoanDetailTab` | `LoanDetail.kt` | enum (`SCHEDULE`, `HISTORY`) — pure client-side tab-selector state, no wire counterpart |
| `RecordRepaymentRequest` | `RecordRepayment.kt` | data class — loan-repayment-dialog submission input (`make_repayment`) |
| `RepaymentResult` | `RecordRepayment.kt` | data class — `make_repayment` success result |
| `PaymentMethod` | `RecordRepayment.kt` | enum (`MPESA`, `CASH`) with `paymentTypeId: Int` property; pure client-side chip state, no wire counterpart |
| `WriteoffLoanRequest` | `WriteoffLoan.kt` | `data object` — loan-mark-defaulted-dialog confirm-to-writeoff marker (`write_off_loan`); zero domain-meaningful fields |
| `WriteoffResult` | `WriteoffLoan.kt` | data class — `write_off_loan` success result |
| `GroupMember` | `LoanApply.kt` | data class — loan-apply member selector row (`get_group_members`); deliberately NOT `Member` (field-shape divergence, see `core/network/model/API.md`) |
| `LoanProduct` | `LoanApply.kt` | data class — loan-apply product catalogue row (`get_loan_products`) |
| `LoanApplyTemplate` | `LoanApply.kt` | data class — composite loan-apply template (products + defaults + eligibility inputs); reuses `MemberStatus`; `maxEligibleAmount` derived property |
| `ApplyLoanRequest` | `LoanApply.kt` | data class — simplified loan-apply submission input (`create_new_loan`) |
| `LoanApplicationResult` | `LoanApply.kt` | data class — `create_new_loan` success result |
| `LoanPurpose` | `LoanApply.kt` | enum (`MEDICAL`, `EDUCATION`, `BUSINESS`, `EMERGENCY`, `OTHER`, `SCHOOL_FEES`, `FARMING`, `HOME_IMPROVEMENT`, `UNKNOWN`) with `fineractPurposeId: Int` property — extended by loan-request (PP-1), see `core/model/API.md` |
| `LoanRequestPayload` | `LoanRequest.kt` | data class — loan-request member-side submission input (`submit_loan_request`); reuses `LoanPurpose` |
| `LoanRequestResult` | `LoanRequest.kt` | data class — `submit_loan_request` success result |
| `EntityType` | `BatchSync.kt` | enum (`MEETING`, `LOAN`, `SAVINGS`, `ATTENDANCE`, `SHARE_OUT`, `MEMBER`) — sync-status classification of a queued write; resolved from `SyncQueueItem.operationType` by `SyncClassifier.kt`, see registry-divergence note below |
| `SyncOperation` | `BatchSync.kt` | enum (`CREATE`, `UPDATE`, `DELETE`) — sync-status write-kind classification; also resolved from `SyncQueueItem.operationType` by `SyncClassifier.kt` |
| `SyncOverallStatus` | `BatchSync.kt` | enum (`SYNCED`, `PENDING`, `FAILED`) — sync-status screen rollup badge; client-computed from `SyncQueueCounts`, no direct wire source |
| `BatchOperation` | `BatchSync.kt` | data class — one Fineract `/batches` request row; domain projection of a `SyncQueueItem` (`BatchSyncMappers.kt#toBatchOperation`) |
| `BatchSyncRequest` | `BatchSync.kt` | data class — the full `/batches` submission (`requests: List<BatchOperation>`) |
| `BatchSyncResponseItem` | `BatchSync.kt` | data class — one `/batches` response row |
| `SyncResult` | `BatchSync.kt` | data class — client-computed rollup of a `/batches` response (`successCount`/`failedCount`/`conflictCount`) |
| `ChangePinRequest` | `ChangePin.kt` | data class — settings change-PIN dialog submission input (`change_pin`); `currentPin` excluded from the wire body (BasicAuth boundary) |
| `ChangePinResult` | `ChangePin.kt` | data class — `change_pin` success result |

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
  (`GET /groups/{groupId}/loans`); the loan-detail repository maps
  `LoanDetailMappers.kt` output the same way — `LoanRepository`
  (`GET /loans/{loanId}`); the loan-repayment-dialog submission maps
  `RecordRepaymentMappers.kt` output the same way, BOTH directions — domain ->
  DTO for the submitted `RecordRepaymentRequest`, DTO -> domain for the
  `RepaymentResult` — `LoanRepository`
  (`POST /loans/{loanId}/transactions?command=repayment`); the
  loan-mark-defaulted-dialog submission maps `WriteoffLoanMappers.kt` output
  the same way, BOTH directions — domain -> DTO for the confirm-marker
  `WriteoffLoanRequest`, DTO -> domain for the `WriteoffResult` —
  `LoanRepository.markDefaulted(loanId)`
  (`POST /loans/{loanId}/transactions?command=writeoff`); the loan-apply
  repositories map `LoanApplyMappers.kt` output the same way, BOTH
  directions — DTO -> domain composite for the 6 parallel reads
  (`get_group_members`/`get_loan_products`/`get_loan_template`/
  `get_member_savings`/`get_group_corpus`/`get_group_config`), domain -> DTO
  for the submitted `ApplyLoanRequest` — `LoanRepository`/`MemberRepository`/
  `GroupRepository` (`POST /loans`); the loan-request submission maps
  `LoanRequestMappers.kt` output the same way, BOTH directions — domain -> DTO
  for the submitted `LoanRequestPayload`, DTO -> domain for the
  `LoanRequestResult` — `LoanRequestRepository`
  (`POST /datatables/dt_loan_request`); offline, `LoanRequestMappers.kt#toJsonPayload()`
  serializes the resolved `LoanRequestPayloadDto` for `SyncQueueRepository`
  (out of this generation's scope) to persist as a queued retry row); the
  sync-status batch-drain maps `BatchSyncMappers.kt` output the same way,
  BOTH directions — `SyncQueueItem` (already-shipped, `SyncQueueRepository`)
  -> `BatchOperation` -> DTO for the submitted `BatchSyncRequest`, DTO ->
  domain for the `/batches` response rows, folded into `SyncResult` —
  `SyncQueueRepository`/`SyncManager.triggerSync()`
  (`POST /fineract-provider/api/v1/batches`); `SyncClassifier.kt`'s
  `pendingByType(items: List<SyncQueueItem>): Map<EntityType, Int>` is the
  read-side counterpart consumed directly by `SyncQueueRepository.getPendingByType()`
  (no DTO/mapper involved — pure domain-to-domain classification); the
  settings change-PIN submission maps `ChangePinMappers.kt` output the same
  way, BOTH directions — domain -> DTO for the submitted `ChangePinRequest`,
  DTO -> domain for the `ChangePinResult` — `SettingsRepository.changePin(...)`
  (`PUT /fineract-provider/api/v1/self/user/updatePassword`)

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
| `LoanDetail` | `id` | `Long` | non-null |
| `LoanDetail` | `memberId` | `Long` | non-null |
| `LoanDetail` | `memberName` | `String` | non-null |
| `LoanDetail` | `loanProductName` | `String` | non-null |
| `LoanDetail` | `principalAmount` | `Double` | non-null |
| `LoanDetail` | `disbursedDate` | `String` | non-null |
| `LoanDetail` | `interestRatePercent` | `Double` | non-null |
| `LoanDetail` | `totalOutstanding` | `Double` | non-null |
| `LoanDetail` | `totalOverdue` | `Double` | non-null |
| `LoanDetail` | `status` | `LoanAccountStatus` | non-null |
| `LoanDetail` | `fineractLoanId` | `Long` | non-null |
| `RepaymentScheduleRow` | `weekNumber` | `Int` | non-null |
| `RepaymentScheduleRow` | `dueDate` | `String` | non-null |
| `RepaymentScheduleRow` | `dueAmount` | `Double` | non-null |
| `RepaymentScheduleRow` | `paidAmount` | `Double` | non-null |
| `RepaymentScheduleRow` | `balance` | `Double` | non-null |
| `RepaymentScheduleRow` | `status` | `RepaymentRowStatus` | non-null |
| `RepaymentTransaction` | `id` | `Long` | non-null |
| `RepaymentTransaction` | `type` | `String` | non-null |
| `RepaymentTransaction` | `date` | `String` | non-null |
| `RepaymentTransaction` | `amount` | `Double` | non-null |
| `LoanDetailResponse` | `loan` | `LoanDetail` | non-null |
| `LoanDetailResponse` | `repaymentSchedule` | `List<RepaymentScheduleRow>` | non-null (may be empty) |
| `LoanDetailResponse` | `repaymentHistory` | `List<RepaymentTransaction>` | non-null (may be empty) |
| `RecordRepaymentRequest` | `amount` | `Double` | non-null |
| `RecordRepaymentRequest` | `paymentMethod` | `PaymentMethod` | non-null |
| `RecordRepaymentRequest` | `referenceNumber` | `String?` | nullable |
| `RepaymentResult` | `officeId` | `Int` | non-null |
| `RepaymentResult` | `clientId` | `Long` | non-null |
| `RepaymentResult` | `loanId` | `Long` | non-null |
| `RepaymentResult` | `resourceId` | `Long` | non-null |
| `WriteoffLoanRequest` | (none — `data object`) | — | — |
| `WriteoffResult` | `officeId` | `Int` | non-null |
| `WriteoffResult` | `clientId` | `Long` | non-null |
| `WriteoffResult` | `loanId` | `Long` | non-null |
| `WriteoffResult` | `resourceId` | `Long` | non-null |
| `GroupMember` | `id` | `Long` | non-null |
| `GroupMember` | `displayName` | `String` | non-null |
| `GroupMember` | `imagePresent` | `Boolean` | non-null |
| `GroupMember` | `fineractClientId` | `Long` | non-null (derived, = `id`) |
| `LoanProduct` | `id` | `Long` | non-null |
| `LoanProduct` | `name` | `String` | non-null |
| `LoanProduct` | `shortName` | `String` | non-null |
| `LoanProduct` | `principal` | `Double` | non-null |
| `LoanProduct` | `minPrincipal` | `Double` | non-null |
| `LoanProduct` | `maxPrincipal` | `Double` | non-null |
| `LoanProduct` | `numberOfRepayments` | `Int` | non-null |
| `LoanProduct` | `interestRatePerPeriod` | `Double` | non-null |
| `LoanApplyTemplate` | `products` | `List<LoanProduct>` | non-null (may be empty) |
| `LoanApplyTemplate` | `principal` | `Double` | non-null |
| `LoanApplyTemplate` | `numberOfRepayments` | `Int` | non-null |
| `LoanApplyTemplate` | `interestRatePerPeriod` | `Double` | non-null |
| `LoanApplyTemplate` | `interestType` | `MemberStatus` | non-null |
| `LoanApplyTemplate` | `amortizationType` | `MemberStatus` | non-null |
| `LoanApplyTemplate` | `repaymentEvery` | `Int` | non-null |
| `LoanApplyTemplate` | `memberSavingsBalance` | `Double` | non-null |
| `LoanApplyTemplate` | `groupCorpusBalance` | `Double` | non-null |
| `LoanApplyTemplate` | `loanMultiplier` | `Double` | non-null |
| `LoanApplyTemplate` | `maxLoanAmount` | `Double` | non-null |
| `LoanApplyTemplate` | `maxEligibleAmount` | `Double` | non-null (derived, `min(savings*multiplier, maxLoanAmount)`) |
| `ApplyLoanRequest` | `memberId` | `Long` | non-null |
| `ApplyLoanRequest` | `productId` | `Long` | non-null |
| `ApplyLoanRequest` | `amount` | `Double` | non-null |
| `ApplyLoanRequest` | `durationWeeks` | `Int` | non-null |
| `ApplyLoanRequest` | `purpose` | `LoanPurpose` | non-null |
| `ApplyLoanRequest` | `groupId` | `Long` | non-null |
| `LoanApplicationResult` | `officeId` | `Long` | non-null |
| `LoanApplicationResult` | `clientId` | `Long` | non-null |
| `LoanApplicationResult` | `loanId` | `Long` | non-null |
| `LoanApplicationResult` | `resourceId` | `Long` | non-null |
| `LoanRequestPayload` | `clientId` | `Long` | non-null |
| `LoanRequestPayload` | `requestedAmount` | `Double` | non-null |
| `LoanRequestPayload` | `purpose` | `LoanPurpose` | non-null |
| `LoanRequestPayload` | `durationWeeks` | `Int` | non-null |
| `LoanRequestPayload` | `savingsBalanceAtRequest` | `Double` | non-null |
| `LoanRequestResult` | `resourceId` | `Long` | non-null |
| `LoanRequestResult` | `officeId` | `Long` | non-null |
| `LoanRequestResult` | `clientId` | `Long` | non-null |
| `LoanRequestResult` | `resourceExternalId` | `String` | non-null |
| `BatchOperation` | `requestId` | `Int` | non-null |
| `BatchOperation` | `relativeUrl` | `String` | non-null |
| `BatchOperation` | `method` | `String` | non-null |
| `BatchOperation` | `body` | `String` | non-null |
| `BatchSyncRequest` | `requests` | `List<BatchOperation>` | non-null (may be empty) |
| `BatchSyncResponseItem` | `requestId` | `Int` | non-null |
| `BatchSyncResponseItem` | `statusCode` | `Int` | non-null |
| `BatchSyncResponseItem` | `body` | `String` | non-null |
| `SyncResult` | `successCount` | `Int` | non-null |
| `SyncResult` | `failedCount` | `Int` | non-null |
| `SyncResult` | `conflictCount` | `Int` | non-null |
| `ChangePinRequest` | `currentPin` | `String` | non-null |
| `ChangePinRequest` | `newPin` | `String` | non-null |
| `ChangePinResult` | `resourceId` | `Long` | non-null |

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
`LoanDetailMappersTest.kt` covers `LoanDetailDto -> LoanDetail` (every
field), `RepaymentScheduleRowDto -> RepaymentScheduleRow` (every field plus
batch converter), `RepaymentTransactionDto -> RepaymentTransaction` (every
field plus batch converter), the `LoanDetailResponseDto -> LoanDetailResponse`
composite conversion (including the `transactions` -> `repaymentHistory`
rename), and every `RepaymentRowStatusDto -> RepaymentRowStatus` value.
`RecordRepaymentMappersTest.kt` covers `RecordRepaymentRequest ->
RecordRepaymentRequestDto` (every field, including the `paymentMethod ->
paymentTypeId` resolution for both `PaymentMethod` values and the
blank-`referenceNumber`-to-`null` normalization) AND the reverse
`RecordRepaymentResponseDto -> RepaymentResult`, plus direct boundary-value
tests for the `fineractTransactionDate` wire-date helper.
`WriteoffLoanMappersTest.kt` covers `WriteoffLoanRequest -> WriteoffLoanRequestDto`
(transactionDate pass-through, locale/dateFormat defaults + override) AND the
reverse `WriteoffLoanResponseDto -> WriteoffResult` (every field); reuses
`fineractTransactionDate` (`RecordRepaymentMappers.kt`) rather than defining a
second wire-date helper.
`LoanApplyMappersTest.kt` covers `GroupMemberDto -> GroupMember` (`fineractClientId`
derived from `id`) plus the batch + envelope converters; `LoanProductDto ->
LoanProduct` (every field) plus the batch converter; the composite
`LoanApplyTemplateDto -> LoanApplyTemplate` conversion (products + summed
savings balance + corpus + config, every field) plus
`LoanApplyTemplate.maxEligibleAmount`'s two boundary cases
(multiplier-capped vs `maxLoanAmount`-capped); `ApplyLoanRequest ->
ApplyLoanRequestDto` against a pinned `kotlin.time.Instant` (every resolved
field, reusing `fineractTransactionDate` rather than a second wire-date
helper); `ApplyLoanResponseDto -> LoanApplicationResult` (every field); and
every `LoanPurposeDto <-> LoanPurpose` value plus
`LoanPurpose.fineractPurposeId`'s sequential assignment (extended by
`LoanRequestMappersTest.kt`'s own `LoanPurpose`/`LoanPurposeDto` usage to cover
the 3 loan-request-added values via the SAME shared mapper pair).
`LoanRequestMappersTest.kt` covers `LoanRequestPayload -> LoanRequestPayloadDto`
(every field, against a pinned `kotlin.time.Instant`, including the
`status` default-to-`"PENDING"` and the `purpose` resolution via the reused
`LoanPurpose.toDto()`) AND the reverse `LoanRequestResponseDto ->
LoanRequestResult` (every field), plus the `toJsonPayload()`/
`loanRequestPayloadDtoFromJson()` offline-SyncQueue serialization round-trip.
`SyncClassifierTest.kt` (`core/model/src/commonTest`, the FIRST direct
commonTest suite in this module — `commonTest.dependencies { implementation(libs.kotlin.test) }`
added to `core/model/build.gradle.kts` for it) covers the full
`operationTypeToEntityType`/`operationTypeToSyncOperation` known mapping
table (`LOAN_REQUEST`/`CREATE_MEMBER`/`ASSIGN_MEMBER_ROLE`/`UPLOAD_MEMBER_PHOTO`),
the prefix-based fallback for each of the 5 other `EntityType` values, the
documented unknown-operationType fallback (`MEMBER`/`UPDATE`), and
`pendingByType`'s PENDING-only grouping (multi-row same-type, zero-pending
map, empty-input map). `BatchSyncMappersTest.kt`
(`core/network/src/commonTest/.../mapper`) covers `SyncQueueItem.toBatchOperation`
(every resolved field — `relativeUrl` derivation, hardcoded `method`,
verbatim `body`, caller-supplied vs. queue-row-id `requestId`),
`BatchOperation <-> BatchOperationDto` (every field, both directions),
`BatchSyncRequest <-> BatchSyncRequestDto` (batch converter, declaration
order), `BatchSyncResponseItemDto -> BatchSyncResponseItem` (every field
plus the batch converter), and `List<BatchSyncResponseItem>.toSyncResult`'s
3-way fold (mixed 200/201/409/500/400, all-success, empty-list boundary
cases). `ChangePinMappersTest.kt` (`core/network/src/commonTest/.../mapper`)
covers `ChangePinRequest -> ChangePinRequestDto` (`newPin` resolves to BOTH
`password`/`repeatPassword`, `currentPin` never leaks into either wire
field) AND the reverse `ChangePinResponseDto -> ChangePinResult`.
`LanguageConfigTest.kt` (`core/model/src/commonTest/.../user`) covers the
added `SWAHILI` entry's `localeName`/`text` plus the settings screen's full
declared `AppLanguage` value-set (`ENGLISH`/`SWAHILI`/`FRENCH`/`HINDI`).

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
`LoanDetail` carries `memberName` (display-name PII, same threat model as
`LoanSummary.memberName`) — avoid bulk-logging; amounts/`status` are not
sensitive. `RepaymentScheduleRow`/`RepaymentTransaction`/`LoanDetailResponse`
carry no PII (amounts + dates only). `RecordRepaymentRequest`/`RepaymentResult`
carry no PII (amounts, an internally-resolved `paymentTypeId`, and Fineract
resource IDs only — `referenceNumber` is a treasurer-entered reference code,
not a credential, but avoid bulk-logging it alongside amounts).
`WriteoffLoanRequest`/`WriteoffResult` carry no PII (zero fields / Fineract
resource IDs only) — the write-off IS the irreversible destructive event
itself, so its occurrence is appropriate to log at info level (loanId only,
per the `docs.yaml` analytics event contract), just not paired with member PII
from a joined model. `GroupMember` carries `displayName` (display-name PII,
same threat model as `Member.displayName`) — avoid bulk-logging the full
member-selector list; `imagePresent`/`id`/`fineractClientId` are not
sensitive. `LoanProduct`/`LoanApplyTemplate` carry no PII (product catalogue
+ balances + eligibility config only). `ApplyLoanRequest`/
`LoanApplicationResult` carry no PII (amounts, a week count, an enum
purpose, and Fineract resource IDs only) — the loan-submission event itself
is appropriate to log at info level (loanId only, matching the
`WriteoffResult` precedent above), just not paired with member PII from a
joined model. `LoanRequestPayload`/`LoanRequestResult` carry no PII (amounts,
a week count, an enum purpose, a savings-balance snapshot, and Fineract
resource IDs only) — same "amounts/enum/resource-id only" threat model as
`ApplyLoanRequest`/`LoanApplicationResult`; the queued offline JSON
(`toJsonPayload()`) carries the identical field set, so no additional
exposure beyond the online path. `EntityType`/`SyncOperation`/
`SyncOverallStatus`/`BatchOperation`/`BatchSyncRequest`/
`BatchSyncResponseItem`/`SyncResult` carry no PII — enum classifications,
Fineract `relativeUrl`/`method`, and status-code counts only; `BatchOperation.body`/
`BatchSyncResponseItem.body` echo a queued mutation's `payloadJson` and MAY
carry member-identifying fields (e.g. a loan-request's `clientId`) depending
on the originating feature's payload shape — avoid bulk-logging batch
request/response bodies verbatim; log `requestId`/`statusCode` only.
`ChangePinRequest.currentPin`/`.newPin` are BOTH sensitive PIN material —
NEVER log either field, matching the `LoginCredentials.password` threat
model. `ChangePinResult` carries no sensitive fields (a Fineract resource ID
only).

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
`## models` section (API.md). **Loan-detail's own domain concepts live in
`LoanDetail.kt`** (`LoanDetail`, `RepaymentScheduleRow`, `RepaymentRowStatus`,
`RepaymentTransaction`, `LoanDetailResponse`, `LoanDetailTab`) —
`LoanDetail.status` reuses `LoanAccountStatus` outright (no new enum). Before
extending `LoanDetail`/`RepaymentTransaction`, resolve the
`idea-layer/dtos/LoanDto.yaml`/`LoanRepaymentDto.yaml` registry-divergences
flagged in `core/network/model/API.md`. **Loan-repayment-dialog's own domain
concepts live in `RecordRepayment.kt`** (`RecordRepaymentRequest`,
`RepaymentResult`, `PaymentMethod`) — `RepaymentTransaction` (loan-detail) was
NOT reused (it models the read-side `get_loan_hist` history row, not
`make_repayment`'s submit/result shape). Before extending
`RecordRepaymentRequest`/`RepaymentResult`, resolve the
`idea-layer/dtos/LoanRepaymentDto.yaml` registry-divergence flagged in
`core/network/model/API.md` first. **Loan-mark-defaulted-dialog's own domain
concepts live in `WriteoffLoan.kt`** (`WriteoffLoanRequest`, `WriteoffResult`)
— `RecordRepaymentRequest`/`RepaymentResult` were NOT reused despite the
structurally-identical `WriteoffResult`/`RepaymentResult` shapes, matching the
established per-operation-type precedent (no cross-operation domain-model
sharing across distinct Fineract transaction commands, even when their
response envelopes coincide). **Loan-apply's own domain concepts live in
`LoanApply.kt`** (`GroupMember`, `LoanProduct`, `LoanApplyTemplate`,
`ApplyLoanRequest`, `LoanApplicationResult`, `LoanPurpose`) — `GroupMember`
was introduced rather than reusing the canonical `Member` (member-list):
`Member` requires non-null `role`/`savingsBalance`/`loanStatus` (none of
which `get_group_members` returns) and its `id` is a `String`, while this
endpoint's `id` is a raw Fineract `Long`; forcing `Member` reuse would mean
fabricating values with no wire source (Hard Rule 4), same test already
applied to `MemberProfile`/`GroupDetail`. `LoanApplyTemplate` reuses the
SHARED `MemberStatus` (`MemberProfile.kt`) for `interestType`/
`amortizationType` rather than introducing a new `{id, value}` lookup-pair
type. Before generating a future feature that also needs a group's
loan-eligibility policy (`loanMultiplier`/`maxLoanAmount`), reuse
`LoanApplyTemplate`'s eligibility fields / `LoanApplyMappers.kt` rather than
re-deriving the `dt_group_config` read. `LoanPurpose.fineractPurposeId`'s
sequential 1-5 assignment is a confirmed gap (`api.yaml` declares no
explicit per-value wire id) — if a future `api.yaml` revision declares
explicit ids, update the enum's constructor arguments in place (no shape
change, no `SCHEMA_VERSION` bump needed on the domain side). **Loan-request's
own domain concepts live in `LoanRequest.kt`** (`LoanRequestPayload`,
`LoanRequestResult`) — `LoanRequestPayload.purpose` reuses `LoanPurpose`
outright, EXTENDED with `SCHOOL_FEES`/`FARMING`/`HOME_IMPROVEMENT` (PP-1,
`idea-layer/screens/loan-request/ui.yaml#components.purpose_dropdown.options`
is the value-set SoT) rather than forking a second purpose enum — the 3 new
`fineractPurposeId` placeholders (`6`/`7`/`8`) continue loan-apply's
sequential-assignment confirmed gap and are unused by loan-request's own wire
contract (which transmits the bare enum name as a literal `String`, not an
`Int` id). Before generating a future feature needing an offline-queue
serialization helper for a different mutation payload, reuse the
`toJsonPayload()`/`{...}FromJson()` pattern (`LoanRequestMappers.kt`) rather
than re-deriving a per-feature Json config — `SyncQueueEntry`/
`SyncQueueRepository` themselves remain out of DTO/mapper generation scope
(declared in `api.yaml#dependencies.repositories`, same "repository lives
outside this generation step" precedent as `MemberAddRepository`).
**Sync-status's own domain concepts live in `BatchSync.kt`** (`EntityType`,
`SyncOperation`, `SyncOverallStatus`, `BatchOperation`, `BatchSyncRequest`,
`BatchSyncResponseItem`, `SyncResult`) + `SyncClassifier.kt` (the pure
`operationTypeToEntityType`/`operationTypeToSyncOperation`/`pendingByType`
functions) — **the ALREADY-SHIPPED `SyncQueueItem`/`SyncStatus`/`SyncQueueCounts`
(`SyncQueue.kt`) were REUSED OUTRIGHT, not redefined**, per this generation's
explicit brief. Before extending the sync-status domain surface, resolve the
`idea-layer/screens/sync-status/api.yaml#dtos.SyncQueueItem` registry
divergence flagged on `EntityType`'s kdoc (the registry models
`entityType`/`operation` as first-class `SyncQueueItem` columns; the shipped
schema keeps the single generic `operationType: String` instead —
`SyncClassifier.kt` bridges the two without a DB migration). If a future
`api.yaml` revision needs a genuinely NEW `EntityType`/`SyncOperation` value
this classifier's prefix-fallback doesn't already cover, extend the enum +
the KNOWN mapping table in `SyncClassifier.kt`'s kdoc (never invent a second
classification helper). **Settings' own domain concepts live in `ChangePin.kt`**
(`ChangePinRequest`, `ChangePinResult`) — settings otherwise REUSES two
pre-existing enums rather than forking new ones: `LanguageConfig`
(`kpt/core/model/user/LanguageConfig.kt`, a pre-existing template enum, NOT a
`kmp-dto-gen`-owned file) gained a `SWAHILI` entry (`localeName = "sw"`,
`text = "Kiswahili"`) for `api.yaml#dtos.AppLanguage`; no exhaustive
`when (LanguageConfig)` branch needed updating (`LanguageDialog.kt` iterates
`LanguageConfig.entries.forEach { ... }`, not a `when`). `DarkThemeConfig`
(`kpt/core/model/user/DarkThemeConfig.kt`) was NOT extended or forked —
`ui.yaml#state_model.selectedTheme`'s `AppTheme` (`LIGHT`/`DARK`/`SYSTEM`)
maps 1:1 onto its existing 3 values (`SYSTEM` <-> `FOLLOW_SYSTEM`); the
settings ViewModel should read/write `DarkThemeConfig` directly via
`UserPreferencesRepository` rather than introducing a parallel `AppTheme`
type. Before generating a future feature that also submits a
BasicAuth-authenticated Fineract self-service write, reuse the
"caller-supplied credential excluded from the DTO body" pattern
(`ChangePinRequest.currentPin`) rather than inventing a request field with no
wire source.
<!-- kmp-dto-gen:END -->
