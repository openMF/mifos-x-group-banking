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
 * Koin-injectable base-URL config for the loan-apply feature's client stack — `GET
 * /groups/{groupId}?associations=clientMembers` (`get_group_members`), `GET /loanproducts`
 * (`get_loan_products`), `GET /loans/template` (`get_loan_template`), `GET
 * /clients/{clientId}/accounts` (`get_member_savings`), `GET /datatables/dt_group_corpus/{groupId}`
 * (`get_group_corpus`), `GET /datatables/dt_group_config/{groupId}` (`get_group_config`), and
 * `POST /loans` (`create_new_loan`) — all raw Fineract passthroughs served by the same companion
 * host as every other feature in this module. Follows the same default-param config-class pattern
 * as [MemberAddApiConfig] / [LoanApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to
 * the companion server's base URL via `CompanionAuthApiConfig` —
 * `org.mifos.groupbanking.core.network.service.loanapply.LoanApplyApiImpl` reuses that client
 * rather than constructing a second engine (Hard Rule "don't duplicate the HttpClient"). This
 * config class exists for override-surface symmetry / documentation and for forks that split the
 * loan-apply endpoints onto a different host.
 *
 * See API.md#services — LoanApplyApi base URL.
 */
data class LoanApplyApiConfig(
    val baseUrl: String = BuildKonfig.COMPANION_BASE_URL,
)
