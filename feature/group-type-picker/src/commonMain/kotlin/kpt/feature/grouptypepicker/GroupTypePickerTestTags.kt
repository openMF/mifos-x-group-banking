/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouptypepicker

import kpt.core.model.GroupTypeSlug

/**
 * Append-only test-tag registry for the `group-type-picker` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/group-type-picker/src/commonTest/` and by
 * the Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 *
 * The 9 `TYPE_CARD_*` constants mirror `ui.yaml#components` ids (`type_card_vsla`,
 * `type_card_rosca`, ...) verbatim. Cards render data-driven from
 * `GroupTypePickerState.typeConfigs` (not as 9 separate composables), so [cardTag] resolves the
 * correct stable constant for a given [GroupTypeSlug] at render time.
 */
object GroupTypePickerTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "group_type_picker_screen"

    /** Circular loading indicator — `GroupTypePickerScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "group_type_picker_loading_indicator"

    /** Inline error banner — `GroupTypePickerScreenState.Error`. Mirrors `ui.yaml#components.error_banner`. */
    const val ERROR_BANNER: String = "group_type_picker_error_banner"

    /** Retry CTA on the error state. */
    const val RETRY_BUTTON: String = "group_type_picker_retry_button"

    /** Scrollable list surface wrapping the 9 type cards — `GroupTypePickerScreenState.Content`. */
    const val TYPE_LIST: String = "group_type_picker_type_list"

    /** `ui.yaml#components.type_card_vsla` — VSLA (Village Savings & Loan Association). */
    const val TYPE_CARD_VSLA: String = "group_type_picker_card_vsla"

    /** `ui.yaml#components.type_card_rosca` — ROSCA (Rotating Savings & Credit Association). */
    const val TYPE_CARD_ROSCA: String = "group_type_picker_card_rosca"

    /** `ui.yaml#components.type_card_asca` — ASCA (Accumulating Savings & Credit Association). */
    const val TYPE_CARD_ASCA: String = "group_type_picker_card_asca"

    /** `ui.yaml#components.type_card_silc` — SILC (Savings & Internal Lending Community). */
    const val TYPE_CARD_SILC: String = "group_type_picker_card_silc"

    /** `ui.yaml#components.type_card_shg` — SHG (Self-Help Group). */
    const val TYPE_CARD_SHG: String = "group_type_picker_card_shg"

    /** `ui.yaml#components.type_card_sacco` — SACCO / Credit Union. */
    const val TYPE_CARD_SACCO: String = "group_type_picker_card_sacco"

    /** `ui.yaml#components.type_card_cbo` — CBO / Village Bank (FINCA model). */
    const val TYPE_CARD_CBO: String = "group_type_picker_card_cbo"

    /** `ui.yaml#components.type_card_burial` — Burial / Welfare Society. */
    const val TYPE_CARD_BURIAL: String = "group_type_picker_card_burial"

    /** `ui.yaml#components.type_card_jlg` — JLG (Joint Liability Group). */
    const val TYPE_CARD_JLG: String = "group_type_picker_card_jlg"

    /** Defensive fallback for a [GroupTypeSlug.UNKNOWN] row — never expected in the happy path. */
    const val TYPE_CARD_UNKNOWN: String = "group_type_picker_card_unknown"

    /** Resolves the stable card test tag for a given [GroupTypeSlug]. See class KDoc. */
    fun cardTag(slug: GroupTypeSlug): String = when (slug) {
        GroupTypeSlug.VSLA -> TYPE_CARD_VSLA
        GroupTypeSlug.ROSCA -> TYPE_CARD_ROSCA
        GroupTypeSlug.ASCA -> TYPE_CARD_ASCA
        GroupTypeSlug.SILC -> TYPE_CARD_SILC
        GroupTypeSlug.SHG -> TYPE_CARD_SHG
        GroupTypeSlug.SACCO -> TYPE_CARD_SACCO
        GroupTypeSlug.CBO_VILLAGE_BANK -> TYPE_CARD_CBO
        GroupTypeSlug.BURIAL_WELFARE -> TYPE_CARD_BURIAL
        GroupTypeSlug.JLG -> TYPE_CARD_JLG
        GroupTypeSlug.UNKNOWN -> TYPE_CARD_UNKNOWN
    }
}
