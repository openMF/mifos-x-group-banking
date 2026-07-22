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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * `ui.yaml#components.amount_field` (`type: text-field`, `keyboard_type: number`,
 * `ime_action: next`, `prefix: "KES"`, `leading_icon: currency_exchange`). Renders
 * [LoanRequestState.requestedAmountError] (resolved to a localized string by the caller, see
 * `LoanRequestScreen.kt` `fieldErrorMessage(...)`) as [OutlinedTextField.supportingText] when
 * present, else falls back to `ui.yaml#components.amount_field.supporting_text`. Copied from
 * `feature/loan-apply`'s `LoanApplyAmountField.kt` pattern, adding the `KES` prefix + `Next` IME
 * action `ui.yaml` declares for this screen. See API.md#screen.
 */
@Composable
fun LoanRequestAmountField(
    value: String,
    label: String,
    prefix: String,
    supportingText: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    contentDescription: String? = null,
    leadingIconContentDescription: String? = null,
    enabled: Boolean = true,
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
        prefix = { Text(prefix) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.CurrencyExchange, contentDescription = leadingIconContentDescription)
        },
        singleLine = true,
        enabled = enabled,
        isError = errorMessage != null,
        supportingText = {
            Text(
                text = errorMessage ?: supportingText,
                color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = fieldModifier,
    )
}
