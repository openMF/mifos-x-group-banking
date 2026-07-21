/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.personaldashboard.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the two nested collections folded into a single [MemberDashboardCacheEntity] row
 * (`myGroups` + `recentTransactions`). Lives in `core/database` because that module owns the
 * `kotlinx-serialization` dependency — `core/store` does not, so the store maps its domain lists to
 * these flat, wire-neutral payloads and delegates (de)serialization here.
 *
 * The payloads are deliberately String-scalar (enums / dates persisted as strings) so the JSON
 * shape stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build
 * writes.
 *
 * See API.md#stores — MemberDashboard.
 */
object MemberDashboardCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeGroups(groups: List<CachedGroupSummary>): String = json.encodeToString(groups)

    fun decodeGroups(raw: String): List<CachedGroupSummary> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)

    fun encodeTransactions(transactions: List<CachedSavingsTransaction>): String =
        json.encodeToString(transactions)

    fun decodeTransactions(raw: String): List<CachedSavingsTransaction> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}

/**
 * Persistence payload for one `MemberDashboard.myGroups` row — the group-selector chip data.
 * [poolModel] is the `SavingsMechanism` enum `name` (re-parsed with `UNKNOWN` fallback on read).
 */
@Serializable
data class CachedGroupSummary(
    val groupId: String,
    val name: String,
    val poolModel: String,
)

/**
 * Persistence payload for one `MemberDashboard.recentTransactions` row (the shared
 * `savings_transactions` recent-activity list). [date] is an ISO `LocalDate` string; [type] is the
 * `TransactionType` enum `name` (re-parsed with `UNKNOWN` fallback on read).
 */
@Serializable
data class CachedSavingsTransaction(
    val id: String,
    val date: String,
    val type: String,
    val amount: Double,
)
