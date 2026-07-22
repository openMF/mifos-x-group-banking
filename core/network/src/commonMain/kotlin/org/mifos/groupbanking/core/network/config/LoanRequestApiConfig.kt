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
 * Koin-injectable base-URL config for the loan-request feature's client stack —
 * `POST /datatables/dt_loan_request` (`submit_loan_request`), a raw Fineract datatable passthrough
 * served by the same companion host as every other feature in this module. Follows the same
 * default-param config-class pattern as [MemberAddApiConfig] / [LoanApplyApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via `CompanionAuthApiConfig` —
 * `org.mifos.groupbanking.core.network.service.loanrequest.LoanRequestApiImpl` reuses that client
 * rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient"). This
 * config class exists for override-surface symmetry / documentation and for forks that split the
 * loan-request endpoint onto a different host.
 *
 * See API.md#services — LoanRequestApi base URL.
 */
data class LoanRequestApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
