/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for `get_client` — `GET /clients/{clientId}` (raw Fineract client resource,
 * `idea-layer/screens/member-profile/api.yaml#api[0]`). Carries the member-profile screen's
 * identity/join-date/phone header (`stale-while-revalidate`, `ttl=300`, offline show-cached).
 *
 * **Deliberately NOT the canonical member-list `MemberDto`** (flagged for the cross-feature
 * repair station, same "forcing reuse would require fabricating values" precedent as
 * `GroupDetailDto` vs `GroupDto` — see `GroupDashboardDto.kt`): `MemberDto` (member-list row) is
 * `id: String, fineractClientId: Long, displayName: String, photoUri: String?, role:
 * MemberRoleDto, savingsBalance: Double, loanStatus: LoanStatusDto` — this endpoint returns
 * NONE of `role`/`savingsBalance`/`loanStatus` and DOES return `firstname`/`lastname`/
 * `mobileNo`/`imagePresent`/`status`/`activationDate`/`officeId`, which `MemberDto` doesn't
 * carry. Forcing `MemberDto` reuse here would mean fabricating a role/balance/loan-status this
 * op never returns (Hard Rule 4 forbids inventing undeclared fields) — `MemberProfileDto` was
 * introduced instead, named after the `MemberRepository.getMemberProfile(...)` method this
 * feature's own `api.yaml#dependencies.repositories` declares.
 *
 * **`api.yaml#dtos.Member` in-file divergence (ALSO flagged, same file, distinct from the
 * cross-feature divergence above):** the abbreviated `dtos.Member` registry block declares a
 * THIRD shape (`id: String`, `firstName`/`lastName`/`phone`/`joinDate` camelCase, `photoUri:
 * String?`, `status: String`) that matches neither this operation's literal response nor
 * member-list's `MemberDto`. This DTO's `@SerialName`s mirror the literal `get_client` operation
 * response per Hard Rule 5 (wire truth over an abbreviated registry summary) — `firstname`/
 * `lastname` are genuine Fineract API casing (lowercase `n`), NOT a typo. The abbreviated
 * `dtos.Member` block's friendlier field names (`firstName`/`lastName`/`phone`/`joinDate`)
 * informed the DOMAIN model's naming instead (see `MemberProfile.kt` kdoc) — resolve the
 * three-way shape mismatch at Station 3.
 *
 * See API.md#dtos — MemberProfile.
 */
