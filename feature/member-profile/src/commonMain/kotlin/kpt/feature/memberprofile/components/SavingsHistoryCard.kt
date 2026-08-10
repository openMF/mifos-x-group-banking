/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberprofile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kpt.core.model.SavingsDataPoint
import kpt.feature.memberprofile.MemberProfileTestTags
import kpt.feature.memberprofile.generated.resources.Res
import kpt.feature.memberprofile.generated.resources.screens_member_profile_savings_balance
import kpt.feature.memberprofile.generated.resources.screens_member_profile_savings_label
import kpt.feature.memberprofile.generated.resources.screens_member_profile_sparkline_cd
import kpt.feature.memberprofile.generated.resources.screens_member_profile_view_full_history
import org.jetbrains.compose.resources.stringResource

private val SPARKLINE_HEIGHT = 80.dp
private val BAR_GAP = 6.dp
private const val BAR_CORNER_RADIUS_PX = 6f

/**
 * `ui.yaml#components.savings_history_card` — balance headline + a lightweight bar-chart
 * sparkline over [savingsHistory] (no external chart library is wired anywhere in this codebase
 * — `Canvas` bars mirror `preview/content.html`'s `.sparkline .bar` treatment exactly, latest bar
 * highlighted in [MaterialTheme.colorScheme.primary], the rest in a muted tint) + the
 * [onViewFullHistoryClick] CTA ([MemberProfileTestTags.VIEW_FULL_HISTORY_BUTTON]). See API.md#screen.
 */
@Composable
fun SavingsHistoryCard(
    savingsBalance: Double,
    savingsHistory: List<SavingsDataPoint>,
    onViewFullHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val label = stringResource(Res.string.screens_member_profile_savings_label)
    val balanceText = stringResource(Res.string.screens_member_profile_savings_balance, savingsBalance.formatGrouped(0))
    val viewHistoryLabel = stringResource(Res.string.screens_member_profile_view_full_history)
    val sparklineCd = stringResource(
        Res.string.screens_member_profile_sparkline_cd,
        savingsHistory.firstOrNull()?.balance?.formatGrouped(0).orEmpty(),
        savingsHistory.lastOrNull()?.balance?.formatGrouped(0).orEmpty(),
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = sp.xs,
        shape = RoundedCornerShape(sp.lg),
        modifier = modifier.fillMaxWidth().testTag(MemberProfileTestTags.SAVINGS_CARD),
    ) {
        Column(modifier = Modifier.padding(sp.lg)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = balanceText,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = sp.xs, bottom = sp.md),
            )
            if (savingsHistory.isNotEmpty()) {
                SavingsSparkline(
                    points = savingsHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SPARKLINE_HEIGHT)
                        .semantics { contentDescription = sparklineCd }
                        .testTag(MemberProfileTestTags.SPARKLINE),
                )
            }
            TextButton(
                onClick = onViewFullHistoryClick,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.sm)
                    .testTag(MemberProfileTestTags.VIEW_FULL_HISTORY_BUTTON),
            ) {
                Text(text = viewHistoryLabel)
            }
        }
    }
}

/**
 * Minimal bar-chart sparkline drawn with `Canvas` — no external chart library dependency. Bars
 * are scaled to [points]'s max [SavingsDataPoint.balance]; the LAST bar (most recent) is drawn in
 * `primary`, the rest in `primaryContainer` — mirrors `preview/content.html`'s `.bar.latest`
 * treatment. Purely decorative for screen-reader purposes (the parent applies the semantic
 * [Modifier.semantics] contentDescription summarising the trend) — no additional labels are
 * drawn on-canvas.
 */
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
