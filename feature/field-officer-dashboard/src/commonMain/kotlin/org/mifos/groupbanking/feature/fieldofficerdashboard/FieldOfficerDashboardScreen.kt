/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.core.TopAppBarAction
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.core.ui.scaffold.rememberKptPullToRefreshState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.GroupStatusFilter
import org.mifos.groupbanking.core.model.OverdueRateFilter
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.ClearFilterIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.FieldOfficerFilterChip
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.GroupHealthCard
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.KpiCardData
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.KpiCardsRow
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.KpiGroupsIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.KpiLoansIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.KpiMembersIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.KpiSavingsIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.OverdueFilterIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.RegionFilterIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.StatusFilterIcon
import org.mifos.groupbanking.feature.fieldofficerdashboard.components.formatCompact
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.Res
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_card_cd
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_content_desc_export
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_cta_retry
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_dialog_cancel
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_empty_body
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_empty_filter_body
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_empty_filter_title
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_empty_title
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_error_auth
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_error_load_title
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_error_network
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_error_no_groups
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_error_server
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_export_error
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_all_regions
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_all_statuses
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_clear
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_overdue_any
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_overdue_gt10
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_overdue_lt10
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_overdue_none
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_status_active
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_status_closed
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_filter_status_pending
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_groups_count_label
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_health_amber
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_health_green
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_health_red
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_health_unknown
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_kpi_groups
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_kpi_loans
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_kpi_members
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_kpi_savings
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_loading_message
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_picker_all
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_picker_overdue_title
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_picker_region_title
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_picker_status_title
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_subtitle
import org.mifos.groupbanking.feature.fieldofficerdashboard.generated.resources.screens_field_officer_dashboard_title
import org.mifos.groupbanking.core.model.HealthIndicator

/**
 * Container for `field-officer-dashboard-screen`. Collects [FieldOfficerDashboardViewModel] state,
 * consumes one-shot [FieldOfficerDashboardEvent]s (navigate to group-dashboard, export share-sheet,
 * snackbar, open picker dialogs) through [EventsEffect], and delegates rendering to the stateless
 * [FieldOfficerDashboardContent]. See API.md#screen.
 */
