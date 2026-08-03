/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.loanlist.entity

import androidx.room3.Entity
import androidx.room3.Index

/**
 * Room persisted representation of one cached loan-list row (`GET /groups/{groupId}/loans`).
 *
 * Backs the offline cache (SourceOfTruth) for the loan-list's PAGINATED NETWORK_WITH_CACHE
 * store ([org.mifos.groupbanking.core.store.loanlist.impl.provideLoansPagingStore]) so a cold
 * start with no network still renders the last-seen pages
 * (`data-flow.yaml#cache.offline: show_cached`, SC2 — never memory-only).
 *
 * The cache is per-group AND per-page: the composite primary key is [groupId] + [loanId] (a loan
 * account appears exactly once within a group's list — the natural business key scoped by group),
 * so distinct groups never collide. [pageIndex] records which offset page the row belongs to so the
 * store's per-page reader/writer can slice the cache, and [rowOrder] preserves server order within
 * a page. [groupId] is stored as the String form of the numeric group id (the value threaded through
 * [kpt.core.base.store.paging.PageKey.query]) so the store keys line up 1:1 with the persisted rows.
 *
 * [status] is persisted as the enum `.name` string (mapped back on read by the store) so a
 * server-added value never breaks the schema.
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning
 * ([org.mifos.groupbanking.core.database.loanlist.dao.LoanListDao.deleteOlderThan]); freshness
 * for the UI banner is tracked separately by the framework `framework_fetched_at` table via
 * `FetchedAtRepository`.
 *
 * See API.md#stores — LoanList.
 */
@Entity(
    tableName = "loan_list_cache",
    primaryKeys = ["groupId", "loanId"],
    indices = [Index(value = ["groupId", "pageIndex"])],
)
data class LoanListEntity(
    val groupId: String,
    val loanId: Long,
    val pageIndex: Int,
    val rowOrder: Int,
    val memberId: Long,
    val memberName: String,
    val memberPhotoUrl: String?,
    val loanProductName: String,
    val principalAmount: Double,
    val outstandingBalance: Double,
    val overdueAmount: Double,
    val status: String,
    val nextRepaymentDate: String?,
    val isOverdue: Boolean,
    val fineractLoanId: Long,
    val fetchedAt: Long,
)
