/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.memberprofile.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE member-profile composite snapshot per client — the 3-way
 * parallel fan-in of `get_client` + `get_client_accounts` + `get_member_role`
 * (`idea-layer/screens/member-profile/api.yaml`).
 *
 * Backs the offline cache (SourceOfTruth) for the composite NETWORK_WITH_CACHE store
 * ([org.mifos.groupbanking.core.store.memberprofile.impl.provideMemberProfileStore]), a
 * **dynamic-key** read: one row per [clientId] so a cold start with no network still renders the
 * last-seen profile for that client (`data-flow.yaml#cache.offline: show_cached`, SC2 — never
 * memory-only).
 *
 * The whole `MemberProfileDetail` composite (nested identity + accounts + role datatable) is
 * persisted as ONE row per client via a single [profileJson] column encoded by
 * [org.mifos.groupbanking.core.database.memberprofile.entity.MemberProfileCacheCodec] — the
 * `core/database` module owns `kotlinx-serialization`. A single-row upsert is inherently atomic, so
 * there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001 S5-3 satisfied by construction).
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([MemberProfileCacheDao.deleteOlderThan]); freshness for the UI banner is tracked separately by
 * the framework `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — MemberProfile.
 */
@Entity(tableName = "member_profile_cache")
data class MemberProfileCacheEntity(
    @PrimaryKey
    val clientId: String,
    val profileJson: String,
    val fetchedAt: Long,
)
