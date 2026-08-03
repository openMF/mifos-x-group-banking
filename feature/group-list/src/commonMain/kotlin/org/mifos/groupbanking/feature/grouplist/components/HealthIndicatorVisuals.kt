/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouplist.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.mifos.groupbanking.core.model.HealthIndicator

/**
 * Traffic-light visual tokens for [HealthIndicator] — mirrors
 * `idea-layer/exports/group-management/SPEC.md#design-tokens` (GREEN badge bg=#C8E6C9
 * text=#1B5E20; AMBER badge bg=#FFF9C4 text=#E65100) plus `MaterialTheme.colorScheme.error` /
 * `errorContainer` for RED (already themed, no hardcoded hex needed there). [UNKNOWN] falls back
 * to the neutral `surfaceVariant` pairing. Used both for the badge chip container/content colors
 * and the [kpt.core.base.designsystem.component.AppCard] left accent stripe. See API.md#screen.
 */
@Composable
fun HealthIndicator.badgeContainerColor(): Color = when (this) {
    HealthIndicator.GREEN -> Color(0xFFC8E6C9)
    HealthIndicator.AMBER -> Color(0xFFFFF9C4)
    HealthIndicator.RED -> MaterialTheme.colorScheme.errorContainer
    HealthIndicator.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun HealthIndicator.badgeContentColor(): Color = when (this) {
    HealthIndicator.GREEN -> Color(0xFF1B5E20)
    HealthIndicator.AMBER -> Color(0xFFE65100)
    HealthIndicator.RED -> MaterialTheme.colorScheme.onErrorContainer
    HealthIndicator.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Left-edge card accent stripe — a saturated variant of the badge color. */
@Composable
fun HealthIndicator.accentColor(): Color = when (this) {
    HealthIndicator.GREEN -> Color(0xFF2E7D32)
    HealthIndicator.AMBER -> Color(0xFFFF8F00)
    HealthIndicator.RED -> MaterialTheme.colorScheme.error
    HealthIndicator.UNKNOWN -> MaterialTheme.colorScheme.outline
}
