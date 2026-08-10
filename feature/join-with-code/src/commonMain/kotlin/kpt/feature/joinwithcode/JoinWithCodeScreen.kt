/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.joinwithcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import kpt.feature.joinwithcode.components.GroupPreviewCard
import kpt.feature.joinwithcode.components.InviteCodeField
import kpt.feature.joinwithcode.components.JoinCodeErrorBanner
import kpt.feature.joinwithcode.components.JoinHeroIcon
import kpt.feature.joinwithcode.generated.resources.Res
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_action_join_group
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_action_join_group_cd
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_action_validate
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_action_validate_cd
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_back_cd
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_error_already_member_message
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_error_auth_message
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_error_expired_message
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_error_invalid_code_message
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_error_network_message
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_title
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_validating_cd
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_validating_message
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Container for the invite-code entry + group-preview confirmation screen
 * (`join-with-code-screen`). Collects [JoinWithCodeViewModel] state via
 * [collectAsStateWithLifecycle], consumes one-shot [JoinWithCodeEvent]s (navigation + snackbar)
 * through [EventsEffect], and delegates all rendering to the stateless [JoinWithCodeContent].
 * [inviteCode] is the optional deep-link nav-arg (`ui.yaml#nav_params.inviteCode` /
 * `flow.yaml#on_mount.parse_deep_link_code`) forwarded to [JoinWithCodeViewModel] via Koin
 * `parametersOf(...)` — a non-null 6-char code auto-triggers validation inside the ViewModel's
 * `init` block, skipping the manual entry step. See API.md#screen.
 */
