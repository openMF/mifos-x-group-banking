/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loanapply.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

/** `ui.yaml#components.corpus_warning_banner.style.background` -- `#FFF9C4` (soft amber). */
private val CorpusWarningBackground = Color(0xFFFFF9C4)

/** `ui.yaml#components.corpus_warning_banner.style.text_color` -- `#E65100` (deep amber/orange). */
private val CorpusWarningText = Color(0xFFE65100)

/**
 * `ui.yaml#components.corpus_warning_banner` (`type: warning-banner`, `visible_when:
 * corpusWarning`). DISPLAY + SOFT-WARN ONLY -- see `LoanApplyViewModel.handleSubmit` KDoc; a
 * corpus-below-buffer disbursement is still submittable, this banner never blocks
 * `submit_button`. See API.md#screen.
 */
@Composable
fun LoanApplyCorpusWarningBanner(
    message: String,
    iconContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = CorpusWarningBackground, shape = RoundedCornerShape(8.dp))
            .padding(sp.md)
            .semantics { contentDescription = message },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = iconContentDescription,
            tint = CorpusWarningText,
            modifier = Modifier.padding(end = sp.sm),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = CorpusWarningText,
        )
    }
}
