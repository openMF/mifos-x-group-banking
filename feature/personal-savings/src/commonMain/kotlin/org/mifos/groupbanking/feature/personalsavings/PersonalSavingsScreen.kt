/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalsavings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.SavingsLedgerEntry
import org.mifos.groupbanking.core.model.SavingsTab
import org.mifos.groupbanking.feature.personalsavings.components.SavingsBalanceHeroCard
import org.mifos.groupbanking.feature.personalsavings.components.SavingsContributionProgressCard
import org.mifos.groupbanking.feature.personalsavings.components.SavingsEmptyIndividualState
import org.mifos.groupbanking.feature.personalsavings.components.SavingsTabRow
import org.mifos.groupbanking.feature.personalsavings.components.SavingsTransactionRow
import org.mifos.groupbanking.feature.personalsavings.components.SavingsTransactionSkeletonRow
import org.mifos.groupbanking.feature.personalsavings.generated.resources.Res
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_action_retry
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_error_icon_cd
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_error_network_message
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_error_server_message
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_error_session_message
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_error_title
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_shimmer_cd
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_title
import org.mifos.groupbanking.feature.personalsavings.generated.resources.screens_personal_savings_transaction_history_label

/**
 * Container for `personal-savings-screen`. Collects [PersonalSavingsViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [PersonalSavingsEvent]s through [EventsEffect],
 * and delegates all rendering to the stateless [PersonalSavingsContent]. [clientId] /
 * [groupLinkedSavingsId] / [individualSavingsId] are the `ui.yaml#nav_params` values forwarded
 * from `personal-dashboard`'s `user_taps_savings_card` entry point — supplied to
 * [PersonalSavingsViewModel] via Koin `parametersOf(...)` (matching `PersonalSavingsModule`'s
 * `viewModel { parameters -> ... }` declaration).
 *
 * [onNavigateBack] is a PLAIN nav callback, NOT a [PersonalSavingsAction] dispatch —
 * `ui.yaml#components.top_bar.on_navigation_click` wires `NavigateBack`, but
 * `state_model.actions.members` does NOT declare a matching `OnBack` action member (see
 * [PersonalSavingsEvent] KDoc "NavigateBack reachability gap") — same class of drift as
 * `personal-loans`'s identical top-bar gap. The back icon pops the NavController stack directly.
 * See API.md#screen.
 */
@Composable
internal fun PersonalSavingsScreen(
    clientId: Long,
    groupLinkedSavingsId: Long,
    individualSavingsId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalSavingsViewModel = koinViewModel(
        parameters = { parametersOf(clientId, groupLinkedSavingsId, individualSavingsId) },
    ),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            PersonalSavingsEvent.NavigateBack -> onNavigateBack()
        }
    }

    PersonalSavingsContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `personal-savings-screen`. State-driven per
 * [PersonalSavingsState.deriveScreenState] — every [PersonalSavingsScreenState] member is
 * handled (Loading/Content/Error). [SavingsTabRow] is rendered OUTSIDE the `when` block —
 * `ui.yaml#states.*.components` includes `savings_tab_row` in every one of the 3 declared states
 * (loading/content/error). [onNavigateBack] is threaded through as a plain callback (see
 * [PersonalSavingsScreen] KDoc) rather than folded into [onAction]. See API.md#screen.
 */
@Composable
internal fun PersonalSavingsContent(
    state: PersonalSavingsState,
    onAction: (PersonalSavingsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.screens_personal_savings_title)
    val screenState = state.deriveScreenState()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onNavigateBack,
        title = title,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = screenState == PersonalSavingsScreenState.Content,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(PersonalSavingsAction.OnRefresh) },
        ),
        modifier = modifier.testTag(PersonalSavingsTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SavingsTabRow(
                selectedTab = state.selectedTab,
                hasIndividualAccount = state.individualSavingsId != null,
                onTabSelected = { onAction(PersonalSavingsAction.OnTabSelected(it)) },
            )

            when (screenState) {
                PersonalSavingsScreenState.Loading -> PersonalSavingsLoadingSection()
                PersonalSavingsScreenState.Content -> PersonalSavingsContentSection(state = state)
                PersonalSavingsScreenState.Error -> PersonalSavingsErrorSection(
                    error = state.error,
                    onRetry = { onAction(PersonalSavingsAction.OnRetry) },
                )
            }
        }
    }
}

