/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kpt.core.database.AppDatabase

/**
 * Wipes ALL locally-cached, user-scoped state. Called by [AuthRepositoryImpl.clearSession] on
 * logout so the NEXT user to sign in on the same device never reads the PREVIOUS user's rows.
 *
 * **Why this is needed:** the Store5 read-caches are keyed by DOMAIN id (e.g. `member_dashboard_cache`
 * by groupId, `loan_list` by group), NOT by user. Two users of the same group therefore share the
 * same cached row — so without a logout wipe, user B sees user A's personal dashboard, loans,
 * savings, etc. The offline sync-queue is user-scoped too (user A's pending writes must not replay
 * under user B). Clearing everything on logout is exactly the contract the logout dialog already
 * promises the user: "Any unsynced changes will be lost."
 *
 * Behind an interface (not a direct `AppDatabase` dependency on [AuthRepositoryImpl]) so the auth
 * repository stays unit-testable with a fake, and so the "what counts as user-scoped local state"
 * decision lives in one place.
 */
interface LocalCacheCleaner {
    /** Clear every local cache + the offline sync-queue. Safe to call when already empty. */
    suspend fun clearAll()
}

/**
 * [LocalCacheCleaner] backed by Room's [AppDatabase.clearAllTables] — wipes every table's data
 * (all Store5 caches, freshness stamps, drafts, and the offline sync-queue) while keeping the
 * schema. One transaction, whole-DB.
 */
class RoomLocalCacheCleaner(
    private val database: AppDatabase,
) : LocalCacheCleaner {
    override suspend fun clearAll() {
        database.clearAllTables()
    }
}
