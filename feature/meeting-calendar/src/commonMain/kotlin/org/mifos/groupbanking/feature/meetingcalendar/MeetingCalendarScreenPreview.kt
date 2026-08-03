/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingcalendar

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.MeetingListItem
import org.mifos.groupbanking.core.model.MeetingStatus

/**
 * `@Preview` gallery for `MeetingCalendarScreen.kt`. Data source: `demo-data.yaml` (Mwangaza
 * Women's Group weekly-Tuesday cycle — 4 past meetings + 1 upcoming). See API.md#preview.
 */
private val previewMeetings: List<MeetingListItem> = listOf(
    MeetingListItem("MTG-2026-07-21", 5, "2026-07-21", MeetingStatus.UPCOMING),
    MeetingListItem("MTG-2026-07-14", 4, "2026-07-14", MeetingStatus.COMPLETED, attendanceCount = 5, totalCollectedKES = 2500),
    MeetingListItem("MTG-2026-07-07", 3, "2026-07-07", MeetingStatus.MISSED),
    MeetingListItem("MTG-2026-06-30", 2, "2026-06-30", MeetingStatus.COMPLETED, attendanceCount = 4, totalCollectedKES = 1900),
    MeetingListItem("MTG-2026-06-23", 1, "2026-06-23", MeetingStatus.COMPLETED, attendanceCount = 5, totalCollectedKES = 2350),
)

private class MeetingCalendarStatePreviewProvider : PreviewParameterProvider<MeetingCalendarState> {
    override val values: Sequence<MeetingCalendarState> = sequenceOf(
        MeetingCalendarState(isLoading = true, meetings = emptyList(), error = null),
        MeetingCalendarState(isLoading = false, meetings = previewMeetings, error = null),
        MeetingCalendarState(isLoading = false, meetings = emptyList(), error = null),
        MeetingCalendarState(isLoading = false, meetings = emptyList(), error = MeetingCalendarError.Network),
    )
}

@Preview
@Composable
private fun MeetingCalendarContentPreview(
    @PreviewParameter(MeetingCalendarStatePreviewProvider::class)
    state: MeetingCalendarState,
) {
    KptTheme {
        MeetingCalendarContent(state = state, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun MeetingCalendarLoadingSectionPreview() {
    KptTheme { MeetingCalendarLoadingSection() }
}

@Preview
@Composable
private fun MeetingCalendarEmptySectionPreview() {
    KptTheme {
        MeetingCalendarEmptySection(state = MeetingCalendarState(isLoading = false), onAction = {})
    }
}

@Preview
@Composable
private fun MeetingCalendarErrorSectionPreview() {
    KptTheme {
        MeetingCalendarErrorSection(message = "No internet connection. Showing cached meetings.", onRetry = {})
    }
}
