/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.ChangePinRequest
import org.mifos.groupbanking.core.model.ChangePinResult

/**
 * Change-PIN submission repository (`idea-layer/screens/settings`). Wraps `ChangePinApi`
 * (core/network) directly.
 *
 * **Store5 branch (SP-04):** settings' `business_logic.kind` is `crud`
 * (`ui.yaml#business_logic.kind`) — the legacy template path
 * (RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i zero-regression). There is no read-stream to back with
 * a Store5 cache — change-PIN is a pure fire-and-forget mutation, not a cached entity. This
 * repository surfaces [NetworkResult] directly rather than `.asScreenStream()` /
 * `.asPagingScreenStream()` / `MutableStore.write(...)` — same branch as
 * [LoanRequestRepositoryImpl] / [MemberAddRepositoryImpl] / [InvitationRepositoryImpl] /
 * [GroupCreateRepositoryImpl]. No `org.mobilenativefoundation.store` import anywhere in this
 * stack.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [ChangePinRepositoryImpl] is a
 * plain `when` chain over the service's sealed [NetworkResult]; `ChangePinApiImpl` is the sole
 * layer allowed to catch exceptions.
 *
 * Deliberately SEPARATE from [AuthRepository] — this repository owns only the settings screen's
 * change-PIN write; it does not extend, wrap, or otherwise touch `AuthRepository`/
 * `AuthRepositoryImpl` (kept green, untouched).
 *
 * See API.md#repositories — ChangePinRepository.
 */
interface ChangePinRepository {

    /**
     * Submits [request] to `PUT /fineract-provider/api/v1/self/user/updatePassword` via the
     * wrapped `ChangePinApi`. [ChangePinRequest.currentPin] is NOT part of the wire body — it
     * authenticates the request via the `BasicAuth` header the shared `HttpClient` attaches (see
     * `ChangePinMappers.kt` KDoc); only [ChangePinRequest.newPin] is threaded through, mapped
     * into BOTH `ChangePinRequestDto.password` and `.repeatPassword`. On success the Fineract
     * command-processing envelope is mapped to [ChangePinResult].
     */
    suspend fun changePin(request: ChangePinRequest): NetworkResult<ChangePinResult, NetworkError>
}
