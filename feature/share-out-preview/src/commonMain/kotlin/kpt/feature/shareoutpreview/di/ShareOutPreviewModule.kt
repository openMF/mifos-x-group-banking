/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.shareoutpreview.di

import kpt.feature.shareoutpreview.ShareOutPreviewViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the `share-out-preview` feature. `ShareOutRepository` resolves from `DataModule`,
 * `NetworkMonitor` from `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`,
 * `CrashReporter` from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * from `LoginSignupModule`'s single process-wide binding — all already included via
 * `KoinModules.allModules` (mirrors `SavingsDashboardModule`'s identical wiring). See API.md#di.
 *
 * `ShareOutPreviewViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * because its `groupId`/`typeConfig` constructor parameters are the `ui.yaml#nav_params` values
 * forwarded from `group-dashboard`'s share-out entry point, not DI-graph types — same convention as
 * `SavingsDashboardModule`. `ShareOutPreviewScreen.kt` supplies them via
 * `koinViewModel<ShareOutPreviewViewModel> { parametersOf(groupId, typeConfig) }`.
 */
val ShareOutPreviewModule = module {
    viewModel { parameters ->
        ShareOutPreviewViewModel(
            repository = get(),
            networkMonitor = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<String>(),
            typeConfig = parameters.get(),
        )
    }
}