@Serializable
data class MemberProfileDto(
    @SerialName("id") val id: Long,
    @SerialName("displayName") val displayName: String,
    @SerialName("firstname") val firstName: String,
    @SerialName("lastname") val lastName: String,
    @SerialName("mobileNo") val mobileNo: String,
    @SerialName("imagePresent") val imagePresent: Boolean,
    @SerialName("status") val status: FineractStatusDto,
    @SerialName("activationDate") val activationDate: String,
    @SerialName("officeId") val officeId: Long,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the nested Fineract `{id, value}` status pair — appears verbatim on `get_client`
 * (`status`), and on each `get_client_accounts` savings/loan account row (`status`). ONE shared
 * type rather than three near-identical duplicates, since all three are literally the same
 * `{id: Int, value: String}` shape declared by `api.yaml`.
 *
 * See API.md#dtos — FineractStatus.
 */
@Serializable
data class FineractStatusDto(
    @SerialName("id") val id: Int,
    @SerialName("value") val value: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `get_client_accounts` — `GET /clients/{clientId}/accounts` (`stale-while-revalidate`,
 * `ttl=120`). Carries the LITERAL raw response shape: an array of savings accounts and an array
 * of loan accounts.
 *
 * **`api.yaml#dtos.MemberAccounts` divergence (flagged for the cross-feature repair station):**
 * the registry block declares a DIFFERENT, client-aggregated shape — `savingsBalance: Double`
 * (a single scalar), `savingsHistory: List<SavingsDataPoint>` (a weekly balance sparkline), and
 * `activeLoan: ActiveLoanSummary?` (a single derived loan) — none of which this operation
 * returns directly. This DTO mirrors the LITERAL `get_client_accounts` response per Hard Rule 5;
 * the aggregated `dtos.MemberAccounts` shape is emitted as the DOMAIN model instead (see
 * `MemberProfile.kt` kdoc), derived from this DTO's arrays in `MemberProfileMappers.kt`.
 * [SavingsDataPoint]'s weekly sparkline has NO wire source anywhere in `api.yaml` (confirmed
 * gap, same class as `GroupConfigDto.shareMin` — mapped to an empty list until a real
 * time-series endpoint exists).
 *
 * See API.md#dtos — MemberAccounts.
 */
@Serializable
data class MemberAccountsDto(
    @SerialName("savingsAccounts") val savingsAccounts: List<MemberSavingsAccountDto> = emptyList(),
    @SerialName("loanAccounts") val loanAccounts: List<MemberLoanAccountDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of `MemberAccountsDto.savingsAccounts`.
 *
 * See API.md#dtos — MemberSavingsAccount.
 */
@Serializable
data class MemberSavingsAccountDto(
    @SerialName("id") val id: Long,
    @SerialName("productName") val productName: String,
    @SerialName("accountNo") val accountNo: String,
    @SerialName("balance") val balance: Double,
    @SerialName("status") val status: FineractStatusDto,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of `MemberAccountsDto.loanAccounts`.
 *
 * See API.md#dtos — MemberLoanAccount.
 */
@Serializable
data class MemberLoanAccountDto(
    @SerialName("id") val id: Long,
    @SerialName("productName") val productName: String,
    @SerialName("accountNo") val accountNo: String,
    @SerialName("status") val status: FineractStatusDto,
    @SerialName("summary") val summary: MemberLoanAccountSummaryDto,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `MemberLoanAccountDto.summary` — used by the mapper to derive
 * `ActiveLoanSummary.inArrears` (`totalOverdue > 0.0`) and `.outstandingBalance`
 * (`principalOutstanding`).
 *
 * See API.md#dtos — MemberLoanAccountSummary.
 */
@Serializable
data class MemberLoanAccountSummaryDto(
    @SerialName("principalDisbursed") val principalDisbursed: Double,
    @SerialName("principalOutstanding") val principalOutstanding: Double,
    @SerialName("totalOverdue") val totalOverdue: Double,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of `get_member_role` — `GET /datatables/dt_member_role/{clientId}`
 * (response `type: array`, `stale-while-revalidate`, `ttl=300`, offline show-cached). [role]
 * reuses the SHARED `MemberRoleDto` enum (declared in `MemberDto.kt`, member-list) — its 4
 * known values (`CHAIRPERSON`/`TREASURER`/`SECRETARY`/`MEMBER`) match
 * `api.yaml#dtos.MemberRole.values` for THIS feature exactly (same enum, no duplicate).
 *
 * **`api.yaml#dependencies.repositories.MemberRepository.getMemberRole` signature note (flagged
 * for the cross-feature repair station):** the declared repository method returns
 * `Flow<MemberRole>` (the bare enum), but the operation's own response is `type: array` of
 * `{role, groupId, assignedDate}` rows — collapsing an array to a single enum (presumably
 * "first row" or "row for the currently-viewed group") is a repository-layer decision out of
 * this DTO/mapper's scope. This DTO/mapper preserves the full row (`role` + `groupId` +
 * `assignedDate`) per Hard Rule 4 ("all fields mapped") rather than silently dropping
 * `groupId`/`assignedDate` to match the narrower repository signature.
 *
 * See API.md#dtos — MemberRoleInfo.
 */
@Serializable
data class MemberRoleInfoDto(
    @SerialName("role") val role: MemberRoleDto = MemberRoleDto.UNKNOWN,
    @SerialName("groupId") val groupId: Long,
    @SerialName("assignedDate") val assignedDate: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the `update_member_role` request body — `PUT /datatables/dt_member_role/{clientId}`
 * (403 if the caller is not chairperson). [role] reuses the SHARED `MemberRoleDto` enum, same
 * rationale as [MemberRoleInfoDto.role].
 *
 * See API.md#dtos — UpdateMemberRoleRequest.
 */
@Serializable
data class UpdateMemberRoleRequestDto(
    @SerialName("role") val role: MemberRoleDto = MemberRoleDto.UNKNOWN,
    @SerialName("groupId") val groupId: Long,
    @SerialName("assignedDate") val assignedDate: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the `update_member_role` response — `{resourceId: Long}`.
 *
 * See API.md#dtos — UpdateMemberRoleResponse.
 */
@Serializable
data class UpdateMemberRoleResponseDto(
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
