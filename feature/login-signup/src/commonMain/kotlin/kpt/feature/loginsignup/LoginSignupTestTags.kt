/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.loginsignup

/**
 * Append-only test-tag registry for the `login-signup` feature (RULE-KMP-COMPOSE-UITEST-001
 * CU-5 — names are stable across regenerations; only append new entries, never rename or
 * remove). Consumed by Compose UI tests under `feature/login-signup/src/commonTest/` and by
 * the Maestro flow generator (`core/scripts/maestro-flow-gen.ts`), which emits
 * `tapOn: { id: "<test_tag>" }` selectors from these constants. See API.md#tags.
 */
object LoginSignupTestTags {

    /** Root [kpt.core.ui.scaffold.KptScaffold] surface — always rendered regardless of screenState. */
    const val SCREEN: String = "login_signup_screen"

    /** [kpt.core.base.designsystem.component.AppCard] wrapping the login/signup form. */
    const val AUTH_CARD: String = "login_signup_auth_card"

    /** LOGIN tab of the mode-toggle [androidx.compose.material3.TabRow]. */
    const val TAB_LOGIN: String = "login_signup_tab_login"

    /** SIGNUP tab of the mode-toggle [androidx.compose.material3.TabRow]. */
    const val TAB_SIGNUP: String = "login_signup_tab_signup"

    /** Full-name input — signup mode only. */
    const val FIELD_NAME: String = "login_signup_field_name"

    /** Email-or-phone input — shared by both login and signup modes. */
    const val FIELD_EMAIL_PHONE: String = "login_signup_field_email_phone"

    /** Password input — shared by both login and signup modes. */
    const val FIELD_PASSWORD: String = "login_signup_field_password"

    /** "Forgot password?" link — login mode only. */
    const val FORGOT_PASSWORD_LINK: String = "login_signup_forgot_password_link"

    /** "Sign In" primary submit button — login mode. */
    const val LOGIN_BUTTON: String = "login_signup_login_button"

    /** "Create Account" primary submit button — signup mode. */
    const val SIGNUP_BUTTON: String = "login_signup_signup_button"

    /** Biometric unlock button — login mode, shown only when hardware is available. */
    const val BIOMETRIC_BUTTON: String = "login_signup_biometric_button"

    /** Inline error banner surfaced when `LoginSignupState.error != null`. */
    const val ERROR_BANNER: String = "login_signup_error_banner"

    /** Zero-groups empty-state illustration icon. */
    const val ZERO_GROUPS_ILLUSTRATION: String = "login_signup_zero_groups_illustration"

    /** "Create Your First Group" button — ZeroGroups screenState. */
    const val CREATE_GROUP_BUTTON: String = "login_signup_create_group_button"

    /** "Join with Invite Code" button — ZeroGroups screenState. */
    const val JOIN_WITH_CODE_BUTTON: String = "login_signup_join_with_code_button"

    /** Labeled divider separating the primary CTA from the first-class alt-auth entries (both modes). */
    const val ALT_ACTIONS_DIVIDER: String = "login_signup_alt_actions_divider"

    /** First-class PRE-AUTH "Accept an Invitation" outlined button — Content state, both modes. */
    const val ACCEPT_INVITATION_BUTTON: String = "login_signup_accept_invitation_button"

    /** "Demo Explore" text button — Content state, both modes. */
    const val DEMO_EXPLORE_BUTTON: String = "login_signup_demo_explore_button"

    /** Demo Explore confirm [androidx.compose.material3.AlertDialog] (shown while showDemoDialog || isSeedingDemo). */
    const val DEMO_CONFIRM_DIALOG: String = "login_signup_demo_confirm_dialog"

    /** "Continue" confirm button inside the Demo Explore dialog (shows a spinner while isSeedingDemo). */
    const val DEMO_CONFIRM_BUTTON: String = "login_signup_demo_confirm_button"

    /** "Cancel" dismiss button inside the Demo Explore dialog. */
    const val DEMO_CANCEL_BUTTON: String = "login_signup_demo_cancel_button"

    // -- Signup password-requirement chips (live Fineract-policy feedback) --------------------------

    /** "12+ characters" rule chip — filled when `passwordRequirements.hasMinLength`. */
    const val PASSWORD_RULE_LEN: String = "login_signup_password_rule_len"

    /** "Upper & lower" rule chip — filled when `passwordRequirements.hasMixedCase`. */
    const val PASSWORD_RULE_CASE: String = "login_signup_password_rule_case"

    /** "Number" rule chip — filled when `passwordRequirements.hasDigit`. */
    const val PASSWORD_RULE_DIGIT: String = "login_signup_password_rule_digit"

    /** "Symbol" rule chip — filled when `passwordRequirements.hasSymbol`. */
    const val PASSWORD_RULE_SYMBOL: String = "login_signup_password_rule_symbol"

    /** "No repeats" rule chip — filled when `passwordRequirements.hasNoRepeats`. */
    const val PASSWORD_RULE_NOREPEAT: String = "login_signup_password_rule_norepeat"
}