@Composable
internal fun JoinWithCodeScreen(
    inviteCode: String?,
    onNavigateToPersonalDashboard: () -> Unit,
    onNavigateToLoginSignup: (pendingInviteCode: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinWithCodeViewModel = koinViewModel(parameters = { parametersOf(inviteCode) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    EventsEffect(viewModel) { event ->
        when (event) {
            JoinWithCodeEvent.NavigateToPersonalDashboard -> onNavigateToPersonalDashboard()
            is JoinWithCodeEvent.NavigateToLoginSignup -> onNavigateToLoginSignup(event.pendingInviteCode)
            JoinWithCodeEvent.NavigateBack -> onNavigateBack()
            // Declared for JoinWithCodeEvent exhaustiveness (ui.yaml#state_model.events) — not
            // currently emitted anywhere in JoinWithCodeViewModel.handleAction (see event KDoc);
            // message is already a resolved display string on the event, not a resource key, so
            // it is shown as-is rather than routed through stringResource(...).
            is JoinWithCodeEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
        }
    }

    JoinWithCodeContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `join-with-code-screen`. State-driven per
 * `ui.yaml#state_model.screen_state` — every [JoinWithCodeScreenState] member is handled.
 * `Success` renders the same in-flight [GroupPreviewSection] as `Joining` since it is never
 * actually produced by [deriveScreenState] (see that KDoc) — the branch exists only for
 * exhaustiveness parity with the domain model. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JoinWithCodeContent(
    state: JoinWithCodeState,
    onAction: (JoinWithCodeAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_join_with_code_title)
    val backCd = stringResource(Res.string.screens_join_with_code_back_cd)

    KptScaffold(
        modifier = modifier.testTag(JoinWithCodeTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationIonClick = { onAction(JoinWithCodeAction.OnBack) },
                    testTag = JoinWithCodeTestTags.BACK_BUTTON,
                    contentDescription = backCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        when (state.deriveScreenState()) {
            JoinWithCodeScreenState.Initial -> JoinCodeEntrySection(
                state = state,
                onAction = onAction,
                isValidating = false,
            )

            JoinWithCodeScreenState.Validating -> JoinCodeEntrySection(
                state = state,
                onAction = onAction,
                isValidating = true,
            )

            JoinWithCodeScreenState.Preview -> GroupPreviewSection(state = state, onAction = onAction, isJoining = false)
            JoinWithCodeScreenState.Joining -> GroupPreviewSection(state = state, onAction = onAction, isJoining = true)

            // Transient/unreachable — see JoinWithCodeScreenState.Success KDoc. Rendered the same
            // as Joining so a stray recomposition never shows a blank frame while the
            // NavigateToPersonalDashboard event (emitted the same frame it would occur) is consumed.
            JoinWithCodeScreenState.Success -> GroupPreviewSection(state = state, onAction = onAction, isJoining = true)

            JoinWithCodeScreenState.ErrorInvalidCode,
            JoinWithCodeScreenState.ErrorExpired,
            JoinWithCodeScreenState.ErrorAlreadyMember,
            JoinWithCodeScreenState.ErrorNetwork,
            -> JoinCodeErrorSection(state = state, onAction = onAction)
        }
    }
}

/**
 * `JoinWithCodeScreenState.Initial` / `.Validating` — `ui.yaml#states.initial` / `.validating`.
 * Renders [JoinHeroIcon] + [InviteCodeField] + either the "Validate Code" button (disabled until
 * exactly 6 characters are entered — `ui.yaml#components.validate_button.enabled`) or a centered
 * [CircularProgressIndicator] + label while `inviteStatus == VALIDATING`
 * (`ui.yaml#components.validating_indicator`). See API.md#screen.
 */
@Composable
internal fun JoinCodeEntrySection(
    state: JoinWithCodeState,
    onAction: (JoinWithCodeAction) -> Unit,
    isValidating: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val validateCd = stringResource(Res.string.screens_join_with_code_action_validate_cd)
    val validatingCd = stringResource(Res.string.screens_join_with_code_validating_cd)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(sp.xxl))
        JoinHeroIcon()
        Spacer(Modifier.height(sp.lg))

        InviteCodeField(
            value = state.inviteCode,
            enabled = !isValidating,
            onValueChange = { onAction(JoinWithCodeAction.OnCodeChange(it)) },
        )
        Spacer(Modifier.height(sp.lg))

        if (isValidating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sp.sm),
                modifier = Modifier.testTag(JoinWithCodeTestTags.VALIDATING_INDICATOR),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).semantics { contentDescription = validatingCd },
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(Res.string.screens_join_with_code_validating_message))
            }
        } else {
            KptButton(
                onClick = { onAction(JoinWithCodeAction.OnValidateCode) },
                enabled = state.inviteCode.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(JoinWithCodeTestTags.VALIDATE_BUTTON)
                    .semantics { contentDescription = validateCd },
            ) {
                Text(stringResource(Res.string.screens_join_with_code_action_validate))
            }
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/**
 * `JoinWithCodeScreenState.Preview` / `.Joining` — `ui.yaml#states.preview` / `.joining`. Renders
 * the (disabled while joining) [InviteCodeField] + [GroupPreviewCard] + the "Join Group" CTA,
 * whose loading spinner mirrors `ui.yaml#components.confirm_join_button.loading`. See
 * API.md#screen.
 */
@Composable
internal fun GroupPreviewSection(
    state: JoinWithCodeState,
    onAction: (JoinWithCodeAction) -> Unit,
    isJoining: Boolean,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val joinCd = stringResource(Res.string.screens_join_with_code_action_join_group_cd)
    val preview = state.groupPreview

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(sp.xxl))
        JoinHeroIcon()
        Spacer(Modifier.height(sp.lg))

        InviteCodeField(
            value = state.inviteCode,
            enabled = false,
            onValueChange = { onAction(JoinWithCodeAction.OnCodeChange(it)) },
        )
        Spacer(Modifier.height(sp.lg))

        if (preview != null) {
            GroupPreviewCard(preview = preview)
            Spacer(Modifier.height(sp.lg))

            KptButton(
                onClick = { onAction(JoinWithCodeAction.OnConfirmJoin) },
                enabled = !isJoining,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(JoinWithCodeTestTags.CONFIRM_JOIN_BUTTON)
                    .semantics { contentDescription = joinCd },
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(sp.sm))
                }
                Text(stringResource(Res.string.screens_join_with_code_action_join_group))
            }
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/**
 * `JoinWithCodeScreenState.ErrorInvalidCode` / `.ErrorExpired` / `.ErrorAlreadyMember` /
 * `.ErrorNetwork` — `ui.yaml#states.error_*`. Renders [JoinHeroIcon] + [InviteCodeField] +
 * "Validate Code" button + [JoinCodeErrorBanner]. `state.error` is guaranteed non-null in every
 * branch that reaches this composable (`deriveScreenState()`'s `when (error)` gate). See
 * API.md#screen.
 */
@Composable
internal fun JoinCodeErrorSection(
    state: JoinWithCodeState,
    onAction: (JoinWithCodeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    val validateCd = stringResource(Res.string.screens_join_with_code_action_validate_cd)
    val error = state.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = sp.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(sp.xxl))
        JoinHeroIcon()
        Spacer(Modifier.height(sp.lg))

        InviteCodeField(
            value = state.inviteCode,
            enabled = true,
            onValueChange = { onAction(JoinWithCodeAction.OnCodeChange(it)) },
        )
        Spacer(Modifier.height(sp.lg))

        KptButton(
            onClick = { onAction(JoinWithCodeAction.OnValidateCode) },
            enabled = state.inviteCode.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(JoinWithCodeTestTags.VALIDATE_BUTTON)
                .semantics { contentDescription = validateCd },
        ) {
            Text(stringResource(Res.string.screens_join_with_code_action_validate))
        }
        Spacer(Modifier.height(sp.lg))

        if (error != null) {
            JoinCodeErrorBanner(
                error = error,
                message = error.resolvedMessage(),
                onRetry = { onAction(JoinWithCodeAction.OnRetry) },
            )
        }
        Spacer(Modifier.height(sp.xxl))
    }
}

/** Maps [JoinError] to its localized message — mirrors `messageKey` on each variant. */
@Composable
private fun JoinError.resolvedMessage(): String = when (this) {
    JoinError.InvalidCode -> stringResource(Res.string.screens_join_with_code_error_invalid_code_message)
    JoinError.ExpiredCode -> stringResource(Res.string.screens_join_with_code_error_expired_message)
    JoinError.AlreadyMember -> stringResource(Res.string.screens_join_with_code_error_already_member_message)
    JoinError.Network -> stringResource(Res.string.screens_join_with_code_error_network_message)
    JoinError.Auth -> stringResource(Res.string.screens_join_with_code_error_auth_message)
}
