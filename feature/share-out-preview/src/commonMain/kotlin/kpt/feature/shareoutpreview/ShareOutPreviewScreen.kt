/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.shareoutpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.common.formatDecimal
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.GroupTypeConfig
import kpt.core.model.MemberPayout
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import kpt.feature.shareoutpreview.generated.resources.Res
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_a11y_confirm_button
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_a11y_error_state
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_a11y_formula_chip
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_a11y_payout_table
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_a11y_refresh_action
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_a11y_rotation_card
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_amount_kes_format
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_col_member
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_col_payout
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_col_savings
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_col_share
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_col_shares
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_confirm_button
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_cycle_label_format
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_error_auth
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_error_insufficient_data
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_error_network
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_error_server
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_error_title
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_auction
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_equal
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_fixed_order
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_lottery
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_prorata_savings
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_prorata_shares
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_formula_unknown
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_next_recipient_label
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_payout_amount_label
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_preview_disclaimer
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_retry_button
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_screen_title
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_share_percent_format
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_total_corpus
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_total_pool
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_total_profit
import kpt.feature.shareoutpreview.generated.resources.screens_share_out_preview_your_position_label_format
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SHIMMER_COUNT = 4
private val SHIMMER_HEIGHT = 80.dp

/**
 * Container for `share-out-preview-screen`. Collects [ShareOutPreviewViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [ShareOutPreviewEvent]s (navigate to execute /
 * navigate back / show a snackbar) through [EventsEffect], and delegates all rendering to the
 * stateless [ShareOutPreviewContent]. The on-mount load is fired from the ViewModel's `init` (no
 * matching declared user action). [groupId]/[typeConfig] are the `ui.yaml#nav_params` supplied to
 * the ViewModel via Koin `parametersOf(groupId, typeConfig)`. See API.md#screen.
 */
@Composable
internal fun ShareOutPreviewScreen(
    groupId: String,
    typeConfig: GroupTypeConfig,
    onNavigateToShareOutExecute: (
        groupId: String,
        typeConfig: GroupTypeConfig,
        totalPool: Double,
        memberPayouts: List<MemberPayout>,
        cycleNumber: Int,
    ) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareOutPreviewViewModel = koinViewModel(
        parameters = { parametersOf(groupId, typeConfig) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and the EventsEffect
    // callback runs in a suspend (non-composable) scope.
    val networkMessage = stringResource(Res.string.screens_share_out_preview_error_network)
    val serverMessage = stringResource(Res.string.screens_share_out_preview_error_server)
    val insufficientMessage = stringResource(Res.string.screens_share_out_preview_error_insufficient_data)
    val authMessage = stringResource(Res.string.screens_share_out_preview_error_auth)

    EventsEffect(viewModel) { event ->
        when (event) {
            is ShareOutPreviewEvent.NavigateToShareOutExecute ->
                onNavigateToShareOutExecute(event.groupId, event.typeConfig, event.totalPool, event.memberPayouts, event.cycleNumber)

            ShareOutPreviewEvent.NavigateBack -> onNavigateBack()

            is ShareOutPreviewEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    ShareOutPreviewError.Network.messageKey -> networkMessage
                    ShareOutPreviewError.Server.messageKey -> serverMessage
                    ShareOutPreviewError.InsufficientData.messageKey -> insufficientMessage
                    ShareOutPreviewError.Auth.messageKey -> authMessage
                    else -> event.message
                },
            )
        }
    }

    ShareOutPreviewContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `share-out-preview-screen`. State-driven per
 * [ShareOutPreviewState.deriveScreenState] — every [ShareOutPreviewScreenState] member is handled
 * (Loading / ContentAccumulating / ContentRotating / Error). The top-bar refresh action, pull-to-
 * refresh, and the error-state retry all dispatch the real declared actions
 * ([ShareOutPreviewAction.OnRefresh] / [ShareOutPreviewAction.Retry]); confirm dispatches
 * [ShareOutPreviewAction.OnConfirm]; back dispatches [ShareOutPreviewAction.OnBack] — no free
 * callbacks threaded through (RULE-IMPL-DEAD-CLICKABLE-001 Rule 3). See API.md#screen.
 */
@Composable
internal fun ShareOutPreviewContent(
    state: ShareOutPreviewState,
    onAction: (ShareOutPreviewAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_share_out_preview_screen_title)
    val refreshCd = stringResource(Res.string.screens_share_out_preview_a11y_refresh_action)
    val screenState = state.deriveScreenState()
    val isContent = screenState == ShareOutPreviewScreenState.ContentAccumulating ||
        screenState == ShareOutPreviewScreenState.ContentRotating

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = { onAction(ShareOutPreviewAction.OnBack) },
        title = title,
        actions = listOf(
            TopAppBarAction(
                icon = Icons.Filled.Refresh,
                contentDescription = refreshCd,
                onClick = { onAction(ShareOutPreviewAction.OnRefresh) },
            ),
        ),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = isContent,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(ShareOutPreviewAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(ShareOutPreviewTestTags.SCREEN),
    ) {
        when (screenState) {
            ShareOutPreviewScreenState.Loading -> ShareOutPreviewLoadingSection()
            ShareOutPreviewScreenState.ContentAccumulating ->
                ShareOutPreviewAccumulatingSection(state = state, onAction = onAction)
            ShareOutPreviewScreenState.ContentRotating ->
                ShareOutPreviewRotatingSection(state = state, onAction = onAction)
            ShareOutPreviewScreenState.Error -> ShareOutPreviewErrorSection(state = state, onAction = onAction)
        }
    }
}

// -- Loading (ui.yaml#components.shimmer_detail: 4 shimmer blocks, 80dp) --------------------------

@Composable
private fun ShareOutPreviewLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(ShareOutPreviewTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        repeat(SHIMMER_COUNT) { ShimmerBlock(height = SHIMMER_HEIGHT) }
    }
}

