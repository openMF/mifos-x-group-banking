/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberprofile

/**
 * Append-only test-tag registry for the `member-profile` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/member-profile/src/commonTest/` and by
 * the Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. Constant names are derived 1:1
 * from `ui.yaml#components[].id`. Mirrors `GroupDashboardTestTags`'s identical convention.
 *
 * The `top_bar`/back-navigation icon carries no per-screen testTag hook — `KptTopAppBar`'s
 * navigation `IconButton` is not individually taggable (only the whole app-bar surface is, via
 * `KptTopAppBarConfiguration.testTag`), same documented gap as `GroupDashboardTestTags` (no
 * `BACK_BUTTON` constant there either).
 *
 * See API.md#tags.
 */
object MemberProfileTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "member_profile_screen"

    /** `ui.yaml#components.member_header_card`. */
    const val HEADER_CARD: String = "member_profile_header_card"

    /** `ui.yaml#components.member_header_card.content.member_avatar`. */
    const val AVATAR: String = "member_profile_avatar"

    /** `ui.yaml#components.member_header_card.content.role_chip`. */
    const val ROLE_CHIP: String = "member_profile_role_chip"

    /** `ui.yaml#components.member_header_card.content.edit_role_button` — [MemberProfileAction.OnEditRoleTap], visible only when `isCurrentUserChairperson`. */
    const val EDIT_ROLE_BUTTON: String = "member_profile_edit_role_button"

    /** `ui.yaml#components.savings_history_card`. */
    const val SAVINGS_CARD: String = "member_profile_savings_card"

    /** `ui.yaml#components.savings_history_card.content.savings_sparkline`. */
    const val SPARKLINE: String = "member_profile_savings_sparkline"

    /** `ui.yaml#components.savings_history_card.content.view_full_history_button` — [MemberProfileAction.OnViewSavings]. */
    const val VIEW_FULL_HISTORY_BUTTON: String = "member_profile_view_full_history_button"

    /** `ui.yaml#components.active_loan_card` — visible only when `accounts.activeLoan != null`. */
    const val LOAN_CARD: String = "member_profile_loan_card"

    /** `ui.yaml#components.active_loan_card.content.loan_arrears_banner` — visible only when `activeLoan.inArrears`. */
    const val LOAN_ARREARS_BANNER: String = "member_profile_loan_arrears_banner"

    /** `ui.yaml#components.attendance_card`. */
    const val ATTENDANCE_CARD: String = "member_profile_attendance_card"

    /** `ui.yaml#components.attendance_card.content.attendance_progress_bar`. */
    const val ATTENDANCE_PROGRESS_BAR: String = "member_profile_attendance_progress_bar"

    /** `ui.yaml#components.role_edit_bottom_sheet` — visible when `isEditingRole`. */
    const val ROLE_BOTTOM_SHEET: String = "member_profile_role_bottom_sheet"

    /** `ui.yaml#components.role_edit_bottom_sheet.content.role_option_chairperson`. */
    const val ROLE_OPTION_CHAIRPERSON: String = "member_profile_role_option_chairperson"

    /** `ui.yaml#components.role_edit_bottom_sheet.content.role_option_treasurer`. */
    const val ROLE_OPTION_TREASURER: String = "member_profile_role_option_treasurer"

    /** `ui.yaml#components.role_edit_bottom_sheet.content.role_option_secretary`. */
    const val ROLE_OPTION_SECRETARY: String = "member_profile_role_option_secretary"

    /** `ui.yaml#components.role_edit_bottom_sheet.content.role_option_member`. */
    const val ROLE_OPTION_MEMBER: String = "member_profile_role_option_member"

    /** `ui.yaml#components.role_edit_bottom_sheet.content.confirm_role_button` — [MemberProfileAction.OnConfirmRoleChange]. */
    const val CONFIRM_ROLE_BUTTON: String = "member_profile_confirm_role_button"

    /** Shimmer skeleton region — `MemberProfileScreenState.Loading`. */
    const val LOADING_SECTION: String = "member_profile_loading_section"

    /** Full-screen error surface — `MemberProfileScreenState.Error`. */
    const val ERROR_SECTION: String = "member_profile_error_section"

    /** Retry CTA on the error state — [MemberProfileAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "member_profile_error_retry_button"
}
