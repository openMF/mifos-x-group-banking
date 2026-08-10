/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingsummary

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/meetings/{meetingId}/summary` — the meeting-summary route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { meeting_id, meeting_number, group_id }`). All three params are carried;
 * [groupId] + [meetingNumber] scope the single-key Store5 read, [meetingId] is used for
 * display/analytics. See API.md#route.
 */
@Serializable
data class MeetingSummaryRoute(
    val meetingId: String,
    val meetingNumber: Int,
    val groupId: Int,
)

fun NavController.navigateToMeetingSummary(
    meetingId: String,
    meetingNumber: Int,
    groupId: Int,
    navOptions: NavOptions? = null,
) = navigate(
    MeetingSummaryRoute(meetingId = meetingId, meetingNumber = meetingNumber, groupId = groupId),
    navOptions,
)

/**
 * Registers [MeetingSummaryScreen] on the host [NavGraphBuilder]. [onNavigateDone] closes the single
 * [MeetingSummaryEvent.NavigateToCalendar] navigation branch consumed by the Container, matching
 * `flow.yaml#navigates_to: [meeting-calendar]`. See API.md#route.
 */
fun NavGraphBuilder.meetingSummaryScreen(
    onNavigateDone: () -> Unit,
) {
    composableWithRootPushTransitions<MeetingSummaryRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MeetingSummaryRoute>()
        MeetingSummaryScreen(
            meetingId = route.meetingId,
            meetingNumber = route.meetingNumber,
            groupId = route.groupId,
            onNavigateDone = onNavigateDone,
        )
    }
}
