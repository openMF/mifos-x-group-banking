/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.MemberRole

// -- demo-data.yaml#entries fixtures (MemberAddState / MemberAddState_Offline /
// MemberAddState_ValidationError) -------------------------------------------------------------

/** `entries[dto=MemberAddState]` — Grace Achieng, Mwangaza Women's Group (groupId 42). */
private val demoContentState = MemberAddState(
    firstName = "Grace",
    lastName = "Achieng",
    phone = "+254723456789",
    photoUri = null,
    selectedRole = MemberRole.SECRETARY,
    groupId = "42",
)

/** `entries[dto=MemberAddState_Offline]` — Faith Mutua, offline queue path. */
private val demoOfflineState = MemberAddState(
    firstName = "Faith",
    lastName = "Mutua",
    phone = "+254745000006",
    selectedRole = MemberRole.MEMBER,
    isOffline = true,
    groupId = "42",
)

/** `entries[dto=MemberAddState_ValidationError]`. */
private val demoValidationErrorState = MemberAddState(
    firstName = "",
    lastName = "Wambui",
    phone = "0712",
    selectedRole = MemberRole.MEMBER,
    validationErrors = mapOf(
        "firstName" to "error_validation",
        "phone" to "error_phone_format",
    ),
    groupId = "42",
)

/** `ui.yaml#states.submitting.description` — form frozen, in-flight `create_client` call. */
private val demoSubmittingState = demoContentState.copy(isSubmitting = true)

/** `ui.yaml#states.error.demo_data` — duplicate-phone rejection from Fineract. */
private val demoErrorState = demoContentState.copy(error = MemberAddError.PhoneAlreadyExists)

/** Transient `ui.yaml#states.success` frame — `NavigateToMemberProfile` fires the same frame. */
private val demoSuccessState = demoContentState.copy(isSubmitting = false, isSubmitSuccess = true)

/** `entries[dto=MemberAddState]` with a photo already attached — exercises the remove affordance. */
private val demoContentWithPhotoState = demoContentState.copy(photoUri = "pending-image-picker://camera-capture")

/**
 * `@Preview` gallery for `MemberAddScreen.kt`. See API.md#preview. Data source: `demo-data.yaml`
 * (`entries[dto=MemberAddState]`, `entries[dto=MemberAddState_Offline]`,
 * `entries[dto=MemberAddState_ValidationError]`) plus `ui.yaml#states.*.demo_data`.
 */
private class MemberAddScreenPreviewProvider : PreviewParameterProvider<MemberAddState> {
    override val values: Sequence<MemberAddState> = sequenceOf(
        demoContentState,
        demoContentWithPhotoState,
        demoOfflineState,
        demoValidationErrorState,
        demoSubmittingState,
        demoErrorState,
        demoSuccessState,
    )
}

@Preview
@Composable
private fun MemberAddContentPreview(
    @PreviewParameter(MemberAddScreenPreviewProvider::class)
    state: MemberAddState,
) {
    KptTheme {
        MemberAddContent(state = state, onAction = {})
    }
}

@Preview
@Composable
private fun MemberAddContentSectionPreview() {
    KptTheme {
        MemberAddContentSection(state = demoContentState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberAddSubmittingSectionPreview() {
    KptTheme {
        MemberAddSubmittingSection(state = demoSubmittingState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberAddErrorSectionPreview() {
    KptTheme {
        MemberAddErrorSection(state = demoErrorState, onAction = {})
    }
}

@Preview
@Composable
private fun MemberAddSuccessSectionPreview() {
    KptTheme {
        MemberAddSuccessSection()
    }
}

@Preview
@Composable
private fun MemberAddFormFieldsPreview() {
    KptTheme {
        MemberAddFormFields(state = demoContentWithPhotoState, onAction = {}, isSubmitting = false)
    }
}

@Preview
@Composable
private fun MemberAddOfflineSyncDialogPreview() {
    KptTheme {
        MemberAddOfflineSyncDialog(onDismiss = {})
    }
}
