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
 * Koin-injectable base-URL config for the group-create wizard's client stack —
 * `POST /companion/groups` (COMP-GRP-001, companion bridge) and `GET /offices` (raw Fineract
 * passthrough, same companion host). Both live behind the same `mcp-mifosx` Go companion server
 * as every other feature in this module (see `server-layer/COMPANION_API_BUILD_DEPLOY.md`).
 * Follows the same default-param config-class pattern as [CompanionAuthApiConfig] /
 * [GroupApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via [CompanionAuthApiConfig] —
 * [org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiImpl] reuses that
 * client rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient").
 * This config class exists for override-surface symmetry / documentation and for forks that
 * split the group-create endpoints onto a different host.
 *
 * See API.md#services — GroupCreateApi base URL.
 */
data class GroupCreateApiConfig(
    val baseUrl: String = BuildKonfig.COMPANION_BASE_URL,
)
