/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.syncstatus

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/sync-status` — the read-only offline-sync dashboard route (`ui.yaml#route`,
 * `ui.yaml#nav_params: {}`). No nav-args — [SyncStatusViewModel] resolves purely from the DI graph
 * (`SyncStatusModule`'s `viewModelOf(::SyncStatusViewModel)`). Reachable from `flow.yaml#entry_points`:
 * `bottom_nav` (the "Sync" tab) and `sync_indicator_tap` (conditional on
 * `pending_or_failed_ops_exist`). See API.md#route.
 */
@Serializable
data object SyncStatusRoute

fun NavController.navigateToSyncStatus(navOptions: NavOptions? = null) = navigate(SyncStatusRoute, navOptions)

/**
 * Registers [SyncStatusScreen] on the host [NavGraphBuilder]. `sync-status-screen` is a
 * **terminal screen with no outbound navigation** (`ui.yaml#screens[0].description`) — no
 * `NavigateBack`/navigation event is declared in `ui.yaml#state_model.events.members`, so this
 * registration takes no navigation callbacks. DC3 count-assertion: 0 `= {}` defaults declared in
 * `SyncStatusScreen.kt` / 0 overrides needed / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.syncStatusScreen() {
    composableWithRootPushTransitions<SyncStatusRoute> {
        SyncStatusScreen()
    }
}
