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

/**
 * Append-only test-tag registry for the `join-with-code` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/join-with-code/src/commonTest/` and by
 * the Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object JoinWithCodeTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "join_with_code_screen"

    /**
     * Top app bar carrying the `OnBack` navigation icon (`ui.yaml#components.top_bar`). The
     * back arrow is the app bar's only interactive element (no `actions`), so this tag is the
     * stable Maestro target for the "dismiss and go back" affordance. `KptTopAppBar`'s internal
     * `IconButton` has no dedicated per-button testTag slot of its own — `core-base/designsystem`
     * is framework-shared and not modifiable per RULE-TEMPLATE-MODULE-FIX-UPSTREAM-001, so this
     * constant tags the whole app-bar container via `KptTopAppBarConfiguration.testTag`.
     */
    const val BACK_BUTTON: String = "join_with_code_back_button"

    /** 6-char invite-code entry field — `ui.yaml#components.invite_code_field`. */
    const val INVITE_CODE_FIELD: String = "join_with_code_invite_code_field"

    /**
     * "Validate Code" button — rendered on `Initial` and every `Error*` screen state (mirrors
     * `ui.yaml#components.validate_button`).
     */
    const val VALIDATE_BUTTON: String = "join_with_code_validate_button"

    /** Spinner shown while `inviteStatus == VALIDATING` — `ui.yaml#components.validating_indicator`. */
    const val VALIDATING_INDICATOR: String = "join_with_code_validating_indicator"

    /** Group preview confirmation card — `ui.yaml#components.group_preview_card`. */
    const val PREVIEW_CARD: String = "join_with_code_preview_card"

    /** "Join Group" CTA — `Preview` / `Joining` screen states. */
    const val CONFIRM_JOIN_BUTTON: String = "join_with_code_confirm_join_button"

    /** Inline error banner — `ui.yaml#components.error_card`. */
    const val ERROR_CARD: String = "join_with_code_error_card"

    /** Retry CTA inside the error banner — visible only when `error.retry == true`. */
    const val RETRY_BUTTON: String = "join_with_code_retry_button"
}
