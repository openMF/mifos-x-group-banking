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

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

// -- ui.yaml#states.*.demo_data fixtures ---------------------------------------------------------

/** `ui.yaml#states.idle` — awaiting user confirmation, no unsynced-op warning surfaced (no key). */
private val demoIdleState = SettingsLogoutDialogState(
    isLoggingOut = false,
    logoutError = null,
    unsyncedCount = 0,
)

/** `ui.yaml#states.logging_out.description` — Log Out button shows a loading indicator; both buttons disabled. */
private val demoLoggingOutState = demoIdleState.copy(isLoggingOut = true)

/** `ui.yaml#states.error.description` — session-clear failure rendered inline below the body text. */
private val demoErrorState = demoIdleState.copy(logoutError = "error_logout_failed")

/**
 * `@Preview` gallery for `SettingsLogoutDialog.kt`. See API.md#preview. Data source:
 * `ui.yaml#states.*` (no `demo-data.yaml` fixture file exists for this feature — flat
 * `SettingsLogoutDialogState`, same class as `LoanMarkDefaultedDialogPreview.kt`'s convention).
 */
private class SettingsLogoutDialogPreviewProvider : PreviewParameterProvider<SettingsLogoutDialogState> {
    override val values: Sequence<SettingsLogoutDialogState> = sequenceOf(
        demoIdleState,
        demoLoggingOutState,
        demoErrorState,
    )
}

@Preview
@Composable
private fun SettingsLogoutDialogContentPreview(
    @PreviewParameter(SettingsLogoutDialogPreviewProvider::class)
    state: SettingsLogoutDialogState,
) {
    KptTheme {
        SettingsLogoutDialogContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun SettingsLogoutDialogBodyPreview() {
    KptTheme {
        SettingsLogoutDialogBody(state = demoErrorState)
    }
}
