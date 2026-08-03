/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.shareoutpreview

/**
 * Append-only test-tag registry for the `share-out-preview` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/share-out-preview/src/commonTest/` and by the Maestro
 * flow generator. Constant names are derived 1:1 from `ui.yaml#components[].id`. See API.md#tags.
 */
object ShareOutPreviewTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "share_out_preview_screen"

    /** `ui.yaml#components.top_bar` refresh action. */
    const val REFRESH_ACTION: String = "share_out_preview_refresh_action"

    /** `ui.yaml#components.shimmer_detail` — `ShareOutPreviewScreenState.Loading`. */
    const val LOADING_SECTION: String = "share_out_preview_loading_section"

    /** `ui.yaml#components.cycle_info_banner`. */
    const val CYCLE_INFO_BANNER: String = "share_out_preview_cycle_info_banner"

    /** `ui.yaml#components.fund_summary_card`. */
    const val FUND_SUMMARY_CARD: String = "share_out_preview_fund_summary_card"

    /** `ui.yaml#components.distribution_formula_chip`. */
    const val FORMULA_CHIP: String = "share_out_preview_formula_chip"

    /** `ui.yaml#components.member_payout_table` — ACCUMULATING path. */
    const val MEMBER_PAYOUT_TABLE: String = "share_out_preview_member_payout_table"

    /** `ui.yaml#components.rotation_preview_card` — ROTATING_PAYOUT path. */
    const val ROTATION_PREVIEW_CARD: String = "share_out_preview_rotation_preview_card"

    /** `ui.yaml#components.confirm_button`. */
    const val CONFIRM_BUTTON: String = "share_out_preview_confirm_button"

    /** `ui.yaml#components.error_state` full-screen surface. */
    const val ERROR_SECTION: String = "share_out_preview_error_section"

    /** Retry CTA on the full-screen error state — dispatches `ShareOutPreviewAction.Retry`. */
    const val ERROR_RETRY_BUTTON: String = "share_out_preview_error_retry_button"

    /**
     * Resolves the stable per-row test tag for one member payout row.
     * `ui.yaml#components.member_payout_table` renders data-driven from
     * `ShareOutPreviewState.memberPayouts` (not N separate composables), so this is a function
     * rather than a fixed constant set.
     */
    fun memberPayoutRowTag(memberId: String): String = "share_out_preview_member_row_$memberId"
}
