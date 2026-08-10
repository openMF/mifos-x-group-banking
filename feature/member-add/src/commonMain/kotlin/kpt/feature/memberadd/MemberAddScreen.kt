/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.AppCard
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.model.MemberRole
import kpt.core.ui.scaffold.KptScaffold
import kpt.feature.memberadd.components.MemberAddDropdownField
import kpt.feature.memberadd.components.MemberAddErrorBanner
import kpt.feature.memberadd.components.MemberAddOfflineBanner
import kpt.feature.memberadd.components.MemberAddTextField
import kpt.feature.memberadd.components.PhotoPickerArea
import kpt.feature.memberadd.components.PhotoSourceBottomSheet
import kpt.feature.memberadd.generated.resources.Res
import kpt.feature.memberadd.generated.resources.screens_member_add_action_retry
import kpt.feature.memberadd.generated.resources.screens_member_add_action_retry_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_action_save
import kpt.feature.memberadd.generated.resources.screens_member_add_action_save_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_action_saving
import kpt.feature.memberadd.generated.resources.screens_member_add_action_saving_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_close_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_error_auth
import kpt.feature.memberadd.generated.resources.screens_member_add_error_banner_icon_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_error_banner_title
import kpt.feature.memberadd.generated.resources.screens_member_add_error_network
import kpt.feature.memberadd.generated.resources.screens_member_add_error_phone_exists
import kpt.feature.memberadd.generated.resources.screens_member_add_error_phone_format
import kpt.feature.memberadd.generated.resources.screens_member_add_error_server
import kpt.feature.memberadd.generated.resources.screens_member_add_error_validation
import kpt.feature.memberadd.generated.resources.screens_member_add_field_first_name_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_field_first_name_label
import kpt.feature.memberadd.generated.resources.screens_member_add_field_first_name_placeholder
import kpt.feature.memberadd.generated.resources.screens_member_add_field_last_name_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_field_last_name_label
import kpt.feature.memberadd.generated.resources.screens_member_add_field_last_name_placeholder
import kpt.feature.memberadd.generated.resources.screens_member_add_field_phone_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_field_phone_helper
import kpt.feature.memberadd.generated.resources.screens_member_add_field_phone_label
import kpt.feature.memberadd.generated.resources.screens_member_add_field_phone_placeholder
import kpt.feature.memberadd.generated.resources.screens_member_add_field_role_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_field_role_label
import kpt.feature.memberadd.generated.resources.screens_member_add_offline_banner_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_offline_dialog_confirm
import kpt.feature.memberadd.generated.resources.screens_member_add_offline_dialog_message
import kpt.feature.memberadd.generated.resources.screens_member_add_offline_dialog_title
import kpt.feature.memberadd.generated.resources.screens_member_add_offline_notice
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_add
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_area_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_camera
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_camera_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_gallery
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_gallery_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_preview_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_photo_remove_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_role_chairperson
import kpt.feature.memberadd.generated.resources.screens_member_add_role_member
import kpt.feature.memberadd.generated.resources.screens_member_add_role_secretary
import kpt.feature.memberadd.generated.resources.screens_member_add_role_treasurer
import kpt.feature.memberadd.generated.resources.screens_member_add_success_icon_cd
import kpt.feature.memberadd.generated.resources.screens_member_add_success_message
import kpt.feature.memberadd.generated.resources.screens_member_add_topbar_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Placeholder camera-capture URI dispatched by [PhotoSourceBottomSheet]'s "Take Photo" option.
 * `ImagePickerHelper` (`ui.yaml#state_model.di`) — the platform (androidMain/iosMain) service
 * responsible for actually launching the camera and returning a real captured-file URI — does not
 * exist anywhere in this codebase yet (same documented gap as [MemberAddViewModel]'s own
 * "Photo-bytes deferral" KDoc). Dispatching [MemberAddAction.OnPhotoCaptured] with this sentinel
 * still exercises the REAL state transition (`photoUri` gets set, the bottom sheet closes, the
 * remove affordance appears) — this is a flagged, documented seam, NOT a dead click. Flagged for a
 * follow-up generation step once `ImagePickerHelper` lands (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001
 * CFF1).
 */
private const val PLACEHOLDER_CAMERA_PHOTO_URI = "pending-image-picker://camera-capture"

