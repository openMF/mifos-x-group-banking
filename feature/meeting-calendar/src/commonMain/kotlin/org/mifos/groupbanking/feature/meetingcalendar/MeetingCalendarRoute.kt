/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingcalendar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithPushTransitions

/**
 * `/meetings` — the calendar/list of a group center's scheduled + past meetings
 * (`ui.yaml#route`, `ui.yaml#nav_params: { center_id }`), entered from `group-dashboard`'s
 * "Meetings" quick action or `bottom_nav`. See API.md#route.
 */
@Serializable
data class MeetingCalendarRoute(val centerId: Int)

fun NavController.navigateToMeetingCalendar(centerId: Int, navOptions: NavOptions? = null) =
    navigate(MeetingCalendarRoute(centerId = centerId), navOptions)

/**
 * Registers [MeetingCalendarScreen] on the host [NavGraphBuilder]. Both onward callbacks close a
 * [MeetingCalendarEvent] navigation branch consumed by the Container
 * (`flow.yaml#navigates_to`: `meeting-conduct`, `previous-meeting-review`) plus [onNavigateBack], a
 * plain nav-pop callback for the top-bar back affordance. Neither `meeting-conduct` nor
 * `previous-meeting-review` is yet a generated feature module in this codebase — typed nav-arg
 * contracts for the caller to wire, mirroring `LoanListRoute.kt`'s identical
 * not-yet-generated-target convention. See API.md#route.
 */
fun NavGraphBuilder.meetingCalendarScreen(
    onNavigateToConduct: (meetingId: String, meetingNumber: Int) -> Unit,
    onNavigateToReview: (meetingId: String, meetingNumber: Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<MeetingCalendarRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MeetingCalendarRoute>()
        MeetingCalendarScreen(
            centerId = route.centerId,
            onNavigateToConduct = onNavigateToConduct,
            onNavigateToReview = onNavigateToReview,
            onNavigateBack = onNavigateBack,
        )
    }
}
