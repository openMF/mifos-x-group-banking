/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.memberlist

/**
 * Append-only test-tag registry for the `member-list` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5 —
 * names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/member-list/src/commonTest/` and by the Maestro flow
 * generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }`
 * selectors from these constants. Mirrors `GroupListTestTags` / `GroupDashboardTestTags`'s
 * identical convention. See API.md#tags.
 */
object MemberListTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "member_list_screen"

    /** Loading section — `MemberListScreenState.Loading` (shimmer rows + spinner). */
    const val LOADING_SECTION: String = "member_list_loading_section"

    /** Circular loading indicator inside the loading section. */
    const val LOADING_INDICATOR: String = "member_list_loading_indicator"

    /** Scrollable list surface wrapping the member rows — `MemberListScreenState.Content`. */
    const val MEMBER_LIST: String = "member_list_lazy_column"

    /** Linear progress footer shown while `MemberListState.isLoadingMore` — `ui.yaml#load_more_indicator`. */
    const val LOAD_MORE_INDICATOR: String = "member_list_load_more_indicator"

    /** `+ Add Member` FAB — [MemberListAction.OnAddMember]. */
    const val FAB_ADD_MEMBER: String = "member_list_fab_add_member"

    /** Invite-member top-bar action — [MemberListAction.OnInviteMember] (invite-path -> member-invite). */
    const val INVITE_ACTION: String = "member_list_invite_action"

    /** Full-illustration empty surface — `MemberListScreenState.Empty` (zero members). */
    const val EMPTY_SECTION: String = "member_list_empty_section"

    /** "Add Member" CTA on the empty state — [MemberListAction.OnAddMember]. */
    const val EMPTY_ADD_BUTTON: String = "member_list_empty_add_button"

    /** Inline error surface — `MemberListScreenState.Error`. */
    const val ERROR_SECTION: String = "member_list_error_section"

    /** Retry CTA on the error state — [MemberListAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "member_list_error_retry_button"

    /**
     * Resolves the stable per-member test tag for a given member id. Rows render data-driven from
     * `MemberListState.members` (not as N separate composables), so this is a function rather than
     * a fixed constant set — mirrors `GroupListTestTags.cardTag`.
     */
    fun memberRowTag(memberId: String): String = "member_list_row_$memberId"
}
