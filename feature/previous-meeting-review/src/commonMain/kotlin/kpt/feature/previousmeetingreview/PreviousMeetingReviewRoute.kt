/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.previousmeetingreview

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions

/**
 * `/meetings/{previousMeetingId}/review` — the previous-meeting-review route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { meeting_id, meeting_number, group_id, launched_from }`). All four params
 * are carried; [groupId] + [meetingNumber] scope the reused record read, [meetingId] scopes the
 * attendance read + display/analytics, [launchedFrom] toggles the context banner + Start-Meeting CTA
 * (`conduct` vs `calendar`). See API.md#route.
 */
@Serializable
data class PreviousMeetingReviewRoute(
    val meetingId: String,
    val meetingNumber: Int,
    val groupId: Int,
    val launchedFrom: String,
)

fun NavController.navigateToPreviousMeetingReview(
    meetingId: String,
    meetingNumber: Int,
    groupId: Int,
    launchedFrom: String,
    navOptions: NavOptions? = null,
) = navigate(
    PreviousMeetingReviewRoute(
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        groupId = groupId,
        launchedFrom = launchedFrom,
    ),
    navOptions,
)

/**
 * Registers [PreviousMeetingReviewScreen] on the host [NavGraphBuilder]. [onNavigateBack] pops the
 * screen (top-bar back + `NavigateBack` event); [onNavigateToConduct] forwards to meeting-conduct to
 * start the next meeting (the `StartNewMeeting` CTA, `flow.yaml#navigates_to: [meeting-conduct]`,
 * only reachable when `launchedFrom == conduct`). See API.md#route.
 */
fun NavGraphBuilder.previousMeetingReviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConduct: (meetingId: String, meetingNumber: Int, groupId: Int) -> Unit,
) {
    composableWithRootPushTransitions<PreviousMeetingReviewRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PreviousMeetingReviewRoute>()
        PreviousMeetingReviewScreen(
            meetingId = route.meetingId,
            meetingNumber = route.meetingNumber,
            groupId = route.groupId,
            launchedFrom = route.launchedFrom,
            onNavigateBack = onNavigateBack,
            onNavigateToConduct = onNavigateToConduct,
        )
    }
}
