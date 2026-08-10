/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalloans.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.designsystem.theme.spacing
import kpt.core.model.LoanStatusFilter
import kpt.feature.personalloans.PersonalLoansTestTags
import kpt.feature.personalloans.generated.resources.Res
import kpt.feature.personalloans.generated.resources.screens_personal_loans_filter_active
import kpt.feature.personalloans.generated.resources.screens_personal_loans_filter_all
import kpt.feature.personalloans.generated.resources.screens_personal_loans_filter_chips_cd
import kpt.feature.personalloans.generated.resources.screens_personal_loans_filter_closed
import org.jetbrains.compose.resources.stringResource

/**
 * Horizontal status-filter chip row — `ui.yaml#components.filter_chips_row` (3 chips: All /
 * Active / Closed, `style.scroll_horizontal: true`). No `Overdue` chip is declared here (unlike
 * `loan-list`'s 4-chip row) — `ui.yaml#components.filter_chips_row.chips` only lists ALL/ACTIVE/
 * CLOSED for this screen; overdue loans surface inline via the loan card's own warning icon
 * instead. Each chip dispatches [kpt.feature.personalloans.PersonalLoansAction
 * .OnFilterChange] via [onFilterChange] — pure client-side transform, no network call
 * (`ui.yaml#components.filter_chips_row.on_click.action_contract.effect: transform_state`). See
 * API.md#screen.
 */
@Composable
fun PersonalLoansFilterChips(
    selectedFilter: LoanStatusFilter,
    onFilterChange: (LoanStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val allLabel = stringResource(Res.string.screens_personal_loans_filter_all)
    val activeLabel = stringResource(Res.string.screens_personal_loans_filter_active)
    val closedLabel = stringResource(Res.string.screens_personal_loans_filter_closed)
    val chipsCd = stringResource(Res.string.screens_personal_loans_filter_chips_cd)

    LazyRow(
        modifier = modifier
            .padding(horizontal = sp.lg, vertical = sp.sm)
            .semantics { contentDescription = chipsCd },
        horizontalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.ALL,
                onClick = { onFilterChange(LoanStatusFilter.ALL) },
                label = { Text(text = allLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(PersonalLoansTestTags.FILTER_CHIP_ALL),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.ACTIVE,
                onClick = { onFilterChange(LoanStatusFilter.ACTIVE) },
                label = { Text(text = activeLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(PersonalLoansTestTags.FILTER_CHIP_ACTIVE),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == LoanStatusFilter.CLOSED,
                onClick = { onFilterChange(LoanStatusFilter.CLOSED) },
                label = { Text(text = closedLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(PersonalLoansTestTags.FILTER_CHIP_CLOSED),
            )
        }
    }
}
