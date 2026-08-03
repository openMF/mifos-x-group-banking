/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanapply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatDecimal
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.feature.loanapply.components.LoanApplyAmountField
import org.mifos.groupbanking.feature.loanapply.components.LoanApplyCorpusWarningBanner
import org.mifos.groupbanking.feature.loanapply.components.LoanApplyDropdownField
import org.mifos.groupbanking.feature.loanapply.components.LoanApplyEligibilityBanner
import org.mifos.groupbanking.feature.loanapply.generated.resources.Res
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_action_retry
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_action_retry_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_action_submit
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_action_submit_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_action_submitting
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_action_submitting_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_amount_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_amount_icon_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_amount_label
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_amount_placeholder
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_back_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_corpus_warning
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_corpus_warning_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_12w
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_24w
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_4w
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_52w
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_8w
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_duration_label
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_eligibility_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_eligibility_max
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_eligibility_savings
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_amount_exceeds
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_auth
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_corpus
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_icon_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_network
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_server
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_error_title
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_loading_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_member_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_member_label
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_member_placeholder
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_product_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_product_label
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_product_placeholder
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_business
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_education
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_emergency
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_label
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_medical
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_purpose_other
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_success_icon_cd
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_success_message
import org.mifos.groupbanking.feature.loanapply.generated.resources.screens_loan_apply_topbar_title

/**
 * Container for `loan-apply-screen` (`ui.yaml#route`: `/groups/{groupId}/loans/apply`). Collects
 * [LoanApplyViewModel] state via [collectAsStateWithLifecycle], forwards the required [groupId]
 * nav-arg to Koin via `parametersOf(groupId)` (mirrors `LoanApplyModule`'s `viewModel {
 * parameters -> ... }` builder KDoc), consumes one-shot [LoanApplyEvent]s (navigation +
 * snackbar) through [EventsEffect], and delegates all rendering to the stateless
 * [LoanApplyContent]. See API.md#screen.
 */
@Composable
internal fun LoanApplyScreen(
    groupId: Long,
    onNavigateToMeetingConduct: (loanId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanApplyViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect -- stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as MemberAddScreen.kt /
    // GroupCreateScreen.kt. LoanApplyEvent.ShowSnackbar only ever carries "error_network" or
    // "error_auth" (see LoanApplyViewModel.handleSubmit / handleSubmitResult).
    val networkErrorMessage = stringResource(Res.string.screens_loan_apply_error_network)
    val authErrorMessage = stringResource(Res.string.screens_loan_apply_error_auth)

    EventsEffect(viewModel) { event ->
        when (event) {
            is LoanApplyEvent.NavigateToMeetingConduct -> onNavigateToMeetingConduct(event.loanId)
            LoanApplyEvent.NavigateBack -> onNavigateBack()
            is LoanApplyEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_network" -> networkErrorMessage
                    "error_auth" -> authErrorMessage
                    else -> event.message
                },
            )
        }
    }

    LoanApplyContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `loan-apply-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` -- every [LoanApplyScreenState] member is handled.
 * `isLoadingTemplate` (see [LoanApplyState.deriveScreenState]) flips the WHOLE screen back to
 * [LoanApplyScreenState.Loading] whenever a member/product selection is refreshing the template
 * -- the top-bar-only `ui.yaml#states.loading` shape is deliberately reused for that reload, not
 * just the initial mount. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoanApplyContent(
    state: LoanApplyState,
    onAction: (LoanApplyAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val screenState = state.deriveScreenState()
    val title = stringResource(Res.string.screens_loan_apply_topbar_title)
    val backCd = stringResource(Res.string.screens_loan_apply_back_cd)

    KptScaffold(
        modifier = modifier.testTag(LoanApplyTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationIonClick = { onAction(LoanApplyAction.OnBack) },
                    testTag = LoanApplyTestTags.TOP_BAR,
                    contentDescription = backCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        when (screenState) {
            LoanApplyScreenState.Loading -> LoanApplyLoadingSection()
            LoanApplyScreenState.Content -> LoanApplyFormSection(state = state, onAction = onAction, enabled = true)
            LoanApplyScreenState.Submitting -> LoanApplyFormSection(state = state, onAction = onAction, enabled = false)
            LoanApplyScreenState.Error -> LoanApplyErrorSection(state = state, onAction = onAction)
            LoanApplyScreenState.Success -> LoanApplySuccessSection()
        }
    }
}

/** `LoanApplyScreenState.Loading` -- `ui.yaml#states.loading` (`components: [top_bar]`). See API.md#screen. */
@Composable
internal fun LoanApplyLoadingSection(modifier: Modifier = Modifier) {
    val loadingCd = stringResource(Res.string.screens_loan_apply_loading_cd)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingCd },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(LoanApplyTestTags.LOADING_INDICATOR))
    }
}

