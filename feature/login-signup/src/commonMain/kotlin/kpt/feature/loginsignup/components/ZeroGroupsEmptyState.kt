/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loginsignup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptOutlinedButton
import kpt.core.designsystem.theme.spacing
import kpt.feature.loginsignup.LoginSignupTestTags
import kpt.feature.loginsignup.generated.resources.Res
import kpt.feature.loginsignup.generated.resources.screens_login_signup_action_create_group
import kpt.feature.loginsignup.generated.resources.screens_login_signup_action_join_with_code
import kpt.feature.loginsignup.generated.resources.screens_login_signup_create_group_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_join_with_code_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_zero_groups_illustration_cd
import kpt.feature.loginsignup.generated.resources.screens_login_signup_zero_groups_message
import kpt.feature.loginsignup.generated.resources.screens_login_signup_zero_groups_title
import org.jetbrains.compose.resources.stringResource

/**
 * Post-auth empty state rendered when `groupMemberships.isEmpty()` — illustration, copy, and
 * the two onboarding CTAs. Mirrors `ui.yaml#states.zero_groups`. See API.md#screen.
 */
@Composable
fun ZeroGroupsEmptyState(
    onCreateGroupClick: () -> Unit,
    onJoinWithCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val createGroupDescription = stringResource(Res.string.screens_login_signup_create_group_cd)
    val joinWithCodeDescription = stringResource(Res.string.screens_login_signup_join_with_code_cd)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = stringResource(Res.string.screens_login_signup_zero_groups_illustration_cd),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .testTag(LoginSignupTestTags.ZERO_GROUPS_ILLUSTRATION),
        )
        Spacer(Modifier.height(sp.lg))
        Text(
            text = stringResource(Res.string.screens_login_signup_zero_groups_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(sp.sm))
        Text(
            text = stringResource(Res.string.screens_login_signup_zero_groups_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(sp.xxl))
        KptButton(
            onClick = onCreateGroupClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(LoginSignupTestTags.CREATE_GROUP_BUTTON)
                .semantics { contentDescription = createGroupDescription },
        ) {
            Text(stringResource(Res.string.screens_login_signup_action_create_group))
        }
        Spacer(Modifier.height(sp.md))
        KptOutlinedButton(
            onClick = onJoinWithCodeClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(LoginSignupTestTags.JOIN_WITH_CODE_BUTTON)
                .semantics { contentDescription = joinWithCodeDescription },
        ) {
            Text(stringResource(Res.string.screens_login_signup_action_join_with_code))
        }
    }
}
