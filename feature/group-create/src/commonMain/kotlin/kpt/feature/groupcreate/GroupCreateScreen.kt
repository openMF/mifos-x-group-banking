/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.groupcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.AppCard
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptOutlinedButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.model.GroupTypeConfig
import kpt.core.ui.scaffold.KptScaffold
import kpt.feature.groupcreate.components.GroupCreateDropdownField
import kpt.feature.groupcreate.components.GroupCreateErrorBanner
import kpt.feature.groupcreate.components.GroupCreateTextField
import kpt.feature.groupcreate.components.GroupTypeBanner
import kpt.feature.groupcreate.components.OfflineNoticeBanner
import kpt.feature.groupcreate.components.ReviewSection
import kpt.feature.groupcreate.components.WizardStepIndicator
import kpt.feature.groupcreate.generated.resources.Res
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_back
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_back_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_next
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_next_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_retry
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_retry_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_submit
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_submit_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_submitting
import kpt.feature.groupcreate.generated.resources.screens_group_create_action_submitting_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_close_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_error_auth
import kpt.feature.groupcreate.generated.resources.screens_group_create_error_icon_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_error_network
import kpt.feature.groupcreate.generated.resources.screens_group_create_error_server
import kpt.feature.groupcreate.generated.resources.screens_group_create_error_title
import kpt.feature.groupcreate.generated.resources.screens_group_create_error_validation
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_contribution_amount_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_contribution_amount_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_currency_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_currency_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_cycle_length_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_cycle_length_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_fine_amount_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_fine_amount_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_interest_rate_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_interest_rate_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_loan_multiplier_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_loan_multiplier_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_max_members_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_max_members_help
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_max_members_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_meeting_day_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_meeting_day_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_meeting_time_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_meeting_time_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_meeting_time_placeholder
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_name_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_name_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_name_placeholder
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_office_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_office_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_office_placeholder
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_payout_order_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_payout_order_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_share_max_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_share_max_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_share_min_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_share_min_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_share_value_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_share_value_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_social_fund_percent_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_social_fund_percent_help
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_social_fund_percent_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_social_fund_toggle_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_field_social_fund_toggle_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_offline_dialog_confirm
import kpt.feature.groupcreate.generated.resources.screens_group_create_offline_dialog_message
import kpt.feature.groupcreate.generated.resources.screens_group_create_offline_dialog_title
import kpt.feature.groupcreate.generated.resources.screens_group_create_offline_notice
import kpt.feature.groupcreate.generated.resources.screens_group_create_offline_notice_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_review_title
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_contribution
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_contribution_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_currency
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_cycle_length
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_cycle_length_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_group_type
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_interest_rate
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_interest_rate_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_late_fine
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_late_fine_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_loan_multiplier
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_loan_multiplier_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_max_members
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_meeting
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_meeting_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_name
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_office
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_payout_order
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_share_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_share_value_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_shares_per_meeting
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_shares_per_meeting_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_social_fund
import kpt.feature.groupcreate.generated.resources.screens_group_create_row_social_fund_value
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_identity
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_identity_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_loan_cycle_rules
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_members
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_members_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_rules
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_rules_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_section_share_contributions
import kpt.feature.groupcreate.generated.resources.screens_group_create_step_identity
import kpt.feature.groupcreate.generated.resources.screens_group_create_step_indicator_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_step_members
import kpt.feature.groupcreate.generated.resources.screens_group_create_step_review
import kpt.feature.groupcreate.generated.resources.screens_group_create_step_rules
import kpt.feature.groupcreate.generated.resources.screens_group_create_success_icon_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_success_invite_label
import kpt.feature.groupcreate.generated.resources.screens_group_create_success_message
import kpt.feature.groupcreate.generated.resources.screens_group_create_success_redirecting
import kpt.feature.groupcreate.generated.resources.screens_group_create_success_title
import kpt.feature.groupcreate.generated.resources.screens_group_create_success_topbar_title
import kpt.feature.groupcreate.generated.resources.screens_group_create_topbar_title
import kpt.feature.groupcreate.generated.resources.screens_group_create_type_banner_cd
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_contribution_amount_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_cycle_length_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_fine_amount_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_group_name_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_interest_rate_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_loan_multiplier_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_max_members_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_meeting_day_required
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_meeting_time_required
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_office_required
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_payout_order_required
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_share_max_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_share_min_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_share_value_invalid
import kpt.feature.groupcreate.generated.resources.screens_group_create_validation_social_fund_percent_invalid
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Weekday options for `meeting_day_dropdown` — domain vocabulary, not translatable UI copy. */
private val MEETING_DAY_OPTIONS = // i18n:skip
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Currency codes for `currency_dropdown` — ISO codes, not translatable UI copy. */
private val CURRENCY_OPTIONS = listOf("KES", "USD", "UGX", "TZS") // i18n:skip

/** Payout-order method codes for `payout_order_dropdown` — domain enum values, shown raw. */
private val PAYOUT_ORDER_OPTIONS = // i18n:skip
    listOf("FIXED_ORDER", "LOTTERY", "AUCTION", "NEED_BASED")

