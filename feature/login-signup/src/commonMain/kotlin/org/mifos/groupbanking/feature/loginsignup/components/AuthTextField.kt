/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_hide_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_show_cd

/**
 * Reusable single-line auth text field wrapping [OutlinedTextField]: rounded 12dp corners, a
 * subtle `surfaceVariant` container tint that lifts to `surface` + a `primary` border on focus, an
 * optional [leadingIcon], an optional inline validation error, and — for password fields — a
 * built-in show/hide eye trailing icon (visibility state is local UI, held in [rememberSaveable]).
 * Backs `ui.yaml#components.signup_name_field` / `email_phone_field` / `password_field`. See
 * API.md#screen.
 */
@Composable
fun AuthTextField(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null,
    contentDescription: String? = null,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val showPasswordCd = stringResource(Res.string.screens_login_signup_password_show_cd)
    val hidePasswordCd = stringResource(Res.string.screens_login_signup_password_hide_cd)

    val fieldModifier = if (contentDescription != null) {
        modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription }
    } else {
        modifier.fillMaxWidth()
    }

    val visualTransformation = when {
        !isPassword -> VisualTransformation.None
        passwordVisible -> VisualTransformation.None
        else -> PasswordVisualTransformation()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        enabled = enabled,
        isError = errorMessage != null,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) hidePasswordCd else showPasswordCd,
                    )
                }
            }
        } else {
            null
        },
        supportingText = errorMessage?.let { message ->
            { Text(text = message, color = MaterialTheme.colorScheme.error) }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            // Never auto-capitalise: the email/phone/username identifier must be entered exactly as
            // typed (Fineract usernames are case-sensitive) — sentence-case would capitalise the
            // first letter and break login for a self-registered account.
            capitalization = KeyboardCapitalization.None,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = fieldModifier,
    )
}
