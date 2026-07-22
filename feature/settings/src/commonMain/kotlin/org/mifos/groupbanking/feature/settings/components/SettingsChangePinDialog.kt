/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kpt.core.designsystem.theme.spacing

/**
 * `ui.yaml#components.change_pin_dialog` (`visible_when: isChangingPin`) — a single `AlertDialog`
 * mapping `actions.cancel` -> [onDismiss] / `actions.confirm` -> [onConfirm]. [currentPin] /
 * [newPin] are LOCAL, ephemeral `remember`-scoped Compose state (NOT part of
 * [org.mifos.groupbanking.feature.settings.SettingsState] — `ui.yaml#state_model.state` declares
 * no `currentPin`/`newPin` fields; `SettingsAction.OnSubmitPinChange` carries the raw values as
 * action params instead, mirroring how a transient, never-persisted PIN should never round-trip
 * through a `@Serializable` MVI state). Composing this dialog fresh each time `isChangingPin`
 * flips true (the caller wraps it in `if (isChangingPin) { SettingsChangePinDialog(...) }`)
 * naturally resets both fields on every re-open — no PIN digit ever survives process death or a
 * dialog dismiss/reopen cycle. See API.md#screen.
 */
@Composable
fun SettingsChangePinDialog(
    title: String,
    currentPinLabel: String,
    newPinLabel: String,
    cancelLabel: String,
    confirmLabel: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (currentPin: String, newPin: String) -> Unit,
    modifier: Modifier = Modifier,
    dialogContentDescription: String? = null,
    currentPinFieldContentDescription: String? = null,
    newPinFieldContentDescription: String? = null,
    cancelContentDescription: String? = null,
    confirmContentDescription: String? = null,
    dialogTestTag: String? = null,
    currentPinFieldTestTag: String? = null,
    newPinFieldTestTag: String? = null,
    errorTextTestTag: String? = null,
    cancelButtonTestTag: String? = null,
    confirmButtonTestTag: String? = null,
) {
    val sp = MaterialTheme.spacing
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    val dialogModifier = modifier
        .let { base -> if (dialogContentDescription != null) base.semantics { contentDescription = dialogContentDescription } else base }
        .let { base -> if (dialogTestTag != null) base.testTag(dialogTestTag) else base }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = dialogModifier,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it },
                    label = { Text(text = currentPinLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (currentPinFieldContentDescription != null) {
                                base.semantics { contentDescription = currentPinFieldContentDescription }
                            } else {
                                base
                            }
                        }
                        .let { base -> if (currentPinFieldTestTag != null) base.testTag(currentPinFieldTestTag) else base },
                )
                Spacer(modifier = Modifier.height(sp.md))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text(text = newPinLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (newPinFieldContentDescription != null) {
                                base.semantics { contentDescription = newPinFieldContentDescription }
                            } else {
                                base
                            }
                        }
                        .let { base -> if (newPinFieldTestTag != null) base.testTag(newPinFieldTestTag) else base },
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(sp.sm))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.let { base ->
                            if (errorTextTestTag != null) base.testTag(errorTextTestTag) else base
                        },
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .let { base -> if (cancelContentDescription != null) base.semantics { contentDescription = cancelContentDescription } else base }
                    .let { base -> if (cancelButtonTestTag != null) base.testTag(cancelButtonTestTag) else base },
            ) {
                Text(text = cancelLabel)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPin, newPin) },
                modifier = Modifier
                    .heightIn(min = sp.touchTargetMin)
                    .let { base -> if (confirmContentDescription != null) base.semantics { contentDescription = confirmContentDescription } else base }
                    .let { base -> if (confirmButtonTestTag != null) base.testTag(confirmButtonTestTag) else base },
            ) {
                Text(text = confirmLabel)
            }
        },
    )
}
