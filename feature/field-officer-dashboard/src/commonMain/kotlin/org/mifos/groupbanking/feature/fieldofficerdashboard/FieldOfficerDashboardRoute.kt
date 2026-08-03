/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/field-officer/dashboard` — the field officer's read-only cross-group monitoring dashboard
 * (FR-009, `SPEC.md#screens.field-officer-dashboard`). See API.md#route.
 */
@Serializable
data object FieldOfficerDashboardRoute

fun NavController.navigateToFieldOfficerDashboard(navOptions: NavOptions? = null) =
    navigate(FieldOfficerDashboardRoute, navOptions)

/**
 * Registers [FieldOfficerDashboardScreen] on the host [NavGraphBuilder]. [onNavigateToGroupDashboard]
 * closes the group-health-card tap branch (the field officer opens a group in read-only supervisory
 * mode); [onExportReport] closes the Export Report branch (the OS share sheet handoff for the CSV).
 * See API.md#route.
 */
fun NavGraphBuilder.fieldOfficerDashboardScreen(
    onNavigateToGroupDashboard: (groupId: Long) -> Unit,
    onExportReport: (staffId: Long) -> Unit,
) {
    composableWithRootPushTransitions<FieldOfficerDashboardRoute> {
        FieldOfficerDashboardScreen(
            onNavigateToGroupDashboard = onNavigateToGroupDashboard,
            onExportReport = onExportReport,
        )
    }
}
