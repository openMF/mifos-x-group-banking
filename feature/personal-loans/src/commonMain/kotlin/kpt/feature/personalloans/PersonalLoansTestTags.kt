/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalloans

/**
 * Append-only test-tag registry for the `personal-loans` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/personal-loans/src/commonTest/` and by
 * the Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. Mirrors `LoanListTestTags`'s
 * identical convention. See API.md#tags.
 */
object PersonalLoansTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "personal_loans_screen"

    /** `filter_chips_row`'s "All" chip — [PersonalLoansAction.OnFilterChange] with `LoanStatusFilter.ALL`. */
    const val FILTER_CHIP_ALL: String = "personal_loans_filter_chip_all"

    /** `filter_chips_row`'s "Active" chip — [PersonalLoansAction.OnFilterChange] with `LoanStatusFilter.ACTIVE`. */
    const val FILTER_CHIP_ACTIVE: String = "personal_loans_filter_chip_active"

    /** `filter_chips_row`'s "Closed" chip — [PersonalLoansAction.OnFilterChange] with `LoanStatusFilter.CLOSED`. */
    const val FILTER_CHIP_CLOSED: String = "personal_loans_filter_chip_closed"

    /** Shimmer skeleton column — `PersonalLoansScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "personal_loans_loading_indicator"

    /** Scrollable list surface wrapping the loan cards — `PersonalLoansScreenState.Content`. */
    const val LOAN_LIST: String = "personal_loans_lazy_column"

    /** `+ Request Loan` FAB — [PersonalLoansAction.OnRequestLoanClick]; visible only in `Content`. */
    const val FAB_REQUEST_LOAN: String = "personal_loans_fab_request_loan"

    /** Full-illustration empty surface — `PersonalLoansScreenState.Empty` (member has no loans). */
    const val EMPTY_SECTION: String = "personal_loans_empty_section"

    /** "Request a Loan" CTA button on the empty state — [PersonalLoansAction.OnRequestLoanClick]. */
    const val EMPTY_ACTION_BUTTON: String = "personal_loans_empty_action_button"

    /** Inline error surface — `PersonalLoansScreenState.Error`. */
    const val ERROR_SECTION: String = "personal_loans_error_section"

    /** Retry CTA on the error state — [PersonalLoansAction.OnRetry]; hidden when `LoanError.retry == false`. */
    const val ERROR_RETRY_BUTTON: String = "personal_loans_error_retry_button"

    /**
     * Resolves the stable per-card test tag for a given loan id. Cards render data-driven from
     * `PersonalLoansState.filteredLoans` (not as N separate composables), so this is a function
     * rather than a fixed constant set — mirrors `LoanListTestTags.cardTag`.
     */
    fun cardTag(loanId: Long): String = "personal_loans_card_$loanId"

    /**
     * Resolves the stable per-card expanded-details test tag for a given loan id —
     * `PersonalLoansAction.OnLoanExpand` toggle target (`selectedLoanId == loan.id`).
     */
    fun cardDetailsTag(loanId: Long): String = "personal_loans_card_details_$loanId"
}
