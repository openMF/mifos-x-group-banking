/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.shareoutpreview.impl

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.StoreFactory
import org.mifos.groupbanking.core.model.ShareOutPreview
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.service.shareout.ShareOutApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store

/**
 * Builds the **single-key** read-only NETWORK_WITH_CACHE [Store] for the share-out-preview screen
 * (`GET /companion/groups/{groupId}/shareout/preview`, COMP-DIST-001) that backs the strategy-aware
 * distribution preview.
 *
 * The key is the `String` `groupId` and the value is one [ShareOutPreview] snapshot resolved by the
 * single companion endpoint in one round trip; Store5 caches each group independently. The read side
 * is exposed to the UI exclusively through `ShareOutPreviewRepository.shareOutPreviewStream(...)` →
 * `.asScreenStream(...)`, so the feature ViewModel consumes `ScreenState<ShareOutPreview>` and never
 * `NetworkResult` (RULE-IMPLEMENT-STORE5-001 S5-2 inconsistent-read-path fix). The preview is
 * read-only (the irreversible `execute` write lives on the separate share-out-execute screen /
 * `ShareOutRepository`), so there is no write path here (S5-1).
 *
 * **In-memory SourceOfTruth (honest deviation from loan-detail's Room SoT):** this store is
 * memory-backed ([StoreFactory.createMemoryStore]) rather than Room-backed. The preview is a
 * server-computed, cycle-specific projection (`totalPool`/`memberPayouts`/rotation next-recipient)
 * that is deliberately re-derived from the companion on every open; the primary user-flagged defect
 * being fixed here is the S5-2 read-path leak, which the ScreenState-surfacing `.asScreenStream()`
 * seam resolves. A Room SoT for cross-process offline persistence is a follow-up upgrade that slots
 * in later WITHOUT changing this store's public shape (`createMemoryStore` →
 * `createStore(sourceOfTruth = …)`). The `NetworkResult` from the service is consumed INSIDE the
 * fetcher and never surfaced up.
 *
 * - **Fetcher** — [ShareOutApi.getShareOutPreview] with the groupId decoded from the store key. On
 *   [NetworkResult.Success] the DTO is mapped to domain via [toDomainModel]; on [NetworkResult.Error]
 *   the fetcher throws [ShareOutPreviewFetchException] so Store5 routes it to an error response (no
 *   try-catch, no `Result` envelope — Store5 owns the error channel).
 *
 * See API.md#stores — ShareOutPreview.
 */
fun provideShareOutPreviewStore(
    api: ShareOutApi,
): Store<String, ShareOutPreview> = StoreFactory.createMemoryStore(
    fetcher = Fetcher.of { groupId: String ->
        when (val result = api.getShareOutPreview(groupId)) {
            is NetworkResult.Success -> result.data.toDomainModel()
            is NetworkResult.Error -> throw ShareOutPreviewFetchException(result.error)
        }
    },
)

/**
 * Signals a failed share-out-preview fetch to Store5's error channel. Carries the sealed
 * [NetworkError] so downstream feature-layer error mapping can branch on the exact cause; the
 * message is `categorize()`-friendly for the default state routing.
 */
class ShareOutPreviewFetchException(
    val networkError: NetworkError,
) : Exception("Share-out preview fetch failed: $networkError")