/**
 * Container for the 4-step, type-adaptive group-create wizard (`group-create-screen`). Collects
 * [GroupCreateViewModel] state via [collectAsStateWithLifecycle], forwards the required
 * [typeConfig] nav-arg to Koin via `parametersOf(typeConfig)` (`GroupTypeConfig` is a plain,
 * non-`@Serializable` domain model — see `GroupCreateRoute.kt` for how the caller reconstructs it
 * from the route's flattened primitive args), consumes one-shot [GroupCreateEvent]s (navigation +
 * offline dialog + snackbar) through [EventsEffect], and delegates all rendering to the stateless
 * [GroupCreateContent]. See API.md#screen.
 */
@Composable
internal fun GroupCreateScreen(
    typeConfig: GroupTypeConfig,
    onNavigateToGroupDashboard: (groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupCreateViewModel = koinViewModel(parameters = { parametersOf(typeConfig) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOfflineDialog by remember { mutableStateOf(false) }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as LoginSignupScreen.kt.
    val networkErrorMessage = stringResource(Res.string.screens_group_create_error_network)
    val authErrorMessage = stringResource(Res.string.screens_group_create_error_auth)
    val serverErrorMessage = stringResource(Res.string.screens_group_create_error_server)
    val validationErrorMessage = stringResource(Res.string.screens_group_create_error_validation)

    EventsEffect(viewModel) { event ->
        when (event) {
            is GroupCreateEvent.NavigateToGroupDashboard -> onNavigateToGroupDashboard(event.groupId)
            GroupCreateEvent.NavigateBack -> onNavigateBack()
            GroupCreateEvent.ShowOfflineSyncDialog -> showOfflineDialog = true
            is GroupCreateEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_network" -> networkErrorMessage
                    "error_auth" -> authErrorMessage
                    "error_server" -> serverErrorMessage
                    "error_validation" -> validationErrorMessage
                    else -> event.message
                },
            )
        }
    }

    GroupCreateContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (showOfflineDialog) {
        OfflineSyncDialog(onDismiss = { showOfflineDialog = false })
    }
}

/**
 * Stateless render surface for `group-create-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` — every [GroupCreateScreenState] member is handled. The top
 * app bar's close icon is hidden while `Submitting`/`Success` (mirrors `submitting.html`'s
 * `visibility:hidden` close button — the mid-call/post-success wizard should not be dismissible).
 * See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupCreateContent(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val screenState = state.deriveScreenState()
    val closeCd = stringResource(Res.string.screens_group_create_close_cd)
    val wizardTitle = stringResource(Res.string.screens_group_create_topbar_title, state.groupTypeName)
    val successTitle = stringResource(Res.string.screens_group_create_success_topbar_title)
    val showCloseIcon = screenState == GroupCreateScreenState.Content || screenState == GroupCreateScreenState.Error

    KptScaffold(
        modifier = modifier.testTag(GroupCreateTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = if (screenState == GroupCreateScreenState.Success) successTitle else wizardTitle,
                    navigationIcon = if (showCloseIcon) Icons.Filled.Close else null,
                    onNavigationIonClick = if (showCloseIcon) {
                        { onAction(GroupCreateAction.OnBack) }
                    } else {
                        null
                    },
                    testTag = GroupCreateTestTags.TOP_BAR,
                    contentDescription = closeCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        when (screenState) {
            GroupCreateScreenState.Content -> GroupCreateWizardSection(state = state, onAction = onAction)
            GroupCreateScreenState.Submitting -> GroupCreateSubmittingSection(state = state, onAction = onAction)
            GroupCreateScreenState.Error -> GroupCreateErrorSection(state = state, onAction = onAction)
            GroupCreateScreenState.Success -> GroupCreateSuccessSection(state = state)
        }
    }
}

/**
 * `GroupCreateScreenState.Content` — `ui.yaml#states.content`. Renders the type banner + step
 * indicator + the active step's form (`currentStep` 1-4). See API.md#screen.
 */
@Composable
internal fun GroupCreateWizardSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val typeBannerCd = stringResource(Res.string.screens_group_create_type_banner_cd, state.groupTypeName)
    val stepIndicatorCd = stringResource(Res.string.screens_group_create_step_indicator_cd)
    val stepLabels = listOf(
        stringResource(Res.string.screens_group_create_step_identity),
        stringResource(Res.string.screens_group_create_step_rules),
        stringResource(Res.string.screens_group_create_step_members),
        stringResource(Res.string.screens_group_create_step_review),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(Modifier.height(sp.md))
        GroupTypeBanner(
            groupTypeName = state.groupTypeName,
            contentDescription = typeBannerCd,
            modifier = Modifier.testTag(GroupCreateTestTags.TYPE_BANNER),
        )
        Spacer(Modifier.height(sp.md))
        WizardStepIndicator(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps,
            labels = stepLabels,
            contentDescription = stepIndicatorCd,
            modifier = Modifier.fillMaxWidth().testTag(GroupCreateTestTags.STEP_INDICATOR),
        )
        Spacer(Modifier.height(sp.lg))
        when (state.currentStep) {
            1 -> GroupIdentityStepSection(state = state, onAction = onAction)
            2 -> GroupRulesStepSection(state = state, onAction = onAction)
            3 -> GroupMembersStepSection(state = state, onAction = onAction)
            else -> GroupReviewStepSection(state = state, onAction = onAction, isSubmitting = false)
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/** Step 1 — Identity (`ui.yaml#forms.group_identity_form`). See API.md#screen. */
@Composable
internal fun GroupIdentityStepSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val nameCd = stringResource(Res.string.screens_group_create_field_name_cd)
    val officeCd = stringResource(Res.string.screens_group_create_field_office_cd)
    val currencyCd = stringResource(Res.string.screens_group_create_field_currency_cd)
    val meetingDayCd = stringResource(Res.string.screens_group_create_field_meeting_day_cd)
    val meetingTimeCd = stringResource(Res.string.screens_group_create_field_meeting_time_cd)
    val nextCd = stringResource(Res.string.screens_group_create_action_next_cd)

    Column(modifier = modifier) {
        AppCard {
            Column {
                GroupCreateTextField(
                    value = state.groupName,
                    label = stringResource(Res.string.screens_group_create_field_name_label),
                    placeholder = stringResource(Res.string.screens_group_create_field_name_placeholder),
                    onValueChange = { onAction(GroupCreateAction.OnNameChange(it)) },
                    errorMessage = state.validationErrors["groupName"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = nameCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.FIELD_GROUP_NAME),
                )
                Spacer(Modifier.height(sp.md))

                GroupCreateDropdownField(
                    label = stringResource(Res.string.screens_group_create_field_office_label),
                    selectedOption = state.officeName,
                    options = state.officeList.map { it.name },
                    onOptionSelected = { name ->
                        state.officeList.firstOrNull { it.name == name }?.let { office ->
                            onAction(GroupCreateAction.OnOfficeSelect(officeId = office.id, officeName = office.name))
                        }
                    },
                    placeholder = stringResource(Res.string.screens_group_create_field_office_placeholder),
                    errorMessage = state.validationErrors["officeId"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = officeCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.DROPDOWN_OFFICE),
                )
                Spacer(Modifier.height(sp.md))

                GroupCreateDropdownField(
                    label = stringResource(Res.string.screens_group_create_field_currency_label),
                    selectedOption = state.currency,
                    options = CURRENCY_OPTIONS,
                    onOptionSelected = { onAction(GroupCreateAction.OnCurrencyChange(it)) },
                    contentDescription = currencyCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.DROPDOWN_CURRENCY),
                )
                Spacer(Modifier.height(sp.md))

                GroupCreateDropdownField(
                    label = stringResource(Res.string.screens_group_create_field_meeting_day_label),
                    selectedOption = state.meetingDay,
                    options = MEETING_DAY_OPTIONS,
                    onOptionSelected = { onAction(GroupCreateAction.OnMeetingDaySelect(it)) },
                    errorMessage = state.validationErrors["meetingDay"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = meetingDayCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.DROPDOWN_MEETING_DAY),
                )
                Spacer(Modifier.height(sp.md))

                GroupCreateTextField(
                    value = state.meetingTime,
                    label = stringResource(Res.string.screens_group_create_field_meeting_time_label),
                    placeholder = stringResource(Res.string.screens_group_create_field_meeting_time_placeholder),
                    onValueChange = { onAction(GroupCreateAction.OnMeetingTimeSelect(it)) },
                    errorMessage = state.validationErrors["meetingTime"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = meetingTimeCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.FIELD_MEETING_TIME),
                )
            }
        }
        Spacer(Modifier.height(sp.lg))
        KptButton(
            onClick = { onAction(GroupCreateAction.OnNextStep) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.NEXT_BUTTON)
                .semantics { contentDescription = nextCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_next))
        }
    }
}