/** Same seam as [PLACEHOLDER_CAMERA_PHOTO_URI], for the "Choose from Gallery" option. */
private const val PLACEHOLDER_GALLERY_PHOTO_URI = "pending-image-picker://gallery-selection"

/**
 * Container for `member-add-screen` (`ui.yaml#route`: `/groups/{groupId}/members/add`). Collects
 * [MemberAddViewModel] state via [collectAsStateWithLifecycle], forwards the required [groupId]
 * nav-arg to Koin via `parametersOf(groupId)` (mirrors `MemberProfileScreen.kt`'s identical
 * `memberId`/`groupId` nav-arg convention), consumes one-shot [MemberAddEvent]s (navigation +
 * photo picker + offline dialog + snackbar) through [EventsEffect], and delegates all rendering to
 * the stateless [MemberAddContent]. `MemberAddEvent.ShowPhotoPicker` and
 * `MemberAddEvent.ShowOfflineSyncDialog` are both one-shot signals rendered via LOCAL UI state
 * (mirrors `GroupCreateScreen.kt`'s identical `showOfflineDialog` convention) rather than
 * `MemberAddState.showPhotoPicker` directly — there is no `OnPhotoPickerDismiss` action in the
 * declared 10-member `MemberAddAction` set, so a tap-outside/back-gesture dismissal is handled
 * purely client-side without needing a VM round-trip. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberAddScreen(
    groupId: String,
    onNavigateToMemberProfile: (memberId: String, groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberAddViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as GroupCreateScreen.kt.
    val networkErrorMessage = stringResource(Res.string.screens_member_add_error_network)
    val authErrorMessage = stringResource(Res.string.screens_member_add_error_auth)
    val serverErrorMessage = stringResource(Res.string.screens_member_add_error_server)
    val validationErrorMessage = stringResource(Res.string.screens_member_add_error_validation)
    val phoneExistsMessage = stringResource(Res.string.screens_member_add_error_phone_exists)
    val photoSheetTitle = stringResource(Res.string.screens_member_add_photo_add)
    val cameraLabel = stringResource(Res.string.screens_member_add_photo_camera)
    val galleryLabel = stringResource(Res.string.screens_member_add_photo_gallery)
    val cameraCd = stringResource(Res.string.screens_member_add_photo_camera_cd)
    val galleryCd = stringResource(Res.string.screens_member_add_photo_gallery_cd)

    EventsEffect(viewModel) { event ->
        when (event) {
            is MemberAddEvent.NavigateToMemberProfile -> onNavigateToMemberProfile(event.memberId, event.groupId)
            MemberAddEvent.NavigateBack -> onNavigateBack()
            MemberAddEvent.ShowPhotoPicker -> showPhotoSourceSheet = true
            MemberAddEvent.ShowOfflineSyncDialog -> showOfflineDialog = true
            is MemberAddEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_network" -> networkErrorMessage
                    "error_auth" -> authErrorMessage
                    "error_server" -> serverErrorMessage
                    "error_validation" -> validationErrorMessage
                    "error_phone_exists" -> phoneExistsMessage
                    else -> event.message
                },
            )
        }
    }

    MemberAddContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (showPhotoSourceSheet) {
        PhotoSourceBottomSheet(
            title = photoSheetTitle,
            cameraLabel = cameraLabel,
            galleryLabel = galleryLabel,
            cameraContentDescription = cameraCd,
            galleryContentDescription = galleryCd,
            onCameraClick = {
                viewModel.trySendAction(MemberAddAction.OnPhotoCaptured(uri = PLACEHOLDER_CAMERA_PHOTO_URI))
                showPhotoSourceSheet = false
            },
            onGalleryClick = {
                viewModel.trySendAction(MemberAddAction.OnPhotoSelected(uri = PLACEHOLDER_GALLERY_PHOTO_URI))
                showPhotoSourceSheet = false
            },
            onDismissRequest = { showPhotoSourceSheet = false },
            modifier = Modifier.testTag(MemberAddTestTags.PHOTO_SOURCE_SHEET),
            cameraOptionModifier = Modifier.testTag(MemberAddTestTags.PHOTO_SOURCE_CAMERA_OPTION),
            galleryOptionModifier = Modifier.testTag(MemberAddTestTags.PHOTO_SOURCE_GALLERY_OPTION),
        )
    }

    if (showOfflineDialog) {
        MemberAddOfflineSyncDialog(onDismiss = { showOfflineDialog = false })
    }
}

/**
 * Stateless render surface for `member-add-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` — every [MemberAddScreenState] member is handled. The top app
 * bar's close icon is hidden while `Submitting`/`Success` (mirrors `submitting.html`'s
 * `visibility:hidden` close button — the mid-call/post-success form should not be dismissible).
 * See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberAddContent(
    state: MemberAddState,
    onAction: (MemberAddAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val screenState = state.deriveScreenState()
    val closeCd = stringResource(Res.string.screens_member_add_close_cd)
    val title = stringResource(Res.string.screens_member_add_topbar_title)
    val showCloseIcon = screenState == MemberAddScreenState.Content || screenState == MemberAddScreenState.Error

    KptScaffold(
        modifier = modifier.testTag(MemberAddTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = if (showCloseIcon) Icons.Filled.Close else null,
                    onNavigationIonClick = if (showCloseIcon) {
                        { onAction(MemberAddAction.OnBack) }
                    } else {
                        null
                    },
                    testTag = MemberAddTestTags.TOP_BAR,
                    contentDescription = closeCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        when (screenState) {
            MemberAddScreenState.Content -> MemberAddContentSection(state = state, onAction = onAction)
            MemberAddScreenState.Submitting -> MemberAddSubmittingSection(state = state, onAction = onAction)
            MemberAddScreenState.Error -> MemberAddErrorSection(state = state, onAction = onAction)
            MemberAddScreenState.Success -> MemberAddSuccessSection()
        }
    }
}

/** `MemberAddScreenState.Content` — `ui.yaml#states.content`. See API.md#screen. */
@Composable
internal fun MemberAddContentSection(
    state: MemberAddState,
    onAction: (MemberAddAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(sp.md)
        MemberAddFormFields(state = state, onAction = onAction, isSubmitting = false)
        Spacer(sp.xxl)
    }
}

