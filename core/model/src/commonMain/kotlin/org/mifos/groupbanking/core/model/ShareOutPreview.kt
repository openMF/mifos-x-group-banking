/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Domain model for one member's row in the strategy-aware share-out distribution preview
 * (`idea-layer/screens/share-out-preview/api.yaml#dtos.MemberPayout`, COMP-DIST-001). Present
 * only for ACCUMULATING pool models (VSLA/ASCA/SHG/SILC); the list is empty for ROTATING_PAYOUT
 * (ROSCA/chit) types, which render a single next-recipient card instead.
 *
 * [sharesHeld] is populated only for the `PRORATA_SHARES` formula (VSLA/SILC — shares × share
 * value contributed); [totalSavings] only for `PRORATA_SAVINGS` (ASCA/SHG — total savings in
 * KES). Exactly one of the two is meaningful per formula, mirroring the mutually-exclusive
 * nullable-field-group convention `MemberGroupSavingsRow` uses (`sharesHeld`/`shareValue` vs
 * `totalContributed`). [payoutAmount] is the member's KES share of the total distribution pool.
 *
 * See API.md#models — MemberPayout.
 */
data class MemberPayout(
    val memberId: String,
    val memberName: String,
    val sharesHeld: Int?,
    val totalSavings: Double?,
    val sharePercent: Double,
    val payoutAmount: Double,
)

/**
 * Domain model for the strategy-aware share-out distribution preview computed by the companion
 * API (`GET /companion/groups/{groupId}/shareout/preview`, COMP-DIST-001). Replaces the raw
 * `/centers/{centerId}/accounts` + `dt_share_out` datatable calls.
 *
 * The [poolModel] discriminator (`ACCUMULATING` | `ROTATING_PAYOUT`) selects which strategy-
 * specific payload is meaningful: [memberPayouts] (per-member payout table) for ACCUMULATING
 * types, or [rotationPosition]/[nextRecipientName]/[nextRecipientAmount] (rotation next-recipient
 * card) for ROTATING_PAYOUT types. [shareoutFormula] (`PRORATA_SHARES` | `PRORATA_SAVINGS` |
 * `EQUAL` | `FIXED_ORDER` | `LOTTERY` | `AUCTION`) drives the formula chip label and, for the
 * ACCUMULATING table, whether the second column shows shares-held or savings-contribution.
 *
 * [poolModel]/[shareoutFormula] are kept as `String` (not enums) verbatim per
 * `api.yaml#response.fields` — the companion server is authoritative for the value-set and a
 * server-added value must never crash an old client (T7/EC30). [totalPool] is the sum of
 * [totalCorpus] (accumulated fund) + [totalProfit] (interest earned) for ACCUMULATING types, or
 * the fixed rotation pot for ROTATING_PAYOUT types.
 *
 * See API.md#models — ShareOutPreview.
 */
data class ShareOutPreview(
    val cycleNumber: Int,
    val poolModel: String,
    val shareoutFormula: String,
    val totalCorpus: Double,
    val totalProfit: Double,
    val totalPool: Double,
    val memberPayouts: List<MemberPayout>?,
    val rotationPosition: Int?,
    val nextRecipientName: String?,
    val nextRecipientAmount: Double?,
) {
    companion object {
        /** Discriminator value for the rotation (ROSCA/chit) strategy — next-recipient card path. */
        const val POOL_MODEL_ROTATING_PAYOUT: String = "ROTATING_PAYOUT"

        /** Discriminator value for the accumulating (VSLA/ASCA/SHG/SILC) strategy — payout-table path. */
        const val POOL_MODEL_ACCUMULATING: String = "ACCUMULATING"
    }
}

/**
 * Per-member execution status of one payout row during the irreversible share-out execution
 * (`idea-layer/screens/share-out-execute/api.yaml#dtos.MemberExecutionStatus`). Streamed by
 * `ShareOutExecuteViewModel` as the COMP-DIST-001 response resolves: every payout starts [PENDING],
 * flips to [IN_PROGRESS] the moment the batch POST is dispatched, then settles to [DONE] (present in
 * neither `failedMemberIds`) or [FAILED] (present in `failedMemberIds`). [QUEUED] is the offline
 * terminal — every row is marked [QUEUED] when the whole request is serialized to the HIGH-priority
 * `sync_queue` instead of being sent (`data-flow.yaml#offline_behavior`).
 *
 * See API.md#models — MemberExecutionStatus.
 */
enum class MemberExecutionStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    FAILED,
    QUEUED,
}

/**
 * Domain request for the ACCUMULATING share-out execution
 * (`POST /companion/groups/{groupId}/shareout/execute`, COMP-DIST-001,
 * `api.yaml#api.post_shareout_execute.body`). Carries the same [memberPayouts] the preview computed
 * (only `memberId`/`payoutAmount`/`sharePercent` are sent on the wire — see
 * `ShareOutMappers.kt#toRequestDto`); [shareoutFormula] is echoed for the server-side audit trail
 * and [executedAt] is the ISO-8601 client timestamp the companion records on `dt_share_out`.
 *
 * See API.md#models — ShareOutExecuteRequest.
 */
data class ShareOutExecuteRequest(
    val cycleNumber: Int,
    val totalPool: Double,
    val memberPayouts: List<MemberPayout>,
    val shareoutFormula: String,
    val executedAt: String,
)

/**
 * Domain result of a COMP-DIST-001 share-out execution
 * (`api.yaml#api.post_shareout_execute.response`). [failedMemberIds] drives the PartialFailure
 * state's per-row FAILED badges and the retry subset; [succeededCount] + [failedCount] feed the
 * completion / partial-failure banners.
 *
 * See API.md#models — ShareOutExecuteResult.
 */
data class ShareOutExecuteResult(
    val shareoutRecordId: String,
    val succeededCount: Int,
    val failedCount: Int,
    val failedMemberIds: List<String>,
)

/**
 * Domain request for the ROTATING_PAYOUT (ROSCA/chit) rotation execution
 * (`POST /companion/groups/{groupId}/rotation/execute`, COMP-DIST-002,
 * `api.yaml#api.post_rotation_payout_execute.body`). A single-recipient atomic call —
 * [recipientMemberId] is `memberPayouts[0]` from the preview, [payoutOrderMethod] is the
 * `shareoutFormula` (`FIXED_ORDER` | `LOTTERY` | `AUCTION`).
 *
 * See API.md#models — RotationPayoutRequest.
 */
data class RotationPayoutRequest(
    val recipientMemberId: String,
    val amount: Double,
    val payoutOrderMethod: String,
    val executedAt: String,
)

/**
 * Domain result of a COMP-DIST-002 rotation execution
 * (`api.yaml#api.post_rotation_payout_execute.response`). [newRotationPosition] + [nextRecipientId]
 * let the group-dashboard advance its rotation state on return (`OnDone`).
 *
 * See API.md#models — RotationPayoutExecuteResult.
 */
data class RotationPayoutExecuteResult(
    val rotationRecordId: String,
    val newRotationPosition: Int,
    val nextRecipientId: String?,
)
