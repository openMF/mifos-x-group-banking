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

/**
 * Append-only test-tag registry for the `member-invite` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests + the Maestro flow generator. See API.md#tags.
 */
object MemberInviteTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered. */
    const val SCREEN: String = "member_invite_screen"

    /** Top app bar (`ui.yaml#components.top_bar`) — back arrow dismisses the screen. */
    const val TOP_BAR: String = "member_invite_top_bar"

    /** `email_phone_field` — contact input. */
    const val FIELD_EMAIL_PHONE: String = "member_invite_field_email_phone"

    /** `role_dropdown`. */
    const val DROPDOWN_ROLE: String = "member_invite_dropdown_role"

    /** `generate_button` — fires OnGenerateInvite. */
    const val GENERATE_BUTTON: String = "member_invite_generate_button"

    /** Centered spinner overlay while COMP-DT-002 is in-flight (`MemberInviteScreenState.Generating`). */
    const val GENERATING_INDICATOR: String = "member_invite_generating_indicator"

    /** `generated_code_card` — visible when `generatedCode != null`. */
    const val GENERATED_CARD: String = "member_invite_generated_card"

    /** `copy_code_button` on the generated card. */
    const val COPY_CODE_BUTTON: String = "member_invite_copy_code_button"

    /** `copy_link_button` on the generated card. */
    const val COPY_LINK_BUTTON: String = "member_invite_copy_link_button"

    /** `share_button` on the generated card. */
    const val SHARE_BUTTON: String = "member_invite_share_button"

    /** Loading spinner for the pending-invites list (`isLoadingPending`). */
    const val PENDING_LOADING_INDICATOR: String = "member_invite_pending_loading_indicator"

    /** Empty-state text when there are no pending invites. */
    const val PENDING_EMPTY_STATE: String = "member_invite_pending_empty_state"

    /** Pending-invites list container. */
    const val PENDING_LIST: String = "member_invite_pending_list"

    /** Per-row `revoke_button` — fires OnRevokeInvite(rowId). */
    const val REVOKE_BUTTON: String = "member_invite_revoke_button"

    /** Inline error banner — `MemberInviteScreenState.Error`. */
    const val ERROR_BANNER: String = "member_invite_error_banner"

    /** `retry_action` embedded in [ERROR_BANNER] — fires OnRetry. */
    const val ERROR_RETRY_BUTTON: String = "member_invite_error_retry_button"
}
