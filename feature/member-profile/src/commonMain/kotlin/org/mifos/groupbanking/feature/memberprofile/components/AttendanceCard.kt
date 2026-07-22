/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.memberprofile.MemberProfileTestTags
import org.mifos.groupbanking.feature.memberprofile.generated.resources.Res
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_attendance_fraction
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_attendance_label
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_attendance_progress_cd
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_attendance_rate

private const val RATE_HIGH_THRESHOLD = 0.80
private const val RATE_MID_THRESHOLD = 0.60

/**
 * `ui.yaml#components.attendance_card` — meeting attendance fraction + rate + a colour-coded
 * `LinearProgressIndicator` (`ui.yaml#components.attendance_progress_bar.style.indicator_color`:
 * `tertiary` at ≥80%, `secondary` at ≥60%, `error` below 60%). See API.md#screen.
 */
@Composable
fun AttendanceCard(
    meetingsAttended: Int,
    totalMeetings: Int,
    attendanceRate: Double,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_member_profile_attendance_label)
    val fractionText = stringResource(Res.string.screens_member_profile_attendance_fraction, meetingsAttended, totalMeetings)
    val ratePercent = (attendanceRate * 100).toInt()
    val rateText = stringResource(Res.string.screens_member_profile_attendance_rate, ratePercent)
    val progressCd = stringResource(Res.string.screens_member_profile_attendance_progress_cd, ratePercent)
    val indicatorColor = when {
        attendanceRate >= RATE_HIGH_THRESHOLD -> MaterialTheme.colorScheme.tertiary
        attendanceRate >= RATE_MID_THRESHOLD -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = sp.xs,
        shape = RoundedCornerShape(sp.lg),
        modifier = modifier.fillMaxWidth().testTag(MemberProfileTestTags.ATTENDANCE_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = fractionText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = sp.xs),
            )
            Text(
                text = rateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = sp.xs, bottom = sp.sm),
            )
            LinearProgressIndicator(
                progress = { attendanceRate.toFloat().coerceIn(0f, 1f) },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = indicatorColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics {
                        contentDescription = progressCd
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = attendanceRate.toFloat().coerceIn(0f, 1f),
                            range = 0f..1f,
                        )
                    }
                    .testTag(MemberProfileTestTags.ATTENDANCE_PROGRESS_BAR),
            )
        }
    }
}
