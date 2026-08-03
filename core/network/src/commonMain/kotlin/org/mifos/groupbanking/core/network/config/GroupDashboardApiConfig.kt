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
 * Koin-injectable base-URL config for the group-dashboard companion bridge (COMP-GRP-001 read
 * path), served by the same mcp-mifosx Go companion server as the group-list, group-create, and
 * personal-dashboard bridges. Follows the same default-param config-class pattern as
 * [GroupApiConfig] / [MemberDashboardApiConfig] / [CompanionAuthApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via [CompanionAuthApiConfig] —
 * [org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApiImpl] reuses that
 * client rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient").
 * This config class exists for override-surface symmetry / documentation and for forks that
 * split the group-dashboard endpoints onto a different host.
 *
 * See API.md#services — GroupDashboardApi base URL.
 */
data class GroupDashboardApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
