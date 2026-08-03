/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for a single row of `MemberDashboardResponseDto.recentTransactions` — the CANONICAL
 * `SavingsTransactionDto` shape sourced from
 * `idea-layer/screens/personal-dashboard/api.yaml#dtos.SavingsTransactionDto` (nested in the
 * companion `GET /companion/member/dashboard` response, COMP-DASH-001). Kept deliberately
 * lightweight (companion-normalized `id`/`date`/`type`/`amount` — no raw Fineract
 * `transactionType` object, `date` component array, `runningBalance`, or `currency` object)
 * because the member-dashboard card only needs a compact recent-activity list, not the full
 * ledger row a savings-account statement screen would need.
 *
 * **Registry + cross-feature divergence note (flagged for the cross-feature repair station):**
 * `idea-layer/dtos/SavingsTransactionDto.yaml` (registry v1.0.0) declares a DIFFERENT, richer
 * Fineract-raw shape (`id: Long`, `memberId`, `savingsAccountId`, `transactionType` with 4 values
 * incl. `interest_posting`/`fee_deduction`, `currency`, `runningBalance: Double?`, `note: String?`)
 * sourced from `GET /savingsaccounts/{accountId}/transactions` and lists its `end-user-dashboard`
 * consumer against api_id `list_self_savings` — that api_id does not exist on this (approved)
 * feature's `api.yaml`; `personal-dashboard/docs.yaml` states the single companion
 * `/companion/member/dashboard` call REPLACES the old per-account SelfService path, so the
 * registry entry pre-dates that migration and is stale for this consumer. Additionally,
 * `idea-layer/screens/personal-savings/api.yaml#dtos.SavingsTransactionDto` declares a THIRD,
 * still-richer shape (`id: Long`, `transactionType: {value, code, description}`,
 * `date: List<Int>`, `runningBalance`, `currency: {code, displaySymbol}` — raw Fineract
 * SelfService component shape) under the SAME DTO name. This file emits the shape approved on
 * `personal-dashboard`'s own `api.yaml` per Hard Rule 5 (`@SerialName` must match the declaring
 * feature's contract). Before `personal-savings` is generated, the naming collision (same class
 * name `SavingsTransactionDto`, three incompatible shapes across registry / personal-savings /
 * personal-dashboard) MUST be resolved at Station 3 — e.g. rename personal-savings' raw ledger
 * row to `SavingsLedgerEntryDto` and keep `SavingsTransactionDto` as this compact companion shape,
 * or migrate personal-savings onto the companion API and reuse this DTO outright.
 *
 * See API.md#dtos — SavingsTransaction.
 */
@Serializable
data class SavingsTransactionDto(
    @SerialName("id") val id: String,
    @SerialName("date") val date: String,
    @SerialName("type") val type: TransactionTypeDto = TransactionTypeDto.UNKNOWN,
    @SerialName("amount") val amount: Double,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for the direction of a [SavingsTransactionDto]. [UNKNOWN] fallback per T7/EC30 so a
 * server-added transaction type (e.g. `INTEREST_POSTING`, `FEE_DEDUCTION` — both present in the
 * richer registry/personal-savings shapes but NOT part of this compact companion contract) never
 * crashes an old client.
 */
@Serializable
enum class TransactionTypeDto {
    @SerialName("DEPOSIT")
    DEPOSIT,

    @SerialName("WITHDRAWAL")
    WITHDRAWAL,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
