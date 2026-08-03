/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.demo

/**
 * Result value object of a successful [DemoSessionManager.startDemoSession] — identifies the
 * seeded guest identity so callers can route (the demo user is the treasurer/organizer of the
 * seeded group). Pure business shape.
 */
data class DemoSession(
    val userId: String,
    val groupId: String,
    val organizerName: String,
)

/**
 * Hydrates the bundled offline demo fixture (`idea-layer/PROJECT_DEMO_DATA.yaml` — the Mwangaza
 * Women's Group VSLA, demo user Amina Otieno, live invite code DEMO24) into the LOCAL cache under
 * a synthetic demo session token, 100% OFFLINE — no companion API, no Fineract, no network.
 * Backs `login-signup`'s Demo Explore flow (`ui.yaml#demo_confirm_dialog`, `flow.yaml#on_demo_confirm`).
 *
 * `login-signup`'s `business_logic.kind` is `processor`, so this is a direct persistence-seed
 * manager (mirrors [org.mifos.groupbanking.core.data.repository.SyncManager] /
 * `kpt.core.data.user.UserLogoutManager` session-scoped managers), NOT a Store5 read-store — it
 * carries no `org.mobilenativefoundation.store` import.
 *
 * Any write inside a demo session is expected to be a local-only no-op at the feature layer; this
 * manager only seeds the read caches + persists the synthetic session.
 */
interface DemoSessionManager {

    /**
     * Seeds the offline landing-screen cache (organizer-dashboard) from the bundled fixture, marks
     * it fresh (so the offline session performs no network revalidate), and persists a synthetic
     * demo session so downstream screens treat the app as authenticated. Fully offline — never
     * throws to the caller; a local persistence failure is folded into [Result.failure].
     */
    suspend fun startDemoSession(): Result<DemoSession>

    /** True while a demo session is active (seeded and not yet cleared). */
    fun isDemoSession(): Boolean

    /** Clears the seeded demo caches + the synthetic session (logout / exit-demo recovery). */
    suspend fun clearDemoSession()
}
