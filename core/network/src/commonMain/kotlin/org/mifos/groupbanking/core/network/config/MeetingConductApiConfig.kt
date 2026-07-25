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
 * Koin-injectable base-URL config for the `meeting-conduct` feature's client stack — the 11 Fineract
 * collection-sheet endpoints (`idea-layer/screens/meeting-conduct/api.yaml`). Follows the same
 * default-param config-class pattern as [LoanApplyApiConfig] / [MemberAddApiConfig].
 *
 * The shared `HttpClient` singleton in `kpt.core.network.di.NetworkModule` is already bound to the
 * companion server's base URL — `MeetingConductApiImpl` reuses that client rather than constructing
 * a second engine (Hard Rule "don't duplicate the HttpClient"). This config class exists for
 * override-surface symmetry and for forks that split the meeting endpoints onto a different host.
 *
 * See API.md#services — MeetingConductApi base URL.
 */
data class MeetingConductApiConfig(
    val baseUrl: String = "http://localhost:8080",
)
