/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalLayoutApi::class)

package kpt.feature.grouptypepicker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import kpt.core.model.GroupTypeConfig

/**
 * One selectable group-type card — `ui.yaml#components.type_card_*` (9 instances, one per
 * [GroupTypeConfig] returned by COMP-DT-003). [config].displayName / [config].tagline are
 * data-bound (dynamic, sourced from the API/cache — not a hardcoded literal); [icon] and
 * [features] are the static per-type catalogue resolved by [groupTypeIcon] / [groupTypeFeatureChips].
 * The whole row is a single merged-semantics touch target (≥48dp, RULE-FEATURE-A11Y-001) labelled
 * with [config].displayName; the leading icon and trailing chevron are decorative. See API.md#screen.
 */
@Composable
fun GroupTypeCard(
    config: GroupTypeConfig,
    icon: ImageVector,
    features: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    val sp = MaterialTheme.spacing
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = config.displayName },
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(sp.md)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    // Decorative — the card's merged semantics above already carries the
                    // accessible label (config.displayName). See GroupTypeVisuals#groupTypeIcon KDoc.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = config.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = config.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clearAndSetSemantics {},
            )
        }

        if (features.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = sp.sm, start = 44.dp + sp.md),
                horizontalArrangement = Arrangement.spacedBy(sp.xs),
                verticalArrangement = Arrangement.spacedBy(sp.xs),
            ) {
                features.forEach { feature ->
                    // Decorative pill — deliberately NOT clickable. A disabled AssistChip still
                    // registers a pointer/clickable node that SWALLOWED taps landing on the card's
                    // centre, so the card's own .clickable (onClick, above) never fired → dead card
                    // nav to group-create (device-truth A/B capture 2026-07-31, RULE-IMPL-DEAD-CLICKABLE-001).
                    // A plain Box has no pointer node, so taps fall through to the card.
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = sp.sm, vertical = sp.xs),
                    ) {
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
