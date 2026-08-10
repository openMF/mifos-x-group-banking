/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
plugins {
    alias(libs.plugins.cmp.feature.convention)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.model)
            implementation(projects.core.datastore) // <- added by kmp-viewmodel-gen (UserPreferencesRepository)
            implementation(projects.core.analytics) // <- added by kmp-viewmodel-gen (KptAnalyticsTracker)
            implementation(projects.coreBase.ui)
            implementation(projects.coreBase.network) // <- added by kmp-viewmodel-gen (NetworkResult/NetworkError)
            implementation(projects.coreBase.security) // <- added by kmp-viewmodel-gen (BiometricAuthenticator)
            implementation(projects.coreBase.observability) // <- added by kmp-viewmodel-gen (CrashReporter)

            implementation(libs.kermit.logging) // <- added by kmp-viewmodel-gen

            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended) // <- added by kmp-screen-gen (Icons.AutoMirrored.Filled.ArrowBack)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies { // <- added by kmp-viewmodel-gen
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

compose {
    resources {
        packageOfResClass = "kpt.feature.settings.generated.resources"
    }
}
