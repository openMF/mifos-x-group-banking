/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberprofile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing
import kpt.core.model.MemberRole
import kpt.feature.memberprofile.MemberProfileTestTags
import kpt.feature.memberprofile.generated.resources.Res
import kpt.feature.memberprofile.generated.resources.screens_member_profile_change_role_title
import kpt.feature.memberprofile.generated.resources.screens_member_profile_confirm
import org.jetbrains.compose.resources.stringResource

private val ROLE_OPTIONS = listOf(MemberRole.CHAIRPERSON, MemberRole.TREASURER, MemberRole.SECRETARY, MemberRole.MEMBER)

/**
 * `ui.yaml#components.role_edit_bottom_sheet` — visible only while `isEditingRole`. Lists the 4
 * assignable [MemberRole] options as radio list-items ([onRoleSelected] dispatches
 * `OnRoleSelected(role)` per tap, `ui.yaml#content.role_option_*`), then a Confirm button
 * ([onConfirmClick], shows a spinner while [isUpdatingRole]). Dismissing the sheet (swipe-down or
 * scrim tap) dispatches [onDismiss] — matches `ui.yaml#on_dismiss` (`OnDismissRoleEdit`). See
 * API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleEditBottomSheet(
    selectedRole: MemberRole?,
    isUpdatingRole: Boolean,
    onRoleSelected: (MemberRole) -> Unit,
    onConfirmClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState? = null,
) {
    val sp = MaterialTheme.spacing
    val title = stringResource(Res.string.screens_member_profile_change_role_title)
    val confirmLabel = stringResource(Res.string.screens_member_profile_confirm)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState ?: rememberModalBottomSheetState(),
        modifier = modifier.testTag(MemberProfileTestTags.ROLE_BOTTOM_SHEET),
    ) {
        Column(modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.md)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            ROLE_OPTIONS.forEach { role ->
                RoleOptionRow(
                    role = role,
                    selected = role == selectedRole,
                    onClick = { onRoleSelected(role) },
                    testTag = role.optionTestTag(),
                )
            }
            Button(
                onClick = onConfirmClick,
                enabled = !isUpdatingRole && selectedRole != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.md, bottom = sp.lg)
                    .testTag(MemberProfileTestTags.CONFIRM_ROLE_BUTTON),
            ) {
                if (isUpdatingRole) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = confirmLabel)
                }
            }
        }
    }
}

/** One radio list-item — `ui.yaml#content.role_option_*`, ≥56dp touch target. */
@Composable
private fun RoleOptionRow(role: MemberRole, selected: Boolean, onClick: () -> Unit, testTag: String, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = role.label(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = sp.sm),
        )
    }
}

/** Resolves the stable per-role test tag — see [MemberProfileTestTags]. */
private fun MemberRole.optionTestTag(): String = when (this) {
    MemberRole.CHAIRPERSON -> MemberProfileTestTags.ROLE_OPTION_CHAIRPERSON
    MemberRole.TREASURER -> MemberProfileTestTags.ROLE_OPTION_TREASURER
    MemberRole.SECRETARY -> MemberProfileTestTags.ROLE_OPTION_SECRETARY
    MemberRole.MEMBER, MemberRole.UNKNOWN -> MemberProfileTestTags.ROLE_OPTION_MEMBER
}
