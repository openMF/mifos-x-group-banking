/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.membersavingsdetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.common.formatGrouped
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.SavingsDataPoint
import org.mifos.groupbanking.feature.membersavingsdetail.MemberSavingsDetailTestTags
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.Res
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_sparkline_cd_format
import org.mifos.groupbanking.feature.membersavingsdetail.generated.resources.screens_member_savings_detail_trend_label

private val SPARKLINE_HEIGHT = 80.dp
private val BAR_GAP = 6.dp
private const val BAR_CORNER_RADIUS_PX = 6f

/**
 * `ui.yaml#components.savings_sparkline_card` — a lightweight `Canvas` bar-chart sparkline over
 * [points] (no external chart library is wired anywhere in this codebase — mirrors
 * `MemberProfileTestTags`/`SavingsHistoryCard`'s identical bar-Canvas precedent). The LAST bar
 * (most recent point) is drawn in `primary`, the rest in `primaryContainer`. Caller guards on
 * `points.isNotEmpty()` — this composable is not invoked at all for an empty series (see
 * `MemberSavingsDetailHeaderSection`), same convention as `SavingsHistoryCard`.
 *
 * See API.md#screen.
 */
@Composable
fun SavingsSparklineCard(points: List<SavingsDataPoint>, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val trendLabel = stringResource(Res.string.screens_member_savings_detail_trend_label)
    val sparklineCd = stringResource(
        Res.string.screens_member_savings_detail_sparkline_cd_format,
        points.firstOrNull()?.balance?.formatGrouped(0).orEmpty(),
        points.lastOrNull()?.balance?.formatGrouped(0).orEmpty(),
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = sp.xs,
        shape = RoundedCornerShape(sp.lg),
        modifier = modifier.fillMaxWidth().testTag(MemberSavingsDetailTestTags.SPARKLINE_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            Text(text = trendLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SavingsSparkline(
                points = points,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SPARKLINE_HEIGHT)
                    .padding(top = sp.sm)
                    .semantics { contentDescription = sparklineCd }
                    .testTag(MemberSavingsDetailTestTags.SPARKLINE),
            )
        }
    }
}

/** Minimal bar-chart sparkline drawn with `Canvas` — purely decorative; the parent applies the accessible [Modifier.semantics] summary. */
@Composable
private fun SavingsSparkline(points: List<SavingsDataPoint>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primaryContainer
    val latestBarColor = MaterialTheme.colorScheme.primary
    val maxBalance = points.maxOf { it.balance }.let { if (it <= 0.0) 1.0 else it }

    Canvas(modifier = modifier) {
        drawSparklineBars(points = points, maxBalance = maxBalance, barColor = barColor, latestBarColor = latestBarColor)
    }
}

private fun DrawScope.drawSparklineBars(
    points: List<SavingsDataPoint>,
    maxBalance: Double,
    barColor: Color,
    latestBarColor: Color,
) {
    val gapPx = BAR_GAP.toPx()
    val totalGap = gapPx * (points.size - 1).coerceAtLeast(0)
    val barWidth = ((size.width - totalGap) / points.size).coerceAtLeast(1f)

    points.forEachIndexed { index, point ->
        val fraction = (point.balance / maxBalance).coerceIn(0.0, 1.0).toFloat()
        val barHeight = size.height * fraction
        val left = index * (barWidth + gapPx)
        val top = size.height - barHeight
        val color = if (index == points.lastIndex) latestBarColor else barColor
        drawRoundRect(
            color = color,
            topLeft = Offset(x = left, y = top),
            size = Size(width = barWidth, height = barHeight),
            cornerRadius = CornerRadius(BAR_CORNER_RADIUS_PX, BAR_CORNER_RADIUS_PX),
        )
    }
}
