/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.loandetail.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the two nested collections folded into a single [LoanDetailCacheEntity] row
 * (`repaymentSchedule` + `repaymentHistory`). Lives in `core/database` because that module owns the
 * `kotlinx-serialization` dependency — `core/store` does not, so the store maps its domain lists to
 * these flat, wire-neutral payloads and delegates (de)serialization here (same seam as
 * `MemberDashboardCacheCodec`).
 *
 * The payloads are deliberately String-scalar (enums / dates persisted as strings) so the JSON
 * shape stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build
 * writes.
 *
 * See API.md#stores — LoanDetailResponse.
 */
object LoanDetailCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeSchedule(rows: List<CachedRepaymentScheduleRow>): String = json.encodeToString(rows)

    fun decodeSchedule(raw: String): List<CachedRepaymentScheduleRow> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)

    fun encodeHistory(rows: List<CachedRepaymentTransaction>): String = json.encodeToString(rows)

    fun decodeHistory(raw: String): List<CachedRepaymentTransaction> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}

/**
 * Persistence payload for one `LoanDetailResponse.repaymentSchedule` row (one installment period).
 * [status] is the `RepaymentRowStatus` enum `name` (re-parsed with `UNKNOWN` fallback on read).
 */
@Serializable
data class CachedRepaymentScheduleRow(
    val weekNumber: Int,
    val dueDate: String,
    val dueAmount: Double,
    val paidAmount: Double,
    val balance: Double,
    val status: String,
)

/**
 * Persistence payload for one `LoanDetailResponse.repaymentHistory` row (a posted transaction
 * against the loan). [type] stays a raw `String` — the wire declares no value-set for it.
 */
@Serializable
data class CachedRepaymentTransaction(
    val id: Long,
    val type: String,
    val date: String,
    val amount: Double,
)
