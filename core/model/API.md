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

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`
(contract refs COMP-AUTH-001, COMP-AUTH-002, COMP-AUTH-003);
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003).

Wire counterparts + `@SerialName` mapping: see `core/network/model/API.md`.
<!-- kmp-dto-gen:END -->
