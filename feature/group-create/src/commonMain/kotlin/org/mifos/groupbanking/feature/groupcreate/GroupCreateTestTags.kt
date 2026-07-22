/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate

/**
 * Append-only test-tag registry for the `group-create` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/group-create/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. Next/Back/Submit reuse a single
 * tag each across the 4 wizard steps — `ui.yaml#components` declares exactly one `next_button` /
 * `back_step_button` / `submit_button` component id, and only one of them is ever composed on
 * screen at a time (visibility is gated by `currentStep`), so per-step duplicate tags would add
 * no Maestro-selector value. See API.md#tags.
 */
object GroupCreateTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "group_create_screen"

    /** Top app bar (`ui.yaml#components.top_bar`) — close icon dismisses the wizard. */
    const val TOP_BAR: String = "group_create_top_bar"

    /** `groupTypeName` chip badge — `ui.yaml#components.group_type_banner`. */
    const val TYPE_BANNER: String = "group_create_type_banner"

    /** 4-step progress indicator — `ui.yaml#components.step_indicator`. */
    const val STEP_INDICATOR: String = "group_create_step_indicator"

    // -- Step 1: Identity ------------------------------------------------------------------------

    /** `group_name_field`. */
    const val FIELD_GROUP_NAME: String = "group_create_field_group_name"

    /** `office_dropdown`. */
    const val DROPDOWN_OFFICE: String = "group_create_dropdown_office"

    /** `currency_dropdown`. */
    const val DROPDOWN_CURRENCY: String = "group_create_dropdown_currency"

    /** `meeting_day_dropdown`. */
    const val DROPDOWN_MEETING_DAY: String = "group_create_dropdown_meeting_day"

    /** `meeting_time_picker`. */
    const val FIELD_MEETING_TIME: String = "group_create_field_meeting_time"

    // -- Step 2: Type-adaptive Rules ---------------------------------------------------------------

    /** `share_value_field` — visible when `isShareBasedContribution`. */
    const val FIELD_SHARE_VALUE: String = "group_create_field_share_value"

    /** `share_min_field` — visible when `isShareBasedContribution`. */
    const val FIELD_SHARE_MIN: String = "group_create_field_share_min"

    /** `share_max_field` — visible when `isShareBasedContribution`. */
    const val FIELD_SHARE_MAX: String = "group_create_field_share_max"

    /** `contribution_amount_field` — visible when NOT `isShareBasedContribution`. */
    const val FIELD_CONTRIBUTION_AMOUNT: String = "group_create_field_contribution_amount"

    /** `payout_order_dropdown` — visible when `isRotatingPayout`. */
    const val DROPDOWN_PAYOUT_ORDER: String = "group_create_dropdown_payout_order"

    /** `loan_multiplier_field`. */
    const val FIELD_LOAN_MULTIPLIER: String = "group_create_field_loan_multiplier"

    /** `interest_rate_field`. */
    const val FIELD_INTEREST_RATE: String = "group_create_field_interest_rate"

    /** `cycle_length_field`. */
    const val FIELD_CYCLE_LENGTH: String = "group_create_field_cycle_length"

    /** `fine_amount_field`. */
    const val FIELD_FINE_AMOUNT: String = "group_create_field_fine_amount"

    /** `social_fund_toggle`. */
    const val SWITCH_SOCIAL_FUND: String = "group_create_switch_social_fund"

    /** `social_fund_percent_field` — visible when `socialFundEnabled`. */
    const val FIELD_SOCIAL_FUND_PERCENT: String = "group_create_field_social_fund_percent"

    // -- Step 3: Members ---------------------------------------------------------------------------

    /** `max_members_field`. */
    const val FIELD_MAX_MEMBERS: String = "group_create_field_max_members"

    // -- Step 4: Review + shared wizard navigation ---------------------------------------------------

    /** `review_card` — Step 4 only. */
    const val REVIEW_CARD: String = "group_create_review_card"

    /** `next_button` — Steps 1-3 (reused; only one is ever composed at a time). */
    const val NEXT_BUTTON: String = "group_create_next_button"

    /** `back_step_button` — Steps 2-4 (reused; only one is ever composed at a time). */
    const val BACK_BUTTON: String = "group_create_back_button"

    /** `submit_button` — Step 4 (label switches to "Retry" on `GroupCreateScreenState.Error`). */
    const val SUBMIT_BUTTON: String = "group_create_submit_button"

    // -- Error / offline surfaces ---------------------------------------------------------------------

    /** Inline error banner — `GroupCreateScreenState.Error`. */
    const val ERROR_BANNER: String = "group_create_error_banner"

    /** `offline_notice_banner` — visible when `isOffline`. */
    const val OFFLINE_NOTICE_BANNER: String = "group_create_offline_notice_banner"

    /** `GroupCreateEvent.ShowOfflineSyncDialog` modal. */
    const val OFFLINE_SYNC_DIALOG: String = "group_create_offline_sync_dialog"

    /** Dismiss/acknowledge button on [OFFLINE_SYNC_DIALOG]. */
    const val OFFLINE_SYNC_DIALOG_CONFIRM: String = "group_create_offline_sync_dialog_confirm"

    // -- Success (transient) --------------------------------------------------------------------------

    /** Success celebration surface — `GroupCreateScreenState.Success` (transient, nav fires same frame). */
    const val SUCCESS_SECTION: String = "group_create_success_section"
}
