/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.feature.settings.components.SettingsChangePinDialog
import org.mifos.groupbanking.feature.settings.components.SettingsLanguageSection
import org.mifos.groupbanking.feature.settings.components.SettingsLogoutButton
import org.mifos.groupbanking.feature.settings.components.SettingsNavRow
import org.mifos.groupbanking.feature.settings.components.SettingsSectionHeader
import org.mifos.groupbanking.feature.settings.components.SettingsThemeSection
import org.mifos.groupbanking.feature.settings.components.SettingsToggleRow
import org.mifos.groupbanking.feature.settings.components.SettingsVersionRow
import org.mifos.groupbanking.feature.settings.generated.resources.Res
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_biometric_row
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_biometric_switch
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_change_pin_dialog
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_change_pin_row
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_chevron_right
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_current_pin_field
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_dialog_cancel
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_dialog_confirm
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_language_group
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_logout_button
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_logout_progress_cd
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_new_pin_field
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_notifications_row
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_notifications_switch
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_theme_group
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_a11y_top_bar
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_biometric_label
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_biometric_sublabel
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_biometric_unavailable
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_change_pin_label
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_change_pin_sublabel
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_current_pin
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_dialog_cancel
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_dialog_confirm
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_dialog_title
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_error_pin_change
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_logout_button
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_new_pin
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_notifications_label
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_notifications_sublabel
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_section_about
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_section_appearance
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_section_language
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_section_notifications
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_section_security
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_title
import org.mifos.groupbanking.feature.settings.generated.resources.screens_settings_version_label

/**
 * Container for `settings-screen` (`ui.yaml#route`: `/settings`). Collects
 * [SettingsViewModel] state via [collectAsStateWithLifecycle], consumes one-shot [SettingsEvent]s
 * (present the logout-confirmation overlay, navigate to login, snackbar) through [EventsEffect],
 * and delegates all rendering to the stateless [SettingsContent]. See API.md#screen.
 */
@Composable
internal fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onShowLogoutDialog: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    EventsEffect(viewModel) { event ->
        when (event) {
            SettingsEvent.ShowLogoutDialog -> onShowLogoutDialog()
            SettingsEvent.NavigateToLogin -> onNavigateToLogin()
            // See `SettingsViewModel` class KDoc "No success snackbar copy" -- the ViewModel never
            // actually emits this event today (no declared i18n success key), but the branch stays
            // wired for real (not a no-op) so a future emission renders immediately.
            is SettingsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(message = event.message)
        }
    }

    SettingsContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `settings-screen`. State-driven per `ui.yaml#state_model
 * .screen_state` -- every [SettingsScreenState] member is handled: `Content` renders every
 * section; `ChangingPin` renders the same sections PLUS the [SettingsChangePinDialog] overlay
 * (`ui.yaml#states.changing_pin`); `LoggingOut` renders only the top bar + a disabled, spinning
 * logout button (`ui.yaml#states.logging_out.components: [top_bar, logout_button]`). See
 * API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val screenState = state.deriveScreenState()
    val title = stringResource(Res.string.screens_settings_title)
    val topBarCd = stringResource(Res.string.screens_settings_a11y_top_bar)

    KptScaffold(
        modifier = modifier.testTag(SettingsTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationIonClick = onNavigateBack,
                    testTag = SettingsTestTags.TOP_BAR,
                    contentDescription = topBarCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        when (screenState) {
            SettingsScreenState.Content -> SettingsMainSection(state = state, onAction = onAction)

            SettingsScreenState.ChangingPin -> {
                SettingsMainSection(state = state, onAction = onAction)
                SettingsChangePinDialogHost(state = state, onAction = onAction)
            }

            SettingsScreenState.LoggingOut -> SettingsLoggingOutSection(state = state, onAction = onAction)
        }
    }
}

/**
 * `SettingsScreenState.Content` / shared body of `.ChangingPin` -- Language, Appearance, Security,
 * Notifications, About sections + the Logout button, in `ui.yaml#states.content.components` order.
 * See API.md#screen.
 */
