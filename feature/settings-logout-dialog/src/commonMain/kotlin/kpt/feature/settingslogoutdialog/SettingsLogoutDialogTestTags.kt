/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.settingslogoutdialog

/**
 * Append-only test-tag registry for the `settings-logout-dialog` feature
 * (RULE-KMP-COMPOSE-UITEST-001 CU-5 — names are stable across regenerations; only append new
 * entries, never rename or remove). Consumed by Compose UI tests under
 * `feature/settings-logout-dialog/src/commonTest/` and by the Maestro flow generator
 * (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }` selectors from
 * these constants. Constant names are derived 1:1 from `ui.yaml#components[].id`, mirroring
 * `LoanMarkDefaultedDialogTestTags` / `LoanRepaymentDialogTestTags`'s identical convention.
 *
 * See API.md#tags.
 */
object SettingsLogoutDialogTestTags {

    /** Root [androidx.compose.material3.AlertDialog] surface — always rendered while the dialog is shown. */
    const val DIALOG: String = "settings_logout_dialog"

    /** `ui.yaml#components.dialog_title` — always rendered. */
    const val TITLE: String = "settings_logout_dialog_title"

    /** `ui.yaml#components.logout_body_text` — always rendered. */
    const val BODY_TEXT: String = "settings_logout_dialog_body_text"

    /** `ui.yaml#components.logout_error_text` — visible when `state.logoutError != null`. */
    const val ERROR_TEXT: String = "settings_logout_dialog_error_text"

    /** `ui.yaml#components.dialog_actions_row.children.cancel_button` — [SettingsLogoutDialogAction.OnDismiss]. */
    const val CANCEL_BUTTON: String = "settings_logout_dialog_cancel_button"

    /** `ui.yaml#components.dialog_actions_row.children.logout_button` — [SettingsLogoutDialogAction.OnConfirmLogout]. */
    const val LOGOUT_BUTTON: String = "settings_logout_dialog_logout_button"

    /** In-button [androidx.compose.material3.CircularProgressIndicator] — visible when `state.isLoggingOut`. */
    const val LOGOUT_PROGRESS: String = "settings_logout_dialog_logout_progress"
}
