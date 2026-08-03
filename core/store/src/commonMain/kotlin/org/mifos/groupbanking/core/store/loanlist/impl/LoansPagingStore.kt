/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.loanlist.impl

import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.base.store.paging.PageKey
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.loanlist.dao.LoanListDao
import org.mifos.groupbanking.core.database.loanlist.entity.LoanListEntity
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanSummary
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.service.loanlist.LoanApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the PAGINATED read-only NETWORK_WITH_CACHE [Store] for the loan-list screen — the
 * offset-paginated list of loan accounts belonging to a savings group
 * (`GET /groups/{groupId}/loans`).
 *
 * The key is a [PageKey] (offset paging, `page_size=20` per `api.yaml#pagination`) that carries the
 * groupId in its [PageKey.query] field as its String form, so Store5 caches each `(groupId, page)`
 * slice independently; the value is one page slice `List<LoanSummary>`. The read side is exposed to
 * the UI exclusively through `LoanRepository.loansPagingStream(...)` → `.asPagingScreenStream(...)`
 * — there is no DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the list is read-only so
 * there is no write path (S5-1). The status-filter chips filter the emitted list client-side; they
 * never re-key the store.
 *
 * - **Fetcher** — [LoanApi.getGroupLoans] with `groupId = key.query.toLong()`, `offset = key.offset`
 *   (`= page * pageSize`) and `limit = key.pageSize`. The service returns a sealed [NetworkResult];
 *   on [NetworkResult.Success] the [org.mifos.groupbanking.core.network.model.LoanPageDto] is
 *   mapped to domain via [toDomainModel] and its `loans` slice returned; on [NetworkResult.Error]
 *   the fetcher throws so Store5 routes it to an error response (no try-catch, no `Result` envelope
 *   — Store5 owns the error channel).
 * - **SourceOfTruth** — a Room table ([LoanListDao]) so pages survive process death and a cold
 *   start with no network still renders the last-seen pages
 *   (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only). The writer swaps ONE
 *   `(groupId, page)`'s rows atomically via [LoanListDao.replacePage] (single `@Transaction`,
 *   guards S5-PAGE-ATOMIC / S5-3 — no delete-then-upsert race), then calls
 *   [DefaultValidator.markFresh] so the TTL window opens on the successful network write (S5-5
 *   cold-start-stale guard).
 * - **Validator** — TTL 3m ([AppStoreRegistry.Ttl.LOAN_LIST]) matching `data-flow.yaml`
 *   `ttl_seconds: 180` (stale-while-revalidate).
 *
 * See API.md#stores — LoanList.
 */
fun provideLoansPagingStore(
    api: LoanApi,
    dao: LoanListDao,
): Store<PageKey, List<LoanSummary>> {
    val validator = DefaultValidator.withTtl<List<LoanSummary>>(
        AppStoreRegistry.Ttl.LOAN_LIST,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { key: PageKey ->
            when (
                val result = api.getGroupLoans(
                    groupId = key.groupId.toLong(),
                    limit = key.pageSize,
                    offset = key.offset,
                )
            ) {
                is NetworkResult.Success -> result.data.toDomainModel().loans
                is NetworkResult.Error -> throw LoanListFetchException(result.error)
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: PageKey ->
                dao.observePage(key.groupId, key.page).map { rows ->
                    if (rows.isEmpty()) null else rows.toDomainModels()
                }
            },
            writer = { key: PageKey, loans: List<LoanSummary> ->
                dao.replacePage(key.groupId, key.page, loans.toEntities(key.groupId, key.page))
                validator.markFresh()
            },
            delete = { key: PageKey -> dao.deletePage(key.groupId, key.page) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Signals a failed loan-list page fetch to Store5's error channel. Carries the sealed
 * [NetworkError] so downstream error mapping (feature-layer `AppErrorMapper`) can branch on the
 * exact cause; the message is `categorize()`-friendly for the default state routing.
 */
class LoanListFetchException(
    val networkError: NetworkError,
) : Exception("Loan list fetch failed: $networkError")

// ---------------------------------------------------------------------------
// groupId is threaded through PageKey.query (as its String form) so the store stays
// Store<PageKey, List<LoanSummary>> (the exact shape asPagingScreenStream requires) while the cache
// remains per-group. The repository always passes a non-null groupId as the paging query, so a
// missing query is a wiring bug, not a runtime input — hence requireNotNull.
// ---------------------------------------------------------------------------

private val PageKey.groupId: String
    get() = requireNotNull(query) {
        "LoansPagingStore requires the groupId carried in PageKey.query"
    }

// ---------------------------------------------------------------------------
// Inline entity <-> domain mapping — private to this store (MembersPagingStore precedent).
// core/store depends on core/model + core/database, so mapping lives here rather than adding a
// core/model dependency to core/database. status is persisted as the enum .name string and
// re-resolved on read (UNKNOWN-absorbing) so a server-added value never breaks a cached row.
// ---------------------------------------------------------------------------

private fun List<LoanListEntity>.toDomainModels(): List<LoanSummary> = map { it.toDomain() }

private fun LoanListEntity.toDomain(): LoanSummary = LoanSummary(
    id = loanId,
    memberId = memberId,
    memberName = memberName,
    memberPhotoUrl = memberPhotoUrl,
    loanProductName = loanProductName,
    principalAmount = principalAmount,
    outstandingBalance = outstandingBalance,
    overdueAmount = overdueAmount,
    status = status.toLoanAccountStatus(),
    nextRepaymentDate = nextRepaymentDate,
    isOverdue = isOverdue,
    fineractLoanId = fineractLoanId,
)

private fun List<LoanSummary>.toEntities(groupId: String, pageIndex: Int): List<LoanListEntity> {
    val now = Clock.System.now().toEpochMilliseconds()
    return mapIndexed { index, loan -> loan.toEntity(groupId, pageIndex, index, now) }
}

private fun LoanSummary.toEntity(
    groupId: String,
    pageIndex: Int,
    rowOrder: Int,
    fetchedAt: Long,
): LoanListEntity = LoanListEntity(
    groupId = groupId,
    loanId = id,
    pageIndex = pageIndex,
    rowOrder = rowOrder,
    memberId = memberId,
    memberName = memberName,
    memberPhotoUrl = memberPhotoUrl,
    loanProductName = loanProductName,
    principalAmount = principalAmount,
    outstandingBalance = outstandingBalance,
    overdueAmount = overdueAmount,
    status = status.name,
    nextRepaymentDate = nextRepaymentDate,
    isOverdue = isOverdue,
    fineractLoanId = fineractLoanId,
    fetchedAt = fetchedAt,
)

private fun String.toLoanAccountStatus(): LoanAccountStatus =
    LoanAccountStatus.entries.firstOrNull { it.name == this } ?: LoanAccountStatus.UNKNOWN
