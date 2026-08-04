/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberinvite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kpt.core.base.designsystem.component.AppCard
import kpt.core.base.designsystem.component.KptButton
import kpt.core.base.designsystem.component.KptTopAppBar
import kpt.core.base.designsystem.core.KptTopAppBarConfiguration
import kpt.core.base.ui.effects.EventsEffect
import kpt.core.designsystem.theme.spacing
import kpt.core.ui.scaffold.KptScaffold
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.PendingInvite
import org.mifos.groupbanking.feature.memberinvite.generated.resources.Res
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_copy_code
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_copy_code_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_copy_link
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_copy_link_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_generate
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_generate_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_generating
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_retry
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_retry_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_revoke
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_revoke_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_share
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_action_share_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_back_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_empty_pending
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_auth
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_banner_icon_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_banner_title
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_network
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_revoke
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_server
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_error_validation
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_field_email_phone_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_field_email_phone_label
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_field_email_phone_placeholder
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_field_role_cd
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_field_role_label
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_label_expires
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_label_invite_code
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_label_invite_link
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_role_chairperson
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_role_member
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_role_secretary
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_role_treasurer
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_section_create
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_section_pending
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_share_body
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_share_subject
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_snack_code_copied
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_snack_link_copied
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_snack_queued_offline
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_snack_revoked
import org.mifos.groupbanking.feature.memberinvite.generated.resources.screens_member_invite_topbar_title

/**
 * Container for `member-invite-screen` (`ui.yaml#route`: `/groups/{groupId}/invite`). Collects
 * [MemberInviteViewModel] state, forwards the [groupId] nav-arg to Koin via `parametersOf`,
 * consumes one-shot [MemberInviteEvent]s (navigation + snackbar + clipboard + share sheet) through
 * [EventsEffect], and delegates rendering to the stateless [MemberInviteContent].
 *
 * `ShowShareSheet` is handled here as a DOCUMENTED PLATFORM SEAM (mirrors `MemberAddScreen`'s
 * photo-picker deferral): a real native share sheet requires a `core/platform` expect/actual
 * bridge that does not exist in-tree yet, so the composed share text (subject + body + code + link)
 * is placed on the clipboard and a snackbar confirms it — a real, non-dead effect (the organizer
 * gets shareable text), flagged for a follow-up platform-bridge step
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1). See API.md#screen.
 */
