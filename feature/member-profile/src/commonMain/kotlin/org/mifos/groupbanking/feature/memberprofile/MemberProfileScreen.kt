/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.feature.memberprofile.components.ActiveLoanCard
import org.mifos.groupbanking.feature.memberprofile.components.AttendanceCard
import org.mifos.groupbanking.feature.memberprofile.components.MemberHeaderCard
import org.mifos.groupbanking.feature.memberprofile.components.RoleEditBottomSheet
import org.mifos.groupbanking.feature.memberprofile.components.SavingsHistoryCard
import org.mifos.groupbanking.feature.memberprofile.generated.resources.Res
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_btn_retry
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_auth
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_icon_cd
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_network
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_not_found
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_role_update
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_server
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_error_state_title
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_role_updated
import org.mifos.groupbanking.feature.memberprofile.generated.resources.screens_member_profile_screen_title

/**
 * Container for `member-profile-screen`. Collects [MemberProfileViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [MemberProfileEvent]s (navigate back to
 * member-list / navigate to member-savings-detail / show a snackbar) through [EventsEffect], and
 * delegates all rendering to the stateless [MemberProfileContent]. [memberId] and [groupId] are
 * the `ui.yaml#nav_params` forwarded from `member-list` — supplied to [MemberProfileViewModel]
 * via Koin `parametersOf(memberId, groupId)` (memberId FIRST, groupId SECOND, matching
 * `MemberProfileModule`'s declaration order). See API.md#screen.
 */
@Composable
internal fun MemberProfileScreen(
    memberId: String,
    groupId: String,
    onNavigateToMemberList: (groupId: String) -> Unit,
    onNavigateToSavingsDetail: (memberId: String, groupId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberProfileViewModel = koinViewModel(parameters = { parametersOf(memberId, groupId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only and EventsEffect's
    // callback runs in a suspend (non-composable) scope, same convention as
    // GroupDashboardScreen.kt.
    val networkMessage = stringResource(Res.string.screens_member_profile_error_network)
    val serverMessage = stringResource(Res.string.screens_member_profile_error_server)
    val notFoundMessage = stringResource(Res.string.screens_member_profile_error_not_found)
    val authMessage = stringResource(Res.string.screens_member_profile_error_auth)
    val roleUpdateFailedMessage = stringResource(Res.string.screens_member_profile_error_role_update)
    val roleUpdatedMessage = stringResource(Res.string.screens_member_profile_role_updated)

    EventsEffect(viewModel) { event ->
        when (event) {
            is MemberProfileEvent.NavigateToMemberList -> onNavigateToMemberList(event.groupId)
            is MemberProfileEvent.NavigateToSavingsDetail -> onNavigateToSavingsDetail(event.memberId, event.groupId)
            is MemberProfileEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "error_network" -> networkMessage
                    "error_server" -> serverMessage
                    "error_not_found" -> notFoundMessage
                    "error_auth" -> authMessage
                    "error_role_update" -> roleUpdateFailedMessage
                    "role_updated" -> roleUpdatedMessage
                    else -> event.message
                },
            )
        }
    }

    MemberProfileContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `member-profile-screen`. State-driven per
 * [MemberProfileState.screenState] — every [MemberProfileScreenState] member is handled
 * (Loading/Content/Error, this composite screen has no `Empty` variant — see
 * [MemberProfileScreenState] KDoc). The role-edit [RoleEditBottomSheet] overlays on top of the
 * `Content` state whenever `state.isEditingRole` is true, matching
 * `ui.yaml#components.role_edit_bottom_sheet.visible: "{{isEditingRole}}"`. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberProfileContent(
    state: MemberProfileState,
    onAction: (MemberProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_member_profile_screen_title)

    KptScaffold(
        onNavigationIconClick = { onAction(MemberProfileAction.OnBack) },
        title = title,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag(MemberProfileTestTags.SCREEN),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screenState) {
                MemberProfileScreenState.Loading -> MemberProfileLoadingSection()
                MemberProfileScreenState.Content -> MemberProfileContentSection(state = state, onAction = onAction)
                MemberProfileScreenState.Error -> MemberProfileErrorSection(state = state, onAction = onAction)
            }

            if (state.isEditingRole) {
                RoleEditBottomSheet(
                    selectedRole = state.selectedRole,
                    isUpdatingRole = state.isUpdatingRole,
                    onRoleSelected = { role -> onAction(MemberProfileAction.OnRoleSelected(role)) },
                    onConfirmClick = { onAction(MemberProfileAction.OnConfirmRoleChange) },
                    onDismiss = { onAction(MemberProfileAction.OnDismissRoleEdit) },
                )
            }
        }
    }
}

/**
 * `MemberProfileScreenState.Loading` — 4 shimmer blocks mirroring `preview/loading.html`'s
 * `shimmer_profile` (header / savings / loan / attendance block heights). See API.md#screen.
 */
@Composable
internal fun MemberProfileLoadingSection(modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MemberProfileTestTags.LOADING_SECTION),
        verticalArrangement = Arrangement.spacedBy(sp.md),
    ) {
        MemberProfileSkeletonBlock(height = 220.dp)
        MemberProfileSkeletonBlock(height = 180.dp)
        MemberProfileSkeletonBlock(height = 120.dp)
        MemberProfileSkeletonBlock(height = 140.dp)
    }
}

