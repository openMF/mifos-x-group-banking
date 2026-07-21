/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.grouptypepicker.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing

/**
 * Shimmering skeleton row shown while `GroupTypePickerScreenState.Loading` — mirrors
 * `preview/loading.html` (avatar + 3 text lines + chip row, animated opacity in lieu of a CSS
 * shimmer gradient). Purely decorative — no semantics/testTag of its own; the surrounding
 * [kpt.core.ui.scaffold.KptScaffold] loading region carries the single accessible "loading"
 * announcement. See API.md#screen.
 */
@Composable
fun GroupTypeCardSkeleton(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val transition = rememberInfiniteTransition(label = "group-type-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "group-type-skeleton-alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f)

    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(sp.md)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(skeletonColor))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
                Box(modifier = Modifier.fillMaxWidth(0.92f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
                Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(skeletonColor))
            }
        }
        Row(
            modifier = Modifier.padding(top = sp.sm, start = 44.dp + sp.md),
            horizontalArrangement = Arrangement.spacedBy(sp.xs),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(78.dp)
                        .height(18.dp)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(4.dp))
                        .background(skeletonColor),
                )
            }
        }
    }
}
