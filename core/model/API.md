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
| `CreateGroupRequest` | `name: String`, `officeId: Long`, `userId: Long`, `currency: String`, `meetingDay: String`, `meetingTime: String`, `typeConfig: CreateGroupTypeConfig` | group-create wizard submission (COMP-GRP-001); assembled across the wizard's 4 steps |
| `CreateGroupTypeConfig` | `groupType: GroupTypeSlug`, `poolModel: SavingsMechanism`, `contributionModel: ContributionModel`, `shareoutFormula: ShareoutFormula`, `payoutOrderMethod: PayoutOrderMethod`, `shareValue: Double`, `contributionAmount: Double`, `socialFundEnabled: Boolean`, `socialFundPercent: Double`, `cycleLengthMonths: Int`, `loanMultiplier: Double`, `interestRate: Double`, `fineAmount: Double`, `maxMembers: Int` | type-adaptive rule set forwarded to provision the `group_type_config` datatable row; reuses `GroupTypeSlug` + `SavingsMechanism` (no duplicate enums for those 2 axes) |
| `GroupCreationResult` | `groupId: String`, `fineractCenterId: Long`, `inviteCode: String` | group-create success result |
| `ContributionModel` | enum: `FIXED_AMOUNT`, `SHARE_BASED_VARIABLE`, `FIXED_NEGOTIATED`, `UNKNOWN` | mirrors wire `ContributionModelDto` 1:1; distinct from `ContributionMode` (different value-set) |
| `ShareoutFormula` | enum: `NONE`, `PRORATA_SHARES`, `PRORATA_SAVINGS`, `EQUAL`, `INVESTMENT_PROPORTIONAL`, `UNKNOWN` | mirrors wire `ShareoutFormulaDto` 1:1 |
| `PayoutOrderMethod` | enum: `FIXED_ORDER`, `LOTTERY`, `AUCTION`, `NEED_BASED`, `NA`, `UNKNOWN` | mirrors wire `PayoutOrderMethodDto` 1:1 |
| `Office` | `id: Long`, `name: String`, `nameDecorated: String`, `externalId: String?` | office dropdown row; `externalId` nullable (registry gap, see `core/network/model/API.md`) |
| `GroupDashboard` | `group: GroupDetail`, `viewerRole: ViewerRoleInfo`, `corpus: GroupCorpus`, `accounts: GroupAccounts` | group-dashboard composite (COMP-GRP-001 4-way parallel fan-in); NOT returned by a single endpoint; `GroupConfig` deliberately excluded — see notes below |
| `GroupDetail` | `id: String`, `fineractCenterId: Long`, `name: String`, `cycleNumber: Int`, `cycleLengthMonths: Int`, `meetingFrequency: String`, `memberCount: Int`, `overdueLoansCount: Int`, `status: String`, `typeConfig: GroupInstanceConfig` | `get_group` identity/header shape; deliberately NOT `Group` — see field-shape-divergence note below |
| `GroupInstanceConfig` | `groupType: GroupTypeSlug`, `poolModel: SavingsMechanism`, `contributionModel: GroupContributionModel`, `shareoutFormula: String`, `payoutOrderMethod: String`, `shareValue: Double`, `contributionAmount: Double`, `socialFundEnabled: Boolean`, `cycleLengthMonths: Int`, `loanMultiplier: Double`, `interestRate: Double`, `fineAmount: Double` | THIS group's configured instance (embedded on `GroupDetail.typeConfig`); reuses `GroupTypeSlug` + `SavingsMechanism`; naming-collision with catalogue `GroupTypeConfig` — see note below |
| `GroupContributionModel` | enum: `FIXED_AMOUNT`, `SHARE_BASED_VARIABLE`, `FIXED_NEGOTIATED`, `UNKNOWN` | mirrors wire `GroupContributionModelDto` 1:1; distinct from both `ContributionMode` and `ContributionModel` (see note below) |
| `ViewerRoleInfo` | `role: ViewerRole`, `memberId: Long` | `get_viewer_role` result; reuses `ViewerRole` (no new enum) |
| `GroupCorpus` | `currentBalance: Double`, `openingBalance: Double`, `totalContributionsThisCycle: Double`, `totalLoansOutstanding: Double`, `lastUpdated: String`, `rotationPosition: Int?`, `nextRecipientName: String?`, `nextRecipientPosition: Int?` | `get_group_corpus` result; ROTATING_PAYOUT fields nullable |
| `ActivityItem` | `id: String`, `type: ActivityType`, `description: String`, `amount: Double?`, `date: String`, `memberName: String?` | row of `GroupAccounts.recentActivity` (last 10) |
| `ActivityType` | enum: `MEETING`, `DEPOSIT`, `LOAN`, `PENALTY`, `SHARE_OUT`, `UNKNOWN` | mirrors wire `ActivityTypeDto` 1:1 |
| `GroupAccounts` | `savingsBalance: Double`, `loansOutstanding: Double`, `activeLoanCount: Int`, `shareOutProjection: Double?`, `recentActivity: List<ActivityItem>` | `get_group_accounts` result; `shareOutProjection` populated for ACCUMULATING pool models only |
| `GroupConfig` | `shareValue: Double?`, `shareMin: Int?`, `shareMax: Int?`, `contributionAmount: Double?`, `loanMultiplier: Double?`, `interestRate: Double?`, `cycleLengthMonths: Int`, `fineAmount: Double?`, `minimumDisbursementThreshold: Double?` | client-side-constructed savings/loan rule set (`savings_summary_card`); repository-layer merge of `GroupInstanceConfig` + catalogue `GroupTypeConfig`, out of DTO/mapper scope; `shareMin`/`shareMax`/`minimumDisbursementThreshold` have NO wire source (confirmed gap) |

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`
(contract refs COMP-AUTH-001, COMP-AUTH-002, COMP-AUTH-003);
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003);
`idea-layer/screens/group-list/{api.yaml,docs.yaml,data-flow.yaml}` (COMP-GRP-001);
`idea-layer/screens/join-with-code/{api.yaml,docs.yaml}` (COMP-DT-004 + COMP-GRP-003);
`idea-layer/screens/personal-dashboard/{api.yaml,docs.yaml}` (companion `GET /companion/member/dashboard`);
`idea-layer/screens/group-create/api.yaml` (COMP-GRP-001 `POST /companion/groups`
+ `GET /offices`; no dedicated `idea-layer/dtos/{Dto}.yaml` registry entry
exists for this feature — `api.yaml` is the sole SoT);
`idea-layer/screens/group-dashboard/{api.yaml,ui.yaml,docs.yaml}` (COMP-GRP-001
4-way parallel fan-in: `get_group` + `get_viewer_role` + `get_group_corpus` +
`get_group_accounts`; no dedicated `idea-layer/dtos/{Dto}.yaml` registry entry
exists for this feature — `api.yaml` is the sole SoT, per PP-1).

**`GroupDetail` vs `Group` field-shape divergence (flagged for the
cross-feature repair station):** `idea-layer/screens/group-dashboard/ui.yaml#state_model`
pseudocodes its state field as `group: Group?`, and this feature's generation
brief explicitly instructed reusing `Group` "do NOT duplicate" — but
`get_group`'s actual `api.yaml` response (`id`, `fineractCenterId`, `name`,
`cycleNumber`, `cycleLengthMonths`, `meetingFrequency`, `memberCount`,
`overdueLoansCount`, `status`, `typeConfig`) genuinely diverges from `Group`'s
wire shape (`GroupDto`, from `group-list`'s COMP-GRP-001): it does NOT return
`groupType`/`viewerRole`/`lastMeetingDate`/`healthIndicator`/`overdueRate`
(all non-null required on `Group`, no default) and DOES return 4 fields
`Group` doesn't carry. Forcing `Group` reuse would require fabricating values
with no wire source, so `GroupDetail` was introduced instead — resolve at
Station 3 (widen `Group` to a superset, or keep the two identity shapes
formally distinct as they are today).

**`GroupInstanceConfig` naming collision (flagged for the cross-feature
repair station — same pattern as the already-documented `SavingsTransaction`
collision):** `idea-layer/screens/group-dashboard/api.yaml#dtos.GroupTypeConfig`
declares the per-group-instance shape embedded on `GroupDetail.typeConfig`
under the bare name `GroupTypeConfig` — the SAME name already used by the
COMP-DT-003 seed-catalogue row (`core.model.GroupTypeConfig`, reused
elsewhere per this feature's own explicit "reuse GroupTypeConfig" instruction)
— but the two shapes are NOT interchangeable (this one has `shareValue`/
`contributionAmount`/`fineAmount`/`shareoutFormula`/`payoutOrderMethod`; the
catalogue row has `displayName`/`tagline`/`defaultLoanMultiplier`/
`maxMembers`/`minMembers` — no overlap beyond the group-type/pool-model
axes). Named `GroupInstanceConfig` here to avoid the Kotlin class-name clash
while flagging the source collision for Station 3.

**`GroupContributionModel` is a THIRD contribution-mode-adjacent enum**
(alongside `ContributionMode` — group-type-picker catalogue — and
`ContributionModel` — group-create wizard): its 3-value set (`FIXED_AMOUNT`/
`SHARE_BASED_VARIABLE`/`FIXED_NEGOTIATED`) happens to be IDENTICAL to
`ContributionModel`'s (group-create), but was NOT unified with it because the
two are declared independently on unrelated features' `api.yaml`s with no
shared source-of-truth reference between them — flagged for Station 3 to
evaluate whether `ContributionModel` should be reused here instead.

Wire counterparts + `@SerialName` mapping: see `core/network/model/API.md`
(includes a registry-divergence note re: `idea-layer/dtos/GroupDto.yaml`, a
THREE-way naming-collision note re: `SavingsTransaction` /
`idea-layer/dtos/SavingsTransactionDto.yaml` /
`idea-layer/screens/personal-savings/api.yaml`, the group-create enum
reuse-vs-new-enum rationale + `OfficeDto.externalId` / `PayoutOrderMethodDto`
value-set notes, and the group-dashboard `GroupDetail`/`GroupInstanceConfig`
divergence + collision notes above).
<!-- kmp-dto-gen:END -->