/** One shimmering placeholder block used by [MemberProfileLoadingSection] — purely decorative. */
@Composable
internal fun MemberProfileSkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(MaterialTheme.spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/**
 * `MemberProfileScreenState.Content` — [MemberHeaderCard] + [SavingsHistoryCard] +
 * [ActiveLoanCard] (only when `state.accounts.activeLoan != null`) + [AttendanceCard], mirroring
 * `preview/content.html`. Guards on `state.member`/`state.accounts` being non-null —
 * `handleStreamUpdated`'s `ScreenState.Content` branch always populates both together with
 * `isLoading = false`, so `screenState == Content` implies they are set; this is a defensive
 * no-render rather than a crash if that invariant is ever violated. See API.md#screen.
 */
@Composable
internal fun MemberProfileContentSection(
    state: MemberProfileState,
    onAction: (MemberProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val member = state.member ?: return
    val accounts = state.accounts ?: return
    val sp = MaterialTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(sp.md),
        contentPadding = PaddingValues(sp.lg),
    ) {
        item {
            MemberHeaderCard(
                member = member,
                role = state.role,
                isCurrentUserChairperson = state.isCurrentUserChairperson,
                onEditRoleClick = { onAction(MemberProfileAction.OnEditRoleTap) },
            )
        }
        item {
            SavingsHistoryCard(
                savingsBalance = accounts.savingsBalance,
                savingsHistory = accounts.savingsHistory,
                onViewFullHistoryClick = { onAction(MemberProfileAction.OnViewSavings) },
            )
        }
        accounts.activeLoan?.let { loan ->
            item { ActiveLoanCard(loan = loan) }
        }
        item {
            AttendanceCard(
                meetingsAttended = state.meetingsAttended,
                totalMeetings = state.totalMeetings,
                attendanceRate = state.attendanceRate,
            )
        }
        item { Box(modifier = Modifier.height(sp.xl)) }
    }
}

/**
 * `MemberProfileScreenState.Error` — full-screen error surface (cloud_off icon, title, resolved
 * [MemberProfileError.messageKey], Retry CTA shown only when the mapped error is retryable),
 * mirroring `preview/error.html`. See API.md#screen.
 */
@Composable
internal fun MemberProfileErrorSection(
    state: MemberProfileState,
    onAction: (MemberProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val errorIconCd = stringResource(Res.string.screens_member_profile_error_icon_cd)
    val titleText = stringResource(Res.string.screens_member_profile_error_state_title)
    val retryLabel = stringResource(Res.string.screens_member_profile_btn_retry)
    val message = when (state.error) {
        MemberProfileError.Network, null -> stringResource(Res.string.screens_member_profile_error_network)
        MemberProfileError.Server -> stringResource(Res.string.screens_member_profile_error_server)
        MemberProfileError.NotFound -> stringResource(Res.string.screens_member_profile_error_not_found)
        MemberProfileError.Auth -> stringResource(Res.string.screens_member_profile_error_auth)
        MemberProfileError.RoleUpdateFailed -> stringResource(Res.string.screens_member_profile_error_role_update)
    }
    val canRetry = state.error?.retry ?: true

    Column(
        modifier = modifier.fillMaxSize().padding(sp.lg).testTag(MemberProfileTestTags.ERROR_SECTION),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = errorIconCd,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Text(text = titleText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = sp.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = sp.sm),
        )
        if (canRetry) {
            Button(
                onClick = { onAction(MemberProfileAction.Retry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sp.touchTargetMin)
                    .padding(top = sp.lg)
                    .testTag(MemberProfileTestTags.ERROR_RETRY_BUTTON),
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Text(text = retryLabel, modifier = Modifier.padding(start = sp.xs))
            }
        }
    }
}
