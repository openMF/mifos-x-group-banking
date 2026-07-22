/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestAmountField
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestDropdownField
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestDurationSelector
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestErrorCard
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestOfflineBanner
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestRepaymentSummaryCard
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestSavingsLimitCard
import org.mifos.groupbanking.feature.loanrequest.components.LoanRequestSuccessDialog
import org.mifos.groupbanking.feature.loanrequest.generated.resources.Res
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_amount_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_amount_icon_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_amount_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_amount_supporting_text
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_back_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_currency_prefix
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_duration_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_duration_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_duration_weeks_value
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_error_icon_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_error_network_queued
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_error_server
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_error_session_expired
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_error_validation
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_interest_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_kes_amount_format
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_loan_multiplier_hint
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_max_loan_icon_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_max_loan_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_ok_button
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_ok_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_offline_banner
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_offline_banner_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_principal_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_business
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_emergency
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_farming
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_home_improvement
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_medical
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_other
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_purpose_school_fees
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_retry_button
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_retry_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_savings_balance_label
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_savings_icon_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_submit_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_submitting_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_success_dialog_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_success_icon_cd
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_success_offline_body
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_success_offline_title
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_success_online_body
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_success_online_title
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_submit_button
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_topbar_title
import org.mifos.groupbanking.feature.loanrequest.generated.resources.screens_loan_request_total_repayment_label

/**
 * Container for `loan-request-screen` (`ui.yaml#route`: `/loan-request`). Collects
 * [LoanRequestViewModel] state via [collectAsStateWithLifecycle], forwards the required
 * [clientId]/[savingsBalance]/[loanMultiplier] nav-args to Koin via
 * `parametersOf(clientId, savingsBalance, loanMultiplier)` (mirrors `LoanRequestModule`'s
 * `viewModel { parameters -> ... }` builder KDoc), consumes one-shot [LoanRequestEvent]s through
 * [EventsEffect], and delegates all rendering to the stateless [LoanRequestContent]. The top-bar
 * back icon is wired straight to [onNavigateBack] rather than an action dispatch --
 * `ui.yaml#state_model.actions.members` declares no `OnBack`/`NavigateBack` action member (see
 * [LoanRequestEvent.NavigateBack] KDoc's documented reachability gap), so the nav-host callback is
 * invoked directly from the top app bar. See API.md#screen.
 */
@Composable
internal fun LoanRequestScreen(
    clientId: Long,
    savingsBalance: Double,
    loanMultiplier: Double,
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanRequestViewModel = koinViewModel(
        parameters = { parametersOf(clientId, savingsBalance, loanMultiplier) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            LoanRequestEvent.NavigateToDashboardAfterSuccess -> onNavigateToDashboard()
            LoanRequestEvent.NavigateBack -> onNavigateBack()
            // The success dialog already reflects the offline-queued outcome via
            // `successDialogVisible` + `isOfflineMode` -- no additional snackbar is required.
            LoanRequestEvent.ShowOfflineQueuedConfirmation -> Unit
        }
    }

    LoanRequestContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `loan-request-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` -- every [LoanRequestScreenState] member is handled. The
 * scrollable form body ([LoanRequestFormFields]) is shared across every state (only the trailing
 * submit affordance + overlays differ), mirroring `LoanApplyContent`'s identical
 * Content/Submitting factoring. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoanRequestContent(
    state: LoanRequestState,
    onAction: (LoanRequestAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenState = state.deriveScreenState()
    val title = stringResource(Res.string.screens_loan_request_topbar_title)
    val backCd = stringResource(Res.string.screens_loan_request_back_cd)

    KptScaffold(
        modifier = modifier.testTag(LoanRequestTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationIonClick = onNavigateBack,
                    testTag = LoanRequestTestTags.TOP_BAR,
                    contentDescription = backCd,
                ),
            )
        },
    ) {
        LoanRequestFormFields(state = state, onAction = onAction, screenState = screenState)
    }

    if (state.successDialogVisible) {
        val isOffline = screenState == LoanRequestScreenState.OfflineQueued
        val successDialogCd = stringResource(Res.string.screens_loan_request_success_dialog_cd)
        LoanRequestSuccessDialog(
            title = if (isOffline) {
                stringResource(Res.string.screens_loan_request_success_offline_title)
            } else {
                stringResource(Res.string.screens_loan_request_success_online_title)
            },
            body = if (isOffline) {
                stringResource(Res.string.screens_loan_request_success_offline_body)
            } else {
                stringResource(Res.string.screens_loan_request_success_online_body)
            },
            iconContentDescription = stringResource(Res.string.screens_loan_request_success_icon_cd),
            confirmLabel = stringResource(Res.string.screens_loan_request_ok_button),
            confirmContentDescription = stringResource(Res.string.screens_loan_request_ok_cd),
            onConfirm = { onAction(LoanRequestAction.OnSuccessDialogDismiss) },
            modifier = Modifier
                .testTag(LoanRequestTestTags.SUCCESS_DIALOG)
                .semantics { contentDescription = successDialogCd },
            confirmTestTag = LoanRequestTestTags.SUCCESS_DIALOG_OK_BUTTON,
        )
    }
}

