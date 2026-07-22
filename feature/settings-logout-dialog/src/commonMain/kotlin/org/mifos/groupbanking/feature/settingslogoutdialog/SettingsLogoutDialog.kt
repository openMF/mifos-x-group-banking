/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settingslogoutdialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.Res
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_a11y_cancel
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_a11y_logout
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_body
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_cancel
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_error_logout_failed
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_logout
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_logout_progress_cd
import org.mifos.groupbanking.feature.settingslogoutdialog.generated.resources.screens_settings_logout_dialog_title

/**
 * Container for the `settings-logout-dialog` modal (`ui.yaml#screens[0].type: dialog`,
 * `route: dialog (overlaid on settings)`). Same non-routed overlay convention as
 * [org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialog] /
 * [org.mifos.groupbanking.feature.loanmarkdefaulteddialog.LoanMarkDefaultedDialog] — this is NOT a
 * `NavGraphBuilder`/`*Route.kt` destination, it is rendered directly by the `settings` screen /
 * `cmp-navigation`'s `GroupBankingNavHost.kt` as an overlay on top of `settings` when the user taps
 * the Logout button (`ui.yaml#entry_points[0]`: `trigger: user_taps_logout_button`). Unlike the two
 * loan dialogs this feature carries no nav-args (`SettingsLogoutDialogModule` registers it with a
 * bare `viewModelOf`), so the call-site convention is a nullable `Boolean`/`Unit`-flag local state
 * (`if (showLogoutDialog) { SettingsLogoutDialog(...) }`) rather than a nullable data-class target.
 *
 * See API.md#screen.
 */
@Composable
fun SettingsLogoutDialog(
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsLogoutDialogViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            SettingsLogoutDialogEvent.Dismiss -> onDismiss()
            SettingsLogoutDialogEvent.NavigateToLogin -> onNavigateToLogin()
        }
    }

    SettingsLogoutDialogContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `settings-logout-dialog` — a single `AlertDialog` mapping
 * `ui.yaml#components.dialog_actions_row.children` 1:1 onto `dismissButton` (Cancel ->
 * [SettingsLogoutDialogAction.OnDismiss]) / `confirmButton` (Log Out ->
 * [SettingsLogoutDialogAction.OnConfirmLogout]). `onDismissRequest` also dispatches `OnDismiss` —
 * covers both the Cancel tap and "tap outside the dialog"
 * (`ui.yaml#cancel_button.on_click.action_contract.description`: "no session data is touched").
 * [SettingsLogoutDialogState.deriveScreenState] drives an exhaustive `when` over
 * [SettingsLogoutDialogScreenState] (`Idle` / `LoggingOut` / `Error`) — the state-driven-rendering
 * contract this dialog's ViewModel deliberately exposes (unlike `LoanMarkDefaultedDialogState` /
 * `LoanRepaymentDialogState`, which have no separate screen-state enum). See API.md#screen.
 */
@Composable
fun SettingsLogoutDialogContent(
    state: SettingsLogoutDialogState,
    onAction: (SettingsLogoutDialogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val cancelCd = stringResource(Res.string.screens_settings_logout_dialog_a11y_cancel)
    val logoutCd = stringResource(Res.string.screens_settings_logout_dialog_a11y_logout)
    val loggingOutCd = stringResource(Res.string.screens_settings_logout_dialog_logout_progress_cd)

    val screenState = state.deriveScreenState()
    val isLoggingOut = when (screenState) {
        SettingsLogoutDialogScreenState.LoggingOut -> true
        SettingsLogoutDialogScreenState.Idle, SettingsLogoutDialogScreenState.Error -> false
    }

    AlertDialog(
        onDismissRequest = { onAction(SettingsLogoutDialogAction.OnDismiss) },
        modifier = modifier.testTag(SettingsLogoutDialogTestTags.DIALOG),
        title = {
            Text(
                text = stringResource(Res.string.screens_settings_logout_dialog_title),
                modifier = Modifier.testTag(SettingsLogoutDialogTestTags.TITLE),
            )
        },
        text = {
            SettingsLogoutDialogBody(state = state)
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(SettingsLogoutDialogAction.OnDismiss) },
                enabled = !isLoggingOut,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .semantics { contentDescription = cancelCd }
                    .testTag(SettingsLogoutDialogTestTags.CANCEL_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_settings_logout_dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(SettingsLogoutDialogAction.OnConfirmLogout) },
                enabled = !isLoggingOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(SettingsLogoutDialogTestTags.LOGOUT_BUTTON)
                    .semantics { contentDescription = if (isLoggingOut) loggingOutCd else logoutCd },
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .testTag(SettingsLogoutDialogTestTags.LOGOUT_PROGRESS),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(Res.string.screens_settings_logout_dialog_logout))
                }
            }
        },
    )
}

/**
 * Body-text + inline-error body shared by [SettingsLogoutDialogContent]'s `text` slot —
 * `ui.yaml#components.logout_body_text` + `logout_error_text` (`visible_when:
 * "logoutError != null"`). [SettingsLogoutDialogState.logoutError] stores the raw message-KEY
 * (`SettingsLogoutDialogViewModel`'s `LOGOUT_FAILED_MESSAGE_KEY = "error_logout_failed"`), resolved
 * to display text via [resolveSettingsLogoutDialogMessage] — mirrors
 * `LoanMarkDefaultedDialogBody` / `LoanRepaymentDialogFields`'s identical key->resource resolution
 * convention. [SettingsLogoutDialogState.unsyncedCount] is intentionally NOT interpolated into the
 * body here — `ui.yaml#components.logout_body_text.value` is a static string with no
 * interpolation placeholder and no backing `i18n` key exists for a count-aware variant (see the
 * ViewModel's own [unsyncedCount] KDoc "ADDED field" note) — surfacing it would require fabricating
 * an undeclared string resource. See API.md#screen.
 */
@Composable
internal fun SettingsLogoutDialogBody(
    state: SettingsLogoutDialogState,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.screens_settings_logout_dialog_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SettingsLogoutDialogTestTags.BODY_TEXT),
        )

        state.logoutError?.let { key ->
            Spacer(modifier = Modifier.height(sp.sm))
            Text(
                text = resolveSettingsLogoutDialogMessage(key),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsLogoutDialogTestTags.ERROR_TEXT),
            )
        }
    }
}

/**
 * Maps a `SettingsLogoutDialogState.logoutError` message-key string (emitted by
 * `SettingsLogoutDialogViewModel`'s `LOGOUT_FAILED_MESSAGE_KEY`) to its localized text. Only one
 * error key is currently emitted (`error_logout_failed`, `ui.yaml#state_model.errors.LogoutFailed`)
 * — the `when` still carries an explicit `else` branch resolving to the same resource, mirroring
 * `LoanMarkDefaultedDialogBody` / `LoanRepaymentDialogFields`'s defensive fallback convention so a
 * future additional error key never crashes into an unresolved string.
 */
@Composable
private fun resolveSettingsLogoutDialogMessage(key: String): String = when (key) {
    "error_logout_failed" -> stringResource(Res.string.screens_settings_logout_dialog_error_logout_failed)
    else -> stringResource(Res.string.screens_settings_logout_dialog_error_logout_failed)
}