@Composable
internal fun FieldOfficerDashboardScreen(
    onNavigateToGroupDashboard: (groupId: Long) -> Unit,
    onExportReport: (staffId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FieldOfficerDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRegionPicker by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var showOverduePicker by remember { mutableStateOf(false) }

    val networkMsg = stringResource(Res.string.screens_field_officer_dashboard_error_network)
    val serverMsg = stringResource(Res.string.screens_field_officer_dashboard_error_server)
    val authMsg = stringResource(Res.string.screens_field_officer_dashboard_error_auth)
    val noGroupsMsg = stringResource(Res.string.screens_field_officer_dashboard_error_no_groups)
    val exportErrMsg = stringResource(Res.string.screens_field_officer_dashboard_export_error)

    EventsEffect(viewModel) { event ->
        when (event) {
            is FieldOfficerDashboardEvent.NavigateToGroupDashboard -> onNavigateToGroupDashboard(event.groupId)
            is FieldOfficerDashboardEvent.NavigateToExportReport -> onExportReport(event.staffId)
            is FieldOfficerDashboardEvent.ShowSnackbar -> {
                val resolved = when (event.message) {
                    "error_network" -> networkMsg
                    "error_server" -> serverMsg
                    "error_auth" -> authMsg
                    "error_no_groups" -> noGroupsMsg
                    "export_error" -> exportErrMsg
                    else -> event.message
                }
                snackbarHostState.showSnackbar(resolved)
            }

            FieldOfficerDashboardEvent.ShowRegionPickerDialog -> showRegionPicker = true
            FieldOfficerDashboardEvent.ShowStatusPickerDialog -> showStatusPicker = true
            FieldOfficerDashboardEvent.ShowOverduePickerDialog -> showOverduePicker = true
        }
    }

    FieldOfficerDashboardContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (showRegionPicker) {
        RegionPickerDialog(
            regions = state.availableRegions,
            onSelect = { region ->
                showRegionPicker = false
                viewModel.trySendAction(FieldOfficerDashboardAction.OnRegionFilterSelected(region))
            },
            onDismiss = { showRegionPicker = false },
        )
    }
    if (showStatusPicker) {
        StatusPickerDialog(
            onSelect = { status ->
                showStatusPicker = false
                viewModel.trySendAction(FieldOfficerDashboardAction.OnStatusFilterSelected(status))
            },
            onDismiss = { showStatusPicker = false },
        )
    }
    if (showOverduePicker) {
        OverduePickerDialog(
            onSelect = { filter ->
                showOverduePicker = false
                viewModel.trySendAction(FieldOfficerDashboardAction.OnOverdueFilterSelected(filter))
            },
            onDismiss = { showOverduePicker = false },
        )
    }
}

/**
 * Stateless render surface for `field-officer-dashboard-screen`. State-driven per
 * `FieldOfficerDashboardState.screenState` — every member is handled (Loading/Content/Empty/Error).
 * See API.md#screen.
 */
@Composable
internal fun FieldOfficerDashboardContent(
    state: FieldOfficerDashboardState,
    onAction: (FieldOfficerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_field_officer_dashboard_title)
    val exportCd = stringResource(Res.string.screens_field_officer_dashboard_content_desc_export)

    val actions = if (state.canExport) {
        listOf(
            TopAppBarAction(
                icon = Icons.Filled.FileDownload,
                contentDescription = exportCd,
                onClick = { onAction(FieldOfficerDashboardAction.OnExportReport) },
            ),
        )
    } else {
        emptyList()
    }

    KptScaffold(
        showNavigationIcon = false,
        title = title,
        actions = actions,
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(FieldOfficerDashboardAction.OnRefresh) },
        ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(FieldOfficerDashboardTestTags.SCREEN),
    ) {
        when (state.screenState) {
            FieldOfficerDashboardScreenState.Loading -> FieldOfficerLoadingSection()
            FieldOfficerDashboardScreenState.Content -> FieldOfficerContentSection(state = state, onAction = onAction)
            FieldOfficerDashboardScreenState.Empty -> FieldOfficerEmptySection()
            FieldOfficerDashboardScreenState.Error -> FieldOfficerErrorSection(
                message = when (state.error) {
                    FieldOfficerDashboardError.Network, null -> stringResource(Res.string.screens_field_officer_dashboard_error_network)
                    FieldOfficerDashboardError.Server -> stringResource(Res.string.screens_field_officer_dashboard_error_server)
                    FieldOfficerDashboardError.Auth -> stringResource(Res.string.screens_field_officer_dashboard_error_auth)
                    FieldOfficerDashboardError.NoGroupsAssigned -> stringResource(Res.string.screens_field_officer_dashboard_error_no_groups)
                },
                onRetry = { onAction(FieldOfficerDashboardAction.OnRetry) },
            )
        }
    }
}

@Composable
private fun FieldOfficerLoadingSection(modifier: Modifier = Modifier) {
    val loadingLabel = stringResource(Res.string.screens_field_officer_dashboard_loading_message)
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(FieldOfficerDashboardTestTags.LOADING_INDICATOR),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = loadingLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MaterialTheme.spacing.md),
        )
    }
}

