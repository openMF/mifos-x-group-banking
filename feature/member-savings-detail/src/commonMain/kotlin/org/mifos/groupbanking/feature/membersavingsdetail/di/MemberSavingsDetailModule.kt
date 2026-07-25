/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsDetailViewModel

/**
 * Koin module for the `member-savings-detail` feature. `SavingsRepository` resolves from
 * `DataModule`, `NetworkMonitor` from `DataModule`'s
 * `single<NetworkMonitor> { NetworkMonitorProvider.install() }` (mirrors `LoanApplyModule`'s
 * identical wiring), `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter`
 * from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` from
 * `LoginSignupModule`'s single process-wide binding — all already included via
 * `KoinModules.allModules`. See API.md#di.
 *
 * `MemberSavingsDetailViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::MemberSavingsDetailViewModel)`) because its `memberId`/`groupId`/
 * `typeConfig` constructor parameters are the `ui.yaml#nav_params` values forwarded from
 * `savings-dashboard`'s `tap_member_row` (or `member-profile`'s `view_full_history_button`) entry
 * point, not DI-graph types — same convention as `PersonalSavingsModule`'s nav-param wiring. The
 * (not-yet-generated) `MemberSavingsDetailRoute.kt` composable MUST supply them via
 * `koinViewModel<MemberSavingsDetailViewModel> { parametersOf(memberId, groupId, typeConfig) }`.
 */
val MemberSavingsDetailModule = module {
    viewModel { parameters ->
        MemberSavingsDetailViewModel(
            repository = get(),
            networkMonitor = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            memberId = parameters.get<String>(),
            groupId = parameters.get<String>(),
            typeConfig = parameters.get(),
        )
    }
}
