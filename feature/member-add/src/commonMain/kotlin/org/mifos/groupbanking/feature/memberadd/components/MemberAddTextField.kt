/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType

/**
 * Reusable single-line form field wrapping [OutlinedTextField] with a label, optional
 * placeholder/helper text, and optional inline validation error. Backs every `text-field`
 * component in `ui.yaml#components` (`first_name_field`, `last_name_field`, `phone_field`). See
 * API.md#screen.
 */
@Composable
fun MemberAddTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    errorMessage: String? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
) {
    val fieldModifier = if (contentDescription != null) {
        modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription }
    } else {
        modifier.fillMaxWidth()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        prefix = prefix?.let { text -> { Text(text) } },
        singleLine = true,
        enabled = enabled,
        isError = errorMessage != null,
        supportingText = when {
            errorMessage != null -> {
                { Text(text = errorMessage, color = MaterialTheme.colorScheme.error) }
            }
            helperText != null -> {
                { Text(text = helperText, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            else -> null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = fieldModifier,
    )
}