@Composable
private fun FieldOfficerContentSection(
    state: FieldOfficerDashboardState,
    onAction: (FieldOfficerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val kpiCards = listOf(
        KpiCardData(
            value = state.totalGroupsCount.toString(),
            label = stringResource(Res.string.screens_field_officer_dashboard_kpi_groups),
            icon = KpiGroupsIcon,
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        KpiCardData(
            value = state.totalActiveMembers.toString(),
            label = stringResource(Res.string.screens_field_officer_dashboard_kpi_members),
            icon = KpiMembersIcon,
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        KpiCardData(
            value = "KES ${formatCompact(state.totalSavingsThisMonth)}",
            label = stringResource(Res.string.screens_field_officer_dashboard_kpi_savings),
            icon = KpiSavingsIcon,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        KpiCardData(
            value = "KES ${formatCompact(state.totalLoansOutstanding)}",
            label = stringResource(Res.string.screens_field_officer_dashboard_kpi_loans),
            icon = KpiLoansIcon,
            container = MaterialTheme.colorScheme.errorContainer,
            onContainer = MaterialTheme.colorScheme.onErrorContainer,
        ),
    )

    val regionChipLabel = state.selectedRegionFilter
        ?: stringResource(Res.string.screens_field_officer_dashboard_filter_all_regions)
    val statusChipLabel = state.selectedStatusFilter?.let { statusLabel(it) }
        ?: stringResource(Res.string.screens_field_officer_dashboard_filter_all_statuses)
    val overdueChipLabel = state.selectedOverdueFilter?.let { overdueLabel(it) }
        ?: stringResource(Res.string.screens_field_officer_dashboard_filter_overdue_any)
    val clearLabel = stringResource(Res.string.screens_field_officer_dashboard_filter_clear)
    val countLabel = stringResource(
        Res.string.screens_field_officer_dashboard_groups_count_label,
        state.filteredGroups.size,
        state.groups.size,
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(FieldOfficerDashboardTestTags.GROUP_LIST),
        verticalArrangement = Arrangement.spacedBy(sp.sm),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = sp.md),
    ) {
        item { KpiCardsRow(cards = kpiCards) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = sp.lg)
                    .testTag(FieldOfficerDashboardTestTags.FILTER_ROW),
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
            ) {
                FieldOfficerFilterChip(
                    label = regionChipLabel,
                    icon = RegionFilterIcon,
                    selected = state.selectedRegionFilter != null,
                    onClick = { onAction(FieldOfficerDashboardAction.OnShowRegionPicker) },
                    testTag = FieldOfficerDashboardTestTags.REGION_FILTER_CHIP,
                )
                FieldOfficerFilterChip(
                    label = statusChipLabel,
                    icon = StatusFilterIcon,
                    selected = state.selectedStatusFilter != null,
                    onClick = { onAction(FieldOfficerDashboardAction.OnShowStatusPicker) },
                    testTag = FieldOfficerDashboardTestTags.STATUS_FILTER_CHIP,
                )
                FieldOfficerFilterChip(
                    label = overdueChipLabel,
                    icon = OverdueFilterIcon,
                    selected = state.selectedOverdueFilter != null,
                    onClick = { onAction(FieldOfficerDashboardAction.OnShowOverduePicker) },
                    testTag = FieldOfficerDashboardTestTags.OVERDUE_FILTER_CHIP,
                )
                if (state.hasActiveFilter) {
                    FieldOfficerFilterChip(
                        label = clearLabel,
                        icon = ClearFilterIcon,
                        selected = false,
                        onClick = { onAction(FieldOfficerDashboardAction.OnClearFilters) },
                        testTag = FieldOfficerDashboardTestTags.CLEAR_FILTERS_CHIP,
                    )
                }
            }
        }

        item {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = sp.lg),
            )
        }

        if (state.filteredGroups.isEmpty()) {
            item { FieldOfficerFilterEmptySection(onClear = { onAction(FieldOfficerDashboardAction.OnClearFilters) }) }
        } else {
            items(items = state.filteredGroups, key = { it.id }) { group ->
                GroupHealthCardRow(group = group, onAction = onAction, modifier = Modifier.padding(horizontal = sp.lg))
            }
        }
    }
}

@Composable
private fun GroupHealthCardRow(
    group: GroupHealthSummary,
    onAction: (FieldOfficerDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = stringResource(Res.string.screens_field_officer_dashboard_subtitle, group.officeName, group.cycleNumber)
    val healthLabel = healthLabel(group.healthIndicator)
    val cardCd = stringResource(
        Res.string.screens_field_officer_dashboard_card_cd,
        group.name,
        group.officeName,
        healthLabel,
    )
    GroupHealthCard(
        group = group,
        cardCd = cardCd,
        subtitle = subtitle,
        healthLabel = healthLabel,
        onClick = { onAction(FieldOfficerDashboardAction.OnGroupTapped(group.id)) },
        testTag = FieldOfficerDashboardTestTags.cardTag(group.id),
        modifier = modifier,
    )
}

@Composable
private fun FieldOfficerEmptySection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(FieldOfficerDashboardTestTags.EMPTY_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(80.dp))
        Text(
            text = stringResource(Res.string.screens_field_officer_dashboard_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = stringResource(Res.string.screens_field_officer_dashboard_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )
    }
}

@Composable
private fun FieldOfficerFilterEmptySection(onClear: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth().padding(sp.lg).testTag(FieldOfficerDashboardTestTags.EMPTY_FILTER_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = Icons.Filled.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
        Text(
            text = stringResource(Res.string.screens_field_officer_dashboard_empty_filter_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = sp.md),
        )
        Text(
            text = stringResource(Res.string.screens_field_officer_dashboard_empty_filter_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.xs),
        )
        TextButton(onClick = onClear, modifier = Modifier.padding(top = sp.sm)) {
            Text(text = stringResource(Res.string.screens_field_officer_dashboard_filter_clear))
        }
    }
}

@Composable
private fun FieldOfficerErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(FieldOfficerDashboardTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Filled.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Text(
            text = stringResource(Res.string.screens_field_officer_dashboard_error_load_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = sp.lg),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .padding(top = sp.lg)
                .testTag(FieldOfficerDashboardTestTags.ERROR_RETRY_BUTTON),
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(text = stringResource(Res.string.screens_field_officer_dashboard_cta_retry), modifier = Modifier.padding(start = sp.xs))
        }
    }
}

