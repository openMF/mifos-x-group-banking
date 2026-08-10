/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

plugins {
    alias(libs.plugins.kmp.library.convention)
    alias(libs.plugins.ktrofit)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kmp.supabase.config)
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

buildkonfig {
    // packageName mirrors the module's Kotlin source package (module scope) so the generated
    // `kpt.core.network.BuildKonfig` is imported without ceremony.
    packageName = "kpt.core.network"
    defaultConfigs {
        buildConfigField(
            STRING, "FRED_API_KEY",
            System.getenv("FRED_API_KEY") ?: localProps.getProperty("FRED_API_KEY", ""),
        )
        // The companion server (mcp-mifosx Go BFF) is the app's single backend — it holds the
        // service credential, does orchestration + proxying to the active Fineract instance. Every
        // The app's single governed base URL — every client reads COMPANION_BASE_URL directly
        // (the companion BFF); the app never talks to Fineract directly. Per-API *ApiConfig classes
        // were removed (fixed single base URL + DynamicBaseUrlPlugin for runtime overrides).
        // SoT: COMPANION_BASE_URL env / local.properties#companion.base.url.
        buildConfigField(
            STRING, "COMPANION_BASE_URL",
            System.getenv("COMPANION_BASE_URL")
                ?: localProps.getProperty("companion.base.url", "https://mifossave-companion.onrender.com"),
        )
        buildConfigField(
            STRING, "FINERACT_TENANT",
            System.getenv("FINERACT_TENANT")
                ?: localProps.getProperty("fineract.tenant", "mifos-bank-2"),
        )
        // Anchor group id whose `dt_group_type_catalogue` multi-row datatable holds the 9 seeded
        // group-type archetypes. The group-type-picker reads the global catalogue via
        // GET /datatables/dt_group_type_catalogue/{anchorId} (direct Fineract). SoT: written to
        // local.properties (fineract.catalogue.anchor.id); read here into
        // kpt.core.network.BuildKonfig.FINERACT_CATALOGUE_ANCHOR_ID.
        buildConfigField(
            STRING, "FINERACT_CATALOGUE_ANCHOR_ID",
            System.getenv("FINERACT_CATALOGUE_ANCHOR_ID")
                ?: localProps.getProperty("fineract.catalogue.anchor.id", "25"),
        )
    }
}

// Supabase credentials are sourced dynamically from the gitignored `secrets/supabaseCredentialsFile.json`
// (url + anonKey) via the shared SupabaseConfigConventionPlugin — the project's established secrets
// mechanism — which generates `kpt.core.network.config.SupabaseCredentials`. When the file is absent
// (the toolkit ships no Supabase project) it generates empty creds, so SupabaseConfigClient stays inert.
supabaseConfig {
    packageName = "kpt.core.network.config"
}

androidComponents {
    finalizeDsl { ext ->
        ext.withHostTest {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.coreBase.network)

            implementation(projects.core.datastore)

            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.ktorfit.lib)

            implementation(libs.squareup.okio)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktorfit.lib)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.ktorfit.ksp)
    add("kspJs", libs.ktorfit.ksp)
    add("kspWasmJs", libs.ktorfit.ksp)
    add("kspDesktop", libs.ktorfit.ksp)
    add("kspIosArm64", libs.ktorfit.ksp)
    add("kspIosSimulatorArm64", libs.ktorfit.ksp)
}
