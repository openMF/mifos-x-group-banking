/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.fieldofficerdashboard

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.PagedGroupsResponseDto

/**
 * Ktor client for the field-officer-dashboard Fineract reads (FR-009). All three reads target the
 * raw Fineract provider API (`/fineract-provider/api/v1/…`) scoped by the field officer's
 * `staffId` — see `idea-layer/screens/field-officer-dashboard/api.yaml` + API.md#services.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8). The downstream Store5 wrapper + its Repository switch on
 * the sealed result with no try-catch of their own (Mandatory Rule 4) — this Service is
 * SERVICE-ONLY.
 */
interface FieldOfficerApi {

    /**
     * `GET /fineract-provider/api/v1/groups?staffId=…` (`get_groups_for_staff`). Fetches all groups
     * assigned to [staffId]. Offset-paginated.
     */
    suspend fun getGroupsForStaff(
        staffId: Long,
        paged: Boolean = true,
        limit: Int = 100,
        offset: Int = 0,
    ): NetworkResult<PagedGroupsResponseDto, NetworkError>

    /**
     * `GET /fineract-provider/api/v1/runreports/FieldOfficerGroupReport?R_staffId=…&output-type=CSV`
     * (`run_report`). Generates a CSV report of all groups + KPIs for [staffId]. Returns the raw CSV
     * bytes on success (handed to the OS share sheet by the ViewModel).
     */
    suspend fun runReport(
        staffId: Long,
        outputType: String = "CSV",
    ): NetworkResult<ByteArray, NetworkError>
}
