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

/**
 * Append-only test-tag registry for the `meeting-calendar` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Mirrors `LoanListTestTags`'s identical convention. See API.md#tags.
 */
object MeetingCalendarTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "meeting_calendar_screen"

    /** `toggle_view_btn` in the top bar — [MeetingCalendarAction.ToggleViewMode]. */
    const val TOGGLE_VIEW_BUTTON: String = "meeting_calendar_toggle_view_button"

    /** Circular loading indicator + skeleton rows — `MeetingCalendarScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "meeting_calendar_loading_indicator"

    /** Pinned upcoming-meeting card — visible when an UPCOMING meeting exists. */
    const val UPCOMING_CARD: String = "meeting_calendar_upcoming_card"

    /** "Start Meeting" CTA on the upcoming card — [MeetingCalendarAction.StartMeeting]. */
    const val START_MEETING_BUTTON: String = "meeting_calendar_start_meeting_button"

    /** Scrollable list surface wrapping the past-meeting rows — `MeetingCalendarScreenState.Content`. */
    const val MEETING_LIST: String = "meeting_calendar_lazy_column"

    /** Full-illustration empty surface — `MeetingCalendarScreenState.Empty`. */
    const val EMPTY_SECTION: String = "meeting_calendar_empty_section"

    /** Inline error surface — `MeetingCalendarScreenState.Error`. */
    const val ERROR_SECTION: String = "meeting_calendar_error_section"

    /** Retry CTA on the error state — [MeetingCalendarAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "meeting_calendar_error_retry_button"

    /** Resolves the stable per-row test tag for a given meeting id. */
    fun rowTag(meetingId: String): String = "meeting_calendar_row_$meetingId"
}
