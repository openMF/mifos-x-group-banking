/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanrepaymentdialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.model.PaymentMethod
import kpt.feature.loanrepaymentdialog.generated.resources.Res
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_amount_label
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_amount_placeholder
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_cancel
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_cash_label
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_amount_exceeds
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_amount_invalid
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_amount_required
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_auth
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_insufficient_role
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_loan_not_found
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_offline_no_queue
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_error_server
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_mpesa_label
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_payment_method_label
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_reference_label
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_reference_placeholder
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_submit
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_submit_cd
import kpt.feature.loanrepaymentdialog.generated.resources.screens_loan_repayment_dialog_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Container for the `loan-repayment-dialog` modal (`ui.yaml#archetype: dialog`,
 * `parent_screen: loan-detail`). Unlike every other single-screen feature in this codebase this is
 * NOT a routed `NavGraphBuilder`/`*Route.kt` destination — it is rendered as an overlay directly
 * from `cmp-navigation`'s `GroupBankingNavHost.kt`, on top of `loan-detail`, when
 * `LoanDetailEvent.ShowRepaymentDialog` fires (see `LoanDetailScreen.kt`'s
 * `onShowRepaymentDialog` callback + `GroupBankingNavHost.kt`'s `repaymentDialogTarget` local
 * state). [loanId] / [memberId] / [installmentAmount] are the `ui.yaml#nav_params` values
 * forwarded from that call site, supplied to [LoanRepaymentDialogViewModel] via Koin
 * `parametersOf(loanId, memberId, installmentAmount)` (matching `LoanRepaymentDialogModule`'s
 * declared parameter order).
 *
 * [LoanRepaymentDialogEvent.ShowError] is intentionally handled as a no-op here — `submitError` is
 * already rendered inline by [LoanRepaymentDialogContent] (`ui.yaml#components.submit_error_text`,
 * `visible_when: "submitError != null"`), and `AlertDialog` carries no `SnackbarHost` slot of its
 * own to raise a toast from, unlike a full-screen `KptScaffold`. Inline is the chosen channel for
 * this dialog presentation (see caller's brief "ShowError→snackbar/inline").
 *
 * See API.md#screen.
 */
@Composable
fun LoanRepaymentDialog(
    loanId: Long,
    memberId: Long,
    installmentAmount: Double,
    onDismiss: () -> Unit,
    onRepaymentRecorded: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanRepaymentDialogViewModel = koinViewModel(
        parameters = { parametersOf(loanId, memberId, installmentAmount) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            LoanRepaymentDialogEvent.Dismiss -> onDismiss()
            is LoanRepaymentDialogEvent.RepaymentRecorded -> {
                onRepaymentRecorded(event.loanId)
                onDismiss()
            }
            is LoanRepaymentDialogEvent.ShowError -> Unit // see class KDoc "ShowError" note — inline-only.
        }
    }

    LoanRepaymentDialogContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `loan-repayment-dialog` — a single `AlertDialog` mapping
 * `ui.yaml#components.dialog_actions_row.content` 1:1 onto `dismissButton` (Cancel ->
 * [LoanRepaymentDialogAction.OnDismiss]) / `confirmButton` (Record Repayment ->
 * [LoanRepaymentDialogAction.OnSubmit]). `onDismissRequest` also dispatches `OnDismiss` — covers
 * both the Cancel tap and "tap outside the dialog" (`ui.yaml#cancel_button.on_click.trigger`: "Tap
 * Cancel or outside dialog"). `state.isSubmitting` / `state.amountError` / `state.submitError`
 * flatly drive the three `ui.yaml#states` (`idle` / `submitting` / `error`) — there is no separate
 * `ScreenState` enum for this mutation dialog (mirrors `LoanRepaymentDialogState`'s own class KDoc).
 * See API.md#screen.
 */
@Composable
fun LoanRepaymentDialogContent(
    state: LoanRepaymentDialogState,
    onAction: (LoanRepaymentDialogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val submittingCd = stringResource(Res.string.screens_loan_repayment_dialog_submit_cd)

    AlertDialog(
        onDismissRequest = { onAction(LoanRepaymentDialogAction.OnDismiss) },
        modifier = modifier.testTag(LoanRepaymentDialogTestTags.DIALOG),
        title = { Text(text = stringResource(Res.string.screens_loan_repayment_dialog_title)) },
        text = {
            LoanRepaymentDialogFields(state = state, onAction = onAction)
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(LoanRepaymentDialogAction.OnDismiss) },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanRepaymentDialogTestTags.CANCEL_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_loan_repayment_dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(LoanRepaymentDialogAction.OnSubmit) },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanRepaymentDialogTestTags.SUBMIT_BUTTON)
                    .let { base ->
                        if (state.isSubmitting) base.semantics { contentDescription = submittingCd } else base
                    },
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(Res.string.screens_loan_repayment_dialog_submit))
                }
            }
        },
    )
}

