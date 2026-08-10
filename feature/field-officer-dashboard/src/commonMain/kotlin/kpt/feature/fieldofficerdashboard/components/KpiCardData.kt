/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.fieldofficerdashboard.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One KPI datum for the field-officer-dashboard summary row — value + label + icon + the MD3
 * container/onContainer color pair. Rendered by the KPI card composables in
 * `FieldOfficerDashboardComponents.kt`.
 */
internal data class KpiCardData(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val onContainer: Color,
)
