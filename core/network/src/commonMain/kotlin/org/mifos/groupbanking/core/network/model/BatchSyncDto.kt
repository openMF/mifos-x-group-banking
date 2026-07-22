/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for one request row inside a Fineract `/batches` submission (`batch_sync`,
 * `POST /fineract-provider/api/v1/batches`), matching
 * `api.yaml#dtos.BatchSyncRequest.requests[]` verbatim (camelCase — the standard Fineract Batch
 * API convention, not a raw datatable's snake_case).
 *
 * See API.md#dtos — BatchOperation.
 */
@Serializable
data class BatchOperationDto(
    @SerialName("requestId") val requestId: Int,
    @SerialName("relativeUrl") val relativeUrl: String,
    @SerialName("method") val method: String,
    @SerialName("body") val body: String,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire request envelope for `batch_sync` — matching `api.yaml#dtos.BatchSyncRequest` verbatim
 * (`{ "requests": [...] }`).
 *
 * See API.md#dtos — BatchSyncRequest.
 */
@Serializable
data class BatchSyncRequestDto(
    @SerialName("requests") val requests: List<BatchOperationDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for one response row of `batch_sync` — matching
 * `api.yaml#dtos.BatchSyncResponse.items` verbatim. **The `/batches` HTTP response body is a
 * top-level JSON ARRAY of this shape** (`api.yaml#dtos.BatchSyncResponse` declares
 * `type: array` at the top level) — NO wrapper object, decode with
 * `ListSerializer(BatchSyncResponseItemDto.serializer())` (or the Ktor service's
 * `List<BatchSyncResponseItemDto>` response type), never a synthetic envelope DTO.
 *
 * [statusCode] is the per-request HTTP status Fineract returns for that individual batched
 * operation (2xx success, `409` conflict, anything else a failure) — see
 * `BatchSyncMappers.kt#toSyncResult` for the fold into the domain [org.mifos.groupbanking.core.model.SyncResult].
 *
 * See API.md#dtos — BatchSyncResponseItem.
 */
@Serializable
data class BatchSyncResponseItemDto(
    @SerialName("requestId") val requestId: Int,
    @SerialName("statusCode") val statusCode: Int,
    @SerialName("body") val body: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