/**
 * Shared form body -- member/product/duration/purpose dropdowns + eligibility banner + amount
 * field + corpus-warning banner + submit button -- rendered by both
 * `LoanApplyScreenState.Content` and `LoanApplyScreenState.Submitting`
 * (`ui.yaml#states.content`/`.submitting` declare the identical component list; only [enabled]
 * differs, mirroring `MemberAddFormFields`'s identical Content/Submitting factoring). See
 * API.md#screen.
 */
@Composable
internal fun LoanApplyFormSection(
    state: LoanApplyState,
    onAction: (LoanApplyAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    val memberLabel = stringResource(Res.string.screens_loan_apply_member_label)
    val memberPlaceholder = stringResource(Res.string.screens_loan_apply_member_placeholder)
    val memberCd = stringResource(Res.string.screens_loan_apply_member_cd)
    val eligibilityCd = stringResource(Res.string.screens_loan_apply_eligibility_cd)
    val amountLabel = stringResource(Res.string.screens_loan_apply_amount_label)
    val amountPlaceholder = stringResource(Res.string.screens_loan_apply_amount_placeholder)
    val amountCd = stringResource(Res.string.screens_loan_apply_amount_cd)
    val amountIconCd = stringResource(Res.string.screens_loan_apply_amount_icon_cd)
    val corpusWarningCd = stringResource(Res.string.screens_loan_apply_corpus_warning_cd)
    val durationLabel = stringResource(Res.string.screens_loan_apply_duration_label)
    val durationCd = stringResource(Res.string.screens_loan_apply_duration_cd)
    val purposeLabel = stringResource(Res.string.screens_loan_apply_purpose_label)
    val purposeCd = stringResource(Res.string.screens_loan_apply_purpose_cd)
    val productLabel = stringResource(Res.string.screens_loan_apply_product_label)
    val productPlaceholder = stringResource(Res.string.screens_loan_apply_product_placeholder)
    val productCd = stringResource(Res.string.screens_loan_apply_product_cd)
    val submitLabel = stringResource(Res.string.screens_loan_apply_action_submit)
    val submitCd = stringResource(Res.string.screens_loan_apply_action_submit_cd)
    val submittingLabel = stringResource(Res.string.screens_loan_apply_action_submitting)
    val submittingCd = stringResource(Res.string.screens_loan_apply_action_submitting_cd)

    val durationOptions = listOf(
        4 to stringResource(Res.string.screens_loan_apply_duration_4w),
        8 to stringResource(Res.string.screens_loan_apply_duration_8w),
        12 to stringResource(Res.string.screens_loan_apply_duration_12w),
        24 to stringResource(Res.string.screens_loan_apply_duration_24w),
        52 to stringResource(Res.string.screens_loan_apply_duration_52w),
    )
    val selectedDurationLabel = durationOptions.firstOrNull { it.first == state.durationWeeks }?.second
        ?: durationOptions.first { it.first == 12 }.second

    val purposeOptions = listOf(
        LoanPurpose.MEDICAL to stringResource(Res.string.screens_loan_apply_purpose_medical),
        LoanPurpose.EDUCATION to stringResource(Res.string.screens_loan_apply_purpose_education),
        LoanPurpose.BUSINESS to stringResource(Res.string.screens_loan_apply_purpose_business),
        LoanPurpose.EMERGENCY to stringResource(Res.string.screens_loan_apply_purpose_emergency),
        LoanPurpose.OTHER to stringResource(Res.string.screens_loan_apply_purpose_other),
    )
    val selectedPurposeLabel = purposeOptions.firstOrNull { it.first == state.purpose }?.second
        ?: purposeOptions.first { it.first == LoanPurpose.BUSINESS }.second

    // `ui.yaml#components.submit_button.enabled_when` verbatim, gated additionally by [enabled]
    // (false while Submitting -- disables the button so a spinner tap can't double-submit).
    val isSubmitEnabled = enabled &&
        state.selectedMember != null &&
        state.requestedAmount.isNotBlank() &&
        state.amountError == null &&
        state.selectedProduct != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(sp.md)

        LoanApplyDropdownField(
            label = memberLabel,
            selectedOption = state.selectedMember?.displayName.orEmpty(),
            options = state.members.map { it.displayName },
            onOptionSelected = { selected ->
                state.members.firstOrNull { it.displayName == selected }?.let { member ->
                    onAction(LoanApplyAction.OnMemberSelected(member))
                }
            },
            placeholder = memberPlaceholder,
            contentDescription = memberCd,
            enabled = enabled,
            modifier = Modifier.testTag(LoanApplyTestTags.DROPDOWN_MEMBER),
        )
        Spacer(sp.md)

        if (state.selectedMember != null) {
            LoanApplyEligibilityBanner(
                savingsLine = stringResource(
                    Res.string.screens_loan_apply_eligibility_savings,
                    state.memberSavingsBalance.formatGrouped(0),
                ),
                eligibleLine = stringResource(
                    Res.string.screens_loan_apply_eligibility_max,
                    state.eligibleAmount.formatGrouped(0),
                    state.loanMultiplier.formatDecimal(1),
                ),
                contentDescription = eligibilityCd,
                modifier = Modifier.testTag(LoanApplyTestTags.ELIGIBILITY_BANNER),
            )
            Spacer(sp.md)
        }

        LoanApplyAmountField(
            value = state.requestedAmount,
            label = amountLabel,
            placeholder = amountPlaceholder,
            onValueChange = { onAction(LoanApplyAction.OnAmountChanged(it)) },
            errorMessage = state.amountError?.let { amountErrorMessage(it) },
            contentDescription = amountCd,
            leadingIconContentDescription = amountIconCd,
            enabled = enabled,
            modifier = Modifier.testTag(LoanApplyTestTags.FIELD_AMOUNT),
        )
        Spacer(sp.md)

        if (state.corpusWarning) {
            LoanApplyCorpusWarningBanner(
                message = stringResource(
                    Res.string.screens_loan_apply_corpus_warning,
                    state.corpusBalance.formatGrouped(0),
                ),
                iconContentDescription = corpusWarningCd,
                modifier = Modifier.testTag(LoanApplyTestTags.CORPUS_WARNING_BANNER),
            )
            Spacer(sp.md)
        }

        LoanApplyDropdownField(
            label = durationLabel,
            selectedOption = selectedDurationLabel,
            options = durationOptions.map { it.second },
            onOptionSelected = { selectedLabel ->
                durationOptions.firstOrNull { it.second == selectedLabel }?.let { (weeks, _) ->
                    onAction(LoanApplyAction.OnDurationChanged(weeks))
                }
            },
            contentDescription = durationCd,
            enabled = enabled,
            modifier = Modifier.testTag(LoanApplyTestTags.DROPDOWN_DURATION),
        )
        Spacer(sp.md)

        LoanApplyDropdownField(
            label = purposeLabel,
            selectedOption = selectedPurposeLabel,
            options = purposeOptions.map { it.second },
            onOptionSelected = { selectedLabel ->
                purposeOptions.firstOrNull { it.second == selectedLabel }?.let { (purpose, _) ->
                    onAction(LoanApplyAction.OnPurposeChanged(purpose))
                }
            },
            contentDescription = purposeCd,
            enabled = enabled,
            modifier = Modifier.testTag(LoanApplyTestTags.DROPDOWN_PURPOSE),
        )
        Spacer(sp.md)

        LoanApplyDropdownField(
            label = productLabel,
            selectedOption = state.selectedProduct?.name.orEmpty(),
            options = state.loanProducts.map { it.name },
            onOptionSelected = { selected ->
                state.loanProducts.firstOrNull { it.name == selected }?.let { product ->
                    onAction(LoanApplyAction.OnProductSelected(product))
                }
            },
            placeholder = productPlaceholder,
            contentDescription = productCd,
            enabled = enabled,
            modifier = Modifier.testTag(LoanApplyTestTags.DROPDOWN_PRODUCT),
        )
        Spacer(sp.lg)

        KptButton(
            onClick = { onAction(LoanApplyAction.OnSubmit) },
            enabled = isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .testTag(LoanApplyTestTags.SUBMIT_BUTTON)
                .semantics { contentDescription = if (state.isSubmitting) submittingCd else submitCd },
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                HSpacer(sp.sm)
                Text(submittingLabel)
            } else {
                Text(submitLabel)
            }
        }
        Spacer(sp.xxl)
    }
}

