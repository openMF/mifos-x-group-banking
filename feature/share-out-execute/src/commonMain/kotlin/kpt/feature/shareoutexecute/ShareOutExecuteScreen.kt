/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.shareoutexecute

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.security.BiometricAuthenticator
import kpt.core.base.security.BiometricResult
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.GroupTypeConfig
import kpt.core.model.MemberExecutionStatus
import kpt.core.model.MemberPayout
import kpt.core.ui.scaffold.KptScaffold
import kpt.feature.shareoutexecute.generated.resources.Res
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_a11y_back
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_a11y_biometric_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_a11y_confirmation_field
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_a11y_done_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_a11y_execute_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_a11y_retry_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_amount_kes_format
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_biometric_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_biometric_reason
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_confirm_shareout_body
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_confirm_shareout_title
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_confirmation_field_error
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_confirmation_field_label
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_confirmation_field_placeholder
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_done_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_auth
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_biometric_failed
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_biometric_unavailable
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_confirmation_required
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_partial
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_queued_offline
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_error_server
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_execute_button_rotation
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_execute_button_shareout
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_header_member_payouts
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_header_rotation_payout
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_member_share_format
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_offline_info_banner
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_partial_body_format
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_partial_title
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_progress_format
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_queued_body_format
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_queued_title
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_retry_button
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_rotation_amount_label
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_rotation_paying_out_to
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_screen_title_rotation
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_screen_title_shareout
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_success_body_format
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_success_title_rotation
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_success_title_shareout
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_summary_members_to_receive
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_summary_one_recipient
import kpt.feature.shareoutexecute.generated.resources.screens_share_out_execute_total_pool_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Container for `share-out-execute-screen`. Collects [ShareOutExecuteViewModel] state, consumes
 * one-shot [ShareOutExecuteEvent]s, and delegates rendering to the stateless [ShareOutExecuteContent].
 * The [ShareOutExecuteEvent.ShowBiometricPrompt] event is handled HERE (not in the ViewModel) — the
 * injected [BiometricAuthenticator] presents the platform prompt and the result is routed back via
 * [ShareOutExecuteAction.Internal.BiometricResult] (keeps the ViewModel platform-free + unit-testable).
 * See API.md#screen.
 */
