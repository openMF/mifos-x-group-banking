/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.config

/**
 * Koin-injectable base-URL config for the member-add create-chain's client stack —
 * `POST /clients` (`create_client`), `POST /datatables/dt_member_role/{clientId}`
 * (`assign_member_role`), and `POST /clients/{clientId}/images` (`upload_photo`), all raw
 * Fineract passthroughs served by the same companion host as every other feature in this
 * module. Follows the same default-param config-class pattern as [GroupCreateApiConfig] /
 * [MemberProfileApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via `CompanionAuthApiConfig` —
 * `org.mifos.groupbanking.core.network.service.memberadd.MemberAddApiImpl` reuses that client
 * rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient"). This
 * config class exists for override-surface symmetry / documentation and for forks that split the
 * member-add endpoints onto a different host.
 *
 * See API.md#services — MemberAddApi base URL.
 */
data class MemberAddApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
