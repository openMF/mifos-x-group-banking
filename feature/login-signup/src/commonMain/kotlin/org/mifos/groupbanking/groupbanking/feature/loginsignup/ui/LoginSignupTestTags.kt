/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.ui

/**
 * Stable test-tag constants for the login-signup screen. APPEND-ONLY — drive both
 * Compose UI tests and Maestro flows off these identifiers.
 */
object LoginSignupTestTags {
    const val SCREEN = "login_signup_screen"
    const val MODE_TOGGLE = "login_signup_mode_toggle"
    const val NAME_FIELD = "login_signup_name_field"
    const val EMAIL_PHONE_FIELD = "login_signup_email_phone_field"
    const val PASSWORD_FIELD = "login_signup_password_field"
    const val PIN_FIELD = "login_signup_pin_field"
    const val SUBMIT_BUTTON = "login_signup_submit_button"
    const val BIOMETRIC_CTA = "login_signup_biometric_cta"
    const val FORGOT_PASSWORD = "login_signup_forgot_password"
    const val ERROR_BANNER = "login_signup_error_banner"
    const val LOADING = "login_signup_loading"
    const val ZERO_GROUPS = "login_signup_zero_groups"
    const val CREATE_GROUP_CTA = "login_signup_create_group_cta"
    const val JOIN_WITH_CODE_CTA = "login_signup_join_with_code_cta"
}
