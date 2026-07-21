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

Source feature: `idea-layer/screens/login-signup/{api.yaml,docs.yaml,flow.yaml}`
(contract refs COMP-AUTH-001, COMP-AUTH-002, COMP-AUTH-003).

Wire counterparts + `@SerialName` mapping: see `core/network/model/API.md`.
<!-- kmp-dto-gen:END -->