/**
 * Shared scrollable form body -- offline banner + savings/limit card + amount field + purpose
 * dropdown + duration slider + repayment summary + the trailing submit affordance (button,
 * spinner, or button+error-card depending on [screenState]). Rendered by every
 * [LoanRequestScreenState] member (`ui.yaml#states.*.components` all share this field set). See
 * API.md#screen.
 */
@Composable
internal fun LoanRequestFormFields(
    state: LoanRequestState,
    onAction: (LoanRequestAction) -> Unit,
    screenState: LoanRequestScreenState,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val fieldsEnabled = !state.isSubmitting

    val offlineBannerMessage = stringResource(Res.string.screens_loan_request_offline_banner)
    val offlineBannerCd = stringResource(Res.string.screens_loan_request_offline_banner_cd)

    val savingsLabel = stringResource(Res.string.screens_loan_request_savings_balance_label)
    val savingsIconCd = stringResource(Res.string.screens_loan_request_savings_icon_cd)
    val maxLoanLabel = stringResource(Res.string.screens_loan_request_max_loan_label)
    val maxLoanHint = stringResource(Res.string.screens_loan_request_loan_multiplier_hint)
    val maxLoanIconCd = stringResource(Res.string.screens_loan_request_max_loan_icon_cd)

    val amountLabel = stringResource(Res.string.screens_loan_request_amount_label)
    val amountSupportingText = stringResource(Res.string.screens_loan_request_amount_supporting_text)
    val amountCd = stringResource(Res.string.screens_loan_request_amount_cd)
    val amountIconCd = stringResource(Res.string.screens_loan_request_amount_icon_cd)
    val currencyPrefix = stringResource(Res.string.screens_loan_request_currency_prefix)

    val purposeLabel = stringResource(Res.string.screens_loan_request_purpose_label)
    val purposeCd = stringResource(Res.string.screens_loan_request_purpose_cd)

    val purposeOptions = listOf(
        LoanPurpose.SCHOOL_FEES to stringResource(Res.string.screens_loan_request_purpose_school_fees),
        LoanPurpose.MEDICAL to stringResource(Res.string.screens_loan_request_purpose_medical),
        LoanPurpose.BUSINESS to stringResource(Res.string.screens_loan_request_purpose_business),
        LoanPurpose.FARMING to stringResource(Res.string.screens_loan_request_purpose_farming),
        LoanPurpose.HOME_IMPROVEMENT to stringResource(Res.string.screens_loan_request_purpose_home_improvement),
        LoanPurpose.EMERGENCY to stringResource(Res.string.screens_loan_request_purpose_emergency),
        LoanPurpose.OTHER to stringResource(Res.string.screens_loan_request_purpose_other),
    )
    val selectedPurposeLabel = purposeOptions.firstOrNull { it.first == state.purpose }?.second.orEmpty()

    val durationLabel = stringResource(Res.string.screens_loan_request_duration_label)
    val durationWeeksValue = stringResource(Res.string.screens_loan_request_duration_weeks_value, state.durationWeeks)
    val durationCd = stringResource(Res.string.screens_loan_request_duration_cd)

    val principalLabel = stringResource(Res.string.screens_loan_request_principal_label)
    val interestLabel = stringResource(Res.string.screens_loan_request_interest_label)
    val totalLabel = stringResource(Res.string.screens_loan_request_total_repayment_label)

    val submitLabel = stringResource(Res.string.screens_loan_request_submit_button)
    val submitCd = stringResource(Res.string.screens_loan_request_submit_cd)
    val submittingCd = stringResource(Res.string.screens_loan_request_submitting_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(sp.sm)

        if (state.isOfflineMode) {
            LoanRequestOfflineBanner(
                message = offlineBannerMessage,
                iconContentDescription = offlineBannerCd,
                modifier = Modifier.testTag(LoanRequestTestTags.OFFLINE_BANNER),
            )
            Spacer(sp.sm)
        }

        LoanRequestSavingsLimitCard(
            savingsLabel = savingsLabel,
            savingsValue = stringResource(
                Res.string.screens_loan_request_kes_amount_format,
                state.savingsBalance.formatGrouped(0),
            ),
            savingsIconContentDescription = savingsIconCd,
            maxLoanLabel = maxLoanLabel,
            maxLoanValue = stringResource(
                Res.string.screens_loan_request_kes_amount_format,
                state.maxLoanAmount.formatGrouped(0),
            ),
            maxLoanHint = maxLoanHint,
            maxLoanIconContentDescription = maxLoanIconCd,
            modifier = Modifier.testTag(LoanRequestTestTags.SAVINGS_LIMIT_CARD),
        )
        Spacer(sp.md)

        LoanRequestAmountField(
            value = state.requestedAmount,
            label = amountLabel,
            prefix = currencyPrefix,
            supportingText = amountSupportingText,
            onValueChange = { onAction(LoanRequestAction.OnAmountChange(it)) },
            errorMessage = state.requestedAmountError?.let { fieldErrorMessage(it) },
            contentDescription = amountCd,
            leadingIconContentDescription = amountIconCd,
            enabled = fieldsEnabled,
            modifier = Modifier.testTag(LoanRequestTestTags.FIELD_AMOUNT),
        )
        Spacer(sp.md)

        LoanRequestDropdownField(
            label = purposeLabel,
            selectedOption = selectedPurposeLabel,
            options = purposeOptions.map { it.second },
            onOptionSelected = { selectedLabel ->
                purposeOptions.firstOrNull { it.second == selectedLabel }?.let { (purpose, _) ->
                    onAction(LoanRequestAction.OnPurposeSelected(purpose))
                }
            },
            contentDescription = purposeCd,
            errorMessage = state.purposeError?.let { fieldErrorMessage(it) },
            leadingIcon = Icons.Filled.Notes,
            leadingIconContentDescription = purposeCd,
            enabled = fieldsEnabled,
            modifier = Modifier.testTag(LoanRequestTestTags.DROPDOWN_PURPOSE),
        )
        Spacer(sp.md)

        LoanRequestDurationSelector(
            headerLabel = durationLabel,
            weeksValueLabel = durationWeeksValue,
            durationWeeks = state.durationWeeks,
            onDurationChanged = { onAction(LoanRequestAction.OnDurationChanged(it)) },
            contentDescription = durationCd,
            enabled = fieldsEnabled,
            modifier = Modifier.testTag(LoanRequestTestTags.DURATION_SLIDER),
        )
        Spacer(sp.md)

        if (state.requestedAmount.isNotBlank() && state.requestedAmountError == null) {
            val principal = state.requestedAmount.toDoubleOrNull() ?: 0.0
            val totalRepayment = state.repaymentEstimate * state.durationWeeks
            val interest = totalRepayment - principal
            LoanRequestRepaymentSummaryCard(
                principalLabel = principalLabel,
                principalValue = stringResource(
                    Res.string.screens_loan_request_kes_amount_format,
                    principal.formatGrouped(0),
                ),
                interestLabel = interestLabel,
                interestValue = stringResource(
                    Res.string.screens_loan_request_kes_amount_format,
                    interest.formatGrouped(0),
                ),
                totalLabel = totalLabel,
                totalValue = stringResource(
                    Res.string.screens_loan_request_kes_amount_format,
                    totalRepayment.formatGrouped(0),
                ),
                modifier = Modifier.testTag(LoanRequestTestTags.REPAYMENT_SUMMARY_CARD),
            )
            Spacer(sp.md)
        }

        when (screenState) {
            LoanRequestScreenState.Submitting -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .testTag(LoanRequestTestTags.SUBMITTING_INDICATOR)
                        .semantics { contentDescription = submittingCd },
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LoanRequestScreenState.SubmitError -> {
                LoanRequestSubmitButton(
                    label = submitLabel,
                    contentDescription = submitCd,
                    enabled = state.isFormValid,
                    onClick = { onAction(LoanRequestAction.OnSubmitClick) },
                )
                Spacer(sp.sm)
                state.submitError?.let { error ->
                    LoanRequestErrorCard(
                        message = error.resolvedMessage(),
                        iconContentDescription = stringResource(Res.string.screens_loan_request_error_icon_cd),
                        retryLabel = stringResource(Res.string.screens_loan_request_retry_button),
                        retryContentDescription = stringResource(Res.string.screens_loan_request_retry_cd),
                        onRetryClick = { onAction(LoanRequestAction.OnRetry) },
                        retryTestTag = LoanRequestTestTags.ERROR_CARD_RETRY_BUTTON,
                        modifier = Modifier.testTag(LoanRequestTestTags.ERROR_CARD),
                    )
                }
            }
            LoanRequestScreenState.Content,
            LoanRequestScreenState.SubmitSuccess,
            LoanRequestScreenState.OfflineQueued,
            -> {
                LoanRequestSubmitButton(
                    label = submitLabel,
                    contentDescription = submitCd,
                    enabled = state.isFormValid,
                    onClick = { onAction(LoanRequestAction.OnSubmitClick) },
                )
            }
        }
        Spacer(sp.xxl)
    }
}

