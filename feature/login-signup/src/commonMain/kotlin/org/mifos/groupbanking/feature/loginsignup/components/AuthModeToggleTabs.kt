/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loginsignup.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.AuthMode
import org.mifos.groupbanking.feature.loginsignup.LoginSignupTestTags
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_mode_toggle_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_tab_login
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_tab_signup

/**
 * LOGIN / SIGNUP mode-toggle tab row. Mirrors `ui.yaml#components.mode_toggle_tabs` — dispatches
 * `LoginSignupAction.OnModeToggle` via [onModeSelected]. See API.md#screen.
 */
@Composable
fun AuthModeToggleTabs(
    selectedMode: AuthMode,
    enabled: Boolean,
    onModeSelected: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = if (selectedMode == AuthMode.Login) 0 else 1
    val modeToggleDescription = stringResource(Res.string.screens_login_signup_mode_toggle_cd)
    val loginLabel = stringResource(Res.string.screens_login_signup_tab_login)
    val signupLabel = stringResource(Res.string.screens_login_signup_tab_signup)

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.semantics { contentDescription = modeToggleDescription },
    ) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onModeSelected(AuthMode.Login) },
            enabled = enabled,
            text = { Text(loginLabel) },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(LoginSignupTestTags.TAB_LOGIN),
        )
        Tab(
            selected = selectedIndex == 1,
            onClick = { onModeSelected(AuthMode.Signup) },
            enabled = enabled,
            text = { Text(signupLabel) },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(LoginSignupTestTags.TAB_SIGNUP),
        )
    }
}