@Composable
private fun ShimmerBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

// -- ACCUMULATING content (banner + fund card + formula chip + payout table + confirm) -----------

@Composable
private fun ShareOutPreviewAccumulatingSection(
    state: ShareOutPreviewState,
    onAction: (ShareOutPreviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val tableCd = stringResource(Res.string.screens_share_out_preview_a11y_payout_table)
    val isShares = state.shareoutFormula == "PRORATA_SHARES"
    val maxPayout = state.memberPayouts.maxOfOrNull { it.payoutAmount } ?: 0.0

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            item(key = "cycle_banner") { CycleInfoBanner(cycleNumber = state.cycleNumber) }
            item(key = "fund_summary") { FundSummaryCard(state = state) }
            item(key = "formula_chip") { FormulaChip(shareoutFormula = state.shareoutFormula) }
            item(key = "table_header") {
                PayoutTableHeader(
                    isShares = isShares,
                    modifier = Modifier.testTag(ShareOutPreviewTestTags.MEMBER_PAYOUT_TABLE)
                        .semantics { contentDescription = tableCd },
                )
            }
            items(items = state.memberPayouts, key = { it.memberId }) { payout ->
                PayoutRow(
                    payout = payout,
                    isShares = isShares,
                    isHighlighted = payout.payoutAmount >= maxPayout && maxPayout > 0.0,
                )
            }
        }
        ConfirmButton(
            enabled = state.memberPayouts.isNotEmpty(),
            onConfirm = { onAction(ShareOutPreviewAction.OnConfirm) },
        )
    }
}

// -- ROTATING_PAYOUT content (banner + fund card + formula chip + rotation card + confirm) -------

@Composable
private fun ShareOutPreviewRotatingSection(
    state: ShareOutPreviewState,
    onAction: (ShareOutPreviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            CycleInfoBanner(cycleNumber = state.cycleNumber)
            FundSummaryCard(state = state)
            FormulaChip(shareoutFormula = state.shareoutFormula)
            RotationPreviewCard(state = state)
        }
        ConfirmButton(
            enabled = true,
            onConfirm = { onAction(ShareOutPreviewAction.OnConfirm) },
        )
    }
}

// -- Shared content composables ------------------------------------------------------------------

