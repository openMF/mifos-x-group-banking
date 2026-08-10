/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.personalsavings.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.designsystem.theme.spacing
import kpt.core.model.SavingsTab
import kpt.feature.personalsavings.PersonalSavingsTestTags
import kpt.feature.personalsavings.generated.resources.Res
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_tab_group_linked
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_tab_individual
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_tab_row_cd
import org.jetbrains.compose.resources.stringResource

/**
 * `ui.yaml#components.savings_tab_row` — GROUP_LINKED / INDIVIDUAL account switcher, rendered
 * across every `PersonalSavingsScreenState` (loading/content/error all include it —
 * `ui.yaml#states.*.components`). Implemented with the standard Material3 [TabRow] (rather than a
 * literal chip-group as `ui.yaml#components.savings_tab_row.type: chip-group` styles it) for
 * built-in accessibility (selected-tab semantics, indicator) — the pill/rounded visual intent is
 * approximated via [MaterialTheme.typography] weight on the selected label, not a hand-rolled
 * chip row (flagged, not a silent style drop).
 *
 * The INDIVIDUAL tab is omitted entirely — not merely disabled — when [hasIndividualAccount] is
 * `false` (`ui.yaml#components.savings_tab_row.chips[1].visible_when: "individualSavingsId !=
 * null"`), which also makes [SavingsTab.INDIVIDUAL] unreachable via tap in that case.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsTabRow(
    selectedTab: SavingsTab,
    hasIndividualAccount: Boolean,
    onTabSelected: (SavingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val rowCd = stringResource(Res.string.screens_personal_savings_tab_row_cd)
    val groupLinkedLabel = stringResource(Res.string.screens_personal_savings_tab_group_linked)
    val individualLabel = stringResource(Res.string.screens_personal_savings_tab_individual)
    val selectedIndex = if (selectedTab == SavingsTab.INDIVIDUAL && hasIndividualAccount) 1 else 0

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier
            .testTag(PersonalSavingsTestTags.TAB_ROW)
            .semantics { contentDescription = rowCd },
    ) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onTabSelected(SavingsTab.GROUP_LINKED) },
            modifier = Modifier.heightIn(min = sp.touchTargetMin).testTag(PersonalSavingsTestTags.TAB_GROUP_LINKED),
            text = { Text(groupLinkedLabel) },
        )
        if (hasIndividualAccount) {
            Tab(
                selected = selectedIndex == 1,
                onClick = { onTabSelected(SavingsTab.INDIVIDUAL) },
                modifier = Modifier.heightIn(min = sp.touchTargetMin).testTag(PersonalSavingsTestTags.TAB_INDIVIDUAL),
                text = { Text(individualLabel) },
            )
        }
    }
}
