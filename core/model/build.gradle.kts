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
    alias(libs.plugins.kmp.library.convention)
    alias(libs.plugins.kotlin.parcelize)
    id("kotlinx-serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(libs.kotlinx.serialization.json)
            // api: kotlinx.datetime.Instant is exposed publicly on domain models
            // (e.g. AuthModels.kt AuthSession/GroupMembership) — consumers (core/network
            // mappers, feature ViewModels) need it resolvable on their own classpath.
            api(libs.kotlinx.datetime)
        }
    }
}