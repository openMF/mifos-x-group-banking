/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.meetingcalendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.MeetingFrequency
import org.mifos.groupbanking.feature.meetingcalendar.MeetingCalendarTestTags
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.Res
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_friday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_monday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_saturday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_sunday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_thursday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_tuesday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_day_wednesday
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_freq_biweekly
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_freq_monthly
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_freq_weekly
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_cancel
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_confirm
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_day_label
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_editor_a11y
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_editor_title
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_frequency_label
import org.mifos.groupbanking.feature.meetingcalendar.generated.resources.screens_meeting_calendar_schedule_time_label

/** The 7 selectable meeting days — enum-name value stored in state, localized label rendered. */
private val DAY_OPTIONS = listOf(
    "MONDAY" to Res.string.screens_meeting_calendar_day_monday,
    "TUESDAY" to Res.string.screens_meeting_calendar_day_tuesday,
    "WEDNESDAY" to Res.string.screens_meeting_calendar_day_wednesday,
    "THURSDAY" to Res.string.screens_meeting_calendar_day_thursday,
    "FRIDAY" to Res.string.screens_meeting_calendar_day_friday,
    "SATURDAY" to Res.string.screens_meeting_calendar_day_saturday,
    "SUNDAY" to Res.string.screens_meeting_calendar_day_sunday,
)

/**
 * `ui.yaml#schedule_editor_sheet` — the Set/Adjust-Schedule editor (G3 / F6). Lets the organizer pick
 * the recurring meeting day / time / frequency and Save (offline-queued, server-gated) or Cancel. Every
 * field dispatches its own [onFieldChange] param; Save fires [onConfirm] with the current draft values.
 * Not a dead-click stub — each affordance dispatches a real `MeetingCalendarAction`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleEditorBottomSheet(
    scheduleDay: String,
    scheduleTime: String,
    scheduleFrequency: MeetingFrequency,
    isRescheduling: Boolean,
    onFieldChange: (day: String?, time: String?, frequency: MeetingFrequency?) -> Unit,
    onConfirm: (day: String, time: String, frequency: MeetingFrequency) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val sheetA11y = stringResource(Res.string.screens_meeting_calendar_schedule_editor_a11y)
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
            .testTag(MeetingCalendarTestTags.SCHEDULE_EDITOR_SHEET)
            .semantics { contentDescription = sheetA11y },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = sp.lg).padding(bottom = sp.xl),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            Text(
                text = stringResource(Res.string.screens_meeting_calendar_schedule_editor_title),
                style = MaterialTheme.typography.titleLarge,
            )

            DayDropdown(
                selectedDay = scheduleDay,
                onDaySelected = { onFieldChange(it, null, null) },
            )

            OutlinedTextField(
                value = scheduleTime,
                onValueChange = { onFieldChange(null, it, null) },
                label = { Text(stringResource(Res.string.screens_meeting_calendar_schedule_time_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(MeetingCalendarTestTags.SCHEDULE_TIME_FIELD),
            )

            FrequencySelector(
                selected = scheduleFrequency,
                onSelect = { onFieldChange(null, null, it) },
            )

            Button(
                onClick = { onConfirm(scheduleDay, scheduleTime, scheduleFrequency) },
                enabled = !isRescheduling && scheduleDay.isNotBlank() && scheduleTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(MeetingCalendarTestTags.SCHEDULE_CONFIRM_BUTTON),
            ) {
                if (isRescheduling) {
                    CircularProgressIndicator(
                        modifier = Modifier.heightIn(min = 20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = stringResource(Res.string.screens_meeting_calendar_schedule_confirm))
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag(MeetingCalendarTestTags.SCHEDULE_CANCEL_BUTTON),
            ) {
                Text(text = stringResource(Res.string.screens_meeting_calendar_schedule_cancel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDropdown(selectedDay: String, onDaySelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = DAY_OPTIONS.firstOrNull { it.first == selectedDay }?.second
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel?.let { stringResource(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.screens_meeting_calendar_schedule_day_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(MeetingCalendarTestTags.SCHEDULE_DAY_FIELD),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DAY_OPTIONS.forEach { (value, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onDaySelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(selected: MeetingFrequency, onSelect: (MeetingFrequency) -> Unit) {
    val sp = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
        Text(
            text = stringResource(Res.string.screens_meeting_calendar_schedule_frequency_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val options = listOf(
            MeetingFrequency.WEEKLY to Res.string.screens_meeting_calendar_freq_weekly,
            MeetingFrequency.BIWEEKLY to Res.string.screens_meeting_calendar_freq_biweekly,
            MeetingFrequency.MONTHLY to Res.string.screens_meeting_calendar_freq_monthly,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (freq, labelRes) ->
                SegmentedButton(
                    selected = selected == freq,
                    onClick = { onSelect(freq) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.testTag(MeetingCalendarTestTags.frequencyTag(freq.name)),
                ) {
                    Text(text = stringResource(labelRes))
                }
            }
        }
    }
}