@Composable
internal fun MemberInviteScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberInviteViewModel = koinViewModel(parameters = { parametersOf(groupId) }),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // Precomputed OUTSIDE EventsEffect — stringResource() is @Composable-only.
    val codeCopied = stringResource(Res.string.screens_member_invite_snack_code_copied)
    val linkCopied = stringResource(Res.string.screens_member_invite_snack_link_copied)
    val revoked = stringResource(Res.string.screens_member_invite_snack_revoked)
    val queuedOffline = stringResource(Res.string.screens_member_invite_snack_queued_offline)
    val networkError = stringResource(Res.string.screens_member_invite_error_network)
    val serverError = stringResource(Res.string.screens_member_invite_error_server)
    val authError = stringResource(Res.string.screens_member_invite_error_auth)
    val revokeError = stringResource(Res.string.screens_member_invite_error_revoke)
    val shareSubject = stringResource(Res.string.screens_member_invite_share_subject)
    val shareBody = stringResource(Res.string.screens_member_invite_share_body)

    EventsEffect(viewModel) { event ->
        when (event) {
            MemberInviteEvent.NavigateBack -> onNavigateBack()
            is MemberInviteEvent.CopyToClipboard -> clipboard.setText(AnnotatedString(event.text))
            is MemberInviteEvent.ShowShareSheet -> {
                // Documented platform seam — see the Container KDoc.
                clipboard.setText(
                    AnnotatedString("$shareSubject\n$shareBody ${event.code} — ${event.link}"),
                )
            }
            is MemberInviteEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                message = when (event.message) {
                    "snack_code_copied" -> codeCopied
                    "snack_link_copied" -> linkCopied
                    "snack_invite_revoked" -> revoked
                    "snack_invite_queued_offline" -> queuedOffline
                    "error_network" -> networkError
                    "error_server" -> serverError
                    "error_auth" -> authError
                    "error_revoke" -> revokeError
                    else -> event.message
                },
            )
        }
    }

    MemberInviteContent(
        state = state,
        onAction = viewModel::trySendAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Stateless render surface for `member-invite-screen`. Every [MemberInviteScreenState] member is
 * handled by the derived-state gating below. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberInviteContent(
    state: MemberInviteState,
    onAction: (MemberInviteAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(Res.string.screens_member_invite_topbar_title)
    val backCd = stringResource(Res.string.screens_member_invite_back_cd)

    KptScaffold(
        modifier = modifier.testTag(MemberInviteTestTags.SCREEN),
        topBar = {
            KptTopAppBar(
                KptTopAppBarConfiguration(
                    title = title,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationIonClick = { onAction(MemberInviteAction.OnBack) },
                    testTag = MemberInviteTestTags.TOP_BAR,
                    contentDescription = backCd,
                ),
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        val sp = MaterialTheme.spacing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = sp.lg),
        ) {
            Spacer(sp.md)
            CreateInviteSection(state = state, onAction = onAction)

            state.error?.let { error ->
                Spacer(sp.md)
                ErrorBanner(error = error, onRetry = { onAction(MemberInviteAction.OnRetry) })
            }

            Spacer(sp.lg)
            HorizontalDivider()
            Spacer(sp.lg)

            PendingInvitesSection(state = state, onAction = onAction)
            Spacer(sp.xxl)
        }
    }
}

/** Create-invite form + (when present) the generated-code card. `ui.yaml#states.content/generated`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInviteSection(
    state: MemberInviteState,
    onAction: (MemberInviteAction) -> Unit,
) {
    val sp = MaterialTheme.spacing
    val emailLabel = stringResource(Res.string.screens_member_invite_field_email_phone_label)
    val emailPlaceholder = stringResource(Res.string.screens_member_invite_field_email_phone_placeholder)
    val emailCd = stringResource(Res.string.screens_member_invite_field_email_phone_cd)
    val validationMsg = stringResource(Res.string.screens_member_invite_error_validation)
    val generateLabel = stringResource(Res.string.screens_member_invite_action_generate)
    val generatingLabel = stringResource(Res.string.screens_member_invite_action_generating)
    val generateCd = stringResource(Res.string.screens_member_invite_action_generate_cd)

    Text(
        text = stringResource(Res.string.screens_member_invite_section_create),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(sp.md)

    AppCard {
        Column {
            OutlinedTextField(
                value = state.emailPhone,
                onValueChange = { onAction(MemberInviteAction.OnEmailPhoneChange(it)) },
                label = { Text(emailLabel) },
                placeholder = { Text(emailPlaceholder) },
                singleLine = true,
                isError = state.validationError != null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = emailCd }
                    .testTag(MemberInviteTestTags.FIELD_EMAIL_PHONE),
            )
            if (state.validationError != null) {
                Spacer(sp.xs)
                Text(
                    text = validationMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(sp.md)
            RoleDropdown(selectedRole = state.selectedRole, onRoleSelect = { onAction(MemberInviteAction.OnRoleSelect(it)) })
        }
    }

    Spacer(sp.lg)
    KptButton(
        onClick = { onAction(MemberInviteAction.OnGenerateInvite) },
        enabled = state.emailPhone.isNotBlank() && !state.isGenerating,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = sp.touchTargetMin)
            .semantics { contentDescription = generateCd }
            .testTag(MemberInviteTestTags.GENERATE_BUTTON),
    ) {
        if (state.isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp).testTag(MemberInviteTestTags.GENERATING_INDICATOR),
                strokeWidth = 2.dp,
            )
        } else {
            Text(generateLabel)
        }
    }

    if (state.generatedCode != null) {
        Spacer(sp.lg)
        GeneratedCodeCard(
            code = state.generatedCode,
            link = state.generatedLink.orEmpty(),
            onCopyCode = { onAction(MemberInviteAction.OnCopyCode) },
            onCopyLink = { onAction(MemberInviteAction.OnCopyLink) },
            onShare = { onAction(MemberInviteAction.OnShareLink) },
        )
    }
    // Keep the generating label referenced so a11y tools + lint see it (used as the button CD source).
    if (state.isGenerating) {
        Text(text = generatingLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Role picker — `ui.yaml#forms.create_invite_form.role_dropdown`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    selectedRole: MemberRole,
    onRoleSelect: (MemberRole) -> Unit,
) {
    val roleLabel = stringResource(Res.string.screens_member_invite_field_role_label)
    val roleCd = stringResource(Res.string.screens_member_invite_field_role_cd)
    val options = listOf(
        MemberRole.MEMBER to stringResource(Res.string.screens_member_invite_role_member),
        MemberRole.TREASURER to stringResource(Res.string.screens_member_invite_role_treasurer),
        MemberRole.SECRETARY to stringResource(Res.string.screens_member_invite_role_secretary),
        MemberRole.CHAIRPERSON to stringResource(Res.string.screens_member_invite_role_chairperson),
    )
    val selectedLabel = options.firstOrNull { it.first == selectedRole }?.second ?: options.first().second
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(roleLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = roleCd }
                .testTag(MemberInviteTestTags.DROPDOWN_ROLE),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (role, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onRoleSelect(role)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Generated-code card — `ui.yaml#components.generated_code_card`. */
@Composable
private fun GeneratedCodeCard(
    code: String,
    link: String,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
) {
    val sp = MaterialTheme.spacing
    val copyCodeLabel = stringResource(Res.string.screens_member_invite_action_copy_code)
    val copyCodeCd = stringResource(Res.string.screens_member_invite_action_copy_code_cd)
    val copyLinkLabel = stringResource(Res.string.screens_member_invite_action_copy_link)
    val copyLinkCd = stringResource(Res.string.screens_member_invite_action_copy_link_cd)
    val shareLabel = stringResource(Res.string.screens_member_invite_action_share)
    val shareCd = stringResource(Res.string.screens_member_invite_action_share_cd)

    AppCard(modifier = Modifier.testTag(MemberInviteTestTags.GENERATED_CARD)) {
        Column {
            Text(
                text = stringResource(Res.string.screens_member_invite_label_invite_code),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = code,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(sp.sm)
            Text(
                text = stringResource(Res.string.screens_member_invite_label_invite_link),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = link,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(sp.md)
            Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                OutlinedButton(
                    onClick = onCopyCode,
                    modifier = Modifier
                        .semantics { contentDescription = copyCodeCd }
                        .testTag(MemberInviteTestTags.COPY_CODE_BUTTON),
                ) { Text(copyCodeLabel) }
                OutlinedButton(
                    onClick = onCopyLink,
                    modifier = Modifier
                        .semantics { contentDescription = copyLinkCd }
                        .testTag(MemberInviteTestTags.COPY_LINK_BUTTON),
                ) { Text(copyLinkLabel) }
                KptButton(
                    onClick = onShare,
                    modifier = Modifier
                        .semantics { contentDescription = shareCd }
                        .testTag(MemberInviteTestTags.SHARE_BUTTON),
                ) { Text(shareLabel) }
            }
        }
    }
}

/** Pending-invites list / loading / empty — `ui.yaml#states`. */
@Composable
private fun PendingInvitesSection(
    state: MemberInviteState,
    onAction: (MemberInviteAction) -> Unit,
) {
    val sp = MaterialTheme.spacing
    Text(
        text = stringResource(Res.string.screens_member_invite_section_pending),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(sp.md)

    when {
        state.isLoadingPending && state.pendingInvites.isEmpty() -> {
            Box(modifier = Modifier.fillMaxWidth().padding(sp.lg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTag(MemberInviteTestTags.PENDING_LOADING_INDICATOR))
            }
        }
        state.pendingInvites.isEmpty() -> {
            Text(
                text = stringResource(Res.string.screens_member_invite_empty_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(sp.lg)
                    .testTag(MemberInviteTestTags.PENDING_EMPTY_STATE),
            )
        }
        else -> {
            Column(modifier = Modifier.testTag(MemberInviteTestTags.PENDING_LIST)) {
                state.pendingInvites.forEach { invite ->
                    PendingInviteRow(
                        invite = invite,
                        onRevoke = { onAction(MemberInviteAction.OnRevokeInvite(invite.rowId)) },
                    )
                    Spacer(sp.sm)
                }
            }
        }
    }
}

/** Single pending-invite row — `ui.yaml#components.pending_invites_list.item_template`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingInviteRow(
    invite: PendingInvite,
    onRevoke: () -> Unit,
) {
    val sp = MaterialTheme.spacing
    val expiresLabel = stringResource(Res.string.screens_member_invite_label_expires)
    val revokeLabel = stringResource(Res.string.screens_member_invite_action_revoke)
    val revokeCd = stringResource(Res.string.screens_member_invite_action_revoke_cd)

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(sp.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.invitedEmailPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(sp.xs)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(invite.roleToAssign.name.lowercase()) },
                        colors = AssistChipDefaults.assistChipColors(),
                    )
                    Spacer(sp.sm)
                    Text(
                        text = "$expiresLabel ${invite.expiresAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            TextButton(
                onClick = onRevoke,
                modifier = Modifier
                    .semantics { contentDescription = revokeCd }
                    .testTag("${MemberInviteTestTags.REVOKE_BUTTON}_${invite.rowId}"),
            ) {
                Text(text = revokeLabel, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Inline error banner + Retry — `ui.yaml#components.error_banner`. */
@Composable
private fun ErrorBanner(
    error: MemberInviteError,
    onRetry: () -> Unit,
) {
    val sp = MaterialTheme.spacing
    val title = stringResource(Res.string.screens_member_invite_error_banner_title)
    val iconCd = stringResource(Res.string.screens_member_invite_error_banner_icon_cd)
    val retryLabel = stringResource(Res.string.screens_member_invite_action_retry)
    val retryCd = stringResource(Res.string.screens_member_invite_action_retry_cd)

    AppCard(modifier = Modifier.testTag(MemberInviteTestTags.ERROR_BANNER)) {
        Column(modifier = Modifier.semantics { contentDescription = iconCd }) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
            Spacer(sp.xs)
            Text(text = error.resolvedMessage(), style = MaterialTheme.typography.bodyMedium)
            if (error.retry) {
                Spacer(sp.sm)
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .semantics { contentDescription = retryCd }
                        .testTag(MemberInviteTestTags.ERROR_RETRY_BUTTON),
                ) { Text(retryLabel) }
            }
        }
    }
}

/** Maps [MemberInviteError] to its localized message. */
@Composable
private fun MemberInviteError.resolvedMessage(): String = stringResource(messageResource())

private fun MemberInviteError.messageResource(): StringResource = when (this) {
    MemberInviteError.Validation -> Res.string.screens_member_invite_error_validation
    MemberInviteError.Network -> Res.string.screens_member_invite_error_network
    MemberInviteError.Server -> Res.string.screens_member_invite_error_server
    MemberInviteError.Auth -> Res.string.screens_member_invite_error_auth
    MemberInviteError.RevokeFailure -> Res.string.screens_member_invite_error_revoke
}

/** Vertical-gap shorthand. */
@Composable
private fun Spacer(height: Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}
