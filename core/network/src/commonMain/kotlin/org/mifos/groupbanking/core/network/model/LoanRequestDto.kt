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
 * Wire request DTO for `submit_loan_request` (`POST /datatables/dt_loan_request`) — the
 * member-side loan-application form's submission body, matching `api.yaml#api[0].request_body`
 * verbatim. Creates a `PENDING` `dt_loan_request` datatable row for organizer review at the
 * next group meeting.
 *
 * [clientId] is camelCase per `api.yaml`'s own declared shape (the standard Fineract client
 * identifier passed alongside the datatable body, not a `dt_loan_request` column itself);
 * [requestedAmount]/[purpose]/[durationWeeks]/[savingsBalanceAtRequest]/[submittedAt]/[status]
 * are the literal snake_case `dt_loan_request` datatable columns (Hard Rule 5), same convention
 * as `InvitationRowDto`/`GroupCorpusRowDto`/`GroupLoanConfigDto`.
 *
 * [purpose] reuses the SHARED `LoanPurposeDto` (extended by this feature — see that type's
 * kdoc in `LoanApplyDto.kt` for the PP-1 registry-wins rationale). kotlinx.serialization encodes
 * an enum by its `@SerialName` string directly, so this field satisfies `api.yaml`'s literal
 * `purpose: String` wire contract with no wrapper object.
 *
 * [status] defaults to the literal `"PENDING"` constant `api.yaml#request_body.fields.status`
 * declares — every loan-request submission starts in this state; the domain `LoanRequestPayload`
 * carries no `status` field of its own (same "wire-only literal constant excluded from the pure
 * domain model" precedent as `ApplyLoanRequestDto`'s 5 lookup-pair defaults).
 *
 * **Offline queueing (`cache.offline: queue_to_syncqueue`):** when `cmp-network-monitor` reports
 * offline, `LoanRequestMappers.kt#toJsonPayload()` serializes this DTO to a JSON `String` for
 * `SyncQueueRepository.enqueue(...)` to persist as a `SyncQueueEntry.payload` row — the
 * `SyncQueueEntry`/`SyncQueueRepository` shapes themselves live outside this generation step
 * (declared in `api.yaml#dependencies.repositories`, same "repository lives outside this
 * generation step" precedent as `MemberAddRepository`'s kdoc), consistent with PP-1 (`api.yaml`
 * is the sole SoT for this feature — no dedicated `idea-layer/dtos/{Dto}.yaml` registry entry
 * exists).
 *
 * See API.md#dtos — LoanRequestPayload.
 */
@Serializable
data class LoanRequestPayloadDto(
    @SerialName("clientId") val clientId: Long,
    @SerialName("requested_amount") val requestedAmount: Double,
    @SerialName("purpose") val purpose: LoanPurposeDto,
    @SerialName("duration_weeks") val durationWeeks: Int,
    @SerialName("savings_balance_at_request") val savingsBalanceAtRequest: Double,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("status") val status: String = "PENDING",
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for `submit_loan_request` — the literal Fineract datatable resource-create
 * envelope, matching `api.yaml#api[0].response.fields` verbatim.
 *
 * See API.md#dtos — LoanRequestResponse.
 */
@Serializable
data class LoanRequestResponseDto(
    @SerialName("resourceId") val resourceId: Long,
    @SerialName("officeId") val officeId: Long,
    @SerialName("clientId") val clientId: Long,
    @SerialName("resourceExternalId") val resourceExternalId: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
