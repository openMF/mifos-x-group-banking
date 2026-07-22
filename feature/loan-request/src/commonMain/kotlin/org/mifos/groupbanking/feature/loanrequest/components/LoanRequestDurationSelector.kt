/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.duration_selector` (`type: card`, `slider: { min: 4, max: 52, steps: 12 }`).
 * [weeksValueLabel] is the pre-localized "N weeks" caption for the current [durationWeeks] value
 * (formatted by the caller via `stringResource(..., durationWeeks)`, since a `@Composable`
 * `stringResource` call cannot happen inside a plain value-formatting helper). See API.md#screen.
 */
@Composable
fun LoanRequestDurationSelector(
    headerLabel: String,
    weeksValueLabel: String,
    durationWeeks: Int,
    onDurationChanged: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(sp.md),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = headerLabel,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = weeksValueLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = durationWeeks.toFloat(),
            onValueChange = { onDurationChanged(it.roundToInt()) },
            valueRange = 4f..52f,
            steps = 12,
            enabled = enabled,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .semantics { this.contentDescription = contentDescription },
        )
    }
}
