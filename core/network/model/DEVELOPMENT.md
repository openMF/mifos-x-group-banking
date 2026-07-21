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

## 3. Consumers

- Ktor Services (`core/network` — the companion auth service + companion
  datatables service + companion groups service built on these DTOs)
- Repository mappers (`core/network/mapper/LoginSignupMappers.kt` → `core/data`
  `AuthRepositoryImpl`; `core/network/mapper/GroupTypeConfigMappers.kt` →
  `core/data` `GroupTypeConfigRepositoryImpl`; `core/network/mapper/GroupMappers.kt`
  → `core/data` `GroupRepositoryImpl`)

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

## 6. Errors

Malformed JSON or an unrecognized required field raises
`kotlinx.serialization.SerializationException`, caught by the shared Ktor
error-mapper (`core-base/network`, consumed not modified). Unknown *optional*
fields and unknown enum values are tolerated (never thrown) via the shared
`Json { ignoreUnknownKeys = true; coerceInputValues = true }` config wired in
`NetworkModule` (emitted by `kmp-client-gen`).

## 7. Testing

`core/network/src/commonTest/.../model/LoginSignupDtoTest.kt`,
`GroupTypeConfigDtoTest.kt`, and `GroupDtoTest.kt` — construction,
serialization round-trip, default-value, and equality tests per DTO, plus a
T7/EC30 cross-version fixture proving an old client tolerates a
server-added field + a server-added enum value without crashing.

## 8. Observability

Never log `SelfRegisterRequestDto.password`, `LoginRequestDto.password`, or
`AuthResponseDto.sessionToken` — all carry sensitive auth material.
`GroupTypeConfigDto` and `GroupDto` carry no sensitive fields.

## 9. Evolution

Bump the affected DTO's `SCHEMA_VERSION` when its shape changes; add new enum
values above `UNKNOWN` (never remove existing entries) to keep old clients
decoding safely.
<!-- kmp-dto-gen:END -->