/**
 * Step 2 — type-adaptive Rules (`ui.yaml#forms.group_rules_form`). Field visibility mirrors
 * `GroupCreateState.isShareBasedContribution` / `.isRotatingPayout`. See API.md#screen.
 */
@Composable
internal fun GroupRulesStepSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val shareValueCd = stringResource(Res.string.screens_group_create_field_share_value_cd)
    val shareMinCd = stringResource(Res.string.screens_group_create_field_share_min_cd)
    val shareMaxCd = stringResource(Res.string.screens_group_create_field_share_max_cd)
    val contributionCd = stringResource(Res.string.screens_group_create_field_contribution_amount_cd)
    val payoutOrderCd = stringResource(Res.string.screens_group_create_field_payout_order_cd)
    val loanMultiplierCd = stringResource(Res.string.screens_group_create_field_loan_multiplier_cd)
    val interestRateCd = stringResource(Res.string.screens_group_create_field_interest_rate_cd)
    val cycleLengthCd = stringResource(Res.string.screens_group_create_field_cycle_length_cd)
    val fineAmountCd = stringResource(Res.string.screens_group_create_field_fine_amount_cd)
    val socialFundToggleCd = stringResource(Res.string.screens_group_create_field_social_fund_toggle_cd)
    val socialFundPercentCd = stringResource(Res.string.screens_group_create_field_social_fund_percent_cd)
    val backCd = stringResource(Res.string.screens_group_create_action_back_cd)
    val nextCd = stringResource(Res.string.screens_group_create_action_next_cd)

    Column(modifier = modifier) {
        AppCard {
            Column {
                if (state.isShareBasedContribution) {
                    Text(
                        text = stringResource(Res.string.screens_group_create_section_share_contributions),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(sp.sm))
                    GroupCreateTextField(
                        value = state.shareValue,
                        label = stringResource(Res.string.screens_group_create_field_share_value_label),
                        onValueChange = { onAction(GroupCreateAction.OnShareValueChange(it)) },
                        keyboardType = KeyboardType.Number,
                        errorMessage = state.validationErrors["shareValue"]?.let { groupCreateValidationMessage(it) },
                        contentDescription = shareValueCd,
                        modifier = Modifier.testTag(GroupCreateTestTags.FIELD_SHARE_VALUE),
                    )
                    Spacer(Modifier.height(sp.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.md)) {
                        GroupCreateTextField(
                            value = state.shareMin,
                            label = stringResource(Res.string.screens_group_create_field_share_min_label),
                            onValueChange = { onAction(GroupCreateAction.OnShareMinChange(it)) },
                            keyboardType = KeyboardType.Number,
                            errorMessage = state.validationErrors["shareMin"]?.let { groupCreateValidationMessage(it) },
                            contentDescription = shareMinCd,
                            modifier = Modifier.weight(1f).testTag(GroupCreateTestTags.FIELD_SHARE_MIN),
                        )
                        GroupCreateTextField(
                            value = state.shareMax,
                            label = stringResource(Res.string.screens_group_create_field_share_max_label),
                            onValueChange = { onAction(GroupCreateAction.OnShareMaxChange(it)) },
                            keyboardType = KeyboardType.Number,
                            errorMessage = state.validationErrors["shareMax"]?.let { groupCreateValidationMessage(it) },
                            contentDescription = shareMaxCd,
                            modifier = Modifier.weight(1f).testTag(GroupCreateTestTags.FIELD_SHARE_MAX),
                        )
                    }
                } else {
                    GroupCreateTextField(
                        value = state.contributionAmount,
                        label = stringResource(Res.string.screens_group_create_field_contribution_amount_label),
                        onValueChange = { onAction(GroupCreateAction.OnContributionAmountChange(it)) },
                        keyboardType = KeyboardType.Number,
                        errorMessage = state.validationErrors["contributionAmount"]?.let {
                            groupCreateValidationMessage(it)
                        },
                        contentDescription = contributionCd,
                        modifier = Modifier.testTag(GroupCreateTestTags.FIELD_CONTRIBUTION_AMOUNT),
                    )
                }

                if (state.isRotatingPayout) {
                    Spacer(Modifier.height(sp.sm))
                    GroupCreateDropdownField(
                        label = stringResource(Res.string.screens_group_create_field_payout_order_label),
                        selectedOption = state.payoutOrderMethod,
                        options = PAYOUT_ORDER_OPTIONS,
                        onOptionSelected = { onAction(GroupCreateAction.OnPayoutOrderChange(it)) },
                        errorMessage = state.validationErrors["payoutOrderMethod"]?.let {
                            groupCreateValidationMessage(it)
                        },
                        contentDescription = payoutOrderCd,
                        modifier = Modifier.testTag(GroupCreateTestTags.DROPDOWN_PAYOUT_ORDER),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = sp.md))
                Text(
                    text = stringResource(Res.string.screens_group_create_section_loan_cycle_rules),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(sp.sm))
                GroupCreateTextField(
                    value = state.loanMultiplier,
                    label = stringResource(Res.string.screens_group_create_field_loan_multiplier_label),
                    onValueChange = { onAction(GroupCreateAction.OnLoanMultiplierChange(it)) },
                    keyboardType = KeyboardType.Number,
                    errorMessage = state.validationErrors["loanMultiplier"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = loanMultiplierCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.FIELD_LOAN_MULTIPLIER),
                )
                Spacer(Modifier.height(sp.sm))
                GroupCreateTextField(
                    value = state.interestRate,
                    label = stringResource(Res.string.screens_group_create_field_interest_rate_label),
                    onValueChange = { onAction(GroupCreateAction.OnInterestRateChange(it)) },
                    keyboardType = KeyboardType.Number,
                    errorMessage = state.validationErrors["interestRate"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = interestRateCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.FIELD_INTEREST_RATE),
                )
                Spacer(Modifier.height(sp.sm))
                GroupCreateTextField(
                    value = state.cycleLengthMonths,
                    label = stringResource(Res.string.screens_group_create_field_cycle_length_label),
                    onValueChange = { onAction(GroupCreateAction.OnCycleLengthChange(it)) },
                    keyboardType = KeyboardType.Number,
                    errorMessage = state.validationErrors["cycleLengthMonths"]?.let {
                        groupCreateValidationMessage(it)
                    },
                    contentDescription = cycleLengthCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.FIELD_CYCLE_LENGTH),
                )
                Spacer(Modifier.height(sp.sm))
                GroupCreateTextField(
                    value = state.fineAmount,
                    label = stringResource(Res.string.screens_group_create_field_fine_amount_label),
                    onValueChange = { onAction(GroupCreateAction.OnFineAmountChange(it)) },
                    keyboardType = KeyboardType.Number,
                    errorMessage = state.validationErrors["fineAmount"]?.let { groupCreateValidationMessage(it) },
                    contentDescription = fineAmountCd,
                    modifier = Modifier.testTag(GroupCreateTestTags.FIELD_FINE_AMOUNT),
                )
            }
        }
        Spacer(Modifier.height(sp.lg))
        AppCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = sp.touchTargetMin),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.screens_group_create_field_social_fund_toggle_label),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.socialFundEnabled,
                        onCheckedChange = { onAction(GroupCreateAction.OnSocialFundToggle(it)) },
                        modifier = Modifier
                            .testTag(GroupCreateTestTags.SWITCH_SOCIAL_FUND)
                            .semantics { contentDescription = socialFundToggleCd },
                    )
                }
                if (state.socialFundEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = sp.md))
                    GroupCreateTextField(
                        value = state.socialFundPercent,
                        label = stringResource(Res.string.screens_group_create_field_social_fund_percent_label),
                        onValueChange = { onAction(GroupCreateAction.OnSocialFundPercentChange(it)) },
                        keyboardType = KeyboardType.Number,
                        helperText = stringResource(Res.string.screens_group_create_field_social_fund_percent_help),
                        errorMessage = state.validationErrors["socialFundPercent"]?.let {
                            groupCreateValidationMessage(it)
                        },
                        contentDescription = socialFundPercentCd,
                        modifier = Modifier.testTag(GroupCreateTestTags.FIELD_SOCIAL_FUND_PERCENT),
                    )
                }
            }
        }
        Spacer(Modifier.height(sp.lg))
        KptButton(
            onClick = { onAction(GroupCreateAction.OnNextStep) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.NEXT_BUTTON)
                .semantics { contentDescription = nextCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_next))
        }
        Spacer(Modifier.height(sp.sm))
        KptOutlinedButton(
            onClick = { onAction(GroupCreateAction.OnPreviousStep) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.BACK_BUTTON)
                .semantics { contentDescription = backCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_back))
        }
    }
}

