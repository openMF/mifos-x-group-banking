/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loandetail.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.loandetail.LoanDetailViewModel

/**
 * Koin module for the `loan-detail` feature. `LoanDetailRepository` is resolved from
 * `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter`
 * from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` from
 * `LoginSignupModule`'s single process-wide binding — all already included via
 * `KoinModules.allModules`. See API.md#di.
 *
 * `LoanDetailViewModel` is registered with the `viewModel { parameters -> ... }` builder (rather
 * than `viewModelOf(::LoanDetailViewModel)`) because its `loanId` constructor parameter is the
 * `ui.yaml#nav_params` value forwarded from `loan-list`'s "Loan card tap" entry point, not a
 * DI-graph type — same convention as `LoanListModule`'s `groupId` / `GroupDashboardModule`'s
 * `groupId`/`viewerRole` pair. The (not-yet-generated) `LoanDetailRoute.kt` composable MUST supply
 * it via `koinViewModel<LoanDetailViewModel> { parametersOf(loanId) }`.
 */
val LoanDetailModule = module {
    viewModel { parameters ->
        LoanDetailViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            loanId = parameters.get<Long>(),
        )
    }
}
