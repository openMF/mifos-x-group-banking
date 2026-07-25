/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.meetingsummary.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the two nested collections folded into a single [MeetingRecordCacheEntity] row
 * (`savingsBreakdown` + `loanItems`). Lives in `core/database` because that module owns the
 * `kotlinx-serialization` dependency — `core/store` does not, so the store maps its domain lists to
 * these flat, wire-neutral payloads and delegates (de)serialization here (same seam as
 * `LoanDetailCacheCodec`).
 *
 * [Json.ignoreUnknownKeys] absorbs any extra field a newer build writes.
 *
 * See API.md#stores — MeetingSummaryData.
 */
object MeetingRecordCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeSavings(rows: List<CachedSavingsBreakdownItem>): String = json.encodeToString(rows)

    fun decodeSavings(raw: String): List<CachedSavingsBreakdownItem> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)

    fun encodeLoans(rows: List<CachedLoanSummaryItem>): String = json.encodeToString(rows)

    fun decodeLoans(raw: String): List<CachedLoanSummaryItem> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}

/** Persistence payload for one `MeetingSummaryData.savingsBreakdown` row. */
@Serializable
data class CachedSavingsBreakdownItem(
    val memberId: String,
    val memberName: String,
    val groupSavings: Long,
    val individualSavings: Long,
)

/** Persistence payload for one `MeetingSummaryData.loanItems` row. */
@Serializable
data class CachedLoanSummaryItem(
    val memberId: String,
    val memberName: String,
    val amountDisbursed: Long,
    val amountRepaid: Long,
    val outstandingAfter: Long,
)
