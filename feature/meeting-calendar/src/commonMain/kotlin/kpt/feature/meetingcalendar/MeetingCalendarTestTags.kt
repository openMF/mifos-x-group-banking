/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.meetingcalendar

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

    // -- G3 / F6 schedule editor (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5) --------------------

    /** "Reschedule" text button on the upcoming card — [MeetingCalendarAction.OpenScheduleEditor]. */
    const val RESCHEDULE_BUTTON: String = "meeting_calendar_reschedule_button"

    /** "Set / Adjust Schedule" CTA in the no-upcoming card + empty state — [MeetingCalendarAction.OpenScheduleEditor]. */
    const val SET_SCHEDULE_BUTTON: String = "meeting_calendar_set_schedule_button"

    /** The schedule-editor modal bottom sheet surface — visible when `showScheduleEditor`. */
    const val SCHEDULE_EDITOR_SHEET: String = "meeting_calendar_schedule_editor_sheet"

    /** Meeting-day dropdown field in the schedule editor. */
    const val SCHEDULE_DAY_FIELD: String = "meeting_calendar_schedule_day_field"

    /** Meeting-time input field in the schedule editor. */
    const val SCHEDULE_TIME_FIELD: String = "meeting_calendar_schedule_time_field"

    /** Save CTA in the schedule editor — [MeetingCalendarAction.RescheduleMeeting]. */
    const val SCHEDULE_CONFIRM_BUTTON: String = "meeting_calendar_schedule_confirm_button"

    /** Cancel CTA in the schedule editor — [MeetingCalendarAction.DismissScheduleEditor]. */
    const val SCHEDULE_CANCEL_BUTTON: String = "meeting_calendar_schedule_cancel_button"

    /** The no-upcoming schedule card (replaces the former dead "Next meeting not scheduled" placeholder). */
    const val NO_UPCOMING_CARD: String = "meeting_calendar_no_upcoming_card"

    /** Resolves the stable per-row test tag for a given meeting id. */
    fun rowTag(meetingId: String): String = "meeting_calendar_row_$meetingId"

    /** Resolves the stable per-frequency segmented-button test tag (WEEKLY/BIWEEKLY/MONTHLY). */
    fun frequencyTag(frequency: String): String = "meeting_calendar_frequency_$frequency"
}
