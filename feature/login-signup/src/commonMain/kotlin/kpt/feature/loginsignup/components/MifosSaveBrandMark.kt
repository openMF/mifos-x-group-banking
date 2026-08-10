/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loginsignup.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kpt.feature.loginsignup.generated.resources.Res
import kpt.feature.loginsignup.generated.resources.screens_login_signup_brand_mark_cd
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val MEMBER_COUNT = 6

/**
 * The MifosSave brand mark — a ring of [MEMBER_COUNT] small `colorScheme.primary` circles (the
 * members) arranged evenly around a filled `colorScheme.secondary` centre circle (the shared-savings
 * pool). Drawn as a pure Compose [Canvas] so it inherits the live theme colours (the theme is being
 * re-coloured to MifosSave green + amber in parallel — no hardcoded hex here). Decorative
 * container-level [contentDescription] is set for a11y; the individual circles carry none.
 */
@Composable
fun MifosSaveBrandMark(modifier: Modifier = Modifier, markSize: Dp = 56.dp) {
    val memberColor = MaterialTheme.colorScheme.primary
    val poolColor = MaterialTheme.colorScheme.secondary
    val cd = stringResource(Res.string.screens_login_signup_brand_mark_cd)

    Canvas(
        modifier = modifier
            .size(markSize)
            .semantics { contentDescription = cd },
    ) {
        val radius = size.minDimension / 2f
        val centre = Offset(radius, radius)
        val poolRadius = radius * 0.30f
        val memberRadius = radius * 0.13f
        val ringRadius = radius * 0.72f

        // Shared-savings pool (amber centre).
        drawCircle(color = poolColor, radius = poolRadius, center = centre)

        // Members evenly spaced around the ring, starting at the top (−90°).
        repeat(MEMBER_COUNT) { index ->
            val angle = (2.0 * PI / MEMBER_COUNT) * index - PI / 2.0
            val cx = centre.x + ringRadius * cos(angle).toFloat()
            val cy = centre.y + ringRadius * sin(angle).toFloat()
            drawCircle(color = memberColor, radius = memberRadius, center = Offset(cx, cy))
        }
    }
}
