/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail

/**
 * Append-only test-tag registry for the `member-savings-detail` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Consumed by Compose UI tests under
 * `feature/member-savings-detail/src/commonTest/` and by the Maestro flow generator
 * (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }` selectors from
 * these constants. Constant names are derived 1:1 from `ui.yaml#components[].id`. Mirrors
 * `MemberProfileTestTags`'s/`LoanListTestTags`'s identical convention. See API.md#tags.
 */
object MemberSavingsDetailTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "member_savings_detail_screen"

    /** `ui.yaml#components.member_header_card`. */
    const val HEADER_CARD: String = "member_savings_detail_header_card"

    /** `ui.yaml#components.member_header_card.content.member_avatar`. */
    const val AVATAR: String = "member_savings_detail_avatar"

    /** `ui.yaml#components.savings_sparkline_card`. */
    const val SPARKLINE_CARD: String = "member_savings_detail_sparkline_card"

    /** `ui.yaml#components.savings_sparkline_card.content.sparkline_chart`. */
    const val SPARKLINE: String = "member_savings_detail_sparkline"

    /** `ui.yaml#components.filter_chips_row`. */
    const val FILTER_CHIPS_ROW: String = "member_savings_detail_filter_chips_row"

    /** `ui.yaml#components.filter_chips_row.chips.chip_all` — [MemberSavingsDetailAction.OnFilterSelected] with `SavingsTransactionFilter.ALL`. */
    const val FILTER_CHIP_ALL: String = "member_savings_detail_filter_chip_all"

    /** `ui.yaml#components.filter_chips_row.chips.chip_deposits` — [MemberSavingsDetailAction.OnFilterSelected] with `SavingsTransactionFilter.DEPOSITS`. */
    const val FILTER_CHIP_DEPOSITS: String = "member_savings_detail_filter_chip_deposits"

    /** `ui.yaml#components.filter_chips_row.chips.chip_withdrawals` — [MemberSavingsDetailAction.OnFilterSelected] with `SavingsTransactionFilter.WITHDRAWALS`. */
    const val FILTER_CHIP_WITHDRAWALS: String = "member_savings_detail_filter_chip_withdrawals"

    /** Scrollable surface wrapping header/sparkline/filters + paginated transaction rows — `MemberSavingsDetailScreenState.Content`. */
    const val TRANSACTION_LIST: String = "member_savings_detail_transaction_list"

    /** `ui.yaml#components.load_more_indicator` — visible only when `isLoadingNextPage`. */
    const val LOAD_MORE_INDICATOR: String = "member_savings_detail_load_more_indicator"

    /** `ui.yaml#components.shimmer_list` — `MemberSavingsDetailScreenState.Loading`. */
    const val LOADING_SECTION: String = "member_savings_detail_loading_section"

    /** `ui.yaml#components.empty_state` — `MemberSavingsDetailScreenState.Empty`. */
    const val EMPTY_SECTION: String = "member_savings_detail_empty_section"

    /** `ui.yaml#components.error_state` — `MemberSavingsDetailScreenState.Error`. */
    const val ERROR_SECTION: String = "member_savings_detail_error_section"

    /** Retry CTA on the error state — [MemberSavingsDetailAction.Retry]; hidden when [org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsError.retry] is `false`. */
    const val ERROR_RETRY_BUTTON: String = "member_savings_detail_error_retry_button"

    /**
     * Resolves the stable per-row test tag for a given transaction id.
     * `ui.yaml#components.transaction_card` renders data-driven from
     * `MemberSavingsDetailState.filteredTransactions` (not as N separate composables), so this is a
     * function rather than a fixed constant set — mirrors `PersonalSavingsTestTags.transactionRowTag`.
     */
    fun transactionRowTag(transactionId: String): String = "member_savings_detail_transaction_row_$transactionId"

    /** Resolves the stable per-row EXPANDED-detail test tag for a given transaction id — visible only when `expandedTransactionId == transactionId`. */
    fun transactionExpandedTag(transactionId: String): String = "member_savings_detail_transaction_expanded_$transactionId"
}
