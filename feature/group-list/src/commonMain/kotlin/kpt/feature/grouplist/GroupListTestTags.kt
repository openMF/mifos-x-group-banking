/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouplist

/**
 * Append-only test-tag registry for the `group-list` feature (RULE-KMP-COMPOSE-UITEST-001 CU-5 —
 * names are stable across regenerations; only append new entries, never rename or remove).
 * Consumed by Compose UI tests under `feature/group-list/src/commonTest/` and by the Maestro flow
 * generator (`core/scripts/maestro-flow-gen.ts`), which emits `tapOn: { id: "<test_tag>" }`
 * selectors from these constants. Mirrors `GroupTypePickerTestTags`'s identical convention. See
 * API.md#tags.
 */
object GroupListTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "group_list_screen"

    /**
     * Top-bar notification bell — [GroupListAction.OnOpenNotifications] (G15, deferred-notifications
     * snackbar). Rendered as a framework `TopAppBarAction`, which exposes NO `testTag`/`Modifier`
     * slot, so UI tests / Maestro select this affordance by its `contentDescription`
     * (`screens_group_list_notifications_cd`), not by this tag string; kept here for registry
     * completeness + Maestro selector documentation.
     */
    const val NOTIFICATION_ACTION: String = "group_list_notification_action"

    /** Search input — `ui.yaml#components.search_bar`. Bound to [GroupListAction.OnSearch]. */
    const val SEARCH_FIELD: String = "group_list_search_field"

    /** Clear-icon inside the search field — [GroupListAction.OnClearSearch]. */
    const val SEARCH_CLEAR_ICON: String = "group_list_search_clear_icon"

    /** Inline "no results for query" message — search-active + zero matches, still `Content`. */
    const val SEARCH_EMPTY_MESSAGE: String = "group_list_search_empty_message"

    /** Circular loading indicator + skeleton rows — `GroupListScreenState.Loading`. */
    const val LOADING_INDICATOR: String = "group_list_loading_indicator"

    /** Scrollable list surface wrapping the group cards — `GroupListScreenState.Content`. */
    const val GROUP_LIST: String = "group_list_lazy_column"

    /** `+ New Group` FAB — [GroupListAction.OnCreateGroup]. */
    const val FAB_CREATE: String = "group_list_fab_create"

    /** Full-illustration empty surface — `GroupListScreenState.Empty` (zero groups, no search). */
    const val EMPTY_SECTION: String = "group_list_empty_section"

    /** "Create Group" CTA on the empty state — [GroupListAction.OnCreateGroup]. */
    const val EMPTY_CREATE_BUTTON: String = "group_list_empty_create_button"

    /** "Join with Code" CTA on the empty state — [GroupListAction.OnJoinGroup]. */
    const val EMPTY_JOIN_BUTTON: String = "group_list_empty_join_button"

    /** Inline error surface — `GroupListScreenState.Error`. */
    const val ERROR_SECTION: String = "group_list_error_section"

    /** Retry CTA on the error state — [GroupListAction.Retry]. */
    const val ERROR_RETRY_BUTTON: String = "group_list_error_retry_button"

    /**
     * Resolves the stable per-card test tag for a given group id. Cards render data-driven from
     * `GroupListState.filteredGroups` (not as N separate composables), so this is a function
     * rather than a fixed constant set — mirrors `GroupTypePickerTestTags.cardTag`.
     */
    fun cardTag(groupId: String): String = "group_list_card_$groupId"
}