/**
 * `LoanApplyScreenState.Error` -- `ui.yaml#states.error` (`components: [top_bar, error_state]`
 * -- the error frame REPLACES the form entirely, unlike `MemberAddErrorSection`'s inline-banner
 * convention). The CTA re-dispatches `OnSubmit` (`ui.yaml#components.error_state.on_click.action`).
 * See API.md#screen.
 */
@Composable
internal fun LoanApplyErrorSection(
    state: LoanApplyState,
    onAction: (LoanApplyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val title = stringResource(Res.string.screens_loan_apply_error_title)
    val iconCd = stringResource(Res.string.screens_loan_apply_error_icon_cd)
    val retryLabel = stringResource(Res.string.screens_loan_apply_action_retry)
    val retryCd = stringResource(Res.string.screens_loan_apply_action_retry_cd)
    val error = state.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = sp.lg)
            .testTag(LoanApplyTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = iconCd,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(sp.md)
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (error != null) {
            Spacer(sp.sm)
            Text(
                text = error.resolvedMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(sp.lg)
        KptButton(
            onClick = { onAction(LoanApplyAction.OnSubmit) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .testTag(LoanApplyTestTags.ERROR_RETRY_BUTTON)
                .semantics { contentDescription = retryCd },
        ) {
            Text(retryLabel)
        }
    }
}

/**
 * Transient `LoanApplyScreenState.Success` frame -- `LoanApplyEvent.NavigateToMeetingConduct`
 * fires the same frame this renders (mirrors `MemberAddSuccessSection`'s identical
 * transient-frame precedent; `ui.yaml#states` declares no dedicated `success` block since the
 * screen navigates away immediately). See API.md#screen.
 */
@Composable
internal fun LoanApplySuccessSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val successIconCd = stringResource(Res.string.screens_loan_apply_success_icon_cd)
    val successMessage = stringResource(Res.string.screens_loan_apply_success_message)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = sp.lg)
            .testTag(LoanApplyTestTags.SUCCESS_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp).semantics { contentDescription = successIconCd },
        )
        Spacer(sp.md)
        Text(text = successMessage, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Maps [LoanApplyState.amountError] (a `messageKey` string written by
 * `LoanApplyViewModel.computeAmountError`, currently only ever
 * `LoanApplyError.AmountExceedsEligibility.messageKey`) to its localized message.
 */
@Composable
private fun amountErrorMessage(key: String): String = when (key) {
    "error_amount_exceeds" -> stringResource(Res.string.screens_loan_apply_error_amount_exceeds)
    else -> stringResource(Res.string.screens_loan_apply_error_amount_exceeds)
}

/** Maps [LoanApplyError.messageKey] to its localized message. */
@Composable
private fun LoanApplyError.resolvedMessage(): String = when (this) {
    LoanApplyError.Network -> stringResource(Res.string.screens_loan_apply_error_network)
    LoanApplyError.Server -> stringResource(Res.string.screens_loan_apply_error_server)
    LoanApplyError.AmountExceedsEligibility -> stringResource(Res.string.screens_loan_apply_error_amount_exceeds)
    LoanApplyError.CorpusInsufficient -> stringResource(Res.string.screens_loan_apply_error_corpus)
    LoanApplyError.Auth -> stringResource(Res.string.screens_loan_apply_error_auth)
}

/** Vertical-gap shorthand -- `Spacer(sp.md)` reads cleaner than `Spacer(Modifier.height(sp.md))`. */
@Composable
private fun Spacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}

/** Horizontal-gap shorthand used inside the submit button's spinner+label row. */
@Composable
private fun HSpacer(width: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(width))
}
