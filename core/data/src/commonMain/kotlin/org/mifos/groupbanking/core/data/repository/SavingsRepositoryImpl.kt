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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.GroupSavingsSummary
import org.mifos.groupbanking.core.model.IndividualSavingsSummary
import org.mifos.groupbanking.core.model.MemberSavingsBundle
import org.mifos.groupbanking.core.model.MemberSavingsDetail
import org.mifos.groupbanking.core.model.SavingsDashboardSummary
import org.mifos.groupbanking.core.model.SavingsLedgerEntry
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.mapper.toDomainModels
import org.mifos.groupbanking.core.network.service.savings.SavingsApi

private const val TAG = "SavingsRepository"

/**
 * See [SavingsRepository] KDoc for the Store5-branch rationale (`savings` has no
 * `AppStoreRegistry` entry yet) and the parallel-combine contracts. No try-catch here — every
 * method is a plain `when`/`coroutineScope` chain over [SavingsApi]'s [NetworkResult].
 *
 * See API.md#repositories — SavingsRepository.
 */
class SavingsRepositoryImpl(
    private val api: SavingsApi,
) : SavingsRepository {

    override suspend fun getSavingsTransactions(
        savingsId: Long,
        limit: Int,
        offset: Int,
    ): NetworkResult<List<SavingsLedgerEntry>, NetworkError> {
        Logger.d(TAG) { "getSavingsTransactions: savingsId=$savingsId, limit=$limit, offset=$offset" }
        return when (val result = api.getSavingsTransactions(savingsId, limit, offset)) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getSavingsTransactions: failed for savingsId=$savingsId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val transactions = result.data.toDomainModels()
                Logger.i(TAG) { "getSavingsTransactions: succeeded savingsId=$savingsId (${transactions.size} rows)" }
                NetworkResult.Success(transactions)
            }
        }
    }

    override suspend fun loadMemberSavings(
        groupLinkedSavingsId: Long,
        individualSavingsId: Long?,
    ): NetworkResult<MemberSavingsBundle, NetworkError> = coroutineScope {
        Logger.d(TAG) {
            "loadMemberSavings: groupLinkedSavingsId=$groupLinkedSavingsId, " +
                "individualSavingsId=$individualSavingsId"
        }

        // Both reads start concurrently; structured concurrency via coroutineScope means both are
        // awaited (even after a short-circuit below) before this function returns, so no orphaned
        // in-flight request survives the call. individualSavingsId == null skips the second read
        // entirely — no network call is made for it.
        val groupLinkedDeferred = async { api.getSavingsTransactions(groupLinkedSavingsId) }
        val individualDeferred = individualSavingsId?.let { id -> async { api.getSavingsTransactions(id) } }

        val groupLinkedResult = groupLinkedDeferred.await()
        val individualResult = individualDeferred?.await()

        // First failure wins, checked in declaration order (group-linked, then individual)
        // regardless of completion order — deterministic error surfacing.
        if (groupLinkedResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadMemberSavings: group-linked read failed: ${groupLinkedResult.error}" }
            return@coroutineScope groupLinkedResult
        }
        if (individualResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadMemberSavings: individual read failed: ${individualResult.error}" }
            return@coroutineScope individualResult
        }

        // Every early-return above handled the Error branch — by this point group-linked is
        // guaranteed Success, and individualResult is either null or Success, so the `as` cast
        // below is safe.
        val bundle = MemberSavingsBundle(
            groupLinkedTransactions = (groupLinkedResult as NetworkResult.Success).data.toDomainModels(),
            individualTransactions = (individualResult as? NetworkResult.Success)?.data?.toDomainModels(),
        )
        Logger.i(TAG) {
            "loadMemberSavings: succeeded groupLinkedSavingsId=$groupLinkedSavingsId " +
                "(groupLinked=${bundle.groupLinkedTransactions.size} rows, " +
                "individual=${bundle.individualTransactions?.size ?: "n/a"} rows)"
        }
        NetworkResult.Success(bundle)
    }

    override suspend fun getMemberSavingsDetail(
        groupId: String,
        memberId: String,
        limit: Int,
        offset: Int,
    ): NetworkResult<MemberSavingsDetail, NetworkError> {
        Logger.d(TAG) { "getMemberSavingsDetail: groupId=$groupId, memberId=$memberId, limit=$limit, offset=$offset" }
        return when (val result = api.getMemberSavingsDetail(groupId, memberId, limit, offset)) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getMemberSavingsDetail: failed for groupId=$groupId, memberId=$memberId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val detail = result.data.toDomainModel()
                Logger.i(TAG) { "getMemberSavingsDetail: succeeded memberId=$memberId (balance=${detail.savingsBalance})" }
                NetworkResult.Success(detail)
            }
        }
    }

    override suspend fun getGroupSavingsSummary(
        groupId: String,
    ): NetworkResult<GroupSavingsSummary, NetworkError> {
        Logger.d(TAG) { "getGroupSavingsSummary: groupId=$groupId" }
        return when (val result = api.getGroupSavingsSummary(groupId)) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getGroupSavingsSummary: failed for groupId=$groupId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val summary = result.data.toDomainModel()
                Logger.i(TAG) { "getGroupSavingsSummary: succeeded groupId=$groupId (${summary.memberRows.size} members)" }
                NetworkResult.Success(summary)
            }
        }
    }

    override suspend fun getIndividualSavingsSummary(
        groupId: String,
    ): NetworkResult<IndividualSavingsSummary, NetworkError> {
        Logger.d(TAG) { "getIndividualSavingsSummary: groupId=$groupId" }
        return when (val result = api.getIndividualSavingsSummary(groupId)) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getIndividualSavingsSummary: failed for groupId=$groupId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val summary = result.data.toDomainModel()
                Logger.i(TAG) { "getIndividualSavingsSummary: succeeded groupId=$groupId (${summary.memberRows.size} members)" }
                NetworkResult.Success(summary)
            }
        }
    }

    override suspend fun loadSavingsDashboard(groupId: String): NetworkResult<SavingsDashboardSummary, NetworkError> = coroutineScope {
        Logger.d(TAG) { "loadSavingsDashboard: groupId=$groupId (2-way parallel)" }

        // Both reads start concurrently; structured concurrency via coroutineScope means both are
        // awaited (even after a short-circuit below) before this function returns.
        val groupDeferred = async { api.getGroupSavingsSummary(groupId) }
        val individualDeferred = async { api.getIndividualSavingsSummary(groupId) }

        val groupResult = groupDeferred.await()
        val individualResult = individualDeferred.await()

        // First failure wins, checked in declaration order (group, then individual) regardless of
        // completion order — deterministic error surfacing.
        if (groupResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadSavingsDashboard: getGroupSavingsSummary failed: ${groupResult.error}" }
            return@coroutineScope groupResult
        }
        if (individualResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadSavingsDashboard: getIndividualSavingsSummary failed: ${individualResult.error}" }
            return@coroutineScope individualResult
        }

        // Every early-return above handled the Error branch for its own read — by this point both
        // are guaranteed Success, so the `as` casts below are safe.
        val summary = SavingsDashboardSummary(
            group = (groupResult as NetworkResult.Success).data.toDomainModel(),
            individual = (individualResult as NetworkResult.Success).data.toDomainModel(),
        )
        Logger.i(TAG) { "loadSavingsDashboard: succeeded groupId=$groupId" }
        NetworkResult.Success(summary)
    }
}
