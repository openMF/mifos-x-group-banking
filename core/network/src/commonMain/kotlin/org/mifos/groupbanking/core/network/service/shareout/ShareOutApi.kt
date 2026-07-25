/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.shareout

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.RotationPayoutExecuteRequestDto
import org.mifos.groupbanking.core.network.model.RotationPayoutExecuteResponseDto
import org.mifos.groupbanking.core.network.model.ShareOutExecuteRequestDto
import org.mifos.groupbanking.core.network.model.ShareOutExecuteResponseDto
import org.mifos.groupbanking.core.network.model.ShareOutPreviewDto

/**
 * Ktor client for the share-out-preview companion read
 * (`GET /companion/groups/{groupId}/shareout/preview`, COMP-DIST-001). See
 * `idea-layer/screens/share-out-preview/api.yaml` + `core/network/API.md#services` for the
 * endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). `ShareOutRepositoryImpl` (`core/data`) is the sole
 * consumer — this Service is SERVICE-ONLY; no repository/store is emitted here. `share-out-preview`
 * has no `AppStoreRegistry` entry (SP-03 `kmp-store-gen` has not run for this feature), so the
 * Store5-wrapping question is out of scope — same Store5-free branch as `SavingsApi`.
 *
 * See API.md#services — ShareOutApi.
 */
interface ShareOutApi {

    /**
     * `GET /companion/groups/{groupId}/shareout/preview`
     * (`api.yaml#api[get_shareout_preview]`) — the strategy-aware distribution preview. The
     * companion computes `totalCorpus`/`totalProfit`/`totalPool` and the pool-model-appropriate
     * payload (`memberPayouts[]` for ACCUMULATING types, or `rotationPosition` +
     * `nextRecipientName` + `nextRecipientAmount` for ROTATING_PAYOUT types) server-side from the
     * caller's token-resolved group. Role gating (organizer/treasurer/chair) is enforced
     * server-side: 403 -> [NetworkError.UNKNOWN] (no dedicated bucket, same else-branch convention
     * as `MemberProfileApi`); 401 -> [NetworkError.UNAUTHORIZED]; 404 -> [NetworkError.NOT_FOUND]
     * ("group not found"); 409 (share-out already executed this cycle) -> [NetworkError.UNKNOWN];
     * 5xx -> [NetworkError.SERVER].
     */
    suspend fun getShareOutPreview(groupId: String): NetworkResult<ShareOutPreviewDto, NetworkError>

    /**
     * `POST /companion/groups/{groupId}/shareout/execute`
     * (`share-out-execute/api.yaml#api.post_shareout_execute`, COMP-DIST-001) — executes the
     * ACCUMULATING (VSLA/ASCA/SHG/SILC) share-out. The companion records `dt_share_out` and drains
     * each member's savings withdrawal via Fineract in one transaction, returning per-member
     * success/failure counts. Same status-code -> [NetworkError] mapping as [getShareOutPreview]:
     * 403 role-forbidden and 409 already-executed both fall into [NetworkError.UNKNOWN] (no
     * dedicated bucket); 401 -> [NetworkError.UNAUTHORIZED]; 5xx -> [NetworkError.SERVER].
     */
    suspend fun executeShareOut(
        groupId: String,
        request: ShareOutExecuteRequestDto,
    ): NetworkResult<ShareOutExecuteResponseDto, NetworkError>

    /**
     * `POST /companion/groups/{groupId}/rotation/execute`
     * (`share-out-execute/api.yaml#api.post_rotation_payout_execute`, COMP-DIST-002) — executes the
     * ROTATING_PAYOUT (ROSCA/chit) single-recipient rotation. The companion advances
     * `rosca_rotation` (marks the slot paid, advances position) and drains the one savings
     * withdrawal. Same status-code mapping as [executeShareOut].
     */
    suspend fun executeRotationPayout(
        groupId: String,
        request: RotationPayoutExecuteRequestDto,
    ): NetworkResult<RotationPayoutExecuteResponseDto, NetworkError>
}
