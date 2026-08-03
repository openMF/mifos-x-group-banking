/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.loanrequest.LoanRequestViewModel

/**
 * Koin module for the `loan-request` feature. `LoanRequestRepository` resolves from `DataModule`,
 * `NetworkMonitor` from `DataModule`'s `single<NetworkMonitor> { NetworkMonitorProvider.install() }`,
 * `CrashReporter` from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * (`core/analytics`) from `LoginSignupModule`'s shared process-wide registration — all already
 * included via `KoinModules.allModules` (mirrors `LoanApplyModule`'s / `MemberAddModule`'s
 * identical `KptAnalyticsTracker` reuse note, so this module does NOT re-declare a
 * `single { KptAnalyticsTracker(...) }` binding). See API.md#di.
 *
 * `LoanRequestViewModel` is registered with the `viewModel { parameters -> ... }` builder (rather
 * than `viewModelOf(::LoanRequestViewModel)`) because its `clientId`/`savingsBalance`/
 * `loanMultiplier` constructor parameters are `ui.yaml#nav_params` forwarded from
 * `personal-dashboard`'s "Request Loan" CTA or `personal-loans`'s FAB, not DI-graph types — same
 * convention as `LoanApplyModule`'s `groupId` wiring. `loanMultiplier` is read with a `3.0`
 * fallback matching `ui.yaml#nav_params.loanMultiplier.default` when the caller omits it. The
 * (not-yet-generated) `LoanRequestRoute.kt` composable is expected to supply all three via
 * `koinViewModel<LoanRequestViewModel> { parametersOf(clientId, savingsBalance, loanMultiplier) }`.
 */
val LoanRequestModule = module {
    viewModel { parameters ->
        LoanRequestViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            clientId = parameters.get<Long>(),
            savingsBalance = parameters.get<Double>(),
            loanMultiplier = parameters.getOrNull<Double>() ?: 3.0,
        )
    }
}
