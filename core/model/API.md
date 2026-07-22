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
| `Member` | `id: String`, `fineractClientId: Long`, `displayName: String`, `photoUri: String?`, `role: MemberRole`, `savingsBalance: Double`, `loanStatus: LoanStatus` | CANONICAL member-list row (`GET /groups/{groupId}/clients`), also reused by member-profile + member-add + member-invite; see registry-divergence note below |
| `MemberPage` | `totalFilteredRecords: Int`, `members: List<Member>` | offset-paginated envelope (`page_size=20`) |
| `MemberRole` | enum: `CHAIRPERSON`, `TREASURER`, `SECRETARY`, `MEMBER`, `UNKNOWN` | mirrors wire `MemberRoleDto` 1:1; NOT unified with `GroupRole` (missing `CHAIRPERSON`) or `ViewerRole` (extra `ORGANIZER` not in this feature's declared value-set) — see `Member.kt` kdoc |
| `LoanStatus` | enum: `ACTIVE`, `NONE`, `OVERDUE`, `UNKNOWN` | mirrors wire `LoanStatusDto` 1:1; no pre-existing loan-status enum found to reuse |
| `MemberProfile` | `id: Long`, `displayName: String`, `firstName: String`, `lastName: String`, `phone: String`, `hasPhoto: Boolean`, `status: MemberStatus`, `joinDate: String`, `officeId: Long` | `get_client` identity/join-date/phone header; deliberately NOT the canonical `Member` — see field-shape-divergence note below |
| `MemberStatus` | `id: Int`, `value: String` | shared nested Fineract `{id, value}` status pair — reused by `MemberProfile.status` and `ActiveLoanSummary`-adjacent derivations |
| `MemberAccounts` | `savingsBalance: Double`, `savingsHistory: List<SavingsDataPoint>`, `activeLoan: ActiveLoanSummary?` | member-profile accounts card (`get_client_accounts`); `savingsBalance` derived by summing raw `savingsAccounts[].balance`, `activeLoan` derived from the first raw `loanAccounts[]` row, `savingsHistory` has no wire source (confirmed gap) — see note below |
| `SavingsDataPoint` | `date: String`, `balance: Double` | weekly sparkline point; NO wire source anywhere in `api.yaml` — mapper always produces an empty list |
| `ActiveLoanSummary` | `id: Long`, `productName: String`, `outstandingBalance: Double`, `inArrears: Boolean`, `dueDate: String?` | derived from the first `get_client_accounts.loanAccounts[]` row; `dueDate` has no wire source (confirmed gap) |
| `MemberRoleInfo` | `role: MemberRole`, `groupId: Long`, `assignedDate: String` | `get_member_role` datatable row (response is `type: array`); reuses `MemberRole` (no new enum) |
| `UpdateMemberRoleRequest` | `role: MemberRole`, `groupId: Long`, `assignedDate: String` | `update_member_role` PUT body; reuses `MemberRole` |
| `UpdateMemberRoleResult` | `resourceId: Long` | `update_member_role` result |
| `LoanSummary` | `id: Long`, `memberId: Long`, `memberName: String`, `memberPhotoUrl: String?`, `loanProductName: String`, `principalAmount: Double`, `outstandingBalance: Double`, `overdueAmount: Double`, `status: LoanAccountStatus`, `nextRepaymentDate: String?`, `isOverdue: Boolean`, `fineractLoanId: Long` | CANONICAL loan-list row (`GET /groups/{groupId}/loans`), also reused by loan-detail + loan dialogs + personal-loans; see registry-divergence note below |
| `LoanPage` | `totalFilteredRecords: Int`, `loans: List<LoanSummary>` | offset-paginated envelope (`page_size=20`) |
| `LoanAccountStatus` | enum: `ACTIVE`, `OVERDUE`, `CLOSED`, `PENDING`, `REJECTED`, `UNKNOWN` | mirrors wire `LoanAccountStatusDto` 1:1; NOT unified with `LoanStatus` (member-list's per-member loan-status chip — mismatched value-set) — see `LoanSummary.kt` kdoc |
| `LoanStatusFilter` | enum: `ALL`, `ACTIVE`, `OVERDUE`, `CLOSED` | loan-list status-filter chips; pure client-side UI state, no wire counterpart |

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
exists for this feature — `api.yaml` is the sole SoT, per PP-1);
`idea-layer/screens/member-list/api.yaml` (`GET /groups/{groupId}/clients`,
offset-paginated, `dtos.Member` — a DIFFERENT, richer `idea-layer/dtos/MemberDto.yaml`
registry entry (v2.0.0) also exists for the SAME list endpoint but was NOT used
as the generation SoT; see the registry-divergence note below);
`idea-layer/screens/member-profile/api.yaml` (3 parallel reads — `get_client`,
`get_client_accounts`, `get_member_role` — + 1 write `update_member_role`; no
dedicated `idea-layer/dtos/{Dto}.yaml` registry entry exists for this feature
— `api.yaml` is the sole SoT, per PP-1);
`idea-layer/screens/loan-list/{api.yaml,ui.yaml}` (`GET
/groups/{groupId}/loans`, offset-paginated, `page_size=20`; `api.yaml#dtos.LoanSummary`
is the SoT used here — a DIFFERENT `idea-layer/dtos/LoanDto.yaml` registry
entry also exists but describes a different endpoint/consumer set, see
divergence note below).

**`LoanSummary` vs `idea-layer/dtos/LoanDto.yaml` registry divergence
(flagged for the cross-feature repair station, same pattern as the `Member`
divergence above):** the registry entry (v1.0.0) declares a DIFFERENT
`LoanDto` shape (`principal`, `interestRate`, `status: String` lowercase
lifecycle values, `disbursedOn`/`expectedMaturityDate`/`amountRepaid`/
`amountOutstanding`) sourced from `GET /loans/{loanId}` +
`GET /loans?groupId={groupId}`, with `used_by: loan-management,
end-user-dashboard` — NOT `loan-list`. `LoanSummary` here was generated from
loan-list's own approved `api.yaml#dtos.LoanSummary` instead (`GET
/groups/{groupId}/loans`, a different endpoint) — full note in
`core/network/model/API.md`.

**`LoanAccountStatus` naming collision with `LoanStatus` (flagged for the
cross-feature repair station):** member-list's `LoanStatus` (`Member.kt`) is
a per-member loan-status chip (`ACTIVE`/`NONE`/`OVERDUE`/`UNKNOWN`);
loan-list's own registry declares a per-loan lifecycle status
(`ACTIVE`/`OVERDUE`/`CLOSED`/`PENDING`/`REJECTED`). Neither value-set is a
subset of the other, and the bare name `LoanStatus` was already taken by a
different concept in the SAME package (`org.mifos.groupbanking.core.model`)
— `LoanAccountStatus` was introduced instead of forcing reuse or a symbol
clash. Resolve at Station 3.

**`MemberProfile` vs canonical `Member` field-shape divergence (flagged for
the cross-feature repair station, same "forcing reuse would require
fabricating values" precedent as `GroupDetail` vs `Group`):** the generation
brief instructed reusing `Member`/`MemberRole` from member-list, but
`get_client`'s actual response (`id`, `displayName`, `firstname`, `lastname`,
`mobileNo`, `imagePresent`, `status`, `activationDate`, `officeId`) carries
NONE of `Member`'s non-null-required `role`/`savingsBalance`/`loanStatus` and
DOES carry 6 fields `Member` doesn't have. A THIRD shape also exists in the
SAME `api.yaml` file — the abbreviated `dtos.Member` block (`id: String`,
`firstName`/`lastName`/`phone`/`photoUri`/`joinDate`/`status: String`) —
which matches neither `get_client`'s literal response nor member-list's
`Member`. `MemberProfile` was introduced (named after this feature's own
`MemberRepository.getMemberProfile(...)` method) rather than forcing any of
the three mismatched shapes into one — resolve all three at Station 3.
`MemberRole` (the enum, not `Member` the row) WAS reused outright for
`MemberRoleInfo`/`UpdateMemberRoleRequest` — its value-set is an exact match.

**`MemberAccounts` client-side aggregation (flagged for the cross-feature
repair station, same "constructed in GroupRepository" precedent as
`GroupConfig`):** `get_client_accounts`' literal response is
`savingsAccounts: List<{id,productName,accountNo,balance,status}>` +
`loanAccounts: List<{id,productName,accountNo,status,summary}>` — an array of
raw accounts, not the aggregated `savingsBalance`/`savingsHistory`/
`activeLoan` shape `api.yaml#dtos.MemberAccounts` declares. The domain
`MemberAccounts` model matches the declared aggregated shape (the feature's
own registry SoT); the mapper derives `savingsBalance` (sum of balances) and
`activeLoan` (first loan account, `inArrears = totalOverdue > 0.0`) from the
raw arrays. `savingsHistory` (weekly sparkline) has NO wire source anywhere
in `api.yaml` — mapped to `emptyList()` until a real time-series endpoint
exists (confirmed gap, same class as `GroupConfig.shareMin`).

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

**`Member` vs `idea-layer/dtos/MemberDto.yaml` registry divergence (flagged
for the cross-feature repair station — same pattern as the `GroupDto.yaml`
and `SavingsTransactionDto.yaml` divergences above):** the registry entry
(v2.0.0) declares a DIFFERENT `MemberDto` shape (`id: Long`, `roleInGroup:
String` with lowercase values `organizer`/`treasurer`/`chairperson`/`secretary`/`member`,
`status: String` Fineract client status, `imageId: Long?`, `savingsAccountId:
Long?`, `joinedDate: String?`) sourced from `GET /clients/{clientId}` +
`list_endpoint: GET /groups/{groupId}/clients` (the SAME list endpoint as
this feature), yet its `used_by` lists only `member-onboarding` +
`meeting-lifecycle` — NOT `member-list`. It carries no `loanStatus`/
`savingsBalance`/role-badge concept at all, whereas member-list's own
approved `api.yaml#dtos.Member` explicitly declares `role: MemberRole` +
`loanStatus: LoanStatus` (the role-badge + loan-status shape this feature's
generation brief requested). `Member` here was generated from member-list's
OWN `api.yaml`, matching the established precedent of trusting the
declaring feature's approved contract over a registry entry that neither
matches its fields nor lists the feature as a consumer — resolve the two
`MemberDto` declarations at Station 3.

Wire counterparts + `@SerialName` mapping: see `core/network/model/API.md`
(includes a registry-divergence note re: `idea-layer/dtos/GroupDto.yaml`, a
THREE-way naming-collision note re: `SavingsTransaction` /
`idea-layer/dtos/SavingsTransactionDto.yaml` /
`idea-layer/screens/personal-savings/api.yaml`, the group-create enum
reuse-vs-new-enum rationale + `OfficeDto.externalId` / `PayoutOrderMethodDto`
value-set notes, the group-dashboard `GroupDetail`/`GroupInstanceConfig`
divergence + collision notes above, and the `idea-layer/dtos/MemberDto.yaml`
registry-divergence note for `Member`).
<!-- kmp-dto-gen:END -->
