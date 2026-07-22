/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanmarkdefaulteddialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.Res
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_cancel
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_confirm
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_confirm_cd
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_error_forbidden
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_error_ineligible
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_error_loan_not_found
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_error_offline_no_queue
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_error_server
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_title
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_warning_body
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.generated.resources.screens_loan_mark_defaulted_dialog_warning_icon_cd

/**
 * Container for the `loan-mark-defaulted-dialog` modal (`ui.yaml#archetype: dialog`,
 * `parent_screen: loan-detail`). Same non-routed overlay convention as
 * [org.mifos.groupbanking.feature.loanrepaymentdialog.LoanRepaymentDialog] — it is NOT a
 * `NavGraphBuilder`/`*Route.kt` destination, it is rendered directly from `cmp-navigation`'s
 * `GroupBankingNavHost.kt`, on top of `loan-detail`, when `LoanDetailEvent.ShowDefaultConfirmDialog`
 * fires (see `LoanDetailScreen.kt`'s `onShowDefaultDialog` callback +
 * `GroupBankingNavHost.kt`'s `defaultDialogTarget` local state). [loanId] / [memberName] /
 * [loanAmountKes] are the `ui.yaml#nav_params` values forwarded from that call site, supplied to
 * [LoanMarkDefaultedDialogViewModel] via Koin `parametersOf(loanId, memberName, loanAmountKes)`
 * (matching `LoanMarkDefaultedDialogModule`'s declared parameter order).
 *
 * [LoanMarkDefaultedDialogEvent.ShowError] is intentionally handled as a no-op here — `submitError`
 * is already rendered inline by [LoanMarkDefaultedDialogContent] (`ui.yaml#components.submit_error_text`,
 * `visible_when: "submitError != null"`), and `AlertDialog` carries no `SnackbarHost` slot of its own
 * to raise a toast from — same inline-only convention as `LoanRepaymentDialog`'s identical KDoc note.
 *
 * See API.md#screen.
 */
@Composable
fun LoanMarkDefaultedDialog(
    loanId: Long,
    memberName: String,
    loanAmountKes: Double,
    onDismiss: () -> Unit,
    onMarkedDefaulted: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanMarkDefaultedDialogViewModel = koinViewModel(
        parameters = { parametersOf(loanId, memberName, loanAmountKes) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            LoanMarkDefaultedDialogEvent.Dismiss -> onDismiss()
            is LoanMarkDefaultedDialogEvent.LoanMarkedDefaulted -> {
                onMarkedDefaulted(event.loanId)
                onDismiss()
            }
            is LoanMarkDefaultedDialogEvent.ShowError -> Unit // see class KDoc "ShowError" note — inline-only.
        }
    }

    LoanMarkDefaultedDialogContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `loan-mark-defaulted-dialog` — a single `AlertDialog` mapping
 * `ui.yaml#components.dialog_actions_row.content` 1:1 onto `dismissButton` (Cancel ->
 * [LoanMarkDefaultedDialogAction.OnDismiss]) / `confirmButton` (Mark Defaulted ->
 * [LoanMarkDefaultedDialogAction.OnConfirm]). `onDismissRequest` also dispatches `OnDismiss` — covers
 * both the Cancel tap and "tap outside the dialog" (`ui.yaml#cancel_button.on_click.trigger`: "Tap
 * Cancel or outside dialog"). `state.isSubmitting` / `state.submitError` flatly drive the three
 * `ui.yaml#states` (`idle` / `submitting` / `error`) — there is no separate `ScreenState` enum for
 * this confirm-only mutation dialog, same shape as `LoanMarkDefaultedDialogState`'s own class KDoc.
 * See API.md#screen.
 */
@Composable
fun LoanMarkDefaultedDialogContent(
    state: LoanMarkDefaultedDialogState,
    onAction: (LoanMarkDefaultedDialogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val warningIconCd = stringResource(Res.string.screens_loan_mark_defaulted_dialog_warning_icon_cd)
    val confirmingCd = stringResource(Res.string.screens_loan_mark_defaulted_dialog_confirm_cd)

    AlertDialog(
        onDismissRequest = { onAction(LoanMarkDefaultedDialogAction.OnDismiss) },
        modifier = modifier.testTag(LoanMarkDefaultedDialogTestTags.DIALOG),
        icon = {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = warningIconCd,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(LoanMarkDefaultedDialogTestTags.WARNING_ICON),
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.screens_loan_mark_defaulted_dialog_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            LoanMarkDefaultedDialogBody(state = state)
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(LoanMarkDefaultedDialogAction.OnDismiss) },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanMarkDefaultedDialogTestTags.CANCEL_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_loan_mark_defaulted_dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(LoanMarkDefaultedDialogAction.OnConfirm) },
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanMarkDefaultedDialogTestTags.CONFIRM_BUTTON)
                    .let { base ->
                        if (state.isSubmitting) base.semantics { contentDescription = confirmingCd } else base
                    },
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(Res.string.screens_loan_mark_defaulted_dialog_confirm))
                }
            }
        },
    )
}

/**
 * Irreversible-warning body shared by [LoanMarkDefaultedDialogContent]'s `text` slot —
 * `ui.yaml#components.warning_body_text` (interpolating `state.memberName` / `state.loanAmountKes`)
 * + `submit_error_text` (`visible_when: "submitError != null"`). See API.md#screen.
 */
@Composable
internal fun LoanMarkDefaultedDialogBody(
    state: LoanMarkDefaultedDialogState,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                Res.string.screens_loan_mark_defaulted_dialog_warning_body,
                state.memberName,
                state.loanAmountKes.formatGrouped(0),
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoanMarkDefaultedDialogTestTags.WARNING_BODY_TEXT),
        )

        state.submitError?.let { key ->
            Spacer(modifier = Modifier.height(sp.sm))
            Text(
                text = resolveLoanMarkDefaultedMessage(key),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LoanMarkDefaultedDialogTestTags.SUBMIT_ERROR_TEXT),
            )
        }
    }
}

/**
 * Maps a `LoanMarkDefaultedDialogState.submitError` message-key string (emitted by
 * `LoanMarkDefaultedDialogViewModel.kt`'s `toLoanMarkDefaultedMessageKey` top-level helper /
 * `OFFLINE_MESSAGE_KEY`) to its localized text. `error_offline_no_queue` / `error_loan_not_found` /
 * `error_forbidden` / `error_ineligible` are backfilled composeResources entries — see this module's
 * `strings.xml` note (the ViewModel emits `error_offline_no_queue` / `error_loan_not_found` /
 * `error_server` verbatim; `error_forbidden` / `error_ineligible` are declared by `ui.yaml#i18n.en`
 * but not currently reachable — see the ViewModel's own "403/409 disambiguation gap" KDoc note —
 * backfilled here anyway so the resource exists once the gap is closed upstream).
 */
@Composable
private fun resolveLoanMarkDefaultedMessage(key: String): String = when (key) {
    "error_loan_not_found" -> stringResource(Res.string.screens_loan_mark_defaulted_dialog_error_loan_not_found)
    "error_offline_no_queue" -> stringResource(Res.string.screens_loan_mark_defaulted_dialog_error_offline_no_queue)
    "error_forbidden" -> stringResource(Res.string.screens_loan_mark_defaulted_dialog_error_forbidden)
    "error_ineligible" -> stringResource(Res.string.screens_loan_mark_defaulted_dialog_error_ineligible)
    else -> stringResource(Res.string.screens_loan_mark_defaulted_dialog_error_server)
}
