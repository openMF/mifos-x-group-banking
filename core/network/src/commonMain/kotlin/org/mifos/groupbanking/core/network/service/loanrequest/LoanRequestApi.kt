/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loanrequest

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.LoanRequestPayloadDto
import org.mifos.groupbanking.core.network.model.LoanRequestResponseDto

/**
 * Ktor client for the loan-request feature — the member-side loan-application form's single
 * endpoint (`idea-layer/screens/loan-request/api.yaml#api`). Returns [NetworkResult] — never a raw
 * [Result] envelope, never a thrown exception (Mandatory Rule 2). `NetworkResult`/`NetworkError`
 * are the framework's `core-base/network` sealed types (consumed, never edited — Hard Rule #8).
 * This Service is SERVICE-ONLY — the offline-vs-online decision (`data-flow.yaml#offline_behavior`,
 * `strategy: enqueue_to_sync_queue`) is made by the caller (ViewModel, informed by
 * `NetworkMonitor`), never by this interface; `LoanRequestRepository`'s `enqueueOffline` is the
 * SyncQueue-enqueue counterpart to this Service's network path.
 *
 * See API.md#services — LoanRequestApi.
 */
interface LoanRequestApi {

    /**
     * `POST /datatables/dt_loan_request` (`api.yaml#api[submit_loan_request]`). Creates a
     * `dt_loan_request` datatable row in `PENDING` status for organizer review at the next group
     * meeting. [request] carries the literal `dt_loan_request` wire field names
     * (`requested_amount`/`purpose`/`duration_weeks`/`savings_balance_at_request`/`submitted_at`/
     * `status`) — see `LoanRequestMappers.kt#LoanRequestPayload.toDto` for how the domain payload
     * resolves into this wire shape.
     *
     * 400 -> [NetworkError.BAD_REQUEST] ("field missing or invalid"); 401 ->
     * [NetworkError.UNAUTHORIZED] ("session expired"); 409 falls into the shared status-table's
     * `else` branch -> [NetworkError.UNKNOWN] ("duplicate loan request pending" — no dedicated
     * `CONFLICT` entry in `core-base/network`'s `NetworkError`, same as every other Service in this
     * module); 503 -> [NetworkError.SERVER] ("server unavailable — queue for sync", the 500..599
     * range this Service's status mapper already covers) — the caller (never this Service) is
     * responsible for routing a 503/offline outcome to `LoanRequestRepository.enqueueOffline`.
     */
    suspend fun submitLoanRequest(request: LoanRequestPayloadDto): NetworkResult<LoanRequestResponseDto, NetworkError>
}
