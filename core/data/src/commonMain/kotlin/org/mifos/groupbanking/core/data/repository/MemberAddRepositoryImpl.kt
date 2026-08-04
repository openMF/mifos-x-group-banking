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

import co.touchlab.kermit.Logger
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.CreateMemberRequest
import org.mifos.groupbanking.core.model.MemberCreationResult
import org.mifos.groupbanking.core.network.mapper.toAssignMemberRoleRequestDto
import org.mifos.groupbanking.core.network.mapper.toCreateMemberRequestDto
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toOfflinePayloadJson
import org.mifos.groupbanking.core.network.service.memberadd.MemberAddApi

private const val TAG = "MemberAddRepository"
private const val DEFAULT_PHOTO_FILE_NAME = "member-photo.jpg"
private const val CREATE_MEMBER_OPERATION_TYPE = "CREATE_MEMBER"
private const val CREATE_MEMBER_TARGET_ROUTE = "/companion/members"

/**
 * See [MemberAddRepository] KDoc for the Store5-branch rationale (`business_logic.kind:
 * processor`) and the offline-queue seam. No try-catch here — every method is a plain `when`
 * chain over [MemberAddApi]'s [NetworkResult].
 *
 * See API.md#repositories — MemberAddRepository.
 */
class MemberAddRepositoryImpl(
    private val api: MemberAddApi,
    private val syncQueueRepository: SyncQueueRepository,
) : MemberAddRepository {

    override suspend fun createMember(
        request: CreateMemberRequest,
        photoBytes: ByteArray?,
    ): NetworkResult<MemberCreationResult, NetworkError> {
        Logger.d(TAG) { "createMember: creating client for groupId=${request.groupId}" }
        val createResult = api.createClient(request.toCreateMemberRequestDto())
        return when (createResult) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "createMember: createClient failed, chain aborted: ${createResult.error}" }
                createResult
            }
            is NetworkResult.Success -> {
                val clientId = createResult.data.clientId
                Logger.i(TAG) { "createMember: createClient succeeded clientId=$clientId" }

                val assignRoleResult = api.assignMemberRole(
                    clientId = clientId.toString(),
                    request = request.toAssignMemberRoleRequestDto(),
                )
                when (assignRoleResult) {
                    is NetworkResult.Error -> {
                        Logger.e(TAG) {
                            "createMember: assignMemberRole failed for clientId=$clientId " +
                                "(client already created — upload photo skipped): ${assignRoleResult.error}"
                        }
                        assignRoleResult
                    }
                    is NetworkResult.Success -> {
                        Logger.i(TAG) { "createMember: assignMemberRole succeeded clientId=$clientId" }
                        // Optional, best-effort final step — a photo-upload failure never fails the
                        // create-chain (photo capture is optional per ui.yaml#actions.OnPhotoRemoved).
                        val photoUploaded = uploadMemberPhotoBestEffort(clientId, photoBytes, request.photoFileName())
                        NetworkResult.Success(
                            createResult.data.toDomainModel(request = request, photoUploaded = photoUploaded),
                        )
                    }
                }
            }
        }
    }

    /**
     * Best-effort member-photo upload — returns whether the photo was stored. A null [photoBytes]
     * (no photo captured) or an upload failure both return `false` WITHOUT failing the surrounding
     * create-member chain (the client + role are already committed on the server).
     */
    private suspend fun uploadMemberPhotoBestEffort(
        clientId: Long,
        photoBytes: ByteArray?,
        fileName: String,
    ): Boolean {
        if (photoBytes == null) return false
        return when (
            val uploadResult = api.uploadMemberPhoto(
                clientId = clientId.toString(),
                photoBytes = photoBytes,
                fileName = fileName,
            )
        ) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "createMember: uploadMemberPhoto succeeded clientId=$clientId" }
                true
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) {
                    "createMember: uploadMemberPhoto failed (non-fatal, member still created): ${uploadResult.error}"
                }
                false
            }
        }
    }

    override suspend fun enqueueOffline(request: CreateMemberRequest): Long {
        val payloadJson = request.toOfflinePayloadJson()
        Logger.i(TAG) { "enqueueOffline: queuing $CREATE_MEMBER_OPERATION_TYPE for groupId=${request.groupId}" }
        return syncQueueRepository.enqueue(
            operationType = CREATE_MEMBER_OPERATION_TYPE,
            targetTable = CREATE_MEMBER_TARGET_ROUTE,
            payloadJson = payloadJson,
        )
    }
}

/**
 * Derives the multipart filename from [CreateMemberRequest.photoUri]'s last path segment,
 * falling back to [DEFAULT_PHOTO_FILE_NAME] when [CreateMemberRequest.photoUri] is null/blank
 * (defensive — [MemberAddRepositoryImpl.createMember]'s `photoBytes` parameter is the
 * platform-read source of truth for whether a photo exists; [CreateMemberRequest.photoUri] is
 * used here only for its filename, not re-validated as the upload trigger).
 */
private fun CreateMemberRequest.photoFileName(): String =
    photoUri?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: DEFAULT_PHOTO_FILE_NAME
