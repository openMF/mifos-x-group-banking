/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loandetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import kpt.core.model.RepaymentRowStatus
import kpt.core.model.RepaymentScheduleRow
import kpt.feature.loandetail.generated.resources.Res
import kpt.feature.loandetail.generated.resources.screens_loan_detail_schedule_header_balance
import kpt.feature.loandetail.generated.resources.screens_loan_detail_schedule_header_due
import kpt.feature.loandetail.generated.resources.screens_loan_detail_schedule_header_due_date
import kpt.feature.loandetail.generated.resources.screens_loan_detail_schedule_header_paid
import kpt.feature.loandetail.generated.resources.screens_loan_detail_schedule_header_status
import kpt.feature.loandetail.generated.resources.screens_loan_detail_schedule_header_week
import kpt.feature.loandetail.generated.resources.screens_loan_detail_status_overdue
import kpt.feature.loandetail.generated.resources.screens_loan_detail_status_paid
import kpt.feature.loandetail.generated.resources.screens_loan_detail_status_partial
import kpt.feature.loandetail.generated.resources.screens_loan_detail_status_upcoming
import org.jetbrains.compose.resources.stringResource

/** Column weights for [RepaymentScheduleHeaderRow] / [RepaymentScheduleRowItem] — Wk / Due Date / Due / Paid / Balance / Status. */
private val CELL_WEIGHTS = floatArrayOf(0.6f, 1.4f, 1f, 1f, 1f, 1f)

/** `ui.yaml#components.schedule_table.headers` — 6-column header row. See API.md#screen. */
@Composable
fun RepaymentScheduleHeaderRow(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth().padding(vertical = sp.xs)) {
        HeaderCell(stringResource(Res.string.screens_loan_detail_schedule_header_week), CELL_WEIGHTS[0])
        HeaderCell(stringResource(Res.string.screens_loan_detail_schedule_header_due_date), CELL_WEIGHTS[1])
        HeaderCell(stringResource(Res.string.screens_loan_detail_schedule_header_due), CELL_WEIGHTS[2], TextAlign.End)
        HeaderCell(stringResource(Res.string.screens_loan_detail_schedule_header_paid), CELL_WEIGHTS[3], TextAlign.End)
        HeaderCell(stringResource(Res.string.screens_loan_detail_schedule_header_balance), CELL_WEIGHTS[4], TextAlign.End)
        HeaderCell(stringResource(Res.string.screens_loan_detail_schedule_header_status), CELL_WEIGHTS[5], TextAlign.End)
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = Modifier.weight(weight),
    )
}

/**
 * One weekly repayment-schedule row — `ui.yaml#components.schedule_table.rows_source`. Row
 * background/text colour follows [RepaymentScheduleRow.status] per `ui.yaml#row_style` (PAID
 * #F1F8E9/#33691E, PARTIAL #FFF9C4/#E65100, OVERDUE #FFCDD2/#B71C1C, UPCOMING surface/onSurface).
 * See API.md#screen.
 */
@Composable
fun RepaymentScheduleRowItem(row: RepaymentScheduleRow, modifier: Modifier = Modifier, testTag: String = "") {
    val sp = MaterialTheme.spacing
    val statusLabel = row.status.scheduleStatusLabel()
    val backgroundColor = row.status.scheduleRowBackgroundColor()
    val contentColor = row.status.scheduleRowContentColor()

    Row(
        modifier = (if (testTag.isNotEmpty()) modifier.testTag(testTag) else modifier)
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = sp.xs),
    ) {
        // row.weekNumber / dueDate / amounts — i18n:skip, dynamic data binding.
        Text(
            text = row.weekNumber.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.weight(CELL_WEIGHTS[0]),
        )
        Text(
            text = row.dueDate,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.weight(CELL_WEIGHTS[1]),
        )
        Text(
            text = row.dueAmount.formatGrouped(0),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(CELL_WEIGHTS[2]),
        )
        Text(
            text = row.paidAmount.formatGrouped(0),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(CELL_WEIGHTS[3]),
        )
        Text(
            text = row.balance.formatGrouped(0),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(CELL_WEIGHTS[4]),
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(CELL_WEIGHTS[5]),
        )
    }
}

@Composable
private fun RepaymentRowStatus.scheduleStatusLabel(): String = when (this) {
    RepaymentRowStatus.PAID -> stringResource(Res.string.screens_loan_detail_status_paid)
    RepaymentRowStatus.PARTIAL -> stringResource(Res.string.screens_loan_detail_status_partial)
    RepaymentRowStatus.UPCOMING -> stringResource(Res.string.screens_loan_detail_status_upcoming)
    RepaymentRowStatus.OVERDUE -> stringResource(Res.string.screens_loan_detail_status_overdue)
    // UNKNOWN absorbs any wire value this client build does not yet recognize (see
    // RepaymentRowStatus KDoc) — name fallback, i18n:skip, no dedicated string key for an
    // unmapped enum member.
    RepaymentRowStatus.UNKNOWN -> name
}

@Composable
private fun RepaymentRowStatus.scheduleRowBackgroundColor(): Color = when (this) {
    RepaymentRowStatus.PAID -> Color(0xFFF1F8E9)
    RepaymentRowStatus.PARTIAL -> Color(0xFFFFF9C4)
    RepaymentRowStatus.OVERDUE -> Color(0xFFFFCDD2)
    RepaymentRowStatus.UPCOMING, RepaymentRowStatus.UNKNOWN -> MaterialTheme.colorScheme.surface
}

@Composable
private fun RepaymentRowStatus.scheduleRowContentColor(): Color = when (this) {
    RepaymentRowStatus.PAID -> Color(0xFF33691E)
    RepaymentRowStatus.PARTIAL -> Color(0xFFE65100)
    RepaymentRowStatus.OVERDUE -> Color(0xFFB71C1C)
    RepaymentRowStatus.UPCOMING, RepaymentRowStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurface
}
