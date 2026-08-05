/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fineract-native auth wire contracts — replaces the companion `/companion/auth` bridge with
 * direct Fineract endpoints so the app authenticates against the single-instance SoT
 * (BuildKonfig.FINERACT_BASE_URL). See `CompanionAuthApiImpl` for the login/me rewire.
 */

/**
 * Request body for `POST /fineract-provider/api/v1/authentication` — the platform's basic-auth
 * key exchange. `username` is sourced from the app's `emailPhone` login field.
 */
@Serializable
data class FineractAuthRequestDto(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)

/**
 * Response body for `POST /fineract-provider/api/v1/authentication`. On `authenticated == true`
 * the [base64EncodedAuthenticationKey] is the credential the app attaches as
 * `Authorization: Basic <key>` on every subsequent request (see `CompanionAuthHeaderPlugin`).
 * The key does not expire, so callers synthesize a far-future `tokenExpiresAt`.
 */
@Serializable
data class FineractAuthResponseDto(
    @SerialName("username") val username: String? = null,
    @SerialName("userId") val userId: Long? = null,
    @SerialName("base64EncodedAuthenticationKey") val base64EncodedAuthenticationKey: String? = null,
    @SerialName("authenticated") val authenticated: Boolean = false,
    @SerialName("officeId") val officeId: Long? = null,
    @SerialName("officeName") val officeName: String? = null,
)

/**
 * Response body for `GET /fineract-provider/api/v1/userdetails` — the authenticated caller's
 * profile. Maps onto the app's `UserProfileDto`; Fineract exposes no email/phone or group
 * memberships here, so those fields fall back to the username / empty list.
 */
@Serializable
data class FineractUserDetailsDto(
    @SerialName("username") val username: String? = null,
    @SerialName("userId") val userId: Long? = null,
    @SerialName("officeId") val officeId: Long? = null,
    @SerialName("officeName") val officeName: String? = null,
)
