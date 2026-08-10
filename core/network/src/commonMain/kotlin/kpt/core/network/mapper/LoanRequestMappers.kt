/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.mapper

import kotlinx.serialization.json.Json
import kpt.core.model.LoanRequestPayload
import kpt.core.model.LoanRequestResult
import kpt.core.network.model.LoanRequestPayloadDto
import kpt.core.network.model.LoanRequestResponseDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * DTO <-> domain mappers for the loan-request wire contract (`submit_loan_request` — `POST
 * /datatables/dt_loan_request`). Every field on every DTO declared in `LoanRequestDto.kt` is
 * mapped — no field left unmapped.
 *
 * [kpt.core.network.model.LoanPurposeDto]'s `toDto()`/`toDomainModel()`
 * mappers are the SHARED pair already declared in `LoanApplyMappers.kt` — reused here (same
 * package), not redefined.
 */

// ---------- domain -> request DTO ----------

/**
 * Domain -> wire request. [now] defaults via `kotlin.time.Clock` (NOT the deprecated
 * `kotlinx.datetime.Clock`, same precedent as `LoanApplyMappers.kt#ApplyLoanRequest.toDto`) and
 * is formatted as an ISO-8601 instant string via [Instant.toString] — matching `api.yaml`'s
 * declared `submitted_at: { format: "ISO-8601" }` contract directly (unlike
 * `ApplyLoanRequest.toDto`'s `fineractTransactionDate` helper, which formats a DIFFERENT
 * `dd MMMM yyyy` shape for a different endpoint; not reused here since the two operations
 * declare genuinely different wire date formats). [status] is left at [LoanRequestPayloadDto]'s
 * own declared default (`"PENDING"`) — the domain [LoanRequestPayload] carries no `status` field
 * of its own (see that type's kdoc).
 */
@OptIn(ExperimentalTime::class)
fun LoanRequestPayload.toDto(now: Instant = Clock.System.now()): LoanRequestPayloadDto =
    LoanRequestPayloadDto(
        clientId = clientId,
        requestedAmount = requestedAmount,
        purpose = purpose.toDto(),
        durationWeeks = durationWeeks,
        savingsBalanceAtRequest = savingsBalanceAtRequest,
        submittedAt = now.toString(),
    )

// ---------- response DTO -> domain ----------

fun LoanRequestResponseDto.toDomainModel(): LoanRequestResult = LoanRequestResult(
    resourceId = resourceId,
    officeId = officeId,
    clientId = clientId,
    resourceExternalId = resourceExternalId,
)

// ---------- offline SyncQueue serialization helpers ----------

/**
 * Server-parity Json config (mirrors `NetworkModule`'s client config — `ignoreUnknownKeys` +
 * `coerceInputValues`, T7/EC30) reused for the SyncQueue payload round-trip below, kept private
 * to this file rather than threaded through as a parameter — the offline-queue payload is always
 * this project's own wire shape, never an arbitrary caller-supplied schema.
 */
private val syncQueueJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Serializes this [LoanRequestPayloadDto] to a JSON `String` for `SyncQueueRepository.enqueue(...)`
 * to persist as a `SyncQueueEntry.payload` row when `cmp-network-monitor` reports offline
 * (`api.yaml#api[0].cache.offline: queue_to_syncqueue`). `SyncQueueEntry`/`SyncQueueRepository`
 * themselves live outside this generation step (declared in `api.yaml#dependencies.repositories`,
 * same "repository lives outside this generation step" precedent as `MemberAddRepository`'s
 * kdoc) — this helper is the SoT for how the queued payload is (de)serialized so a future
 * `SyncQueueRepository` implementation and this feature's own retry path never drift.
 */
fun LoanRequestPayloadDto.toJsonPayload(): String =
    syncQueueJson.encodeToString(LoanRequestPayloadDto.serializer(), this)

/** Reverse of [toJsonPayload] — deserializes a queued `SyncQueueEntry.payload` row back to a [LoanRequestPayloadDto] for retry. */
fun loanRequestPayloadDtoFromJson(json: String): LoanRequestPayloadDto =
    syncQueueJson.decodeFromString(LoanRequestPayloadDto.serializer(), json)
