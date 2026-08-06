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
 * Koin-injectable base-URL config for the member-profile feature's raw-Fineract passthrough
 * endpoints (`/clients/{clientId}`, `/clients/{clientId}/accounts`,
 * `/datatables/dt_member_role/{clientId}`), served by the same mcp-mifosx Go companion server as
 * the group-list, group-create, and personal-dashboard bridges today. Follows the same
 * default-param config-class pattern as [GroupCreateApiConfig] / [GroupDashboardApiConfig] /
 * [CompanionAuthApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via [CompanionAuthApiConfig] —
 * [org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApiImpl] reuses that
 * client rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient").
 * This config class exists for override-surface symmetry / documentation and for forks that
 * split the member-profile endpoints onto a different host.
 *
 * See API.md#services — MemberProfileApi base URL.
 */
data class MemberProfileApiConfig(
    val baseUrl: String = BuildKonfig.COMPANION_BASE_URL,
)
