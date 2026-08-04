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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.AuthMode
import org.mifos.groupbanking.feature.loginsignup.LoginSignupTestTags
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_mode_toggle_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_tab_login
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_tab_signup

/**
 * LOGIN / SIGNUP pill segmented control. A rounded `surfaceVariant` track holds two segments; the
 * active segment lifts onto a `surface` background with `primary` text + a subtle shadow, the
 * inactive segment stays muted. Mirrors `ui.yaml#components.mode_toggle_tabs` — dispatches
 * `LoginSignupAction.OnModeToggle` via [onModeSelected]. TestTags [LoginSignupTestTags.TAB_LOGIN] /
 * [LoginSignupTestTags.TAB_SIGNUP] are preserved on the clickable segments (Maestro selectors).
 * See API.md#screen.
 */
@Composable
fun AuthModeToggleTabs(
    selectedMode: AuthMode,
    enabled: Boolean,
    onModeSelected: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeToggleDescription = stringResource(Res.string.screens_login_signup_mode_toggle_cd)
    val loginLabel = stringResource(Res.string.screens_login_signup_tab_login)
    val signupLabel = stringResource(Res.string.screens_login_signup_tab_signup)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = modeToggleDescription },
    ) {
        Row(modifier = Modifier.padding(MaterialTheme.spacing.xs)) {
            AuthModeSegment(
                label = loginLabel,
                selected = selectedMode == AuthMode.Login,
                enabled = enabled,
                onClick = { onModeSelected(AuthMode.Login) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(LoginSignupTestTags.TAB_LOGIN),
            )
            AuthModeSegment(
                label = signupLabel,
                selected = selectedMode == AuthMode.Signup,
                enabled = enabled,
                onClick = { onModeSelected(AuthMode.Signup) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(LoginSignupTestTags.TAB_SIGNUP),
            )
        }
    }
}

@Composable
private fun AuthModeSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val shape = RoundedCornerShape(10.dp)
    val segmentModifier = if (selected) {
        modifier.shadow(elevation = 2.dp, shape = shape)
    } else {
        modifier
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        modifier = segmentModifier
            .clip(shape)
            .heightIn(min = sp.touchTargetMin)
            .semantics {
                this.selected = selected
                this.role = Role.Tab
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = sp.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
