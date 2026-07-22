/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd

/**
 * Append-only test-tag registry for the `member-add` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/member-add/src/commonTest/` and by the
 * Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object MemberAddTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "member_add_screen"

    /** Top app bar (`ui.yaml#components.top_bar`) — close icon dismisses the form. */
    const val TOP_BAR: String = "member_add_top_bar"

    // -- Photo picker ------------------------------------------------------------------------------

    /** `photo_picker_area` — 120dp circular tap target that opens [PHOTO_SOURCE_SHEET]. */
    const val PHOTO_AREA: String = "member_add_photo_area"

    /** `photo_remove_button` — visible only when `MemberAddState.photoUri != null`. */
    const val PHOTO_REMOVE_BUTTON: String = "member_add_photo_remove_button"

    /** `photo_source_bottom_sheet` — camera/gallery chooser opened by [PHOTO_AREA]. */
    const val PHOTO_SOURCE_SHEET: String = "member_add_photo_source_sheet"

    /** `camera_option` list-item inside [PHOTO_SOURCE_SHEET]. */
    const val PHOTO_SOURCE_CAMERA_OPTION: String = "member_add_photo_source_camera_option"

    /** `gallery_option` list-item inside [PHOTO_SOURCE_SHEET]. */
    const val PHOTO_SOURCE_GALLERY_OPTION: String = "member_add_photo_source_gallery_option"

    // -- Form fields -------------------------------------------------------------------------------

    /** `first_name_field`. */
    const val FIELD_FIRST_NAME: String = "member_add_field_first_name"

    /** `last_name_field`. */
    const val FIELD_LAST_NAME: String = "member_add_field_last_name"

    /** `phone_field`. */
    const val FIELD_PHONE: String = "member_add_field_phone"

    /** `role_dropdown`. */
    const val DROPDOWN_ROLE: String = "member_add_dropdown_role"

    // -- Offline / submit / error --------------------------------------------------------------------

    /** `offline_banner` — visible when `MemberAddState.isOffline`. */
    const val OFFLINE_BANNER: String = "member_add_offline_banner"

    /** `save_button` — always labelled "Save Member"; text switches to "Saving…" while submitting. */
    const val SAVE_BUTTON: String = "member_add_save_button"

    /** Inline error alert — `MemberAddScreenState.Error`. */
    const val ERROR_BANNER: String = "member_add_error_banner"

    /** Secondary "Retry" button embedded in [ERROR_BANNER] — also dispatches `OnSubmit`. */
    const val ERROR_RETRY_BUTTON: String = "member_add_error_retry_button"

    /** Centered overlay spinner — `MemberAddScreenState.Submitting`. */
    const val SUBMITTING_INDICATOR: String = "member_add_submitting_indicator"

    /** Success celebration surface — `MemberAddScreenState.Success` (transient, nav fires same frame). */
    const val SUCCESS_SECTION: String = "member_add_success_section"

    /** `MemberAddEvent.ShowOfflineSyncDialog` modal. */
    const val OFFLINE_SYNC_DIALOG: String = "member_add_offline_sync_dialog"

    /** Dismiss/acknowledge button on [OFFLINE_SYNC_DIALOG]. */
    const val OFFLINE_SYNC_DIALOG_CONFIRM: String = "member_add_offline_sync_dialog_confirm"
}