/** `ui.yaml#components.submit_button` -- `enabled_when: "isFormValid && !isSubmitting"`. */
@Composable
internal fun LoanRequestSubmitButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    KptButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(LoanRequestTestTags.SUBMIT_BUTTON)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(label)
    }
}

/** Vertical-gap shorthand -- `Spacer(sp.sm)` reads cleaner than `Spacer(Modifier.height(sp.sm))`. */
@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}

/**
 * Maps [LoanRequestState.requestedAmountError] / [LoanRequestState.purposeError] (a `messageKey`
 * string written by `LoanRequestViewModel`, currently only ever `"error_validation"`) to its
 * localized message.
 */
@Composable
private fun fieldErrorMessage(key: String): String = when (key) {
    "error_validation" -> stringResource(Res.string.screens_loan_request_error_validation)
    else -> stringResource(Res.string.screens_loan_request_error_validation)
}

/** Maps [SubmitError.messageKey] to its localized message. */
@Composable
private fun SubmitError.resolvedMessage(): String = when (this) {
    SubmitError.Network -> stringResource(Res.string.screens_loan_request_error_network_queued)
    SubmitError.Server -> stringResource(Res.string.screens_loan_request_error_server)
    SubmitError.Validation -> stringResource(Res.string.screens_loan_request_error_validation)
    SubmitError.Unauthorized -> stringResource(Res.string.screens_loan_request_error_session_expired)
}