/** Step 3 — Members (`ui.yaml#forms.group_members_form`). See API.md#screen. */
@Composable
internal fun GroupMembersStepSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val maxMembersCd = stringResource(Res.string.screens_group_create_field_max_members_cd)
    val backCd = stringResource(Res.string.screens_group_create_action_back_cd)
    val nextCd = stringResource(Res.string.screens_group_create_action_next_cd)

    Column(modifier = modifier) {
        AppCard {
            GroupCreateTextField(
                value = state.maxMembers,
                label = stringResource(Res.string.screens_group_create_field_max_members_label),
                onValueChange = { onAction(GroupCreateAction.OnMaxMembersChange(it)) },
                keyboardType = KeyboardType.Number,
                helperText = stringResource(Res.string.screens_group_create_field_max_members_help),
                errorMessage = state.validationErrors["maxMembers"]?.let { groupCreateValidationMessage(it) },
                contentDescription = maxMembersCd,
                modifier = Modifier.testTag(GroupCreateTestTags.FIELD_MAX_MEMBERS),
            )
        }
        Spacer(Modifier.height(sp.lg))
        KptButton(
            onClick = { onAction(GroupCreateAction.OnNextStep) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.NEXT_BUTTON)
                .semantics { contentDescription = nextCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_next))
        }
        Spacer(Modifier.height(sp.sm))
        KptOutlinedButton(
            onClick = { onAction(GroupCreateAction.OnPreviousStep) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.BACK_BUTTON)
                .semantics { contentDescription = backCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_back))
        }
    }
}

