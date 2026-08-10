/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.database.memberprofile.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON codec for the WHOLE member-profile composite folded into a single
 * [MemberProfileCacheEntity.profileJson] column — the 3-way parallel fan-in of `get_client` +
 * `get_client_accounts` + `get_member_role`. Lives in `core/database` because that module owns the
 * `kotlinx-serialization` dependency — `core/store` does not, so the store maps its domain composite
 * to these flat, wire-neutral payloads and delegates (de)serialization here (same seam as
 * `GroupDashboardCacheCodec`).
 *
 * The payloads are deliberately String-scalar (enums persisted as their `name`) so the JSON shape
 * stays forward-compatible; [Json.ignoreUnknownKeys] absorbs any extra field a newer build writes.
 *
 * See API.md#stores — MemberProfile.
 */
object MemberProfileCacheCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(profile: CachedMemberProfile): String = json.encodeToString(profile)

    fun decode(raw: String): CachedMemberProfile = json.decodeFromString(raw)
}

/**
 * Persistence payload mirroring the domain `MemberProfileDetail` composite — the 3 fanned-in
 * sections ([member], [accounts], [roles]) serialized as one JSON blob per client row.
 */
@Serializable
data class CachedMemberProfile(
    val member: CachedMemberIdentity,
    val accounts: CachedMemberAccounts,
    val roles: List<CachedMemberRole>,
)

/** Persistence payload for the `get_client` identity header. [statusId] / [statusValue] flatten the nested Fineract status pair. */
@Serializable
data class CachedMemberIdentity(
    val id: Long,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val hasPhoto: Boolean,
    val statusId: Int,
    val statusValue: String,
    val joinDate: String,
    val officeId: Long,
)

/** Persistence payload for the `get_client_accounts` savings/loan card (nested [savingsHistory] + [activeLoan]). */
@Serializable
data class CachedMemberAccounts(
    val savingsBalance: Double,
    val savingsHistory: List<CachedSavingsDataPoint>,
    val activeLoan: CachedActiveLoanSummary?,
)

/** Persistence payload for one weekly savings sparkline point. */
@Serializable
data class CachedSavingsDataPoint(
    val date: String,
    val balance: Double,
)

/** Persistence payload for the derived active-loan summary (nullable — a member may have no loan). */
@Serializable
data class CachedActiveLoanSummary(
    val id: Long,
    val productName: String,
    val outstandingBalance: Double,
    val inArrears: Boolean,
    val dueDate: String?,
)

/** Persistence payload for one `get_member_role` datatable row. [role] is the `MemberRole` enum `name`. */
@Serializable
data class CachedMemberRole(
    val role: String,
    val groupId: Long,
    val assignedDate: String,
)
