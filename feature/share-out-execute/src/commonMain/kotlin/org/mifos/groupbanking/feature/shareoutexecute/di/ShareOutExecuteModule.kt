/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.shareoutexecute.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.MemberPayout
import org.mifos.groupbanking.feature.shareoutexecute.ShareOutExecuteViewModel

/**
 * Koin module for the `share-out-execute` feature. `ShareOutRepository` resolves from `DataModule`,
 * `NetworkMonitor` from `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`,
 * `CrashReporter` from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * from `LoginSignupModule`'s process-wide binding — all already included via `KoinModules.allModules`
 * (mirrors `ShareOutPreviewModule`'s wiring). `BiometricAuthenticator` (also from `SecurityModule`)
 * is injected into `ShareOutExecuteScreen` directly, not the ViewModel — the platform prompt is a
 * composition concern (the ViewModel only emits `ShowBiometricPrompt` and consumes the result).
 *
 * `ShareOutExecuteViewModel` is registered with the `viewModel { parameters -> ... }` builder because
 * its `groupId`/`typeConfig`/`totalPool`/`memberPayouts` constructor parameters are the
 * `ui.yaml#nav_params` values forwarded from `share-out-preview`'s confirm handoff, not DI-graph
 * types — same convention as `ShareOutPreviewModule`. `ShareOutExecuteScreen.kt` supplies them via
 * `koinViewModel { parametersOf(groupId, typeConfig, totalPool, memberPayouts) }`.
 */
val ShareOutExecuteModule = module {
    viewModel { parameters ->
        ShareOutExecuteViewModel(
            repository = get(),
            networkMonitor = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            groupId = parameters.get<String>(),
            typeConfig = parameters.get<GroupTypeConfig>(),
            totalPool = parameters.get<Double>(),
            memberPayouts = parameters.get<List<MemberPayout>>(),
            cycleNumber = parameters.get<Int>(),
        )
    }
}
