<!-- generated-by: kmp-dto-gen -->
<!-- kmp-dto-gen:BEGIN -->
# core/network/model — API.md

## dtos

| DTO | `@SerialName` mapping | Nullability | Wire operation |
|---|---|---|---|
| `SelfRegisterRequestDto` | `name`, `emailPhone`, `password` | all non-null, required | `POST /companion/auth/self-register` (COMP-AUTH-001) |
| `LoginRequestDto` | `emailPhone`, `password` | all non-null, required | `POST /companion/auth/login` (COMP-AUTH-002) |
| `AuthResponseDto` | `userId`, `sessionToken`, `tokenExpiresAt`, `groupMemberships` (default `[]`) | `groupMemberships` defaults empty; others required | response of COMP-AUTH-001 + COMP-AUTH-002 |
| `UserProfileDto` | `userId`, `name`, `emailPhone`, `groupMemberships` (default `[]`) | `groupMemberships` defaults empty; others required | `GET /companion/auth/me` (COMP-AUTH-003) |
| `GroupMembershipDto` | `groupId`, `groupName`, `role` (default `UNKNOWN`), `joinedAt` | `role` defaults `UNKNOWN`; others required | nested in `AuthResponseDto` / `UserProfileDto` |
| `GroupRoleDto` | enum `@SerialName`: `ORGANIZER`, `MEMBER`, `TREASURER`, `SECRETARY`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback for any future server-added role | field of `GroupMembershipDto.role` |
| `GroupTypeConfigDto` | `typeSlug` (default `UNKNOWN`), `displayName`, `tagline`, `savingsMechanism` (default `UNKNOWN`), `contributionMode` (default `UNKNOWN`), `lendingEnabled`, `hasSocialFund`, `hasBankLinkage`, `welfareOnlyMode`, `formallyRegistered`, `defaultLoanMultiplier`, `defaultInterestRatePct`, `defaultCycleLengthMonths`, `maxMembers`, `minMembers` | 3 enum fields default `UNKNOWN`; remaining 12 non-null required | `GET /companion/datatables/group_type_config/{entityId}` (COMP-DT-003, entityId=0=seed catalogue) |
| `GroupTypeSlugDto` | enum `@SerialName`: `VSLA`, `ROSCA`, `ASCA`, `SILC`, `SHG`, `SACCO`, `CBO_VILLAGE_BANK`, `BURIAL_WELFARE`, `JLG`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback for any future server-added group type | field of `GroupTypeConfigDto.typeSlug` |
| `SavingsMechanismDto` | enum `@SerialName`: `ACCUMULATING`, `ROTATING_PAYOUT`, `NONE`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `GroupTypeConfigDto.savingsMechanism` |
| `ContributionModeDto` | enum `@SerialName`: `SHARE_BASED_VARIABLE`, `FIXED`, `MINIMAL`, `UNKNOWN` | `UNKNOWN` is the T7/EC30 fallback | field of `GroupTypeConfigDto.contributionMode` |

Source features: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`;
`idea-layer/screens/group-type-picker/{api.yaml,docs.yaml}` (COMP-DT-003).

Field-name casing precedent: `idea-layer/dtos/LoanDto.yaml` (camelCase wire
fields, e.g. `memberId`, `disbursedOn`) — the companion bridge returns
camelCase JSON, not raw Postgres snake_case, so every `@SerialName` here
matches `api.yaml#dtos` field names verbatim (no case translation).

Domain counterparts + field mapping: see `core/model/API.md`. DTO↔domain
mappers: `core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/mapper/LoginSignupMappers.kt`,
`GroupTypeConfigMappers.kt`.
<!-- kmp-dto-gen:END -->
