/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for one member row of the ACCUMULATING share-out distribution preview
 * (`idea-layer/screens/share-out-preview/api.yaml#dtos.MemberPayout`, COMP-DIST-001). [sharesHeld]
 * is present only for the `PRORATA_SHARES` formula, [totalSavings] only for `PRORATA_SAVINGS` —
 * both nullable with a `null` default so a payload carrying only one of them (or neither, for
 * `EQUAL`) deserializes cleanly. Present in [ShareOutPreviewDto.memberPayouts] for ACCUMULATING
 * pool models; that array is `null` for ROTATING_PAYOUT.
 *
 * See API.md#dtos — MemberPayout.
 */
@Serializable
data class MemberPayoutDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("memberName") val memberName: String,
    @SerialName("sharesHeld") val sharesHeld: Int? = null,
    @SerialName("totalSavings") val totalSavings: Double? = null,
    @SerialName("sharePercent") val sharePercent: Double,
    @SerialName("payoutAmount") val payoutAmount: Double,
)

/**
 * Wire DTO for the `GET /companion/groups/{groupId}/shareout/preview` response (COMP-DIST-001
 * preview) — the strategy-aware distribution preview. [poolModel] (`ACCUMULATING` |
 * `ROTATING_PAYOUT`) discriminates which strategy-specific payload is populated: [memberPayouts]
 * for ACCUMULATING types (VSLA/ASCA/SHG/SILC), or
 * [rotationPosition]/[nextRecipientName]/[nextRecipientAmount] for ROTATING_PAYOUT types
 * (ROSCA/chit). [memberPayouts] defaults to `null` (absent for ROTATING_PAYOUT); the rotation
 * fields default to `null` (absent for ACCUMULATING). Replaces the raw
 * `/groups/{groupId}/accounts` + `dt_share_out` datatable calls.
 *
 * See API.md#dtos — ShareOutPreview.
 */
@Serializable
data class ShareOutPreviewDto(
    @SerialName("cycleNumber") val cycleNumber: Int,
    @SerialName("poolModel") val poolModel: String,
    @SerialName("shareoutFormula") val shareoutFormula: String,
    @SerialName("totalCorpus") val totalCorpus: Double,
    @SerialName("totalProfit") val totalProfit: Double,
    @SerialName("totalPool") val totalPool: Double,
    @SerialName("memberPayouts") val memberPayouts: List<MemberPayoutDto>? = null,
    @SerialName("rotationPosition") val rotationPosition: Int? = null,
    @SerialName("nextRecipientName") val nextRecipientName: String? = null,
    @SerialName("nextRecipientAmount") val nextRecipientAmount: Double? = null,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for one member row of the ACCUMULATING share-out EXECUTE request
 * (`share-out-execute/api.yaml#dtos.MemberPayoutRequest`, COMP-DIST-001). A deliberately narrower
 * shape than [MemberPayoutDto] (the read/preview row) — only the three fields the companion needs to
 * drain each savings withdrawal are sent (`memberId`, `payoutAmount`, `sharePercent`); the display
 * fields (`memberName`, `sharesHeld`, `totalSavings`) are preview-only and never posted.
 *
 * See API.md#dtos — MemberPayoutRequest.
 */
@Serializable
data class MemberPayoutRequestDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("payoutAmount") val payoutAmount: Double,
    @SerialName("sharePercent") val sharePercent: Double,
)

/**
 * Wire DTO for the `POST /companion/groups/{groupId}/shareout/execute` request body
 * (`share-out-execute/api.yaml#api.post_shareout_execute.body`, COMP-DIST-001). Replaces the legacy
 * `POST /datatables/dt_share_out/{groupId}` + N sequential `POST /savingsaccounts/{id}/transactions`
 * calls with a single companion transaction. [shareoutFormula] is echoed for the server-side audit;
 * [executedAt] is the ISO-8601 client execution timestamp.
 *
 * See API.md#dtos — ShareOutExecuteRequest.
 */
@Serializable
data class ShareOutExecuteRequestDto(
    @SerialName("cycleNumber") val cycleNumber: Int,
    @SerialName("totalPool") val totalPool: Double,
    @SerialName("memberPayouts") val memberPayouts: List<MemberPayoutRequestDto>,
    @SerialName("shareoutFormula") val shareoutFormula: String,
    @SerialName("executedAt") val executedAt: String,
)

/**
 * Wire DTO for the COMP-DIST-001 execute response
 * (`share-out-execute/api.yaml#api.post_shareout_execute.response`). [failedMemberIds] defaults to
 * an empty list so a fully-successful response (server may omit the field) deserializes cleanly and
 * derives the Success (not PartialFailure) state.
 *
 * See API.md#dtos — ShareOutExecuteResponse.
 */
@Serializable
data class ShareOutExecuteResponseDto(
    @SerialName("shareoutRecordId") val shareoutRecordId: String,
    @SerialName("succeededCount") val succeededCount: Int,
    @SerialName("failedCount") val failedCount: Int,
    @SerialName("failedMemberIds") val failedMemberIds: List<String> = emptyList(),
)

/**
 * Wire DTO for the `POST /companion/groups/{groupId}/rotation/execute` request body
 * (`share-out-execute/api.yaml#api.post_rotation_payout_execute.body`, COMP-DIST-002). The
 * single-recipient ROTATING_PAYOUT (ROSCA/chit) call — advances `rosca_rotation` and drains the one
 * savings withdrawal. [payoutOrderMethod] is the `shareoutFormula` (`FIXED_ORDER`/`LOTTERY`/`AUCTION`).
 *
 * See API.md#dtos — RotationPayoutExecuteRequest.
 */
@Serializable
data class RotationPayoutExecuteRequestDto(
    @SerialName("recipientMemberId") val recipientMemberId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("payoutOrderMethod") val payoutOrderMethod: String,
    @SerialName("executedAt") val executedAt: String,
)

/**
 * Wire DTO for the COMP-DIST-002 rotation execute response
 * (`share-out-execute/api.yaml#api.post_rotation_payout_execute.response`). [nextRecipientId]
 * defaults to `null` (the final round has no next recipient).
 *
 * See API.md#dtos — RotationPayoutExecuteResponse.
 */
@Serializable
data class RotationPayoutExecuteResponseDto(
    @SerialName("rotationRecordId") val rotationRecordId: String,
    @SerialName("newRotationPosition") val newRotationPosition: Int,
    @SerialName("nextRecipientId") val nextRecipientId: String? = null,
)