@Composable
internal fun SettingsMainSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(SettingsTestTags.CONTENT),
    ) {
        SettingsSectionHeader(text = stringResource(Res.string.screens_settings_section_language))
        SettingsLanguageSection(
            selected = state.selectedLanguage,
            onSelected = { language -> onAction(SettingsAction.OnLanguageSelected(language)) },
            groupContentDescription = stringResource(Res.string.screens_settings_a11y_language_group),
            testTagFor = SettingsTestTags::languageOptionTag,
            modifier = Modifier.testTag(SettingsTestTags.LANGUAGE_RADIO_GROUP),
        )

        SettingsSectionHeader(text = stringResource(Res.string.screens_settings_section_appearance))
        SettingsThemeSection(
            selected = state.selectedTheme,
            onSelected = { theme -> onAction(SettingsAction.OnThemeSelected(theme)) },
            groupContentDescription = stringResource(Res.string.screens_settings_a11y_theme_group),
            testTagFor = SettingsTestTags::themeOptionTag,
            modifier = Modifier.testTag(SettingsTestTags.THEME_RADIO_GROUP),
        )

        SettingsSectionHeader(text = stringResource(Res.string.screens_settings_section_security))
        SettingsToggleRow(
            headline = stringResource(Res.string.screens_settings_biometric_label),
            supporting = if (state.isBiometricAvailable) {
                stringResource(Res.string.screens_settings_biometric_sublabel)
            } else {
                stringResource(Res.string.screens_settings_biometric_unavailable)
            },
            checked = state.isBiometricEnabled,
            enabled = state.isBiometricAvailable,
            onCheckedChange = { enabled -> onAction(SettingsAction.OnBiometricToggled(enabled)) },
            rowContentDescription = stringResource(Res.string.screens_settings_a11y_biometric_row),
            switchContentDescription = stringResource(Res.string.screens_settings_a11y_biometric_switch),
            switchTestTag = SettingsTestTags.BIOMETRIC_SWITCH,
        )
        SettingsNavRow(
            headline = stringResource(Res.string.screens_settings_change_pin_label),
            supporting = stringResource(Res.string.screens_settings_change_pin_sublabel),
            onClick = { onAction(SettingsAction.OnChangePinTapped) },
            contentDescription = stringResource(Res.string.screens_settings_a11y_change_pin_row),
            chevronContentDescription = stringResource(Res.string.screens_settings_a11y_chevron_right),
            modifier = Modifier.testTag(SettingsTestTags.CHANGE_PIN_ROW),
        )

        SettingsSectionHeader(text = stringResource(Res.string.screens_settings_section_notifications))
        SettingsToggleRow(
            headline = stringResource(Res.string.screens_settings_notifications_label),
            supporting = stringResource(Res.string.screens_settings_notifications_sublabel),
            checked = state.isNotificationsEnabled,
            onCheckedChange = { enabled -> onAction(SettingsAction.OnNotificationsToggled(enabled)) },
            rowContentDescription = stringResource(Res.string.screens_settings_a11y_notifications_row),
            switchContentDescription = stringResource(Res.string.screens_settings_a11y_notifications_switch),
            switchTestTag = SettingsTestTags.NOTIFICATIONS_SWITCH,
        )

        SettingsSectionHeader(text = stringResource(Res.string.screens_settings_section_about))
        SettingsVersionRow(
            headline = stringResource(Res.string.screens_settings_version_label),
            versionText = "${state.appVersion} (${state.buildNumber})",
            modifier = Modifier.testTag(SettingsTestTags.APP_VERSION_ROW),
        )

        SettingsLogoutButton(
            label = stringResource(Res.string.screens_settings_logout_button),
            contentDescription = stringResource(Res.string.screens_settings_a11y_logout_button),
            loggingOutContentDescription = stringResource(Res.string.screens_settings_a11y_logout_progress_cd),
            isLoggingOut = state.isLoggingOut,
            onClick = { onAction(SettingsAction.OnLogoutTapped) },
            testTag = SettingsTestTags.LOGOUT_BUTTON,
            modifier = Modifier.padding(sp.lg),
        )
    }
}

