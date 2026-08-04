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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.feature.loginsignup.LoginSignupTestTags
import org.mifos.groupbanking.feature.loginsignup.PasswordRequirements
import org.mifos.groupbanking.feature.loginsignup.generated.resources.Res
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_requirements_cd
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_rule_case
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_rule_digit
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_rule_len
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_rule_norepeat
import org.mifos.groupbanking.feature.loginsignup.generated.resources.screens_login_signup_password_rule_symbol

/**
 * Live Fineract password-policy feedback — a wrap-row of small pill chips, one per rule, each
 * turning `primary`-filled when its [PasswordRequirements] flag is satisfied and muted
 * (`surfaceVariant`) when not. Signup mode only. Rendered from VM state so every keystroke
 * re-evaluates. Mirrors `ui.yaml#components.password_requirement_chips`. See API.md#screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordRequirementChips(
    requirements: PasswordRequirements,
    modifier: Modifier = Modifier,
) {
    val requirementsCd = stringResource(Res.string.screens_login_signup_password_requirements_cd)

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = requirementsCd },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        PasswordRuleChip(
            label = stringResource(Res.string.screens_login_signup_password_rule_len),
            satisfied = requirements.hasMinLength,
            testTag = LoginSignupTestTags.PASSWORD_RULE_LEN,
        )
        PasswordRuleChip(
            label = stringResource(Res.string.screens_login_signup_password_rule_case),
            satisfied = requirements.hasMixedCase,
            testTag = LoginSignupTestTags.PASSWORD_RULE_CASE,
        )
        PasswordRuleChip(
            label = stringResource(Res.string.screens_login_signup_password_rule_digit),
            satisfied = requirements.hasDigit,
            testTag = LoginSignupTestTags.PASSWORD_RULE_DIGIT,
        )
        PasswordRuleChip(
            label = stringResource(Res.string.screens_login_signup_password_rule_symbol),
            satisfied = requirements.hasSymbol,
            testTag = LoginSignupTestTags.PASSWORD_RULE_SYMBOL,
        )
        PasswordRuleChip(
            label = stringResource(Res.string.screens_login_signup_password_rule_norepeat),
            satisfied = requirements.hasNoRepeats,
            testTag = LoginSignupTestTags.PASSWORD_RULE_NOREPEAT,
        )
    }
}

@Composable
private fun PasswordRuleChip(
    label: String,
    satisfied: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (satisfied) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (satisfied) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        )
    }
}
