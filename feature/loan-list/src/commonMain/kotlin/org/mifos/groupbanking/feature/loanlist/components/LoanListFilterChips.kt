/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.LoanStatusFilter
import org.mifos.groupbanking.feature.loanlist.LoanListTestTags
import org.mifos.groupbanking.feature.loanlist.generated.resources.Res
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_filter_active
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_filter_all
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_filter_closed
import org.mifos.groupbanking.feature.loanlist.generated.resources.screens_loan_list_filter_overdue

/**
 * Horizontal status-filter chip row — `ui.yaml#components.filter_chips_row` (4 chips: All /
 * Active / Overdue / Closed, `style.scroll: horizontal`). Each chip dispatches
 * [org.mifos.groupbanking.feature.loanlist.LoanListAction.OnFilterChange] via [onFilterChange] —
 * pure client-side transform, no network call
 * (`ui.yaml#components.filter_chips_row.chips[].on_click.action_contract.effect:
 * transform_state`). `chip_overdue`'s `selected_color: errorContainer` is honoured via
 * [FilterChipDefaults.filterChipColors]; `chip_all`/`chip_active` (`primaryContainer`) and
 * `chip_closed` (`surfaceVariant`) already match M3's own default selected-chip tone, so no extra
 * override is needed for them. See API.md#screen.
 */
@Composable
fun LoanListFilterChips(
    selectedFilter: LoanStatusFilter,
    onFilterChange: (LoanStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val allLabel = stringResource(Res.string.screens_loan_list_filter_all)
    val activeLabel = stringResource(Res.string.screens_loan_list_filter_active)
    val overdueLabel = stringResource(Res.string.screens_loan_list_filter_overdue)
    val closedLabel = stringResource(Res.string.screens_loan_list_filter_closed)

    LazyRow(
        modifier = modifier.padding(horizontal = sp.lg, vertical = sp.sm),
        horizontalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.ALL,
                onClick = { onFilterChange(LoanStatusFilter.ALL) },
                label = { Text(text = allLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanListTestTags.FILTER_CHIP_ALL),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.ACTIVE,
                onClick = { onFilterChange(LoanStatusFilter.ACTIVE) },
                label = { Text(text = activeLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanListTestTags.FILTER_CHIP_ACTIVE),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.OVERDUE,
                onClick = { onFilterChange(LoanStatusFilter.OVERDUE) },
                label = { Text(text = overdueLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanListTestTags.FILTER_CHIP_OVERDUE),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.CLOSED,
                onClick = { onFilterChange(LoanStatusFilter.CLOSED) },
                label = { Text(text = closedLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(LoanListTestTags.FILTER_CHIP_CLOSED),
            )
        }
    }
}