/**
 * `SettingsScreenState.LoggingOut` -- `ui.yaml#states.logging_out.components: [top_bar,
 * logout_button]` -- only the (disabled, spinning) logout button is rendered, centered. See
 * API.md#screen.
 */
@Composable
internal fun SettingsLoggingOutSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SettingsLogoutButton(
            label = stringResource(Res.string.screens_settings_logout_button),
            contentDescription = stringResource(Res.string.screens_settings_a11y_logout_button),
            loggingOutContentDescription = stringResource(Res.string.screens_settings_a11y_logout_progress_cd),
            isLoggingOut = state.isLoggingOut,
            onClick = { onAction(SettingsAction.OnLogoutTapped) },
            testTag = SettingsTestTags.LOGOUT_BUTTON,
            modifier = Modifier.padding(sp.lg),
        )
    }
}

/**
 * `ui.yaml#components.change_pin_dialog` overlay -- resolves [SettingsState.pinChangeError]'s raw
 * message-KEY (`SettingsViewModel.PIN_CHANGE_ERROR_MESSAGE_KEY`) to its localized text via
 * [resolveSettingsMessage] (mirrors `SettingsLogoutDialogBody`'s identical key->resource
 * convention in the sibling `settings-logout-dialog` feature). See API.md#screen.
 */
@Composable
internal fun SettingsChangePinDialogHost(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsChangePinDialog(
        title = stringResource(Res.string.screens_settings_dialog_title),
        currentPinLabel = stringResource(Res.string.screens_settings_current_pin),
        newPinLabel = stringResource(Res.string.screens_settings_new_pin),
        cancelLabel = stringResource(Res.string.screens_settings_dialog_cancel),
        confirmLabel = stringResource(Res.string.screens_settings_dialog_confirm),
        error = state.pinChangeError?.let { key -> resolveSettingsMessage(key) },
        onDismiss = { onAction(SettingsAction.OnDismissPinDialog) },
        onConfirm = { currentPin, newPin -> onAction(SettingsAction.OnSubmitPinChange(currentPin, newPin)) },
        dialogContentDescription = stringResource(Res.string.screens_settings_a11y_change_pin_dialog),
        currentPinFieldContentDescription = stringResource(Res.string.screens_settings_a11y_current_pin_field),
        newPinFieldContentDescription = stringResource(Res.string.screens_settings_a11y_new_pin_field),
        cancelContentDescription = stringResource(Res.string.screens_settings_a11y_dialog_cancel),
        confirmContentDescription = stringResource(Res.string.screens_settings_a11y_dialog_confirm),
        dialogTestTag = SettingsTestTags.CHANGE_PIN_DIALOG,
        currentPinFieldTestTag = SettingsTestTags.CURRENT_PIN_FIELD,
        newPinFieldTestTag = SettingsTestTags.NEW_PIN_FIELD,
        errorTextTestTag = SettingsTestTags.PIN_ERROR_TEXT,
        cancelButtonTestTag = SettingsTestTags.DIALOG_CANCEL_BUTTON,
        confirmButtonTestTag = SettingsTestTags.DIALOG_CONFIRM_BUTTON,
        modifier = modifier,
    )
}

/**
 * Maps a [SettingsState.pinChangeError] message-key string (emitted by
 * `SettingsViewModel.PIN_CHANGE_ERROR_MESSAGE_KEY`) to its localized text. Only one error key is
 * currently emitted (`error_pin_change`, `ui.yaml#state_model.errors.PinChangeFailed`) -- the
 * `when` still carries an explicit `else` branch resolving to the same resource, mirroring
 * `LoanRepaymentDialogFields` / `SettingsLogoutDialogBody`'s identical defensive-fallback
 * convention so a future additional error key never crashes into an unresolved string.
 */
@Composable
private fun resolveSettingsMessage(key: String): String = when (key) {
    "error_pin_change" -> stringResource(Res.string.screens_settings_error_pin_change)
    else -> stringResource(Res.string.screens_settings_error_pin_change)
}
