/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouptypepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.feature.grouptypepicker.components.GroupTypeCard
import org.mifos.groupbanking.feature.grouptypepicker.components.GroupTypeCardSkeleton
import org.mifos.groupbanking.feature.grouptypepicker.components.GroupTypeErrorSection
import org.mifos.groupbanking.feature.grouptypepicker.components.groupTypeFeatureChips
import org.mifos.groupbanking.feature.grouptypepicker.components.groupTypeIcon
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.Res
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_error_auth_message
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_error_network_message
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_error_server_message
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_loading_message
import org.mifos.groupbanking.feature.grouptypepicker.generated.resources.screens_group_type_picker_title

/**
 * Container for `group-type-picker-screen`. Collects [GroupTypePickerViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [GroupTypePickerEvent]s (navigate-forward with
 * the resolved [GroupTypeConfig] nav-arg, navigate-back) through [EventsEffect], and delegates all
 * rendering to the stateless [GroupTypePickerContent]. See API.md#screen.
 */
@Composable
internal fun GroupTypePickerScreen(
    onNavigateToGroupCreate: (GroupTypeConfig) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupTypePickerViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel) { event ->
        when (event) {
            is GroupTypePickerEvent.NavigateToGroupCreate -> onNavigateToGroupCreate(event.typeConfig)
            GroupTypePickerEvent.NavigateBack -> onNavigateBack()
        }
    }

    GroupTypePickerContent(
        state = state,
        onAction = viewModel::trySendAction,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `group-type-picker-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` — every [GroupTypePickerScreenState] member is handled
 * (Loading/Content/Error; ui.yaml declares no `empty` state — an empty catalogue is not a
 * reachable happy-path per `business_logic.description`, so it renders as Content with zero
 * cards rather than inventing an unlisted screen state). See API.md#screen.
 */
@Composable
internal fun GroupTypePickerContent(
    state: GroupTypePickerState,
    onAction: (GroupTypePickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.screens_group_type_picker_title)

    KptScaffold(
        onNavigationIconClick = { onAction(GroupTypePickerAction.OnBack) },
        title = title,
        modifier = modifier.testTag(GroupTypePickerTestTags.SCREEN),
    ) {
        when (state.screenState) {
            GroupTypePickerScreenState.Loading -> GroupTypeLoadingSection()
            GroupTypePickerScreenState.Content -> GroupTypeListSection(
                typeConfigs = state.typeConfigs,
                onAction = onAction,
            )
            GroupTypePickerScreenState.Error -> GroupTypeErrorSection(
                message = when (state.error) {
                    GroupTypePickerError.Network, null ->
                        stringResource(Res.string.screens_group_type_picker_error_network_message)
                    GroupTypePickerError.Server ->
                        stringResource(Res.string.screens_group_type_picker_error_server_message)
                    GroupTypePickerError.Auth ->
                        stringResource(Res.string.screens_group_type_picker_error_auth_message)
                },
                onRetry = { onAction(GroupTypePickerAction.OnRetry) },
            )
        }
    }
}

/**
 * `GroupTypePickerScreenState.Loading` — 4 shimmering [GroupTypeCardSkeleton] rows behind a
 * centered [CircularProgressIndicator], mirroring `preview/loading.html`. See API.md#screen.
 */
@Composable
internal fun GroupTypeLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val loadingLabel = stringResource(Res.string.screens_group_type_picker_loading_message)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(sp.lg)
            .semantics { contentDescription = loadingLabel },
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        repeat(4) { GroupTypeCardSkeleton() }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(GroupTypePickerTestTags.LOADING_INDICATOR).size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * `GroupTypePickerScreenState.Content` — the scrollable list of 9 [GroupTypeCard]s. Tapping a
 * card dispatches [GroupTypePickerAction.OnTypeCardTap] with the raw wire `typeSlug` (mirrors
 * `ui.yaml#components.type_card_*.on_click.params.typeSlug`) — the ViewModel resolves it back to
 * the full [GroupTypeConfig] and forwards it as the [GroupTypePickerEvent.NavigateToGroupCreate]
 * nav-arg. See API.md#screen.
 */
@Composable
internal fun GroupTypeListSection(
    typeConfigs: List<GroupTypeConfig>,
    onAction: (GroupTypePickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(GroupTypePickerTestTags.TYPE_LIST),
        contentPadding = PaddingValues(sp.lg),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        items(items = typeConfigs, key = { it.typeSlug.name }) { config ->
            GroupTypeCard(
                config = config,
                icon = groupTypeIcon(config.typeSlug),
                features = groupTypeFeatureChips(config.typeSlug),
                onClick = { onAction(GroupTypePickerAction.OnTypeCardTap(config.typeSlug.name)) },
                testTag = GroupTypePickerTestTags.cardTag(config.typeSlug),
            )
        }
    }
}
