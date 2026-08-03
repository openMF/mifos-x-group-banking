/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loanwriteoff

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.WriteoffLoanRequestDto
import org.mifos.groupbanking.core.network.model.WriteoffLoanResponseDto

/**
 * Ktor client for the loan-mark-defaulted-dialog feature — posts an irreversible Fineract
 * write-off transaction against a loan. See
 * `idea-layer/screens/loan-mark-defaulted-dialog/api.yaml#api[mark_loan_defaulted]`
 * (`function: write_off_loan`) + API.md#services for the endpoint contract.
 *
 * Returns [NetworkResult] — never a raw [Result] envelope, never a thrown exception (Mandatory
 * Rule 2). `NetworkResult`/`NetworkError` are the framework's `core-base/network` sealed types
 * (consumed, never edited — Hard Rule #8).
 * [org.mifos.groupbanking.core.data.repository.LoanWriteoffRepositoryImpl] switches on the
 * sealed result directly, with no try-catch of its own (Mandatory Rule 4) — this Service is
 * SERVICE-ONLY.
 *
 * See API.md#services — LoanWriteoffApi.
 */
interface LoanWriteoffApi {

    /**
     * `POST /loans/{loanId}/transactions?command=writeoff`
     * (`api.yaml#api[mark_loan_defaulted]`). Marks [loanId] as defaulted in Fineract by posting
     * an irreversible write-off transaction. [request] carries `transactionDate`/`locale`/
     * `dateFormat` (`api.yaml#api[0].body`) — [loanId] itself is a PATH parameter, never
     * duplicated into the body. 401 -> [NetworkError.UNAUTHORIZED]; 403 -> [NetworkError.UNKNOWN]
     * (chairperson-role check, no dedicated bucket — same convention as
     * [org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApi]); 404 ->
     * [NetworkError.NOT_FOUND] ("loan not found"); 409 -> [NetworkError.UNKNOWN] ("loan not in an
     * eligible state for write-off", no dedicated bucket either); 500 -> [NetworkError.SERVER].
     */
    suspend fun writeoffLoan(
        loanId: Long,
        request: WriteoffLoanRequestDto,
    ): NetworkResult<WriteoffLoanResponseDto, NetworkError>
}
