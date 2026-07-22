/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.database.loandetail.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room persisted representation of ONE loan-detail composite snapshot per loan
 * (`GET /loans/{loanId}?associations=repaymentSchedule,transactions`).
 *
 * Backs the offline cache (SourceOfTruth) for the single-key NETWORK_WITH_CACHE loan-detail store
 * ([org.mifos.groupbanking.core.store.loandetail.impl.provideLoanDetailStore]), a **single-key**
 * read: one row per loan so a cold start with no network still renders the last-seen loan header +
 * repayment schedule + transaction history (`data-flow.yaml#cache_strategy.offline`, SC2 — never
 * memory-only). Keyed by [loanId] = the store key (also the loan header's own `id`, they are one
 * and the same identifier for this single-endpoint composite).
 *
 * The whole `LoanDetailResponse` domain object is persisted as ONE row per loan (task-sanctioned
 * single-row shape): the flat loan-header fields are scalar columns, and the two nested collections
 * `repaymentSchedule` + `repaymentHistory` are folded into [repaymentScheduleJson] /
 * [repaymentHistoryJson] JSON columns via
 * [org.mifos.groupbanking.core.database.loandetail.entity.LoanDetailCacheCodec]. A single-row upsert
 * is inherently atomic, so there is no delete-then-upsert race (RULE-IMPLEMENT-STORE5-001 S5-3
 * satisfied by construction).
 *
 * [status] is persisted as the `LoanAccountStatus` enum `name` string and re-parsed on read with an
 * `UNKNOWN` fallback for forward compatibility.
 *
 * [fetchedAt] is the epoch-millis write time — used only for TTL pruning ([LoanDetailDao
 * .deleteOlderThan]); freshness for the UI banner is tracked separately by the framework
 * `framework_fetched_at` table via `FetchedAtRepository`.
 *
 * See API.md#stores — LoanDetailResponse.
 */
@Entity(tableName = "loan_detail_cache")
data class LoanDetailCacheEntity(
    @PrimaryKey
    val loanId: Long,
    val memberId: Long,
    val memberName: String,
    val loanProductName: String,
    val principalAmount: Double,
    val disbursedDate: String,
    val interestRatePercent: Double,
    val totalOutstanding: Double,
    val totalOverdue: Double,
    val status: String,
    val fineractLoanId: Long,
    val repaymentScheduleJson: String,
    val repaymentHistoryJson: String,
    val fetchedAt: Long,
)
