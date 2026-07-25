/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail.components

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
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.SavingsTransactionFilter
import org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsDetailTestTags
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.Res
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_filter_all
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_filter_deposits
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_filter_withdrawals

/**
 * `ui.yaml#components.filter_chips_row` — 3 chips (All / Deposits / Withdrawals,
 * `style.scroll_horizontal: true`). Each dispatches
 * [org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsDetailAction.OnFilterSelected]
 * via [onFilterSelected] — pure client-side transform, no network call
 * (`ui.yaml#components.filter_chips_row.chips[].on_click.action_contract.effect: transform_state`).
 * See API.md#screen.
 */
@Composable
fun SavingsFilterChips(
    selectedFilter: SavingsTransactionFilter,
    onFilterSelected: (SavingsTransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val allLabel = stringResource(Res.string.screens_member_savings_detail_filter_all)
    val depositsLabel = stringResource(Res.string.screens_member_savings_detail_filter_deposits)
    val withdrawalsLabel = stringResource(Res.string.screens_member_savings_detail_filter_withdrawals)

    LazyRow(
        modifier = modifier.testTag(MemberSavingsDetailTestTags.FILTER_CHIPS_ROW),
        horizontalArrangement = Arrangement.spacedBy(sp.xs),
    ) {
        item {
            FilterChip(
                selected = selectedFilter == SavingsTransactionFilter.ALL,
                onClick = { onFilterSelected(SavingsTransactionFilter.ALL) },
                label = { Text(text = allLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .testTag(MemberSavingsDetailTestTags.FILTER_CHIP_ALL),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SavingsTransactionFilter.DEPOSITS,
                onClick = { onFilterSelected(SavingsTransactionFilter.DEPOSITS) },
                label = { Text(text = depositsLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .padding(start = sp.xs)
                    .testTag(MemberSavingsDetailTestTags.FILTER_CHIP_DEPOSITS),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SavingsTransactionFilter.WITHDRAWALS,
                onClick = { onFilterSelected(SavingsTransactionFilter.WITHDRAWALS) },
                label = { Text(text = withdrawalsLabel) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .padding(start = sp.xs)
                    .testTag(MemberSavingsDetailTestTags.FILTER_CHIP_WITHDRAWALS),
            )
        }
    }
}
