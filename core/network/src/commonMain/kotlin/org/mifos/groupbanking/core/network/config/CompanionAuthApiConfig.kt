/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.config

/**
 * Koin-injectable base-URL config for the companion auth bridge (COMP-AUTH-001/002/003),
 * served by the mcp-mifosx Go companion server (see
 * `server-layer/COMPANION_API_BUILD_DEPLOY.md`). Follows the same default-param config-class
 * pattern documented on `kpt.core.network.config.AppUrlTypes` (Koin-injected config carrying a
 * `baseUrl: String`, overridden per-fork/per-environment) rather than BuildKonfig, keeping the
 * override surface in plain Kotlin.
 *
 * Forks/environments override the default by re-registering the Koin binding, e.g.:
 * ```kotlin
 * single<CompanionAuthApiConfig> { CompanionAuthApiConfig(baseUrl = "https://companion.example.org") }
 * ```
 *
 * See API.md#services — CompanionAuthApi base URL.
 */
data class CompanionAuthApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
