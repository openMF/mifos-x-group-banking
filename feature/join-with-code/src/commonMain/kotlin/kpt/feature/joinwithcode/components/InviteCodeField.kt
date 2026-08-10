/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.joinwithcode.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kpt.feature.joinwithcode.JoinWithCodeTestTags
import kpt.feature.joinwithcode.generated.resources.Res
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_field_invite_code_cd
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_field_invite_code_label
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_field_invite_code_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * 6-character invite-code entry field — `ui.yaml#components.invite_code_field` /
 * `forms.invite_code_form.invite_code_field`. Renders centered, letter-spaced, uppercase
 * headline-styled text mirroring `preview/initial.html`'s `.code-field input` styling
 * (`letter-spacing: 8-10px; text-align: center; text-transform: uppercase`).
 * Sanitization (uppercase / alphanumeric filter / 6-char truncation) happens in
 * `JoinWithCodeViewModel.handleCodeChange` — this component only forwards the raw
 * [onValueChange] callback and renders whatever [value] the ViewModel already sanitized. See
 * API.md#screen.
 */
@Composable
fun InviteCodeField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldCd = stringResource(Res.string.screens_join_with_code_field_invite_code_cd)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.screens_join_with_code_field_invite_code_label)) },
        placeholder = { Text(stringResource(Res.string.screens_join_with_code_field_invite_code_placeholder)) },
        singleLine = true,
        enabled = enabled,
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = modifier
            .fillMaxWidth()
            .testTag(JoinWithCodeTestTags.INVITE_CODE_FIELD)
            .semantics { contentDescription = fieldCd },
    )
}
