/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import co.touchlab.kermit.Logger
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.CreateGroupRequest
import org.mifos.groupbanking.core.model.GroupCreationResult
import org.mifos.groupbanking.core.model.Office
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDomainModels
import org.mifos.groupbanking.core.network.mapper.toDto
import org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApi

private const val TAG = "GroupCreateRepository"

/**
 * See [GroupCreateRepository] KDoc for the Store5-branch rationale (`business_logic.kind:
 * processor` for [createGroup]) and the [getOffices] SC2 gap note. No try-catch here — every
 * method is a plain `when` over [GroupCreateApi]'s [NetworkResult].
 *
 * See API.md#repositories — GroupCreateRepository.
 */
class GroupCreateRepositoryImpl(
    private val api: GroupCreateApi,
) : GroupCreateRepository {

    override suspend fun getOffices(orderBy: String): NetworkResult<List<Office>, NetworkError> {
        Logger.d(TAG) { "getOffices: orderBy=$orderBy" }
        return when (val result = api.getOffices(orderBy)) {
            is NetworkResult.Success -> {
                val offices = result.data.toDomainModels()
                Logger.i(TAG) { "getOffices succeeded count=${offices.size}" }
                NetworkResult.Success(offices)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getOffices failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun createGroup(request: CreateGroupRequest): NetworkResult<GroupCreationResult, NetworkError> {
        Logger.d(TAG) { "createGroup: submitting name=${request.name} officeId=${request.officeId}" }
        return when (val result = api.createGroup(request.toDto())) {
            is NetworkResult.Success -> {
                val creationResult = result.data.toDomainModel()
                Logger.i(TAG) { "createGroup succeeded groupId=${creationResult.groupId}" }
                NetworkResult.Success(creationResult)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "createGroup failed: ${result.error}" }
                result
            }
        }
    }
}
