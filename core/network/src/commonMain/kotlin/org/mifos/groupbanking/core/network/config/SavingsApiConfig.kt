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
 * Koin-injectable base-URL config for the shared savings client stack — personal-savings' raw
 * Fineract self-service ledger (`GET /self/savingsaccounts/{savingsId}/transactions`) plus
 * member-savings-detail's and savings-dashboard's companion reads (`GET /companion/groups/...`).
 * Follows the same default-param config-class pattern as [LoanApiConfig] / [LoanApplyApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via `CompanionAuthApiConfig` —
 * [org.mifos.groupbanking.core.network.service.savings.SavingsApiImpl] reuses that client rather
 * than constructing a second engine (Hard Rule "don't duplicate the HttpClient"), even though the
 * self-service ledger path is a raw Fineract passthrough rather than a `/companion/…` one — same
 * convention as [MemberProfileApiConfig]. This config class exists for override-surface symmetry
 * / documentation and for forks that split the savings endpoints onto a different host.
 *
 * See API.md#services — SavingsApi base URL.
 */
data class SavingsApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
