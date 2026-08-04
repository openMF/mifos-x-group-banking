/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.organizerdashboard.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One KPI datum for the organizer-dashboard summary row (`ui.yaml#components.kpi_summary_row`) —
 * value + label + icon + the flag pair driving the nonzero error accent (the Share-Outs Due card).
 * Rendered by [OrganizerKpiCard] in `OrganizerDashboardComponents.kt`.
 */
internal data class OrganizerKpiCardData(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val emphasizeNonZero: Boolean = false,
    val nonZero: Boolean = false,
)
