/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.personalloans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.FloatingActionButtonContent
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.LoanStatusFilter
import org.mifos.groupbanking.feature.personalloans.components.PersonalLoansCard
import org.mifos.groupbanking.feature.personalloans.components.PersonalLoansCardSkeleton
import org.mifos.groupbanking.feature.personalloans.components.PersonalLoansFilterChips
import org.mifos.groupbanking.feature.personalloans.generated.resources.Res
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_action_retry
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_empty_action
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_empty_body
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_empty_icon_cd
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_empty_title
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_error_icon_cd
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_error_network_message
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_error_server_message
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_error_session_message
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_error_title
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_fab_cd
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_fab_label
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_shimmer_cd
import org.mifos.groupbanking.feature.personalloans.generated.resources.screens_personal_loans_title

/**
 * Container for `personal-loans-screen`. Collects [PersonalLoansViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [PersonalLoansEvent]s (navigate to
 * loan-request / back) through [EventsEffect], and delegates all rendering to the stateless
 * [PersonalLoansContent]. [clientId] is the `ui.yaml#nav_params` value forwarded from
 * `personal-dashboard`'s `user_taps_loan_card` entry point — supplied to
 * [PersonalLoansViewModel] via Koin `parametersOf(clientId)` (matching `PersonalLoansModule`'s
 * `viewModel { parameters -> ... }` declaration).
 *
 * **[onNavigateToLoanRequest] only** — no `onNavigateToLoanDetail` callback is declared:
 * `ui.yaml#components.loan_card.on_click.action` is `OnLoanExpand` (an in-place expand/collapse
 * toggle via [PersonalLoansAction.OnLoanExpand], not a navigation event) and
 * `flow.yaml#navigates_to` lists only `loan-request` and `personal-dashboard` (back) — no
 * `loan-detail` target. Inventing an unwired detail callback here would fail the
 * generated-nav-vs-flow completion gate (every `navigate*()` target in `PersonalLoansRoute.kt`
 * must appear in `flow.yaml#navigates_to[]`).
 *
 * [onNavigateBack] is a PLAIN nav callback, NOT a [PersonalLoansAction] dispatch —
 * `ui.yaml#components.top_bar.on_navigation_click` wires `NavigateBack`, but
 * `state_model.actions.members` does NOT declare a matching `OnBack` action member (see
 * [PersonalLoansEvent] KDoc "NavigateBack reachability gap") — same class of drift as
 * `loan-list`'s identical top-bar gap. The back icon pops the NavController stack directly.
 * [PersonalLoansEvent.NavigateBack] is still exhaustively handled below (it is declared on the
 * event sealed interface even though this ViewModel never emits it) and also routes to
 * [onNavigateBack] for contract completeness. See API.md#screen.
 */
@Composable
internal fun PersonalLoansScreen(
    clientId: Long,
    onNavigateToLoanRequest: (clientId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalLoansViewModel = koinViewModel(parameters = { parametersOf(clientId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            PersonalLoansEvent.NavigateToLoanRequest -> onNavigateToLoanRequest(clientId)
            PersonalLoansEvent.NavigateBack -> onNavigateBack()
        }
    }

    PersonalLoansContent(
        state = state,
        onAction = viewModel::trySendAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `personal-loans-screen`. State-driven per
 * [PersonalLoansState.deriveScreenState] — every [PersonalLoansScreenState] member is handled
 * (Loading/Content/Empty/Error). [onNavigateBack] is threaded through as a plain callback (see
 * [PersonalLoansScreen] KDoc) rather than folded into [onAction] — every OTHER interactive
 * element on this screen dispatches a real declared [PersonalLoansAction] member. See
 * API.md#screen.
 */
@Composable
internal fun PersonalLoansContent(
    state: PersonalLoansState,
    onAction: (PersonalLoansAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.screens_personal_loans_title)
    val fabCd = stringResource(Res.string.screens_personal_loans_fab_cd)
    val fabLabel = stringResource(Res.string.screens_personal_loans_fab_label)
    val screenState = state.deriveScreenState()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onNavigateBack,
        title = title,
        floatingActionButtonContent = if (screenState == PersonalLoansScreenState.Content) {
            // ui.yaml#components.request_loan_fab.style.background: secondary — the shared
            // FloatingActionButtonContent contract (core/ui/scaffold/KptScaffold.kt) only exposes
            // an onClick/contentColor/content triple (no containerColor override), so the FAB
            // renders with the framework's default M3 container tone rather than a literal
            // `secondary` swap; flagged (not worked around by touching core/ui) same as
            // `loan-list`'s identical FAB-color constraint.
            FloatingActionButtonContent(
                onClick = { onAction(PersonalLoansAction.OnRequestLoanClick) },
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                content = { PersonalLoansFabContent(fabCd = fabCd, fabLabel = fabLabel) },
            )
        } else {
            null
        },
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = screenState == PersonalLoansScreenState.Content,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(PersonalLoansAction.OnRefresh) },
        ),
        modifier = modifier.testTag(PersonalLoansTestTags.SCREEN),
    ) {
        when (screenState) {
            PersonalLoansScreenState.Loading -> PersonalLoansLoadingSection(
                filterStatus = state.filterStatus,
                onAction = onAction,
            )

            PersonalLoansScreenState.Content -> PersonalLoansContentSection(state = state, onAction = onAction)

            PersonalLoansScreenState.Empty -> PersonalLoansEmptySection(onAction = onAction)

            PersonalLoansScreenState.Error -> PersonalLoansErrorSection(
                error = state.error,
                onRetry = { onAction(PersonalLoansAction.OnRetry) },
            )
        }
    }
}

/**
 * FAB content — icon + label, giving the plain FAB slot an extended-FAB look (mirrors
 * `LoanListFabContent`). The merged [fabCd] semantics carry the accessible label; the icon is
 * decorative. `ui.yaml#components.request_loan_fab.label`.
 */
@Composable
internal fun PersonalLoansFabContent(fabCd: String, fabLabel: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        modifier = Modifier.semantics { contentDescription = fabCd }.testTag(PersonalLoansTestTags.FAB_REQUEST_LOAN),
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Text(text = fabLabel)
    }
}

