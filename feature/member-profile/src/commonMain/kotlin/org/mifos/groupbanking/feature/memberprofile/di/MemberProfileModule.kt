/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.mifos.groupbanking.feature.memberprofile.MemberProfileViewModel

/**
 * Koin module for the `member-profile` feature. `MemberProfileRepository` resolves from
 * `DataModule`, `SessionManager` from `core-base/security`'s `SecurityModule`, `CrashReporter`
 * from `core-base/observability`'s `observabilityModule`, and `KptAnalyticsTracker`
 * (`core/analytics`) from `LoginSignupModule`'s shared registration — all already included via
 * `KoinModules.allModules` (mirrors `GroupDashboardModule`'s / `MemberListModule`'s identical
 * `KptAnalyticsTracker` reuse note). See API.md#di.
 *
 * `MemberProfileViewModel` is registered with the `viewModel { parameters -> ... }` builder
 * (rather than `viewModelOf(::MemberProfileViewModel)`) because its `memberId` + `groupId`
 * constructor parameters are `ui.yaml#nav_params` forwarded from `member-list`
 * (`params: { memberId: String, groupId: String }`), not DI-graph types — same convention as
 * `GroupDashboardModule`'s `groupId` / `viewerRole` wiring. The Koin
 * [org.koin.core.parameter.ParametersHolder] resolves same-typed params by declaration ORDER, so
 * the (not-yet-generated) `MemberProfileRoute.kt` composable MUST supply them via
 * `koinViewModel<MemberProfileViewModel> { parametersOf(memberId, groupId) }` — memberId FIRST,
 * groupId SECOND, matching this module's `parameters.get<String>()` call order below.
 */
val MemberProfileModule = module {
    viewModel { parameters ->
        MemberProfileViewModel(
            repository = get(),
            sessionManager = get(),
            crashReporter = get(),
            analytics = get(),
            memberId = parameters.get<String>(),
            groupId = parameters.get<String>(),
        )
    }
}
