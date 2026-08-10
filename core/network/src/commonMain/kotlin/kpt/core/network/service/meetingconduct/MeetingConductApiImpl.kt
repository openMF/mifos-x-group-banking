/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.meetingconduct

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.CorpusRecordDto
import kpt.core.network.model.CreateAttendanceRequestDto
import kpt.core.network.model.CreateMeetingRecordRequestDto
import kpt.core.network.model.DataTableEntryResponseDto
import kpt.core.network.model.GroupMembersDetailDto
import kpt.core.network.model.LoanDisbursalRequestDto
import kpt.core.network.model.LoanListResponseDto
import kpt.core.network.model.LoanRepaymentRequestDto
import kpt.core.network.model.LoanVoteRecordDto
import kpt.core.network.model.MeetingLoanApplicationDto
import kpt.core.network.model.MeetingRecordDetailDto
import kpt.core.network.model.SavingsTransactionRequestDto
import kpt.core.network.model.UpdateCorpusRequestDto

private const val TAG = "MeetingConductApi"
private const val MEETING_RECORD_DATATABLE = "/datatables/dt_meeting_record"
private const val ATTENDANCE_DATATABLE = "/datatables/dt_meeting_attendance"
private const val GROUP_CORPUS_DATATABLE = "/datatables/dt_group_corpus"
private const val LOAN_VOTE_DATATABLE = "/datatables/dt_loan_vote"
private const val GROUPS_PATH = "/groups"
private const val LOANS_PATH = "/loans"
private const val SAVINGS_ACCOUNTS_PATH = "/savingsaccounts"
private const val GROUP_LOAN_REQUESTS_PATH = "/companion/groups"
private const val LOAN_APPLICATIONS_PATH = "/companion/loan-applications"

/**
 * Plain-Ktor implementation of [MeetingConductApi]. This class is the ONLY layer in the
 * meeting-conduct client stack allowed a try-catch — every exception is caught HERE and converted
 * into [NetworkResult.Error]; nothing throws past this boundary (Mandatory Rule 4 forbids try-catch
 * one layer up, in `MeetingConductRepositoryImpl`). The HTTP-status → [NetworkError] mapping mirrors
 * `core-base/network`'s `ResultSuspendConverterFactory` table exactly, same as
 * [kpt.core.network.service.loanapply.LoanApplyApiImpl]. Reuses the shared
 * `HttpClient` singleton registered in `kpt.core.network.di.NetworkModule` — no second engine.
 *
 * See API.md#services — MeetingConductApi.
 */
