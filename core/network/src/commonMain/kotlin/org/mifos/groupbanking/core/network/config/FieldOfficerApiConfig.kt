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

import kpt.core.network.BuildKonfig

/**
 * Koin-injectable base-URL config for the field-officer-dashboard Fineract reads (FR-009 —
 * `GET /groups`, `GET /runreports/FieldOfficerGroupReport`). Follows the same
 * default-param config-class pattern as [CompanionAuthApiConfig] / [GroupApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` dispatches these
 * requests (no second engine); this config class exists for override-surface symmetry and for
 * forks that split the Fineract host from the companion host.
 *
 * See API.md#services — FieldOfficerApi base URL.
 */
data class FieldOfficerApiConfig(
    val baseUrl: String = BuildKonfig.COMPANION_BASE_URL,
)