/**
 * Step 4 — Review + Submit (`ui.yaml#components.review_card` / `submit_button`). Also embedded
 * (with `isSubmitting = true`) by [GroupCreateSubmittingSection]. See API.md#screen.
 */
@Composable
internal fun GroupReviewStepSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val backCd = stringResource(Res.string.screens_group_create_action_back_cd)
    val submitCd = stringResource(Res.string.screens_group_create_action_submit_cd)
    val submittingCd = stringResource(Res.string.screens_group_create_action_submitting_cd)

    Column(modifier = modifier) {
        AppCard(modifier = Modifier.testTag(GroupCreateTestTags.REVIEW_CARD)) {
            GroupCreateReviewCardBody(state = state)
        }

        if (state.isOffline) {
            Spacer(Modifier.height(sp.sm))
            OfflineNoticeBanner(
                message = stringResource(Res.string.screens_group_create_offline_notice),
                iconContentDescription = stringResource(Res.string.screens_group_create_offline_notice_cd),
                modifier = Modifier.testTag(GroupCreateTestTags.OFFLINE_NOTICE_BANNER),
            )
        }

        Spacer(Modifier.height(sp.lg))
        KptButton(
            onClick = { onAction(GroupCreateAction.OnSubmit) },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.SUBMIT_BUTTON)
                .semantics { contentDescription = if (isSubmitting) submittingCd else submitCd },
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(sp.sm))
                Text(stringResource(Res.string.screens_group_create_action_submitting))
            } else {
                Text(stringResource(Res.string.screens_group_create_action_submit))
            }
        }
        Spacer(Modifier.height(sp.sm))
        KptOutlinedButton(
            onClick = { onAction(GroupCreateAction.OnPreviousStep) },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.BACK_BUTTON)
                .semantics { contentDescription = backCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_back))
        }
    }
}

