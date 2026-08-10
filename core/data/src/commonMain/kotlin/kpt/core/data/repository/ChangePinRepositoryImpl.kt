/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import co.touchlab.kermit.Logger
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.ChangePinRequest
import kpt.core.model.ChangePinResult
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDto
import kpt.core.network.service.changepin.ChangePinApi

private const val TAG = "ChangePinRepository"

/**
 * See [ChangePinRepository] KDoc for the Store5-branch rationale (`business_logic.kind: crud`).
 * No try-catch here — [changePin] is a plain `when` chain over [ChangePinApi]'s [NetworkResult].
 *
 * See API.md#repositories — ChangePinRepository.
 */
class ChangePinRepositoryImpl(
    private val api: ChangePinApi,
) : ChangePinRepository {

    override suspend fun changePin(request: ChangePinRequest): NetworkResult<ChangePinResult, NetworkError> {
        Logger.d(TAG) { "changePin: submitting new PIN" }
        return when (val result = api.changePin(request.toDto())) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "changePin: succeeded resourceId=${result.data.resourceId}" }
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "changePin: failed error=${result.error}" }
                result
            }
        }
    }
}
