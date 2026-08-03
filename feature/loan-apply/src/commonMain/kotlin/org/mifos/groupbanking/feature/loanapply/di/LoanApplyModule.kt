/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanapply.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.loanapply.LoanApplyViewModel

/**
 * Koin module for the `loan-apply` feature. `LoanApplyRepository` resolves from `DataModule`,
 * `NetworkMonitor` from `DataModule`'s `single<NetworkMonitor> { NetworkMonitorProvider.install() }`,
 * `CrashReporter` from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * (`core/analytics`) from `LoginSignupModule`'s shared process-wide registration — all already
 * included via `KoinModules.allModules` (mirrors `MemberAddModule`'s identical `KptAnalyticsTracker`
 * reuse note, so this module does NOT re-declare a `single { KptAnalyticsTracker(...) }` binding).
 * See API.md#di.
 *
 * `LoanApplyViewModel` is registered with the `viewModel { parameters -> ... }` builder (rather
 * than `viewModelOf(::LoanApplyViewModel)`) because its `groupId` constructor parameter is the
 * `ui.yaml#nav_params.groupId` forwarded from `loan-list`'s "Apply for Loan (FAB)" entry point, not
 * a DI-graph type — same convention as `LoanDetailModule`'s `loanId` wiring. The
 * (not-yet-generated) `LoanApplyRoute.kt` composable is expected to supply it via
 * `koinViewModel<LoanApplyViewModel> { parametersOf(groupId) }`.
 */
val LoanApplyModule = module {
    viewModel { parameters ->
        LoanApplyViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            groupId = parameters.get<Long>(),
        )
    }
}
