/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.organizerdashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.core.model.OrganizerActivityItem
import org.mifos.groupbanking.core.model.OrganizerActivityType
import org.mifos.groupbanking.core.model.ScheduledMeeting

// ---------------------------------------------------------------------------
// KPI summary cards — ui.yaml#components.kpi_summary_row (4 cards; share-out card error-accents
// when its count > 0).
// ---------------------------------------------------------------------------

/** One KPI datum — value + label + icon + a flag driving the nonzero error accent. */
internal data class OrganizerKpiCardData(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val emphasizeNonZero: Boolean = false,
    val nonZero: Boolean = false,
)

/**
 * One tappable KPI card (`ui.yaml#components.kpi_summary_row.*`). All four cards navigate to
 * group-list on tap ([onClick]). When [OrganizerKpiCardData.emphasizeNonZero] and
 * [OrganizerKpiCardData.nonZero] are both set (the Share-Outs Due card with count > 0) the value +
 * border switch to the error color (`ui.yaml`: `color_nonzero: error`, `border_overdue`).
 */
@Composable
internal fun OrganizerKpiCard(
    data: OrganizerKpiCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val nonZeroAccent = data.emphasizeNonZero && data.nonZero
    val valueColor = if (nonZeroAccent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val borderColor = if (nonZeroAccent) MaterialTheme.colorScheme.error else Color.Transparent
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (nonZeroAccent) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .heightIn(min = 92.dp)
            .clickable(onClick = onClick)
            .testTag(data.testTag),
    ) {
        Column(
            modifier = Modifier.padding(sp.md),
            verticalArrangement = Arrangement.spacedBy(sp.xs),
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = data.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Icons for the 4 KPI cards, in row order (groups / members / share-outs / meetings). */
internal val KpiGroupsIcon: ImageVector = Icons.Filled.Groups
internal val KpiMembersIcon: ImageVector = Icons.Filled.Person
internal val KpiShareOutIcon: ImageVector = Icons.Filled.MonetizationOn
internal val KpiMeetingsIcon: ImageVector = Icons.Filled.EventAvailable

// ---------------------------------------------------------------------------
// Quick-nav tiles — ui.yaml#components.quick_nav_section.quick_nav_grid
// ---------------------------------------------------------------------------

/**
 * One quick-navigation tile (`ui.yaml#components.quick_nav_grid.*_card`): icon + label + sublabel on
 * a colored container, whole tile a single ≥56dp touch target dispatching [onClick]. Optional
 * [badgeLabel] renders the "Optional" chip on the field-officer tile.
 */
@Composable
internal fun OrganizerQuickNavTile(
    icon: ImageVector,
    label: String,
    sublabel: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    badgeLabel: String? = null,
) {
    val sp = MaterialTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(imageVector = icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(28.dp))
                if (badgeLabel != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                        )
                    }
                }
            }
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = onContainer)
            Text(text = sublabel, style = MaterialTheme.typography.bodySmall, color = onContainer)
        }
    }
}

/** Icons for the quick-nav tiles. */
internal val NavGroupsIcon: ImageVector = Icons.Filled.Groups
internal val NavFieldOfficerIcon: ImageVector = Icons.Filled.PersonPinCircle

// ---------------------------------------------------------------------------
// Today's Schedule row — ui.yaml#components.todays_schedule_section.schedule_list_item
// ---------------------------------------------------------------------------

/**
 * One Today's-Schedule row (`ui.yaml#components.schedule_list_item`, repeats over `todaySchedule`):
 * calendar leading icon, group name headline, "time · N members" supporting line, trailing chevron.
 * The whole row is a single merged-semantics ≥56dp touch target dispatching [onClick]. [supporting]
 * / [rowCd] are pre-resolved by the caller so this component takes no `stringResource` dependency.
 */
@Composable
internal fun ScheduleRow(
    meeting: ScheduledMeeting,
    supporting: String,
    rowCd: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .semantics(mergeDescendants = true) { contentDescription = rowCd }
            .padding(vertical = sp.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(text = meeting.groupName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Recent Activity row — ui.yaml#components.recent_activity_section.activity_list_item
// ---------------------------------------------------------------------------

/**
 * One Recent-Activity row (`ui.yaml#components.activity_list_item`, repeats over `recentActivity`):
 * type-driven leading icon, description headline, "date · groupName" supporting line, trailing
 * KES-formatted amount (blank for non-monetary rows). Non-interactive (`interaction_type: static`).
 * [supporting] is pre-resolved by the caller.
 */
@Composable
internal fun ActivityRow(
    activity: OrganizerActivityItem,
    supporting: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
            .padding(vertical = sp.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        Icon(
            imageVector = activity.type.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Text(text = activity.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        activity.amount?.let { amount ->
            Text(
                text = "KES ${formatAmount(amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Type -> leading-icon mapping for the activity feed (`api.yaml#dtos.ActivityItem.type`). */
private fun OrganizerActivityType.icon(): ImageVector = when (this) {
    OrganizerActivityType.DEPOSIT -> Icons.Filled.Savings
    OrganizerActivityType.LOAN -> Icons.Filled.MonetizationOn
    OrganizerActivityType.SHARE_OUT -> Icons.Filled.MonetizationOn
    OrganizerActivityType.NEW_MEMBER -> Icons.Filled.Person
    OrganizerActivityType.MEETING -> Icons.Filled.EventAvailable
    OrganizerActivityType.UNKNOWN -> Icons.Filled.EventAvailable
}

/**
 * KMP-safe thousands-grouped 2-decimal amount formatter (e.g. `24000.0` -> `"24,000.00"`).
 * `String.format` is JVM-only, so this is hand-rolled — mirrors `core/common`'s formatter rationale.
 */
internal fun formatAmount(value: Double): String {
    val negative = value < 0
    val cents = kotlin.math.round(kotlin.math.abs(value) * 100).toLong()
    val whole = cents / 100
    val frac = cents % 100
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    val fracStr = frac.toString().padStart(2, '0')
    return (if (negative) "-" else "") + "$grouped.$fracStr"
}
