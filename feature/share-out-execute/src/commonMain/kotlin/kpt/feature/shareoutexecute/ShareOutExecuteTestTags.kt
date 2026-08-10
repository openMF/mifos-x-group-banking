/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.shareoutexecute

/**
 * Append-only test-tag registry for the `share-out-execute` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests + the Maestro flow generator. Constant names are derived 1:1 from
 * `ui.yaml#components[].id`. See API.md#tags.
 */
object ShareOutExecuteTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "share_out_execute_screen"

    /** `ui.yaml#components.top_bar` back navigation icon. */
    const val BACK_ACTION: String = "share_out_execute_back_action"

    /** `ui.yaml#components.summary_card`. */
    const val SUMMARY_CARD: String = "share_out_execute_summary_card"

    /** `ui.yaml#components.member_payout_row` (ACCUMULATING list). */
    const val MEMBER_PAYOUT_LIST: String = "share_out_execute_member_payout_list"

    /** `ui.yaml#components.rotation_execute_card` (ROTATING_PAYOUT single-recipient card). */
    const val ROTATION_EXECUTE_CARD: String = "share_out_execute_rotation_card"

    /** `ui.yaml#components.offline_info_banner`. */
    const val OFFLINE_BANNER: String = "share_out_execute_offline_banner"

    /** `ui.yaml#components.double_confirmation_section`. */
    const val CONFIRMATION_SECTION: String = "share_out_execute_confirmation_section"

    /** `ui.yaml#components.confirmation_text_field`. */
    const val CONFIRMATION_FIELD: String = "share_out_execute_confirmation_field"

    /** `ui.yaml#components.biometric_button`. */
    const val BIOMETRIC_BUTTON: String = "share_out_execute_biometric_button"

    /** `ui.yaml#components.execute_button` (strategy-adaptive label). */
    const val EXECUTE_BUTTON: String = "share_out_execute_execute_button"

    /** `ui.yaml#components.progress_indicator`. */
    const val PROGRESS_INDICATOR: String = "share_out_execute_progress_indicator"

    /** `ui.yaml#components.completion_banner` (Success). */
    const val COMPLETION_BANNER: String = "share_out_execute_completion_banner"

    /** `ui.yaml#components.partial_failure_banner` (PartialFailure). */
    const val PARTIAL_FAILURE_BANNER: String = "share_out_execute_partial_failure_banner"

    /** `ui.yaml#components.queued_offline_banner`. */
    const val QUEUED_BANNER: String = "share_out_execute_queued_banner"

    /** `ui.yaml#components.retry_failed_button`. */
    const val RETRY_FAILED_BUTTON: String = "share_out_execute_retry_failed_button"

    /** `ui.yaml#components.done_button`. */
    const val DONE_BUTTON: String = "share_out_execute_done_button"

    /**
     * Resolves the stable per-row test tag for one member payout row. The payout list renders
     * data-driven from `ShareOutExecuteState.memberPayouts` (not N separate composables), so this is
     * a function rather than a fixed constant set.
     */
    fun memberPayoutRowTag(memberId: String): String = "share_out_execute_member_row_$memberId"
}
