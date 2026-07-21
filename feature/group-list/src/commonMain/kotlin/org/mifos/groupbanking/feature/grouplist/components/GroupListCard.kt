/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.Group
import org.mifos.groupbanking.feature.grouplist.generated.resources.Res
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_card_cd
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_cycle_label
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_last_met
import org.mifos.groupbanking.feature.grouplist.generated.resources.screens_group_list_member_count

/**
 * One group row — `ui.yaml#components.group_card` (mirrors `preview/content.html` +
 * `SPEC.md#screens.group-list`). Displays name, group-type chip, viewer-role badge, cycle +
 * member count, last-meeting date, and a traffic-light [org.mifos.groupbanking.core.model
 * .HealthIndicator] badge — both as a badge chip AND as the card's left accent stripe (see
 * `HealthIndicatorVisuals.kt`'s [accentColor] extension) for a stronger at-a-glance signal than
 * the mockup's badge-only treatment. The whole row is a single merged-semantics ≥48dp touch target
 * (RULE-FEATURE-A11Y-001) dispatching [onClick]; `group.groupType`/`group.viewerRole` render via
 * their raw enum `.name` (dynamic domain data, not a hardcoded literal — `GroupTypeConfig`'s
 * humanized `displayName` is not attached to this list-row `Group` shape; flagged as an
 * idea-layer follow-up rather than invented here). See API.md#screen.
 */
@Composable
fun GroupListCard(group: Group, onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String = "") {
    val sp = MaterialTheme.spacing
    val cardCd = stringResource(
        Res.string.screens_group_list_card_cd,
        group.name,
        group.cycleNumber,
        group.memberCount,
        group.healthIndicator.name,
    )
    val cycleLabel = stringResource(Res.string.screens_group_list_cycle_label, group.cycleNumber)
    val memberLabel = stringResource(Res.string.screens_group_list_member_count, group.memberCount)
    val lastMetLabel = stringResource(Res.string.screens_group_list_last_met, group.lastMeetingDate.toString())

    AppCard(
        accentColor = group.healthIndicator.accentColor(),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardCd },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)

                Surface(
                    color = group.healthIndicator.badgeContainerColor(),
                    contentColor = group.healthIndicator.badgeContentColor(),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = group.healthIndicator.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = cycleLabel, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = null,
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = group.groupType.name, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = null,
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = group.viewerRole.name, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    border = null,
                )
            }

            Text(
                text = memberLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = lastMetLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Shimmering skeleton row shown while `GroupListScreenState.Loading` — mirrors
 * `preview/loading.html` (title + chip row + two supporting lines, animated opacity in lieu of a
 * CSS shimmer gradient). Purely decorative — no semantics/testTag of its own; the surrounding
 * [kpt.core.ui.scaffold.KptScaffold] loading region carries the single accessible "loading"
 * announcement via [org.mifos.groupbanking.feature.grouplist.GroupListTestTags.LOADING_INDICATOR].
 * See API.md#screen.
 */
@Composable
fun GroupListCardSkeleton(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val transition = rememberInfiniteTransition(label = "group-list-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "group-list-skeleton-alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f)

    AppCard(modifier = modifier.fillMaxWidth().heightIn(min = 88.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
            Row(horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(skeletonColor),
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
        }
    }
}
