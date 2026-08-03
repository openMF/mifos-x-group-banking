/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
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

/**
 * 4-step wizard progress indicator — `ui.yaml#components.step_indicator`. Each step renders a
 * dot (checkmark once passed, filled + step number while current, outlined otherwise) + its
 * `labels[step]` caption underneath, connected by a thin divider line — mirrors the preview
 * HTML `.stepper` layout ("✓ Identity" / "2 Rules" / ...). [labels] must have exactly
 * [totalSteps] entries (`ui.yaml#components.step_indicator.labels`). See API.md#screen.
 */
@Composable
fun WizardStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    labels: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (step in 1..totalSteps) {
            StepDot(
                stepNumber = step,
                label = labels.getOrElse(step - 1) { step.toString() },
                isDone = step < currentStep,
                isCurrent = step == currentStep,
            )
            if (step != totalSteps) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (step < currentStep) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun StepDot(stepNumber: Int, label: String, isDone: Boolean, isCurrent: Boolean) {
    val sp = MaterialTheme.spacing
    val background: Color
    val contentColor: Color
    when {
        isDone -> {
            background = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
        }
        isCurrent -> {
            background = MaterialTheme.colorScheme.primary
            contentColor = MaterialTheme.colorScheme.onPrimary
        }
        else -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(sp.xl)
                .background(color = background, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent || isDone) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