/**
 * `GroupCreateScreenState.Submitting` — `ui.yaml#states.submitting`. Wraps the type banner + step
 * indicator (both frozen on Step 4) + [GroupReviewStepSection] with `isSubmitting = true`
 * (spinner in place of the "Create Group" label, both buttons disabled). See API.md#screen.
 */
@Composable
internal fun GroupCreateSubmittingSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val typeBannerCd = stringResource(Res.string.screens_group_create_type_banner_cd, state.groupTypeName)
    val stepIndicatorCd = stringResource(Res.string.screens_group_create_step_indicator_cd)
    val stepLabels = listOf(
        stringResource(Res.string.screens_group_create_step_identity),
        stringResource(Res.string.screens_group_create_step_rules),
        stringResource(Res.string.screens_group_create_step_members),
        stringResource(Res.string.screens_group_create_step_review),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(Modifier.height(sp.md))
        GroupTypeBanner(
            groupTypeName = state.groupTypeName,
            contentDescription = typeBannerCd,
            modifier = Modifier.testTag(GroupCreateTestTags.TYPE_BANNER),
        )
        Spacer(Modifier.height(sp.md))
        WizardStepIndicator(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps,
            labels = stepLabels,
            contentDescription = stepIndicatorCd,
            modifier = Modifier.fillMaxWidth().testTag(GroupCreateTestTags.STEP_INDICATOR),
        )
        Spacer(Modifier.height(sp.lg))
        GroupReviewStepSection(state = state, onAction = onAction, isSubmitting = true)
        Spacer(Modifier.height(sp.xxl))
    }
}

/**
 * `GroupCreateScreenState.Error` — `ui.yaml#states.error`. Renders the type banner + step
 * indicator + inline error banner (+ offline banner when `isOffline`) + the review card + a
 * "Retry" primary button (re-dispatches `OnSubmit`) + Back. See API.md#screen.
 */
