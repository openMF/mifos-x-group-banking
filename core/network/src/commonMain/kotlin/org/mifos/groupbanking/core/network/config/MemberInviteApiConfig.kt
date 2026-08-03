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
 * Koin-injectable base-URL config for the organizer-side member-invite companion bridge
 * (COMP-DT-002 / COMP-DT-003 / COMP-DT-005), served by the same mcp-mifosx Go companion server as
 * the join-with-code bridge. Follows the same default-param config-class pattern as
 * [CompanionAuthApiConfig] / [InvitationApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to the
 * companion server's base URL via [CompanionAuthApiConfig] —
 * [org.mifos.groupbanking.core.network.service.memberinvite.MemberInviteApiImpl] reuses that
 * client rather than constructing a second engine. This config exists for override-surface
 * symmetry / documentation and for forks that split these endpoints onto a different host.
 *
 * See API.md#services — MemberInviteApi base URL.
 */
data class MemberInviteApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
