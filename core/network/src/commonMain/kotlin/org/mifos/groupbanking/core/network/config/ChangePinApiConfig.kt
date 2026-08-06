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
 * Koin-injectable base-URL config for the settings screen's change-PIN client stack —
 * `PUT /fineract-provider/api/v1/self/user/updatePassword` (`change_pin`), a raw Fineract
 * self-service passthrough served by the same companion host as every other feature in this
 * module. Follows the same default-param config-class pattern as [MemberProfileApiConfig] /
 * [MemberAddApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via `CompanionAuthApiConfig` —
 * `org.mifos.groupbanking.core.network.service.changepin.ChangePinApiImpl` reuses that client
 * rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient"). This
 * config class exists for override-surface symmetry / documentation and for forks that split the
 * change-PIN endpoint onto a different host.
 *
 * See API.md#services — ChangePinApi base URL.
 */
data class ChangePinApiConfig(
    val baseUrl: String = BuildKonfig.COMPANION_BASE_URL,
)
