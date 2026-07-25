/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.serialization.json.Json
import org.mifos.groupbanking.core.model.MemberPayout
import org.mifos.groupbanking.core.model.RotationPayoutExecuteResult
import org.mifos.groupbanking.core.model.RotationPayoutRequest
import org.mifos.groupbanking.core.model.ShareOutExecuteRequest
import org.mifos.groupbanking.core.model.ShareOutExecuteResult
import org.mifos.groupbanking.core.model.ShareOutPreview
import org.mifos.groupbanking.core.network.model.MemberPayoutDto
import org.mifos.groupbanking.core.network.model.MemberPayoutRequestDto
import org.mifos.groupbanking.core.network.model.RotationPayoutExecuteRequestDto
import org.mifos.groupbanking.core.network.model.RotationPayoutExecuteResponseDto
import org.mifos.groupbanking.core.network.model.ShareOutExecuteRequestDto
import org.mifos.groupbanking.core.network.model.ShareOutExecuteResponseDto
import org.mifos.groupbanking.core.network.model.ShareOutPreviewDto

/**
 * DTO -> domain mappers for the share-out-preview feature (`ShareOutDto.kt`). Every field on every
 * DTO is mapped — no field left unmapped. [ShareOutPreviewDto.memberPayouts] `null` (ROTATING_PAYOUT)
 * maps to a `null` domain list (distinct from an empty ACCUMULATING list), preserving the pool-model
 * discriminator semantics downstream.
 */

fun MemberPayoutDto.toDomainModel(): MemberPayout = MemberPayout(
    memberId = memberId,
    memberName = memberName,
    sharesHeld = sharesHeld,
    totalSavings = totalSavings,
    sharePercent = sharePercent,
    payoutAmount = payoutAmount,
)

/** Batch converter — maps every member payout row in declaration order. */
fun List<MemberPayoutDto>.toShareOutMemberPayouts(): List<MemberPayout> = map { it.toDomainModel() }

fun ShareOutPreviewDto.toDomainModel(): ShareOutPreview = ShareOutPreview(
    cycleNumber = cycleNumber,
    poolModel = poolModel,
    shareoutFormula = shareoutFormula,
    totalCorpus = totalCorpus,
    totalProfit = totalProfit,
    totalPool = totalPool,
    memberPayouts = memberPayouts?.toShareOutMemberPayouts(),
    rotationPosition = rotationPosition,
    nextRecipientName = nextRecipientName,
    nextRecipientAmount = nextRecipientAmount,
)

// -----------------------------------------------------------------------------------------------
// share-out-execute (COMP-DIST-001 / COMP-DIST-002) — domain <-> wire mappers + sync_queue payloads
// -----------------------------------------------------------------------------------------------

/**
 * `MemberPayout` (domain, preview row) -> [MemberPayoutRequestDto] (narrow execute-request wire
 * shape). Only the three fields the companion needs to drain each savings withdrawal are sent — the
 * display fields (`memberName`/`sharesHeld`/`totalSavings`) are dropped.
 */
fun MemberPayout.toRequestDto(): MemberPayoutRequestDto = MemberPayoutRequestDto(
    memberId = memberId,
    payoutAmount = payoutAmount,
    sharePercent = sharePercent,
)

/** [ShareOutExecuteRequest] (domain) -> [ShareOutExecuteRequestDto] (COMP-DIST-001 request body). */
fun ShareOutExecuteRequest.toDto(): ShareOutExecuteRequestDto = ShareOutExecuteRequestDto(
    cycleNumber = cycleNumber,
    totalPool = totalPool,
    memberPayouts = memberPayouts.map { it.toRequestDto() },
    shareoutFormula = shareoutFormula,
    executedAt = executedAt,
)

/** [ShareOutExecuteResponseDto] (COMP-DIST-001 response) -> [ShareOutExecuteResult] (domain). */
fun ShareOutExecuteResponseDto.toDomainModel(): ShareOutExecuteResult = ShareOutExecuteResult(
    shareoutRecordId = shareoutRecordId,
    succeededCount = succeededCount,
    failedCount = failedCount,
    failedMemberIds = failedMemberIds,
)

/** [RotationPayoutRequest] (domain) -> [RotationPayoutExecuteRequestDto] (COMP-DIST-002 request body). */
fun RotationPayoutRequest.toDto(): RotationPayoutExecuteRequestDto = RotationPayoutExecuteRequestDto(
    recipientMemberId = recipientMemberId,
    amount = amount,
    payoutOrderMethod = payoutOrderMethod,
    executedAt = executedAt,
)

/** [RotationPayoutExecuteResponseDto] (COMP-DIST-002 response) -> [RotationPayoutExecuteResult] (domain). */
fun RotationPayoutExecuteResponseDto.toDomainModel(): RotationPayoutExecuteResult = RotationPayoutExecuteResult(
    rotationRecordId = rotationRecordId,
    newRotationPosition = newRotationPosition,
    nextRecipientId = nextRecipientId,
)

/**
 * Server-parity Json config for the offline `sync_queue` payload column — mirrors `NetworkModule`'s
 * client config (`ignoreUnknownKeys` + `encodeDefaults`), same convention as
 * `LoanRequestMappers.kt#syncQueueJson`. Used to serialize an execute request into the opaque
 * `SyncQueueItem.payloadJson` when the device is offline (`data-flow.yaml#offline_behavior`), and to
 * decode it back for the background sync-worker drain.
 */
private val shareOutSyncQueueJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Serializes a COMP-DIST-001 execute request to the `sync_queue` payload string (offline enqueue). */
fun ShareOutExecuteRequestDto.toJsonPayload(): String =
    shareOutSyncQueueJson.encodeToString(ShareOutExecuteRequestDto.serializer(), this)

/** Reverse of [toJsonPayload] — decodes a queued `SyncQueueItem.payloadJson` for offline drain/retry. */
fun shareOutExecuteRequestDtoFromJson(json: String): ShareOutExecuteRequestDto =
    shareOutSyncQueueJson.decodeFromString(ShareOutExecuteRequestDto.serializer(), json)

/** Serializes a COMP-DIST-002 rotation request to the `sync_queue` payload string (offline enqueue). */
fun RotationPayoutExecuteRequestDto.toJsonPayload(): String =
    shareOutSyncQueueJson.encodeToString(RotationPayoutExecuteRequestDto.serializer(), this)

/** Reverse of [toJsonPayload] — decodes a queued rotation `SyncQueueItem.payloadJson` for drain/retry. */
fun rotationPayoutExecuteRequestDtoFromJson(json: String): RotationPayoutExecuteRequestDto =
    shareOutSyncQueueJson.decodeFromString(RotationPayoutExecuteRequestDto.serializer(), json)
