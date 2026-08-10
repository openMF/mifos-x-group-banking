/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.ApplyLoanRequest
import kpt.core.model.GroupMember
import kpt.core.model.LoanApplicationResult
import kpt.core.model.LoanApplyTemplate
import kpt.core.model.LoanProduct
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDomainModels
import kpt.core.network.mapper.toDto
import kpt.core.network.service.loanapply.LoanApplyApi

private const val TAG = "LoanApplyRepository"

/**
 * See [LoanApplyRepository] KDoc for the Store5-branch rationale (`business_logic.kind: composite`,
 * no `AppStoreRegistry` entry yet) and the 5-way parallel-combine contract. No try-catch here —
 * every method is a plain `when`/`coroutineScope` chain over [LoanApplyApi]'s [NetworkResult].
 *
 * See API.md#repositories — LoanApplyRepository.
 */
class LoanApplyRepositoryImpl(
    private val api: LoanApplyApi,
) : LoanApplyRepository {

    override suspend fun getGroupMembers(groupId: Long): NetworkResult<List<GroupMember>, NetworkError> {
        Logger.d(TAG) { "getGroupMembers: groupId=$groupId" }
        return when (val result = api.getGroupMembers(groupId)) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "getGroupMembers: failed for groupId=$groupId: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val members = result.data.toDomainModels()
                Logger.i(TAG) { "getGroupMembers: succeeded groupId=$groupId (${members.size} members)" }
                NetworkResult.Success(members)
            }
        }
    }

    override suspend fun loadTemplate(
        groupId: Long,
        clientId: Long,
        productId: Long,
    ): NetworkResult<LoanApplyTemplate, NetworkError> = coroutineScope {
        Logger.d(TAG) { "loadTemplate: groupId=$groupId, clientId=$clientId, productId=$productId (5-way parallel)" }

        // 5-way parallel fan-out — every read starts concurrently; structured concurrency via
        // coroutineScope means all 5 are awaited (even after a short-circuit below) before this
        // function returns, so no orphaned in-flight request survives the call.
        val productsDeferred = async { api.getLoanProducts() }
        val templateDeferred = async { api.getLoanTemplate(clientId, productId) }
        val savingsDeferred = async { api.getMemberSavings(clientId) }
        val corpusDeferred = async { api.getGroupCorpus(groupId) }
        val configDeferred = async { api.getGroupLoanConfig(groupId) }

        val productsResult = productsDeferred.await()
        val templateResult = templateDeferred.await()
        val savingsResult = savingsDeferred.await()
        val corpusResult = corpusDeferred.await()
        val configResult = configDeferred.await()

        // First failure wins, checked in a fixed declaration order (products, template, savings,
        // corpus, config) regardless of completion order — deterministic error surfacing.
        if (productsResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadTemplate: getLoanProducts failed: ${productsResult.error}" }
            return@coroutineScope productsResult
        }
        if (templateResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadTemplate: getLoanTemplate failed: ${templateResult.error}" }
            return@coroutineScope templateResult
        }
        if (savingsResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadTemplate: getMemberSavings failed: ${savingsResult.error}" }
            return@coroutineScope savingsResult
        }
        if (corpusResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadTemplate: getGroupCorpus failed: ${corpusResult.error}" }
            return@coroutineScope corpusResult
        }
        if (configResult is NetworkResult.Error) {
            Logger.e(TAG) { "loadTemplate: getGroupLoanConfig failed: ${configResult.error}" }
            return@coroutineScope configResult
        }

        // Every early-return above handled the Error branch for its own read — by this point all
        // 5 are guaranteed NetworkResult.Success, so the `as` casts below are safe.
        val template = (templateResult as NetworkResult.Success).data.toDomainModel(
            products = (productsResult as NetworkResult.Success).data,
            savings = (savingsResult as NetworkResult.Success).data,
            corpus = (corpusResult as NetworkResult.Success).data,
            config = (configResult as NetworkResult.Success).data,
        )
        Logger.i(TAG) {
            "loadTemplate: succeeded groupId=$groupId, clientId=$clientId, productId=$productId " +
                "(maxEligibleAmount=${template.maxEligibleAmount})"
        }
        NetworkResult.Success(template)
    }

    override suspend fun applyLoan(
        request: ApplyLoanRequest,
        product: LoanProduct,
    ): NetworkResult<LoanApplicationResult, NetworkError> {
        Logger.d(TAG) {
            "applyLoan: memberId=${request.memberId}, productId=${request.productId}, amount=${request.amount}"
        }
        return when (val result = api.applyLoan(request.toDto(product))) {
            is NetworkResult.Error -> {
                Logger.e(TAG) { "applyLoan: failed for memberId=${request.memberId}: ${result.error}" }
                result
            }
            is NetworkResult.Success -> {
                val applicationResult = result.data.toDomainModel()
                Logger.i(TAG) { "applyLoan: succeeded loanId=${applicationResult.loanId}" }
                NetworkResult.Success(applicationResult)
            }
        }
    }
}