@Composable
private fun CycleInfoBanner(cycleNumber: Int, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(ShareOutPreviewTestTags.CYCLE_INFO_BANNER),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(
                text = stringResource(Res.string.screens_share_out_preview_cycle_label_format, cycleNumber),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(Res.string.screens_share_out_preview_preview_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun FundSummaryCard(state: ShareOutPreviewState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().testTag(ShareOutPreviewTestTags.FUND_SUMMARY_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            LabeledValue(
                label = stringResource(Res.string.screens_share_out_preview_total_corpus),
                value = amountKes(state.totalCorpus),
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
            LabeledValue(
                label = stringResource(Res.string.screens_share_out_preview_total_profit),
                value = amountKes(state.totalProfit),
                valueColor = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).padding(vertical = sp.xs)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.screens_share_out_preview_total_pool),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = amountKes(state.totalPool),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FormulaChip(shareoutFormula: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val chipCd = stringResource(Res.string.screens_share_out_preview_a11y_formula_chip)
    Text(
        text = formulaLabel(shareoutFormula),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = sp.md, vertical = sp.xs)
            .semantics { contentDescription = chipCd }
            .testTag(ShareOutPreviewTestTags.FORMULA_CHIP),
    )
}

@Composable
private fun formulaLabel(shareoutFormula: String): String = when (shareoutFormula) {
    "PRORATA_SHARES" -> stringResource(Res.string.screens_share_out_preview_formula_prorata_shares)
    "PRORATA_SAVINGS" -> stringResource(Res.string.screens_share_out_preview_formula_prorata_savings)
    "EQUAL" -> stringResource(Res.string.screens_share_out_preview_formula_equal)
    "FIXED_ORDER" -> stringResource(Res.string.screens_share_out_preview_formula_fixed_order)
    "LOTTERY" -> stringResource(Res.string.screens_share_out_preview_formula_lottery)
    "AUCTION" -> stringResource(Res.string.screens_share_out_preview_formula_auction)
    else -> stringResource(Res.string.screens_share_out_preview_formula_unknown)
}

@Composable
private fun PayoutTableHeader(isShares: Boolean, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val secondCol = if (isShares) {
        stringResource(Res.string.screens_share_out_preview_col_shares)
    } else {
        stringResource(Res.string.screens_share_out_preview_col_savings)
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = sp.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.screens_share_out_preview_col_member),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = secondCol,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(104.dp),
        )
        Text(
            text = stringResource(Res.string.screens_share_out_preview_col_share),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = stringResource(Res.string.screens_share_out_preview_col_payout),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(104.dp),
        )
    }
}

@Composable
private fun PayoutRow(
    payout: MemberPayout,
    isShares: Boolean,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val background = if (isHighlighted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
    val textColor = if (isHighlighted) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
    val secondValue = if (isShares) {
        (payout.sharesHeld ?: 0).toString()
    } else {
        (payout.totalSavings ?: 0.0).formatGrouped(0)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = sp.sm, vertical = sp.sm)
            .testTag(ShareOutPreviewTestTags.memberPayoutRowTag(payout.memberId)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = payout.memberName,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = secondValue,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(104.dp),
        )
        Text(
            text = stringResource(Res.string.screens_share_out_preview_share_percent_format, payout.sharePercent.formatDecimal(1)),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = payout.payoutAmount.formatGrouped(0),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(104.dp),
        )
    }
}

@Composable
private fun RotationPreviewCard(state: ShareOutPreviewState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val cardCd = stringResource(Res.string.screens_share_out_preview_a11y_rotation_card)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
            .semantics { contentDescription = cardCd }
            .testTag(ShareOutPreviewTestTags.ROTATION_PREVIEW_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(
                text = stringResource(Res.string.screens_share_out_preview_next_recipient_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = state.nextRecipientName.orEmpty(),
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
                    text = stringResource(Res.string.screens_share_out_preview_payout_amount_label),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = amountKes(state.nextRecipientAmount ?: 0.0),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)),
            )
            Text(
                text = stringResource(
                    Res.string.screens_share_out_preview_your_position_label_format,
                    state.rotationPosition ?: 0,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ConfirmButton(enabled: Boolean, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val confirmCd = stringResource(Res.string.screens_share_out_preview_a11y_confirm_button)
    Button(
        onClick = onConfirm,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(sp.lg)
            .semantics { contentDescription = confirmCd }
            .testTag(ShareOutPreviewTestTags.CONFIRM_BUTTON),
    ) {
        Text(text = stringResource(Res.string.screens_share_out_preview_confirm_button))
    }
}

// -- Error (ui.yaml#components.error_state) -------------------------------------------------------

@Composable
private fun ShareOutPreviewErrorSection(
    state: ShareOutPreviewState,
    onAction: (ShareOutPreviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorCd = stringResource(Res.string.screens_share_out_preview_a11y_error_state)
    val message = when (state.error) {
        ShareOutPreviewError.Network -> stringResource(Res.string.screens_share_out_preview_error_network)
        ShareOutPreviewError.Auth -> stringResource(Res.string.screens_share_out_preview_error_auth)
        ShareOutPreviewError.InsufficientData -> stringResource(Res.string.screens_share_out_preview_error_insufficient_data)
        else -> stringResource(Res.string.screens_share_out_preview_error_server)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg)
            .semantics { contentDescription = errorCd }
            .testTag(ShareOutPreviewTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(Res.string.screens_share_out_preview_error_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = sp.sm),
        )
        if (canRetry) {
            Button(
                onClick = { onAction(ShareOutPreviewAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(top = sp.lg)
                    .testTag(ShareOutPreviewTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(
                    text = stringResource(Res.string.screens_share_out_preview_retry_button),
                    modifier = Modifier.padding(start = sp.xs),
                )
            }
        }
    }
}

@Composable
private fun amountKes(amount: Double): String =
    stringResource(Res.string.screens_share_out_preview_amount_kes_format, amount.formatGrouped(0))
