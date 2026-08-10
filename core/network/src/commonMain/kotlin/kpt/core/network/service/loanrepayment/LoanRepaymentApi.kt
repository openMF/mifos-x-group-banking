/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.loanrepayment

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.RecordRepaymentRequestDto
import kpt.core.network.model.RecordRepaymentResponseDto

/**
 * Ktor client for the loan-repayment-dialog feature — records a treasurer-entered repayment
 * against a loan. See `idea-layer/screens/loan-repayment-dialog/api.yaml#api[make_repayment]`
 * (`function: make_repayment`) + API.md#services for the endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8).
 * [kpt.core.data.repository.LoanRepaymentRepositoryImpl] switches on the
 * sealed result directly, with no try-catch of its own (Mandatory Rule 4) — this Service is
 * SERVICE-ONLY.
 *
 * See API.md#services — LoanRepaymentApi.
 */
interface LoanRepaymentApi {

    /**
     * `POST /loans/{loanId}/transactions?command=repayment` (`api.yaml#api[make_repayment]`).
     * Records a repayment transaction against [loanId] in Fineract. [request] carries
     * `transactionDate`/`transactionAmount`/`paymentTypeId`/`receiptNumber`/`locale`/`dateFormat`
     * (`api.yaml#api[0].body`) — [loanId] itself is a PATH parameter, never duplicated into the
     * body. 400 -> [NetworkError.BAD_REQUEST] ("amount exceeds outstanding"); 401 ->
     * [NetworkError.UNAUTHORIZED]; 403 -> [NetworkError.UNKNOWN] (role check, no dedicated
     * bucket — same convention as [kpt.core.network.service.groupcreate.GroupCreateApi]);
     * 404 -> [NetworkError.NOT_FOUND] ("loan not found"); 500 -> [NetworkError.SERVER].
     */
    suspend fun recordRepayment(
        loanId: Long,
        request: RecordRepaymentRequestDto,
    ): NetworkResult<RecordRepaymentResponseDto, NetworkError>
}
