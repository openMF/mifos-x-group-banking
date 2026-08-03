/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanrequest.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Reusable read-only dropdown-select field built on [ExposedDropdownMenuBox]. Backs
 * `ui.yaml#components.purpose_dropdown` (`type: dropdown`). Selection happens exclusively through
 * the dropdown menu, never free text entry -- copied from `feature/loan-apply`'s
 * `LoanApplyDropdownField.kt` pattern verbatim into this feature's own `components/` package
 * rather than a feature-to-feature import, per module-ownership rules; adds an optional
 * [leadingIcon] slot (`ui.yaml#components.purpose_dropdown.leading_icon: notepad`) not present on
 * the `loan-apply` original. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRequestDropdownField(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    contentDescription: String? = null,
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconContentDescription: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val fieldModifier = if (contentDescription != null) {
        modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription }
    } else {
        modifier.fillMaxWidth()
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = fieldModifier,
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            isError = errorMessage != null,
            label = { Text(label) },
            placeholder = placeholder?.let { text -> { Text(text) } },
            leadingIcon = leadingIcon?.let { icon ->
                { Icon(imageVector = icon, contentDescription = leadingIconContentDescription) }
            },
            supportingText = errorMessage?.let { message -> { Text(message) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