/**
 * `PersonalLoansScreenState.Loading` — functional filter-chip row (bound, though the underlying
 * list is still empty) + 3 shimmering [PersonalLoansCardSkeleton] rows
 * (`ui.yaml#components.shimmer_loading.count: 3`), mirroring `preview/loading.html` /
 * `ui.yaml#states.loading`. See API.md#screen.
 */
@Composable
internal fun PersonalLoansLoadingSection(
    filterStatus: LoanStatusFilter,
    onAction: (PersonalLoansAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val loadingCd = stringResource(Res.string.screens_personal_loans_shimmer_cd)

    Column(modifier = modifier.fillMaxSize()) {
        PersonalLoansFilterChips(
            selectedFilter = filterStatus,
            onFilterChange = { onAction(PersonalLoansAction.OnFilterChange(it)) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = sp.lg)
                .semantics { contentDescription = loadingCd }
                .testTag(PersonalLoansTestTags.LOADING_INDICATOR),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            repeat(3) { PersonalLoansCardSkeleton() }
        }
    }
}

/**
 * `PersonalLoansScreenState.Content` — filter-chip row + scrollable list of [PersonalLoansCard]s
 * over `state.filteredLoans`. Non-paginated (`business_logic.kind: crud`, plain `LazyColumn`, no
 * `PagingScreenContent`). Each card's expand toggle reads `state.selectedLoanId == loan.id` and
 * dispatches [PersonalLoansAction.OnLoanExpand]. See API.md#screen.
 */
@Composable
internal fun PersonalLoansContentSection(
    state: PersonalLoansState,
    onAction: (PersonalLoansAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        PersonalLoansFilterChips(
            selectedFilter = state.filterStatus,
            onFilterChange = { onAction(PersonalLoansAction.OnFilterChange(it)) },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = sp.lg).testTag(PersonalLoansTestTags.LOAN_LIST),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            items(items = state.filteredLoans, key = { it.id }) { loan ->
                PersonalLoansCard(
                    loan = loan,
                    isExpanded = state.selectedLoanId == loan.id,
                    onClick = { onAction(PersonalLoansAction.OnLoanExpand(loan.id)) },
                    testTag = PersonalLoansTestTags.cardTag(loan.id),
                    detailsTestTag = PersonalLoansTestTags.cardDetailsTag(loan.id),
                )
            }
        }
    }
}

/**
 * `PersonalLoansScreenState.Empty` — genuinely zero loans for this member
 * (`PersonalLoansState.loans.isEmpty()`). Illustration + title + body + a "Request a Loan" CTA
 * button — `ui.yaml#states.empty.components` is `[top_bar, empty_state]` only (no filter chips,
 * no FAB), so the CTA lives in-body via [PersonalLoansAction.OnRequestLoanClick]. See
 * API.md#screen.
 */
@Composable
internal fun PersonalLoansEmptySection(
    onAction: (PersonalLoansAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val iconCd = stringResource(Res.string.screens_personal_loans_empty_icon_cd)
    val titleText = stringResource(Res.string.screens_personal_loans_empty_title)
    val bodyText = stringResource(Res.string.screens_personal_loans_empty_body)
    val actionLabel = stringResource(Res.string.screens_personal_loans_empty_action)

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(PersonalLoansTestTags.EMPTY_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalanceWallet,
            contentDescription = iconCd,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp).padding(bottom = sp.lg),
        )
        Text(text = titleText, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = sp.sm),
        )
        Button(
            onClick = { onAction(PersonalLoansAction.OnRequestLoanClick) },
            modifier = Modifier
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(PersonalLoansTestTags.EMPTY_ACTION_BUTTON),
        ) {
            Text(text = actionLabel)
        }
    }
}

/**
 * `PersonalLoansScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [error]-mapped message, Retry CTA), mirroring `preview/error.html` / `ui.yaml#states.error`
 * (`ui.yaml#states.error.components` is `[top_bar, filter_chips_row, error_state]`, but the
 * chip row is inert with nothing to filter yet, so this screen omits it — mirrors
 * `loan-list`'s identical no-chips-during-error precedent). The Retry button is hidden when
 * [LoanError.retry] is `false` (`LoanError.Unauthorized` — session expiry is not retryable; the
 * member must sign back in). See API.md#screen.
 */
@Composable
internal fun PersonalLoansErrorSection(
    error: LoanError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_personal_loans_error_icon_cd)
    val titleText = stringResource(Res.string.screens_personal_loans_error_title)
    val retryLabel = stringResource(Res.string.screens_personal_loans_action_retry)
    val message = when (error) {
        LoanError.Network, null -> stringResource(Res.string.screens_personal_loans_error_network_message)
        LoanError.Server -> stringResource(Res.string.screens_personal_loans_error_server_message)
        LoanError.Unauthorized -> stringResource(Res.string.screens_personal_loans_error_session_message)
    }
    val canRetry = error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(PersonalLoansTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = errorIconCd,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
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
                    .testTag(PersonalLoansTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
