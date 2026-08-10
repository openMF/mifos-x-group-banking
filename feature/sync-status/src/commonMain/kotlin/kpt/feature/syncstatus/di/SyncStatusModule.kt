/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.syncstatus.di

import kpt.feature.syncstatus.SyncStatusViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module for the `sync-status` feature. `SyncQueueRepository`/`SyncManager` resolve from
 * `DataModule`, `NetworkMonitor` from `DataModule`'s
 * `single<NetworkMonitor> { NetworkMonitorProvider.install() }`, `CrashReporter` from
 * `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` (`core/analytics`)
 * from the shared registration already included via `KoinModules.allModules` (mirrors
 * `MemberAddModule`'s / `GroupDashboardModule`'s identical `KptAnalyticsTracker` reuse note — this
 * module does NOT re-declare a `single { KptAnalyticsTracker(...) } ` binding).
 *
 * `SyncStatusViewModel` is registered with `viewModelOf(::SyncStatusViewModel)` (rather than the
 * `viewModel { parameters -> ... }` builder) because `ui.yaml#nav_params` is empty — this screen
 * takes no nav-args, every constructor dependency resolves purely from the DI graph, same
 * convention as `GroupListModule`/`MemberListModule`.
 *
 * See API.md#di.
 */
val SyncStatusModule = module {
    viewModelOf(::SyncStatusViewModel)
}
