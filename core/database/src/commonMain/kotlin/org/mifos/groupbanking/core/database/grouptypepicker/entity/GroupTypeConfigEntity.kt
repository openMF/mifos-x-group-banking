/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.grouptypepicker.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of one seeded group-type catalogue row (COMP-DT-003).
 *
 * Backs the offline cache (SourceOfTruth) for the group-type-picker's NETWORK_WITH_CACHE
 * store — the 9 seeded rows survive process death so a cold start with no network still
 * renders the catalogue (`data-flow.yaml#cache.offline: show_cached`). Keyed by [typeSlug]
 * (the natural business key). Enum axes are persisted as their `name` string and re-parsed
 * on read with an `UNKNOWN` fallback for forward compatibility.
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([GroupTypeConfigDao.deleteOlderThan]); freshness for the UI banner is tracked separately
 * by the framework `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — GroupTypeConfig.
 */
@Entity(tableName = "group_type_config")
data class GroupTypeConfigEntity(
    @PrimaryKey
    val typeSlug: String,
    val displayName: String,
    val tagline: String,
    val savingsMechanism: String,
    val contributionMode: String,
    val lendingEnabled: Boolean,
    val hasSocialFund: Boolean,
    val hasBankLinkage: Boolean,
    val welfareOnlyMode: Boolean,
    val formallyRegistered: Boolean,
    val defaultLoanMultiplier: Double,
    val defaultInterestRatePct: Double,
    val defaultCycleLengthMonths: Int,
    val maxMembers: Int,
    val minMembers: Int,
    val fetchedAt: Long,
)
