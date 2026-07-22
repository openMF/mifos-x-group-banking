/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.loandetail.impl

import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.loandetail.dao.LoanDetailDao
import org.mifos.groupbanking.core.database.loandetail.entity.CachedRepaymentScheduleRow
import org.mifos.groupbanking.core.database.loandetail.entity.CachedRepaymentTransaction
import org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheCodec
import org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheEntity
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanDetail
import org.mifos.groupbanking.core.model.LoanDetailResponse
import org.mifos.groupbanking.core.model.RepaymentRowStatus
import org.mifos.groupbanking.core.model.RepaymentScheduleRow
import org.mifos.groupbanking.core.model.RepaymentTransaction
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the **single-key** read-only NETWORK_WITH_CACHE [Store] for the loan-detail screen
 * (`GET /loans/{loanId}?associations=repaymentSchedule,transactions`) that backs the loan-detail
 * composite view.
 *
 * The key is the `Long` loanId and the value is one composite [LoanDetailResponse] snapshot (loan
 * header + repayment schedule + transaction history) resolved by the single endpoint in one round
 * trip; Store5 caches each loan independently, so re-opening a loan re-uses that loan's per-key
 * cache or re-fetches if stale. The read side is exposed to the UI exclusively through
 * `LoanDetailRepository.loanDetailStream(...)` → `.asScreenStream(...)` — there is no DAO-bypass
 * read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the loan-detail screen is read-only
 * (`data-flow.yaml#cache_strategy`, no write) so there is no write path (S5-1). This is a NEW store
 * — the existing paginated `LoansPagingStore` is deliberately not overloaded (single-key composite
 * read vs. paged list are distinct Store shapes).
 *
 * - **Fetcher** — [LoanDetailApi.getLoanDetail] with the loanId decoded from the store key. The
 *   service returns a sealed [NetworkResult]; on [NetworkResult.Success] the composite DTO is mapped
 *   to domain via [toDomainModel], on [NetworkResult.Error] the fetcher throws so Store5 routes it
 *   to an error response (no try-catch, no `Result` envelope — Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([LoanDetailDao]) so each loan's detail snapshot survives
 *   process death and a cold start with no network still renders the last-seen loan header +
 *   schedule + history (`data-flow.yaml#cache_strategy.offline`, SC2 — never memory-only). The whole
 *   snapshot is one row per loan, so the writer's keyed [LoanDetailDao.replaceForKey] upsert is
 *   inherently atomic (guards S5-3 — no delete-then-upsert race); it then calls
 *   [DefaultValidator.markFresh] so the TTL window opens on the successful network write (S5-5
 *   cold-start-stale guard).
 * - **Validator** — TTL 2m ([AppStoreRegistry.Ttl.LOAN_DETAIL]) matching `data-flow.yaml`
 *   `cache_strategy: stale_while_revalidate`, `ttl: 120`.
 *
 * See API.md#stores — LoanDetailResponse.
 */
fun provideLoanDetailStore(
    api: LoanDetailApi,
    dao: LoanDetailDao,
): Store<Long, LoanDetailResponse> {
    val validator = DefaultValidator.withTtl<LoanDetailResponse>(
        AppStoreRegistry.Ttl.LOAN_DETAIL,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { key: Long ->
            when (val result = api.getLoanDetail(loanId = key)) {
                is NetworkResult.Success -> result.data.toDomainModel()
                is NetworkResult.Error -> throw LoanDetailFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: Long ->
                dao.observeByKey(key).map { row -> row?.toDomain() }
            },
            writer = { key: Long, response: LoanDetailResponse ->
                dao.replaceForKey(response.toEntity(key))
                validator.markFresh()
            },
            delete = { key: Long -> dao.deleteByKey(key) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed loan-detail fetch to Store5's error channel. Carries the sealed [NetworkError] so
 * downstream error mapping (feature-layer `AppErrorMapper`) can branch on the exact cause (401 →
 * login, 404 → loan-not-found, offline → cache fallback per `data-flow.yaml`); the message is
 * `categorize()`-friendly for the default state routing.
 */
class LoanDetailFetchException(
    val networkError: NetworkError,
) : Exception("Loan detail fetch failed: $networkError")

// ---------------------------------------------------------------------------
// Inline entity <-> domain mapping — private to this store (MemberDashboardStore precedent).
// core/store depends on core/model + core/database, so mapping lives here rather than adding a
// core/model dependency to core/database. The two nested lists round-trip through
// LoanDetailCacheCodec (core/database owns kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun LoanDetailResponse.toEntity(cacheKey: Long): LoanDetailCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return LoanDetailCacheEntity(
        loanId = cacheKey,
        memberId = loan.memberId,
        memberName = loan.memberName,
        loanProductName = loan.loanProductName,
        principalAmount = loan.principalAmount,
        disbursedDate = loan.disbursedDate,
        interestRatePercent = loan.interestRatePercent,
        totalOutstanding = loan.totalOutstanding,
        totalOverdue = loan.totalOverdue,
        status = loan.status.name,
        fineractLoanId = loan.fineractLoanId,
        repaymentScheduleJson = LoanDetailCacheCodec.encodeSchedule(
            repaymentSchedule.map { it.toPayload() },
        ),
        repaymentHistoryJson = LoanDetailCacheCodec.encodeHistory(
            repaymentHistory.map { it.toPayload() },
        ),
        fetchedAt = now,
    )
}

private fun LoanDetailCacheEntity.toDomain(): LoanDetailResponse = LoanDetailResponse(
    loan = LoanDetail(
        id = loanId,
        memberId = memberId,
        memberName = memberName,
        loanProductName = loanProductName,
        principalAmount = principalAmount,
        disbursedDate = disbursedDate,
        interestRatePercent = interestRatePercent,
        totalOutstanding = totalOutstanding,
        totalOverdue = totalOverdue,
        status = status.toLoanAccountStatus(),
        fineractLoanId = fineractLoanId,
    ),
    repaymentSchedule = LoanDetailCacheCodec.decodeSchedule(repaymentScheduleJson).map { it.toDomain() },
    repaymentHistory = LoanDetailCacheCodec.decodeHistory(repaymentHistoryJson).map { it.toDomain() },
)

private fun RepaymentScheduleRow.toPayload(): CachedRepaymentScheduleRow = CachedRepaymentScheduleRow(
    weekNumber = weekNumber,
    dueDate = dueDate,
    dueAmount = dueAmount,
    paidAmount = paidAmount,
    balance = balance,
    status = status.name,
)

private fun CachedRepaymentScheduleRow.toDomain(): RepaymentScheduleRow = RepaymentScheduleRow(
    weekNumber = weekNumber,
    dueDate = dueDate,
    dueAmount = dueAmount,
    paidAmount = paidAmount,
    balance = balance,
    status = status.toRepaymentRowStatus(),
)

private fun RepaymentTransaction.toPayload(): CachedRepaymentTransaction = CachedRepaymentTransaction(
    id = id,
    type = type,
    date = date,
    amount = amount,
)

private fun CachedRepaymentTransaction.toDomain(): RepaymentTransaction = RepaymentTransaction(
    id = id,
    type = type,
    date = date,
    amount = amount,
)

private fun String.toLoanAccountStatus(): LoanAccountStatus =
    LoanAccountStatus.entries.firstOrNull { it.name == this } ?: LoanAccountStatus.UNKNOWN

private fun String.toRepaymentRowStatus(): RepaymentRowStatus =
    RepaymentRowStatus.entries.firstOrNull { it.name == this } ?: RepaymentRowStatus.UNKNOWN
