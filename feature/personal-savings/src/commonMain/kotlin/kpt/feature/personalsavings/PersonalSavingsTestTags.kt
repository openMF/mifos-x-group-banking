/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalsavings

/**
 * Append-only test-tag registry for the `personal-savings` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/personal-savings/src/commonTest/` and by
 * the Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object PersonalSavingsTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "personal_savings_screen"

    /** `savings_tab_row`'s [androidx.compose.material3.TabRow] container — always rendered. */
    const val TAB_ROW: String = "personal_savings_tab_row"

    /** GROUP_LINKED tab — [PersonalSavingsAction.OnTabSelected] with `SavingsTab.GROUP_LINKED`. */
    const val TAB_GROUP_LINKED: String = "personal_savings_tab_group_linked"

    /** INDIVIDUAL tab — [PersonalSavingsAction.OnTabSelected] with `SavingsTab.INDIVIDUAL`; rendered only when `individualSavingsId != null`. */
    const val TAB_INDIVIDUAL: String = "personal_savings_tab_individual"

    /** Shimmer skeleton column — `PersonalSavingsScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "personal_savings_loading_indicator"

    /** Scrollable surface wrapping the balance card + progress card + transaction rows — `PersonalSavingsScreenState.Content`. */
    const val TRANSACTION_LIST: String = "personal_savings_transaction_list"

    /** `balance_hero_card` — active-tab balance + account number. */
    const val BALANCE_CARD: String = "personal_savings_balance_card"

    /** `contribution_progress_card` — visible only when `selectedTab == GROUP_LINKED`. */
    const val CONTRIBUTION_PROGRESS_CARD: String = "personal_savings_contribution_progress_card"

    /** Empty-individual-account promo surface — `selectedTab == INDIVIDUAL && individualSavingsId == null`. */
    const val EMPTY_INDIVIDUAL_SECTION: String = "personal_savings_empty_individual_section"

    /** Inline error surface — `PersonalSavingsScreenState.Error`. */
    const val ERROR_SECTION: String = "personal_savings_error_section"

    /** Retry CTA on the error state — [PersonalSavingsAction.OnRetry]; hidden when `SavingsError.retry == false`. */
    const val ERROR_RETRY_BUTTON: String = "personal_savings_error_retry_button"

    /**
     * Resolves the stable per-row test tag for a given transaction id. Rows render data-driven
     * from `PersonalSavingsState.groupLinkedTransactions`/`individualTransactions` (not as N
     * separate composables), so this is a function rather than a fixed constant set — mirrors
     * `PersonalLoansTestTags.cardTag`.
     */
    fun transactionRowTag(transactionId: Long): String = "personal_savings_transaction_row_$transactionId"
}
