/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrepaymentdialog.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialogViewModel

/**
 * Koin module for the `loan-repayment-dialog` feature. `LoanRepaymentRepository` resolves from
 * `RepositoryModule` (`core/data`), `NetworkMonitor` from `DataModule`'s
 * `single<NetworkMonitor> { NetworkMonitorProvider.install() }`, `CrashReporter` from
 * `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker` (`core/analytics`)
 * from `LoginSignupModule`'s/`GroupCreateModule`'s shared registration — all already included via
 * `KoinModules.allModules` (same `KptAnalyticsTracker` reuse convention as `MemberAddModule`).
 *
 * `LoanRepaymentDialogViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::LoanRepaymentDialogViewModel)`) because its `loanId` / `memberId` /
 * `installmentAmount` constructor parameters are the `ui.yaml#nav_params` values forwarded from
 * `loan-detail`'s "Record Repayment" entry point, not DI-graph types — same convention as
 * `LoanDetailModule`'s `loanId` / `GroupDashboardModule`'s `groupId`/`viewerRole` pair. The
 * (not-yet-generated) `LoanRepaymentDialogRoute.kt` composable MUST supply them via
 * `koinViewModel<LoanRepaymentDialogViewModel> { parametersOf(loanId, memberId, installmentAmount) }`.
 *
 * See API.md#di.
 */
val LoanRepaymentDialogModule = module {
    viewModel { parameters ->
        LoanRepaymentDialogViewModel(
            repository = get(),
            networkMonitor = get(),
            analytics = get(),
            crashReporter = get(),
            loanId = parameters.get<Long>(),
            memberId = parameters.get<Long>(),
            installmentAmount = parameters.get<Double>(),
        )
    }
}
