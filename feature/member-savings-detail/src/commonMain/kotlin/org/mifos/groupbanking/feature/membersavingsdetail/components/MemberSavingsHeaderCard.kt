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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.SavingsMember
import org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsDetailTestTags
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.Res
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_account_no_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_avatar_cd_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_balance_amount_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_balance_label_savings
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_balance_label_shares
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_shares_at_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_shares_total_format

private val AVATAR_SIZE = 48.dp

/**
 * `ui.yaml#components.member_header_card` — avatar + name + contribution-model-adaptive
 * balance display + account number. `contributionModel` is compared against
 * [ContributionMode.SHARE_BASED_VARIABLE]'s `.name` (the [org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsDetailState.contributionModel]
 * field is a `String`, derived from `typeConfig.contributionMode.name` at construction — see that
 * state's KDoc "contributionModel value-set" note).
 *
 * **`shares_total_label` — documented derivation, not a literal ui.yaml re-interpolation:**
 * `ui.yaml#components.member_header_card.content.savings_balance_total.value` literally
 * re-interpolates `{{shareValue}}` (per-share KES value) for the "= KES {total} total" line, which
 * would render the wrong number (the per-share price, not the member's total share value). This
 * component instead computes the actual total as `sharesHeld * shareValue` — the only reading that
 * makes "= KES X total" true — flagged for the cross-feature repair station (CFF1) rather than
 * silently reproducing the wrong literal, same "flag, don't blindly copy" convention this feature's
 * ViewModel already established for its own confirmed idea-layer drifts.
 *
 * See API.md#screen.
 */
@Composable
fun MemberSavingsHeaderCard(
    member: SavingsMember,
    contributionModel: String,
    sharesHeld: Int?,
    shareValue: Long?,
    savingsBalance: Double,
    savingsAccountNo: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val isShareBased = contributionModel == ContributionMode.SHARE_BASED_VARIABLE.name
    val avatarCd = stringResource(Res.string.screens_member_savings_detail_avatar_cd_format, member.displayName)
    val balanceLabel = stringResource(
        if (isShareBased) {
            Res.string.screens_member_savings_detail_balance_label_shares
        } else {
            Res.string.screens_member_savings_detail_balance_label_savings
        },
    )
    val balanceAmountText = if (isShareBased && sharesHeld != null && shareValue != null) {
        stringResource(Res.string.screens_member_savings_detail_shares_at_format, sharesHeld, shareValue.formatGrouped())
    } else {
        stringResource(Res.string.screens_member_savings_detail_balance_amount_format, savingsBalance.formatGrouped(0))
    }
    val sharesTotalText = if (isShareBased && sharesHeld != null && shareValue != null) {
        stringResource(Res.string.screens_member_savings_detail_shares_total_format, (sharesHeld * shareValue).formatGrouped())
    } else {
        null
    }
    val accountNoText = stringResource(Res.string.screens_member_savings_detail_account_no_format, savingsAccountNo)

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(sp.lg),
        modifier = modifier.fillMaxWidth().testTag(MemberSavingsDetailTestTags.HEADER_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            MemberAvatar(displayName = member.displayName, avatarCd = avatarCd)
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.sm),
            )
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.md),
            )
            Text(
                text = balanceAmountText,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (sharesTotalText != null) {
                Text(
                    text = sharesTotalText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = accountNoText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = sp.sm),
            )
        }
    }
}

/** `ui.yaml#components.member_header_card.content.member_avatar` — photoUri is never populated for [SavingsMember] on this companion read (fallback initials only, per `Savings.kt`'s KDoc). */
@Composable
private fun MemberAvatar(displayName: String, avatarCd: String, modifier: Modifier = Modifier) {
    val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")

    Box(
        modifier = modifier
            .size(AVATAR_SIZE)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = avatarCd }
            .testTag(MemberSavingsDetailTestTags.AVATAR),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}