/**
 * `MemberAddScreenState.Submitting` — `ui.yaml#states.submitting`. Wraps [MemberAddFormFields]
 * (all fields disabled, `isSubmitting = true`) dimmed to 50% opacity, with a centered
 * [CircularProgressIndicator] overlay (`submitting.html`'s `.spinner` element). See API.md#screen.
 */
@Composable
internal fun MemberAddSubmittingSection(
    state: MemberAddState,
    onAction: (MemberAddAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val savingCd = stringResource(Res.string.screens_member_add_action_saving_cd)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = sp.lg)
                .alpha(0.5f),
        ) {
            Spacer(sp.md)
            MemberAddFormFields(state = state, onAction = onAction, isSubmitting = true)
            Spacer(sp.xxl)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = savingCd },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag(MemberAddTestTags.SUBMITTING_INDICATOR))
        }
    }
}

/**
 * `MemberAddScreenState.Error` — `ui.yaml#states.error`. Renders [MemberAddFormFields] (still
 * editable, `isSubmitting = false`) followed by an inline [MemberAddErrorBanner] with its own
 * embedded Retry affordance (`error.html`'s `.alert.alert-danger` block). See API.md#screen.
 */
@Composable
internal fun MemberAddErrorSection(
    state: MemberAddState,
    onAction: (MemberAddAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val retryLabel = stringResource(Res.string.screens_member_add_action_retry)
    val retryCd = stringResource(Res.string.screens_member_add_action_retry_cd)
    val errorBannerTitle = stringResource(Res.string.screens_member_add_error_banner_title)
    val errorIconCd = stringResource(Res.string.screens_member_add_error_banner_icon_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
    ) {
        Spacer(sp.md)
        MemberAddFormFields(state = state, onAction = onAction, isSubmitting = false)
        state.error?.let { error ->
            Spacer(sp.md)
            MemberAddErrorBanner(
                title = errorBannerTitle,
                message = error.resolvedMessage(),
                iconContentDescription = errorIconCd,
                retryLabel = retryLabel,
                retryContentDescription = retryCd,
                onRetryClick = { onAction(MemberAddAction.OnSubmit) },
                modifier = Modifier.testTag(MemberAddTestTags.ERROR_BANNER),
                retryButtonModifier = Modifier.testTag(MemberAddTestTags.ERROR_RETRY_BUTTON),
            )
        }
        Spacer(sp.xxl)
    }
}

/**
 * `MemberAddScreenState.Success` — `ui.yaml#states.success` ("Navigated to member-profile" — a
 * transient frame; `NavigateToMemberProfile` fires the same frame this renders, so there is
 * deliberately NO interactive CTA here, mirroring `GroupCreateSuccessSection`'s identical
 * transient-frame precedent). See API.md#screen.
 */
@Composable
internal fun MemberAddSuccessSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val successIconCd = stringResource(Res.string.screens_member_add_success_icon_cd)
    val successMessage = stringResource(Res.string.screens_member_add_success_message)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = sp.lg)
            .testTag(MemberAddTestTags.SUCCESS_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp).semantics { contentDescription = successIconCd },
        )
        Spacer(sp.md)
        Text(text = successMessage, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Shared form body (photo picker + identity/phone fields + role dropdown + offline banner + Save
 * button) rendered by [MemberAddContentSection] / [MemberAddSubmittingSection] /
 * [MemberAddErrorSection] — factored out to avoid duplicating the `ui.yaml#components` assembly
 * logic three times. See API.md#screen.
 */
@Composable
internal fun MemberAddFormFields(
    state: MemberAddState,
    onAction: (MemberAddAction) -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val photoAreaCd = stringResource(Res.string.screens_member_add_photo_area_cd)
    val photoPreviewCd = stringResource(Res.string.screens_member_add_photo_preview_cd)
    val photoRemoveCd = stringResource(Res.string.screens_member_add_photo_remove_cd)
    val photoAddLabel = stringResource(Res.string.screens_member_add_photo_add)
    val firstNameCd = stringResource(Res.string.screens_member_add_field_first_name_cd)
    val lastNameCd = stringResource(Res.string.screens_member_add_field_last_name_cd)
    val phoneCd = stringResource(Res.string.screens_member_add_field_phone_cd)
    val roleCd = stringResource(Res.string.screens_member_add_field_role_cd)
    val offlineNotice = stringResource(Res.string.screens_member_add_offline_notice)
    val offlineBannerCd = stringResource(Res.string.screens_member_add_offline_banner_cd)
    val saveLabel = stringResource(Res.string.screens_member_add_action_save)
    val savingLabel = stringResource(Res.string.screens_member_add_action_saving)
    val saveCd = stringResource(Res.string.screens_member_add_action_save_cd)
    val savingCd = stringResource(Res.string.screens_member_add_action_saving_cd)

    val roleOptions = listOf(
        MemberRole.CHAIRPERSON to stringResource(Res.string.screens_member_add_role_chairperson),
        MemberRole.TREASURER to stringResource(Res.string.screens_member_add_role_treasurer),
        MemberRole.SECRETARY to stringResource(Res.string.screens_member_add_role_secretary),
        MemberRole.MEMBER to stringResource(Res.string.screens_member_add_role_member),
    )
    val selectedRoleLabel = roleOptions.firstOrNull { it.first == state.selectedRole }?.second
        ?: roleOptions.last().second

    Column(modifier = modifier) {
        PhotoPickerArea(
            photoUri = state.photoUri,
            onAddPhotoClick = { onAction(MemberAddAction.OnPhotoPickerOpen) },
            onRemoveClick = { onAction(MemberAddAction.OnPhotoRemoved) },
            areaContentDescription = photoAreaCd,
            selectedContentDescription = photoPreviewCd,
            removeContentDescription = photoRemoveCd,
            enabled = !isSubmitting,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag(MemberAddTestTags.PHOTO_AREA),
            removeButtonModifier = Modifier.testTag(MemberAddTestTags.PHOTO_REMOVE_BUTTON),
        )
        Text(
            text = photoAddLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = sp.xs),
        )
        Spacer(sp.lg)

        AppCard {
            Column {
                MemberAddTextField(
                    value = state.firstName,
                    label = stringResource(Res.string.screens_member_add_field_first_name_label),
                    placeholder = stringResource(Res.string.screens_member_add_field_first_name_placeholder),
                    onValueChange = { onAction(MemberAddAction.OnFirstNameChange(it)) },
                    errorMessage = state.validationErrors["firstName"]?.let { memberAddValidationMessage(it) },
                    contentDescription = firstNameCd,
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag(MemberAddTestTags.FIELD_FIRST_NAME),
                )
                Spacer(sp.md)

                MemberAddTextField(
                    value = state.lastName,
                    label = stringResource(Res.string.screens_member_add_field_last_name_label),
                    placeholder = stringResource(Res.string.screens_member_add_field_last_name_placeholder),
                    onValueChange = { onAction(MemberAddAction.OnLastNameChange(it)) },
                    errorMessage = state.validationErrors["lastName"]?.let { memberAddValidationMessage(it) },
                    contentDescription = lastNameCd,
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag(MemberAddTestTags.FIELD_LAST_NAME),
                )
                Spacer(sp.md)

                MemberAddTextField(
                    value = state.phone,
                    label = stringResource(Res.string.screens_member_add_field_phone_label),
                    placeholder = stringResource(Res.string.screens_member_add_field_phone_placeholder),
                    helperText = stringResource(Res.string.screens_member_add_field_phone_helper),
                    onValueChange = { onAction(MemberAddAction.OnPhoneChange(it)) },
                    keyboardType = KeyboardType.Phone,
                    errorMessage = state.validationErrors["phone"]?.let { memberAddValidationMessage(it) },
                    contentDescription = phoneCd,
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag(MemberAddTestTags.FIELD_PHONE),
                )
                Spacer(sp.md)

                MemberAddDropdownField(
                    label = stringResource(Res.string.screens_member_add_field_role_label),
                    selectedOption = selectedRoleLabel,
                    options = roleOptions.map { it.second },
                    onOptionSelected = { label ->
                        roleOptions.firstOrNull { it.second == label }?.let { (role, _) ->
                            onAction(MemberAddAction.OnRoleSelected(role))
                        }
                    },
                    contentDescription = roleCd,
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag(MemberAddTestTags.DROPDOWN_ROLE),
                )
            }
        }

        if (state.isOffline) {
            Spacer(sp.md)
            MemberAddOfflineBanner(
                message = offlineNotice,
                iconContentDescription = offlineBannerCd,
                modifier = Modifier.testTag(MemberAddTestTags.OFFLINE_BANNER),
            )
        }

        Spacer(sp.lg)
        KptButton(
            onClick = { onAction(MemberAddAction.OnSubmit) },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sp.touchTargetMin)
                .testTag(MemberAddTestTags.SAVE_BUTTON)
                .semantics { contentDescription = if (isSubmitting) savingCd else saveCd },
        ) {
            Text(if (isSubmitting) savingLabel else saveLabel)
        }
    }
}

