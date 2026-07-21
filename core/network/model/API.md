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

Source feature: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`.

Field-name casing precedent: `idea-layer/dtos/LoanDto.yaml` (camelCase wire
fields, e.g. `memberId`, `disbursedOn`) — the companion bridge returns
camelCase JSON, not raw Postgres snake_case, so every `@SerialName` here
matches `api.yaml#dtos` field names verbatim (no case translation).

Domain counterparts + field mapping: see `core/model/API.md`. DTO↔domain
mappers: `core/network/src/commonMain/kotlin/org/mifos/groupbanking/core/network/mapper/LoginSignupMappers.kt`.
<!-- kmp-dto-gen:END -->
