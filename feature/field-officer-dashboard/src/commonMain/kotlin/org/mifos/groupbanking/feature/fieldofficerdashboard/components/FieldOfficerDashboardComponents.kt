/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.HealthIndicator
import org.mifos.groupbanking.feature.fieldofficerdashboard.FieldOfficerDashboardTestTags

// ---------------------------------------------------------------------------
// KPI summary cards — ui.yaml#components.kpi_cards_row (4 cards, distinct MD3 container tokens)
// ---------------------------------------------------------------------------

/**
 * Horizontally-scrollable row of 4 non-interactive KPI cards, each using its declared MD3 container
 * token (primaryContainer / secondaryContainer / tertiaryContainer / errorContainer) —
 * `ui.yaml#components.kpi_cards_row`.
 */
@Composable
internal fun KpiCardsRow(cards: List<KpiCardData>, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = sp.lg)
            .testTag(FieldOfficerDashboardTestTags.KPI_ROW),
        horizontalArrangement = Arrangement.spacedBy(sp.sm),
    ) {
        cards.forEach { KpiCard(it) }
    }
}

@Composable
private fun KpiCard(data: KpiCardData) {
    val sp = MaterialTheme.spacing
    Card(
        colors = CardDefaults.cardColors(containerColor = data.container, contentColor = data.onContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(min = 140.dp).heightIn(min = 88.dp),
    ) {
        Column(modifier = Modifier.padding(sp.md), verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Icon(imageVector = data.icon, contentDescription = null, tint = data.onContainer, modifier = Modifier.size(20.dp))
            Text(text = data.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = data.onContainer)
            Text(text = data.label, style = MaterialTheme.typography.bodySmall, color = data.onContainer)
        }
    }
}

/** Icons for the 4 KPI cards, in row order (groups / members / savings / loans). */
internal val KpiGroupsIcon: ImageVector = Icons.Filled.Groups
internal val KpiMembersIcon: ImageVector = Icons.Filled.Person
internal val KpiSavingsIcon: ImageVector = Icons.Filled.Savings
internal val KpiLoansIcon: ImageVector = Icons.Filled.AccountBalance

/** Icons for the filter chips. */
internal val RegionFilterIcon: ImageVector = Icons.Filled.LocationOn
internal val StatusFilterIcon: ImageVector = Icons.Filled.FilterList
internal val OverdueFilterIcon: ImageVector = Icons.Filled.Warning
internal val ClearFilterIcon: ImageVector = Icons.Filled.Clear

/**
 * KMP-safe compact currency-ish formatter (e.g. `142000.0` -> `"142.0K"`, `1_500_000.0` -> `"1.5M"`).
 * `String.format` is JVM-only, so this is hand-rolled — mirrors `core/common`'s formatter rationale.
 */
internal fun formatCompact(value: Double): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> "$sign${oneDecimal(abs / 1_000_000)}M"
        abs >= 1_000 -> "$sign${oneDecimal(abs / 1_000)}K"
        else -> "$sign${oneDecimal(abs)}"
    }
}

private fun oneDecimal(v: Double): String {
    val scaled = (v * 10).toLong()
    val whole = scaled / 10
    val frac = scaled % 10
    return "$whole.$frac"
}

// ---------------------------------------------------------------------------
// Filter chip row — ui.yaml#components.filter_row
// ---------------------------------------------------------------------------

/**
 * One filter chip — tapping it opens the corresponding picker dialog (via [onClick]); [selected]
 * reflects an active filter. Mirrors `ui.yaml#components.filter_row.*_filter_chip`.
 */
@Composable
internal fun FieldOfficerFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier.testTag(testTag),
    )
}

// ---------------------------------------------------------------------------
// Group health card — ui.yaml#components.group_health_card
// ---------------------------------------------------------------------------

/**
 * One group-health row (`ui.yaml#components.group_health_card`, repeats over `filteredGroups`):
 * group name, office + cycle, and a traffic-light [HealthIndicator] badge, both as a badge chip and
 * as the card's left accent stripe. The whole row is a single merged-semantics >=72dp touch target
 * dispatching [onClick]. [cardCd] / [subtitle] / [healthLabel] are pre-resolved by the caller so
 * this component takes no `stringResource` dependency (keeps it preview-friendly).
 */
@Composable
internal fun GroupHealthCard(
    group: GroupHealthSummary,
    cardCd: String,
    subtitle: String,
    healthLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
) {
    val sp = MaterialTheme.spacing
    AppCard(
        accentColor = group.healthIndicator.accentColor(),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardCd },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                Surface(
                    color = group.healthIndicator.badgeContainerColor(),
                    contentColor = group.healthIndicator.badgeContentColor(),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = healthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = sp.sm, vertical = sp.xs),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Health traffic-light visuals — mirrors group-list's HealthIndicatorVisuals.kt
// ---------------------------------------------------------------------------

@Composable
internal fun HealthIndicator.badgeContainerColor(): Color = when (this) {
    HealthIndicator.GREEN -> MaterialTheme.colorScheme.primaryContainer
    HealthIndicator.AMBER -> Color(0xFFFFF9C4)
    HealthIndicator.RED -> MaterialTheme.colorScheme.errorContainer
    HealthIndicator.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
internal fun HealthIndicator.badgeContentColor(): Color = when (this) {
    HealthIndicator.GREEN -> MaterialTheme.colorScheme.onPrimaryContainer
    HealthIndicator.AMBER -> Color(0xFFE65100)
    HealthIndicator.RED -> MaterialTheme.colorScheme.onErrorContainer
    HealthIndicator.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun HealthIndicator.accentColor(): Color = when (this) {
    HealthIndicator.GREEN -> Color(0xFF2E7D32)
    HealthIndicator.AMBER -> Color(0xFFFF8F00)
    HealthIndicator.RED -> MaterialTheme.colorScheme.error
    HealthIndicator.UNKNOWN -> MaterialTheme.colorScheme.outline
}