/**
 * `MemberAddEvent.ShowOfflineSyncDialog` modal — acknowledges the offline queue, no further
 * ViewModel action needed (`MemberAddViewModel.handleSubmit` has already set `isOffline = true` +
 * `error = MemberAddError.Network`; the form stays fully filled so `OnSubmit` can be re-dispatched
 * once connectivity returns). See API.md#screen.
 */
@Composable
internal fun MemberAddOfflineSyncDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(MemberAddTestTags.OFFLINE_SYNC_DIALOG),
        title = { Text(stringResource(Res.string.screens_member_add_offline_dialog_title)) },
        text = { Text(stringResource(Res.string.screens_member_add_offline_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = MaterialTheme.spacing.touchTargetMin)
                    .testTag(MemberAddTestTags.OFFLINE_SYNC_DIALOG_CONFIRM),
            ) {
                Text(stringResource(Res.string.screens_member_add_offline_dialog_confirm))
            }
        },
    )
}

/** Maps [MemberAddError.messageKey] to its localized message. */
@Composable
private fun MemberAddError.resolvedMessage(): String = when (this) {
    MemberAddError.Validation -> stringResource(Res.string.screens_member_add_error_validation)
    MemberAddError.Network -> stringResource(Res.string.screens_member_add_error_network)
    MemberAddError.Server -> stringResource(Res.string.screens_member_add_error_server)
    MemberAddError.Auth -> stringResource(Res.string.screens_member_add_error_auth)
    MemberAddError.PhoneAlreadyExists -> stringResource(Res.string.screens_member_add_error_phone_exists)
}

/**
 * Maps a `MemberAddState.validationErrors` value (an `error_key` string emitted by
 * `MemberAddViewModel.validateMemberAddForm`) to its localized message.
 */
@Composable
private fun memberAddValidationMessage(errorKey: String): String = when (errorKey) {
    "error_phone_format" -> stringResource(Res.string.screens_member_add_error_phone_format)
    else -> stringResource(Res.string.screens_member_add_error_validation)
}

/** Vertical-gap shorthand — `Spacer(sp.md)` reads cleaner than `Spacer(Modifier.height(sp.md))`. */
@Composable
private fun Spacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}
