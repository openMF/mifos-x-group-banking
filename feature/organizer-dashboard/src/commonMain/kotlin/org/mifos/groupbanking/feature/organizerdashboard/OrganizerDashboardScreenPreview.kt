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

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.OrganizerActivityItem
import org.mifos.groupbanking.core.model.OrganizerActivityType
import org.mifos.groupbanking.core.model.ScheduledMeeting

/**
 * `@Preview` gallery for `OrganizerDashboardScreen.kt`. Data source: `demo-data.yaml` /
 * `ui.yaml#states.content.demo_data` seeded rows (Mwangaza Women's Group / Tumaini Savings Group).
 * See API.md#preview.
 */
private val previewSchedule: List<ScheduledMeeting> = listOf(
    ScheduledMeeting("ctr-001", "Mwangaza Women's Group", "09:00", 12, "Community Hall, Kisumu"),
    ScheduledMeeting("ctr-002", "Tumaini Savings Group", "14:00", 8, "St. Mary's Hall, Kisumu"),
)

private val previewActivity: List<OrganizerActivityItem> = listOf(
    OrganizerActivityItem("act-001", OrganizerActivityType.DEPOSIT, "Weekly contribution — Amina Wanjiru", 300.0, "2026-05-09", "Amina Wanjiru", "Mwangaza Women's Group"),
    OrganizerActivityItem("act-002", OrganizerActivityType.LOAN, "Loan disbursed — Grace Akinyi", 8000.0, "2026-05-08", "Grace Akinyi", "Tumaini Savings Group"),
    OrganizerActivityItem("act-003", OrganizerActivityType.NEW_MEMBER, "New member joined", null, "2026-05-05", "John Odhiambo", "Baraka Chama"),
)

private fun contentState(fieldOfficerEnabled: Boolean = false): OrganizerDashboardState = OrganizerDashboardState(
    isLoading = false,
    organizerName = "David Otieno",
    myGroupCount = 5,
    totalMembers = 42,
    pendingShareOutCount = 1,
    meetingsTodayCount = 2,
    fieldOfficerEnabled = fieldOfficerEnabled,
    todaySchedule = previewSchedule,
    recentActivity = previewActivity,
)

private class OrganizerStatePreviewProvider : PreviewParameterProvider<OrganizerDashboardState> {
    override val values: Sequence<OrganizerDashboardState> = sequenceOf(
        OrganizerDashboardState(isLoading = true),
        contentState(),
        contentState(fieldOfficerEnabled = true),
        OrganizerDashboardState(isLoading = false, myGroupCount = 0),
        OrganizerDashboardState(isLoading = false, error = OrganizerDashboardError.Network),
    )
}

@Preview
@Composable
private fun OrganizerDashboardContentPreview(
    @PreviewParameter(OrganizerStatePreviewProvider::class)
    state: OrganizerDashboardState,
) {
    KptTheme {
        OrganizerDashboardContent(state = state, onAction = {})
    }
}
