/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.organizerdashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/dashboard/organizer` — the organizer-per-group hub surface reached by any user whose
 * `dt_member_role` resolves an organizer/treasurer/chairperson role in at least one group
 * (`ui.yaml#route`). See API.md#route.
 */
@Serializable
data object OrganizerDashboardRoute

fun NavController.navigateToOrganizerDashboard(navOptions: NavOptions? = null) =
    navigate(OrganizerDashboardRoute, navOptions)

/**
 * Registers [OrganizerDashboardScreen] on the host [NavGraphBuilder]. [onNavigateToGroupList] closes
 * the KPI-card / All-Groups quick-nav / meeting-row / empty-state CTA taps (all target group-list);
 * [onNavigateToFieldOfficerDashboard] closes the optional-tier field-officer quick-nav tap. See
 * API.md#route.
 */
fun NavGraphBuilder.organizerDashboardScreen(
    onNavigateToGroupList: () -> Unit,
    onNavigateToFieldOfficerDashboard: () -> Unit,
) {
    composableWithRootPushTransitions<OrganizerDashboardRoute> {
        OrganizerDashboardScreen(
            onNavigateToGroupList = onNavigateToGroupList,
            onNavigateToFieldOfficerDashboard = onNavigateToFieldOfficerDashboard,
        )
    }
}