class MeetingConductApiImpl(
    private val httpClient: HttpClient,
) : MeetingConductApi {

    override suspend fun getPreviousMeetingRecord(groupId: Int): NetworkResult<MeetingRecordDetailDto, NetworkError> {
        val path = "$MEETING_RECORD_DATATABLE/$groupId"
        Logger.d(TAG) { "getPreviousMeetingRecord: GET $path" }
        return requestAsNetworkResult(op = "getPreviousMeetingRecord") { httpClient.get(path) }
    }

    override suspend fun getGroupMembers(groupId: Int): NetworkResult<GroupMembersDetailDto, NetworkError> {
        val path = "$GROUPS_PATH/$groupId"
        Logger.d(TAG) { "getGroupMembers: GET $path (associations=clientMembers,groupRoles)" }
        return requestAsNetworkResult(op = "getGroupMembers") {
            httpClient.get(path) { parameter("associations", "clientMembers,groupRoles") }
        }
    }

    override suspend fun getGroupCorpus(groupId: Int): NetworkResult<CorpusRecordDto, NetworkError> {
        val path = "$GROUP_CORPUS_DATATABLE/$groupId"
        Logger.d(TAG) { "getGroupCorpus: GET $path" }
        return requestAsNetworkResult(op = "getGroupCorpus") { httpClient.get(path) }
    }

    override suspend fun getActiveLoans(groupId: Int): NetworkResult<LoanListResponseDto, NetworkError> {
        Logger.d(TAG) { "getActiveLoans: GET $LOANS_PATH (groupId=$groupId, loanStatus=active)" }
        return requestAsNetworkResult(op = "getActiveLoans") {
            httpClient.get(LOANS_PATH) {
                parameter("groupId", groupId)
                parameter("loanStatus", "active")
            }
        }
    }

    override suspend fun getPendingLoanApplications(
        groupId: Int,
    ): NetworkResult<List<MeetingLoanApplicationDto>, NetworkError> {
        val path = "$GROUP_LOAN_REQUESTS_PATH/$groupId/loan-requests"
        Logger.d(TAG) { "getPendingLoanApplications: GET $path" }
        return requestAsNetworkResult(op = "getPendingLoanApplications") { httpClient.get(path) }
    }

    override suspend fun getLoanVotes(loanId: String): NetworkResult<LoanVoteRecordDto, NetworkError> {
        val path = "$LOAN_VOTE_DATATABLE/$loanId"
        Logger.d(TAG) { "getLoanVotes: GET $path" }
        return requestAsNetworkResult(op = "getLoanVotes") { httpClient.get(path) }
    }

    override suspend fun postMeetingRecord(
        request: CreateMeetingRecordRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError> {
        Logger.d(TAG) { "postMeetingRecord: POST $MEETING_RECORD_DATATABLE (groupId=${request.groupId})" }
        return requestAsNetworkResult(op = "postMeetingRecord") {
            httpClient.post(MEETING_RECORD_DATATABLE) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun postMeetingAttendance(
        request: CreateAttendanceRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError> {
        Logger.d(TAG) { "postMeetingAttendance: POST $ATTENDANCE_DATATABLE (memberId=${request.memberId})" }
        return requestAsNetworkResult(op = "postMeetingAttendance") {
            httpClient.post(ATTENDANCE_DATATABLE) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun postSavingsTransaction(
        savingsId: String,
        request: SavingsTransactionRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError> {
        val path = "$SAVINGS_ACCOUNTS_PATH/$savingsId/transactions"
        Logger.d(TAG) { "postSavingsTransaction: POST $path (amount=${request.transactionAmount})" }
        return requestAsNetworkResult(op = "postSavingsTransaction") {
            httpClient.post(path) {
                parameter("command", "deposit")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun postLoanRepayment(
        loanId: String,
        request: LoanRepaymentRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError> {
        val path = "$LOANS_PATH/$loanId/transactions"
        Logger.d(TAG) { "postLoanRepayment: POST $path?command=repayment (amount=${request.transactionAmount})" }
        return requestAsNetworkResult(op = "postLoanRepayment") {
            httpClient.post(path) {
                parameter("command", "repayment")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun postLoanDisbursal(
        loanId: String,
        request: LoanDisbursalRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError> {
        // loanId is the application id (== borrower clientId). The companion materialises the approved
        // request into a live Fineract loan (create → approve → disburse); this is NOT a raw
        // /loans/{id}/transactions disburse (an application has no Fineract loan yet).
        val path = "$LOAN_APPLICATIONS_PATH/$loanId/disburse"
        Logger.d(TAG) { "postLoanDisbursal: POST $path (approve+materialise application clientId=$loanId)" }
        return requestAsNetworkResult(op = "postLoanDisbursal") {
            httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun updateCorpus(
        groupId: Int,
        request: UpdateCorpusRequestDto,
    ): NetworkResult<DataTableEntryResponseDto, NetworkError> {
        val path = "$GROUP_CORPUS_DATATABLE/$groupId"
        Logger.d(TAG) { "updateCorpus: PUT $path (closingCorpus=${request.corpusBalance})" }
        return requestAsNetworkResult(op = "updateCorpus") {
            httpClient.put(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}

/**
 * Executes [block], catches transport/serialization exceptions, and maps the resulting
 * [HttpResponse] status code into a [NetworkResult]. Mirrors the shared loanapply/memberadd
 * requestAsNetworkResult helper.
 */
private suspend inline fun <reified T> requestAsNetworkResult(
    op: String,
    block: suspend () -> HttpResponse,
): NetworkResult<T, NetworkError> {
    val response = try {
        block()
    } catch (e: SerializationException) {
        Logger.e(TAG) { "$op request failure: ${e.message}" }
        return NetworkResult.Error(NetworkError.SERIALIZATION)
    } catch (e: Exception) {
        Logger.e(TAG) { "$op transport failure: ${e.message}" }
        return NetworkResult.Error(NetworkError.UNKNOWN)
    }

    return if (response.status.value in 200..299) {
        try {
            val data = response.body<T>()
            Logger.i(TAG) { "$op succeeded (${response.status.value})" }
            NetworkResult.Success(data)
        } catch (e: NoTransformationFoundException) {
            Logger.e(TAG) { "$op response deserialization failure: ${e.message}" }
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: SerializationException) {
            Logger.e(TAG) { "$op response deserialization failure: ${e.message}" }
            NetworkResult.Error(NetworkError.SERIALIZATION)
        } catch (e: ContentConvertException) {
            Logger.e(TAG) { "$op response deserialization failure: ${e.message}" }
            NetworkResult.Error(NetworkError.SERIALIZATION)
        }
    } else {
        val error = response.status.toNetworkError()
        Logger.e(TAG) { "$op failed with HTTP ${response.status.value} -> $error" }
        NetworkResult.Error(error)
    }
}

private fun HttpStatusCode.toNetworkError(): NetworkError = when (value) {
    HttpStatusCode.BadRequest.value -> NetworkError.BAD_REQUEST
    HttpStatusCode.Unauthorized.value -> NetworkError.UNAUTHORIZED
    HttpStatusCode.NotFound.value -> NetworkError.NOT_FOUND
    HttpStatusCode.RequestTimeout.value -> NetworkError.REQUEST_TIMEOUT
    HttpStatusCode.TooManyRequests.value -> NetworkError.TOO_MANY_REQUESTS
    in 500..599 -> NetworkError.SERVER
    else -> NetworkError.UNKNOWN
}
