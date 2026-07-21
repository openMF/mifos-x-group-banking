<!-- generated-by: kmp-dto-gen -->
<!-- kmp-dto-gen:BEGIN -->
# core/model — API.md

## models

| Model | Fields | Notes |
|---|---|---|
| `LoginCredentials` | `emailPhone: String`, `password: String` | login-signup input value object |
| `SelfRegistration` | `name: String`, `emailPhone: String`, `password: String` | login-signup input value object |
| `AuthSession` | `userId: String`, `sessionToken: String`, `tokenExpiresAt: Instant`, `groupMemberships: List<GroupMembership>` | auth-result value object; `hasGroups` derived property |
| `UserProfile` | `userId: String`, `name: String`, `emailPhone: String`, `groupMemberships: List<GroupMembership>` | companion_me (COMP-AUTH-003) domain shape; `hasGroups` derived property |
| `GroupMembership` | `groupId: String`, `groupName: String`, `role: GroupRole`, `joinedAt: Instant` | nested in `AuthSession` / `UserProfile` |
| `GroupRole` | enum: `ORGANIZER`, `MEMBER`, `TREASURER`, `SECRETARY`, `UNKNOWN` | mirrors wire `GroupRoleDto` 1:1; `UNKNOWN` absorbs unrecognized wire values |
| `GroupTypeConfig` | `typeSlug: GroupTypeSlug`, `displayName: String`, `tagline: String`, `savingsMechanism: SavingsMechanism`, `contributionMode: ContributionMode`, `lendingEnabled: Boolean`, `hasSocialFund: Boolean`, `hasBankLinkage: Boolean`, `welfareOnlyMode: Boolean`, `formallyRegistered: Boolean`, `defaultLoanMultiplier: Double`, `defaultInterestRatePct: Double`, `defaultCycleLengthMonths: Int`, `maxMembers: Int`, `minMembers: Int` | COMP-DT-003 seeded catalogue row; read-only, backs the 9 group-type-picker cards |
| `GroupTypeSlug` | enum: `VSLA`, `ROSCA`, `ASCA`, `SILC`, `SHG`, `SACCO`, `CBO_VILLAGE_BANK`, `BURIAL_WELFARE`, `JLG`, `UNKNOWN` | mirrors wire `GroupTypeSlugDto` 1:1 |
| `SavingsMechanism` | enum: `ACCUMULATING`, `ROTATING_PAYOUT`, `NONE`, `UNKNOWN` | mirrors wire `SavingsMechanismDto` 1:1 |
| `ContributionMode` | enum: `SHARE_BASED_VARIABLE`, `FIXED`, `MINIMAL`, `UNKNOWN` | mirrors wire `ContributionModeDto` 1:1 |
| `Group` | `id: String`, `name: String`, `groupType: GroupTypeSlug`, `viewerRole: ViewerRole`, `cycleNumber: Int`, `memberCount: Int`, `lastMeetingDate: LocalDate`, `healthIndicator: HealthIndicator`, `overdueRate: Double`, `status: String`, `fineractCenterId: Long` | COMP-GRP-001 canonical group shape; reuses `GroupTypeSlug` (see `GroupTypeConfig.kt`), read-only |
| `GroupPage` | `totalFilteredRecords: Int`, `groups: List<Group>` | offset-paginated envelope of COMP-GRP-001 |
| `ViewerRole` | enum: `ORGANIZER`, `MEMBER`, `TREASURER`, `CHAIRPERSON`, `SECRETARY`, `UNKNOWN` | mirrors wire `ViewerRoleDto` 1:1; NOT unified with `GroupRole` (missing `CHAIRPERSON`) — see `Group.kt` kdoc |
| `HealthIndicator` | enum: `GREEN`, `AMBER`, `RED`, `UNKNOWN` | mirrors wire `HealthIndicatorDto` 1:1; `fromOverdueRate(rate: Double)` factory independently re-derives GREEN(<0.05)/AMBER(0.05–0.20)/RED(>=0.20) |
| `Invitation` | `token: String`, `groupId: Long`, `inviterClientId: Long`, `invitedEmailPhone: String`, `roleToAssign: GroupRole`, `expiresAt: Instant`, `acceptedAt: Instant?` | COMP-DT-004 invitations-datatable row; `isAlreadyUsed` derived property + `isExpired(now: Instant = Clock.System.now())` derived function; reuses `GroupRole` (no new role enum) |
| `GroupPreview` | `groupId: Long`, `groupName: String`, `groupType: GroupTypeSlug`, `organizerName: String`, `memberCount: Int`, `officeId: Long`, `roleToAssign: GroupRole` | join-with-code group preview card; reuses `GroupTypeSlug` + `GroupRole` (no new enums) |
| `JoinGroupRequest` | `clientIds: List<Long>`, `roleToAssign: GroupRole` | COMP-GRP-003 associate-clients input |
| `JoinGroupResult` | `resourceId: Long`, `groupId: Long`, `clientIds: List<Long>` | COMP-GRP-003 associate-clients result |
| `InvitationAcceptance` | `acceptedAt: Instant` | COMP-DT-004 mark-accepted input |
| `InvitationAcceptanceResult` | `resourceId: Long`, `acceptedAt: Instant` | COMP-DT-004 mark-accepted result; flattens wire's nested `changes.accepted_at` |
| `MemberDashboard` | `memberName: String`, `myGroups: List<GroupSummary>`, `selectedGroup: GroupSummary`, `poolModel: SavingsMechanism`, `groupLinkedSavingsBalance: Double`, `individualSavingsBalance: Double`, `shareOutProjection: Double?`, `rotationPosition: Int?`, `nextRecipientEta: String?`, `recentTransactions: List<SavingsTransaction>` | personal-dashboard member home screen (companion `GET /companion/member/dashboard`); reuses `SavingsMechanism` for `poolModel` (no new pool-model enum) |
| `GroupSummary` | `groupId: String`, `name: String`, `poolModel: SavingsMechanism` | lightweight per-group summary row (group-selector chip); reuses `SavingsMechanism` |
| `SavingsTransaction` | `id: String`, `date: LocalDate`, `type: TransactionType`, `amount: Double` | CANONICAL compact recent-activity row; see registry/naming-collision note below |
| `TransactionType` | enum: `DEPOSIT`, `WITHDRAWAL`, `UNKNOWN` | mirrors wire `TransactionTypeDto` 1:1 |

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`
(contract refs COMP-AUTH-001, COMP-AUTH-002, COMP-AUTH-003);
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003);
`idea-layer/screens/group-list/{api.yaml,docs.yaml,data-flow.yaml}` (COMP-GRP-001);
`idea-layer/screens/join-with-code/{api.yaml,docs.yaml}` (COMP-DT-004 + COMP-GRP-003);
`idea-layer/screens/personal-dashboard/{api.yaml,docs.yaml}` (companion `GET /companion/member/dashboard`).

Wire counterparts + `@SerialName` mapping: see `core/network/model/API.md`
(includes a registry-divergence note re: `idea-layer/dtos/GroupDto.yaml`, and
a THREE-way naming-collision note re: `SavingsTransaction` /
`idea-layer/dtos/SavingsTransactionDto.yaml` /
`idea-layer/screens/personal-savings/api.yaml`).
<!-- kmp-dto-gen:END -->