@Composable
internal fun GroupCreateErrorSection(
    state: GroupCreateState,
    onAction: (GroupCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val typeBannerCd = stringResource(Res.string.screens_group_create_type_banner_cd, state.groupTypeName)
    val stepIndicatorCd = stringResource(Res.string.screens_group_create_step_indicator_cd)
    val stepLabels = listOf(
        stringResource(Res.string.screens_group_create_step_identity),
        stringResource(Res.string.screens_group_create_step_rules),
        stringResource(Res.string.screens_group_create_step_members),
        stringResource(Res.string.screens_group_create_step_review),
    )
    val backCd = stringResource(Res.string.screens_group_create_action_back_cd)
    val retryCd = stringResource(Res.string.screens_group_create_action_retry_cd)
    val errorIconCd = stringResource(Res.string.screens_group_create_error_icon_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(Modifier.height(sp.md))
        GroupTypeBanner(
            groupTypeName = state.groupTypeName,
            contentDescription = typeBannerCd,
            modifier = Modifier.testTag(GroupCreateTestTags.TYPE_BANNER),
        )
        Spacer(Modifier.height(sp.md))
        WizardStepIndicator(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps,
            labels = stepLabels,
            contentDescription = stepIndicatorCd,
            modifier = Modifier.fillMaxWidth().testTag(GroupCreateTestTags.STEP_INDICATOR),
        )
        Spacer(Modifier.height(sp.lg))

        state.error?.let { error ->
            GroupCreateErrorBanner(
                title = stringResource(Res.string.screens_group_create_error_title),
                message = error.resolvedMessage(),
                iconContentDescription = errorIconCd,
                modifier = Modifier.testTag(GroupCreateTestTags.ERROR_BANNER),
            )
            Spacer(Modifier.height(sp.sm))
        }
        if (state.isOffline) {
            OfflineNoticeBanner(
                message = stringResource(Res.string.screens_group_create_offline_notice),
                iconContentDescription = stringResource(Res.string.screens_group_create_offline_notice_cd),
                modifier = Modifier.testTag(GroupCreateTestTags.OFFLINE_NOTICE_BANNER),
            )
            Spacer(Modifier.height(sp.lg))
        }

        AppCard(modifier = Modifier.testTag(GroupCreateTestTags.REVIEW_CARD)) {
            GroupCreateReviewCardBody(state = state)
        }

        Spacer(Modifier.height(sp.lg))
        KptButton(
            onClick = { onAction(GroupCreateAction.OnSubmit) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.SUBMIT_BUTTON)
                .semantics { contentDescription = retryCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_retry))
        }
        Spacer(Modifier.height(sp.sm))
        KptOutlinedButton(
            onClick = { onAction(GroupCreateAction.OnPreviousStep) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(GroupCreateTestTags.BACK_BUTTON)
                .semantics { contentDescription = backCd },
        ) {
            Text(stringResource(Res.string.screens_group_create_action_back))
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/**
 * `GroupCreateScreenState.Success` — `ui.yaml#states.success` ("Navigated to group-dashboard
 * after COMP-GRP-001 returns groupId" — a transient frame; `NavigateToGroupDashboard` fires the
 * same frame this renders, so there is deliberately NO interactive CTA here — a tappable "Go to
 * Dashboard" button would either be dead (nav already in flight) or risk a duplicate submission
 * if wired to re-dispatch `OnSubmit`. See API.md#screen.
 */
@Composable
internal fun GroupCreateSuccessSection(state: GroupCreateState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val successIconCd = stringResource(Res.string.screens_group_create_success_icon_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg)
            .testTag(GroupCreateTestTags.SUCCESS_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(sp.xxl))
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = successIconCd,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(sp.lg))
        Text(
            text = stringResource(Res.string.screens_group_create_success_title, state.groupName),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(sp.sm))
        Text(
            text = stringResource(Res.string.screens_group_create_success_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        state.inviteCode?.let { code ->
            Spacer(Modifier.height(sp.lg))
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = sp.md, vertical = sp.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
            ) {
                Text(
                    text = stringResource(Res.string.screens_group_create_success_invite_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // inviteCode is a bound state value (server-assigned code), not translatable UI copy.
                Text(text = code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(sp.lg))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(Res.string.screens_group_create_success_redirecting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/**
 * Shared review-card body (title + 3 [ReviewSection] blocks) rendered by both
 * [GroupReviewStepSection] and [GroupCreateErrorSection] — factored out to avoid duplicating the
 * `ui.yaml#components.review_card.content` assembly logic twice. See API.md#screen.
 */
@Composable
internal fun GroupCreateReviewCardBody(state: GroupCreateState) {
    val sp = MaterialTheme.spacing
    Column {
        Text(text = stringResource(Res.string.screens_group_create_review_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(sp.md))
        ReviewSection(
            label = stringResource(Res.string.screens_group_create_section_identity),
            rows = identityReviewRows(state),
            accessibilityLabel = stringResource(Res.string.screens_group_create_section_identity_cd),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = sp.md))
        ReviewSection(
            label = stringResource(Res.string.screens_group_create_section_rules),
            rows = rulesReviewRows(state),
            accessibilityLabel = stringResource(Res.string.screens_group_create_section_rules_cd),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = sp.md))
        ReviewSection(
            label = stringResource(Res.string.screens_group_create_section_members),
            rows = membersReviewRows(state),
            accessibilityLabel = stringResource(Res.string.screens_group_create_section_members_cd),
        )
    }
}

@Composable
private fun identityReviewRows(state: GroupCreateState): List<Pair<String, String>> = listOf(
    stringResource(Res.string.screens_group_create_row_group_type) to state.groupTypeName,
    stringResource(Res.string.screens_group_create_row_name) to state.groupName,
    stringResource(Res.string.screens_group_create_row_office) to state.officeName,
    stringResource(Res.string.screens_group_create_row_currency) to state.currency,
    stringResource(Res.string.screens_group_create_row_meeting) to
        stringResource(Res.string.screens_group_create_row_meeting_value, state.meetingDay, state.meetingTime),
)

/**
 * Rules review rows. Deliberately resolves currency dynamically from [GroupCreateState.currency]
 * for the `share_value`/`contribution`/`late_fine` rows rather than `ui.yaml#components
 * .review_card.content.review_rules_section`'s hardcoded `"KES {{value}}"` template literal — the
 * Step 1 currency dropdown lets the user pick USD/UGX/TZS, so a hardcoded "KES" would misreport
 * the actual selection. Flagged as a resolved idea-layer template gap in the generation report.
 */
@Composable
private fun rulesReviewRows(state: GroupCreateState): List<Pair<String, String>> = buildList {
    if (state.isShareBasedContribution) {
        add(
            stringResource(Res.string.screens_group_create_row_share_value) to
                stringResource(Res.string.screens_group_create_row_share_value_value, state.currency, state.shareValue),
        )
        add(
            stringResource(Res.string.screens_group_create_row_shares_per_meeting) to
                stringResource(
                    Res.string.screens_group_create_row_shares_per_meeting_value,
                    state.shareMin,
                    state.shareMax,
                ),
        )
    } else {
        add(
            stringResource(Res.string.screens_group_create_row_contribution) to
                stringResource(
                    Res.string.screens_group_create_row_contribution_value,
                    state.currency,
                    state.contributionAmount,
                ),
        )
    }
    if (state.isRotatingPayout) {
        // payoutOrderMethod is a bound enum value (e.g. "LOTTERY"), not translatable UI copy —
        // mirrors `ui.yaml#components.review_card...row_payout_order.value_i18n: skip`.
        add(stringResource(Res.string.screens_group_create_row_payout_order) to state.payoutOrderMethod)
    }
    add(
        stringResource(Res.string.screens_group_create_row_loan_multiplier) to
            stringResource(Res.string.screens_group_create_row_loan_multiplier_value, state.loanMultiplier),
    )
    add(
        stringResource(Res.string.screens_group_create_row_interest_rate) to
            stringResource(Res.string.screens_group_create_row_interest_rate_value, state.interestRate),
    )
    add(
        stringResource(Res.string.screens_group_create_row_cycle_length) to
            stringResource(Res.string.screens_group_create_row_cycle_length_value, state.cycleLengthMonths),
    )
    add(
        stringResource(Res.string.screens_group_create_row_late_fine) to
            stringResource(Res.string.screens_group_create_row_late_fine_value, state.currency, state.fineAmount),
    )
    if (state.socialFundEnabled) {
        add(
            stringResource(Res.string.screens_group_create_row_social_fund) to
                stringResource(Res.string.screens_group_create_row_social_fund_value, state.socialFundPercent),
        )
    }
}

@Composable
private fun membersReviewRows(state: GroupCreateState): List<Pair<String, String>> = listOf(
    stringResource(Res.string.screens_group_create_row_max_members) to state.maxMembers,
)

/**
 * `GroupCreateEvent.ShowOfflineSyncDialog` modal — acknowledges the offline queue, no further
 * ViewModel action needed (the submit request is already enqueued client-side; see
 * `GroupCreateViewModel.handleSubmit` KDoc). See API.md#screen.
 */
@Composable
internal fun OfflineSyncDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(GroupCreateTestTags.OFFLINE_SYNC_DIALOG),
        title = { Text(stringResource(Res.string.screens_group_create_offline_dialog_title)) },
        text = { Text(stringResource(Res.string.screens_group_create_offline_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = MaterialTheme.spacing.touchTargetMin)
                    .testTag(GroupCreateTestTags.OFFLINE_SYNC_DIALOG_CONFIRM),
            ) {
                Text(stringResource(Res.string.screens_group_create_offline_dialog_confirm))
            }
        },
    )
}

/** Maps [GroupCreateError.messageKey] to its localized message. */
@Composable
private fun GroupCreateError.resolvedMessage(): String = when (this) {
    GroupCreateError.Validation -> stringResource(Res.string.screens_group_create_error_validation)
    GroupCreateError.Network -> stringResource(Res.string.screens_group_create_error_network)
    GroupCreateError.Server -> stringResource(Res.string.screens_group_create_error_server)
    GroupCreateError.Auth -> stringResource(Res.string.screens_group_create_error_auth)
}

/**
 * Maps a `GroupCreateState.validationErrors` value (an `error_key` string emitted by
 * [GroupCreateViewModel]'s `validateIdentityStep`/`validateRulesStep`/`validateMembersStep`) to
 * its localized message.
 */
@Composable
private fun groupCreateValidationMessage(errorKey: String): String = when (errorKey) {
    "error_group_name_invalid" -> stringResource(Res.string.screens_group_create_validation_group_name_invalid)
    "error_office_required" -> stringResource(Res.string.screens_group_create_validation_office_required)
    "error_meeting_day_required" -> stringResource(Res.string.screens_group_create_validation_meeting_day_required)
    "error_meeting_time_required" -> stringResource(Res.string.screens_group_create_validation_meeting_time_required)
    "error_share_value_invalid" -> stringResource(Res.string.screens_group_create_validation_share_value_invalid)
    "error_share_min_invalid" -> stringResource(Res.string.screens_group_create_validation_share_min_invalid)
    "error_share_max_invalid" -> stringResource(Res.string.screens_group_create_validation_share_max_invalid)
    "error_contribution_amount_invalid" ->
        stringResource(Res.string.screens_group_create_validation_contribution_amount_invalid)
    "error_payout_order_required" -> stringResource(Res.string.screens_group_create_validation_payout_order_required)
    "error_loan_multiplier_invalid" -> stringResource(Res.string.screens_group_create_validation_loan_multiplier_invalid)
    "error_interest_rate_invalid" -> stringResource(Res.string.screens_group_create_validation_interest_rate_invalid)
    "error_cycle_length_invalid" -> stringResource(Res.string.screens_group_create_validation_cycle_length_invalid)
    "error_fine_amount_invalid" -> stringResource(Res.string.screens_group_create_validation_fine_amount_invalid)
    "error_social_fund_percent_invalid" ->
        stringResource(Res.string.screens_group_create_validation_social_fund_percent_invalid)
    "error_max_members_invalid" -> stringResource(Res.string.screens_group_create_validation_max_members_invalid)
    else -> stringResource(Res.string.screens_group_create_error_validation)
}