/**
 * Amount / payment-method / reference-number form body shared by [LoanRepaymentDialogContent]'s
 * `text` slot — `ui.yaml#components.amount_field` + `payment_method_chips` +
 * `reference_number_field` + `submit_error_text`. See API.md#screen.
 */
@Composable
internal fun LoanRepaymentDialogFields(
    state: LoanRepaymentDialogState,
    onAction: (LoanRepaymentDialogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val amountLabel = stringResource(Res.string.screens_loan_repayment_dialog_amount_label)
    val amountPlaceholder = stringResource(Res.string.screens_loan_repayment_dialog_amount_placeholder)
    val paymentMethodLabel = stringResource(Res.string.screens_loan_repayment_dialog_payment_method_label)
    val mpesaLabel = stringResource(Res.string.screens_loan_repayment_dialog_mpesa_label)
    val cashLabel = stringResource(Res.string.screens_loan_repayment_dialog_cash_label)
    val referenceLabel = stringResource(Res.string.screens_loan_repayment_dialog_reference_label)
    val referencePlaceholder = stringResource(Res.string.screens_loan_repayment_dialog_reference_placeholder)

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.amount,
            onValueChange = { onAction(LoanRepaymentDialogAction.OnAmountChanged(it)) },
            label = { Text(text = amountLabel) },
            placeholder = { Text(text = amountPlaceholder) },
            singleLine = true,
            enabled = !state.isSubmitting,
            isError = state.amountError != null,
            supportingText = state.amountError?.let { key ->
                { Text(text = resolveLoanRepaymentMessage(key), color = MaterialTheme.colorScheme.error) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = amountLabel }
                .testTag(LoanRepaymentDialogTestTags.AMOUNT_FIELD),
        )
        Spacer(sp.md)

        Text(
            text = paymentMethodLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(sp.xs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = paymentMethodLabel }
                .testTag(LoanRepaymentDialogTestTags.PAYMENT_METHOD_CHIPS),
            horizontalArrangement = Arrangement.spacedBy(sp.xs),
        ) {
            FilterChip(
                selected = state.paymentMethod == PaymentMethod.MPESA,
                onClick = { onAction(LoanRepaymentDialogAction.OnPaymentMethodSelected(PaymentMethod.MPESA)) },
                label = { Text(text = mpesaLabel) },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanRepaymentDialogTestTags.CHIP_MPESA),
            )
            FilterChip(
                selected = state.paymentMethod == PaymentMethod.CASH,
                onClick = { onAction(LoanRepaymentDialogAction.OnPaymentMethodSelected(PaymentMethod.CASH)) },
                label = { Text(text = cashLabel) },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanRepaymentDialogTestTags.CHIP_CASH),
            )
        }
        Spacer(sp.md)

        OutlinedTextField(
            value = state.referenceNumber,
            onValueChange = { onAction(LoanRepaymentDialogAction.OnReferenceNumberChanged(it)) },
            label = { Text(text = referenceLabel) },
            placeholder = { Text(text = referencePlaceholder) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = referenceLabel }
                .testTag(LoanRepaymentDialogTestTags.REFERENCE_FIELD),
        )

        state.submitError?.let { key ->
            Spacer(sp.sm)
            Text(
                text = resolveLoanRepaymentMessage(key),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(LoanRepaymentDialogTestTags.SUBMIT_ERROR_TEXT),
            )
        }
    }
}

/**
 * Maps a `LoanRepaymentDialogState.amountError` / `.submitError` message-key string (emitted by
 * `LoanRepaymentDialogViewModel.kt`'s `validateAmount` / `toLoanRepaymentMessageKey` top-level
 * helpers) to its localized text. `error_offline_no_queue` / `error_insufficient_role` /
 * `error_loan_not_found` / `error_auth` are backfilled composeResources entries — see this
 * module's `strings.xml` note (the ViewModel emits them verbatim but `ui.yaml#i18n.en` never
 * declared them).
 */
@Composable
private fun resolveLoanRepaymentMessage(key: String): String = when (key) {
    "error_amount_required" -> stringResource(Res.string.screens_loan_repayment_dialog_error_amount_required)
    "error_amount_invalid" -> stringResource(Res.string.screens_loan_repayment_dialog_error_amount_invalid)
    "error_amount_exceeds" -> stringResource(Res.string.screens_loan_repayment_dialog_error_amount_exceeds)
    "error_loan_not_found" -> stringResource(Res.string.screens_loan_repayment_dialog_error_loan_not_found)
    "error_auth" -> stringResource(Res.string.screens_loan_repayment_dialog_error_auth)
    "error_offline_no_queue" -> stringResource(Res.string.screens_loan_repayment_dialog_error_offline_no_queue)
    "error_insufficient_role" -> stringResource(Res.string.screens_loan_repayment_dialog_error_insufficient_role)
    else -> stringResource(Res.string.screens_loan_repayment_dialog_error_server)
}

/** Vertical-gap shorthand — mirrors `MemberAddScreen.kt`'s identical convention. */
@Composable
private fun Spacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}
