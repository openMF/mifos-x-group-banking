/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanlist

/**
 * Append-only test-tag registry for the `loan-list` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5 —
 * names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/loan-list/src/commonTest/` and by the Maestro flow
 * generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }`
 * selectors from these constants. Mirrors `GroupListTestTags`'s identical convention. See
 * API.md#tags.
 */
object LoanListTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "loan_list_screen"

    /** `filter_chips_row`'s "All" chip — [LoanListAction.OnFilterChange] with `LoanStatusFilter.ALL`. */
    const val FILTER_CHIP_ALL: String = "loan_list_filter_chip_all"

    /** `filter_chips_row`'s "Active" chip — [LoanListAction.OnFilterChange] with `LoanStatusFilter.ACTIVE`. */
    const val FILTER_CHIP_ACTIVE: String = "loan_list_filter_chip_active"

    /** `filter_chips_row`'s "Overdue" chip — [LoanListAction.OnFilterChange] with `LoanStatusFilter.OVERDUE`. */
    const val FILTER_CHIP_OVERDUE: String = "loan_list_filter_chip_overdue"

    /** `filter_chips_row`'s "Closed" chip — [LoanListAction.OnFilterChange] with `LoanStatusFilter.CLOSED`. */
    const val FILTER_CHIP_CLOSED: String = "loan_list_filter_chip_closed"

    /** Circular loading indicator + skeleton rows — `LoanListScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "loan_list_loading_indicator"

    /** Scrollable list surface wrapping the loan cards — `LoanListScreenState.Content`. */
    const val LOAN_LIST: String = "loan_list_lazy_column"

    /** `+ Apply for Loan` FAB — [LoanListAction.OnApplyLoan]; visible only when `canApplyLoan`. */
    const val FAB_APPLY: String = "loan_list_fab_apply"

    /** Full-illustration empty surface — `LoanListScreenState.Empty` (no loans for the selected filter). */
    const val EMPTY_SECTION: String = "loan_list_empty_section"

    /** Inline error surface — `LoanListScreenState.Error`. */
    const val ERROR_SECTION: String = "loan_list_error_section"

    /** Retry CTA on the error state — [LoanListAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "loan_list_error_retry_button"

    /**
     * Resolves the stable per-card test tag for a given loan id. Cards render data-driven from
     * `LoanListState.filteredLoans` (not as N separate composables), so this is a function rather
     * than a fixed constant set — mirrors `GroupListTestTags.cardTag`.
     */
    fun cardTag(loanId: Long): String = "loan_list_card_$loanId"
}