/**
 * `PersonalSavingsScreenState.Loading` — 6 shimmering [SavingsTransactionSkeletonRow]s
 * (`ui.yaml#components.shimmer_loading.count: 6`), mirroring `preview/loading.html` /
 * `ui.yaml#states.loading`. See API.md#screen.
 */
@Composable
internal fun PersonalSavingsLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val loadingCd = stringResource(Res.string.screens_personal_savings_shimmer_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = sp.lg, vertical = sp.md)
            .semantics { contentDescription = loadingCd }
            .testTag(PersonalSavingsTestTags.LOADING_INDICATOR),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        repeat(6) { SavingsTransactionSkeletonRow() }
    }
}

/**
 * `PersonalSavingsScreenState.Content` — active-tab balance hero card, contribution-progress card
 * (`GROUP_LINKED` only), and the active tab's transaction history — OR the
 * [SavingsEmptyIndividualState] promo when `selectedTab == INDIVIDUAL && individualSavingsId ==
 * null` (see that composable's KDoc for the reachability note). Non-paginated (`business_logic
 * .kind: crud`, plain `LazyColumn`, no `PagingScreenContent`). See API.md#screen.
 */
@Composable
internal fun PersonalSavingsContentSection(state: PersonalSavingsState, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val transactionHistoryLabel = stringResource(Res.string.screens_personal_savings_transaction_history_label)

    val showEmptyIndividual = state.selectedTab == SavingsTab.INDIVIDUAL && state.individualSavingsId == null
    val balance = if (state.selectedTab == SavingsTab.GROUP_LINKED) state.groupLinkedBalance else state.individualBalance
    val accountId: Long? = if (state.selectedTab == SavingsTab.GROUP_LINKED) state.groupLinkedSavingsId else state.individualSavingsId
    val transactions: List<SavingsLedgerEntry> =
        if (state.selectedTab == SavingsTab.GROUP_LINKED) state.groupLinkedTransactions else state.individualTransactions

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(PersonalSavingsTestTags.TRANSACTION_LIST),
        contentPadding = PaddingValues(horizontal = sp.lg, vertical = sp.md),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        if (showEmptyIndividual) {
            item(key = "empty_individual") { SavingsEmptyIndividualState() }
        } else {
            item(key = "balance_card") { SavingsBalanceHeroCard(tab = state.selectedTab, balance = balance, accountId = accountId) }

            if (state.selectedTab == SavingsTab.GROUP_LINKED) {
                item(key = "contribution_progress") {
                    SavingsContributionProgressCard(
                        meetingsAttended = state.meetingsAttended,
                        totalMeetings = state.totalMeetings,
                        groupLinkedBalance = state.groupLinkedBalance,
                    )
                }
            }

            item(key = "transactions_header") {
                Text(text = transactionHistoryLabel, style = MaterialTheme.typography.titleMedium)
            }

            items(items = transactions, key = { it.id }) { transaction ->
                SavingsTransactionRow(
                    transaction = transaction,
                    testTag = PersonalSavingsTestTags.transactionRowTag(transaction.id),
                )
            }
        }
    }
}

/**
 * `PersonalSavingsScreenState.Error` — full-screen error surface (wifi_off icon, title, resolved
 * [error]-mapped message, Retry CTA), mirroring `preview/error.html` / `ui.yaml#states.error`. The
 * Retry button is hidden when [SavingsError.retry] is `false` (`SavingsError.Unauthorized` —
 * session expiry is not retryable; the member must sign back in). See API.md#screen.
 */
@Composable
internal fun PersonalSavingsErrorSection(error: SavingsError?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_personal_savings_error_icon_cd)
    val titleText = stringResource(Res.string.screens_personal_savings_error_title)
    val retryLabel = stringResource(Res.string.screens_personal_savings_action_retry)
    val message = when (error) {
        SavingsError.Network, null -> stringResource(Res.string.screens_personal_savings_error_network_message)
        SavingsError.Server -> stringResource(Res.string.screens_personal_savings_error_server_message)
        SavingsError.Unauthorized -> stringResource(Res.string.screens_personal_savings_error_session_message)
    }
    val canRetry = error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(PersonalSavingsTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = errorIconCd,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(text = titleText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = sp.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = sp.sm),
        )

        if (canRetry) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(PersonalSavingsTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
