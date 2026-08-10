/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loandetail.components

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kpt.core.model.LoanDetailTab
import kpt.feature.loandetail.LoanDetailTestTags
import kpt.feature.loandetail.generated.resources.Res
import kpt.feature.loandetail.generated.resources.screens_loan_detail_tab_history
import kpt.feature.loandetail.generated.resources.screens_loan_detail_tab_schedule
import org.jetbrains.compose.resources.stringResource

/**
 * Schedule / Repayment-History tab selector — `ui.yaml#components.detail_tabs`. Dispatches
 * `LoanDetailAction.OnTabChange` — a pure client-side state transition (`ui.yaml effect:
 * transform_state`), no network call. Mirrors `preview/content.html`'s `tab-bar`. See API.md#screen.
 */
@Composable
fun LoanDetailTabs(
    selectedTab: LoanDetailTab,
    onTabSelected: (LoanDetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheduleLabel = stringResource(Res.string.screens_loan_detail_tab_schedule)
    val historyLabel = stringResource(Res.string.screens_loan_detail_tab_history)
    val selectedIndex = if (selectedTab == LoanDetailTab.SCHEDULE) 0 else 1

    TabRow(selectedTabIndex = selectedIndex, modifier = modifier.testTag(LoanDetailTestTags.TABS)) {
        Tab(
            selected = selectedTab == LoanDetailTab.SCHEDULE,
            onClick = { onTabSelected(LoanDetailTab.SCHEDULE) },
            text = { Text(scheduleLabel) },
            modifier = Modifier.testTag(LoanDetailTestTags.TAB_SCHEDULE),
        )
        Tab(
            selected = selectedTab == LoanDetailTab.HISTORY,
            onClick = { onTabSelected(LoanDetailTab.HISTORY) },
            text = { Text(historyLabel) },
            modifier = Modifier.testTag(LoanDetailTestTags.TAB_HISTORY),
        )
    }
}
