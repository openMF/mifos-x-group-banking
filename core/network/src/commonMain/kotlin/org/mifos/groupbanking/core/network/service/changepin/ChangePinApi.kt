/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.changepin

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.ChangePinRequestDto
import org.mifos.groupbanking.core.network.model.ChangePinResponseDto

/**
 * Ktor client for the settings screen's change-PIN submission — its single endpoint
 * (`idea-layer/screens/settings/api.yaml#api[change_pin]`). Returns [NetworkResult] — never a raw
 * [Result] envelope, never a thrown exception (Mandatory Rule 2). `NetworkResult`/`NetworkError`
 * are the framework's `core-base/network` sealed types (consumed, never edited — Hard Rule #8).
 * This Service is SERVICE-ONLY — it makes no offline/session decisions of its own.
 *
 * See API.md#services — ChangePinApi.
 */
interface ChangePinApi {

    /**
     * `PUT /fineract-provider/api/v1/self/user/updatePassword` (`api.yaml#api[change_pin]`).
     * Changes the caller's PIN via Fineract's self-service password-update command. [request]
     * carries ONLY `password`/`repeatPassword` — BOTH set to the member's new PIN. The CURRENT
     * PIN is never part of this wire body; Fineract authenticates the request via the `BasicAuth`
     * header the shared `HttpClient` already attaches, not a JSON field — see
     * `ChangePinMappers.kt` / `ChangePinRepository` KDoc for the full "wire-only exclusion" seam.
     *
     * 400 -> [NetworkError.BAD_REQUEST] ("invalid current PIN or weak new PIN"); 401 ->
     * [NetworkError.UNAUTHORIZED] ("session expired — navigate to login"); 500 ->
     * [NetworkError.SERVER] ("generic error snackbar") — the shared status-table's 500..599
     * range this Service's mapper already covers.
     */
    suspend fun changePin(request: ChangePinRequestDto): NetworkResult<ChangePinResponseDto, NetworkError>
}