// ---------------------------------------------------------------------------
// Picker dialogs — opened by the ShowXPickerDialog events, selection dispatches OnXFilterSelected.
// ---------------------------------------------------------------------------

@Composable
private fun RegionPickerDialog(regions: List<String>, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.screens_field_officer_dashboard_picker_region_title)) },
        text = {
            Column {
                TextButton(onClick = { onSelect(null) }) {
                    Text(stringResource(Res.string.screens_field_officer_dashboard_picker_all))
                }
                regions.forEach { region ->
                    TextButton(onClick = { onSelect(region) }) { Text(region) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.screens_field_officer_dashboard_dialog_cancel))
            }
        },
    )
}

@Composable
private fun StatusPickerDialog(onSelect: (GroupStatusFilter?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.screens_field_officer_dashboard_picker_status_title)) },
        text = {
            Column {
                TextButton(onClick = { onSelect(null) }) {
                    Text(stringResource(Res.string.screens_field_officer_dashboard_picker_all))
                }
                GroupStatusFilter.entries.forEach { status ->
                    TextButton(onClick = { onSelect(status) }) { Text(statusLabel(status)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.screens_field_officer_dashboard_dialog_cancel))
            }
        },
    )
}

@Composable
private fun OverduePickerDialog(onSelect: (OverdueRateFilter?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.screens_field_officer_dashboard_picker_overdue_title)) },
        text = {
            Column {
                TextButton(onClick = { onSelect(null) }) {
                    Text(stringResource(Res.string.screens_field_officer_dashboard_filter_overdue_any))
                }
                OverdueRateFilter.entries.forEach { filter ->
                    TextButton(onClick = { onSelect(filter) }) { Text(overdueLabel(filter)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.screens_field_officer_dashboard_dialog_cancel))
            }
        },
    )
}

@Composable
private fun statusLabel(status: GroupStatusFilter): String = when (status) {
    GroupStatusFilter.ACTIVE -> stringResource(Res.string.screens_field_officer_dashboard_filter_status_active)
    GroupStatusFilter.PENDING -> stringResource(Res.string.screens_field_officer_dashboard_filter_status_pending)
    GroupStatusFilter.CLOSED -> stringResource(Res.string.screens_field_officer_dashboard_filter_status_closed)
}

@Composable
private fun overdueLabel(filter: OverdueRateFilter): String = when (filter) {
    OverdueRateFilter.NONE -> stringResource(Res.string.screens_field_officer_dashboard_filter_overdue_none)
    OverdueRateFilter.LESS_THAN_10 -> stringResource(Res.string.screens_field_officer_dashboard_filter_overdue_lt10)
    OverdueRateFilter.GREATER_THAN_10 -> stringResource(Res.string.screens_field_officer_dashboard_filter_overdue_gt10)
}

@Composable
private fun healthLabel(indicator: HealthIndicator): String = when (indicator) {
    HealthIndicator.GREEN -> stringResource(Res.string.screens_field_officer_dashboard_health_green)
    HealthIndicator.AMBER -> stringResource(Res.string.screens_field_officer_dashboard_health_amber)
    HealthIndicator.RED -> stringResource(Res.string.screens_field_officer_dashboard_health_red)
    HealthIndicator.UNKNOWN -> stringResource(Res.string.screens_field_officer_dashboard_health_unknown)
}
