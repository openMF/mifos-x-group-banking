/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingconduct

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/meetings/{meetingId}/conduct` — the meeting-conduct wizard route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { meetingId, meetingNumber, centerId }`). Forwarded from `meeting-calendar`'s
 * "Start Meeting" entry point (`flow.yaml#entry_points[0]`) and seeds [MeetingConductViewModel] via
 * Koin `parametersOf(meetingId, meetingNumber, centerId)`. See API.md#route.
 */
@Serializable
data class MeetingConductRoute(
    val meetingId: String,
    val meetingNumber: Int,
    val centerId: Int,
)

fun NavController.navigateToMeetingConduct(
    meetingId: String,
    meetingNumber: Int,
    centerId: Int,
    navOptions: NavOptions? = null,
) = navigate(
    MeetingConductRoute(meetingId = meetingId, meetingNumber = meetingNumber, centerId = centerId),
    navOptions,
)

/**
 * Registers [MeetingConductScreen] on the host [NavGraphBuilder]. Every callback closes a
 * [MeetingConductEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3), matching `flow.yaml#navigates_to` (`meeting-summary`, `previous-meeting-review`,
 * `meeting-calendar`). No callback carries a `= {}` default. See API.md#route.
 */
fun NavGraphBuilder.meetingConductScreen(
    onNavigateToMeetingSummary: (meetingId: String, meetingNumber: Int, centerId: Int) -> Unit,
    onNavigateToPreviousMeetingReview: (meetingId: String, centerId: Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<MeetingConductRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MeetingConductRoute>()
        MeetingConductScreen(
            meetingId = route.meetingId,
            meetingNumber = route.meetingNumber,
            centerId = route.centerId,
            onNavigateToMeetingSummary = onNavigateToMeetingSummary,
            onNavigateToPreviousMeetingReview = onNavigateToPreviousMeetingReview,
            onNavigateBack = onNavigateBack,
        )
    }
}
