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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.SavingsTab
import kpt.feature.personalsavings.PersonalSavingsTestTags
import kpt.feature.personalsavings.generated.resources.Res
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_account_number_format
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_balance_amount_format
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_balance_card_cd
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_group_linked_balance_label
import kpt.feature.personalsavings.generated.resources.screens_personal_savings_individual_balance_label
import org.jetbrains.compose.resources.stringResource

/**
 * `ui.yaml#components.balance_hero_card` — the active tab's current balance + savings account
 * number, on a `primary`-toned surface (`ui.yaml#components.balance_hero_card.style.background:
 * primary`). [balance] and [accountId] are pre-resolved by the caller from
 * `PersonalSavingsState.selectedTab` (mirrors `ui.yaml`'s inline ternary binding
 * `{{selectedTab == GROUP_LINKED ? groupLinkedBalance : individualBalance}}`).
 */
@Composable
fun SavingsBalanceHeroCard(tab: SavingsTab, balance: Double, accountId: Long?, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val labelText = when (tab) {
        SavingsTab.GROUP_LINKED -> stringResource(Res.string.screens_personal_savings_group_linked_balance_label)
        SavingsTab.INDIVIDUAL -> stringResource(Res.string.screens_personal_savings_individual_balance_label)
    }
    val amountText = stringResource(Res.string.screens_personal_savings_balance_amount_format, balance.formatGrouped(0))
    val accountText = stringResource(Res.string.screens_personal_savings_account_number_format, accountId?.toString() ?: "-")
    val cardCd = stringResource(Res.string.screens_personal_savings_balance_card_cd, labelText, balance.formatGrouped(0))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(PersonalSavingsTestTags.BALANCE_CARD)
            .semantics(mergeDescendants = true) { contentDescription = cardCd },
    ) {
        Column(modifier = Modifier.padding(sp.xl)) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = sp.sm),
            )
            Text(
                text = accountText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = sp.sm),
            )
        }
    }
}