@Composable
internal fun ShareOutExecuteScreen(
    groupId: String,
    typeConfig: GroupTypeConfig,
    totalPool: Double,
    memberPayouts: List<MemberPayout>,
    cycleNumber: Int,
    onNavigateToGroupDashboard: (groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    biometricAuthenticator: BiometricAuthenticator = koinInject(),
    viewModel: ShareOutExecuteViewModel = koinViewModel(
        parameters = { parametersOf(groupId, typeConfig, totalPool, memberPayouts, cycleNumber) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val biometricReason = stringResource(Res.string.screens_share_out_execute_biometric_reason)
    val confirmationRequiredMsg = stringResource(Res.string.screens_share_out_execute_error_confirmation_required)
    val serverMsg = stringResource(Res.string.screens_share_out_execute_error_server)
    val authMsg = stringResource(Res.string.screens_share_out_execute_error_auth)
    val partialMsg = stringResource(Res.string.screens_share_out_execute_error_partial)
    val queuedOfflineMsg = stringResource(Res.string.screens_share_out_execute_error_queued_offline)
    val biometricUnavailableMsg = stringResource(Res.string.screens_share_out_execute_error_biometric_unavailable)
    val biometricFailedMsg = stringResource(Res.string.screens_share_out_execute_error_biometric_failed)

    EventsEffect(viewModel) { event ->
        when (event) {
            is ShareOutExecuteEvent.NavigateToGroupDashboard -> onNavigateToGroupDashboard(event.groupId)
            ShareOutExecuteEvent.NavigateBack -> onNavigateBack()
            ShareOutExecuteEvent.ShowBiometricPrompt -> {
                val outcome = if (!biometricAuthenticator.isAvailable()) {
                    BiometricOutcome.UNAVAILABLE
                } else {
                    when (biometricAuthenticator.authenticate(biometricReason)) {
                        BiometricResult.Success -> BiometricOutcome.SUCCESS
                        BiometricResult.Unavailable -> BiometricOutcome.UNAVAILABLE
                        is BiometricResult.Failure, BiometricResult.Cancelled -> BiometricOutcome.FAILED
                    }
                }
                viewModel.trySendAction(ShareOutExecuteAction.Internal.BiometricResult(outcome))
            }
            is ShareOutExecuteEvent.ShowSnackbar -> {
                val message = when (event.message) {
                    "error_confirmation_required" -> confirmationRequiredMsg
                    "error_server" -> serverMsg
                    "error_auth" -> authMsg
                    "error_partial" -> partialMsg
                    "error_queued_offline" -> queuedOfflineMsg
                    "error_biometric_unavailable" -> biometricUnavailableMsg
                    "error_biometric_failed" -> biometricFailedMsg
                    else -> event.message
                }
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }

    ShareOutExecuteContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `share-out-execute-screen`. Layout = `Column(TopAppBar +
 * ScrollableContent + StickyFooterButton)` (`ui.yaml#screens.layout`). The scrollable region carries
 * the summary card + payout list / rotation card + the state-specific banner/confirmation section;
 * the sticky footer renders exactly one primary action per [ShareOutExecuteScreenState]
 * (Execute / Retry / Done). Back nav is disabled while executing or after completion
 * (`ui.yaml#components.top_bar.nav_enabled_when`). See API.md#screen.
 */
@Composable
internal fun ShareOutExecuteContent(
    state: ShareOutExecuteState,
    onAction: (ShareOutExecuteAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = if (state.isRotationLabel) {
        stringResource(Res.string.screens_share_out_execute_screen_title_rotation)
    } else {
        stringResource(Res.string.screens_share_out_execute_screen_title_shareout)
    }
    val backCd = stringResource(Res.string.screens_share_out_execute_a11y_back)
    val backEnabled = !state.isExecuting && !state.isCompleted
    val screenState = state.deriveScreenState()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = { if (backEnabled) onAction(ShareOutExecuteAction.OnBack) },
        title = title,
        actions = listOf(
            TopAppBarAction(
                icon = Icons.Filled.CloudOff,
                contentDescription = backCd,
                onClick = { if (backEnabled) onAction(ShareOutExecuteAction.OnBack) },
                enabled = backEnabled,
            ),
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(ShareOutExecuteTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(MaterialTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                item(key = "summary") { SummaryCard(state = state) }

                if (!state.isOnline && screenState == ShareOutExecuteScreenState.Content) {
                    item(key = "offline_banner") { OfflineBanner() }
                }

                item(key = "payout_header") { PayoutListHeader(state = state) }

                if (state.isRotation) {
                    item(key = "rotation_card") { RotationExecuteCard(state = state) }
                } else {
                    items(items = state.memberPayouts, key = { it.memberId }) { payout ->
                        MemberPayoutRow(
                            payout = payout,
                            status = state.memberExecutionStatus[payout.memberId] ?: MemberExecutionStatus.PENDING,
                            showBadge = state.isExecuting || state.isCompleted || state.queuedOffline,
                        )
                    }
                }

                when (screenState) {
                    ShareOutExecuteScreenState.Content, ShareOutExecuteScreenState.Error ->
                        item(key = "confirmation") { ConfirmationSection(state = state, onAction = onAction) }
                    ShareOutExecuteScreenState.Executing ->
                        item(key = "progress") { ExecutionProgress(state = state) }
                    ShareOutExecuteScreenState.Success ->
                        item(key = "success_banner") { CompletionBanner(state = state) }
                    ShareOutExecuteScreenState.PartialFailure ->
                        item(key = "partial_banner") { PartialFailureBanner(state = state) }
                }
            }

            StickyFooter(state = state, screenState = screenState, onAction = onAction)
        }
    }
}

// -- Summary + list header -----------------------------------------------------------------------

@Composable
private fun SummaryCard(state: ShareOutExecuteState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val subtitle = if (state.isRotation) {
        stringResource(Res.string.screens_share_out_execute_summary_one_recipient)
    } else {
        "${state.totalCount} ${stringResource(Res.string.screens_share_out_execute_summary_members_to_receive)}"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(ShareOutExecuteTestTags.SUMMARY_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_share_out_execute_total_pool_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = amountKes(state.totalPool),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PayoutListHeader(state: ShareOutExecuteState, modifier: Modifier = Modifier) {
    val header = if (state.isRotation) {
        stringResource(Res.string.screens_share_out_execute_header_rotation_payout)
    } else {
        stringResource(Res.string.screens_share_out_execute_header_member_payouts)
    }
    Text(
        text = header,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth().testTag(ShareOutExecuteTestTags.OFFLINE_BANNER),
    ) {
        Text(
            text = stringResource(Res.string.screens_share_out_execute_offline_info_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(sp.md),
        )
    }
}

// -- Member payout row + status badge ------------------------------------------------------------

@Composable
private fun MemberPayoutRow(
    payout: MemberPayout,
    status: MemberExecutionStatus,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(ShareOutExecuteTestTags.memberPayoutRowTag(payout.memberId)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = payout.memberName.initials(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = payout.memberName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    Res.string.screens_share_out_execute_member_share_format,
                    payout.payoutAmount.formatGrouped(0),
                    (payout.sharePercent * 100).toInt().toString(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (showBadge) {
            StatusBadge(status = status)
        }
    }
}

@Composable
private fun StatusBadge(status: MemberExecutionStatus, modifier: Modifier = Modifier) {
    when (status) {
        MemberExecutionStatus.IN_PROGRESS -> CircularProgressIndicator(
            modifier = modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.secondary,
        )
        else -> {
            val (icon, tint) = status.badgeIconAndTint()
            Icon(imageVector = icon, contentDescription = status.name, tint = tint, modifier = modifier.size(24.dp))
        }
    }
}

@Composable
private fun MemberExecutionStatus.badgeIconAndTint(): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (this) {
    MemberExecutionStatus.PENDING -> Icons.Filled.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
    MemberExecutionStatus.IN_PROGRESS -> Icons.Filled.HourglassEmpty to MaterialTheme.colorScheme.secondary
    MemberExecutionStatus.DONE -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
    MemberExecutionStatus.FAILED -> Icons.Filled.Error to MaterialTheme.colorScheme.error
    MemberExecutionStatus.QUEUED -> Icons.Filled.CloudUpload to MaterialTheme.colorScheme.secondary
}

// -- Rotation single-recipient card --------------------------------------------------------------

@Composable
private fun RotationExecuteCard(state: ShareOutExecuteState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val recipient = state.memberPayouts.firstOrNull()?.memberName.orEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(ShareOutExecuteTestTags.ROTATION_EXECUTE_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_share_out_execute_rotation_paying_out_to),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = recipient,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.screens_share_out_execute_rotation_amount_label),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = amountKes(state.totalPool),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// -- Double-confirmation section (irreversible-action gate) --------------------------------------

@Composable
private fun ConfirmationSection(
    state: ShareOutExecuteState,
    onAction: (ShareOutExecuteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val fieldCd = stringResource(Res.string.screens_share_out_execute_a11y_confirmation_field)
    val biometricCd = stringResource(Res.string.screens_share_out_execute_a11y_biometric_button)
    val showFieldError = state.confirmationText.isNotBlank() && !state.isConfirmed

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(ShareOutExecuteTestTags.CONFIRMATION_SECTION),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_share_out_execute_confirm_shareout_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.screens_share_out_execute_confirm_shareout_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.confirmationText,
                onValueChange = { onAction(ShareOutExecuteAction.OnConfirmationTextChanged(it)) },
                label = { Text(stringResource(Res.string.screens_share_out_execute_confirmation_field_label)) },
                placeholder = { Text(stringResource(Res.string.screens_share_out_execute_confirmation_field_placeholder)) },
                isError = showFieldError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                supportingText = if (showFieldError) {
                    { Text(stringResource(Res.string.screens_share_out_execute_confirmation_field_error)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { contentDescription = fieldCd }
                    .testTag(ShareOutExecuteTestTags.CONFIRMATION_FIELD),
            )
            OutlinedButton(
                onClick = { onAction(ShareOutExecuteAction.OnBiometricSelected) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { contentDescription = biometricCd }
                    .testTag(ShareOutExecuteTestTags.BIOMETRIC_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null)
                Text(
                    text = stringResource(Res.string.screens_share_out_execute_biometric_button),
                    modifier = Modifier.padding(start = sp.xs),
                )
            }
        }
    }
}

// -- Executing / Success / PartialFailure / Queued banners ---------------------------------------

@Composable
private fun ExecutionProgress(state: ShareOutExecuteState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val fraction = if (state.totalCount > 0) state.executedCount.toFloat() / state.totalCount.toFloat() else 0f
    Column(
        modifier = modifier.fillMaxWidth().testTag(ShareOutExecuteTestTags.PROGRESS_INDICATOR),
        verticalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                Res.string.screens_share_out_execute_progress_format,
                state.executedCount,
                state.totalCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompletionBanner(state: ShareOutExecuteState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    if (state.queuedOffline) {
        BannerCard(
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Filled.CloudUpload,
            title = stringResource(Res.string.screens_share_out_execute_queued_title),
            body = stringResource(Res.string.screens_share_out_execute_queued_body_format, state.totalCount),
            testTag = ShareOutExecuteTestTags.QUEUED_BANNER,
            modifier = modifier,
        )
    } else {
        val title = if (state.isRotationLabel) {
            stringResource(Res.string.screens_share_out_execute_success_title_rotation)
        } else {
            stringResource(Res.string.screens_share_out_execute_success_title_shareout)
        }
        BannerCard(
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Filled.CheckCircle,
            title = title,
            body = stringResource(
                Res.string.screens_share_out_execute_success_body_format,
                state.totalPool.formatGrouped(0),
                state.succeededCount,
            ),
            testTag = ShareOutExecuteTestTags.COMPLETION_BANNER,
            modifier = modifier,
        )
    }
}

@Composable
private fun PartialFailureBanner(state: ShareOutExecuteState, modifier: Modifier = Modifier) {
    BannerCard(
        container = MaterialTheme.colorScheme.errorContainer,
        onContainer = MaterialTheme.colorScheme.onErrorContainer,
        icon = Icons.Filled.Warning,
        title = stringResource(Res.string.screens_share_out_execute_partial_title),
        body = stringResource(
            Res.string.screens_share_out_execute_partial_body_format,
            state.succeededCount,
            state.totalCount,
            state.failedPayouts.size,
        ),
        testTag = ShareOutExecuteTestTags.PARTIAL_FAILURE_BANNER,
        modifier = modifier,
    )
}

@Composable
private fun BannerCard(
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    title: String,
    body: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(testTag),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(sp.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(sp.xs),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(40.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = onContainer, fontWeight = FontWeight.Bold)
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = onContainer, textAlign = TextAlign.Center)
        }
    }
}

// -- Sticky footer primary action ----------------------------------------------------------------

@Composable
private fun StickyFooter(
    state: ShareOutExecuteState,
    screenState: ShareOutExecuteScreenState,
    onAction: (ShareOutExecuteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Surface(shadowElevation = 8.dp, modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(sp.lg)) {
            when (screenState) {
                ShareOutExecuteScreenState.Content, ShareOutExecuteScreenState.Error, ShareOutExecuteScreenState.Executing ->
                    ExecuteButton(state = state, onAction = onAction)
                ShareOutExecuteScreenState.PartialFailure -> RetryFailedButton(onAction = onAction)
                ShareOutExecuteScreenState.Success -> DoneButton(onAction = onAction)
            }
        }
    }
}

@Composable
private fun ExecuteButton(
    state: ShareOutExecuteState,
    onAction: (ShareOutExecuteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val executeCd = stringResource(Res.string.screens_share_out_execute_a11y_execute_button)
    val label = if (state.isRotationLabel) {
        stringResource(Res.string.screens_share_out_execute_execute_button_rotation)
    } else {
        stringResource(Res.string.screens_share_out_execute_execute_button_shareout)
    }
    Button(
        onClick = { onAction(ShareOutExecuteAction.OnExecute) },
        enabled = state.isConfirmed && !state.isExecuting,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { contentDescription = executeCd }
            .testTag(ShareOutExecuteTestTags.EXECUTE_BUTTON),
    ) {
        if (state.isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onError,
            )
        } else {
            Text(text = label)
        }
    }
}

@Composable
private fun RetryFailedButton(onAction: (ShareOutExecuteAction) -> Unit, modifier: Modifier = Modifier) {
    val retryCd = stringResource(Res.string.screens_share_out_execute_a11y_retry_button)
    Button(
        onClick = { onAction(ShareOutExecuteAction.OnRetryFailed) },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics { contentDescription = retryCd }
            .testTag(ShareOutExecuteTestTags.RETRY_FAILED_BUTTON),
    ) {
        Text(text = stringResource(Res.string.screens_share_out_execute_retry_button))
    }
}

@Composable
private fun DoneButton(onAction: (ShareOutExecuteAction) -> Unit, modifier: Modifier = Modifier) {
    val doneCd = stringResource(Res.string.screens_share_out_execute_a11y_done_button)
    Button(
        onClick = { onAction(ShareOutExecuteAction.OnDone) },
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { contentDescription = doneCd }
            .testTag(ShareOutExecuteTestTags.DONE_BUTTON),
    ) {
        Text(text = stringResource(Res.string.screens_share_out_execute_done_button))
    }
}

// -- helpers -------------------------------------------------------------------------------------

@Composable
private fun amountKes(amount: Double): String =
    stringResource(Res.string.screens_share_out_execute_amount_kes_format, amount.formatGrouped(0))

/** First-letter initials (max 2) derived from a member's display name for the row avatar. */
private fun String.initials(): String = trim().split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "?" }
