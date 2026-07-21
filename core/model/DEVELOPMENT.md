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

## 3. Consumers

- UseCases (`core/domain`)
- ViewModels (`feature/*`)
- Repositories (`core/data` — `AuthRepositoryImpl` / `GroupRepositoryImpl` map
  `core/network/model` DTOs through `core/network/mapper` into these domain
  types before returning them; the join-with-code repository maps
  `JoinWithCodeMappers.kt` output the same way)

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

## 6. Errors

`core/model` types carry no error/exception state — parse failures (e.g. a
malformed `tokenExpiresAt` ISO-8601 string, or `Group.lastMeetingDate`'s
`LocalDate.parse`) surface as exceptions at the mapper boundary
(`core/network/mapper`), not here.

## 7. Testing

Domain models are pure data classes exercised indirectly via the mapper test
suites (`core/network/src/commonTest/.../mapper/LoginSignupMappersTest.kt`,
`GroupTypeConfigMappersTest.kt`, `GroupMappersTest.kt`,
`JoinWithCodeMappersTest.kt`), which assert every field is mapped and equality
holds end to end. `HealthIndicator.fromOverdueRate` additionally has direct
boundary-value tests in `GroupMappersTest.kt`; `Invitation.isExpired` /
`isAlreadyUsed` have direct boundary-value tests in `JoinWithCodeMappersTest.kt`
(before-expiry / at-exact-expiry / after-expiry, plus null-vs-non-null
`acceptedAt`).

## 8. Observability

Never log `LoginCredentials.password` or `AuthSession.sessionToken` — both
carry sensitive auth material. `Invitation.invitedEmailPhone` carries PII —
avoid logging it.

## 9. Evolution

New auth-related domain concepts are added to `AuthModels.kt`; unrelated
feature domains get their own `{Feature}.kt` file in this module (one file
per feature, per `kmp-dto-gen`'s Step 3 convention). Join-with-code's domain
concepts live in `Invitation.kt`.
<!-- kmp-dto-gen:END -->
