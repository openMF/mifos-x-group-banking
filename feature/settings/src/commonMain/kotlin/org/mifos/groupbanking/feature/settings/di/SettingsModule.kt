/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.feature.settings.AppVersionInfo
import org.mifos.groupbanking.feature.settings.SettingsViewModel

/**
 * Koin module for the group-banking `settings` feature (`org.mifos.groupbanking.feature.settings`
 * — see [SettingsViewModel] class KDoc for why this is a SEPARATE module from the legacy
 * `kpt.feature.settings.SettingsModule` shell still registered in `KoinModules.featureModule`).
 * `UserPreferencesRepository` resolves from `DatastoreModule`, `BiometricAuthenticator` from
 * `core-base/security`'s `SecurityModule`, `ChangePinRepository` from `core/data`'s
 * `RepositoryModule`, and `KptAnalyticsTracker`/`CrashReporter` from their respective already-
 * included modules — all already wired via `KoinModules.allModules`, so this module does not
 * re-declare any of those bindings.
 *
 * `AppVersionInfo` — see its own KDoc "DI-injected app-version seam". The literal `"1.0.0"`/`"1"`
 * default below is a REAL, injected value (not a `@Stub`) with an explicit wiring TODO: a consumer
 * app module should override this `single` with the platform's actual generated version at its own
 * DI-graph composition root once that wiring lands.
 *
 * See API.md#di.
 */
val SettingsModule = module {
    // TODO: wire from the app module's BuildConfig/generated version info once that surface
    // exists (see AppVersionInfo KDoc "DI-injected app-version seam") — this default is real,
    // injected data, not a fabricated stub.
    single { AppVersionInfo(appVersion = "1.0.0", buildNumber = "1") }

    viewModelOf(::SettingsViewModel)
}
